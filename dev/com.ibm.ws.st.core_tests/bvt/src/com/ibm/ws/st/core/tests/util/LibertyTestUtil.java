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
package com.ibm.ws.st.core.tests.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereUtil;

/**
 *
 */
public class LibertyTestUtil extends ServerTestUtil {

    public static void createLibertyServer(IRuntime rt, String libertyServerName, UserDirectory userDir) throws Exception {
        WebSphereRuntime wsRuntime = getWebSphereRuntime(rt);
        wsRuntime.createServer(libertyServerName, null, userDir.getPath(), null);
    }

    public static void addUserDir(IRuntime runtime, IProject userDirProject) throws Exception {
        IRuntimeWorkingCopy wc = runtime.createWorkingCopy();
        WebSphereRuntime wr = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
        wr.addUserDirectory(userDirProject);
        wr.updateServerCache(true);
        wc.save(true, null);
        WLPCommonUtil.jobWaitBuildandResource();
    }

    public static void addUserDir(IRuntime runtime, IPath userDirPath) throws Exception {
        IRuntimeWorkingCopy wc = runtime.createWorkingCopy();
        WebSphereRuntime wr = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
        wr.addUserDirectory(userDirPath);
        wr.updateServerCache(true);
        wc.save(true, null);
        WLPCommonUtil.jobWaitBuildandResource();
    }

    public static WebSphereRuntime getWebSphereRuntime(IRuntime runtime) {
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.getAdapter(WebSphereRuntime.class);
        if (wsRuntime == null)
            throw new NullPointerException("Runtime should be an instance of WebSphereRuntime.  Actual runtime type is: " + runtime.getClass().getName());
        return wsRuntime;
    }

    public static WebSphereServer getWebSphereServer(IServer server) {
        WebSphereServer wsServer = (WebSphereServer) server.getAdapter(WebSphereServer.class);
        if (wsServer == null)
            throw new NullPointerException("Server should be an instance of WebSphereServer.  Actual server type is: " + server.getClass().getName());
        return wsServer;
    }

    public static WebSphereServer getServer(String serverName) {
        WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
        for (WebSphereServer server : servers) {
            if (serverName.equals(server.getServer().getName())) {
                return server;
            }
        }
        return null;
    }

}
