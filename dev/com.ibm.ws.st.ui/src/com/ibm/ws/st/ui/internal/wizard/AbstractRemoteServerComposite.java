/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.w3c.dom.Document;

import com.ibm.ws.st.common.core.ext.internal.AbstractServerSetup;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.jmx.JMXConnectionException;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 *
 */
public abstract class AbstractRemoteServerComposite extends AbstractWebSphereServerComposite {

    protected ArrayList<IPath> downloadedFiles = new ArrayList<IPath>(2);
    protected String serverConfigDir = "";
    protected String remoteUserPath = null;
    protected String userDir = null;
    protected String serverName = "defaultServer";

    protected AbstractRemoteServerComposite(Composite parent, IWizardHandle wizard) {
        super(parent, wizard);
    }

    protected abstract String getUserId();

    protected abstract String getUserPassword();

    protected abstract String getHost();

    protected abstract String getPort();

    /*
     * Validate the user security
     */
    // TODO: There's probably a better way to get the return value of the validation..
    protected ArrayList<Integer> remoteSecurityValidation(final AbstractServerSetup serverSetup) {

        final ArrayList<Integer> result = new ArrayList<Integer>();

        IRunnableWithProgress runnable = new IRunnableWithProgress() {

            @SuppressWarnings("boxing")
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                SubMonitor mon = SubMonitor.convert(monitor, Messages.taskValidateSecurity, 100);

                // Validate the user security
                String user = getUserId();
                String password = getUserPassword();
                int validation = serverSetup.validateRemoteSecurity(user, password, mon.newChild(100));

                result.add(validation);
            }
        };
        try {
            wizard.run(true, true, runnable);
        } catch (Exception e) {
            Trace.logError("Error validating the user security", e);
        }
        return result;
    }

    /*
     *
     */

    protected void remoteSecurityUpdate(final AbstractServerSetup serverSetup, final int code) {
        IRunnableWithProgress runnable = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                IProgressMonitor mon = monitor;

                if (mon == null)
                    mon = new NullProgressMonitor();
                mon.beginTask(Messages.taskUpdateSecurity, 110);
                mon.worked(10);

                String user = getUserId();
                String password = getUserPassword();
                serverSetup.updateRemoteSecurity(user, password, code, mon);
            }
        };
        try {
            wizard.run(true, true, runnable);
        } catch (Exception e) {
            Trace.logError("Error validating the user security", e);
        }
    }

    /*
     * Set up the server config on the remote machine using serverSetup if not null (add remote
     * administration setup, security).
     * Download the server config files to a temporary location.
     */
    protected MultiStatus remoteConfigSetup(final AbstractServerSetup serverSetup) {
        final String user = getUserId();
        final String passw = getUserPassword();
        final String host = getHost();
        final String port = getPort();

        final MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, com.ibm.ws.st.core.internal.Messages.remoteDownloadingServerConfigFile, null);

        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                SubMonitor mon = SubMonitor.convert(monitor, Messages.taskPreparingServer, 150);

                IStatus downloadStatus = new Status(IStatus.OK, Activator.PLUGIN_ID, com.ibm.ws.st.core.internal.Messages.remoteDownloadingServerConfigFile);

                discardTemporaryFiles();

                if (mon.isCanceled()) {
                    multiStatus.add(Status.CANCEL_STATUS);
                    return;
                }
                mon.worked(10);

                if (serverSetup != null) {
                    try {
                        serverSetup.setup(mon.newChild(80));
                    } catch (Exception e) {
                        Trace.logError("Setting up the server for remote administration failed.", e);
                        multiStatus.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorRemoteServerSetup, e));
                    }
                }

                if (mon.isCanceled()) {
                    multiStatus.add(Status.CANCEL_STATUS);
                    return;
                }

                mon.setWorkRemaining(60);
                mon.setTaskName(Messages.taskConnecting);

                JMXConnection jmxConnection = null;
                // attempt JMX connection
                try {
                    jmxConnection = connect(user, passw, host, port);
                } catch (JMXConnectionException e) {
                    MessageDialog.openError(getShell(), Messages.editorVerifyConnectionError, com.ibm.ws.st.core.internal.Messages.remoteJMXConnectionFailure + e.getStackTrace());
                    multiStatus.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, com.ibm.ws.st.core.internal.Messages.remoteJMXConnectionFailure + e));
                    serverConfigDir = null;
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Failed to establish JMX connection with server", e);
                    }
                }

                if (mon.isCanceled()) {
                    multiStatus.add(Status.CANCEL_STATUS);
                    return;
                }
                mon.worked(20);

                if (serverWC == null) {
                    Trace.logError("Remote server creation failed. Server working copy is not initialized.", null);
                    downloadStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.wizRemoteServerDownloadFailed);
                }

                // attempt to download server config files
                if (jmxConnection != null) {
                    mon.setTaskName(Messages.wizRemoteDownloadingServerConfigFile);
                    try {
                        // Get the value for ${server.config.dir} to set the correct server name
                        serverConfigDir = RemoteUtils.getServerConfigDir(jmxConnection);
                        serverName = RemoteUtils.getServerName(serverConfigDir);
                        IStatus serverValidation = validateServerName();
                        if (!serverValidation.isOK()) {
                            downloadStatus = serverValidation;
                            return;
                        }
                        serverWC.setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH, serverConfigDir);

                        String wlpInstallDir = RemoteUtils.getWLPInstallDir(jmxConnection);
                        serverWC.setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH, wlpInstallDir);

                        String osName = (String) jmxConnection.getMBeanAttribute("java.lang:type=OperatingSystem", "Name");
                        boolean isWindows = osName.toLowerCase().contains("windows");
                        if (isWindows)
                            serverWC.setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
                        else
                            serverWC.setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER);

                        // Get the value for remote ${wlp.user.dir}
                        remoteUserPath = RemoteUtils.getUserDir(jmxConnection);

                        userDir = (server.getWebSphereRuntime().getRemoteUsrMetadataPath().toOSString()).replace("\\", "/");

                        // TODO: Remove temporary code to download config files once runtime APIs are available
                        IPath userMetaDataPath = server.getWebSphereRuntime().getRemoteUsrMetadataPath();

                        if (mon.isCanceled()) {
                            multiStatus.add(Status.CANCEL_STATUS);
                            return;
                        }
                        mon.worked(10);

                        downloadStatus = RemoteUtils.downloadServerFiles(jmxConnection, userMetaDataPath, serverName, downloadedFiles, remoteUserPath, userDir);

                        if (mon.isCanceled()) {
                            multiStatus.add(Status.CANCEL_STATUS);
                            return;
                        }
                        mon.worked(20);
                    } catch (Exception e) {
                        Trace.logError(com.ibm.ws.st.core.internal.Messages.remoteServerDownloadFailed, e);
                        downloadStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, com.ibm.ws.st.core.internal.Messages.remoteServerDownloadFailed, e);
                    }
                }
                multiStatus.add(downloadStatus);
                mon.setWorkRemaining(10);

                if (downloadedFiles.size() > 0 && downloadStatus != null && downloadStatus.isOK()) {
                    try {
                        File file = downloadedFiles.get(0).toFile();
                        InputStream in = new BufferedInputStream(new FileInputStream(file));
                        ConfigurationFile.documentLoad(in);
                    } catch (Exception e) {
                        Trace.logError("The server configuration validation failed", e);
                        multiStatus.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorRemoteServerConfigInvalid, e));
                    }
                }

                if (mon.isCanceled()) {
                    multiStatus.add(Status.CANCEL_STATUS);
                    return;
                }
                mon.worked(10);

                mon.done();
            }
        };

        try {
            wizard.run(true, true, runnable);
        } catch (Exception e1) {
            Trace.logError("An unexpected exception occured while setting up the remote server", e1);
            multiStatus.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorRemoteServerSetup, e1));
        }

        return multiStatus;
    }

    /*
     * Create the user directory and copy over the server config files from the temporary location.
     * Delete the temporary files.
     */
    protected UserDirectory createUserDir(String userDirName, IProgressMonitor monitor) throws CoreException {
        UserDirectory userDir = null;
        try {
            server.setServerName(serverName);
            userDir = RemoteUtils.createUserDir(server, remoteUserPath, userDirName, monitor);
            /*
             * Should get the WebSphereRuntime after RemoteUtils.createUserDir since the runtime will change when it is saved.
             * If the runtime is retrieved before the user directory is created then the server cache won't be updated correctly.
             */
            WebSphereRuntime wsRuntime = server.getWebSphereRuntime();
            if (userDir != null) {
                RemoteUtils.moveDownloadedFilesToUserDir(userDir, downloadedFiles, wsRuntime, serverName);
                server.setUserDir(userDir);
                // we need to update the server cache after moving the files
                wsRuntime.updateServerCache(true);
            }
            removeGeneratedMetaData(wsRuntime.getRuntime());
        } catch (CoreException e) {
            // clean up in case of errors
            try {
                if (userDir != null)
                    userDir.getProject().delete(true, true, monitor);
            } catch (Exception e1) {
                // ignore
            }
            throw e;
        }
        return userDir;
    }

    protected JMXConnection connect(String user, String password, String host, String port) throws JMXConnectionException {
        if (server == null)
            return null;

        JMXConnection jmxConnection = null;
        jmxConnection = new JMXConnection(host, port, user, password);

        // Need to retry a few times here since it takes time for the server
        // to detect and respond to any configuration changes (such as adding
        // config dropins files for user registry and remote administration).
        jmxConnection.connect(5000, 500);

        return jmxConnection;
    }

    protected Document getServerConfigDocument() {
        Document document = null;
        try {
            if (downloadedFiles.size() > 0) {
                document = ConfigUtils.getDOMFromFile(downloadedFiles.get(0).toFile());
            }
        } catch (Throwable t) {
            Trace.logError("Error loading config tree", t);
        }
        return document;
    }

    protected void discardTemporaryFiles() {
        for (IPath file : downloadedFiles) {
            file.toFile().delete();
        }
        downloadedFiles.clear();
        IPath usrDir = RemoteUtils.getMetadataPath(server.getWebSphereRuntime(), serverName);
        if (usrDir != null && usrDir.toFile().exists()) {
            try {
                FileUtil.deleteDirectory(usrDir.toOSString(), true);
            } catch (IOException e) {
                if (Trace.ENABLED) {
                    Trace.logError("Failed to clean up remote server temp files", e);
                }
            }
        }
    }

    protected IStatus validateServerName() {
        if (server == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorNoServers);

        if (serverName == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorNoServers);

        return new Status(IStatus.OK, Activator.PLUGIN_ID, "");
    }

}
