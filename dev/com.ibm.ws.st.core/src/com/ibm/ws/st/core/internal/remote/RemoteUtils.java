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

package com.ibm.ws.st.core.internal.remote;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.Bootstrap;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.internal.config.ServerEnv;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

/**
 * Utilities commonly used for remote servers.
 */
public class RemoteUtils {

    /**
     * Find an unused remote user directory name. Remote user directories are located within
     * the workspace.
     *
     * @param wsRuntime liberty runtime
     * @return a unique user directory name
     */
    public static String generateRemoteUsrDirName(WebSphereRuntime wsRuntime) {
        return generateRemoteUsrDirName(wsRuntime, "Remote");
    }

    /**
     * Find an unused remote user directory name using the base identifier passed in.
     *
     * @param wsRuntime liberty runtime
     * @param baseIdentifier base identifier to use in the user directory name
     * @return
     */
    public static String generateRemoteUsrDirName(WebSphereRuntime wsRuntime, String baseIdentifier) {
        String defaultName = wsRuntime.getRuntime().getName() + " (" + baseIdentifier + ")";
        String name = defaultName;
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        boolean decided = false;
        int i = 1;
        while (!decided) {
            IProject project = root.getProject(name);
            File projectFolder = root.getLocation().append(name).toFile();
            if (project.exists() || projectFolder.exists())
                name = defaultName + "(" + i++ + ")";
            else
                decided = true;
        }
        return name;
    }

    /*
     * Create the user directory and copy over the server config files from the temporary location.
     * Delete the temporary files.
     */
    public static UserDirectory createUserDir(WebSphereServer server, String remoteUserPath, String userDirName, IProgressMonitor monitor) throws CoreException {
        UserDirectory userDir = null;
        WebSphereRuntime wsRuntime = server.getWebSphereRuntime();
        if (wsRuntime != null) {
            IRuntimeWorkingCopy runtimeWorkingCopy = wsRuntime.getRuntime().createWorkingCopy();
            WebSphereRuntime wRuntimeWorkingCopy = (WebSphereRuntime) runtimeWorkingCopy.loadAdapter(WebSphereRuntime.class, null);

            IProject remoteUsrProject = WebSphereUtil.createUserProject(userDirName, null, monitor);
            IPath outputPath = remoteUsrProject.getLocation().append(Constants.SERVERS_FOLDER);
            //changed to getLocation because getFullPAth was not returning the absolute Path
            userDir = new UserDirectory(wsRuntime, remoteUsrProject.getLocation(), remoteUsrProject, outputPath, new Path(remoteUserPath));

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
            try {
                runtimeWorkingCopy.save(true, null);
            } catch (CoreException ce) {
                Trace.logError(ce.getMessage(), ce);
            }
            wsRuntime.updateServerCache(true);
        }

        return userDir;
    }

    public static String getServerConfigDir(JMXConnection jmxConnection) throws Exception {
        CompositeData metadata = (CompositeData) jmxConnection.getMetadata(Constants.SERVER_CONFIG_VAR, "a");
        String serverConfigDir = (String) metadata.get("fileName");
        serverConfigDir = serverConfigDir.replace("\\", "/");
        return serverConfigDir;
    }

    public static String getServerName(String serverConfigDir) {
        return serverConfigDir.substring(serverConfigDir.lastIndexOf('/') + 1, serverConfigDir.length());
    }

    public static String getServerName(JMXConnection jmxConnection) throws Exception {
        String serverConfigDir = getServerConfigDir(jmxConnection);
        return getServerName(serverConfigDir);
    }

    public static String getWLPInstallDir(JMXConnection jmxConnection) throws Exception {
        CompositeData metadata = (CompositeData) jmxConnection.getMetadata(Constants.WLP_INSTALL_VAR, "a");
        String wlpInstallDir = (String) metadata.get("fileName");
        if (wlpInstallDir != null) {
            wlpInstallDir = wlpInstallDir.replace("\\", "/");
        }
        return wlpInstallDir;
    }

    public static String getUserDir(JMXConnection jmxConnection) throws Exception {
        CompositeData userDirMetadata = (CompositeData) jmxConnection.getMetadata("${wlp.user.dir}", "a");
        String remoteUserPath = (String) userDirMetadata.get("fileName");
        remoteUserPath = remoteUserPath.replace("\\", "/");
        return remoteUserPath;
    }

