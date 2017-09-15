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
package com.ibm.ws.st.core.tests;

import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.LibertyTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

/**
 *
 */
public class StandaloneLibertyTestExt implements TestBaseInterface {

    public static final String SERVER_ID = "com.ibm.ws.st.server.wlp";

    /** {@inheritDoc} */
    @Override
    public IServer createServer(IRuntime runtime, Map<String, String> serverInfo) throws Exception {
        String libertyServerName = serverInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_NAME);
        String serverName = serverInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.SERVER_LABEL);
        if (serverName == null) {
            serverName = libertyServerName;
        }
        String looseCfg = serverInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LOOSE_CFG);
        String userDirString = serverInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_USER_DIR);
        String serverConfigResourceLocation = serverInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_CONFIG_SOURCE);
        IPath userDir = userDirString == null ? null : new Path(userDirString);
        if (libertyServerName == null)
            throw new Exception("The liberty server name cannot be null");

        IServer server = null;
        WebSphereServer wsServer = null;

        boolean isLC = looseCfg != null && looseCfg.equals("true");
        IServer[] servers = ServerCore.getServers();
        for (IServer sr : servers) {
            if (sr.getId().equals(libertyServerName) && serverName.equals(sr.getName())) {
                server = sr;
                wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                WLPCommonUtil.print("Server exists...reusing it.");
                if (wsServer.isLooseConfigEnabled() != isLC) {
                    IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
                    wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, isLC);
                    wc.save(true, null);
                    wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                }
                WLPCommonUtil.print("Server running looseConfig: " + isLC);
                return server;
            }
        }

        IPath usrPath;
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (userDir != null) {
            usrPath = userDir;
        } else {
            usrPath = wsRuntime.getDefaultUserDirPath();
            if (!usrPath.toFile().exists())
                usrPath.toFile().mkdirs();
        }

        IPath serverPath = usrPath.append(Constants.SERVERS_FOLDER + "/" + libertyServerName);
        if (serverConfigResourceLocation != null) {
            IPath resourcePath = new Path(serverConfigResourceLocation);
            WLPCommonUtil.print("Copying server files from " + resourcePath.toPortableString() + " to " + serverPath.toPortableString());
            FileUtil.copyFiles(resourcePath.toPortableString(), serverPath.toPortableString());
        } else if (!serverPath.toFile().exists()) {
            WLPCommonUtil.print("Creating a new default server because a server could not be found at path " + serverPath.toOSString());
            wsRuntime.createServer(libertyServerName, "defaultServer", usrPath, null);
        }

        UserDirectory userDirectory = null;
        for (UserDirectory dir : wsRuntime.getUserDirectories()) {
            if (dir.getPath().equals(usrPath)) {
                userDirectory = dir;
                break;
            }
        }

        IServerType st = ServerCore.findServerType(SERVER_ID);
        IServerWorkingCopy wc = st.createServer(serverName, null, runtime, null);
        wc.setName(serverName);
        wc.setAttribute(WebSphereServer.PROP_SERVER_NAME, libertyServerName);
        if (userDirectory != null) {
            wc.setAttribute(WebSphereServer.PROP_USERDIR_ID, userDirectory.getUniqueId());
        } else {
            WLPCommonUtil.print("Failed to locate user directory for path: " + usrPath.toPortableString());
        }
        wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, isLC);
        wc.setAttribute(WebSphereServer.PUBLISH_WITH_ERROR, true);
        server = wc.save(true, null);
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        WLPCommonUtil.basicServerSetup(serverPath);
        if (serverConfigResourceLocation != null) // we should refresh the runtime if the we create the server profile
            wsServer.getWebSphereRuntime().updateServerCache(true);
        WLPCommonUtil.print("Server running looseConfig: " + wsServer.isLooseConfigEnabled());
        WLPCommonUtil.jobWaitBuildandResource();
        return server;
    }

    /** {@inheritDoc} */
    @Override
    public void cleanUp() {
        WLPCommonUtil.cleanUp();
    }

    /** {@inheritDoc} */
    @Override
    public String getBaseURL(IServer server) throws Exception {
        WebSphereServer wsServer = LibertyTestUtil.getWebSphereServer(server);
        return wsServer.getServerWebURL();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsLooseCfg(IServer server) {
        WebSphereServer wsServer = server.getAdapter(WebSphereServer.class);
        return (wsServer != null && wsServer.isLocalHost());
    }

}
