/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.docker;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.ws.st.common.core.ext.internal.AbstractServerSetup;
import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.ServerSetupFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.core.tests.StandaloneLibertyTestExt;
import com.ibm.ws.st.core.tests.remote.RemoteTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility.MountProperty;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerServer;
import com.ibm.ws.st.docker.ui.internal.wizard.LibertyDockerUtil;

/**
 *
 */
public class LibertyDockerTestExt extends StandaloneLibertyTestExt {
    private static final String HTTPS_PORT = "9443";

    /** {@inheritDoc} */
    @Override
    public IServer createServer(IRuntime runtime, Map<String, String> serverInfo) throws Exception {
        addWorkspaceSettings();

        if (runtime == null)
            throw new Exception("Error: runtime cannot be null");

        AbstractDockerMachine machine = DockerTestUtil.getDockerMachine();
        BaseDockerContainer container = null;

        String containerName = serverInfo.get(Constants.DOCKER_CONTAINER);
        if (containerName != null) {
            container = DockerTestUtil.getExistingContainer(machine, containerName);
        }

        if (container == null) {
            container = DockerTestUtil.getDockerContainer(machine);
        }
        assertNotNull("The docker container is null, this typically happens if the docker-machine isn't running", container);

        Map<String, String> connectionInfo = RemoteTestUtil.getConnectionInfo();
        String hostname = connectionInfo.get(Constants.HOSTNAME);
        if (hostname == null || hostname.isEmpty()) {
            hostname = "localhost";
        }

        Map<String, String> serviceInfo = LibertyDockerUtil.getServiceInfo(container, null, hostname, HTTPS_PORT);
        serviceInfo.putAll(connectionInfo);

        String username = DockerTestUtil.getDockerUsername();
        String password = DockerTestUtil.getDockerPassword();

        dockerServerSetup(runtime, serviceInfo, container);

        String connectionHost = null;
        if (SocketUtil.isLocalhost(hostname)) {
            // This is for localhost only.  For remote Docker the connection is made
            // directly to the host.
            connectionHost = container.getDockerMachine().getHost();
        }
        if (connectionHost == null) {
            connectionHost = hostname;
        }
        JMXConnection jmxConnection = RemoteTestUtil.setJMXConnection(connectionHost, container.getHostMappedPort(HTTPS_PORT), username, password);
        String serverName = RemoteUtils.getServerName(jmxConnection);

        String looseCfgProp = serverInfo.get(Constants.LOOSE_CFG);
        boolean isLooseCfg = looseCfgProp != null && Boolean.parseBoolean(looseCfgProp);

        IServer server = setDockerServerAttributes(runtime, serverName, serviceInfo, hostname, HTTPS_PORT, username, password, isLooseCfg);
        RemoteTestUtil.downloadRemoteServerFiles(runtime, server, jmxConnection);

        if (isLooseCfg) {
            setupLooseConfig(server, container, serverInfo, serviceInfo);
        }
        return server;
    }

    private void setupLooseConfig(IServer server, BaseDockerContainer container, Map<String, String> serverInfo, Map<String, String> serviceInfo) throws Exception {
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);

        IPath workspacePath = wsServer.getWebSphereRuntime().getProject().getWorkspace().getRoot().getLocation();
        workspacePath = BaseDockerContainer.getLocalToContainerPath(workspacePath);

        // Setting up a loose config container requires both serviceInfo and serverInfo
        Map<String, String> looseCfgContainerInfo = new HashMap<String, String>();
        looseCfgContainerInfo.putAll(serverInfo);
        looseCfgContainerInfo.putAll(serviceInfo);

        MountProperty existingVolumeStatus = DockerTestUtil.getUsrMountProperty();

