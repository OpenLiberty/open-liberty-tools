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
package com.ibm.ws.st.docker.core.internal.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.common.core.ext.internal.util.FileUtil;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.PromptAction;
import com.ibm.ws.st.core.internal.PromptHandler;
import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.docker.core.internal.AbstractServerCleanupHandler;
import com.ibm.ws.st.docker.core.internal.Activator;
import com.ibm.ws.st.docker.core.internal.Messages;
import com.ibm.ws.st.docker.core.internal.NewContainerPrompt;
import com.ibm.ws.st.docker.core.internal.Trace;

/**
 * Liberty Local Docker Server Implementation
 */

public class LibertyDockerServer extends AbstractServerExtension {

    // Properties for persisting the current run status.  The tools create a new
    // image and container when the user switches between run modes (so that extra
    // ports can be mapped and the container command changed) or loose config modes.  The old container
    // and image are also removed if they were created by the tools.
    private static final String IS_USER_CONTAINER_KEY = "isUserContainer";
    private static final String CURRENT_MODE_KEY = "currentMode";
    private static final String CONTAINER_NAME_KEY = "containerName";
    private static final String CURRENT_LOOSE_CONFIG_MODE = com.ibm.ws.st.common.core.ext.internal.Constants.LOOSE_CFG;
    private static final String ADDITIONAL_VOLUMES = "additionalVolumes";
    private static final String MANDATORY_VOLUMES = "mandatoryVolumes";
    private static final String RUN_PROPERTIES_FILE = "dockerRun.properties";
    private Properties runProperties = null;
    private long runPropertiesTimestamp = 0;

