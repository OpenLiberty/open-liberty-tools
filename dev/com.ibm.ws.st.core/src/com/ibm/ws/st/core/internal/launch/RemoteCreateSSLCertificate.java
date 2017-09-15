/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal.launch;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 * Ceate SSL certificate utility for remote server
 */
public class RemoteCreateSSLCertificate {

    int timeout;
    private String password = null;
    private String passwordEncoding = null;
    private String passwordKey = null;
    private String subject = null;
    private String includeFileName = null;
    private int validity = 0;
    boolean cleanTempFiles = false;
    String remoteKeyFilePath;
    IPath remoteSSLIncludeFilePath;
    IPath localSSLIncludeFilePath;
    IPath remoteKeystoreFile;
    IPath localKeystoreFile;
    IUtilityExecutionDelegate executionDelegate = null;
    private String serverName = null;
    private final String EXPECTED_SYS_OUT = "Created SSL certificate";

    public RemoteCreateSSLCertificate(Map<String, String> commandVariables, int timeout, IUtilityExecutionDelegate helper) {
        this.timeout = timeout;
        this.executionDelegate = helper;
        if (commandVariables != null && !commandVariables.isEmpty()) {
            this.password = commandVariables.get(CommandConstants.PASSWORD);
            String tempString = commandVariables.get(CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_ENCODING);
            if (tempString != null && !tempString.isEmpty())
                this.passwordEncoding = tempString;
            tempString = commandVariables.get(CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_KEY);
            if (tempString != null && !tempString.isEmpty())
                this.passwordKey = tempString;
            tempString = commandVariables.get(CommandConstants.CREATE_CONFIG_FILE);
            if (tempString != null && !tempString.isEmpty())
                this.includeFileName = tempString;
            tempString = commandVariables.get(CommandConstants.CREATE_SSL_CERTIFICATE_VALIDITY);
            if (tempString != null && !tempString.isEmpty())
                if (Integer.parseInt(tempString) > 0)
                    this.validity = Integer.parseInt(tempString);
            tempString = commandVariables.get(CommandConstants.CREATE_SSL_CERTIFICATE_SUBJECT);
            if (tempString != null && !tempString.isEmpty())
                this.subject = tempString;
        }
    }