        // Stop the old container, create a new one with the loose config mounts, and get the new container name
        String looseCfgContainerName = LibertyDockerRunUtility.setupNewContainer(wsServer.getUserDirectory(), workspacePath, container, looseCfgContainerInfo, wsServer,
                                                                                 existingVolumeStatus, null);
        LibertyDockerServer serverExt = (LibertyDockerServer) wsServer.getAdapter(LibertyDockerServer.class);
        serverExt.setCurrentRunStatus(looseCfgContainerName, ILaunchManager.RUN_MODE, false, true, wsServer);
    }

    private void addWorkspaceSettings() {
        System.setProperty("wtp.autotest.noninteractive", "true");
        Activator.setPreference("headless.auto.accept.cert", "true");
    }

    private IServer setDockerServerAttributes(IRuntime runtime, String serverName, Map<String, String> serviceInfo, String hostname, String port, String userName,
                                              String password, boolean isLooseCfg) throws CoreException {
        IServerType st = ServerCore.findServerType(WLPCommonUtil.SERVER_ID);
        IServerWorkingCopy wc = st.createServer(hostname, null, runtime, null);
        wc.setHost(hostname);

        WebSphereServer wsServer = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);
        wsServer.setServerName(serverName);
        wsServer.setLooseConfigEnabled(isLooseCfg);
        wsServer.setStopTimeout(120);
        wsServer.setServerUserName(userName);
        wsServer.setServerPassword(password);
        wsServer.setServerSecurePort(port);

        String dockerInstallPath = serviceInfo.get(LibertyDockerUtil.LIBERTY_RUNTIME_INSTALL_PATH);
        String dockerConfigPath = serviceInfo.get(LibertyDockerUtil.LIBERTY_SERVER_CONFIG_PATH);

        // For remote docker, remote start is always enabled since a remote docker
        // server cannot be created unless remote logon in set up.
        wsServer.setIsRemoteServerStartEnabled(!SocketUtil.isLocalhost(hostname));
        String osName = serviceInfo.get(Constants.OS_NAME);
        int platform = RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX;
        if (osName != null) {
            if (osName.contains("win"))
                platform = RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS;
            else if (osName.contains("mac"))
                platform = RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC;
        }

        wsServer.setRemoteServerStartPlatform(platform);
        wsServer.setRemoteServerStartRuntimePath(dockerConfigPath.replace("\\", "/"));
        wsServer.setRemoteServerStartConfigPath(dockerInstallPath.replace("\\", "/"));
        String logonMethod = serviceInfo.get(Constants.LOGON_METHOD);
        if (Constants.LOGON_METHOD_SSH.equals(logonMethod)) {
            wsServer.setRemoteServerStartLogonMethod(RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_SSH);
            String keyFile = serviceInfo.get(Constants.SSH_KEY);
            String osId = serviceInfo.get(Constants.OS_USER);
            String osPassword = serviceInfo.get(Constants.OS_PASSWORD);
            if (keyFile != null && osId != null && osPassword != null) {
                wsServer.setRemoteServerStartSSHKeyFile(keyFile);
                wsServer.setRemoteServerStartSSHId(osId);
                wsServer.setRemoteServerStartSSHPassphrase(osPassword);
            }
        } else {
            wsServer.setRemoteServerStartLogonMethod(RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS);
            String osId = serviceInfo.get(Constants.OS_USER);
            String osPassword = serviceInfo.get(Constants.OS_PASSWORD);
            if (osId != null && osPassword != null) {
                wsServer.setRemoteServerStartOSId(osId);
                wsServer.setRemoteServerStartOSPassword(osPassword);
            }
        }

        wsServer.setServerType("LibertyDocker");
        wsServer.setServiceInfo(serviceInfo);

        WLPCommonUtil.print("Server running looseConfig: " + wsServer.isLooseConfigEnabled());
        IServer server = wc.save(true, null);
        return server;
    }

    /* Create a docker server using product code */
    private static void dockerServerSetup(IRuntime runtime, Map<String, String> serviceInfo, BaseDockerContainer container) {

        WLPCommonUtil.print("Starting setup of docker server...");
        AbstractServerSetup serverSetup = null;
        try {
            serverSetup = ServerSetupFactory.getServerSetup("LibertyDockerLocal", serviceInfo, runtime);
            WLPCommonUtil.print("Creating basic registry");
            createBasicRegistry(container, serviceInfo.get(LibertyDockerUtil.LIBERTY_SERVER_CONFIG_PATH));
            if (serverSetup != null) {
                serverSetup.setup(null);
            }

            //Need to wait for the JMX feature to be installed
            WLPCommonUtil.print("Installing the restConnector-1.0 feature.");

            int attempt = 0;
            while (attempt <= 3) {
                String logs = container.getLogs();
                boolean containerReady = logs.contains("The server installed the following features") && logs.contains("restConnector");
                if (containerReady)
                    break;
                WLPCommonUtil.wait("Wait an additional second...", 1000);
                attempt++;
            }

        } catch (Exception e) {
            Trace.logError("Failed to create LibertyDockerLocal server setup.", e);
        }
    }

    /*
     * Create a basic user registry or use an existing one
     * Case 1) If a new container was created, create the basic registry
     * Case 2) If an existing container is used without the basic registry, create it
     * Case 3) If an existing container is used with basic registry, use the specified username and password
     */
    private static void createBasicRegistry(IPlatformHandler handler, String dockerConfigPath) {
        String username = DockerTestUtil.getDockerUsername();
        String password = DockerTestUtil.getDockerPassword();

        Path serverConfigPath = new Path(dockerConfigPath);
        String defaultsConfigDropinsPath = serverConfigPath.append(LibertyDockerUtil.DROPINS_DIR).append(LibertyDockerUtil.DEFAULTS_DIR).toString();
        String basicRegistryXML = defaultsConfigDropinsPath + "/" + Constants.LIBERTY_OLD_BASIC_REGISTRY_NAME;
        String basicRegistryXMLNew = defaultsConfigDropinsPath + "/" + Constants.LIBERTY_BASIC_REGISTRY_NAME;

        try {
            if (!handler.directoryExists(defaultsConfigDropinsPath)) {
                handler.createDirectory(defaultsConfigDropinsPath);
                handler.uploadFile(ConfigUtils.createBasicRegConfig(username, password).getAbsolutePath(), basicRegistryXMLNew);
            }

            else {
                if (handler.fileExists(basicRegistryXML)) {
                    handler.deleteFile(basicRegistryXML);
                }
                if (!handler.fileExists(basicRegistryXMLNew)) {
                    handler.uploadFile(ConfigUtils.createBasicRegConfig(username, password).getAbsolutePath(), basicRegistryXMLNew);
                }
            }
        } catch (Exception e) {
            Trace.logError("Error creating the basic user registry", e);
        }
    }

    @Override
    public void cleanUp() {
        WLPCommonUtil.cleanUp();
    }

    @Override
    public void cleanUp(IServer server) {
        // Get the container name from the server before the server is cleaned up
        String containerName = null;
        if (!DockerTestUtil.isContainerSpecified()) {
            containerName = DockerTestUtil.getContainerName(server);
        }

        WLPCommonUtil.cleanUp();

        // Remove the container if a new one was created during test (it was not
        // specified in the properties)
        if (containerName != null) {
            try {
                DockerTestUtil.getDockerMachine().removeContainer(containerName, true);
            } catch (Exception e) {
                Trace.logError("Error removing the container: " + containerName, e);
            }
        }
    }

}
