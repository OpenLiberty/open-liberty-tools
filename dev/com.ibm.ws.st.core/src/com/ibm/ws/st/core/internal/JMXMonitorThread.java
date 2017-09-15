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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.ConsoleStreamsProxy;
import com.ibm.ws.st.core.internal.launch.ExternalProcess;
import com.ibm.ws.st.core.internal.launch.WebSphereLaunchConfigurationDelegate;

/**
 * JMX server state monitor
 */
public class JMXMonitorThread extends AbstractMonitorThread {

    private JMXConnection jmxConnection;
    private boolean resetJMXConnection = false;
    Map<ApplicationNotificationListener, Boolean> appListenersMap = new ConcurrentHashMap<ApplicationNotificationListener, Boolean>();

    /**
     * @param wsBehaviour Server behaviour object
     * @param serverStateSyncObj Server state lock
     * @param name Thread name
     */
    public JMXMonitorThread(WebSphereServerBehaviour wsBehaviour, Object serverStateSyncObj, String name) {
        super(wsBehaviour, serverStateSyncObj, name);
        initRegistrations();
    }

    /**
     * Initialize the application listeners map. This map should contain application listeners for each application we want to monitor the state for.
     */
    private void initRegistrations() {
        IModule[] modules = wsBehaviour.getServer().getModules();
        synchronized (appListenersMap) {
            for (IModule module : modules) {
                appListenersMap.put(new ApplicationNotificationListener(module.getName(), wsBehaviour), Boolean.FALSE);
            }
        }
    }

