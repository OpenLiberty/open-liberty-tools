/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

public class NewExtendedConfigAction extends SelectionProviderAction {
    private final Shell shell;
    private final String fileName;
    private final StructuredViewer viewer;
    private Object selectedObj;
    private WebSphereServerInfo server;
    private final String in;

    public NewExtendedConfigAction(String fileName, ISelectionProvider selectionProvider, StructuredViewer viewer, String in) {
        super(selectionProvider, fileName);
        this.shell = viewer.getControl().getShell();
        this.fileName = fileName;
        this.viewer = viewer;
        this.in = in;
        setImageDescriptor(Activator.getImageDescriptor(fileName));
        selectionChanged(getStructuredSelection());

        if (in != null) {
            this.setText(NLS.bind(Messages.menuJVMOptionsIn, in));
        }
    }

    public NewExtendedConfigAction(String fileName, ISelectionProvider selectionProvider, StructuredViewer viewer) {
        this(fileName, selectionProvider, viewer, null);
        this.setText(fileName);
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }

        server = null;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            selectedObj = iterator.next();
            if (selectedObj instanceof IServer) {
                IServer srv = (IServer) selectedObj;
                WebSphereServer wsServer = (WebSphereServer) srv.loadAdapter(WebSphereServer.class, null);
                if (wsServer != null) {
                    server = wsServer.getServerInfo();
                }
            } else if (selectedObj instanceof WebSphereServerInfo) {
                server = (WebSphereServerInfo) selectedObj;
            } else {
                setEnabled(false);
                return;
            }
        }
        if (server == null) {
            setEnabled(false);
            return;
        }

        // Disable if it is a remote server
        IPath remoteUserDir = server.getUserDirectory().getRemoteUserPath();
        if (remoteUserDir != null && in != null && in.contains(Constants.SHARED_FOLDER)) {
            setEnabled(false);
            return;
        }

        IFolder folder = getFolder();
        if (folder != null && folder.isAccessible()) {
            setEnabled(!folder.getFile(fileName).exists());
        } else {
            IPath path = getPath().append(fileName);
            setEnabled(!path.toFile().exists());
        }
    }

    public boolean isApplicable() {
        return server != null;
    }

    // Returns the path corresponding to the jvm.options specified by fileName and in
    private IPath getPath() {
        IPath path;
        // By default use the server path
        if (in == null)
            path = server.getServerPath();
        else if (in.contains(Constants.SHARED_FOLDER))
            path = server.getUserDirectory().getSharedPath();
        else if (in.contains(Constants.CONFIG_DEFAULT_DROPINS_FOLDER))
            path = server.getConfigDefaultDropinsPath();
        else if (in.contains(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER))
            path = server.getConfigOverrideDropinsPath();
        else
            path = server.getServerPath();

        return path;
    }

    // Returns the folder corresponding to the jvm.options specified by fileName and in
    private IFolder getFolder() {
        IFolder folder;
        // By default use the server folder
        if (in == null)
            folder = server.getServerFolder();
        else if (in.contains(Constants.SHARED_FOLDER))
            folder = server.getUserDirectory().getSharedFolder();
        else if (in.contains(Constants.CONFIG_DEFAULT_DROPINS_FOLDER))
            folder = server.getConfigDefaultDropinsFolder();
        else if (in.contains(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER))
            folder = server.getConfigOverrideDropinsFolder();
        else
            folder = server.getServerFolder();

        return folder;
    }

    @Override
    public void run() {
        if (server == null)
            return;
        try {
            IFile file = null;
            URI uri = null;
            IFolder folder = getFolder();
            if (folder != null) {
                IProject project = folder.getProject();
                if (!project.isOpen() && !MessageDialog.openQuestion(shell, Messages.title, NLS.bind(Messages.confirmProjectOpen, project.getName()))) {
                    IPath path = getPath();
                    if (FileUtil.makeDir(path)) {
                        File f = WebSphereUtil.createFile(path.toFile(), fileName, null);
                        uri = f.toURI();
                        server.updateCache();
                    }
                } else {
                    createFolder(folder);
                    file = WebSphereUtil.createFile(folder, fileName, null);
                    uri = file.getLocationURI();
                    server.updateCache();
                }
            } else {
                IPath path = getPath();
                if (FileUtil.makeDir(path)) {
                    File f = WebSphereUtil.createFile(path.toFile(), fileName, null);
                    uri = f.toURI();
                    server.updateCache();
                }
            }

            // disable this action in case the user tries to do it again on the same server
            if (uri != null && (new File(uri)).exists()) {
                Activator.openEditor(file, uri);
                viewer.refresh(selectedObj);
                setEnabled(false);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            setEnabled(true);
            Trace.logError("Error creating " + this.getText(), e);
        }
    }

    private void createFolder(IFolder folder) throws CoreException {
        if (!folder.exists()) {
            if (in.contains(Constants.CONFIG_DROPINS_FOLDER)) {
                IFolder dropinsFolder = server.getServerFolder().getFolder(Constants.CONFIG_DROPINS_FOLDER);
                if (!dropinsFolder.exists()) {
                    dropinsFolder.create(true, true, new NullProgressMonitor());
                }
                dropinsFolder = dropinsFolder.getFolder(folder.getName());
                if (!dropinsFolder.exists()) {
                    dropinsFolder.create(true, true, new NullProgressMonitor());
                }
            } else {
                folder.create(true, true, new NullProgressMonitor());
            }
        }
    }
}
