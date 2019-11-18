/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jst.server.core.ServerProfilerDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.ext.internal.util.IRemoteUtilityExecutionDelegate;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.LaunchUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;

public class LibertyRemoteUtilityExecutionDelegate implements IUtilityExecutionDelegate {
    public static final String ATTR_SERVER_PROCESS_MODE = "serverProcessMode";

    protected WebSphereServer wsServer;

    protected WebSphereServerBehaviour wsBehaviour;

    protected String serverPath = null;

    protected IPath remoteUserDir = null;

    protected String serverName = null;

    protected String serverId = null;

    protected ILaunch launch = null;

    protected boolean isHotMethodReplaceEnabled;

    protected String launchMode;

    protected int logonMethod = RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS;

    /**
     * We need to init this value ASAP because it is used by other variables and methods.
     */
    protected boolean isWindowsPlatform;

    protected String hostName = null;

    protected static final char sQUOTE = '\'';

    protected static final char dQUOTE = '\"';

    protected char SEP;

    private String id = null;

    private String password = null;

    private String sshKeyFile = null;

    private IRemoteUtilityExecutionDelegate remoteDelegate;

    /**
     * This field is used in the debug view in the properties of the launch server process.
     */
    protected String displayCommand = null;

    protected Properties remoteConfigSyncInfo = null;
    protected Properties localConfigSyncInfo = null;
    protected int amountOfWork = 5;

