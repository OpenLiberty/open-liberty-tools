/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.Arrays;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.AbstractServerStartupExtension;
import com.ibm.ws.st.core.internal.launch.ServerStartInfo;
import com.ibm.ws.st.core.internal.launch.WebSphereLaunchConfigurationDelegate;

/**
 *
 */
public abstract class AbstractMonitorThread extends Thread {

    public static final String SERVER_STATUS_POLLING_DELAY_PROPERTY = "com.ibm.ws.st.serverStatusPollingDelaySeconds";

    protected static int POLLING_DELAY = 3500;
    protected boolean stopMonitor = false;

    protected WebSphereServerBehaviour wsBehaviour;
    protected IServer server;
    protected WebSphereServer wsServer;
    protected Object serverStateSyncObj;

    static {
        String pollingDelay = System.getProperty(SERVER_STATUS_POLLING_DELAY_PROPERTY);
        if (pollingDelay != null && !pollingDelay.isEmpty()) {
            try {
                POLLING_DELAY = Integer.parseInt(pollingDelay) * 1000;
            } catch (NumberFormatException e) {
                Trace.logError("The server status polling delay specified is not valid: " + pollingDelay + ". The default will be used: " + POLLING_DELAY, e);
            }
        }
    }

    public AbstractMonitorThread(WebSphereServerBehaviour wsBehaviour, Object serverStateSyncObj, String name) {
        super(name);
        this.wsBehaviour = wsBehaviour;
        this.server = wsBehaviour.getServer();
        this.wsServer = wsBehaviour.getWebSphereServer();
        this.serverStateSyncObj = serverStateSyncObj;
    }

    public void stopMonitor() {
        stopMonitor = true;
    }

    protected WebSphereRuntime getWebSphereRuntime() {
        return wsBehaviour.getWebSphereRuntime();
    }

    public boolean isRunning() {
        return !stopMonitor;
    }

    protected boolean isProfileMode(String[] vmArgs) throws Exception {
        if (vmArgs == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Profile mode cannot be detected because no vmArgs were found");
            return false;
        }

        AbstractServerStartupExtension[] preStartExtensions = Activator.getInstance().getPreStartExtensions();

        for (AbstractServerStartupExtension startup : preStartExtensions) {
            Boolean isProfiling = startup.isProfiling(new ServerStartInfo(server, server.getMode()), Arrays.asList(vmArgs));
            if (isProfiling != null && isProfiling.booleanValue()) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.SSM, "Profile mode detected by pre-startup class: " + startup.getClass().toString());
                return true;
            }
        }

        return false;
    }

    protected void detectAndSetServerMode(JMXConnection jmxConnection) {
        // Handle remote server debug and profiling modes
        try {
            String[] jvmArgs = jmxConnection.getVMArgs();
            int debugPortNum = jmxConnection.getDebugPortNum();
            String serverMode = server.getMode();
            boolean isDebugMode = debugPortNum > -1;
            boolean isProfileMode = isProfileMode(jvmArgs);

            if (isDebugMode) {
                try {
                    // Check if we're already in debug mode
                    if (!ILaunchManager.DEBUG_MODE.equals(serverMode)) {
                        /************************************************************************************************************
                         * It's important to avoid synchronizing around the call to server.start() with serverStateSyncObj because
                         * it will cause a deadlock. If you do the JMXMonitorThread will hold the lock and server.start() will
                         * use another thread to change the server state and will be stuck.
                         *************************************************************************************************************/
                        //start attaching debugger job
                        if (!wsBehaviour.isDebugAttached(debugPortNum, server)) {
                            WebSphereLaunchConfigurationDelegate.launchIt(ILaunchManager.DEBUG_MODE, wsBehaviour);
                        }
                        if (Trace.ENABLED)
                            Trace.trace(Trace.SSM, "Changing server state to DEBUGGING, attaching debugger. Server listening at port: " + debugPortNum);
                        synchronized (serverStateSyncObj) {
                            wsBehaviour.setInternalMode(ILaunchManager.DEBUG_MODE);
                            wsBehaviour.setServerStateImpl(IServer.STATE_STARTED);
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.logError("Error launching in DEBUGGING mode ", e);
                    }
                }
            } else if (isProfileMode) {
                try {
                    // Check if we're already in profiling mode
                    if (!ILaunchManager.PROFILE_MODE.equals(serverMode)) {
                        /************************************************************************************************************
                         * It's important to avoid synchronizing around the call to server.start() with serverStateSyncObj because
                         * it will cause a deadlock. If you do the JMXMonitorThread will hold the lock and server.start() will
                         * use another thread to change the server state and will be stuck.
                         *************************************************************************************************************/
                        WebSphereLaunchConfigurationDelegate.launchIt(ILaunchManager.PROFILE_MODE, wsBehaviour);
                        if (Trace.ENABLED)
                            Trace.trace(Trace.SSM, "Changing server state to PROFILING");
                        synchronized (serverStateSyncObj) {
                            wsBehaviour.setInternalMode(ILaunchManager.PROFILE_MODE);
                            wsBehaviour.setServerStateImpl(IServer.STATE_STARTED);
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.logError("Error launching in PROFILING mode ", e);
                    }
                }
            } else if (!ILaunchManager.RUN_MODE.equals(serverMode)) {
                wsBehaviour.setInternalMode(ILaunchManager.RUN_MODE);
            }
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.logError("Error parsing vm args ", e);
            }
        }
    }

}