    @Override
    public String[] getServiceInfoKeys() {
        return new String[] { com.ibm.ws.st.common.core.ext.internal.Constants.HOSTNAME,
                              com.ibm.ws.st.common.core.ext.internal.Constants.OS_NAME,
                              com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_MACHINE_TYPE,
                              com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_MACHINE,
                              com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_CONTAINER,
                              com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_IMAGE,
                              com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_RUNTIME_INSTALL_PATH,
                              com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_NAME,
                              com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH,
                              com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD,
                              com.ibm.ws.st.common.core.ext.internal.Constants.LOOSE_CFG,
                              com.ibm.ws.st.common.core.ext.internal.Constants.SSH_KEY,
                              com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER,
                              com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD,
                              com.ibm.ws.st.common.core.ext.internal.Constants.DEBUG_PORT };
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup(IServer server) {
        WebSphereServer wsServer = getWebSphereServer(server);
        if (wsServer != null && !isUserContainer(wsServer)) {
            BaseDockerContainer container = null;
            String containerName = "unknown";
            try {
                container = getContainer(wsServer);
                if (container != null) {
                    containerName = container.getContainerName();
                    AbstractServerCleanupHandler handler = Activator.getServerCleanupHandler();
                    if (handler != null) {
                        handler.handleServerDelete(container, loadRunProperties(wsServer));
                    }
                }
            } catch (Exception e) {
                Trace.logError("Could not get base url for container: " + containerName, e);
            }
        }
        super.cleanup(server);
    }

    /** {@inheritDoc} */
    @Override
    public String getServerDisplayName(IServer server) {
        WebSphereServer wsServer = getWebSphereServer(server);
        BaseDockerContainer container = null;
        String containerName = "unknown";
        if (wsServer != null) {
            try {
                container = getContainer(wsServer);
                if (container != null) {
                    containerName = container.getContainerName();
                    String machineName = container.getDockerMachine().getMachineName();
                    if (machineName != null) {
                        return NLS.bind(Messages.dockerServerDisplayName, new String[] { container.getContainerName(), machineName });
                    }
                    return container.getContainerName();
                }
            } catch (Exception e) {
                Trace.logError("Could not display server name for container: " + containerName, e);
            }
            return wsServer.getServerName();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getBaseURL(IServer server, int port) {
        int mappedPort = -1;
        String mappedHost = null;
        BaseDockerContainer container = null;
        String containerName = "unknown";
        try {
            container = getContainer(server);
            if (container != null) {
                containerName = container.getContainerName();
                String portMapping = container.getPortMapping(Integer.toString(port));
                String portString = BaseDockerContainer.getPortFromMapping(portMapping);
                if (SocketUtil.isLocalhost(server.getHost())) {
                    // This is for localhost only.  For remote Docker the connection
                    // is directly to the host and not the Docker machine.
                    mappedHost = BaseDockerContainer.getHostFromMapping(portMapping);
                    if (mappedHost == null) {
                        mappedHost = container.getDockerMachine().getHost();
                    }
                }
                mappedPort = Integer.parseInt(portString);
            }
        } catch (Exception e) {
            Trace.logError("Could not get base url for container: " + containerName, e);
        }

        if (mappedHost == null) {
            mappedHost = server.getHost();
        }
        if (mappedPort == -1) {
            mappedPort = port;
        }

        if (Trace.ENABLED) {
            String name = container != null ? container.getContainerName() : "unknown";
            Trace.trace(Trace.INFO, "The " + name + " container port  " + port + " mapped to host " + mappedHost + " and port " + mappedPort);
        }

        return super.getBaseURL(server, mappedHost, mappedPort);
    }

    /** {@inheritDoc} */
    @Override
    public boolean requiresRemoteStartSettings(IServer server) {
        return false;
    }

    public boolean isUserContainer(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        String isUserContainer = properties.getProperty(IS_USER_CONTAINER_KEY);
        return Boolean.parseBoolean(isUserContainer);
    }

    public boolean getCurrentLooseConfigMode(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        String isLooseConfigContainer = properties.getProperty(CURRENT_LOOSE_CONFIG_MODE);
        return Boolean.parseBoolean(isLooseConfigContainer);
    }

    public String getCurrentMode(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        return properties.getProperty(CURRENT_MODE_KEY);
    }

    public String getContainerName(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        return properties.getProperty(CONTAINER_NAME_KEY);
    }

    public void setLooseConfigEnabled(boolean isLooseConfig, WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        properties.setProperty(CURRENT_LOOSE_CONFIG_MODE, isLooseConfig ? "true" : "false");
        storeRunProperties(server);
    }

    public void setCurrentRunStatus(String containerName, String mode, boolean isUser, boolean isLooseConfig, WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        properties.setProperty(CONTAINER_NAME_KEY, containerName);
        properties.setProperty(CURRENT_MODE_KEY, mode);
        properties.setProperty(IS_USER_CONTAINER_KEY, isUser ? "true" : "false");
        properties.setProperty(CURRENT_LOOSE_CONFIG_MODE, isLooseConfig ? "true" : "false");
        storeRunProperties(server);
    }

    /**
     * Get the current set of additional volumes needed for supporting applications
     * where the location of the application files is not in the workspace.
     *
     * @param server
     * @return
     */
    public List<String> getAdditionalVolumes(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        String volumes = properties.getProperty(ADDITIONAL_VOLUMES);
        if (volumes == null || volumes.isEmpty()) {
            return Collections.emptyList();
        }
        String[] volumeArray = volumes.split(File.pathSeparator);
        return Arrays.asList(volumeArray);
    }

    public List<String> getMandatoryVolumes(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        String volumes = properties.getProperty(MANDATORY_VOLUMES);
        if (volumes == null || volumes.isEmpty()) {
            return Collections.emptyList();
        }
        String[] volumeArray = volumes.split(File.pathSeparator);
        return Arrays.asList(volumeArray);
    }

    /**
     * Add an additional volume. Since preModifyModules is called before the applications
     * are added to the server, the required additional volumes need to be saved somewhere
     * so that when the new container is created the correct volumes get added.
     *
     * @param server
     * @param newVolume
     */
    public void addAdditionalVolume(WebSphereServer server, String newVolume) {
        Properties properties = loadRunProperties(server);
        String volumes = properties.getProperty(ADDITIONAL_VOLUMES);
        if (volumes == null || volumes.isEmpty()) {
            volumes = newVolume;
        } else {
            String[] volumeArray = volumes.split(File.pathSeparator);
            for (String volume : volumeArray) {
                if (newVolume.equals(volume)) {
                    return;
                }
            }
            volumes = volumes + File.pathSeparator + newVolume;
        }
        properties.setProperty(ADDITIONAL_VOLUMES, volumes);
        storeRunProperties(server);
    }

    /**
     * Set the additional volumes. Used when switching from non-loose config mode to
     * loose config mode. The volumes are recalculated based on the currently deployed
     * modules.
     *
     * @param server
     * @param newVolumes
     */
    public void setAdditionalVolumes(WebSphereServer server, Collection<IPath> newVolumes) {
        if (newVolumes == null || newVolumes.isEmpty()) {
            clearAdditionalVolumes(server);
            return;
        }
        Properties properties = loadRunProperties(server);
        String volumes = null;
        for (IPath newVolume : newVolumes) {
            if (volumes == null) {
                volumes = newVolume.toOSString();
            } else {
                volumes = volumes + File.pathSeparator + newVolume.toOSString();
            }
        }
        properties.setProperty(ADDITIONAL_VOLUMES, volumes);
        storeRunProperties(server);
    }

    public void addMandatoryVolumes(WebSphereServer server, Collection<IPath> newVolumes) {
        Properties properties = loadRunProperties(server);
        String volumes = null;
        for (IPath newVolume : newVolumes) {
            if (volumes == null) {
                volumes = newVolume.toOSString();
            } else {
                volumes = volumes + File.pathSeparator + newVolume.toOSString();
            }
        }
        properties.setProperty(MANDATORY_VOLUMES, volumes);
        storeRunProperties(server);
    }

    /**
     * Clear all of the additional volumes.
     *
     * @param server
     */
    public void clearAdditionalVolumes(WebSphereServer server) {
        Properties properties = loadRunProperties(server);
        String volumes = properties.getProperty(ADDITIONAL_VOLUMES);
        if (volumes != null) {
            properties.remove(ADDITIONAL_VOLUMES);
            storeRunProperties(server);
        }
    }

    @Override
    public String getConnectionPort(IServer server, String port) {
        BaseDockerContainer container = null;
        String mappedPort = null;
        try {
            container = getContainer(server);
            mappedPort = container.getHostMappedPort(port);
        } catch (Exception e) {
            if (Trace.ENABLED) {
                String name = container != null ? container.getContainerName() : "unknown";
                Trace.logError("Failed to get mapped port for the " + name + " container and port " + port, e);
            }
        }
        if (mappedPort == null) {
            mappedPort = port;
        }
        return mappedPort;
    }

    @Override
    public String getConnectionHost(IServer server, String host, String port) {
        if (!SocketUtil.isLocalhost(host)) {
            // For remote Docker, the connection is directly to the host rather
            // than the Docker machine
            return host;
        }
        String mappedHost = null;
        BaseDockerContainer container = null;
        try {
            container = getContainer(server);
            mappedHost = container.getHostMappedIP(port);
        } catch (Exception e) {
            if (Trace.ENABLED) {
                String name = container != null ? container.getContainerName() : "unknown";
                Trace.logError("Failed to get mapped IP for the " + name + " container and port " + port, e);
            }
        }
        if (mappedHost == null) {
            mappedHost = host;
        }
        return mappedHost;
    }

    private Properties loadRunProperties(WebSphereServer server) {
        IPath path = getRunPropertiesPath(server);
        if (path == null) {
            if (runProperties == null) {
                // Server is being created, just return default properties for now
                Properties properties = new Properties();
                setDefaultProperties(properties, server);
                return properties;
            }
            Trace.logError("Could not calculate the Docker run properties path for server: " + server.getServerName(), null);
            return runProperties;
        }

        synchronized (server.getServer()) {
            // Check if the file has been updated
            if (runProperties != null) {
                File file = path.toFile();
                if (file.exists() && file.lastModified() > runPropertiesTimestamp) {
                    runProperties = null;
                }
            }

            if (runProperties == null) {
                runProperties = new Properties();
                File file = path.toFile();
                if (file.exists()) {
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                        runProperties.load(inputStream);
                        runPropertiesTimestamp = file.lastModified();
                    } catch (Exception e) {
                        Trace.logError("Could not load the properties file: " + file.getAbsolutePath(), e);
                        setDefaultProperties(runProperties, server);
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                } else {
                    setDefaultProperties(runProperties, server);
                }
            }
        }
        return runProperties;
    }

    private void storeRunProperties(WebSphereServer server) {
        if (runProperties == null) {
            return;
        }
        IPath path = getRunPropertiesPath(server);
        if (path == null) {
            Trace.logError("Could not calculate the Docker run properties path for server: " + server.getServerName(), null);
            return;
        }
        synchronized (server.getServer()) {
            File file = path.toFile();
            OutputStream outputStream = null;
            try {
                if (!path.toFile().exists()) {
                    // First make sure directories are created
                    IPath dir = path.removeLastSegments(1);
                    if (!dir.toFile().mkdirs()) {
                        Trace.logError("Failed to create directory structure for run properties: " + path.toOSString(), null);
                        return;
                    }
                    if (!file.createNewFile()) {
                        Trace.logError("Failed to create file for persisting run properties: " + file.getAbsolutePath(), null);
                        return;
                    }
                }
                outputStream = new FileOutputStream(file);
                runProperties.store(outputStream, null);
            } catch (Exception e) {
                Trace.logError("Could not store the properties file: " + file.getAbsolutePath(), e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }

    private void setDefaultProperties(Properties properties, WebSphereServer server) {
        Map<String, String> serviceInfo = server.getServiceInfo();
        properties.setProperty(CONTAINER_NAME_KEY, serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_CONTAINER));
        properties.setProperty(IS_USER_CONTAINER_KEY, "true");
        properties.setProperty(CURRENT_MODE_KEY, ILaunchManager.RUN_MODE);
        properties.setProperty(CURRENT_LOOSE_CONFIG_MODE, "false");
    }

    private IPath getRunPropertiesPath(WebSphereServer server) {
        WebSphereServerInfo serverInfo = server.getServerInfo();
        if (serverInfo != null) {
            IPath path = serverInfo.buildMetadataDirectoryPath();
            return path.append(RUN_PROPERTIES_FILE);
        }
        return null;
    }

    public BaseDockerContainer getContainer(WebSphereServer server) throws UnsupportedServiceException {
        Map<String, String> serviceInfo = server.getServiceInfo();
        if (serviceInfo != null) {
            String containerName = getContainerName(server);
            if (containerName != null)
                serviceInfo.put(Constants.DOCKER_CONTAINER, containerName);
            IPlatformHandler handler = PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.DOCKER);
            if (handler instanceof BaseDockerContainer)
                return (BaseDockerContainer) handler;
        }
        return null;
    }

    private WebSphereServer getWebSphereServer(IServer server) {
        return server.getAdapter(WebSphereServer.class);
    }

    private BaseDockerContainer getContainer(IServer server) throws UnsupportedServiceException {
        WebSphereServer wsServer = getWebSphereServer(server);
        if (wsServer != null) {
            return getContainer(wsServer);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus preModifyModules(IServer server, IModule[] add, IModule[] remove, IProgressMonitor monitor) {
        // If no new applications then nothing needs to be done
        if (add == null || add.length == 0) {
            return Status.OK_STATUS;
        }

        WebSphereServer wsServer = server.getAdapter(WebSphereServer.class);
        if (wsServer == null) {
            // This should never happen so just log an error if it does and return Ok
            Trace.logError("Failed to adapt the " + server.getName() + " server to a WebSphereServer.", null);
            return Status.OK_STATUS;
        }

        // If not in loose config mode then don't need to do anything
        if (!wsServer.isLooseConfigEnabled()) {
            return Status.OK_STATUS;
        }

        WebSphereServerBehaviour wsBehaviour = wsServer.getWebSphereServerBehaviour();
        if (wsBehaviour != null) {
            LibertyDockerServerBehaviour extBehaviour = (LibertyDockerServerBehaviour) wsBehaviour.getAdapter(LibertyDockerServerBehaviour.class);
            if (extBehaviour != null) {
                try {
                    // Determine if any of the applications being added have locations outside of the
                    // workspace and if the location is not covered by an existing volume
                    BaseDockerContainer container = getContainer(server);
                    Map<IPath, IPath> volumeHash = extBehaviour.getPathMappingHash(wsBehaviour);
                    if (container != null && volumeHash != null) {
                        Set<IModule> modulesNeedingVolumes = new HashSet<IModule>();
                        Set<IPath> newVolumes = new HashSet<IPath>();
                        for (IModule module : add) {
                            Set<IPath> locations = LibertyDockerRunUtility.getLocations(wsServer, module);
                            for (IPath location : locations) {
                                boolean found = false;
                                for (IPath existingPath : volumeHash.keySet()) {
                                    IPath basePath = FileUtil.getCanonicalPath(existingPath);
                                    if (basePath.isPrefixOf(location)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    if (Trace.ENABLED) {
                                        Trace.trace(Trace.INFO, "No existing volume on the " + container.getContainerName() + " container was found for: " + location);
                                    }
                                    modulesNeedingVolumes.add(module);
                                    LibertyDockerRunUtility.mergeModuleLocation(newVolumes, location);
                                }
                            }
                        }

                        if (!newVolumes.isEmpty()) {
                            // If a volume does not already exist, ask the user if they want to create a new container
                            NewContainerPrompt prompt = new NewContainerPrompt(NLS.bind(Messages.dockerNewVolumesPromptDetails, formatModules(modulesNeedingVolumes)));
                            prompt.getActionHandler().prePromptAction(null, null, new NullProgressMonitor());

                            PromptHandler promptHandler = com.ibm.ws.st.core.internal.Activator.getPromptHandler();
                            if (promptHandler != null && !PromptUtil.isSuppressDialog()) {
                                IPromptResponse response = promptHandler.getResponse(Messages.dockerNewVolumesPromptTitle, new PromptHandler.AbstractPrompt[] { prompt },
                                                                                     PromptHandler.STYLE_QUESTION | PromptHandler.STYLE_CANCEL);

                                if (response == null) {
                                    return Status.CANCEL_STATUS;
                                } else if (!(response.getSelectedAction(prompt.getIssues()[0]) == (PromptAction.YES))) {
                                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.dockerNewVolumesPromptError,
                                                                                                   formatModules(modulesNeedingVolumes)), null);
                                }
                            }

                            // If response is yes or code is called headlessly then add the volumes and restart
                            for (IPath path : newVolumes) {
                                addAdditionalVolume(wsServer, path.toOSString());
                            }
                            IStatus status = extBehaviour.restartServer(wsBehaviour, Messages.dockerUpdateContainerVolumesMsg, monitor);
                            return status;
                        }
                    }
                } catch (Exception e) {
                    return Status.CANCEL_STATUS;
                }
            }
        }
        return Status.OK_STATUS;
    }

    private String formatModules(Collection<IModule> modules) {
        String[] names = new String[modules.size()];
        int i = 0;
        for (IModule module : modules) {
            names[i++] = module.getName();
        }
        Arrays.sort(names);
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            builder.append("\n    ");
            builder.append(name);
        }
        return builder.toString();
    }
}
