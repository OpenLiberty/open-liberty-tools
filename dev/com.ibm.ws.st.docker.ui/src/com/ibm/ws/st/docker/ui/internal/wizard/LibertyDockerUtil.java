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
package com.ibm.ws.st.docker.ui.internal.wizard;

import java.io.File;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ServerEnv;
import com.ibm.ws.st.docker.ui.internal.Trace;

/**
 *
 */
public class LibertyDockerUtil {

    protected static final String OS_NAME = com.ibm.ws.st.common.core.ext.internal.Constants.OS_NAME;
    protected static final String DOCKER_MACHINE_TYPE = com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_MACHINE_TYPE;
    protected static final String DOCKER_MACHINE = com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_MACHINE;
    protected static final String DOCKER_CONTAINER = com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_CONTAINER;
    protected static final String DOCKER_IMAGE = com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_IMAGE;
    protected static final String LIBERTY_SERVER_NAME = com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_NAME;
    protected static final String HOSTNAME = com.ibm.ws.st.common.core.ext.internal.Constants.HOSTNAME;
    protected static final String LIBERTY_HTTPS_PORT = com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_HTTPS_PORT;

    public static final String LIBERTY_RUNTIME_INSTALL_PATH = com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_RUNTIME_INSTALL_PATH;
    public static final String LIBERTY_SERVER_CONFIG_PATH = com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH;
    public static final String DROPINS_DIR = com.ibm.ws.st.common.core.ext.internal.Constants.DROPINS_DIR;
    public static final String DEFAULTS_DIR = com.ibm.ws.st.common.core.ext.internal.Constants.DEFAULTS_DIR;

