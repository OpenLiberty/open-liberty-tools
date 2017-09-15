/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.ExternalProcess;
import com.ibm.ws.st.core.internal.launch.LibertyRemoteUtilityExecutionDelegate;
import com.ibm.ws.st.core.internal.launch.RemoteStopServer;

/**
 * The default behaviour for the Liberty server
 */
public class BaseLibertyBehaviourExtension extends AbstractServerBehaviourExtension {

    public BaseLibertyBehaviourExtension() {
        // Constructor is empty
    }

    @Override
    public void stop(ServerBehaviourDelegate behaviour, boolean force, IProgressMonitor monitor) {
        WebSphereServerBehaviour websphereBehaviour = (WebSphereServerBehaviour) behaviour.getServer().loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());

        if (websphereBehaviour == null || websphereBehaviour.getServer().getServerState() == IServer.STATE_STOPPED)
            return;

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Stopping the server");

        // this method will get the server state object lock to ensure there is no open window for the server
        // state monitor(SSM) to get the server state before this method changes the state to stopping
        // which would cause the Servers view to show the server as STOPPING and then blip back to STARTED
        // before the SSM eventually detects the server is stopped and changes the state to STOPPED
        websphereBehaviour.setServerAndModuleState(IServer.STATE_STOPPING);

        boolean terminate = force;

        WebSphereServer wsServer = websphereBehaviour.getWebSphereServer();

        websphereBehaviour.terminateDebugTarget();

        // local server stop implementation
        if (wsServer.isLocalSetup()) {

            if (wsServer.isFeatureConfigured(Constants.FEATURE_LOCAL_JMX)) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Use JMX to stop the server.");
                JMXConnection jmxConnection = null;
                try {
                    jmxConnection = wsServer.createJMXConnection();
                    jmxConnection.stop();
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not use JMX to stop the server", e);
                    terminate = !stopServerWithServerCmd(websphereBehaviour);
                } finally {
                    if (jmxConnection != null)
                        jmxConnection.disconnect();
                }
            } else {
                terminate = !stopServerWithServerCmd(websphereBehaviour);
            }

            if (terminate) {
                websphereBehaviour.terminateProcess();
            }

            // Need to close the process listener streams which will in turn stop the ConsoleStreamsProxy thread. If we
            // don't do this then as the server is Started and Stopped we can have multiple
            // console monitor threads for the same server instance.
            // When a server is started from the command line we create an external process
            // and the thread is started. If we then stop the server from the command line or
            // from WebSphereServerBehaviour.stop() the thread is still running until we close
            // the streams which will stop the thread.
            ILaunch launch = websphereBehaviour.getServer().getLaunch();
            if (launch != null) {
                for (IProcess process : launch.getProcesses()) {
                    try {
                        if (process instanceof ExternalProcess)
                            ((ExternalProcess) process).closeStreams();
                    } catch (Throwable t) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "The process terminate encountered a problem", t);
                    }
                }
            }
        } else { // Remote server stop implementation

            websphereBehaviour.stopMonitorThread(); // while stopping the server the monitor should be stopped to prevent the monitor from trying to start the server

            // Terminate using remote command if remote execution utilities are enabled
            if (terminate && websphereBehaviour.getWebSphereServer().getIsRemoteServerStartEnabled()) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Stopping the remote server via Remote Execution Utility");
                synchronized (websphereBehaviour.serverStateSyncObj) {
                    try {
                        String launchMode = ILaunchManager.RUN_MODE;
                        ILaunch launch = websphereBehaviour.getServer().getLaunch();
                        RemoteStopServer remoteLauncher = new RemoteStopServer(websphereBehaviour.getServer().getStopTimeout() * 1000, new LibertyRemoteUtilityExecutionDelegate());
                        remoteLauncher.execute(websphereBehaviour.getWebSphereServer(), launchMode, launch, null);
                    } catch (Exception e) {
                        if (Trace.ENABLED)
                            Trace.logError("Problem encountered while stopping server", e);
                    }
                }
            } else { // terminate using JMX
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Stopping the remote server via JMX");
                JMXConnection jmxConnection = null;
                try {
                    jmxConnection = wsServer.createJMXConnection();
                    jmxConnection.stop();
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not use JMX to stop the server", e);
                } finally {
                    if (jmxConnection != null) {
                        jmxConnection.disconnect();
                    }
                }
            }

            websphereBehaviour.startMonitorThread();
        } // end of remote stop implementation
    }

    private boolean stopServerWithServerCmd(WebSphereServerBehaviour behaviour) {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Use server command to stop the server.");
        try {
            ProcessBuilder pb = behaviour.getWebSphereRuntime().createProcessBuilder("stop", behaviour.getWebSphereServerInfo());
            synchronized (behaviour.syncObj1) {
                behaviour.serverCmdStopProcess = pb.start();
            }
        } catch (IOException ie) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not launch stop server: " + ie.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public IStatus canStop(ServerBehaviourDelegate behaviour) {

        WebSphereServerBehaviour websphereBehaviour = (WebSphereServerBehaviour) behaviour.getServer().loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());
        if (websphereBehaviour != null && !websphereBehaviour.getWebSphereServer().isLocalSetup()) {
            if (com.ibm.ws.st.common.core.ext.internal.Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) == null)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptRemoteServerActionsUnavailable);

            if (!websphereBehaviour.getWebSphereServer().getIsRemoteServerStartEnabled())
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptRemoteServerSettingsDisabled);
        }
        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus canRestart(ServerBehaviourDelegate behaviour) {
        WebSphereServerBehaviour websphereBehaviour = (WebSphereServerBehaviour) behaviour.getServer().loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());
        if (websphereBehaviour != null && !websphereBehaviour.getWebSphereServer().isLocalSetup()) {
            // if remote start isn't enabled, throw a core exception to trigger popup dialog informing user it is not installed/enabled
            if (com.ibm.ws.st.common.core.ext.internal.Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) == null)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptRemoteServerActionsUnavailable);

            if (!websphereBehaviour.getWebSphereServer().getIsRemoteServerStartEnabled()) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptRemoteServerSettingsDisabled);
            }
        }
        return Status.OK_STATUS;
    }
}
