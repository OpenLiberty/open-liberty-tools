/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.jmx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.ApplicationNotificationListener;
import com.ibm.ws.st.core.internal.ConfigurationListener;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.security.LibertySecurityHelper;
import com.ibm.ws.st.core.internal.security.LibertyX509TrustManager;

/**
 * JMX connection helper class.
 * Not yet thread safe - use only within the context of one call!
 */
public class JMXConnection {
    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";
    private static final String OSGI_FRAMEWORK_MBEAN_NAME = "osgi.core:type=framework,*";
    private static final String GEN_PLUGIN_CONFIG_OBJECT_NAME = "WebSphere:name=com.ibm.ws.jmx.mbeans.generatePluginConfig";
    private static final String APP_MANAGEMENT_MBEAN_NAME = "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=";
    private static final String FILE_SERVICE_MBEAN_NAME = "WebSphere:feature=restConnector,type=FileService,name=FileService";
    private static final String FILE_TRANSFER_MBEAN_NAME = "WebSphere:feature=restConnector,type=FileTransfer,name=FileTransfer";
    private static final String HTTP_ENDPOINT_SSL_MBEAN_NAME = "WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint-ssl";
    private static final String HTTP_ENDPOINT_MBEAN_NAME = "WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint";
    // future  "WebSphere", "name", "com.ibm.ws.jmx.mbeans.restartServer"  for restart server's JVM
    protected MBeanServerConnection mbsc = null;
    protected IPath serverWorkAreaPath = null;
    protected JMXConnector connector = null;
    protected String host;
    protected String port;
    protected String user;
    protected String password;

    public JMXConnection(IPath serverWorkAreaPath) {
        if (serverWorkAreaPath == null)
            throw new IllegalArgumentException("Path to server cannot be null");

        this.serverWorkAreaPath = serverWorkAreaPath;
    }

    public JMXConnection(String host, String port, String user, String password) {
        if (host == null)
            throw new IllegalArgumentException("Hostname cannot be null");
        if (port == null)
            throw new IllegalArgumentException("Port cannot be null");
        if (user == null)
            throw new IllegalArgumentException("User name cannot be null");
        if (password == null)
            throw new IllegalArgumentException("Password cannot be null");

        // for IPv6 addresses, JMX requires IP address to be enclosed in square brackets [].
        // but first we must check if its a valid IPv6 address
        String hostName = FileUtil.getStringWithoutBrackets(host);

        // isIpv6LiteralAddress don't accept [] brackets. so remove the brackets first.
        try {
            InetAddress addr = InetAddress.getByName(hostName);
            if (addr instanceof Inet6Address) {
                hostName = "[" + hostName + "]";
            }
        } catch (UnknownHostException uhe) {
            // Not a valid ip address
        }
//        if (IPAddressUtil.isIPv6LiteralAddress(hostName)) {
//            hostName = "[" + hostName + "]";
//        }
        this.host = hostName;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public void connect(long timeout, long interval) throws JMXConnectionException {
        JMXConnectionException connectException = null;
        for (long time = 0; time < timeout; time += interval) {
            try {
                connect();
                connectException = null;
                break;
            } catch (JMXConnectionException e) {
                connectException = e;
            }
        }
        if (connectException != null) {
            throw connectException;
        }
    }

    public void connect() throws JMXConnectionException {
        long time0 = System.currentTimeMillis();
        Throwable cause = null;
        try {
            if (connector == null)
                connector = isLocalConnection() ? getLocalConnector() : getRemoteConnector();

            if (connector != null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.JMX, "JMX Connector: " + connector);
                mbsc = connector.getMBeanServerConnection();
            }

            if (mbsc != null) {
                if (Trace.ENABLED)
                    Trace.tracePerf("Time to establish JMX", System.currentTimeMillis() - time0);
                return; // successful, return
            }
        } catch (Throwable t) {
            cause = t;
            try {
                if (Trace.ENABLED)
                    Trace.trace(Trace.JMX, "JMX connection failed.", t);
                if (connector != null)
                    connector.close();
            } catch (Exception e) {
                // ignore
            }
            connector = null;
        }

        // make sure everything is cleaned up when fail to connect
        disconnect();
        throw new JMXConnectionException(cause);
    }