    @Override
    public void initialize(IServer server, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Start to execute.");
        }

        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        monitor2.beginTask(NLS.bind(Messages.L_RemoteExecutingCommands, server.getName()), amountOfWork);
        monitor2.subTask(NLS.bind(Messages.L_RemoteExecutingCommands, server.getName()));

        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, monitor2);
        wsBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, monitor2);

        // Setting variables
        serverPath = wsServer.getRemoteServerStartRuntimePath();

        validateFields(serverPath, Messages.E_RemoteServer_ProfilePathInvalid);
        serverName = wsServer.getServerName();
        validateFields(serverName, Messages.E_RemoteServer_ServerNameInvalid);
        hostName = server.getHost();
        validateFields(hostName, Messages.E_RemoteServer_HostNameInvalid);
        serverId = server.getId();

        int tempI = wsServer.getRemoteServerStartPlatform();

        // We need to do this before other values are set because it is used in other methods.
        if (tempI == RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS) {
            isWindowsPlatform = true;
        } else {
            isWindowsPlatform = false;
        }

        logonMethod = wsServer.getRemoteServerStartLogonMethod();
        if (isWindowsPlatform && logonMethod == RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS) {
            SEP = '\\';
        } else {
            SEP = '/';
        }

        serverPath = ensureEndingPathSeparator(serverPath, SEP);
        remoteUserDir = new Path(wsServer.getRemoteServerStartConfigPath());

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Server Path is: " + serverPath);
        }

        if (logonMethod == RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS) {
            id = wsServer.getRemoteServerStartOSId();
            validateFields(id, Messages.E_RemoteServer_IDInvalid);
            password = wsServer.getRemoteServerStartOSPassword();
            validateFields(password, Messages.E_RemoteServer_passwordInvalid);
        } else {
            id = wsServer.getRemoteServerStartSSHId();
            password = wsServer.getRemoteServerStartSSHPassphrase();
            sshKeyFile = wsServer.getRemoteServerStartSSHKeyFile();

            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "sshKeyFile: " + sshKeyFile);
            }

            validateFields(sshKeyFile, Messages.E_RemoteServer_sshKeyFileInvalid);
        }

        monitor2.worked(1);

        if (monitor2.isCanceled())
            return;

        initRemoteExecutionDelegate(wsServer);
    }

    private void initRemoteExecutionDelegate(WebSphereServer wsServer) throws CoreException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor("com.ibm.ws.st.common.core.ext", "remoteExecutionDelegate");

        for (IConfigurationElement ce : cf) {
            try {
                remoteDelegate = (IRemoteUtilityExecutionDelegate) ce.createExecutableExtension("class");
                remoteDelegate.init(logonMethod, id, password, hostName, isWindowsPlatform, sshKeyFile);
                return;
            } catch (CoreException e) {
                Trace.logError("Error while creating executable extension for remote execution delegate", e);
            }
        }
        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.remoteActionsUnavailable));
    }

    @Override
    public ExecutionOutput execute(WebSphereServer server, String launchMode, ILaunch launch, String execCmd,
                                   String jobName, long timeout,
                                   IProgressMonitor monitor, boolean run) throws CoreException {

        this.launch = launch;
        this.launchMode = launchMode;

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "execute parameters. command=>" + execCmd
                                    + "< serverName=" + serverName + " serverId=" + serverId);
        }

        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        int returnCode = 0;

        ExecutionOutput result = null;

        // Kick off the script to start the server.
        try {
            String jvmArgs = getJVMArgs();
            Map<String, String> envMap = new HashMap<String, String>();

            if (serverPath != null)
                envMap.put("WLP_INSTALL_DIR", serverPath);
            if (remoteUserDir != null && remoteUserDir.segmentCount() > 2)
                envMap.put("WLP_USER_DIR", remoteUserDir.removeLastSegments(2).toString());
            if (ILaunchManager.DEBUG_MODE.equals(launchMode)) {
                String debugPort = wsServer.getRemoteServerStartDebugPort();
                envMap.put("WLP_DEBUG_REMOTE", "y");
                envMap.put("WLP_DEBUG_ADDRESS", debugPort);
            }

            if (!run) {
                result = remoteDelegate.execCommand(launch, execCmd, (int) timeout, jvmArgs, envMap, monitor2);
            } else {
                result = remoteDelegate.runCommand(execCmd, (int) timeout, jvmArgs, envMap, monitor2);
            }

            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Return code, from execution: " + returnCode);
            }
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "The process cannot be launched successfully.");
            }
        } catch (CoreException e) {
            Trace.logError("An exception occured executing the command: " + execCmd, e);
            throw e;
        } catch (Exception e) {
            Trace.logError("An exception occured executing the command: " + execCmd, e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.W_RemoteServer_Problem, e.getLocalizedMessage()), e));
        }

        if (result != null && result.getReturnCode() != 0) {
            Trace.logError("Execution failed for the command: " + execCmd + ", with output: " + result.getOutput() + ", and error: " + result.getError(), null);
            String msg;
            if (result.getError() != null && !result.getError().isEmpty()) {
                msg = NLS.bind(Messages.W_RemoteServer_CommandReturnCodeWithError, new String[] { execCmd, Integer.toString(result.getReturnCode()), result.getError() });
            } else {
                msg = NLS.bind(Messages.W_RemoteServer_CommandReturnCode, new String[] { execCmd, Integer.toString(result.getReturnCode()) });
            }
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg));
        }

        return result;

    }

    /**
     * The isWindowsPlatform and profilePath must be set before calling this method.
     *
     * @param wasServer
     * @return
     */

    @Override
    public String getServerScriptFilePath() {
        String filepath;
        if (isWindowsPlatform) {
            filepath = quote(ensureEndingPathSeparator(serverPath, SEP) + "bin" + SEP + "server.bat") + " ";
        } else {
            filepath = quote(ensureEndingPathSeparator(serverPath, SEP) + "bin" + SEP + "server") + " ";
        }

        return filepath;
    }

    @Override
    public String getServerSecurityUtilityFilePath() {
        String filepath;
        if (isWindowsPlatform) {
            filepath = quote(ensureEndingPathSeparator(serverPath, SEP) + "bin" + SEP + CommandConstants.CREATE_SSL_CERTIFICATE_SECURITY_UTIL + ".bat") + " ";
        } else {
            filepath = quote(ensureEndingPathSeparator(serverPath, SEP) + "bin" + SEP + CommandConstants.CREATE_SSL_CERTIFICATE_SECURITY_UTIL) + " ";
        }
        return filepath;
    }

    @Override
    public String getServerUtilityFilePath(String command) {
        String filepath;
        if (isWindowsPlatform) {
            filepath = quote(ensureEndingPathSeparator(serverPath, SEP) + "bin" + SEP + command + ".bat") + " ";
        } else {
            filepath = quote(ensureEndingPathSeparator(serverPath, SEP) + "bin" + SEP + command) + " ";
        }
        return filepath;
    }

    protected String ensureEndingPathSeparator(String aPath, char SEP) {
        String path = aPath;
        if (path != null && path.length() > 0) {
            if (path.charAt(path.length() - 1) != SEP) {
                path += SEP;
            }
        }
        return path;
    }

    protected void validateFields(String s, String msg) throws CoreException {
        if (s == null || s.trim().isEmpty()) {
            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg);
            throw new CoreException(status);
        }
    }

    protected String quote(final String in) {
        char c = isWindowsPlatform ? dQUOTE : sQUOTE;
        String s = in;
        if (in != null) {
            if (in.charAt(0) != c) {
                s = c + in;
            }
            if (in.charAt(in.length() - 1) != c) {
                s += c;
            }
        }
        return s;
    }

    /**
     * JVMArgs are set only in case of remote start.
     */
    String getJVMArgs() throws Exception {
        String args = "";

        String errorPage = LaunchUtil.getErrorPage();
        if (errorPage != null) {
            if (args.length() != 0)
                args += " ";
            args += WebSphereLaunchConfigurationDelegate.VM_ERROR_PAGE + errorPage;
        }

        if (ILaunchManager.PROFILE_MODE.equals(launchMode)) {
            ILaunchConfiguration configuration = null;
            configuration = launch != null ? launch.getLaunchConfiguration() : null;
            String vmArgs = "";
            if (configuration != null) {
                vmArgs = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
            }

            IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
            VMRunnerConfiguration runConfig = new VMRunnerConfiguration("n/a", new String[0]);

            runConfig.setVMArguments(DebugPlugin.parseArguments(vmArgs));
            try {
                // should only be calling configureProfiling on server start (not on stop)
                ServerProfilerDelegate.configureProfiling(launch, vmInstall, runConfig, new NullProgressMonitor());
                String[] newVmArgs = runConfig.getVMArguments();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < newVmArgs.length; i++) {
                    if (i > 0)
                        sb.append(" ");
                    String s = newVmArgs[i];
                    if (!s.contains("\""))
                        sb.append("\"" + s + "\"");
                    else
                        sb.append(s);
                }
                vmArgs = sb.toString();

                if (!vmArgs.isEmpty()) {
                    if (args.length() != 0)
                        args += " ";
                    args += vmArgs;
                }
            } catch (Exception e) {
                wsBehaviour.stopImpl();
                throw e;
            }
        }

        // get vm args provided by server pre-start extensions
        ServerStartInfo startInfo = new ServerStartInfo(wsServer.getServer(), launchMode);
        args = LaunchUtilities.processVMArguments(args, startInfo);
        return args;
    }

    @Override
    public IPath getLocalUserDir() {
        return wsServer.getServerInfo().getUserDirectory().getProject().getLocation().append(Constants.SERVERS_FOLDER).append(serverName);
    }

    @Override
    public IPath getRemoteUserDir() {
        return new Path(wsServer.getRemoteServerStartConfigPath());
    }

    /**
     * checks the status of fileName/DirName appended at the end of remoteFilePath variable
     * if its a large file, this method periodically checks for the size of file and return true when size is stable
     *
     * @param remoteFilePath Path to the remote file/dir for which status is checked
     * @param isLargeFile    if the file has a large size e.g. 10 Mb
     * @throws CoreException
     */
    public boolean checkFileStatus(IPath remoteFilePath, boolean isLargeFile) throws CoreException {
        String remoteDir = remoteFilePath.removeLastSegments(1).toOSString();
        String fileName = remoteFilePath.lastSegment();
        String[] files;
        long remoteFileSize = -1;
        long newRemoteFileSize = -1;
        boolean fileExists = false;
        int count = 0;

        //check if file exists, loop 10 times (its possible that the specified file exist already, in that case the file is overriden but
        // it could falsely return true
        while (count <= 10 && !fileExists) {
            files = remoteDelegate.listFiles(remoteDir);
            for (String file : files) {
                if (file.equals(fileName)) {
                    fileExists = true;
                    // TODO test retrieval of file size
                    remoteFileSize = remoteDelegate.getFileSize(remoteDir, fileName);
                    break;
                }
            }
            if (!fileExists) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    //do nothing
                }
            }
            count++;
        }

        //check the size of the file continously to see if it changed since previous check, if it did then command is still in progress.
        if (isLargeFile && fileExists && newRemoteFileSize != 0) {
            while (remoteFileSize != newRemoteFileSize) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    //do nothing
                }
                remoteFileSize = newRemoteFileSize;
                files = remoteDelegate.listFiles(remoteDir);
                for (String file : files) {
                    if (file.equals(fileName)) {
                        newRemoteFileSize = remoteDelegate.getFileSize(remoteDir, fileName);
                        break;
                    }
                }
            }
        }
        return fileExists;
    }

    public boolean waitForDeleteOperationToComplete(IPath remoteFilePath) throws CoreException {
        String remoteDir = remoteFilePath.removeLastSegments(1).toOSString();
        String remoteFile = remoteFilePath.lastSegment();
        String[] files;
        boolean fileExists = false;
        int count = 0;

        //check if file exists, loop 10 times (its possible that the specified file exist already, in that case the file is overriden but
        // it could falsely return true
        while (fileExists && count < 10) {
            files = remoteDelegate.listFiles(remoteDir);
            for (String file : files) {
                if (file.equals(remoteFile)) {
                    fileExists = true;
                    break;
                }
            }

            if (fileExists) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //do nothing
                }
            }
            count++;
        }
        return fileExists;
    }

    @Override
    public void modifyConfigFile(String includeFileName) {
        ConfigurationFile serverConfig = wsServer.getServerInfo().getConfigRoot();
        boolean includeExists = false;
        // Include the generated config file in the server config if it isn't already there
        for (ConfigurationFile config : serverConfig.getAllIncludedFiles()) {
            if (config.getPath().lastSegment().equals(includeFileName)) {
                includeExists = true;
                break;
            }
        }

        // Add the include to the server config if it isn't already added
        if (!includeExists) {
            serverConfig.addInclude(false, includeFileName);
            try {
                serverConfig.save(null);
            } catch (IOException e) {
                Trace.logError("Error occured while executing join collective command for remote server", e);
            }
        }

        try {
            wsServer.createJMXConnection();
        } catch (Exception e) {
            //means jmxConnection was not established with the server, in this case auto sync on server.xml file won't work.
            //so upload the file using remote utility.
            try {
                uploadFile(getLocalUserDir().append(Constants.SERVER_XML).toOSString(), remoteUserDir.append(Constants.SERVER_XML).toOSString());
            } catch (CoreException e1) {
                Trace.logError("Error uploading " + serverConfig.getName() + " to the remote sever.", e1);
            }
        }
    }

    public void fillRemoteConfigSyncInfo(String remoteFileLocation) throws CoreException {

        //update sync config file for remote files
        if (wsBehaviour.getTempDirectory().append(WebSphereServerBehaviour.REMOTE_CONFIG_SYNC_FILENAME).toFile().exists()) {
            remoteConfigSyncInfo = new Properties();
            FileUtil.loadProperties(remoteConfigSyncInfo, wsBehaviour.getTempDirectory().append(WebSphereServerBehaviour.REMOTE_CONFIG_SYNC_FILENAME));
            String cachedTS = remoteConfigSyncInfo.getProperty(remoteFileLocation);
            long timeStamp = remoteDelegate.getTimeStamp(remoteFileLocation);
            Date date = new Date(timeStamp / 1000000L);
            String lastModified = date.toString();
            if (lastModified != null) {
                if (cachedTS != null) {
                    if (!lastModified.equals(cachedTS)) {
                        remoteConfigSyncInfo.put(remoteFileLocation.replace("\\", "/"), lastModified);
                    }
                } else {
                    remoteConfigSyncInfo.put(remoteFileLocation.replace("\\", "/"), lastModified);
                }
            }
            wsBehaviour.saveProperties(remoteConfigSyncInfo, WebSphereServerBehaviour.REMOTE_CONFIG_SYNC_FILENAME);
        }
    }

    public void fillLocalConfigSyncInfo(String localFileLocation) {
        //update config sync file for local files
        if (wsBehaviour.getTempDirectory().append(WebSphereServerBehaviour.CONFIG_SYNC_FILENAME).toFile().exists()) {
            localConfigSyncInfo = new Properties();
            FileUtil.loadProperties(localConfigSyncInfo, wsBehaviour.getTempDirectory().append(WebSphereServerBehaviour.CONFIG_SYNC_FILENAME));
            String key = localFileLocation.replace("\\", "/");
            File file = new File(key);
            String cachedTS = localConfigSyncInfo.getProperty(key);
            if (cachedTS != null) {
                Date d = new Date(file.lastModified());
                //String s = Long.toString(file.lastModified());
                String s = d.toString();
                if (!s.equals(cachedTS)) {
                    localConfigSyncInfo.put(key, s);
                }
            } else {
                localConfigSyncInfo.put(key, (new Date(file.lastModified())).toString());
            }
            wsBehaviour.saveProperties(localConfigSyncInfo, WebSphereServerBehaviour.CONFIG_SYNC_FILENAME);
        }
    }

    @Override
    public boolean matchPasswordField(IPath filePath, String expectedPassword) {
        boolean passwordsMatch = true;
        try {
            ConfigurationFile file = new ConfigurationFile(filePath.toFile().toURI(), wsServer.getUserDirectory(), wsServer.getServerInfo());
            Document doc = file.getDocument();
            if (doc != null) {
                Element elem = doc.getDocumentElement();
                Element keyStoreElem = DOMUtils.getFirstChildElement(elem, Constants.KEY_STORE);
                if (keyStoreElem != null) {
                    String actualPassword = PasswordUtil.decode(keyStoreElem.getAttribute(Constants.PASSWORD_TYPE));
                    if (!actualPassword.equals(expectedPassword)) {
                        file.setAttribute(Constants.KEY_STORE, Constants.PASSWORD_TYPE, expectedPassword);
                        file.save(null);
                        passwordsMatch = false;
                    }
                }
            }
        } catch (Exception e) {
            Trace.logError("Unable to fetch the keystore password in file: " + filePath, e);
        }
        return passwordsMatch;
    }

    @Override
    public boolean isDockerExecutionDelegate() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public IPath getRemoteOutputDir() {
        return remoteUserDir;
    }

    /** Remote process */
    @Override
    public boolean isExecutionSuccessful(ExecutionOutput pr, String expectedSysOut) throws CoreException {
        if (pr.getReturnCode() != 0)
            return false;
        if (pr.getReturnCode() == 0 && !pr.getOutput().contains(expectedSysOut))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void startExecution() throws CoreException {
        remoteDelegate.startExecution();
    }

    /** {@inheritDoc} */
    @Override
    public void endExecution() {
        if (remoteDelegate != null)
            remoteDelegate.endExecution();
    }

    /** {@inheritDoc} */
    @Override
    public boolean fileExists(String remoteFilePath) throws Exception {
        return remoteDelegate.fileExists(remoteFilePath);
    }

    /** {@inheritDoc} */
    @Override
    public void renameFile(String remoteOldFileName, String newName) throws Exception {
        remoteDelegate.renameFile(remoteOldFileName, newName);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteFile(String remoteFilePath, boolean recursiveDelete, boolean force) throws Exception {
        remoteDelegate.deleteFile(remoteFilePath, recursiveDelete, force);
    }

    @Override
    public void uploadFile(String localFile, String destinationFile) throws CoreException {
        remoteDelegate.uploadFile(localFile, destinationFile);
    }

    /** {@inheritDoc} */
    @Override
    public void downloadFile(IPath remoteFile, String destFile, IProgressMonitor monitor) throws CoreException {
        remoteDelegate.downloadFile(remoteFile, destFile, monitor);
    }

    @Override
    public String getOSString(String path) {
        String osPath = path;
        if (!isWindowsPlatform)
            osPath = path.replace("\\", "/");
        else
            osPath = path.replace("/", "\\");

        return osPath;
    }
}