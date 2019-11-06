/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.ibm.ws.st.core.internal.WebSphereServerBehaviour.ApplicationStateTracker;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.ConsoleStreamsProxy;
import com.ibm.ws.st.core.internal.launch.ExternalProcess;

public class ConsoleMonitorThread extends AbstractMonitorThread {

    private IStreamListener streamListener;
    private IDebugEventSetListener processListener;
    protected final JsonReaderFactory jsonFactory = Json.createReaderFactory(null);
    protected static final String MESSAGE_KEY = "message";

    ConsoleMonitorThread(WebSphereServerBehaviour wsBehaviour, Object serverStateSyncObj, String name) {
        super(wsBehaviour, serverStateSyncObj, name);
    }

    @Override
    public void run() {
        while (!stopMonitor) {
            try {
                int state = server.getServerState();
                WebSphereRuntime wsRuntime = getWebSphereRuntime();
                int serverStatus = -2; // Set to some value not returned by WebSphereRuntime.getServerStatus so can differentiate
                if (wsRuntime != null && wsServer.getUserDirectory() != null) {
                    // getServerStatus returns -1 if timeout; 0 if the server is running; 1 if the server is stopped; 2 if the status is unknown.
                    serverStatus = wsRuntime.getServerStatus(wsServer.getServerInfo(), 20f, new NullProgressMonitor());
                }
                if (serverStatus == 0) {
                    // Server process is running (0 status means running), but we aren't tracking the state yet so need to add the
                    // process listener to listen for console messages
                    if (state != IServer.STATE_STARTED && state != IServer.STATE_STARTING && state != IServer.STATE_STOPPING && !wsBehaviour.isServerCmdStopProcessRunning()) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.SSM, "Detected externally started server. Changing server state to STARTING");

                        synchronized (serverStateSyncObj) {
                            wsBehaviour.setServerStateImpl(IServer.STATE_STARTING);
                            IModule[] modules = server.getModules();
                            for (IModule module : modules)
                                wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STOPPED);
                        }

                        ILaunchConfiguration lc = server.getLaunchConfiguration(true, null);
                        ILaunch launch = new Launch(lc, ILaunchManager.RUN_MODE, null);
                        IPath consoleLog = wsServer.getOutputPath().append("logs").append(Constants.CONSOLE_LOG);
                        boolean isUseConsoleLog = isUseConsoleLogToMonitor(consoleLog, wsServer.getWorkAreaPath().append(".sLock"));
                        IPath consolePath = isUseConsoleLog ? consoleLog : wsServer.getMessagesFile();
                        ConsoleStreamsProxy streamsProxy = new ConsoleStreamsProxy(consolePath.toFile(), isUseConsoleLog, null);
                        IProcess process = new ExternalProcess(launch, server, streamsProxy);
                        process.setAttribute(IProcess.ATTR_PROCESS_LABEL, LaunchUtil.getProcessLabelAttr(server.getName(), wsServer.getServerName()));
                        process.setAttribute(IProcess.ATTR_PROCESS_TYPE, IJavaLaunchConfigurationConstants.ID_JAVA_PROCESS_TYPE);
                        addProcessListeners(process);
                        launch.addProcess(process);

                        DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
                        wsBehaviour.setLaunch(launch);
                        // we know the server is started based on the command line status from calling WebSphereRuntime.isServerStarted()
                        if (Trace.ENABLED)
                            Trace.trace(Trace.SSM, "Changing server state to STARTED");
                        synchronized (serverStateSyncObj) {
                            wsBehaviour.setServerStateImpl(IServer.STATE_STARTED);
                            wsBehaviour.setInternalMode(ILaunchManager.RUN_MODE);
                            try {
                                IServerWorkingCopy serverWorkingCopy = server.createWorkingCopy();
                                // Since the server is already started, we will explicitly turn off the option, stopOnShutdown.
                                // We do not want to stop the server, which has led to unexpected behavior, when the workbench
                                // is shutdown.
                                boolean stopOnShutDown = serverWorkingCopy.getAttribute("stopOnShutdown", true);
                                if (stopOnShutDown) {
                                    serverWorkingCopy.setAttribute(WebSphereServer.PROP_STOP_ON_SHUTDOWN, false);
                                    serverWorkingCopy.save(true, new NullProgressMonitor());
                                }
                            } catch (Exception e) {
                                if (Trace.ENABLED) {
                                    Trace.trace(Trace.SSM, "Attempting to change the state of the option to stop server on shutdown.", e);
                                }
                            }
                        }

                        // detect Debug and Profiling modes
                        try {
                            JMXConnection jmxConnection = wsServer.createJMXConnection();
                            detectAndSetServerMode(jmxConnection);
                        } catch (Exception e) {
                            Trace.logError("Cannot detect mode of externally started server, ensure the server's localConnector feature is configured", e);
                        }
                    }
                } else if (state == IServer.STATE_UNKNOWN || state == IServer.STATE_STOPPING) {
                    if (Trace.ENABLED) {
                        String stateName = state == IServer.STATE_STOPPING ? "STOPPING" : "UNKNOWN";
                        Trace.trace(Trace.SSM, "Server process is stopped but server state is " + stateName + ". Changing to STOPPED...");
                    }
                    synchronized (serverStateSyncObj) {
                        wsBehaviour.stopImpl();
                    }
                } else if (state == IServer.STATE_STARTED) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.SSM, "Server state is started but the status is: " + serverStatus);
                    ILaunch launch = server.getLaunch();
                    // serverStatus value: -1 if timeout; 0 if the server is running; 1 if the server is stopped; 2 if the status is unknown.
                    if (serverStatus == -1) {
                        // If there was a timeout getting the status and the process is still running, loop around.
                        // Otherwise terminate the launch.
                        if (launch != null && !launch.isTerminated()) {
                            IProcess[] processes = launch.getProcesses();
                            for (IProcess process : processes) {
                                if (process.isTerminated()) {
                                    if (Trace.ENABLED)
                                        Trace.trace(Trace.SSM, "Terminating launch since the process is stopped: " + serverStatus);
                                    launch.terminate();
                                    break;
                                }
                            }
                        }
                    } else {
                        // The server is not running so terminate the launch which will update the state to stopped
                        if (Trace.ENABLED)
                            Trace.trace(Trace.SSM, "Server process is stopped, terminate the launch");
                        if (launch != null)
                            launch.terminate();
                    }
                }
            } catch (Throwable t) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Unable to verify server status", t);
            }
            try {
                Thread.sleep(POLLING_DELAY);
            } catch (InterruptedException e) {
                // ignore
            }
        }

    } // end of run

    public void addProcessListeners(final IProcess newProcess) {
        if (processListener != null || newProcess == null)
            return;

        // [AUDIT   ] CWWKF0011I: The server is ready to run a smarter planet.
        // [AUDIT   ] CWWKZ0001I: Application DemoWeb started in 0.94 seconds.
        // [AUDIT   ] CWWKZ0009I: The application DemoWeb has stopped successfully.
        // [AUDIT   ] CWWKZ0003I: The application DemoWeb updated in 0.2 seconds.
        //
        // APPLICATION_START_FAILED=CWWKZ0002E\: An exception occurred while starting the application {0}. The exception message was\: {1}
        // NO_APPLICATION_HANDLER=CWWKZ0005E\: The application {0} cannot start because the server is not configured to handle applications of type {1}.
        // ** We can't use the normal lookup for CWWKZ0005E because {1} is before {0} in some translation so we try to find the app name by matching the
        //    substitution with names of projects in the workspace.
        //
        // APPLICATION_STOP_FAILED=CWWKZ0010E\: An exception occurred while stopping the application {0}. The exception message was\: {1}
        // APPLICATION_NOT_STARTED=CWWKZ0012I\: The application {0} was not started.
        // [AUDIT   ] CWWKZ0012I: The application OSGi.app was not started.
        // DUPLICATE_APPLICATION_NAME=CWWKZ0013E\: It is not possible to start two applications called {0}.
        // PARTIAL_START=CWWKZ0019I\: Application {0} partly started in {1} seconds.
        // ** We can't use CWWKZ0019I because {1} is before {0} in some translation.  Also, I think it isn't used at this point.
        //
        // APPLICATION_UPDATE_FAILED=CWWKZ0004E\: An exception occurred while starting the application {0}. The exception message was\: {1}
        // APPLICATION_NOT_UPDATED=CWWKZ0020I\: Application {0} not updated.
        //
        // APPLICATION_NOT_FOUND=CWWKZ0014W\: The application {0} could not be started as it could not be found at location {1}.
        // [WARNING ] CWWKZ0014W: The application Web could not be started as it could not be found at location Web.war.
        //
        // JCA messages:
        //    J2CA7001.adapter.install.successful=J2CA7001I\: Resource adapter {0} installed in {1} seconds.
        //    ** We can't use the normal lookup for J2CA7001I because {1} is before {0} in some translation so we try to find the app name by matching the
        //       substitution with names of projects in the workspace.
        //    J2CA7002.adapter.install.failed=J2CA7002E\: An exception occurred while installing the resource adapter {0}. The exception message is\: {1}
        //    J2CA7009.adapter.uninstalled=J2CA7009I\: The resource adapter {0} has uninstalled successfully.
        //    J2CA7010.adapter.uninstall.failed=J2CA7010E\: An exception occurred while uninstalling the resource adapter {0}. The exception message is\: {1}
        //    J2CA7012.adapter.not.installed=J2CA7012I\: The resource adapter {0} was not installed.
        //    J2CA7013.duplicate.adapter.name=J2CA7013E\: It is not possible to install multiple resource adapters or other artifacts called {0}.
        //    J2CA7014.adapter.not.found=J2CA7014W\: The resource adapter {0} could not be installed as it could not be found at location {1}.
        //    J2CA7020.adapter.not.updated=J2CA7020I\: Resource adapter {0} is not updated.

        streamListener = new IStreamListener() {
            @Override
            public void streamAppended(String text, IStreamMonitor monitor) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.SSM, "Stream appended: " + text);
                StringTokenizer st = new StringTokenizer(text, "\r\n");
                Activator actr = Activator.getInstance();
                Properties serverMessageReplacementKey = null;
                if (actr != null) {
                    // In JUnit running on CCB build, I see actr be null when the test exists and it causes NPE
                    // We can revisit why it is still being call after shut down.
                    serverMessageReplacementKey = actr.getServerMessageReplacementKey();
                }
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();

                    if (s.startsWith("{")) {
                        String message = parseJsonMessage(s);
                        if (message != null) {
                            s = message;
                        }
                    }

                    if (serverMessageReplacementKey != null && s.length() > 22) {
                        String tempS = s.substring(11, 21);
                        tempS = serverMessageReplacementKey.getProperty(tempS);
                        if (tempS != null) {
                            StringBuilder sb = new StringBuilder(s);
                            sb.delete(11, 21);
                            sb.insert(11, tempS);
                            s = sb.toString();
                        }
                    }

                    if (s.contains("CWWKF0011I")) {
                        synchronized (serverStateSyncObj) {
                            wsBehaviour.setServerStateImpl(IServer.STATE_STARTED);
                        }
                    } else if (s.contains("CWWKZ0001I") || s.contains("CWWKZ0013E") || s.contains("J2CA7001I") || s.contains("J2CA7013E")) {
                        String appName = s.contains("J2CA7001I") ? RuntimeMessageHelper.matchAppNameFromWorkspaceProjects(s, server) : RuntimeMessageHelper.getAppName(s);
                        wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.STARTED);
                        IModule[] modules = server.getModules();
                        boolean externalModuleAdded = true;
                        synchronized (serverStateSyncObj) {
                            for (IModule module : modules) {
                                if (module.getName().equals(appName)) {
                                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STARTED);
                                    externalModuleAdded = false;
                                    break;
                                }
                            }
                        }

                        if (externalModuleAdded) {
                            // If projects were used to look up the name and it is an external app then
                            // the name will likely be null.
                            Set<String> potentialNames = null;
                            if (appName == null)
                                potentialNames = RuntimeMessageHelper.getAllSubstitutionText(s, server);
                            wsBehaviour.syncExternalModules();
                            modules = server.getModules();
                            synchronized (serverStateSyncObj) {
                                for (IModule module : modules) {
                                    String moduleName = module.getName();
                                    if ((potentialNames != null && potentialNames.contains(moduleName)) || moduleName.equals(appName)) {
                                        wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STARTED);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (s.contains("CWWKZ0009I") || s.contains("J2CA7009I")) {
                        String appName = RuntimeMessageHelper.getAppName(s);
                        wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.STOPPED);
                        IModule[] modules = server.getModules();
                        synchronized (serverStateSyncObj) {
                            for (IModule module : modules) {
                                if (module.getName().equals(appName)) {
                                    if (module.isExternal())
                                        wsBehaviour.syncExternalModules();
                                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STOPPED);
                                    // We cannot break here because it is possible that internal and external module have the same name.
                                    // If we have an external app running and we override it with an internal one, before the publishModules(),
                                    // both apps are on the server.  We should set both.
                                }
                            }
                        }
                    } else if (s.contains("CWWKZ0003I") || s.contains("CWWKZ0062I")) {
                        String appName = RuntimeMessageHelper.getAppName(s);
                        wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.UPDATED);
                        IModule[] modules = server.getModules();
                        synchronized (serverStateSyncObj) {
                            for (IModule module : modules) {
                                if (module.getName().equals(appName))
                                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STARTED);
                            }
                        }
                        // below messages are for publish to exit when there is a failure.
                    } else if (s.contains("CWWKZ0002E") || s.contains("J2CA7002E")) { // fail start
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.getAppName(s), ApplicationStateTracker.FAILED_START);
                    } else if (s.contains("CWWKZ0012I") || s.contains("J2CA7012I")) {
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.getAppName(s), ApplicationStateTracker.FAILED_START);
                    }
                    // Special case since the {0} and {1} can be reversed in some languages so we use a different method that matches
                    // the substitutions with names of projects in the workspace.
                    else if (s.contains("CWWKZ0005E")) {
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.matchAppNameFromWorkspaceProjects(s, server),
                                                                        ApplicationStateTracker.FAILED_START);
                    } else if (s.contains("CWWKZ0004E")) { //fail update
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.getAppName(s), ApplicationStateTracker.FAILED_UPDATE);
                    } else if (s.contains("CWWKZ0020I") || s.contains("J2CA7020I")) {
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.getAppName(s), ApplicationStateTracker.FAILED_UPDATE);
                    } else if (s.contains("CWWKZ0010E") || s.contains("J2CA7010E")) { //fail stop
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.getAppName(s), ApplicationStateTracker.FAILED_STOP);
                        // config is picked up first, need to use JMX to start the app etc
                    } else if (s.contains("CWWKZ0014W") || s.contains("J2CA7014W")) {
                        wsBehaviour.appStateTracker.addApplicationState(RuntimeMessageHelper.getAppName(s), ApplicationStateTracker.NEED_RESTART_APP);
                    }
                }
            }

            private String parseJsonMessage(String s) {
                try {
                    JsonReader reader = jsonFactory.createReader(new ByteArrayInputStream(s.getBytes()));
                    JsonObject obj = reader.readObject();
                    String message = obj.getString(MESSAGE_KEY);
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Message started with '{' but could not parse as JSON: " + s, e);
                    }
                }
                return null;
            }
        };
        newProcess.getStreamsProxy().getOutputStreamMonitor().addListener(streamListener);
        newProcess.getStreamsProxy().getErrorStreamMonitor().addListener(streamListener);

        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Added stream listener to process: " + newProcess.getLabel());

        processListener = new IDebugEventSetListener() {
            @Override
            public void handleDebugEvents(DebugEvent[] events) {
                if (events != null) {
                    int size = events.length;
                    for (int i = 0; i < size; i++) {
                        if (newProcess.equals(events[i].getSource()) && events[i].getKind() == DebugEvent.TERMINATE) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.INFO, "Server processes stopped");

                            wsBehaviour.waitForServerStop(server.getStopTimeout() * 300); // We want to wait about 3/10 of the stop time out
                            wsBehaviour.stopImpl();
                        }
                    }
                }
            }
        };
        DebugPlugin.getDefault().addDebugEventListener(processListener);

        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Added debug event listener for process: " + newProcess.getLabel());
    }

    public void removeProcessListeners() {
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Removing process listeners");

        if (processListener != null) {
            try {
                DebugPlugin.getDefault().removeDebugEventListener(processListener);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not remove debug event listener", e);
            }
            processListener = null;
        }

        if (streamListener != null) {
            try {
                ILaunch launch = server.getLaunch();
                if (launch != null) {
                    launch.getProcesses()[0].getStreamsProxy().getOutputStreamMonitor().removeListener(streamListener);
                    launch.getProcesses()[0].getStreamsProxy().getErrorStreamMonitor().removeListener(streamListener);
                }
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not remove stream listener", e);
            }
            streamListener = null;
        }
    }

    boolean isUseConsoleLogToMonitor(IPath consoleLog, IPath sLock) {
        if (consoleLog == null || sLock == null)
            return false;
        File consoleFie = consoleLog.toFile();
        if (!consoleFie.exists())
            return false;

        long consoleLogModifyTS = consoleFie.lastModified();
        long sLockTS = sLock.toFile().lastModified();

        // from my observation, the TS of the console.log can be 2.7 seconds before the TS of the .sLock
        if (sLockTS - consoleLogModifyTS < 3200)
            return true;

        return false;
    }

}
