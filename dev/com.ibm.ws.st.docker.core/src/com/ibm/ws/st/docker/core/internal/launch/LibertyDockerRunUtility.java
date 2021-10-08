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

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.common.core.ext.internal.util.FileUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.docker.core.internal.AbstractFlattenImageHandler;
import com.ibm.ws.st.docker.core.internal.Activator;
import com.ibm.ws.st.docker.core.internal.Messages;
import com.ibm.ws.st.docker.core.internal.Trace;

/**
 * Shared Run Command utility class for use by debug launch and new server wizard docker page
 */
public class LibertyDockerRunUtility {

    public static String getLibertyUsrPath(Map<String, String> serviceInfo) {
        final String defaultPath = "/opt/ibm/wlp/usr";
        if (serviceInfo == null) {
            Trace.logError("The service info map is null so using default Liberty Docker user directory.", null);
            return defaultPath;
        }
        String installDir = serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH);
        if (installDir != null && !installDir.isEmpty()) {
            return installDir + "/usr";
        }
        Trace.logError("The runtime install path is not set properly in the service info - using the default user directory", null);
        return defaultPath;
    }

    public static String getLibertyStdevPath(Map<String, String> serviceInfo) {
        final String defaultPath = "/opt/ibm/wlp/stdev";
        if (serviceInfo == null) {
            Trace.logError("The service info map is null so using default Liberty Docker stdev directory.", null);
            return defaultPath;
        }
        String installDir = serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH);
        if (installDir != null && !installDir.isEmpty()) {
            return installDir + "/stdev";
        }
        Trace.logError("The runtime install path is not set properly in the service info - using the default stdev directory", null);
        return defaultPath;
    }

    public static List<String> getCreateCommand(BaseDockerContainer container, boolean looseCfgModeChanged, String newMode, String currentMode,
                                                String newContainerName, String imageName, WebSphereServer server) throws Exception {

        List<String> createCmd = getRunCommand(container, looseCfgModeChanged, newMode, currentMode, newContainerName, imageName, server);

        // The only differences between the create command and the run command are:
        // "docker run" -> "docker create"
        // -d flag is not specified for create.
        createCmd.set(1, "create");
        Collections.replaceAll(createCmd, "-td", "-t");

        return createCmd;
    }

    public static List<String> getRunCommand(BaseDockerContainer container, boolean looseCfgModeChanged, String newMode, String currentMode,
                                             String newContainerName, String imageName, WebSphereServer server) throws Exception {

        // If Loose Config is enabled and the Loose Config state changed, then we need to create the local file paths BEFORE the docker run command is started
        // Another way to put it is that if loose config is enabled but it was simply a restart in debug mode, then we do not have to create the file paths.
        // So, both state, and whether the state has changed or not, matters.
        if (server.isLooseConfigEnabled() && looseCfgModeChanged) {
            LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
            if (serverExt != null) {
                Map<String, String> serviceInfo = server.getServiceInfo();
                String containerName = serverExt.getContainerName(server);
                serviceInfo.put(Constants.DOCKER_CONTAINER, containerName);
                IPath userFolder = server.getUserDirectory().getPath();
                copyFromContainerToHost(userFolder, serviceInfo, container);
            }
            Map<IPath, IPath> volumes = calculateVolumes(container, server, true);
            return getRunCommand(container, newMode, currentMode, newContainerName, imageName, volumes, server);
        }
        Map<IPath, IPath> volumes = calculateVolumes(container, server, looseCfgModeChanged);
        return getRunCommand(container, newMode, currentMode, newContainerName, imageName, volumes, server);
    }

    public static List<String> getStartCommand(String containerName) {
        List<String> startCmd = new ArrayList<String>();
        startCmd.add("docker");
        startCmd.add("start");
        startCmd.add(containerName);

        return startCmd;
    }

    public static String getDebugPort(BaseDockerContainer container) throws Exception {
        List<String> env = container.getEnv();

        String debugPort = "7777";
        for (String envDef : env) {
            // Check if WLP_DEBUG_ADDRESS is set
            String varStart = "WLP_DEBUG_ADDRESS=";
            if (envDef.startsWith(varStart)) {
                String port = envDef.substring(varStart.length());
                try {
                    int portNum = Integer.parseInt(port);
                    if (portNum >= 1 && portNum <= 65535) {
                        debugPort = port;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        return debugPort;
    }

    public static String mergeStrings(List<String> strings) {
        boolean start = true;
        StringBuilder builder = new StringBuilder();
        for (String str : strings) {
            if (start) {
                start = false;
            } else {
                builder.append(" ");
            }
            builder.append(str);
        }
        return builder.toString();
    }

    private static List<String> getRunCommand(BaseDockerContainer container, String newMode, String currentMode,
                                              String containerName, String imageName, Map<IPath, IPath> volumes,
                                              WebSphereServer server) throws Exception {
        List<String> newCmd = new ArrayList<String>();
        List<String> env = container.getEnv();
        Map<String, String> portBindings = container.getPortBindings();
        List<String> cmd = container.getCommand();
        boolean isLocal = SocketUtil.isLocalhost(server.getServer().getHost());

        newCmd.add("docker");
        newCmd.add("run");

        for (String envDef : env) {
            // Don't include debug flag
            if (envDef.equals("WLP_DEBUG_REMOTE=y") && (!"debug".equals(newMode))) {
                continue;
            }
            newCmd.add("-e");
            newCmd.add("\"" + envDef + "\"");
        }

        if ("debug".equals(newMode)) {
            newCmd.add("-e WLP_DEBUG_REMOTE=y");
        }

        // If -P was set before or in local debug mode then add it to the command.  It is
        // quicker and easier to let -P find a free port on the VM to map the debug
        // port to.
        // Any -p options take precedence over -P
        if (container.getPublishAllPorts() || ("debug".equals(newMode) && isLocal)) {
            newCmd.add("-P");
        }
        String debugPort = getDebugPort(container);
        for (Map.Entry<String, String> binding : portBindings.entrySet()) {
            // Debug port mapping set elsewhere so don't include here
            if (!binding.getKey().equals(debugPort)) {
                newCmd.add("-p");
                newCmd.add(binding.getValue() + ":" + binding.getKey());
            }
        }
        if ("debug".equals(newMode)) {
            if (isLocal) {
                // Make sure the debug port is exposed or -P will not map it
                newCmd.add("--expose");
                newCmd.add(debugPort);
            } else {
                // If in remote debug mode, use the port set in the server editor
                String mappedPort = server.getRemoteServerStartDebugPort();
                newCmd.add("-p");
                newCmd.add(mappedPort + ":" + debugPort);
            }
        }

        List<String> volumeArgs = getVolumeArgs(volumes);
        newCmd.addAll(volumeArgs);

        newCmd.add("--name");
        newCmd.add(containerName);
        newCmd.add("-td");
        newCmd.add(imageName);
        for (String item : cmd) {
            if (item.equals(currentMode)) {
                newCmd.add(newMode);
            } else {
                newCmd.add(item);
            }
        }

        return newCmd;
    }

    public static String setupNewContainer(final UserDirectory userDir, final IPath workspaceRoot, final BaseDockerContainer origContainer, Map<String, String> serviceInfo,
                                           WebSphereServer wasServer, MountProperty existingVolumeStatus,
                                           IProgressMonitor monitor) throws IOException, ConnectException, Exception {

        IProgressMonitor mon = monitor;
        if (mon == null) {
            mon = new NullProgressMonitor();
        }
        mon.beginTask(Messages.dockerCreatingNewContainer, 100);
        String origContainerName = origContainer.getContainerName();

        try {
            AbstractDockerMachine machine = origContainer.getDockerMachine();
            // We are setting up a new container from the new server wizard here, so we will set the initial mode to "run"
            List<String> newNames = getNewNames("run", serviceInfo.get(Constants.DOCKER_IMAGE), origContainerName, machine, true,
                                                true, false);
            String newImageName = newNames.get(0); // First is the image name
            String containerName = newNames.get(1); // Second is the container name

            IPath adjustedUsrFolder = BaseDockerContainer.getLocalToContainerPath(userDir.getPath());

            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Volume mount is " + adjustedUsrFolder);
            }

            try {
                // Do not copy the usr folder from the container to the user folder because the contents are already there for
                // containers that have an existing usr volume
                if (!existingVolumeStatus.equals(MountProperty.OTHER_USR_MOUNT)) {
                    copyFromContainerToHost(userDir.getPath(), serviceInfo, origContainer);
                }
            } catch (ConnectException ce) {
                throw ce;
            } catch (IOException ioe) {
                throw ioe;
            }

            mon.worked(20);

            // Workaround for 239259: Start/Restart fails after successfully starting the first time for Liberty in a Docker container
            // Must delete the workarea folder prior to doing a commit
            try {
                if (existingVolumeStatus.equals(MountProperty.OTHER_USR_MOUNT)) {
                    // Typically, use the server info to get the server output path, eg. IPath workareaPath = wasServer.getServerInfo().getServerOutputPath().append("workarea");
                    // but it gives you the 'local' filesystem project workarea for the server.  We need the container's.
                    String outputPath = origContainer.getEnvValue(com.ibm.ws.st.core.internal.Constants.WLP_OUTPUT_DIR);
                    // serviceInfo already has the server name from the inspected JSON.
                    IPath workAreaPath = new Path(outputPath).append(serviceInfo.get(Constants.LIBERTY_SERVER_NAME)).append("workarea");
                    // /opt/ibm/wlp/output/defaultServer/workarea
                    origContainer.deleteFolder(workAreaPath.toString());
                }
            } catch (Exception e) {
                // Ignore if we can't delete the workarea first
                Trace.trace(Trace.WARNING, "Cannot delete the workarea folder from the container.");
            }
            // End Workaround

            flattenImage(newImageName, serviceInfo, origContainer, machine);

            mon.worked(20);
            origContainer.stop();
            mon.worked(20);
            // Set up the run command but add the -v option
            List<String> runCmd = LibertyDockerRunUtility.getCreateCommand(origContainer, false, "run", "run", containerName, newImageName, wasServer);
            String cmd = LibertyDockerRunUtility.mergeStrings(runCmd);
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Starting container: " + containerName + " with run command: " + cmd);
            mon.worked(20);
            // Run the command to create the new container
            machine.runCommand(cmd, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5);
            mon.worked(20);

            // start the container
            List<String> startCmd = LibertyDockerRunUtility.getStartCommand(containerName);
            String startCmdStr = LibertyDockerRunUtility.mergeStrings(startCmd);

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Starting container: " + containerName + ", with command: " + startCmdStr);

            machine.runCommand(startCmdStr, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5, mon);

            serviceInfo.put(Constants.DOCKER_CONTAINER, origContainerName);

            return containerName;
        } catch (Exception e) {
            Trace.logError("Failed to create run docker commands need to set up the new container", e);
            throw e;
        } finally {
            mon.done();
        }
    }

    public static void flattenImage(String newImageName, Map<String, String> serviceInfo, BaseDockerContainer finalContainer,
                                    AbstractDockerMachine machine) throws ConnectException {
        boolean flattened = false;
        try {
            String originalImage = serviceInfo.get(Constants.DOCKER_IMAGE);
            String currentImage = finalContainer.getImageName();
            long origSize = machine.getImageSize(originalImage);
            long currentSize = machine.getImageSize(currentImage);
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Docker original image " + originalImage + " size: " + origSize);
                Trace.trace(Trace.INFO, "Docker current image " + currentImage + " size: " + currentSize);
            }
            if (currentSize > (origSize * 2) && currentSize > Math.pow(10, 9)) {
                AbstractFlattenImageHandler handler = Activator.getFlattenImageHandler();
                if (handler != null) {
                    flattened = handler.handleFlattenImage(finalContainer, newImageName);
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to flatten the " + finalContainer.getContainerName() + " container.", e);
            }
        }

        if (!flattened) {
            finalContainer.commit(newImageName);
        }
    }

    /*
     * Get new Image and Container names. First in the list is the image. Second in the list is the container.
     *
     * Ensures that the 'integers' are matched up if they are required to be added.
     *
     * This method added for JUnit test purposes
     */
    public static List<String> getNewNames(String mode, String origImageName, String origContainerName, AbstractDockerMachine machine, boolean isLooseConfigEnabled,
                                           boolean isUserContainer, boolean includeMode) throws Exception {

        List<String> existingContainers = machine.getContainerNames(true);
        List<String> existingImages = machine.getImages();
        return getNewNames(mode, origImageName, origContainerName, isLooseConfigEnabled, isUserContainer, includeMode, existingContainers, existingImages);
    }

    /*
     * Get new Image and Container names. First in the list is the image. Second in the list is the container.
     *
     * Ensures that the 'integers' are matched up if they are required to be added.
     */
    public static List<String> getNewNames(String mode, String origImageName, String origContainerName, boolean isLooseConfigEnabled,
                                           boolean isUserContainer, boolean includeMode, List<String> existingContainers,
                                           List<String> existingImages) throws Exception {
        // If the original container with origContainerName  (eg. wlp) created via the new server wizard is a _user_ container, then the image name shoud be wlp + _ + origImageName.
        // (eg. wlp_websphere-liberty).  However, if the original container is a non-user container created with loose config enabled, then the origContainerName is wlp_dev, after
        // server creation.  We know this because we append _dev to the 'user' container name.   We do not want to use wlp_dev as the 'prefix' for which to add _debug or _run or
        // even _dev, otherwise we will have duplicates.   For example, wlp_dev_dev  for the container name, or wlp_dev_wlp_dev_websphere-liberty for the new image name.
        List<String> newNames = new ArrayList<String>(2); // First is new image, Second is new container

        String modifiedOrig = null;
        String modifiedOrigImage = null;
        if (!isUserContainer) {
            // If it is a non-user container (tools-generated), then we know we added the _dev suffix.  We should strip it out to get
            // the original name of the container.
            modifiedOrig = origContainerName.replace("_dev", ""); //$NON-NLS-1$ //$NON-NLS-2$ // wlp_dev --> wlp
            modifiedOrigImage = origImageName.replace(origContainerName.toLowerCase() + "_", ""); //$NON-NLS-1$ //$NON-NLS-2$ // wlp_dev_websphere-liberty --> websphere-liberty
        } else {
            // For a user container, the container is likely created outside of tools (docker command). However, a non-user contain could
            // become a user container in another workspace.  Eg. the user could have 'saved' a previous container generated by tools and it
            // may have any of our defined suffixes added to it, like _dev_debug or _dev_run.

            // Caveat, if the user 'manually' added any of these suffixes to their container name, then they will be stripped out.
            // We don't want duplicates.  eg. wlp_dev_dev.  Also, it is ok to strip the _debug or _run suffixes, because it will be added
            // back later.
            if (origContainerName.contains("_dev") || origContainerName.contains("_debug") || origContainerName.contains("_run")) {
                modifiedOrig = origContainerName.replace("_dev", ""); //$NON-NLS-1$ //$NON-NLS-2$
                modifiedOrig = modifiedOrig.replace("_debug", ""); //$NON-NLS-1$ //$NON-NLS-2$
                modifiedOrig = modifiedOrig.replace("_run", ""); //$NON-NLS-1$ //$NON-NLS-2$

                // Extreme corner case where the user created a container with the name:  _dev_debug_run or any combination of our suffixes
                // Since we are stripping these suffixes, The modifiedOrig could be an empty string, so let's just assign a default base name
                if (modifiedOrig.length() == 0) {
                    modifiedOrig = "wlp"; //$NON-NLS-1$
                }
            } else {
                modifiedOrig = origContainerName;
            }
            modifiedOrigImage = origImageName;
        }

        boolean endsWithDashNumber = modifiedOrig.matches(".*-[0-9]+"); // 1 or more integers.  Looking for the pattern abc-123, abc-1, abc-5

        if (endsWithDashNumber) {
            modifiedOrig = modifiedOrig.substring(0, modifiedOrig.lastIndexOf("-"));
        }

        String baseContainerName = modifiedOrig + (isLooseConfigEnabled ? "_dev" : "") + (includeMode ? "_" + mode : "");
        String baseImageName = baseContainerName.toLowerCase() + "_" + modifiedOrigImage;

        String containerName = baseContainerName;
        String imageName = baseImageName;

        int i = 1;
        // Increment counter until both names are available
        while (existingContainers.contains(containerName) || existingImages.contains(imageName)) {
            containerName = modifiedOrig + "-" + (i++)
                            + (isLooseConfigEnabled ? "_dev" : "")
                            + (includeMode ? "_" + mode : "");
            imageName = containerName.toLowerCase() + "_" + modifiedOrigImage;
        }
        newNames.add(imageName);
        newNames.add(containerName);
        return newNames;
    }

    private static IPath getWorkspacePath(WebSphereServer wsServer) {
        IPath workspacePath = null;
        if (wsServer.getWebSphereRuntime() != null) {
            workspacePath = wsServer.getWebSphereRuntime().getProject().getWorkspace().getRoot().getLocation();
        }
        if (workspacePath == null) { // Perhaps the project folder got deleted??  If so, try alternate means to get the workspace path
            workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        }
        return workspacePath;
    }

    /**
     * Copy the usr folder from the container to the local file system
     *
     * @param mappedUsrFolder - the destination folder in the local file system
     * @param serviceInfo
     * @param container
     * @throws IOException
     * @throws CoreException
     */
    private static void copyFromContainerToHost(IPath mappedUsrFolder, Map<String, String> serviceInfo,
                                                BaseDockerContainer container) throws ConnectException, IOException, CoreException {

        String serverConfigPath = serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH);
        // Copy using somePath/. to copy all the files from the usr folder to the local filesystem
        String usrFolder = serverConfigPath.replace("/servers/" + serviceInfo.get("libertyServerName"), "/.");

        // remove last segment ("usr") from the target destination
        try {
            container.copyOut(usrFolder, mappedUsrFolder.toString());
        } catch (ConnectException ce) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Can't connect to container in order to copy the following usr folder from the container to the local filesystem:" + usrFolder);
            }
            throw ce;
        } catch (IOException io) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Can't copy the usr folder from the container to the local filesystem:" + mappedUsrFolder.toString());
            }
            throw io;
        }

        // After copying the files into the usr folder (of the runtime project),
        // we need to refresh the project so it sees the changes.
        IProject runtimeProject = ResourcesPlugin.getWorkspace().getRoot().getProject(mappedUsrFolder.lastSegment());
        if (runtimeProject != null) {
            runtimeProject.refreshLocal(IResource.DEPTH_INFINITE, null);
        } else {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Runtime project was null, and could not be refreshed. "
                                           + "MappedUsrFolder was: " + mappedUsrFolder);
            }
        }
    }

    /**
     * Convert a map of volumes into arguments for docker run command
     *
     * @param volumeMap
     * @param container
     * @return
     */
    public static List<String> getVolumeArgs(Map<IPath, IPath> volumeMap) {
        List<String> args = new ArrayList<String>();
        if (volumeMap == null || volumeMap.isEmpty()) {
            return args;
        }
        for (Map.Entry<IPath, IPath> entry : volumeMap.entrySet()) {
            IPath path = entry.getKey();
            // Make sure the path exists otherwise the container will not start
            if (path.toFile().exists()) {
                IPath adjustedSrcPath = BaseDockerContainer.getLocalToContainerPath(path);
                args.add("-v");
                args.add("\"" + adjustedSrcPath.toString() + ":" + entry.getValue().toString() + "\"");
            } else {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "No volume will be created for the following path because it does not exist on the file system: " + path.toOSString());
                }
            }
        }
        return args;
    }

    /**
     * Compare a map of volumes to the volumes on a container and only return volumes that are not already
     * covered by an existing volumes.
     *
     * @param additionalVolumes
     * @param container
     * @return
     * @throws Exception
     */
    public static List<IPath> getAdditionalVolumes(List<String> additionalVolumes, BaseDockerContainer container) throws Exception {
        Map<IPath, IPath> existingVolumes = container.getMountedVolumeHash();
        List<IPath> newVolumes = new ArrayList<IPath>();
        for (String path : additionalVolumes) {
            boolean found = false;
            IPath srcPath = new Path(path);
            for (IPath existingPath : existingVolumes.keySet()) {
                if (existingPath.isPrefixOf(srcPath)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newVolumes.add(srcPath);
            }
        }
        return newVolumes;
    }

    public static Map<IPath, IPath> calculateVolumes(BaseDockerContainer container, WebSphereServer wsServer, boolean looseCfgModeChanged) throws Exception {
        Map<IPath, IPath> volumes = container.getMountedVolumeHash();

        Map<String, String> serviceInfo = wsServer.getServiceInfo();
        String libertyUsrPath = getLibertyUsrPath(serviceInfo);
        String libertyStdevPath = getLibertyStdevPath(serviceInfo);

        if (wsServer.isLooseConfigEnabled()) {
            // Add default loose config volumes
            IPath usrDirPath = wsServer.getUserDirectory().getPath();
            usrDirPath = FileUtil.getCanonicalPath(usrDirPath);
            IPath dest = volumes.get(usrDirPath);
            IPath expectedDest = new Path(libertyUsrPath);
            if (!expectedDest.equals(dest)) {
                volumes.put(usrDirPath, expectedDest);
            }

            LibertyDockerServer serverExt = (LibertyDockerServer) wsServer.getAdapter(LibertyDockerServer.class);
            if (serverExt == null) {
                // At this point we know we are working with a Liberty Docker server
                // so this should not happen unless something went really wrong
                Trace.logError("The server should be a Liberty Docker server but the conversion failed.", null);
            } else {
                // Add any mandatory workspace volumes (stdevX mounts)
                List<String> mandatoryVolumes = serverExt.getMandatoryVolumes(wsServer);
                String candidate = libertyStdevPath;
                for (String mandatoryVolume : mandatoryVolumes) {
                    if (newVolumeNeeded(volumes, new Path(mandatoryVolume))) {
                        volumes.put(new Path(mandatoryVolume), getVolumeDestination(candidate, volumes));
                    }
                }

                IPath workspacePath = getWorkspacePath(wsServer);
                workspacePath = FileUtil.getCanonicalPath(workspacePath);
                // Now add the current workspace volume
                if (newVolumeNeeded(volumes, workspacePath)) {
                    volumes.put(workspacePath, getVolumeDestination(candidate, volumes));
                }

                // Add loose config volumes for modules
                Set<IPath> moduleLocations = getAllModuleLocations(wsServer);
                List<String> additionalVolumes = serverExt.getAdditionalVolumes(wsServer);
                mergeModuleLocations(moduleLocations, additionalVolumes);
                List<IPath> newVolumes = new ArrayList<IPath>();
                for (IPath moduleLocation : moduleLocations) {
                    if (newVolumeNeeded(volumes, moduleLocation)) {
                        dest = getVolumeDestination(libertyStdevPath, volumes);
                        volumes.put(moduleLocation, dest);
                        newVolumes.add(moduleLocation);
                    }
                }
                serverExt.setAdditionalVolumes(wsServer, newVolumes);
            }
        } else if (looseCfgModeChanged) {
            // Remove any loose config volumes added by the tools
            for (Iterator<Map.Entry<IPath, IPath>> it = volumes.entrySet().iterator(); it.hasNext();) {
                Map.Entry<IPath, IPath> entry = it.next();
                IPath dest = entry.getValue();
                String destStr = dest.toString();
                if (destStr.startsWith(libertyStdevPath) || destStr.equals(libertyUsrPath)) {
                    it.remove();
                }
            }
        }
        return volumes;
    }

    /**
     * Add locations for the module to the set of locations if not already covered by existing volumes or locations already in the set
     *
     * @param wsServer
     * @param containerVolumes
     * @param locations
     * @param module
     * @return True if any new locations were added to the set, false otherwise
     */
    public static void addModuleLocations(WebSphereServer wsServer, Map<IPath, IPath> containerVolumes, Set<IPath> locations, IModule module) {
        // Get all of the locations for the module
        Set<IPath> moduleLocations = LibertyDockerRunUtility.getLocations(wsServer, module);
        for (IPath location : moduleLocations) {
            boolean found = false;
            // Check if the location is already covered by a container volume
            for (IPath existingPath : containerVolumes.keySet()) {
                IPath basePath = FileUtil.getCanonicalPath(existingPath);
                if (basePath.isPrefixOf(location)) {
                    found = true;
                }
            }
            if (!found) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "No existing volume was found for: " + location);
                }
                // Merge the location into the set of locations (make sure not already covered,
                // or replace existing locations that are covered by this one)
                LibertyDockerRunUtility.mergeModuleLocation(locations, location);
            }
        }
    }

    /**
     * Get the locations for a module. Considers children of the module as well.
     *
     * @param server
     * @param module
     * @return
     */
    public static Set<IPath> getLocations(WebSphereServer server, IModule module) {
        Set<IPath> locations = new HashSet<IPath>();
        getLocations(server, module, locations);
        return locations;
    }

    /**
     * Get locations for all of the modules currently deployed on the server. Considers
     * the children of each module as well.
     *
     * @param server
     * @return
     */
    public static Set<IPath> getAllModuleLocations(WebSphereServer server) {
        IModule[] modules = server.getServer().getModules();
        Set<IPath> locations = new HashSet<IPath>();
        for (IModule module : modules) {
            getLocations(server, module, locations);
        }
        return locations;
    }

    private static void getLocations(WebSphereServer server, IModule module, Set<IPath> locations) {
        IProject project = module.getProject();
        if (project != null) {
            IPath projectPath = project.getLocation();
            IPath parentPath = FileUtil.getCanonicalPath(projectPath.removeLastSegments(1));
            mergeModuleLocation(locations, parentPath);
        }
        IModule[] children = server.getChildModules(new IModule[] { module });
        for (IModule child : children) {
            getLocations(server, child, locations);
        }
    }

    /**
     * Merge a collections of locations into a set of locations. For each location, if it is already
     * in the set or a parent folder of the location is already in the set, it will not be added.
     *
     * @param locations
     * @param newLocations
     */
    public static void mergeModuleLocations(Set<IPath> locations, Collection<String> newLocations) {
        for (String newLocation : newLocations) {
            mergeModuleLocation(locations, new Path(newLocation));
        }
    }

    /**
     * Merge a location into a set of locations. If the location is already in the set or a parent
     * folder of the location is already in the set it will not be added.
     *
     * Tries to avoid mounting both /a/b and /a/b/c. If there is an existing volume for /a/b/c
     * then it will not be removed and /a/b will be added. But if adding both modules at the
     * same time or switching from non-loose config to loose config then it tries to minimize
     * the volumes.
     *
     * @param locations
     * @param location
     */
    public static void mergeModuleLocation(Set<IPath> locations, IPath location) {
        boolean found = false;
        IPath newPath = FileUtil.getCanonicalPath(location);
        for (IPath path : locations) {
            // If the location or a parent folder of the location is already in the set, do not add it
            if (path.isPrefixOf(newPath)) {
                found = true;
                break;
            }
            // If the new location is a parent of an existing location then replace it with the new one
            if (newPath.isPrefixOf(path)) {
                locations.remove(path);
                break;
            }
        }
        if (!found) {
            locations.add(newPath);
        }
    }

    private static boolean newVolumeNeeded(Map<IPath, IPath> volumes, IPath localPath) {
        boolean found = false;
        for (IPath path : volumes.keySet()) {
            if (path.isPrefixOf(localPath)) {
                found = true;
                break;
            }
        }
        return !found;
    }

    /**
     * Find a destination for a volume that is not already in use.
     * Use root first and then add the index if necessary. eg. root, root1, root2,....
     *
     * @param root
     * @param existingVolumes
     * @return
     */
    private static IPath getVolumeDestination(String root, Map<IPath, IPath> existingVolumes) {
        int i = 0;
        IPath path = new Path(root);
        Collection<IPath> destPaths = existingVolumes.values();
        while (destPaths.contains(path)) {
            i++;
            path = new Path(root + i);
        }
        return path;
    }

    /*
     * Used to help analyze the existing container. Used in checkContainerForLooseConfigMountVolume
     */
    private static enum MountProperties {
        SAME_USR_MOUNT, SAME_WORKSPACE_MOUNT, OTHER_USR_MOUNT, OTHER_WORKSPACE_MOUNT;
    }

    // Using the above, classify the container's user mount
    // 1. usr folder is from another workspace
    // 2. usr folder is in the same workspace so therefore, loose config is already enabled.   This is possible if the server
    //    is deleted but the container is not.  Then, you can create the server again, WITHOUT having to create a new container.
    // 3. There is no usr mount so server setup is as normal
    public static enum MountProperty {
        OTHER_USR_MOUNT, SAME_USR_MOUNT, NO_USR_MOUNT;
    }

    /**
     * An existing container can have any number of mount volumes. If the container was created by OLT from another workspace,
     * then the mount volume for /opt/ibm/wlp/usr (User Directory) will be reused and the workspace mount volume, /opt/ibm/wlp/stdev must
     * be tracked and added later (switching to loose config mode) when the new container is created. This mount volume is not added
     * as part of adding an external module to the server. It is added separately as a mandatory volume and tracked and saved in the server properties.
     * Implied, is that the container, because of the existing mount volume, is already enabled for loose config in the other workspace, but, it is
     * not enabled yet for the current workspace. Therefore, the apps in the other workspace already have loose config XML files.
     *
     * We are not publicizing how we create the mount volume (use of /opt/ibm/wlp/usr) to support loose config, so a user will likely not
     * add this specific mount volume themselves. However, if this contrived case is encountered, then we must create a new container with a new user directory.
     *
     *
     * @param container
     * @param workspacePath
     * @return
     */
    public static MountProperty checkContainerForLooseConfigMountVolume(BaseDockerContainer container, Map<String, String> serviceInfo, IPath workspacePath) {
        // Get the current usr mount volume of the running container
        EnumSet<MountProperties> mountVolumeDescription = EnumSet.noneOf(MountProperties.class);
        try {
            IPath usrMount = BaseDockerContainer.getContainerToLocalPath(new Path(container.getMountSourceForDestination(getLibertyUsrPath(serviceInfo))));
            if (usrMount != null) {
                if (FileUtil.getCanonicalPath(usrMount.toString()).startsWith(FileUtil.getCanonicalPath(workspacePath).toString())) {
                    mountVolumeDescription.add(MountProperties.SAME_USR_MOUNT);
                } else {
                    mountVolumeDescription.add(MountProperties.OTHER_USR_MOUNT);
                }
            }

            // There can be multiple workspace mount volumes from many workspaces, so we have to check each one
            IPath stdevMount = null;
            boolean matchesCurrentWorkspace = false;
            int i = 0;
            String base = getLibertyStdevPath(serviceInfo);
            do {
                String stdevString = container.getMountSourceForDestination(base + (i == 0 ? "" : Integer.toString(i)));
                if (stdevString == null) {
                    break;
                }
                stdevMount = BaseDockerContainer.getContainerToLocalPath(new Path(stdevString));
                // The container already has a mount to 'a' workspace
                if (stdevMount != null && FileUtil.getCanonicalPath(stdevMount.toString()).startsWith(FileUtil.getCanonicalPath(workspacePath).toString())) {
                    matchesCurrentWorkspace = true; // Here, the container uses the same usr folder and workspace as the current environment
                    break;
                }
                i++;
            } while (stdevMount != null); // if there are no more mount paths, then stop

            if (matchesCurrentWorkspace) {
                // The container has a mount to a workspace that is the same as this current eclipse workspace.  It is compatible
                mountVolumeDescription.add(MountProperties.SAME_WORKSPACE_MOUNT);
            } else {
                // The container has a mount to another path or workspace that is not the same as this current eclipse workspace.
                mountVolumeDescription.add(MountProperties.OTHER_WORKSPACE_MOUNT);
            }

        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.logError("Failed to determine if the " + container.getContainerName() + " container is already set up for running applications directly from the workspace",
                               e);
            }
        }
        MountProperty prop = MountProperty.NO_USR_MOUNT; // Default
        if (mountVolumeDescription.contains(MountProperties.SAME_USR_MOUNT) && mountVolumeDescription.contains(MountProperties.SAME_WORKSPACE_MOUNT)) {
            prop = MountProperty.SAME_USR_MOUNT; // Reuse the container.
        }
        if (mountVolumeDescription.contains(MountProperties.OTHER_USR_MOUNT) && mountVolumeDescription.contains(MountProperties.OTHER_WORKSPACE_MOUNT)) {
            prop = MountProperty.OTHER_USR_MOUNT;
        }
        // If it was a 'contrived' situation where it is the same user mount but other workspace mount (or none), then treat it like there is no usr mount - so
        // a new container and a new usr project folder will be created.
        return prop;
    }
}
