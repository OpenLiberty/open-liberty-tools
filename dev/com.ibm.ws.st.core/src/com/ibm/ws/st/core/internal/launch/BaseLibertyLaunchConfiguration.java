/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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
import java.io.InterruptedIOException;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jst.server.core.ServerProfilerDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractLaunchConfigurationExtension;
import com.ibm.ws.st.core.AbstractLibertyDebugFactory;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.LaunchUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

/**
 * Launch configuration for WebSphere server.
 */
// We need to use raw types for the Map.  Otherwise, build for some platforms will fail.
// Liberty RTC 64292
// The methods were copied from com.ibm.ws.st.core.internal.launch.WebSphereLaunchConfigurationDelegate.
// Examine that class to see the history
@SuppressWarnings({ "rawtypes", "restriction" })
public class BaseLibertyLaunchConfiguration extends AbstractLaunchConfigurationExtension {

    protected static final String VM_ERROR_PAGE = "-Dwas4d.error.page=localhost:";

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        final IServer server = ServerUtil.getServer(configuration);
        if (server == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Launch configuration could not find server");
            // throw CoreException();
            return;
        }

        WebSphereServer websphereServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        WebSphereServerBehaviour websphereServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);

        IRuntime runtime = server.getRuntime();
        if (runtime == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "The runtime is null");
            return;
        }
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (websphereServer == null || websphereServerBehaviour == null || wsRuntime == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Launch configuration could not find WebSphere server");
            // throw CoreException();
            return;
        }

        if (monitor.isCanceled())
            return;

        IStatus status2 = websphereServer.validate();
        if (status2 != null && !status2.isOK())
            throw new CoreException(status2);

        setDefaultSourceLocator(launch, configuration);
        websphereServerBehaviour.setLaunch(launch);

        if (monitor.isCanceled())
            return;

        try {
            // local server start implementation
            if (websphereServer.isLocalSetup()) {
                websphereServerBehaviour.preLaunch(launch, mode, monitor);
                launchLocal(configuration, mode, launch, monitor, websphereServerBehaviour);
            }
            // remote server start implementation
            else {
                launchRemote(configuration, mode, launch, monitor, websphereServerBehaviour);
            }
        } catch (CoreException ce) {
            Trace.logError("Failed to launch process", ce);
            websphereServerBehaviour.stopImpl();
            throw ce;
        } catch (Exception e) {
            Trace.logError("Failed to launch process", e);
            websphereServerBehaviour.stopImpl();
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
        }
    }

    /**
     * Sets up a server that is already started prior to calling this method.
     *
     * @param mode
     * @param launch
     * @param serverBehaviour
     * @throws CoreException
     */
    @Override
    public void launchStartedServer(String launchMode, ServerBehaviourDelegate serverBehaviour) throws CoreException {
        IServer server = serverBehaviour.getServer();
        WebSphereServerBehaviour websphereServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());
        if (websphereServerBehaviour == null) {
            // TODO: add logging
            return;
        }

        WebSphereServer websphereServer = websphereServerBehaviour.getWebSphereServer();

        ILaunch launch = server.getLaunch();
        if (launch == null && server.getServerState() == IServer.STATE_STARTED) {
            // if there's no launch, issue server start to create the launch and set up the source computer, etc.
            if (Trace.ENABLED)
                Trace.trace(Trace.SSM, "STARTING SERVER FROM LAUNCHIT");
            server.start(launchMode, new NullProgressMonitor());
        }

        boolean isDebugMode = ILaunchManager.DEBUG_MODE.equals(launchMode);

        try {
            int remoteDebugPort = -1;

            JMXConnection jmxConnection = null;
            try {
                jmxConnection = websphereServer.createJMXConnection();
                remoteDebugPort = jmxConnection.getDebugPortNum();
            } catch (Exception e) {
                //this means the remote server is not started so get the port number from server object
                remoteDebugPort = Integer.parseInt(websphereServer.getRemoteServerStartDebugPort());
            }

            // remote debug case, connect debug client
            if (remoteDebugPort > -1 && isDebugMode && launch != null) {
                // attach debugger
                if (!websphereServerBehaviour.isDebugAttached(remoteDebugPort, server)) {
                    final IDebugTarget oldDebugTarget = websphereServerBehaviour.getDebugTarget();
                    LaunchUtilities.connectRemoteDebugClient(launch, remoteDebugPort, websphereServer);
                    if (oldDebugTarget != null && !websphereServerBehaviour.getDebugTarget().isDisconnected()
                        && !websphereServerBehaviour.getDebugTarget().isTerminated()) {
                        // Clean up the old one.
                        oldDebugTarget.disconnect();
                    }
                } else {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Skipping the debug attach since the debug process is already attached");
                    // delete launch since a debugger is already attached
                    try {
                        DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
                    } catch (Exception e2) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Cannot terminate the failed debug launch process.", e2);
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not attach debugger.", e);
        }

        // In case the server configuration changed while the server was stopped
        LaunchUtilities.updateServerConfig(websphereServer);
    }

    private void launchRemote(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor,
                              WebSphereServerBehaviour websphereServerBehaviour) throws CoreException {
        WebSphereServer websphereServer = websphereServerBehaviour.getWebSphereServer();

        // The remote server may actually be started but if the Server State Monitor (SSM) thread is
        // stopped we aren't detecting the state, then we just need to start up the thread again.
        websphereServerBehaviour.ensureMonitorRunning();

        JMXConnection jmxConnection = null;
        try {
            try {
                // Check if the remote server is actually up and running.
                jmxConnection = websphereServer.createJMXConnection();
                if (Trace.ENABLED)
                    Trace.trace(Trace.SSM, "Server is already started");
            } catch (Exception e) {
                // 1. Server is not started
                // 2. Try to start the server, if remote server start is not enabled it will result in an error dialog
                if (Trace.ENABLED)
                    Trace.trace(Trace.SSM, "Server is not started, attempt to start the server");
                websphereServerBehaviour.start(launch, mode, monitor);
            }

            launchStartedServer(mode, websphereServerBehaviour);
        } finally {
            if (jmxConnection != null) {
                try {
                    jmxConnection.disconnect();
                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Couldn't disconnect JMX connection", e);
                    }
                }
            }
        }
    }

    private void launchLocal(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor,
                             WebSphereServerBehaviour websphereServerBehaviour) throws CoreException, IllegalConnectorArgumentsException, IOException {

        int debugPort = 7777;
        AttachingConnector connector = null;
        Map map = null;
        WebSphereServer websphereServer = websphereServerBehaviour.getWebSphereServer();
        IServer server = websphereServer.getServer();
        WebSphereRuntime wsRuntime = websphereServer.getWebSphereRuntime();
        String serverName = websphereServer.getServerName();

        ProcessBuilder pb = null;
        String scriptMode = "run";
        String runtimeVersion = websphereServer.getWebSphereRuntime().getRuntimeVersion();
        boolean isDebugMode = ILaunchManager.DEBUG_MODE.equals(mode);
        boolean useDebugScript = runtimeVersion != null && !runtimeVersion.startsWith("8.5.0");
        if (isDebugMode && useDebugScript)
            scriptMode = "debug";
        if (websphereServerBehaviour.isCleanOnStartup()) {
            pb = wsRuntime.createProcessBuilder(scriptMode, websphereServer.getServerInfo(), "--clean");
            websphereServerBehaviour.setCleanOnStartup(false);
        } else
            pb = wsRuntime.createProcessBuilder(scriptMode, websphereServer.getServerInfo());

        Map<String, String> env = pb.environment();
        String vmArgs = env.get("JVM_ARGS");

        String addVmArgs = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
        if (addVmArgs != null && addVmArgs.length() > 0) {
            if (vmArgs == null)
                vmArgs = "";
            else
                vmArgs += " ";
            vmArgs += addVmArgs;
        }

        String errorPage = LaunchUtil.getErrorPage();
        if (errorPage != null) {
            if (vmArgs == null)
                vmArgs = "";
            else
                vmArgs += " ";
            vmArgs += VM_ERROR_PAGE + errorPage;
        }

        if (isDebugMode) {
            // if in debug mode, don't use -Xquickstart to avoid IBM JVM warning
            if (vmArgs != null && vmArgs.contains(LaunchUtil.VM_QUICKSTART))
                vmArgs = vmArgs.replace(LaunchUtil.VM_QUICKSTART, "");

            // find a free port and create the connector
            debugPort = LaunchUtilities.findFreePort();
            if (debugPort == -1)
                abort("Could not find a free socket", null, 0);

            connector = LaunchUtilities.getAttachingConnector();
            if (connector == null) {
                abort("Could not find an appropriate debug connector", null, IJavaLaunchConfigurationConstants.ERR_CONNECTOR_NOT_AVAILABLE);
                return;
            }
            map = connector.defaultArguments();
            LaunchUtilities.configureConnector(map, server.getHost(), debugPort);
            env.put("WLP_DEBUG_ADDRESS", debugPort + "");

            // configure vm arguments
            if (!useDebugScript) {
                if (vmArgs == null)
                    vmArgs = "";
                else
                    vmArgs += " ";
                vmArgs += " -Dwas.debug.mode=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:" + debugPort;
            }
        } else if (ILaunchManager.PROFILE_MODE.equals(mode)) {
            IVMInstall vmInstall = wsRuntime.getVMInstall();
            VMRunnerConfiguration runConfig = new VMRunnerConfiguration("n/a", new String[0]);

            runConfig.setVMArguments(DebugPlugin.parseArguments(vmArgs));
            try {
                ServerProfilerDelegate.configureProfiling(launch, vmInstall, runConfig, monitor);
                String[] newVmArgs = runConfig.getVMArguments();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < newVmArgs.length; i++) {
                    if (i > 0)
                        sb.append(" ");
                    String s = newVmArgs[i];
                    if (!s.contains("\""))
                        sb.append("\"" + s + "\"");
                    else
                        sb.append(s);
                }
                vmArgs = sb.toString();
            } catch (CoreException ce) {
                websphereServerBehaviour.stopImpl();
                throw ce;
            }
        }

        // get vm args provided by server pre-start extensions
        ServerStartInfo startInfo = new ServerStartInfo(server, mode);
        vmArgs = LaunchUtilities.processVMArguments(vmArgs, startInfo);

        if (vmArgs != null)
            env.put("JVM_ARGS", vmArgs);

        Process p = pb.start();

        String cmd = pb.command().get(0) + " " + pb.command().get(1);
        IProcess process = LaunchUtilities.newProcess(server, launch, p, LaunchUtil.getProcessLabel(cmd));
        process.setAttribute(IProcess.ATTR_CMDLINE, UtilityLaunchConfigurationDelegate.renderCmdLine(pb));
        process.setAttribute(IProcess.ATTR_PROCESS_LABEL, LaunchUtil.getProcessLabelAttr(wsRuntime.getRuntime().getName(), serverName));
        launch.addProcess(process);
        websphereServerBehaviour.addProcessListeners(process);

        if (isDebugMode) {

            //check to see if there is any extended debugger factory available
            AbstractLibertyDebugFactory df = LaunchUtilities.getDebugTargetFactory();
            IDebugTarget curDebugTarget = null;
            if (df != null) {
                String debugTargetLabel = NLS.bind(Messages.debugTargetLabel, "localhost", debugPort + "");
                curDebugTarget = df.createDebugTarget(launch, "localhost", Integer.toString(debugPort), debugTargetLabel, null);
            }

            //if there is no debugger factory, or if it returned null, fall back to the JDT debugger
            if (curDebugTarget == null) {
                curDebugTarget = connectAndWait(launch, debugPort, connector, map, p, process, server.getStartTimeout(), monitor);
            }

            if (curDebugTarget != null) {
                // Add hot code replace listener to listen for hot code replace failure.
                IJavaDebugTarget jdt = curDebugTarget.getAdapter(IJavaDebugTarget.class);
                if (jdt != null) {
                    jdt.addHotCodeReplaceListener(new LaunchUtilities.WebSphereJavaHotCodeReplaceListener(server));
                }
            }
        }

    }

    /**
     * @param launch
     * @param port
     * @param connector
     * @param map
     * @param p
     * @param process
     * @param timeout
     * @param monitor
     * @throws IllegalConnectorArgumentsException
     * @throws CoreException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private IDebugTarget connectAndWait(ILaunch launch, int port, AttachingConnector connector, Map map, Process p, IProcess process, int timeout,
                                        IProgressMonitor monitor) throws IllegalConnectorArgumentsException, CoreException, IOException {
        boolean retry = false;
        do {
            try {
                VirtualMachine vm = null;
                Exception ex = null;
                int itr = timeout * 4; // We want to check 4 times per second

                if (itr <= 0)
                    itr = 2;

                while (itr-- > 0) {
                    if (monitor.isCanceled()) {
                        p.destroy();
                        return null;
                    }
                    try {
                        vm = connector.attach(map);
                        itr = 0;
                        ex = null;
                    } catch (Exception e) {
                        ex = e;
                        if (Trace.ENABLED_DETAILS) {
                            if (itr % 8 == 0)
                                Trace.trace(Trace.DETAILS, "Waiting for debugger attach.");
                        }
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e1) {
                            // do nothing
                        }
                    }
                }

                try {
                    p.exitValue();
                    checkErrorMessage(process);
                } catch (IllegalThreadStateException e) {
                    // expected while process is alive
                }

                if (ex instanceof IllegalConnectorArgumentsException)
                    throw (IllegalConnectorArgumentsException) ex;

                if (ex instanceof InterruptedIOException)
                    throw (InterruptedIOException) ex;

                if (ex instanceof IOException)
                    throw (IOException) ex;

                IDebugTarget debugTarget = null;
                if (vm != null) {
                    setDebugTimeout(vm);
                    debugTarget = createLocalJDTDebugTarget(launch, port, process, vm);
                    monitor.worked(1);
                    monitor.done();
                }
                return debugTarget;
            } catch (InterruptedIOException e) {
                checkErrorMessage(process);

                // timeout, consult status handler if there is one
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IJavaLaunchConfigurationConstants.ERR_VM_CONNECT_TIMEOUT, "", e);
                IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);

                retry = false;
                if (handler == null)
                    // if there is no handler, throw the exception
                    throw new CoreException(status);

                Object result = handler.handleStatus(status, this);
                if (result instanceof Boolean) {
                    retry = ((Boolean) result).booleanValue();
                }
            }
        } while (retry);
        return null;
    }

    /**
     * Creates a new debug target for the given virtual machine and system process
     * that is connected on the specified port for the given launch.
     *
     * @param launch launch to add the target to
     * @param port port the VM is connected to
     * @param process associated system process
     * @param vm JDI virtual machine
     * @return the {@link IDebugTarget}
     */
    private IDebugTarget createLocalJDTDebugTarget(ILaunch launch, int port, IProcess process, VirtualMachine vm) {
        String name = NLS.bind(Messages.debugTargetLabel, "localhost", port + "");

        return JDIDebugModel.newDebugTarget(launch, vm, name, process, true, false, true);
    }

    /**
     * Checks and forwards an error from the specified process
     *
     * @param process the process to get the error message from
     * @throws CoreException if a problem occurs
     */
    private void checkErrorMessage(IProcess process) throws CoreException {
        IStreamsProxy streamsProxy = process.getStreamsProxy();
        if (streamsProxy != null) {
            String errorMessage = streamsProxy.getErrorStreamMonitor().getContents();
            if (errorMessage.length() == 0)
                errorMessage = streamsProxy.getOutputStreamMonitor().getContents();
            if (errorMessage.length() != 0)
                abort(errorMessage, null, IJavaLaunchConfigurationConstants.ERR_VM_LAUNCH_ERROR);
        }
    }

    /**
     * Set the debug request timeout of a vm in ms, if supported by the vm implementation.
     */
    private static void setDebugTimeout(VirtualMachine vm) {
        IEclipsePreferences node = InstanceScope.INSTANCE.getNode(JDIDebugPlugin.getUniqueIdentifier());
        int timeOut = node.getInt(JDIDebugModel.PREF_REQUEST_TIMEOUT, 0);
        if (timeOut <= 0)
            return;
        if (vm instanceof org.eclipse.jdi.VirtualMachine) {
            org.eclipse.jdi.VirtualMachine vm2 = (org.eclipse.jdi.VirtualMachine) vm;
            vm2.setRequestTimeout(timeOut);
        }
    }

}
