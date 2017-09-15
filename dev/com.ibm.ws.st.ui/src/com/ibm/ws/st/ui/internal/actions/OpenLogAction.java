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
package com.ibm.ws.st.ui.internal.actions;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Iterator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public abstract class OpenLogAction extends SelectionProviderAction {
    protected WebSphereServerInfo wsServerInfo;
    protected WebSphereServer wsServer;
    IPath logFilePath = null;
    protected final Shell shell;

    public OpenLogAction(String name, ISelectionProvider sp, Shell shell) {
        super(sp, name);
        this.shell = shell;
        selectionChanged(getStructuredSelection());
    }

    public abstract IPath getServerInfoLogFile();

    public abstract IPath getServerLogFile() throws ConnectException, UnsupportedServiceException, IOException;

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.isEmpty()) {
            setEnabled(false);
            return;
        }
        boolean enabled = false;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof WebSphereServerInfo) {
                wsServerInfo = (WebSphereServerInfo) obj;
                wsServer = WebSphereUtil.getWebSphereServer(wsServerInfo);
            } else if (obj instanceof IServer) {
                IServer server = (IServer) obj;
                wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                if (wsServer == null) {
                    setEnabled(false);
                    return;
                }
                wsServerInfo = wsServer.getServerInfo();
            } else {
                setEnabled(false);
                return;
            }
        }
        // remote server always needs websphere server object to retrieve logs
        if (wsServer != null && !wsServer.isLocalSetup()) {
            if (wsServer.getServer().getServerState() != IServer.STATE_STARTED) {
                setEnabled(false);
                return;
            }
            enabled = true;
        } else {
            // for localserver enable menu option if log file is present
            if (getServerInfoLogFile() != null && getServerInfoLogFile().toFile().exists())
                enabled = true;

        }
        setEnabled(enabled);
    }

    @Override
    public void run() {

        Job job = new Job(Messages.downloadLogJob) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {

                // for remote host access log files using websphere object ( need jmxconnection )
                if (wsServer != null && !wsServer.isLocalSetup()) {
                    JMXConnection jmx = null;
                    String serverName = wsServer.getServerName();
                    IPath logfile = null;
                    try {
                        logFilePath = getServerLogFile();
                        if (logFilePath == null) {
                            showError(Messages.downloadLogDialogTitle, Messages.downloadLogFailure);
                            return Status.OK_STATUS;
                        }
                        logfile = wsServer.getWebSphereRuntime().getRemoteUsrMetadataPath().append(Constants.SERVERS_FOLDER).append(serverName).append("logs").append(logFilePath.lastSegment());

                        jmx = wsServer.createJMXConnection();
                        IPath dir = logfile.removeLastSegments(1);
                        if (!dir.toFile().exists())
                            FileUtil.makeDir(dir);
                        if (jmx.isConnected()) {
                            jmx.downloadFile(logFilePath.toString(), logfile.toOSString());
                        }
                    } catch (Exception e) {
                        Trace.logError("Error connecting to remote server : " + wsServer.getServerName(), e);
                        showError(Messages.downloadLogDialogTitle,
                                  NLS.bind(Messages.downloadLogError, e.getMessage()));
                        return Status.OK_STATUS;
                    } finally {
                        if (jmx != null)
                            jmx.disconnect();
                    }
                    if (new File(logfile.toOSString()).exists() && new File(logfile.toOSString()).length() != 0) {
                        openFile(logfile);
                    } else {
                        showError(Messages.downloadLogDialogTitle,
                                  NLS.bind(Messages.downloadLogDialogMessage, logFilePath.toOSString()));
                    }
                } else {
                    // option is enabled at this point . Hence null check not required
                    openFile(getServerInfoLogFile());
                }

                return Status.OK_STATUS;
            }

        };
        job.schedule();

    }

    public void showError(final String title, final String message) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openError(shell, title, message);
            }
        });
    }

    @SuppressWarnings("restriction")
    public void openFile(final IPath filePath) {
        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(filePath);
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchPage page = null;
                if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
                    IWorkbenchWindow window;
                    window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    page = window.getActivePage();
                }
                try {
                    if (page != null) {
                        IDE.openEditorOnFileStore(page, fileStore);
                    }

                } catch (PartInitException e) {
                    Trace.logError("Error Opening messages.log located at : " + filePath.toOSString(), e);
                }
            }
        });

    }

    public File[] getFileList() {
        if (wsServer != null && wsServer.isLocalSetup()) {
            IPath file = getServerInfoLogFile();
            final String fileExtension = file.getFileExtension();
            final String fileName = file.removeFileExtension().lastSegment();
            FileFilter fileFilter = new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    String currentFileName = pathname.getName();
                    if (currentFileName.length() == fileName.length() + fileExtension.length() + 1) {
                        return false;
                    }
                    return currentFileName.startsWith(fileName) && currentFileName.endsWith(fileExtension);
                }
            };

            return file.removeLastSegments(1).toFile().listFiles(fileFilter);
        }
        return null;
    }
}