    /**
     * Download server config files.
     *
     * @param jmxConnection
     * @param userMetaDataPath
     * @param serverName
     * @param downloadedFiles
     * @param remoteUserPath
     * @param userDir
     * @return
     */
    public static IStatus downloadServerFiles(JMXConnection jmxConnection, IPath userMetaDataPath, String serverName, ArrayList<IPath> downloadedFiles, String remoteUserPath,
                                              String userDir) {
        IStatus downloadStatus = new Status(IStatus.OK, Activator.PLUGIN_ID, Messages.remoteDownloadingServerConfigFile);

        try {
            IPath userServersDir = userMetaDataPath.append(Constants.SERVERS_FOLDER).append(serverName);
            if (!userServersDir.toFile().exists())
                FileUtil.makeDir(userServersDir);

            IPath configFilePath = userServersDir.append(Constants.SERVER_XML);
            jmxConnection.downloadFile("${server.config.dir}/server.xml", configFilePath.toOSString());
            downloadedFiles.add(configFilePath);

            List<String> filesToDownload = new ArrayList<String>();
            List<IncludeFile> includeFiles = new ArrayList<IncludeFile>();

            //get DOM to server.xml to get all the include files
            if (configFilePath.toFile().exists()) {
                List<IncludeFile> locationList = getIncludeFiles(configFilePath);
                if (locationList != null)
                    includeFiles.addAll(locationList);
            }

            //get the directory entries for remote server config dir and wlp install dir. If server config files exist, then add them to downloadFiles list
            ArrayList<CompositeData[]> listofServerDirFiles = new ArrayList<CompositeData[]>();

            try {
                listofServerDirFiles.add(jmxConnection.getDirectoryEntries("${server.config.dir}", false, ""));

                // Check if the user directory is default and under wlp install directory.
                if (WebSphereUtil.isDefaultRemoteUserDirectory(jmxConnection, remoteUserPath))
                    listofServerDirFiles.add(jmxConnection.getDirectoryEntries(remoteUserPath, false, ""));

                listofServerDirFiles.removeAll(Collections.singleton(null));
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.logError("Error getting the file list for remote server", e);
            }

            //get other config files i.e boostrap.properties, jvm.options, server.env, config dropins
            if (!listofServerDirFiles.isEmpty()) {
                for (CompositeData[] folder : listofServerDirFiles) {
                    for (CompositeData metaData : folder) {
                        String filePath = (String) metaData.get("fileName");
                        String fileName = WebSphereUtil.getLastName(filePath);
                        if (ExtendedConfigFile.JVM_OPTIONS_FILE.equals(fileName) || ExtendedConfigFile.SERVER_ENV_FILE.equals(fileName)
                            || ExtendedConfigFile.BOOTSTRAP_PROPS_FILE.equals(fileName)) {
                            filesToDownload.add(filePath);
                        } else if (Constants.CONFIG_DROPINS_FOLDER.equals(fileName)) {
                            // To handle configDropins files
                            CompositeData[] listOfConfigDropins = null;
                            try {
                                listOfConfigDropins = jmxConnection.getDirectoryEntries(filePath, true, "");
                            } catch (Exception e) {
                                if (Trace.ENABLED)
                                    Trace.logError("Error getting the file list for remote server configDropins directory", e);
                            }
                            if (listOfConfigDropins != null) {
                                for (CompositeData dropinData : listOfConfigDropins) {
                                    String dropinPath = (String) dropinData.get("fileName");
                                    String[] splitPath = dropinPath.split("/");
                                    String dropinName = splitPath[splitPath.length - 1];
                                    if (dropinName == null || dropinName.isEmpty() || splitPath.length < 2)
                                        continue;
                                    String dropinFolder = splitPath[splitPath.length - 2];
                                    // Only download jvm.options and xml files
                                    if ((dropinName.endsWith(".xml") || dropinName.equals(ExtendedConfigFile.JVM_OPTIONS_FILE))
                                        && (Constants.CONFIG_DEFAULT_DROPINS_FOLDER.equals(dropinFolder) ||
                                            Constants.CONFIG_OVERRIDE_DROPINS_FOLDER.equals(dropinFolder))) {
                                        filesToDownload.add(dropinPath);
                                    }
                                }
                            }
                            // To handle the shared case
                        } else if (Constants.SHARED_FOLDER.equals(fileName)) {
                            // Shared folder will be in listOfServerDirFiles if and only if user dir was also read
                            CompositeData[] listOfSharedFiles = null;
                            try {
                                listOfSharedFiles = jmxConnection.getDirectoryEntries(filePath, false, "");
                            } catch (Exception e) {
                                if (Trace.ENABLED)
                                    Trace.logError("Error getting the file list for remote server shared directory", e);
                            }
                            if (listOfSharedFiles != null) {
                                for (CompositeData sharedData : listOfSharedFiles) {
                                    String sharedDataPath = (String) sharedData.get("fileName");
                                    if (sharedDataPath.contains(ExtendedConfigFile.JVM_OPTIONS_FILE)) {
                                        filesToDownload.add(sharedDataPath);
                                        // there can be only one jvm.options, no need to continue the loop
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            List<IPath> downloadedIncludeFiles = new ArrayList<IPath>();
            IPath downloadToPath = null;

            //download all the files other than include files
            for (String file : filesToDownload) {
                downloadToPath = resolveAndDownload(file, jmxConnection, userMetaDataPath, remoteUserPath, userDir);
                if (downloadToPath != null) {
                    downloadedFiles.add(downloadToPath);
                }
            }

            //download all the include files and recursively read the downloaded includes to download the level 2 includes and then level 3 and so on.
            while (!includeFiles.isEmpty()) {
                for (IncludeFile file : includeFiles) {
                    try {
                        downloadToPath = resolveAndDownload(file.filepath, jmxConnection, userServersDir, remoteUserPath, userDir);
                        if (downloadToPath != null) {
                            if (!downloadedIncludeFiles.contains(downloadToPath))
                                downloadedIncludeFiles.add(downloadToPath);
                            if (!downloadedFiles.contains(downloadToPath))
                                downloadedFiles.add(downloadToPath);
                        }
                    } catch (Exception e) {
                        if (file.optional) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.INFO, "Download of file " + file.filepath + " was skipped because the optional include was not found.");
                        } else {
                            throw e;
                        }
                    }
                }
                includeFiles.clear();

                for (IPath file : downloadedIncludeFiles) {
                    List<IncludeFile> locationList = getIncludeFiles(file);
                    if (locationList != null)
                        includeFiles.addAll(locationList);
                }
                downloadedIncludeFiles.clear();
            }
        } catch (Exception e) {
            Trace.logError(Messages.remoteServerDownloadFailed, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.remoteServerDownloadFailed, e);
        }
        return downloadStatus;
    }

    private static List<IncludeFile> getIncludeFiles(IPath file) {
        if (file == null)
            return null;

        Element includeElement = null;
        Element element = null;
        List<IncludeFile> location = new ArrayList<IncludeFile>();
        Document doc = ConfigUtils.getDOMFromFile(file.toFile());

        if (doc == null)
            return null;

        element = doc.getDocumentElement();

        if (element == null)
            return null;

        includeElement = DOMUtils.getFirstChildElement(element, Constants.INCLUDE_ELEMENT);

        if (includeElement != null) {
            for (Element current = includeElement; current != null; current = DOMUtils.getNextElement(current, Constants.INCLUDE_ELEMENT)) {
                String path = DOMUtils.getAttributeValue(current, Constants.LOCATION_ATTRIBUTE);
                String optional = DOMUtils.getAttributeValue(current, Constants.OPTIONAL_ATTRIBUTE);
                boolean isOptional = optional == null ? false : Boolean.parseBoolean(optional);
                location.add(new IncludeFile(path, isOptional));
            }
        }
        return location;
    }

    private static IPath resolveAndDownload(String file, JMXConnection jmxConnection, IPath userServersPath, String remoteUserPath, String userDir) throws Exception {
        if (jmxConnection == null || file == null)
            return null;

        String resolvedPath = resolveRemotePath(file, jmxConnection, userServersPath, remoteUserPath, userDir);

        if (resolvedPath == null)
            return null;

        downloadRemoteFile(resolvedPath, jmxConnection, remoteUserPath, userDir);
        return new Path(resolvedPath);
    }

    private static String resolveRemotePath(String file, JMXConnection jmxConnection, IPath userServersPath, String remoteUserPath, String userDir) throws Exception {
        if (jmxConnection == null || file == null)
            return null;

        String filePath = null;
        String resolvedPath = null;
        CompositeData metadata = null;

        //case when file is a variable
        if (ConfigVarsUtils.containsReference(file)) {
            try {
                metadata = (CompositeData) jmxConnection.getMetadata(file, "a");
                resolvedPath = resolveRemotePath((String) metadata.get("fileName"), jmxConnection, userServersPath, remoteUserPath, userDir);
            } catch (Exception e) {
                Trace.logError("Unable to resolve variable: " + file, e);
                throw new Exception(e);
            }

        }

        // if file is absolute path
        else if (FileUtil.isAbsolutePath(file)) {
            filePath = file.replace("\\", "/");
            if (filePath.startsWith(remoteUserPath)) {
                //create corresponding local directory inside metadata remoteUsr dir
                resolvedPath = filePath.replace(remoteUserPath, userDir);
            }
        }

        //case when file is a relative path or name of the file
        else if (!(new File(file).isAbsolute())) {
            //if its relative path
            if (file.contains("/") || file.startsWith("../")) {
                URI base = userServersPath.toFile().toURI();
                //resolve the relative path against the metadata server config folder
                URI resolvedURI = base.resolve(file);
                resolvedPath = new File(resolvedURI.getPath()).toString();
            }

            //in this case, its a name of the file, download it from parent directory which is server.config.dir
            else if (new Path(file).segmentCount() == 1) {
                resolvedPath = userServersPath.append(file).toOSString();
            }
        }
        return resolvedPath;
    }

    private static void downloadRemoteFile(String resolvedPath, JMXConnection jmxConnection, String remoteUserPath, String userDir) throws Exception {

        IPath downloadTo = new Path(resolvedPath);
        String remoteDownloadPath = null;
        String key = resolvedPath.replace("\\", "/");
        String userDirResolved = userDir.replace("\\", "/");

        if (!downloadTo.removeLastSegments(1).toFile().exists())
            FileUtil.makeDir(downloadTo.removeLastSegments(1));

        if (key.startsWith(userDirResolved))
            remoteDownloadPath = key.replace(userDirResolved, remoteUserPath);

        jmxConnection.downloadFile(remoteDownloadPath, key);
    }

    public static void moveDownloadedFilesToUserDir(UserDirectory userDir, ArrayList<IPath> downloadedFiles, WebSphereRuntime wsRuntime, String serverName) throws CoreException {
        String metadataPath = wsRuntime.getRemoteUsrMetadataPath().toOSString();
        String workspace = userDir.getProject().getLocation().toOSString();
        try {
            userDir.getProject().getLocation().append(Constants.SERVERS_FOLDER).append(serverName).toFile().mkdirs();

            for (IPath aFile : downloadedFiles) {
                String fromPath = aFile.toOSString();
                IPath toPath = new Path(fromPath.replace(metadataPath, workspace));
                if (!toPath.removeLastSegments(1).toFile().exists())
                    FileUtil.makeDir(toPath.removeLastSegments(1));
                FileUtil.move(aFile, toPath);
            }
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.failedCreateDefaultUserDir, userDir.getPath()), e));
        }
    }

    public static void discardTemporaryFiles(List<IPath> downloadedFiles, WebSphereRuntime wsRuntime, String serverName) {
        for (IPath file : downloadedFiles) {
            file.toFile().delete();
        }
        downloadedFiles.clear();
        IPath usrDir = RemoteUtils.getMetadataPath(wsRuntime, serverName);
        if (usrDir != null && usrDir.toFile().exists()) {
            try {
                FileUtil.deleteDirectory(usrDir.toOSString(), true);
            } catch (IOException e) {
                Trace.logError("Failed to clean up remote server temp files", e);
            }
        }
    }

    public static IPath getMetadataPath(WebSphereRuntime runtime, String serverName) {
        return runtime.getRemoteUsrMetadataPath().append(Constants.SERVERS_FOLDER).append(serverName);
    }

    public static IPlatformHandler getPlatformHandler(WebSphereServer server) throws UnsupportedServiceException {
        PlatformType type = null;
        if (server.getServerType().equalsIgnoreCase("LibertyDocker"))
            type = PlatformType.DOCKER;
        else if (server.getServerType().equalsIgnoreCase("WASLibertyCorePlan"))
            type = PlatformType.SSH_KEYLESS;
        else
            return null;
        return PlatformHandlerFactory.getPlatformHandler(server.getServiceInfo(), type);
    }

    public static IPath getRemoteLogDirectory(WebSphereServer server) throws UnsupportedServiceException, ConnectException, IOException {
        IPath remoteLogPath = null;
        String defaultRemotePath = null;
        String logDir = null;
        if (!server.isLocalSetup()) {
            // check if server.xml has logging element
            logDir = server.getConfiguration().getLogDirectoryAttribute();
            if (logDir != null && !logDir.isEmpty())
                return resolveRemoteLogVariable(server, logDir);

            Bootstrap bootstrap = server.getBootstrap();
            if (bootstrap != null) {
                logDir = bootstrap.getLogDir();
                if (logDir != null)
                    return resolveRemoteLogVariable(server, logDir);
            }

            ServerEnv env = (ServerEnv) server.getServerInfo().getServerEnv();
            if (env != null) {
                ConfigVars v = new ConfigVars();
                env.getVariables(v);
                //When both LOG_DIR and WLP_OUTPUT_DIR are specified . Logs will be created under LOG_DIR.
                logDir = v.getValue(Constants.ENV_VAR_PREFIX + Constants.ENV_LOG_DIR);
                if (logDir != null) {
                    return resolveRemoteLogVariable(server, logDir);
                }
                logDir = v.getValue(Constants.ENV_VAR_PREFIX + Constants.WLP_OUTPUT_DIR);
                if (logDir != null) {
                    return resolveRemoteLogVariable(server, logDir);
                }
            }

            IPlatformHandler handler = getPlatformHandler(server);
            // currently there is no way to resolve system variables for remote machine
            if (handler != null && handler instanceof BaseDockerContainer) {

                logDir = handler.getEnvValue(Constants.ENV_LOG_DIR);
                if (logDir != null) {
                    return new Path(logDir);
                }
                logDir = handler.getEnvValue(Constants.WLP_OUTPUT_DIR);
                if (logDir != null) {
                    return new Path(logDir);
                }
            }
            // if log path is not specified. Resolve default log directory using jmx connection
            defaultRemotePath = server.getDefaultRemoteLogDirectory();
            if (defaultRemotePath == null)
                return null;
            remoteLogPath = new Path(defaultRemotePath);

        }
        // Local server scenario is handled in serverInfo object
        return remoteLogPath;
    }

    public static IPath resolveRemoteLogVariable(WebSphereServer server, String logdir) {
        if (logdir != null && ConfigVarsUtils.containsReference(logdir)) {
            IPath result = resolveRemoteConfigPath(server, logdir);
            if (result != null)
                return result.removeLastSegments(1);
        }
        return new Path(logdir);
    }

    private static IPath resolveRemoteConfigPath(WebSphereServer server, String remoteLog) {
        String filename = server.getServerInfo().getMessageFileName();
        String defaultRemotePath = null;
        JMXConnection jmx = null;
        IPath path = null;
        try {
            jmx = server.createJMXConnection();
            defaultRemotePath = server.getWebSphereServerBehaviour().resolveConfigVar(remoteLog, jmx);
            path = new Path(defaultRemotePath);
            return path.append(filename);
        } catch (Exception e) {
            Trace.logError("getMessagesFile :Connection to Remote Server failed", e);
            return null;
        } finally {
            if (jmx != null)
                jmx.disconnect();
        }
    }

    public static void copyRemoteInfoToServiceInfo(RemoteServerInfo remoteInfo, Map<String, String> serviceInfo) {
        int os = remoteInfo.getIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
        String osName;
        switch (os) {
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS:
                osName = "win";
                break;
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC:
                osName = "mac";
                break;
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX:
                osName = "linux";
                break;
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER:
                osName = "other";
                break;
            default:
                osName = "linux";
                break;
        }
        serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.OS_NAME, osName);
        serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.DEBUG_PORT, remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT));
        int method = remoteInfo.getIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS);
        if (method == RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS) {
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD, com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD_OS);
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER, remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID));
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD, remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD));
        } else {
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD, com.ibm.ws.st.common.core.ext.internal.Constants.LOGON_METHOD_SSH);
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.SSH_KEY, remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE));
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.OS_USER, remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID));
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.OS_PASSWORD, remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE));
        }
    }

    static class IncludeFile {
        String filepath;
        boolean optional;

        public IncludeFile(String path, boolean optional) {
            this.filepath = path;
            this.optional = optional;
        }
    }
}