    private JMXConnector getLocalConnector() throws Exception {
        IPath addressFilePath = serverWorkAreaPath.append(CONNECTOR_ADDRESS_FILE_NAME);
        File file = addressFilePath.toFile();

        boolean exist = file.exists();
        if (!exist) {
            int i = 100; // maximum to wait 5 seconds for the JMX address
            long time = System.currentTimeMillis();
            while (!exist && i > 0) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // do nothing
                }
                exist = file.exists();
                i--;
            }
            if (Trace.ENABLED)
                Trace.tracePerf("Time to obtain JMX address", time);
        }

        if (exist) {
            String connectorAddr = null;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(addressFilePath.toFile()));
                connectorAddr = br.readLine();
            } catch (IOException e) {
                throw e;
            } finally {
                try {
                    if (br != null)
                        br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (connectorAddr != null) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.JMX, "JMX connector address:  " + connectorAddr);
                }
                JMXServiceURL url = new JMXServiceURL(connectorAddr);
                return JMXConnectorFactory.connect(url);
            }
            Trace.logError("JMXConnection: JMX connector address is null.  The file is " + file.getAbsolutePath(), null);
        } else {
            Trace.logError("JMXConnection: JMX address file doesn't exist: " + file.getAbsolutePath(), null);
        }
        return null;
    }

    private JMXConnector getRemoteConnector() throws Exception {

        String connectorAddr = "service:jmx:rest://" + host + ":" + port + "/IBMJMXConnectorREST";
        if (Trace.ENABLED) {
            Trace.trace(Trace.JMX, "JMX connector address:  " + connectorAddr);
        }
        JMXServiceURL url = new JMXServiceURL(connectorAddr);

        SSLContext sc = LibertySecurityHelper.getSSLContext();

        TrustManager[] tm = { new LibertyX509TrustManager() };
        sc.init(null, tm, new SecureRandom());

        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put("com.ibm.ws.jmx.connector.client.CUSTOM_SSLSOCKETFACTORY", sc.getSocketFactory());
        environment.put("com.ibm.ws.jmx.connector.client.disableURLHostnameVerification", Boolean.TRUE);
        environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        environment.put(JMXConnector.CREDENTIALS, new String[] { user, password });
        environment.put("com.ibm.ws.jmx.connector.client.rest.maxServerWaitTime", new Integer(10000));

        return JMXConnectorFactory.connect(url, environment);
    }

    private Object invoke(String objectName, String operation, Object[] params, String[] signature) throws Exception {
        return invoke(new ObjectName(objectName), operation, params, signature);
    }

    private Object invoke(ObjectName objectName, String operation, Object[] params, String[] signature) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();

        try {
            return mbsc.invoke(objectName, operation, params, signature);
        } catch (ConnectException e) {
            Trace.trace(Trace.JMX, "Failed JMX operation '" + operation + "' on objectName '" + objectName + "'", e);
            throw new JMXConnectionException();
        } catch (Exception e) {
            Trace.trace(Trace.JMX, "Failed JMX operation '" + operation + "' on objectName '" + objectName + "'", e);
            throw e;
        }
    }

    public void stop() throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Stopping server");

        if (mbsc == null)
            throw new JMXConnectionException();

        // Using wildcards so need to query the object name
        Set<ObjectName> objNames = mbsc.queryNames(new ObjectName(OSGI_FRAMEWORK_MBEAN_NAME), null);
        if (objNames == null || objNames.isEmpty()) {
            throw new Exception("MBean object name query failed for pattern: " + OSGI_FRAMEWORK_MBEAN_NAME);
        } else if (objNames.size() > 1) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Found more than one mbean match for pattern: " + OSGI_FRAMEWORK_MBEAN_NAME);
        }
        invoke(objNames.iterator().next(), "shutdownFramework", null, null);
    }

    public String[] getVMArgs() throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Stopping server");

        if (mbsc == null)
            throw new JMXConnectionException();

        ObjectName objName = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
        return (String[]) mbsc.getAttribute(objName, "InputArguments");
    }

    /**
     * Get the Port Number, the remote server is listening on for debug connections
     */
    public int getDebugPortNum() throws Exception {

        String[] vmArgs = getVMArgs();

        if (vmArgs == null) {
            return -1;
        }

        String debugArgs = null;
        int debugPort = -1;

        for (int i = 0; i < vmArgs.length; i++) {
            String args = vmArgs[i];
            if (args.contains("-agentlib:jdwp=")) {
                debugArgs = args;
                break;
            }
        }

        if (debugArgs != null) {
            int index = debugArgs.indexOf("address=");

            if (index != -1) {
                String debugPortNumStr = debugArgs.substring(index + 8);

                int spaceIndex = debugPortNumStr.indexOf(" ");
                if (spaceIndex != -1) {
                    // if the debug port num is not the last argument, then get the port number string.
                    debugPortNumStr = debugPortNumStr.substring(0, spaceIndex);
                }
                int colonIndex = debugPortNumStr.indexOf(':');
                if (colonIndex != -1) {
                    // if the debug port num is not the last argument, then get the port number string.
                    debugPortNumStr = debugPortNumStr.substring(colonIndex + 1, debugPortNumStr.length());
                }
                try {
                    debugPort = Integer.parseInt(debugPortNumStr);
                } catch (Exception e) {
                    Trace.trace(Trace.WARNING, "Couldn't parse debug port number", e);
                }
            }
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Remote Server is listening on socket:" + debugPort);

        return debugPort;
    }

    /**
     * get the Value of secure HTTP port for the remote server
     */
    public int getRemoteSecureHTTPPort() throws Exception {
        if (mbsc == null)
            connect();
        int httpPort = 0;
        Set<ObjectName> objNames = mbsc.queryNames(new ObjectName(HTTP_ENDPOINT_SSL_MBEAN_NAME), null);
        if (objNames == null) {
            throw new Exception("MBean object name query failed for pattern: " + HTTP_ENDPOINT_SSL_MBEAN_NAME + "*");
        }
        for (ObjectName defaultSecureHTTPEndPoint : objNames) {
            httpPort = (Integer) mbsc.getAttribute(defaultSecureHTTPEndPoint, "Port");
        }
        return httpPort;
    }

    /**
     * get the Value of UnSecure HTTP port for the remote server
     */
    public int getRemoteUnsecureHTTPPort() throws Exception {
        if (mbsc == null)
            connect();
        int httpPort = 0;
        Set<ObjectName> objNames = mbsc.queryNames(new ObjectName(HTTP_ENDPOINT_MBEAN_NAME), null);
        if (objNames == null) {
            throw new Exception("MBean object name query failed for pattern: " + HTTP_ENDPOINT_MBEAN_NAME + "*");
        }
        for (ObjectName defaultUnsecureHTTPEndPoint : objNames) {
            httpPort = (Integer) mbsc.getAttribute(defaultUnsecureHTTPEndPoint, "Port");
        }
        return httpPort;
    }

    public boolean isMBeanExists(String objectName) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();

        Set<ObjectName> objNames = mbsc.queryNames(new ObjectName(objectName), null);
        if (objNames == null || objNames.isEmpty()) {
            return false;
        }

        return true;
    }

    public List<String> getAllApplicationNames() throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();

        Set<ObjectName> objNames = mbsc.queryNames(new ObjectName(APP_MANAGEMENT_MBEAN_NAME + "*"), null);
        if (objNames == null) {
            throw new Exception("MBean object name query failed for pattern: " + APP_MANAGEMENT_MBEAN_NAME + "*");
        }
        if (objNames.isEmpty())
            return Collections.emptyList();
        List<String> appList = new ArrayList<String>();
        for (ObjectName name : objNames) {
            String appName = name.getKeyProperty("name");
            if (appName != null)
                appList.add(appName);
        }
        return appList;
    }

    public void notifyFileChanges(Collection<String> added, Collection<String> changed, Collection<String> removed) throws Exception {
        if (Trace.ENABLED) {
            Trace.trace(Trace.JMX, "Notify the server of added, changed and removed files");
            traceNotifyFileChanges("Added files", added);
            traceNotifyFileChanges("Changed files", changed);
            traceNotifyFileChanges("Removed files", removed);
        }
        String[] signature = new String[] { "java.util.Collection", "java.util.Collection", "java.util.Collection" };
        Collection<?>[] params = new Collection[] { added, changed, removed };
        invoke("WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean",
               "notifyFileChanges", params, signature);
    }

    private void traceNotifyFileChanges(String title, Collection<String> files) {
        if (files != null) {
            Trace.trace(Trace.JMX, title + ": " + files);
        }
    }

    public void generateDefaultPluginConfig() throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Generating default plugin config");
        invoke(GEN_PLUGIN_CONFIG_OBJECT_NAME, "generateDefaultPluginConfig", null, null);
    }

    public void generatePluginConfig(String path, String serverName) throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Generating plugin config");
        String[] params = new String[] { path, serverName };
        String[] signature = new String[] { "java.lang.String", "java.lang.String" };
        invoke(GEN_PLUGIN_CONFIG_OBJECT_NAME, "generatePluginConfig", params, signature);
    }

    public void startApplication(String applicationName) throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Start application: " + applicationName);
        invoke(APP_MANAGEMENT_MBEAN_NAME + applicationName, "start", null, null);
    }

    public void stopApplication(String applicationName) throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Stop application: " + applicationName);
        invoke(APP_MANAGEMENT_MBEAN_NAME + applicationName, "stop", null, null);
    }

    public void restartApplication(String applicationName) throws Exception {
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Restart application: " + applicationName);
        invoke(APP_MANAGEMENT_MBEAN_NAME + applicationName, "restart", null, null);
    }

    public void downloadFile(String remoteFile, String destinationFile) throws Exception {
        if (isLocalConnection())
            throw new UnsupportedOperationException("Downloading files is only supported by the REST connector");
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Downloading remote file: " + remoteFile + " to " + destinationFile);
        String[] params = new String[] { remoteFile, destinationFile };
        String[] signature = new String[] { "java.lang.String", "java.lang.String" };
        invoke(FILE_TRANSFER_MBEAN_NAME, "downloadFile", params, signature);
    }

    public void uploadFile(File localFile, String destinationFile, boolean expand) throws Exception {
        if (isLocalConnection())
            throw new UnsupportedOperationException("Uploading files is only supported by the REST connector");

        String path = null;
        if (localFile != null) {
            path = localFile.getAbsolutePath();
        }
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Uploading file: " + path + " to " + destinationFile);
        Object[] params = new Object[] { path, destinationFile, Boolean.valueOf(expand) };
        String[] signature = new String[] { "java.lang.String", "java.lang.String", "java.lang.Boolean" };
        invoke(FILE_TRANSFER_MBEAN_NAME, "uploadFile", params, signature);
    }

    public void deleteFile(String remoteSourceFile) throws Exception {
        if (isLocalConnection())
            throw new UnsupportedOperationException("Deleting files is only supported by the REST connector");

        String file = remoteSourceFile;

        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Deleting remote file: " + file);
        Object[] params = new Object[] { file };
        String[] signature = new String[] { "java.lang.String" };
        invoke(FILE_TRANSFER_MBEAN_NAME, "deleteFile", params, signature);
    }

    public Object getMetadata(String path, String requestOptions) throws Exception {
        if (isLocalConnection())
            throw new UnsupportedOperationException("Querying the server is only supported by the REST connector");
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Querying server for path : " + path + " with request options " + requestOptions);
        String[] params = new String[] { path, requestOptions };
        String[] signature = new String[] { "java.lang.String", "java.lang.String" };
        return invoke(FILE_SERVICE_MBEAN_NAME, "getMetaData", params, signature);
    }

    public CompositeData[] getDirectoryEntries(String directory, boolean recursive, String requestOptions) throws Exception {
        if (isLocalConnection())
            throw new UnsupportedOperationException("Querying the server is only supported by the REST connector");
        if (Trace.ENABLED)
            Trace.trace(Trace.JMX, "Getting directory entries for: " + directory + " with request options " + requestOptions + " and isRecursive=" + recursive);
        Object[] params = new Object[] { directory, Boolean.valueOf(recursive), requestOptions };
        String[] signature = new String[] { "java.lang.String", "boolean", "java.lang.String" };
        return (CompositeData[]) invoke(FILE_SERVICE_MBEAN_NAME, "getDirectoryEntries", params, signature);
    }

    public void addAppListener(ApplicationNotificationListener appListener) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Adding app listener for " + appListener.getAppName());
        mbsc.addNotificationListener(new ObjectName(APP_MANAGEMENT_MBEAN_NAME + appListener.getAppName()), appListener, null, null);
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "App listener successfully added for " + appListener.getAppName());
    }

    public void removeAppListener(ApplicationNotificationListener appListener) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Removing app listener for " + appListener.getAppName());
        mbsc.removeNotificationListener(new ObjectName(APP_MANAGEMENT_MBEAN_NAME + appListener.getAppName()), appListener);
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "App listener successfully removed for " + appListener.getAppName());
    }

    public void addConfigListener(ConfigurationListener configListener) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Adding config listener");
        mbsc.addNotificationListener(new ObjectName(ConfigurationListener.OBJECT_NAME), configListener, configListener.getFilter(), null);
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Config listener successfully added");
    }

    public void removeConfigListener(ConfigurationListener configListener) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Removing config listener");
        mbsc.removeNotificationListener(new ObjectName(ConfigurationListener.OBJECT_NAME), configListener, configListener.getFilter(), null);
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Config listener successfully removed");
    }

    public String getAppState(String appName) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        return (String) mbsc.getAttribute(new ObjectName(APP_MANAGEMENT_MBEAN_NAME + appName), "State");
    }

    public Object getMBeanAttribute(String objectName, String attribute) throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        return mbsc.getAttribute(new ObjectName(objectName), attribute);
    }

    public List<String> getAPISPIJars() throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        List<String> allDevFiles = new ArrayList<String>(); //all files under /dev
        String srcDir = "${wlp.install.dir}/dev";

        //String requestOption = FileServiceMXBean.REQUEST_OPTIONS_ALL;   //This can be modified to return only the required metadata. But for this userstory it would help to choose all.
        String requestOption = "a"; //This avoids using the hard dependency in above line.
        Boolean recursive = Boolean.TRUE;
        String[] signature = new String[] { "java.lang.String", "boolean", "java.lang.String" };
        Object[] params = new Object[] { srcDir, recursive, requestOption };
        Object ret = mbsc.invoke(new ObjectName(FILE_SERVICE_MBEAN_NAME), "getDirectoryEntries", params, signature);

        if (ret instanceof CompositeData[]) {
            CompositeData[] retList = (CompositeData[]) ret;
            for (CompositeData fileMetaData : retList) {
                if (fileMetaData.get("directory").equals(Boolean.TRUE)) //skip if this a directory
                    continue;
                String filePath = (String) fileMetaData.get("fileName");
                allDevFiles.add(filePath);
            }
        }
        return allDevFiles;
    }

    public Integer getMBeanCount() throws Exception {
        if (mbsc == null)
            throw new JMXConnectionException();
        return mbsc.getMBeanCount();
    }

    public void disconnect() {
        mbsc = null;
        try {
            if (connector != null)
                connector.close();
        } catch (Exception e) {
            // ignore
        }
        connector = null;
    }

    public boolean isLocalConnection() {
        return serverWorkAreaPath != null;
    }

    /**
     * Checks whether there is an Application MBean with the given module name. Returns true if and only if
     * the MBean exists.
     *
     * @param moduleName
     * @return
     */
    public boolean appMBeanExists(String moduleName) {
        try {
            List<String> apps = getAllApplicationNames();
            if (apps != null)
                return apps.contains(moduleName);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.JMX, "Problem looking for MBean application", e);
        }
        return false;
    }

    public boolean isConnected() {
        if (connector == null)
            return false;

        try {
            connector.connect();
        } catch (IOException e) {
            Trace.trace(Trace.JMX, "JMXConnection could not be established.", e);
            return false;
        }
        return true;
    }
}
