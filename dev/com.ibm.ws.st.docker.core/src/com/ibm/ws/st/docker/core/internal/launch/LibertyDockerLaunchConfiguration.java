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
package com.ibm.ws.st.docker.core.internal.launch;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractLaunchConfigurationExtension;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.launch.LaunchUtilities;
import com.ibm.ws.st.docker.core.internal.AbstractModeSwitchHandler;
import com.ibm.ws.st.docker.core.internal.Activator;
import com.ibm.ws.st.docker.core.internal.Messages;
import com.ibm.ws.st.docker.core.internal.Trace;

/**
 * Liberty Local Docker Server Launch Implementation
 */

public class LibertyDockerLaunchConfiguration extends AbstractLaunchConfigurationExtension {

    /** {@inheritDoc} */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        final IServer server = ServerUtil.getServer(configuration);
        if (server == null) {
            Trace.logError("Could not find the server for launch configuration: " + configuration.getName(), null);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerServerLaunchFailed));
        }

        WebSphereServer websphereServer = server.getAdapter(WebSphereServer.class);
        WebSphereServerBehaviour websphereServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);

        IRuntime runtime = server.getRuntime();
        if (runtime == null) {
            Trace.logError("The runtime was null for server:  " + server.getName(), null);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerServerLaunchFailed));
        }
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (websphereServer == null || websphereServerBehaviour == null || wsRuntime == null) {
            Trace.logError("Could not get WebSphere server information for server:  " + server.getName(), null);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerServerLaunchFailed));
        }

        setDefaultSourceLocator(launch, configuration);
        websphereServerBehaviour.setLaunch(launch);

        if (monitor.isCanceled())
            return;

        if (server.getServerState() == IServer.STATE_STARTED) {
            // If the server is already started then just return.  This will
            // happen if the launchStarted method called start on the server.
            return;
        }

        Map<String, String> serviceInfo = websphereServer.getServiceInfo();
        LibertyDockerServer serverExt = (LibertyDockerServer) websphereServer.getAdapter(LibertyDockerServer.class);

        if (serviceInfo == null || serverExt == null) {
            Trace.logError("The service info or server extension was null so failed to launch: " + websphereServer.getServerName(), null);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerServerLaunchFailed));
        }

        // Call the mode switch handler if necessary
        String currentMode = serverExt.getCurrentMode(websphereServer);
        if (!mode.equals(currentMode) && !Boolean.getBoolean("wtp.autotest.noninteractive")) {
            AbstractModeSwitchHandler handler = Activator.getModeSwitchHandler();
            if (handler != null) {
                handler.handleExecutionModeSwitch(websphereServer);
            }
        }

        try {
            // Disable the monitoring thread to prevent the server from being set to stopped
            websphereServerBehaviour.stopMonitorThread();

            websphereServerBehaviour.setServerAndModuleState(IServer.STATE_STARTING);

            String containerName = serverExt.getContainerName(websphereServer);
            String machineType = serviceInfo.get(Constants.DOCKER_MACHINE_TYPE);
            String machineName = serviceInfo.get(Constants.DOCKER_MACHINE);
            String osName = serviceInfo.get(Constants.OS_NAME);

            // Check if the server has loose config turned on
            boolean isLooseConfig = websphereServer.isLooseConfigEnabled();

            // Check if the container is a loose config container
            boolean isLooseConfigContainer = serverExt.getCurrentLooseConfigMode(websphereServer);

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Container is enabled for loose config: " + isLooseConfigContainer + ".  Server is configured for loose config: " + isLooseConfig);

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Docker information - containerName=" + containerName + " machineName=" + machineName + " osName=" + osName);

            IPlatformHandler handler = PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.COMMAND);
            AbstractDockerMachine machine = AbstractDockerMachine.createDockerMachine(machineType, machineName, handler);
            List<String> containerNames = machine.getContainerNames(true);
            if (!containerNames.contains(containerName)) {
                Trace.logError("Last container used for server " + websphereServer.getServerName() + " does not exist: " + containerName, null);
                String origContainerName = serviceInfo.get(Constants.DOCKER_CONTAINER);
                if (!origContainerName.equals(containerName)) {
                    // Try the original container
                    if (containerNames.contains(origContainerName)) {
                        Trace.logError("Reverting to the original container: " + origContainerName, null);
                        containerName = origContainerName;
                        serverExt.setCurrentRunStatus(origContainerName, ILaunchManager.RUN_MODE, true, false, websphereServer);
                    } else {
                        return;
                    }
                }
            }

            serviceInfo.put(Constants.DOCKER_CONTAINER, containerName);
            BaseDockerContainer container = (BaseDockerContainer) PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.DOCKER);
            launchContainer(container, mode, monitor, websphereServer);

            websphereServerBehaviour.startMonitorThread();
        } catch (UnsupportedServiceException e) {
            websphereServerBehaviour.setServerAndModuleState(IServer.STATE_STOPPED);
            Trace.logError("Failed to launch server: " + websphereServer.getServerName(), e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServerActionsUnavailable));
        } catch (CoreException ce) {
            websphereServerBehaviour.setServerAndModuleState(IServer.STATE_STOPPED);
            Trace.logError("Failed to launch server: " + websphereServer.getServerName(), ce);
            throw ce;
        } catch (Exception e) {
            websphereServerBehaviour.setServerAndModuleState(IServer.STATE_STOPPED);
            Trace.logError("Failed to launch server: " + websphereServer.getServerName(), e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage()));
        }

    }

    @Override
    public void launchStartedServer(String launchMode, ServerBehaviourDelegate serverBehaviour) throws CoreException {
        IServer server = serverBehaviour.getServer();
        WebSphereServerBehaviour websphereServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());
        if (websphereServerBehaviour == null) {
            Trace.logError("Could not get WebSphereServerBehaviour for server: " + server.getName(), null);
            return;
        }

        WebSphereServer websphereServer = websphereServerBehaviour.getWebSphereServer();

        ILaunch launch = server.getLaunch();
        if (launch == null && server.getServerState() == IServer.STATE_STARTED) {
            // if there's no launch, issue server start to create the launch and set up the source computer, etc.
            if (Trace.ENABLED)
                Trace.trace(Trace.SSM, "Issuing launch on server " + websphereServer.getServerDisplayName() + " to create a launch artifact and set up source computer.");
            server.start(launchMode, new NullProgressMonitor());
        }

        boolean isDebugMode = ILaunchManager.DEBUG_MODE.equals(launchMode);

        try {
            LibertyDockerServer serverExt = (LibertyDockerServer) websphereServer.getAdapter(LibertyDockerServer.class);
            if (serverExt == null) {
                Trace.logError("The server extension was null so failed to launch: " + websphereServer.getServerName(), null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerServerLaunchFailed));
            }

            // remote debug case, connect debug client
            if (isDebugMode && launch != null) {
                BaseDockerContainer container = serverExt.getContainer(websphereServer);
                String port = LibertyDockerRunUtility.getDebugPort(container);
                port = container.getHostMappedPort(port);
                int debugPort = Integer.parseInt(port);

                // attach debugger
                if (!websphereServerBehaviour.isDebugAttached(debugPort, server)) {
                    final IDebugTarget oldDebugTarget = websphereServerBehaviour.getDebugTarget();
                    LaunchUtilities.connectRemoteDebugClient(launch, debugPort, websphereServer);
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

    private void launchContainer(BaseDockerContainer container, String mode, IProgressMonitor monitor, WebSphereServer websphereServer) throws CoreException {
        WebSphereServerBehaviour websphereServerBehaviour = websphereServer.getWebSphereServerBehaviour();
        websphereServerBehaviour.ensureMonitorRunning();

        websphereServerBehaviour.preLaunch(monitor);
        try {
            String newContainerName = startContainer(container, mode, websphereServerBehaviour.getWebSphereServer(), monitor);
            if (newContainerName != null) {
                LibertyDockerServer serverExt = (LibertyDockerServer) websphereServer.getAdapter(LibertyDockerServer.class);
                serverExt.setCurrentRunStatus(newContainerName, mode, false, websphereServer.isLooseConfigEnabled(), websphereServer);
            }
        } catch (CoreException e) {
            Trace.logError("Failed to start container " + container.getContainerName() + " in " + mode + " mode.", e);
            throw e;
        } catch (Exception e) {
            Trace.logError("Failed to start container: " + container.getContainerName() + " in " + mode + " mode.", e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage()));
        }
        websphereServerBehaviour.postLaunch(true);

        launchStartedServer(mode, websphereServerBehaviour);
    }

    private String startContainer(BaseDockerContainer container, String mode, WebSphereServer server, IProgressMonitor progressMonitor) throws Exception {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, 100);

        AbstractDockerMachine machine = container.getDockerMachine();
        LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        Map<String, String> serviceInfo = server.getServiceInfo();

        String currentMode = serverExt.getCurrentMode(server);
        boolean modeNotChanged = mode.equals(currentMode);
        boolean hasNewVolumes = server.isLooseConfigEnabled() && hasNewVolumes(server, serverExt, container);
        if (modeNotChanged && !isLooseConfigModeChanged(server) && !hasNewVolumes) {
            // Can use the existing container since nothing has changed
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Mode has not changed for container: " + container.getContainerName() + ", so using the same container");
            if (!container.isRunning()) {
                monitor.subTask(NLS.bind(Messages.dockerStartingContainerTask, container.getContainerName()));
                container.start(monitor.newChild(100));
            }
            return null;
        }

        monitor.subTask(Messages.dockerCreatingContainerTask);

        // The mode has changed so need to commit the current container to a new image

        String originalContainerName = serviceInfo.get(Constants.DOCKER_CONTAINER);

        boolean isLooseConfigModeChanged = isLooseConfigModeChanged(server);

        // Need to distinguish between a user container and the non-user loose config container that the wizard created
        // This is used to determine what name to give to the new container and new image.
        boolean isUserContainer = serverExt.isUserContainer(server);

        List<String> newNames = LibertyDockerRunUtility.getNewNames(mode, serviceInfo.get(Constants.DOCKER_IMAGE), originalContainerName, machine, server.isLooseConfigEnabled(),
                                                                    isUserContainer, true);

        String newImageName = newNames.get(0); // First is the image name
        String newContainerName = newNames.get(1); // Second is the container name

        LibertyDockerRunUtility.flattenImage(newImageName, serviceInfo, container, machine);

        // Get the run command for the new image
        List<String> cmd = null;
        // Special case - we want to 'create' the container, not 'run' it, so that we can copy over files
        // before the container is started.
        // RTC Defect #235888
        boolean copyingFilesRequired = isLooseConfigModeChanged && !server.isLooseConfigEnabled();

        if (!isLooseConfigModeChanged) {

            cmd = LibertyDockerRunUtility.getRunCommand(container, false, mode, currentMode, newContainerName, newImageName, server);
        } else {
            // Set up the run command but add the -v option
            if (copyingFilesRequired) {
                cmd = LibertyDockerRunUtility.getCreateCommand(container, isLooseConfigModeChanged, mode, currentMode,
                                                               newContainerName, newImageName, server);
            } else {
                cmd = LibertyDockerRunUtility.getRunCommand(container, isLooseConfigModeChanged, mode, currentMode,
                                                            newContainerName, newImageName, server);
            }
        }

        // Before deleting the current container, grab the mounted temp path for the usr folder
        // We should use this path to copy any files to the new container, instead of reconstructing the path
        String originalTempPath = container.getMountSourceForDestination(LibertyDockerRunUtility.DOCKER_LIBERTY_USR_PATH);

        if (monitor.isCanceled()) {
            throw new CoreException(Status.CANCEL_STATUS);
        }
        monitor.worked(10);

        // Delete the old container and image
        if (!serverExt.isUserContainer(server)) {
            String containerName = container.getContainerName();
            try {
                String imageName = container.getImageName();
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Removing container: " + containerName + ", and image: " + imageName);
                machine.removeContainer(containerName);
                machine.removeImage(imageName);
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to clean up old container and/or image for: " + containerName);
                }
            }
            if (monitor.isCanceled()) {
                throw new CoreException(Status.CANCEL_STATUS);
            }
            monitor.worked(10);
        }

        monitor.setWorkRemaining(80);

        Trace.ENABLED = true;

        // Run or create the new image
        String cmdStr = LibertyDockerRunUtility.mergeStrings(cmd);
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Creating container: " + newContainerName + ", for image: " + newImageName + ", with command: " + cmdStr);

        machine.runCommand(cmdStr, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5, monitor.newChild(50));

        if (monitor.isCanceled()) {
            throw new CoreException(Status.CANCEL_STATUS);
        }

        // For changing to non-loose config, the files from the original container must be copied
        // to the newly created container.
        // The container also requires starting, since above it was created, not run.
        if (copyingFilesRequired) {
            handleNonLooseConfig(newContainerName, server, currentMode, originalTempPath);
            if (monitor.isCanceled()) {
                throw new CoreException(Status.CANCEL_STATUS);
            }
            monitor.worked(10);

            // start the container
            List<String> startCmd = LibertyDockerRunUtility.getStartCommand(newContainerName);
            String startCmdStr = LibertyDockerRunUtility.mergeStrings(startCmd);

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Starting container: " + newContainerName + ", with command: " + startCmdStr);

            machine.runCommand(startCmdStr, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5, monitor.newChild(20));

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, newContainerName + ", is started. ");
        }

        monitor.setWorkRemaining(0);

        return newContainerName;
    }

    private boolean isLooseConfigModeChanged(WebSphereServer server) {
        LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        if (serverExt != null) {
            return server.isLooseConfigEnabled() != serverExt.getCurrentLooseConfigMode(server);
        }
        return false;
    }

    private void handleNonLooseConfig(String newContainerName, WebSphereServer websphereServer,
                                      String mode, String originalTempPath) throws Exception {

        Map<String, String> serviceInfo = websphereServer.getServiceInfo();
        serviceInfo.put(Constants.DOCKER_CONTAINER, newContainerName);
        BaseDockerContainer newContainer = (BaseDockerContainer) PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.DOCKER);

        websphereServer.getWebSphereServerBehaviour().stopMonitorThread();

        // Convert it to the local path, if on Windows.  Eg. c/users/folder ---> c:/users/folder
        IPath tempPath = BaseDockerContainer.getContainerToLocalPath(new Path(originalTempPath));
        String serverConfigPath = serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH);
        // The serverConfigPath contains the path to the usr folder appended by /servers/serverName
        // Also get the wlp folder, the parent of the usr folder because we will copy the usr as a subfolder
        String usrFolder = serverConfigPath.replace("/servers/" + serviceInfo.get("libertyServerName"), "/");
        try {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Copying files from " + tempPath + ", into container " + newContainerName + " " + usrFolder);

            newContainer.copyIn(tempPath.toString() + "/.", usrFolder);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not copy all the files from the usr folder to the new Docker container.  The new container could not be set up properly.", e); //$NON-NLS-1$
            // should prevent launch if there are any exceptions caught from the above commands
            throw e;
        }
    }

    private boolean hasNewVolumes(WebSphereServer server, LibertyDockerServer serverExt, BaseDockerContainer container) throws Exception {
        List<String> additionalVolumes = serverExt.getAdditionalVolumes(server);
        List<IPath> newVolumes = LibertyDockerRunUtility.getAdditionalVolumes(additionalVolumes, container);
        return !newVolumes.isEmpty();
    }

}
