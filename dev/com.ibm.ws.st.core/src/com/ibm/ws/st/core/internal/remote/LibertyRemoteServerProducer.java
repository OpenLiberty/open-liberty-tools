/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal.remote;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.producer.AbstractServerProducer;
import com.ibm.ws.st.common.core.ext.internal.producer.ServerCreationException;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.jmx.JMXConnectionException;

public class LibertyRemoteServerProducer extends AbstractServerProducer {

    /** {@inheritDoc} */
    @Override
    public IServer createServer(IRuntime rt, Map<String, String> serverInfo) throws ServerCreationException {
        if (rt == null)
            throw new ServerCreationException(Messages.libertyProducerRuntimeNull);

        validateServerInfo(serverInfo);

        IServer server = null;

        ArrayList<IPath> downloadedFiles = new ArrayList<IPath>(2);

        String hostname = serverInfo.get(Constants.HOSTNAME);
        String serverLabel = serverInfo.get(Constants.SERVER_LABEL);
        String libertyUser = serverInfo.get(Constants.LIBERTY_USER);
        String libertyPassword = serverInfo.get(Constants.LIBERTY_PASSWORD);
        String libertyHTTPSPort = serverInfo.get(Constants.LIBERTY_HTTPS_PORT);
        String debugPort = serverInfo.get(Constants.DEBUG_PORT);

        String serverConfigDir = serverInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH);
        serverConfigDir = serverConfigDir.replace("\\", "/");
        String serverName = serverConfigDir.substring(serverConfigDir.lastIndexOf('/') + 1, serverConfigDir.length());
        String installPath = serverInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH);

        // check existing servers
        IServer[] servers = ServerCore.getServers();
        for (IServer sr : servers) {
            if (sr.getId().equals(serverName)) {
                throw new ServerCreationException(NLS.bind(Messages.libertyProducerServerExists, serverName));
            }
        }

        JMXConnection jmxConnection = new JMXConnection(hostname, libertyHTTPSPort, libertyUser, libertyPassword);
        try {
            jmxConnection.connect();
        } catch (JMXConnectionException e) {
            throw new ServerCreationException(Messages.remoteJMXConnectionFailure, e);
        }

        try {
            // only non-loose config supported for remote servers
            boolean isLC = false;

            // Get the value for remote the user directory ${wlp.user.dir}
            CompositeData userDirMetadata = (CompositeData) jmxConnection.getMetadata("${wlp.user.dir}", "a");
            String remoteUserPath = (String) userDirMetadata.get("fileName");
            remoteUserPath = remoteUserPath.replace("\\", "/");

            WebSphereRuntime wrt = (WebSphereRuntime) rt.loadAdapter(WebSphereRuntime.class, null);

            IRuntimeWorkingCopy runtimeWorkingCopy = rt.createWorkingCopy();
            WebSphereRuntime wRuntimeWorkingCopy = (WebSphereRuntime) runtimeWorkingCopy.loadAdapter(WebSphereRuntime.class, null);

            //create default user directory which is wlp.user.dir inside the runtime
            wRuntimeWorkingCopy.createDefaultUserDirectory(null);

            // create new user directory for remote case with different name than the default user directory
            IProject remoteUsrProject;
            remoteUsrProject = WebSphereUtil.createUserProject(RemoteUtils.generateRemoteUsrDirName(wrt), null, null);
            IPath outputPath = remoteUsrProject.getLocation().append(com.ibm.ws.st.core.internal.Constants.SERVERS_FOLDER);
            UserDirectory userDir = new UserDirectory(wrt, remoteUsrProject.getLocation(), remoteUsrProject, outputPath, new Path(remoteUserPath));

            String localUserPath = (wrt.getRemoteUsrMetadataPath().toOSString()).replace("\\", "/");
            //download all config and include files
            IPath userMetaDataPath = wrt.getRemoteUsrMetadataPath();
            RemoteUtils.downloadServerFiles(jmxConnection, userMetaDataPath, serverName, downloadedFiles, remoteUserPath, localUserPath);
            RemoteUtils.moveDownloadedFilesToUserDir(userDir, downloadedFiles, wrt, serverName);

            String serverTypeId = rt.getId().endsWith(com.ibm.ws.st.core.internal.Constants.V85_ID_SUFFIX) ? com.ibm.ws.st.core.internal.Constants.SERVERV85_TYPE_ID : com.ibm.ws.st.core.internal.Constants.SERVER_TYPE_ID;
            IServerType st = ServerCore.findServerType(serverTypeId);
            IServerWorkingCopy wc;
            wc = st.createServer(hostname, null, rt, null);

            wc.setHost(hostname);
            wc.setName(serverLabel);

            boolean isRemoteServerStartEnabled = true;

            String osUser = serverInfo.get(Constants.OS_USER);

            String osPassword = serverInfo.get(Constants.OS_PASSWORD);

            String sshKeyFile = serverInfo.get(Constants.SSH_KEY);

            WebSphereServer wsServer = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);
            wsServer.setDefaults(new NullProgressMonitor());
            wsServer.setServerName(serverName);
            wsServer.setUserDir(userDir);
            wsServer.setLooseConfigEnabled(isLC);
            wsServer.setStopTimeout(60);
            wsServer.setServerUserName(libertyUser);
            wsServer.setServerPassword(libertyPassword);
            wsServer.setServerSecurePort(libertyHTTPSPort);
            if (isRemoteServerStartEnabled) {
                wsServer.setIsRemoteServerStartEnabled(isRemoteServerStartEnabled);
                int platform = serverInfo.get(Constants.OS_NAME).contains("windows") ? RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS : RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER;
                wsServer.setRemoteServerStartPlatform(platform);
                wsServer.setRemoteServerStartRuntimePath(installPath.replace("\\", "/"));
                wsServer.setRemoteServerStartConfigPath(serverConfigDir.replace("\\", "/"));
                int logonMethod = sshKeyFile == null ? RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS : RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_SSH;
                wsServer.setRemoteServerStartLogonMethod(logonMethod);
                wsServer.setRemoteServerStartOSId(osUser);
                wsServer.setRemoteServerStartOSPassword(osPassword);
                if (sshKeyFile != null) {
                    wsServer.setRemoteServerStartSSHKeyFile(sshKeyFile.replace("\\", "/"));
                }
                wsServer.setRemoteServerStartDebugPort(debugPort);
            }

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Create remote server with looseConfig: " + wsServer.isLooseConfigEnabled());

            server = wc.save(true, null);
            wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);

            if (wsServer.getServerInfo() == null) {
                // If the user directory doesn't exist we should add it
                boolean found = false;
                for (UserDirectory usr : wRuntimeWorkingCopy.getUserDirectories()) {
                    if (usr.getPath().equals(userDir.getPath())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    wRuntimeWorkingCopy.addUserDirectory(userDir);
                runtimeWorkingCopy.save(true, null);

            }

            WebSphereRuntime wsRuntime = (WebSphereRuntime) rt.loadAdapter(WebSphereRuntime.class, null);
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "updating server cache");
            wsRuntime.updateServerCache(true);

            //update cache again to initialize server.env, jvm.options and bootstrap.properties file variables
            //if the corresponding files exist
            wsServer.getServerInfo().updateCache();

        } catch (Exception e) {
            throw new ServerCreationException("Failed to create server.", e);
        }

        return server;
    }

    public boolean validateServerInfo(Map<String, String> serverInfo) throws ServerCreationException {
        Set<String> keys = serverInfo.keySet();

        for (String key : Constants.LIBERTY_VALIDATION_KEYS) {
            if (!keys.contains(key)) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Missing server info: " + key);
                throw new ServerCreationException(NLS.bind(Messages.libertyProducerMissingInfo, key));
            }
        }

        return keys.size() > 0;
    }
}