    public static boolean isLibertyContainer(BaseDockerContainer container) {
        try {
            List<BaseDockerContainer.DockerProcess> processes = container.getContainerProcesses();
            for (BaseDockerContainer.DockerProcess process : processes) {
                for (String cmdItem : process.getCommand()) {
                    if (cmdItem.contains("ws-server.jar")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Trace.logError("Could not get liberty container processes.", e);
        }
        return false;
    }

    protected static Map<String, String> getLibertyServerInfoMap(BaseDockerContainer container, WebSphereServer server) {
        // Get the server information from the container in order to enable remote administration and set up the connection.
        Map<String, String> libertyServerInfo = new HashMap<String, String>();
        String installPath = "opt/ibm/wlp";
        String serverName = "defaultServer";
        // This method is only called if validate has passed so container should never be null
        try {
            List<String> command = container.getCommand();
            if (command.size() >= 3 && command.get(0).endsWith("server")) {
                // Use which to get the path
                String exec = command.get(0);
                ExecutionOutput result = container.dockerExec("which " + exec);
                if (result.getReturnCode() == 0) {
                    String execPath = result.getOutput().trim();
                    int index = execPath.indexOf("/bin/server");
                    if (index > 0) {
                        installPath = execPath.substring(0, index);
                    } else {
                        if (Trace.ENABLED) {
                            Trace.logError("The executable component of the container command does not end in /bin/server as expected: " + execPath
                                           + ".  Using the default install path: " + installPath, null);
                        }
                    }
                    serverName = command.get(command.size() - 1);
                } else {
                    if (Trace.ENABLED) {
                        Trace.logError("Failed to get the path for the 'server' command: " + result.getOutput(), null);
                    }
                }
            } else {
                if (Trace.ENABLED) {
                    Trace.logError("Container command did not match expected format: " + mergeStrings(command) + ". Using the default install path and server name.", null);
                }
            }
        } catch (ConnectException e) {
            if (Trace.ENABLED) {
                Trace.logError("Failed to get the container command in order to extract the install path and server name, using the defaults.", e);
            }
        }

        libertyServerInfo.put(LIBERTY_RUNTIME_INSTALL_PATH, installPath);
        libertyServerInfo.put(LIBERTY_SERVER_NAME, serverName);

        String serverConfigPath = installPath + "/usr/servers/" + serverName;
        String wlpUserDir = null;
        // See if WLP_USER_DIR is set to something other than the default.  Setting
        // in server.env overrides container environment so check it first.
        try {
            // Check if ${wlp.install.dir}/etc/server.env exists and WLP_USER_DIR is set
            String sourcePath = installPath + "/" + Constants.ETC_FOLDER + "/" + Constants.SERVER_ENV;
            if (container.fileExists(sourcePath)) {
                if (server != null) {
                    IPath destinationPath = server.getWebSphereServerBehaviour().getTempDirectory().append(Constants.SERVER_ENV);
                    container.copyOut(sourcePath, destinationPath.toOSString());
                    File destinationFile = new File(destinationPath.toOSString());
                    ServerEnv serverEnv = new ServerEnv(destinationFile, null);
                    wlpUserDir = serverEnv.getValue(Constants.WLP_USER_DIR);
                    if (!destinationFile.delete()) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Failed to delete the temporary server.env file: " + destinationFile.getAbsolutePath(), null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.logError("Failed to read the ${wlp.install.dir}/etc/server.env file in order to extract the user directory.", e);
            }
        }

        if (wlpUserDir == null) {
            // If not set in server.env, check if WLP_USER_DIR is set in the container environment
            try {
                List<String> env = container.getEnv();
                String start = Constants.WLP_USER_DIR + "=";
                for (String varDef : env) {
                    if (varDef.startsWith(start)) {
                        wlpUserDir = varDef.substring(start.length());
                    }
                }
            } catch (ConnectException e) {
                if (Trace.ENABLED) {
                    Trace.logError("Failed to get the container env in order to extract the user directory, using the default.", e);
                }
            }
        }

        if (wlpUserDir != null && !wlpUserDir.isEmpty()) {
            serverConfigPath = wlpUserDir + "/servers/" + serverName;
        }

        libertyServerInfo.put(LIBERTY_SERVER_CONFIG_PATH, serverConfigPath);
        return libertyServerInfo;
    }

    protected static String getImageName(BaseDockerContainer container) {
        String imageName = null;
        try {
            imageName = container.getImageName();
        } catch (ConnectException e1) {
            Trace.logError("Failed to get the image name for container: " + container.getContainerName(), null);
        }
        return imageName;
    }

    public static Map<String, String> getServiceInfo(BaseDockerContainer container, WebSphereServer server, String host, String port) {
        Map<String, String> libertyServerInfo = getLibertyServerInfoMap(container, server);

        Map<String, String> serviceInfo = new HashMap<String, String>();
        serviceInfo.put(DOCKER_MACHINE_TYPE, container.getDockerMachine().getMachineType().name());
        serviceInfo.put(DOCKER_MACHINE, container.getDockerMachine().getMachineName());
        serviceInfo.put(DOCKER_CONTAINER, container.getContainerName());
        serviceInfo.put(DOCKER_IMAGE, getImageName(container));
        serviceInfo.put(LIBERTY_RUNTIME_INSTALL_PATH, libertyServerInfo.get(LIBERTY_RUNTIME_INSTALL_PATH));
        serviceInfo.put(LIBERTY_SERVER_NAME, libertyServerInfo.get(LIBERTY_SERVER_NAME));
        serviceInfo.put(LIBERTY_SERVER_CONFIG_PATH, libertyServerInfo.get(LIBERTY_SERVER_CONFIG_PATH));
        serviceInfo.put(HOSTNAME, host);
        serviceInfo.put(LIBERTY_HTTPS_PORT, port);

        return serviceInfo;
    }

    private static String mergeStrings(List<String> list) {
        StringBuilder builder = new StringBuilder();
        boolean start = true;
        for (String item : list) {
            if (start) {
                start = false;
            } else {
                builder.append(" ");
            }
            builder.append(item);
        }
        return builder.toString();
    }
}