    @Override
    public void run() {

        while (!stopMonitor) {
            if (resetJMXConnection) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.SSM, "Resetting JMX connection");
                if (jmxConnection != null) {
                    try {
                        jmxConnection.disconnect();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                jmxConnection = null;
                resetJMXConnection = false;
            }

            // create the JMX connection if it does not exist
            if (jmxConnection == null) {
                try {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.SSM, "Creating JMX connection");
                    }
                    jmxConnection = wsServer.createJMXConnection();
                    resetRegistrations();
                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.SSM, "Could not create JMX Connection: " + e.getLocalizedMessage());
                    }
                    synchronized (serverStateSyncObj) {
                        int serverState = server.getServerState();
                        if (serverState != IServer.STATE_STOPPED && serverState != IServer.STATE_STARTING) {
                            if (Trace.ENABLED) {
                                Trace.trace(Trace.SSM, "Changing server state to STOPPED");
                            }
                            stoppedServerCleanup();
                        }
                    }
                }
            }

            if (jmxConnection != null) {
                // attempt connection to set server state
                try {
                    try {
                        // Try to register any app listeners that haven't already been registered.
                        // Also serves as the connection test.
                        connectAndRegisterAppListeners();
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.SSM, "Reusing existing JMX connection");
                        }
                    } catch (Exception e) {
                        wsBehaviour.clearRemoteConfigVarMap();
                        // need to connect
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.SSM, "Attempting reconnect");
                        }
                        unregisterAppListeners();
                        jmxConnection.disconnect();
                        jmxConnection.connect();
                    }

                    String launchMode = server.getMode();
                    int serverState = server.getServerState();
                    // if stop is invoked on the server the server will be in STOPPING state but
                    // the remote server's JMX connection may still be alive, so we should not
                    // switch the server state to STARTED in this case
                    if (serverState != IServer.STATE_STOPPING && serverState != IServer.STATE_STARTED) {

                        synchronized (serverStateSyncObj) { // synchronize with server state lock for any blocks of code that use/set server state
                            if (Trace.ENABLED)
                                Trace.trace(Trace.SSM, "Changing server state to STARTED");
                            wsBehaviour.setServerStateImpl(IServer.STATE_STARTED);
                            if (!ILaunchManager.RUN_MODE.equals(launchMode))
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

                        JMXConnection jmx = wsServer.createJMXConnection();
                        CompositeData metadata = (CompositeData) jmx.getMetadata(Constants.SERVER_CONFIG_VAR, "a");
                        String serverConfigDir = (String) metadata.get("fileName");
                        serverConfigDir = serverConfigDir.replace("\\", "/");

                        String serverName = serverConfigDir.substring(serverConfigDir.lastIndexOf('/') + 1, serverConfigDir.length());
                        IPath remoteUsrMetadataPath = wsServer.getWebSphereRuntime().getRemoteUsrMetadataPath().append(Constants.SERVERS_FOLDER).append(serverName);

                        // need to set up launch processes (ie. ProcessConsole, etc.) so that we get the server console output
                        ILaunch launch = server.getLaunch();
                        if (launch == null) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.SSM, "JMX MONITOR THREAD -> LAUNCHIT");
                            WebSphereLaunchConfigurationDelegate.launchIt(launchMode, wsBehaviour);
                        }

                        int timeout = 0;
                        int MAX = 20;
                        while (launch == null && timeout++ < MAX) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.SSM, "Waiting for launch");
                            launch = server.getLaunch();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e2) {
                                timeout = MAX;
                            }
                        }

                        if (launch != null) {
                            IPath consoleLog = remoteUsrMetadataPath.append("logs").append(Constants.CONSOLE_LOG);
                            boolean isUseConsoleLog = isUseConsoleLogToMonitor(jmx);
                            ConsoleStreamsProxy streamsProxy = new ConsoleStreamsProxy(consoleLog.toFile(), isUseConsoleLog, jmx);
                            IProcess curProcess = new ExternalProcess(launch, server, streamsProxy);
                            curProcess.setAttribute(IProcess.ATTR_PROCESS_LABEL, LaunchUtil.getProcessLabelAttr(server.getName(), wsServer.getServerName()));
                            curProcess.setAttribute(IProcess.ATTR_PROCESS_TYPE, IJavaLaunchConfigurationConstants.ID_JAVA_PROCESS_TYPE);
                            launch.addProcess(curProcess);

                            DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
                            wsBehaviour.setLaunch(launch);
                        } else {
                            // we should never get here since launchIt should be called to ensure we get a launch, but if we do log it
                            Trace.logError("Failed to create server launch.", null);
                        }
                    }

                    detectAndSetServerMode(jmxConnection);

                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.SSM, "Could not refresh JMX Connection: " + e);
                    }
                    synchronized (serverStateSyncObj) {
                        int serverState = server.getServerState();
                        String launchMode = server.getMode();
                        // similar to stop behaviour, if start is invoked on the server the server will be in
                        // STARTING state but the remote server's JMX connection may not be up yet, so we
                        // should not switch the server state to STARTED in this case
                        if (serverState != IServer.STATE_STARTING && serverState != IServer.STATE_STOPPED) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.SSM, "Changing server state to STOPPED");
                            stoppedServerCleanup();
                            //reset the server mode so debugger can be attached once remote server is started in debug mode
                            if (launchMode != ILaunchManager.RUN_MODE)
                                wsBehaviour.setInternalMode(ILaunchManager.RUN_MODE);
                        }
                    }
                }
            } else { // JMX connection is null
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SSM, "JMX connection is NULL");
                }
            }

            try {
                Thread.sleep(POLLING_DELAY);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void stoppedServerCleanup() {
        try {
            ILaunch launch = server.getLaunch();
            if (launch != null && launch.canTerminate())
                launch.terminate();
            wsBehaviour.stopImpl();
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.SSM, "Stopped server cleanup encountered problems: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Called during a publish operation, this method will create a new instance of ApplicationNotificationListener using
     * the given {@code appName}. It then waits for the ApplicationMBean to be initialized on the server before attempting to register
     * the application listener and initialize the application's state.
     *
     * @param appName name of the application being published. (Must match the ApplicationMBean name)
     * @param wsBehaviour instance of WebSphereServerBehaviour
     * @param monitor used to cancel the running operation. {@code monitor} should not be null since it is expected to be called from a publish operation. If the monitor is null
     *            the method returns without doing anything.
     */
    void addAppListenerOnPublish(String appName, WebSphereServerBehaviour wsBehaviour, IProgressMonitor monitor) {
        if (appName == null || monitor == null || monitor.isCanceled())
            return;
        synchronized (appListenersMap) {
            ApplicationNotificationListener listener = new ApplicationNotificationListener(appName, wsBehaviour);
            boolean isRegistered = false;
            if (jmxConnection != null) {
                // Keep checking for ApplicationMBean that matches the appName until successful or user cancels
                try {
                    // monitor should not be null when called from a publish operation
                    while (!jmxConnection.getAllApplicationNames().contains(appName) && !monitor.isCanceled()) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.SSM, "Waiting for application MBean to be created...");
                        }
                        Thread.sleep(250);
                    }

                    if (!monitor.isCanceled()) {
                        // We should attempt to register the app listener first so that we don't miss any messages
                        // before we initialize the app state
                        jmxConnection.addAppListener(listener);
                        // If we got this far without exception then the registration was successful
                        isRegistered = true;

                        if (Trace.ENABLED)
                            Trace.trace(Trace.SSM, "Checking application state for " + listener.getAppName());
                        // Initialize the app state on publish since it is possible to miss a notification
                        // if the notification listener is registered after the app has been processed.
                        String state = jmxConnection.getAppState(listener.getAppName());

                        // if the state is null or STOPPED then we should wait upto 5 seconds to
                        long timeout = System.currentTimeMillis() + 5000;

                        while (System.currentTimeMillis() < timeout && (state == null || state.equals(ApplicationNotificationListener.STATE_STOPPED))) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.SSM, "Waiting for application state update for " + listener.getAppName());
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                // nothing to do here
                            }
                            state = jmxConnection.getAppState(listener.getAppName());
                        }

                        // during a publish we assume the initial state of the app is starting
                        listener.handleState(ApplicationNotificationListener.STATE_STARTING, state != null ? state : ApplicationNotificationListener.STATE_UNKNOWN);

                        if (Trace.ENABLED) {
                            Trace.trace(Trace.SSM, "Registered listener for application: " + appName);
                        }
                    }
                } catch (Exception e) {
                    Trace.logError("Couldn't register application notification listener for application: " + listener.getAppName(), e);
                }
            }
            appListenersMap.put(listener, Boolean.valueOf(isRegistered));
        }
    }

    /**
     * Removes the listener from the registered list of listeners.
     *
     * @param appName
     */
    void removeAppListener(String appName) {
        if (appName == null)
            return;
        synchronized (appListenersMap) {
            boolean found = false;
            Set<ApplicationNotificationListener> listeners = appListenersMap.keySet();
            Iterator<ApplicationNotificationListener> itr = listeners.iterator();
            while (!found && itr.hasNext()) {
                ApplicationNotificationListener listener = itr.next();
                if (listener.getAppName().equals(appName)) {
                    found = true;
                    try {
                        jmxConnection.removeAppListener(listener);
                    } catch (Exception e) {
                        // Since the application may have already been stopped, the mbean might not be available so this
                        // is just a warning and not an error and should only be output if trace is enabled.
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Couldn't unregister application notification listener for app: " + listener.getAppName()
                                                       + ".  The application may have already been stopped by the runtime.",
                                        e);
                        }
                    }
                    appListenersMap.remove(listener);
                }
            }
        }
    }

    /**
     * Iterates through the list of application listeners and registers each one if a corresponding ApplicationMBean exists.
     *
     * @throws Exception
     */
    private void connectAndRegisterAppListeners() throws Exception {
        // try to get all application names as a connection test or throw exception if not possible
        List<String> apps = jmxConnection.getAllApplicationNames();
        synchronized (appListenersMap) {
            for (ApplicationNotificationListener listener : appListenersMap.keySet()) {
                try {
                    // if the app listener hasn't been registered and there exists an ApplicationMBean instance with the same name then try to register it
                    if (appListenersMap.get(listener).equals(Boolean.FALSE)) {
                        if (apps.contains(listener.getAppName())) {
                            jmxConnection.addAppListener(listener);
                            // if we got here without exception then the registration was successful
                            appListenersMap.put(listener, Boolean.TRUE);

                            // get the app state and handle it
                            String state = jmxConnection.getAppState(listener.getAppName());
                            if (state != null) {
                                listener.handleState(ApplicationNotificationListener.STATE_UNKNOWN, state);
                            }
                        } else {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.SSM, "Failed to register application listener because no ApplicationMBean instance found for app: " + listener.getAppName());
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.SSM, "Failed to register application listener for app: " + listener.getAppName(), e);
                }
            }
        }
    }

    /**
     * Resets the tracking map so that all tracked listeners will be registered again the next time the list is checked.
     */
    private void resetRegistrations() {
        synchronized (appListenersMap) {
            for (ApplicationNotificationListener listener : appListenersMap.keySet()) {
                appListenersMap.put(listener, Boolean.FALSE);
            }
        }
    }

    private void unregisterAppListeners() {
        for (ApplicationNotificationListener listener : appListenersMap.keySet()) {
            try {
                jmxConnection.removeAppListener(listener);
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.SSM, "Failed to unregister application listener for app: " + listener.getAppName());
                }
            }
        }
        resetRegistrations();
    }

    /**
     * Resets the JMX connection.
     */
    public void resetJMX() {
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Reset JMX connection");
        resetJMXConnection = true;
    }

    /**
     * Determines whether to use the console.log or messages.log file.
     *
     * @param jmxConnection an established JMX connection
     * @return true if we should use the console log file, false otherwise
     */
    private boolean isUseConsoleLogToMonitor(JMXConnection jmxConnection) {
        if (jmxConnection == null)
            return false;
        try {
            CompositeData metadata = (CompositeData) jmxConnection.getMetadata(Constants.LOGGING_DIR_VAR + "/" + Constants.CONSOLE_LOG, "t");

            if (metadata == null)
                return false;
            Date consoleLastModified = (Date) metadata.get("lastModified");
            metadata = (CompositeData) jmxConnection.getMetadata("${server.output.dir}/workarea/.sLock", "t");
            Date sLockLastModified = (Date) metadata.get("lastModified");
            // from my observation, the TS of the console.log can be 2.7 seconds before the TS of the .sLock
            if (sLockLastModified.getTime() - consoleLastModified.getTime() < 3200)
                return true;
        } catch (Exception e) {
            Trace.logError("Cannot determine log file status", e);
        }
        return false;
    }
}
