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
package com.ibm.ws.st.core.tests.remote;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.jmx.JMXConnectionException;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

/**
 *
 */
public class RemoteTestUtil extends ServerTestUtil {

    private static Map<String, String> connectionInfoCache;

    public static void setRemotePreferences() {
        System.setProperty("wtp.autotest.noninteractive", "true");
        Activator.setPreference("headless.auto.accept.cert", "true"); //setting this preference will automatically accept certificate from remote server
    }

    /* Establish a JMX connection, if it fails wait 2 seconds and try again, up to 20 times */
    public static JMXConnection setJMXConnection(String host, String port, String user, String password) throws Exception {
        JMXConnection jmxConnection = new JMXConnection(host, port, user, password);

        int attempt = 0;
        while (jmxConnection.isConnected() == false) {
            try {
                jmxConnection.connect();
            } catch (JMXConnectionException e) {
                Trace.logError("Error JMXConnection failed, try again...", e);
                WLPCommonUtil.wait("Wait an additional 2 seconds.", 2000);
            }
            attempt++;
            if (attempt >= 20)
                break;
        }
        if (!jmxConnection.isConnected()) {
            print("Could not create connection to the remote server: " + host + ", " + port + ", " + user + ", " + password);
        }
        return jmxConnection;
    }

    public static IServer setRemoteServerAttributes(IRuntime runtime, String serverName, String hostname, String port, String user, String password) throws CoreException {
        IServerType st = ServerCore.findServerType(WLPCommonUtil.SERVER_ID);
        IServerWorkingCopy wc = st.createServer(hostname, null, runtime, null);
        wc.setHost(hostname);

        WebSphereServer wsServer = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);
        wsServer.setServerName(serverName);
        wsServer.setLooseConfigEnabled(false);
        wsServer.setStopTimeout(120);
        wsServer.setServerUserName(user);
        wsServer.setServerPassword(password);
        wsServer.setServerSecurePort(port);

        String remoteServerStartEnabled = System.getProperty("liberty.remote.remoteServerStartEnabled");
        boolean isRemoteServerStartEnabled = Boolean.valueOf(remoteServerStartEnabled).booleanValue();

        Map<String, String> connectionInfo = getConnectionInfo();

