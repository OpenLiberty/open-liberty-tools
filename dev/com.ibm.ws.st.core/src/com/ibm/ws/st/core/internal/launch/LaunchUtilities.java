/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.ws.st.core.AbstractLibertyDebugFactory;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.IPromptIssue;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.PromptAction;
import com.ibm.ws.st.core.internal.PromptHandler;
import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

@SuppressWarnings("restriction")
public class LaunchUtilities {

    public static int TIMEOUT = 10000;
    public static final String DEBUGGER_FACTORY_EXTENSION_POINT = "debugTargetFactory";
    public static final String DEBUGGER_FACTORY_OVERRIDE_PROPERTY = "com.ibm.ws.st.debugFactoryOverride";
    public static final String DEBUGGER_FACTORY_JDT_OVERRIDE_WEIGHT = "0";

    protected static AbstractLibertyDebugFactory debugTargetFactory = null;
    protected static boolean debugTargetFactoryHasBeenInitialized = false;

    /**
     * Returns a free port number on localhost, or -1 if unable to find a free port.
     *
     * @return a free port number on localhost, or -1 if unable to find a free port
     */
    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            // ignore
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return -1;
    }

    protected static IProcess newProcess(final IServer server, ILaunch launch, Process p, String label) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(IProcess.ATTR_PROCESS_TYPE, "java");
        return new RuntimeProcess(launch, p, label, attributes) {
            @Override
            public void terminate() throws DebugException {
                // if the server is still running, have to tell it to stop instead
                int state = server.getServerState();
                WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                if (state != IServer.STATE_STOPPED && state != IServer.STATE_STOPPING && wsServer != null && wsServer.isStopOnShutdown()) {
                    server.stop(false);

                    WebSphereServerBehaviour wsb = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
                    if (wsb != null && wsb.waitForServerStop(10000)) {
                        return;
                    }
                    // If server.stop() fails, then try to terminate the system process by calling super.terminate()
                    // See the line: process.destroy();
                    // Note, on Windows, this call does not appear to do anything.  (So, we must rely on server.stop()) to properly
                    // terminate the process.
                    super.terminate();
                }
            }

        };
    }

    /**
     * Returns the default 'com.sun.jdi.SocketAttach' connector.
     *
     * @return the {@link AttachingConnector}
     */
    public static AttachingConnector getAttachingConnector() {
        List<?> connectors = Bootstrap.virtualMachineManager().attachingConnectors();
        for (int i = 0; i < connectors.size(); i++) {
            AttachingConnector c = (AttachingConnector) connectors.get(i);
            if ("com.sun.jdi.SocketAttach".equals(c.name()))
                return c;
        }

        return null;
    }

    /**
     * Configure the connector properties.
     *
     * @param map argument map
     * @param host the host name or IP address
     * @param portNumber the port number
     */
    public static void configureConnector(Map map, String host, int portNumber) {
        Connector.StringArgument hostArg = (Connector.StringArgument) map.get("hostname");
        hostArg.setValue(host);

        Connector.IntegerArgument portArg = (Connector.IntegerArgument) map.get("port");
        portArg.setValue(portNumber);

        Connector.IntegerArgument timeoutArg = (Connector.IntegerArgument) map.get("timeout");
        if (timeoutArg != null) {
            int timeout = Platform.getPreferencesService().getInt(
                                                                  "org.eclipse.jdt.launching",
                                                                  JavaRuntime.PREF_CONNECT_TIMEOUT,
                                                                  JavaRuntime.DEF_CONNECT_TIMEOUT,
                                                                  null);
            timeoutArg.setValue(timeout);
        }
    }

    public static IDebugTarget createRemoteJDTDebugTarget(ILaunch launch, int remoteDebugPortNum, String hostName, AttachingConnector connector, Map map) throws CoreException {
        if (launch == null || hostName == null || hostName.length() == 0) {
            return null;
        }
        VirtualMachine remoteVM = null;
        Exception ex = null;
        IDebugTarget debugTarget = null;
        try {
            remoteVM = attachJVM(hostName, remoteDebugPortNum, connector, map, TIMEOUT);
        } catch (Exception e) {
            ex = e;
        }
        if (remoteVM == null) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IJavaLaunchConfigurationConstants.ERR_CONNECTION_FAILED, "", ex));
        }
        String name = NLS.bind(Messages.debugTargetLabel, hostName, remoteDebugPortNum + "");
        debugTarget = JDIDebugModel.newDebugTarget(launch, remoteVM, name, null, false, true, true);
        return debugTarget;
    }

    public static VirtualMachine attachJVM(String hostName, int port, AttachingConnector connector, Map map, int timeout) {
        VirtualMachine vm = null;
        int timeOut = timeout;
        try {
            try {
                vm = connector.attach(map);
            } catch (IOException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Error occured while trying to connect to the remote virtual machine " + e);
            } catch (TimeoutException e2) {
                //do nothing
            }

            while (vm == null && timeOut > 0) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    //do  nothing
                }
                timeOut = timeOut - 100;
                try {
                    vm = connector.attach(map);
                } catch (IOException e) {
                    //do nothing
                }
            }
        } catch (IllegalConnectorArgumentsException e) {
            // Do nothing, return vm as null if it fails
        }
        return vm;
    }

    public static void connectRemoteDebugClient(ILaunch launch, int remoteDebugPortNum, WebSphereServer wsServer) throws CoreException {

        WebSphereServerBehaviour wsBehaviour = wsServer.getWebSphereServerBehaviour();
        String hostName = wsServer.getConnectionHost();

        //check to see if there is any extended debugger factory available
        AbstractLibertyDebugFactory df = LaunchUtilities.getDebugTargetFactory();
        IDebugTarget curDebugTarget = null;
        if (df != null) {
            String debugTargetLabel = NLS.bind(Messages.debugTargetLabel, hostName, remoteDebugPortNum + "");
            curDebugTarget = df.createDebugTarget(launch, hostName, Integer.toString(remoteDebugPortNum), debugTargetLabel, null);
        }

        //if there is no debugger factory, or if it returned null, fall back to the JDT debugger
        if (curDebugTarget == null) {
            try {
                AttachingConnector connector = LaunchUtilities.getAttachingConnector();
                Map map = connector.defaultArguments();
                LaunchUtilities.configureConnector(map, hostName, remoteDebugPortNum);
                curDebugTarget = LaunchUtilities.createRemoteJDTDebugTarget(launch, remoteDebugPortNum, hostName, connector, map);
            } catch (CoreException e) {
                Trace.logError("Couldn't create a debug target. Debug connection failed.", e);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
            }
        }

        if (curDebugTarget != null) {
            wsBehaviour.setDebugTarget(curDebugTarget);
            Activator.getInstance().addDebugTarget(curDebugTarget);
            //can be used later to determine if the debugger is already attached at this port
            wsBehaviour.setDebugPortNum(remoteDebugPortNum);
            launch.addDebugTarget(curDebugTarget);

            // Add hot code replace listener to listen for hot code replace failure.
            IJavaDebugTarget jdt = curDebugTarget.getAdapter(IJavaDebugTarget.class);
            if (jdt != null) {
                jdt.addHotCodeReplaceListener(new LaunchUtilities.WebSphereJavaHotCodeReplaceListener(wsServer.getServer()));
            }
        }
    }

    /**
     * Allows other components to specify VM arguments through the preStart extension point.
     *
     * @param vmArgs
     * @param startInfo the server start information
     * @return
     */
    public static String processVMArguments(String vmArgs, ServerStartInfo startInfo) {
        try {
            AbstractServerStartupExtension[] preStartExtensions = Activator.getInstance().getPreStartExtensions();
            // if there aren't any preStart extensions then there's nothing to do
            if (preStartExtensions.length == 0)
                return vmArgs;

            String[] vmArray = DebugPlugin.parseArguments(vmArgs);
            List<String> vmArgsList = new ArrayList<String>(vmArray.length);
            for (String vmArg : vmArray) {
                vmArgsList.add(vmArg);
            }

            for (AbstractServerStartupExtension startup : preStartExtensions) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "VMArgs BEFORE Pre-startup class: " + startup.getClass().toString() + " " + vmArgsList.toString());
                startup.setJVMOptions(startInfo, vmArgsList);
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "VMArgs AFTER Pre-startup class: " + startup.getClass().toString() + " " + vmArgsList.toString());
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vmArgsList.size(); i++) {
                if (i > 0)
                    sb.append(" ");
                String s = vmArgsList.get(i);
                if (!s.contains("\""))
                    sb.append("\"" + s + "\"");
                else
                    sb.append(s);
            }
            return sb.toString();
        } catch (Exception e) {
            Trace.logError("Problem occurred while arguments were processed", e);
        }
        return vmArgs;
    }

    public static AbstractLibertyDebugFactory getDebugTargetFactory() {
        if (!debugTargetFactoryHasBeenInitialized) {
            //Only create when running in UI mode.
            if (PromptUtil.isRunningGUIMode())
                debugTargetFactory = createDebugTargetFactory();
            debugTargetFactoryHasBeenInitialized = true;
        }
        return debugTargetFactory;
    }

    public static AbstractLibertyDebugFactory createDebugTargetFactory() {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetUtil() "
                                    + "Loading debug target factory extension point...");
        }
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] configElements = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, DEBUGGER_FACTORY_EXTENSION_POINT);

        AbstractLibertyDebugFactory debugTargetFactory = null;

        //check a system property to allow the forced selection of a particular debugger for problem determination purposes
        String override = System.getProperty(DEBUGGER_FACTORY_OVERRIDE_PROPERTY);

        // Use weight to determine which debugger to use. If the weights are the same, there
        // are no guarantees on which debugger to use
        int highestWeight = 0;
        IConfigurationElement selectedDebuggerConfig = null;
        for (int i = configElements.length; --i >= 0;) {
            try {
                String curWeightString = configElements[i].getAttribute("weight");

                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetFactory() " +
                                            "Found debugger : " + configElements[i].getAttribute("class") + " with weight : " + curWeightString);
                }

                //if an override has been specified, ignore all other implementors
                if (override != null) {

                    //if the override is zero, force the use of the JDT debugger
                    if (DEBUGGER_FACTORY_JDT_OVERRIDE_WEIGHT.equals(override)) {
                        return null;
                    }

                    //if this debugger's weight matches the specified override, select it immediately
                    if (curWeightString != null && curWeightString.equals(override)) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetFactory() " +
                                                    "Found debugger : " + configElements[i].getAttribute("class") + " which matches override value : " + curWeightString);
                        }
                        selectedDebuggerConfig = configElements[i];
                        break;
                    }

                } else {
                    //otherwise, find the implementor with the highest weight
                    //if the implementor's weight is unspecified or invalid, default to the lowest weight
                    int curWeightInt = 1;
                    try {
                        curWeightInt = Integer.parseInt(curWeightString);
                    } catch (NumberFormatException nfe) {
                        // Do nothing
                    }

                    if (curWeightInt >= highestWeight) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetFactory() " +
                                                    "Found debugger : " + configElements[i].getAttribute("class") + " with weight : " + curWeightInt);
                        }

                        selectedDebuggerConfig = configElements[i];
                        highestWeight = curWeightInt;
                    }
                }

            } catch (Throwable t) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetFactory() " +
                                            "Could not load the debug target util: " + configElements[i].getAttribute("class"),
                                t);
                }
            } // end try statement for debug target
        } // end for loop for debug target

        // We have found a debugger
        if (selectedDebuggerConfig != null) {
            try {
                debugTargetFactory = (AbstractLibertyDebugFactory) selectedDebuggerConfig.createExecutableExtension("class");
            } catch (Throwable t) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetFactory() " +
                                            "Could not load the debug target util: " + selectedDebuggerConfig.getAttribute("class"),
                                t);
                }
            }
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, WebSphereLaunchConfigurationDelegate.class + "createDebugTargetFactory() " +
                                    "Finished creating debug target util: " + debugTargetFactory);
        }

        return debugTargetFactory;

    }

    protected static class HotMethodReplaceFailedPrompt extends PromptHandler.AbstractPrompt {
        private final IPromptIssue issue;

        protected HotMethodReplaceFailedPrompt(String curErrMsg, String[] curModuleNames) {
            issue = new HotCodeReplaceIssue(curErrMsg, curModuleNames);
        }

        @Override
        public boolean getApplyAlways() {
            return false;
        }

        @Override
        public IPromptIssue[] getIssues() {
            return new IPromptIssue[] { issue };
        }
    }

    static class HotCodeReplaceIssue implements IPromptIssue {

        private final boolean isRestartServers;
        private final String details;

        HotCodeReplaceIssue(String errMsg, String[] moduleNames) {
            isRestartServers = moduleNames == null || moduleNames.length == 0;
            details = buildDetails(errMsg, moduleNames);
        }

        @Override
        public String getType() {
            return Messages.hotCodeReplaceFailedIssue;
        }

        @Override
        public String getSummary() {
            return Messages.hotCodeReplaceFailedSummary;
        }

        @Override
        public String getDetails() {
            return details;
        }

        @Override
        public PromptAction[] getPossibleActions() {
            return (isRestartServers) ? new PromptAction[] { PromptAction.RESTART_SERVERS, PromptAction.IGNORE } : new PromptAction[] { PromptAction.RESTART_APPLICATIONS,
                                                                                                                                        PromptAction.IGNORE };
        }

        @Override
        public PromptAction getDefaultAction() {
            return (isRestartServers) ? PromptAction.RESTART_SERVERS : PromptAction.RESTART_APPLICATIONS;
        }

        private String buildDetails(String errMsg, String[] moduleNames) {
            if (moduleNames != null && moduleNames.length > 0) {
                StringBuilder moduleLstStr = new StringBuilder();
                for (String curModuleName : moduleNames) {
                    moduleLstStr.append("\t").append(curModuleName).append("\n");
                }
                return NLS.bind(Messages.hotCodeReplaceFailedGeneralMsg, errMsg) + NLS.bind(Messages.hotCodeReplaceFailedRestartModuleMsg, moduleLstStr);
            }

            return NLS.bind(Messages.hotCodeReplaceFailedGeneralMsg, errMsg) + Messages.hotCodeReplaceFailedRestartServerMsg;
        }
    }

    /**
     * Class for handling java hot code replace failure
     */
    public static class WebSphereJavaHotCodeReplaceListener implements IJavaHotCodeReplaceListener {
        private final IServer server;

        public WebSphereJavaHotCodeReplaceListener(IServer curServer) {
            server = curServer;
        }

        @Override
        public void hotCodeReplaceFailed(IJavaDebugTarget jdt, DebugException de) {
            IThread[] threads;
            List<IModule> matchedModuleLst = new ArrayList<IModule>();
            // Keep track on the list of suspended threads to be resumed.
            List<IThread> resumingThreads = new ArrayList<IThread>();
            List<IThread> suspendedThreads = new ArrayList<IThread>();
            List<String> outOfSyncClasses = new ArrayList<String>(3);
            // Try to find all the modules that contains in the suspended stack frame.  Those are candidate
            // for restarting the applications.
            try {
                threads = jdt.getThreads();
                IModule[] containedModules = server.getModules();
                List<IModule> containedModuleLst = Arrays.asList(containedModules);
                for (IThread curThread : threads) {
                    if (!curThread.isSuspended()) {
                        continue;
                    }
                    suspendedThreads.add(curThread);
                    IStackFrame curStackFrame = curThread.getTopStackFrame();
                    ISourceLocator curSrcLocator = jdt.getLaunch().getSourceLocator();
                    if (curSrcLocator != null) {
                        Object srcElement = curSrcLocator.getSourceElement(curStackFrame);
                        if (srcElement instanceof IFile) {
                            IProject curProj = ((IFile) srcElement).getProject();
                            if (curProj != null) {
                                IModule[] curModules = ServerUtil.getModules(curProj);
                                for (IModule curModule : curModules) {
                                    try {
                                        // Find the root module since the server may only has the parent module, e.g. a WAR in an EAR.
                                        IModule[] rootModules = server.getRootModules(curModule, null);
                                        if (rootModules != null) {
                                            for (IModule curRootModule : rootModules) {
                                                if (containedModuleLst.contains(curRootModule) && !matchedModuleLst.contains(curRootModule)) {
                                                    matchedModuleLst.add(curRootModule);
                                                    if (curStackFrame instanceof IJavaStackFrame) {
                                                        String curStackClassName = ((IJavaStackFrame) curStackFrame).getReceivingTypeName();
                                                        if (curStackClassName != null && !outOfSyncClasses.contains(curStackClassName)) {
                                                            outOfSyncClasses.add(curStackClassName);
                                                        }
                                                    }
                                                    // Add the current thread to the resuming thread list.
                                                    if (!resumingThreads.contains(curThread)) {
                                                        resumingThreads.add(curThread);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (CoreException e) {
                                        if (Trace.ENABLED) {
                                            Trace.trace(Trace.DETAILS, "Failed to find the root module to react to hot code replace failure: server="
                                                                       + server + ", curModule=" + curModule);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Failed to find the containing module that causes hot code replace.", e);
                }
            }
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Hot method replace failed detected: " + server.getName());
            }
            // Prompt for server restart for hot code replace failed.
            PromptHandler promptHandler = Activator.getPromptHandler();
            if (promptHandler != null && !PromptUtil.isSuppressDialog()) {
                // Build the list of matched modules.
                int matchedModuleSize = matchedModuleLst.size();
                String[] matchedModuleNames = new String[matchedModuleSize];
                int index = 0;
                for (IModule curMatchedModule : matchedModuleLst) {
                    matchedModuleNames[index++] = curMatchedModule.getName();
                }

                HotMethodReplaceFailedPrompt prompt = new HotMethodReplaceFailedPrompt(de.getLocalizedMessage(), matchedModuleNames);
                IPromptResponse response = promptHandler.getResponse(Messages.hotCodeReplaceFailedTitle,
                                                                     new PromptHandler.AbstractPrompt[] { prompt },
                                                                     PromptHandler.STYLE_WARN);
                IPromptIssue[] issues = prompt.getIssues();
                if (response != null && response.getSelectedAction(issues[0]) != PromptAction.IGNORE) {
                    if (matchedModuleSize == 0) {
                        // This mean we are restarting the server as no specific module to restart.
                        // We need to resume all threads first.
                        for (IThread curThread : suspendedThreads) {
                            if (curThread.isSuspended() && curThread.canResume()) {
                                try {
                                    curThread.resume();
                                } catch (DebugException e) {
                                    // Do nothing
                                }
                            }
                        }

                        // Workaround the JDT problem that if the hot code replace failed before.
                        // The thread needs to get resumed twice. We'll do another pass to resume
                        // all remaining running thread if there is any.
                        // Wait for any extra suspended threads to show since there is a delay after
                        // the previous resume.
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e1) {
                            // Do nothing.
                        }
                        try {
                            threads = jdt.getThreads();
                            for (IThread curThread : threads) {
                                if (curThread.isSuspended() && curThread.canResume()) {
                                    try {
                                        curThread.resume();
                                    } catch (DebugException e) {
                                        // Do nothing
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Do nothing.
                        }
                    } else {
                        // Resume the suspended threads before restart the applications or server.
                        for (IThread curThread : resumingThreads) {
                            if (curThread.isSuspended() && curThread.canResume()) {
                                try {
                                    curThread.resume();
                                } catch (DebugException e) {
                                    // Do nothing
                                }
                            }
                        }
                    }

                    if (matchedModuleSize > 0) {
                        for (IModule curModule : matchedModuleLst) {
                            // Restart the module
                            try {
                                // Fix the out of sync class state in the debugger.
                                if (jdt instanceof JDIDebugTarget) {
                                    JDIDebugTarget jdiDebug = (JDIDebugTarget) jdt;
                                    jdiDebug.removeOutOfSynchTypes(outOfSyncClasses);
                                    jdiDebug.fireChangeEvent(DebugEvent.STATE);
                                }
                                server.restartModule(new IModule[] { curModule }, null);
                            } catch (Exception e) {
                                if (Trace.ENABLED) {
                                    Trace.trace(Trace.WARNING, "Failed to restart module: " + curModule.getName());
                                }
                            }
                        }
                    } else {
                        // restart the server.
                        server.restart(ILaunchManager.DEBUG_MODE, new IServer.IOperationListener() {
                            @Override
                            public void done(IStatus arg0) {
                                if (Trace.ENABLED) {
                                    Trace.trace(Trace.INFO, "Server has restarted successfully for hot method replace fix.");
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void hotCodeReplaceSucceeded(IJavaDebugTarget jdt) {
            // Do nothing, things working successfully.
        }

        @Override
        public void obsoleteMethods(IJavaDebugTarget jdt) {
            // Do nothing since prompting the user is not useful in this case.
        }
    }

    /**
     * Wait for the server to be started and then initiate an autoConfigSyncJob. With
     * local setup or loose config enabled the user is editing the server configuration
     * directly so no config synchronization is required.
     *
     * @param wsServer The server
     */
    public static void updateServerConfig(WebSphereServer wsServer) {
        if (ServerCore.isAutoPublishing() && !wsServer.isLocalSetup() && !wsServer.getWebSphereServerBehaviour().isLocalUserDir()) {
            Job job = new Job(NLS.bind(com.ibm.ws.st.core.internal.Messages.jobWaitForServerStart, wsServer.getServer().getName())) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    WebSphereServerBehaviour wsBehaviour = wsServer.getWebSphereServerBehaviour();
                    wsBehaviour.waitForServerStart(wsServer.getServer(), monitor);
                    wsBehaviour.startAutoConfigSyncJob();
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }
    }

}