    public void execute(WebSphereServer server, String launchMode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Executing create SSL certificate command for remote server");
        try {
            this.serverName = server.getServerName();
            executionDelegate.initialize(server.getServer(), monitor);
            executionDelegate.startExecution();
            preExecute(server.getServer(), monitor);
            ExecutionOutput result = executionDelegate.execute(server, launchMode, launch, getCommand(), NLS.bind(Messages.taskCreateSSLCertificate, serverName), getTimeout(),
                                                               monitor,
                                                               true);
            if (!executionDelegate.isExecutionSuccessful(result, EXPECTED_SYS_OUT)) {
                Trace.logError("Remote command failed: " + getCommand() + ", exit value: " + result.getReturnCode() + ", output: " + result.getOutput() + ", error: "
                               + result.getError(), null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, result.getError()));
            }
            postExecute(monitor);
            monitor.done();
        } catch (CoreException ce) {
            Trace.logError("Exception occured in RemoteCreateSSLCertificate", ce);
            try {
                if (cleanTempFiles)
                    restoreTempFiles();
                else
                    discardTemporaryFiles();
            } catch (Exception e) {
                Trace.logError("Cleanup action failed", e);
            }
            if (ce.getLocalizedMessage() == null)
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction));
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction, ce));
        } catch (Exception e) {
            Trace.logError("CreateSSLCertificate utility failed", e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction));
        } finally {
            executionDelegate.endExecution();
        }
    }

    int getTimeout() {
        return timeout;
    }

    protected String getCommand() {
        StringBuilder command = new StringBuilder();
        command.append(executionDelegate.getServerSecurityUtilityFilePath());
        command.append(CommandConstants.CREATE_SSL_CERTIFICATE + " ");
        command.append(CommandConstants.CREATE_SSL_CERTIFICATE_SERVER + serverName + " ");
        command.append(CommandConstants.PASSWORD + password);
        if (passwordEncoding != null)
            command.append(" " + CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_ENCODING + passwordEncoding);
        if (passwordKey != null)
            command.append(" " + CommandConstants.CREATE_SSL_CERTIFICATE_PASSWORD_KEY + passwordKey + " ");
        if (includeFileName != null)
            command.append(" " + CommandConstants.CREATE_CONFIG_FILE + executionDelegate.getRemoteUserDir().append(includeFileName));
        if (validity > 0)
            command.append(" " + CommandConstants.CREATE_SSL_CERTIFICATE_VALIDITY + validity + " ");
        if (subject != null)
            command.append(" " + CommandConstants.CREATE_SSL_CERTIFICATE_SUBJECT + subject);
        return command.toString();
    }

    private void preExecute(IServer server, IProgressMonitor monitor) throws Exception {
        executionDelegate.initialize(server, monitor);
        remoteKeyFilePath = executionDelegate.getOSString(executionDelegate.getRemoteOutputDir().append(Constants.RESOURCES_FOLDER).append(Constants.SECURITY_FOLDER).append(Constants.SSL_KEY_FILE).toString());
        remoteSSLIncludeFilePath = executionDelegate.getRemoteUserDir().append("GeneratedSSLInclude").addFileExtension("xml");
        remoteKeystoreFile = executionDelegate.getRemoteUserDir().append(Constants.CONFIG_DROPINS_FOLDER).append(Constants.CONFIG_DEFAULT_DROPINS_FOLDER).append(Constants.KEYSTORE_XML).addFileExtension("xml");
        localKeystoreFile = executionDelegate.getLocalUserDir().append(Constants.CONFIG_DROPINS_FOLDER).append(Constants.CONFIG_DEFAULT_DROPINS_FOLDER).append(Constants.KEYSTORE_XML).addFileExtension("xml");
        localSSLIncludeFilePath = executionDelegate.getLocalUserDir().append(Constants.GENERATED_SSL_INCLUDE);
        createTempFiles();
    }

    private void postExecute(IProgressMonitor monitor) throws Exception {
        cleanTempFiles = false;
        if (!executionDelegate.isDockerExecutionDelegate())
            waitForCommandToFinish();
        if (executionDelegate.fileExists(executionDelegate.getOSString(remoteSSLIncludeFilePath.toString()))) {

            //download the new GeneratedSSLInclude.xml file created on remote machine
            executionDelegate.downloadFile(remoteSSLIncludeFilePath, executionDelegate.getLocalUserDir().append(Constants.GENERATED_SSL_INCLUDE).toString(),
                                           new SubProgressMonitor(monitor, 2));

            //update sync files to add newly downloaded include file
            if (!executionDelegate.isDockerExecutionDelegate())
                updateSyncConfigInfo();

            //make sure the passwords match
            if (!executionDelegate.matchPasswordField(localSSLIncludeFilePath, password)) {
                executionDelegate.uploadFile(localSSLIncludeFilePath.toOSString(), executionDelegate.getOSString(remoteSSLIncludeFilePath.toString()));
                //update sync files to add newly downloaded include file
                if (!executionDelegate.isDockerExecutionDelegate())
                    updateSyncConfigInfo();
            }

            //modify the config file
            executionDelegate.modifyConfigFile(includeFileName);

            //clean up temp files
            discardTemporaryFiles();
        } else { //something went wrong, rename the temp files to old file names
            restoreTempFiles();
        }
    }

    private void createTempFiles() throws Exception {

        cleanTempFiles = true;
        String remoteKeystoreFilePath = executionDelegate.getOSString(remoteKeystoreFile.toOSString());
        String remoteSSLIncludeFile = executionDelegate.getOSString(remoteSSLIncludeFilePath.toString());

        //rename the remote keystore.xml to keystore.xml.tmp file if exists
        if (executionDelegate.fileExists(remoteKeystoreFilePath)) {
            executionDelegate.renameFile(remoteKeystoreFilePath, remoteKeystoreFilePath + ".tmp");
        }

        //rename the local keystore file to GeneratedSSLInclude.xml.tmp
        if (localKeystoreFile.toFile().exists())
            localKeystoreFile.toFile().renameTo(new File(localKeystoreFile.toFile().getAbsolutePath() + ".tmp"));

        //rename the remote key to key.jks.tmp file if exists
        if (executionDelegate.fileExists(remoteKeyFilePath)) {
            executionDelegate.renameFile(remoteKeyFilePath, remoteKeyFilePath + ".tmp");
        }

        //rename the remote GeneratedSSLInclude file to GeneratedSSLInclude.xml.tmp
        if (executionDelegate.fileExists(remoteSSLIncludeFile))
            executionDelegate.renameFile(remoteSSLIncludeFile,
                                         remoteSSLIncludeFile + ".tmp");

        //rename the local GeneratedSSLInclude file to GeneratedSSLInclude.xml.tmp
        if (localSSLIncludeFilePath.toFile().exists())
            localSSLIncludeFilePath.toFile().renameTo(new File(localSSLIncludeFilePath.toFile().getAbsolutePath() + ".tmp"));
    }

    private void waitForCommandToFinish() throws CoreException {
        LibertyRemoteUtilityExecutionDelegate delegate = (LibertyRemoteUtilityExecutionDelegate) executionDelegate;
        delegate.checkFileStatus(executionDelegate.getRemoteUserDir().append(includeFileName), false);
    }

    private void discardTemporaryFiles() throws Exception {
        String remoteTempKeyFilePath = executionDelegate.getOSString(remoteKeyFilePath + ".tmp");
        String remoteTempKeystoreFilePath = executionDelegate.getOSString(remoteKeystoreFile + ".tmp");
        IPath localTempSSLIncludeFilePath = executionDelegate.getLocalUserDir().append(Constants.GENERATED_SSL_INCLUDE + ".tmp");
        String remoteTempSSLIncludeFilePath = executionDelegate.getOSString(remoteSSLIncludeFilePath + ".tmp");
        IPath localTempKeystoreFile = executionDelegate.getLocalUserDir().append(Constants.CONFIG_DROPINS_FOLDER).append(Constants.CONFIG_DEFAULT_DROPINS_FOLDER).append(Constants.KEYSTORE_XML).addFileExtension("xml.tmp");

        if (executionDelegate.fileExists(remoteTempKeystoreFilePath)) {
            executionDelegate.deleteFile(remoteTempKeystoreFilePath, false, false);
        }

        if (executionDelegate.fileExists(remoteTempKeyFilePath))
            executionDelegate.deleteFile(remoteTempKeyFilePath, false, false);

        if (executionDelegate.fileExists(remoteTempSSLIncludeFilePath))
            executionDelegate.deleteFile(remoteTempSSLIncludeFilePath, false, false);

        if (localTempSSLIncludeFilePath.toFile().exists())
            localTempSSLIncludeFilePath.toFile().delete();

        if (localTempKeystoreFile.toFile().exists())
            localTempKeystoreFile.toFile().delete();
    }

    private void restoreTempFiles() throws Exception {
        File localKeystore = new File(localKeystoreFile.toOSString() + ".tmp");
        File localSSLIncludetemp = new File(localSSLIncludeFilePath.toOSString() + ".tmp");
        String remoteTempKeystoreFilePath = executionDelegate.getOSString(remoteKeystoreFile.toString());
        String remoteTempSSLInclude = executionDelegate.getOSString(remoteSSLIncludeFilePath.toString());

        if (executionDelegate.fileExists(remoteKeyFilePath + ".tmp"))
            executionDelegate.renameFile(remoteKeyFilePath + ".tmp", remoteKeyFilePath);
        if (localSSLIncludetemp.exists())
            localSSLIncludetemp.renameTo(new File(localSSLIncludeFilePath.toOSString()));
        if (localKeystore.exists())
            localKeystore.renameTo(new File(localKeystoreFile.toOSString()));
        if (executionDelegate.fileExists(remoteTempSSLInclude + ".tmp"))
            executionDelegate.renameFile(remoteTempSSLInclude + ".tmp",
                                         remoteTempSSLInclude);
        if (executionDelegate.fileExists(remoteTempKeystoreFilePath + ".tmp"))
            executionDelegate.renameFile(remoteTempKeystoreFilePath + ".tmp", remoteTempKeystoreFilePath);
    }

    private void updateSyncConfigInfo() throws CoreException {
        LibertyRemoteUtilityExecutionDelegate delegate = (LibertyRemoteUtilityExecutionDelegate) executionDelegate;
        delegate.fillLocalConfigSyncInfo(localSSLIncludeFilePath.toOSString());
        delegate.fillRemoteConfigSyncInfo(remoteSSLIncludeFilePath.toOSString());
    }

}