        if (isRemoteServerStartEnabled) {
            wsServer.setIsRemoteServerStartEnabled(isRemoteServerStartEnabled);
            String osName = connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.OS_NAME);
            if ("linux".equals(osName)) {
                wsServer.setRemoteServerStartPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX);
            } else if ("mac".equals(osName)) {
                wsServer.setRemoteServerStartPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC);
            } else if ("other".equals(osName)) {
                wsServer.setRemoteServerStartPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER);
            } else {
                wsServer.setRemoteServerStartPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
            }
            wsServer.setRemoteServerStartRuntimePath(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_RUNTIME_INSTALL_PATH));
            wsServer.setRemoteServerStartConfigPath(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH));
            String logonMethod = connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD);
            if (com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD_SSH.equals(logonMethod)) {
                wsServer.setRemoteServerStartLogonMethod(RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_SSH);
                wsServer.setRemoteServerStartSSHId(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER));
                wsServer.setRemoteServerStartSSHPassphrase(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD));
                wsServer.setRemoteServerStartSSHKeyFile(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.SSH_KEY));
            } else {
                wsServer.setRemoteServerStartLogonMethod(RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS);
                wsServer.setRemoteServerStartOSId(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER));
                wsServer.setRemoteServerStartOSPassword(connectionInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD));
            }
        }

        print("Server running looseConfig: " + wsServer.isLooseConfigEnabled());
        IServer server = wc.save(true, null);
        return server;
    }

    public static List<IPath> downloadRemoteServerFiles(IRuntime rt, IServer server, JMXConnection jmxConnection) throws Exception {
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        String serverConfigDir = RemoteUtils.getServerConfigDir(jmxConnection);
        String serverName = RemoteUtils.getServerName(serverConfigDir);
        String remoteUserPath = RemoteUtils.getUserDir(jmxConnection);

        String userDirName = RemoteUtils.generateRemoteUsrDirName(wsServer.getWebSphereRuntime());
        UserDirectory userDir = RemoteUtils.createUserDir(wsServer, remoteUserPath, userDirName, new NullProgressMonitor());
        IServerWorkingCopy serverWC = server.createWorkingCopy();
        WebSphereServer websphereServer = (WebSphereServer) serverWC.loadAdapter(WebSphereServer.class, null);
        if (websphereServer != null) {
            websphereServer.setUserDir(userDir);
            serverWC.save(true, null);
        }

        String userDirPath = userDir.getPath().toOSString();
        IPath userMetaDataPath = userDir.getPath();
        ArrayList<IPath> downloadedFiles = new ArrayList<IPath>();
        IStatus downloadStatus = RemoteUtils.downloadServerFiles(jmxConnection, userMetaDataPath, serverName, downloadedFiles, remoteUserPath, userDirPath);
        if (!downloadStatus.isOK()) {
            Exception e = new ConnectException("Failed to download remote server files: " + downloadStatus.getMessage());
            e.initCause(downloadStatus.getException());
            throw e;
        }

        WebSphereRuntime wsRuntime = (WebSphereRuntime) rt.loadAdapter(WebSphereRuntime.class, null);
        print("updating server cache");
        wsRuntime.updateServerCache(true);

        return downloadedFiles;
    }

    public static Map<String, String> getConnectionInfo() {
        if (connectionInfoCache == null) {
            String hostname = System.getProperty("liberty.remote.hostname");
            String username = System.getProperty("liberty.remote.username");
            String password = System.getProperty("liberty.remote.password");
            String httpsPort = System.getProperty("liberty.remote.https.port");
            String osName = System.getProperty("liberty.remote.osname");
            String osUser = System.getProperty("liberty.remote.oslogon.user");
            String osPassword = System.getProperty("liberty.remote.oslogon.pass");
            String sshUser = System.getProperty("liberty.remote.sshlogon.user");
            String sshPassword = System.getProperty("liberty.remote.sshlogon.password");
            String sshKeyFile = System.getProperty("liberty.remote.sshlogon.keyfile");
            String installPath = System.getProperty("liberty.remote.installPath");
            String configPath = System.getProperty("liberty.remote.configPath");
            String logonMethod = System.getProperty("liberty.remote.logonMethod");

            if (hostname == null || hostname.isEmpty()) {
                hostname = "localhost";
            } else {
                // Allow for multiple test runs at the same time, trying to keep them from
                // all accessing the same remote host.  This only works for Docker where a
                // new server (container) is created for each test case since tests may
                // still access the same host at the same time.  This is just meant to spread
                // it around a bit.
                String[] hostList = hostname.split(",");
                if (hostList.length > 1) {
                    // Support hostname, osName, osUser and osPassword being different, the
                    // rest of the setup is expected to be the same.  More can be added later
                    // if needed.
                    int index = (int) (Math.random() * hostList.length);
                    hostname = hostList[index];
                    osName = getIndexedProperty(index, osName);
                    osUser = getIndexedProperty(index, osUser);
                    osPassword = getIndexedProperty(index, osPassword);
                }
            }

            Map<String, String> connectionInfo = new HashMap<String, String>();
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.HOSTNAME, hostname);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_USER, username);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_PASSWORD, password);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_HTTPS_PORT, httpsPort);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.OS_NAME, osName);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.SSH_KEY, sshKeyFile);
            if (com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD_SSH.equals(logonMethod) && sshUser != null && sshPassword != null) {
                addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER, sshUser);
                addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD, sshPassword);
            } else {
                addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER, osUser);
                addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD, osPassword);
            }
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_RUNTIME_INSTALL_PATH, installPath);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH, configPath);
            addConnectionProperty(connectionInfo, com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD, logonMethod);
            connectionInfoCache = connectionInfo;
        }

        return connectionInfoCache;
    }

    private static void addConnectionProperty(Map<String, String> connectionInfo, String key, String value) {
        if (value != null && !value.isEmpty()) {
            connectionInfo.put(key, value);
        }
    }

    private static String getIndexedProperty(int index, String property) {
        String[] list = property.split(",");
        if (list.length > index) {
            return list[index];
        }
        return list[0];
    }

}
