/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.wizard.NewConfigDropinWizard;

public class NewConfigDropinAction extends SelectionProviderAction {
    private final Shell shell;
    private final DropinType type;
    private final StructuredViewer viewer;
    private WebSphereServerInfo server;

    public enum DropinType {
        DEFAULTS(Messages.newConfigDropinDefaults, Constants.CONFIG_DEFAULT_DROPINS_FOLDER, Messages.newConfigDropinWizardDefaultsTitle, Messages.newConfigDropinWizardDefaultsDesc),
        OVERRIDES(Messages.newConfigDropinOverrides, Constants.CONFIG_OVERRIDE_DROPINS_FOLDER, Messages.newConfigDropinWizardOverridesTitle, Messages.newConfigDropinWizardOverridesDesc);

        private final String label;
        private final String folder;
        private final String title;
        private final String desc;

        private DropinType(String label, String folder, String title, String desc) {
            this.label = label;
            this.folder = folder;
            this.title = title;
            this.desc = desc;
        }

        public String getLabel() {
            return label;
        }

        public String getFolder() {
            return folder;
        }

        public String getTitle() {
            return title;
        }

        public String getDesc() {
            return desc;
        }

    }

    public NewConfigDropinAction(DropinType type, ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, type.getLabel());
        this.shell = viewer.getControl().getShell();
        this.type = type;
        this.viewer = viewer;
        setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_CONFIG_FILE));
        selectionChanged(getStructuredSelection());
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
            Object obj = iterator.next();
            if (obj instanceof IServer) {
                IServer srv = (IServer) obj;
                WebSphereServer wsServer = (WebSphereServer) srv.loadAdapter(WebSphereServer.class, null);
                if (wsServer != null)
                    server = wsServer.getServerInfo();
            } else if (obj instanceof WebSphereServerInfo) {
                server = (WebSphereServerInfo) obj;
            } else {
                setEnabled(false);
                return;
            }
        }
        if (server == null) {
            setEnabled(false);
            return;
        }

        setEnabled(isApplicable());
    }

    public boolean isApplicable() {
        if (server == null)
            return false;
        WebSphereRuntime runtime = server.getWebSphereRuntime();
        if (runtime != null && WebSphereUtil.isGreaterOrEqualVersion("8.5.5.6", runtime.getRuntimeVersion()))
            return true;
        return false;
    }

    @Override
    public void run() {
        if (server == null)
            return;

        String fileName = null;

        try {
            IPath serverPath = server.getServerPath();
            IPath path = serverPath.append(Constants.CONFIG_DROPINS_FOLDER).append(type.getFolder());
            NewConfigDropinWizard wizard = new NewConfigDropinWizard(path, type);
            WizardDialog dialog = new WizardDialog(shell, wizard);
            dialog.setPageSize(200, 100);
            if (dialog.open() == Window.CANCEL)
                return;

            fileName = wizard.getFileName();

            // Create the file
            IFile file = null;
            URI uri = null;
            IFolder serverFolder = server.getServerFolder();
            if (serverFolder != null) {
                IProject project = serverFolder.getProject();
                if (!project.isOpen() && !MessageDialog.openQuestion(shell, Messages.title, NLS.bind(Messages.confirmProjectOpen, project.getName()))) {
                    FileUtil.makeDir(path);
                    File f = WebSphereUtil.createFile(path.toFile(), fileName, null);
                    uri = f.toURI();
                    server.updateCache();
                } else {
                    IFolder dropinsFolder = serverFolder.getFolder(Constants.CONFIG_DROPINS_FOLDER);
                    if (!dropinsFolder.exists()) {
                        dropinsFolder.create(true, true, new NullProgressMonitor());
                    }
                    dropinsFolder = dropinsFolder.getFolder(type.getFolder());
                    if (!dropinsFolder.exists()) {
                        dropinsFolder.create(true, true, new NullProgressMonitor());
                    }
                    file = WebSphereUtil.createFile(dropinsFolder, fileName, null);
                    uri = file.getLocationURI();
                }
            } else {
                FileUtil.makeDir(path);
                File f = WebSphereUtil.createFile(path.toFile(), fileName, null);
                uri = f.toURI();
                server.updateCache();
            }

            // Add the server element to the file
            if (file != null) {
                InputStream initialContent = new ByteArrayInputStream(Constants.INITIAL_CONFIG_CONTENT.getBytes());
                file.appendContents(initialContent, true, false, new NullProgressMonitor());
            } else {
                File f = new File(uri);
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(f);
                    outputStream.write(Constants.INITIAL_CONFIG_CONTENT.getBytes());
                } finally {
                    try {
                        if (outputStream != null)
                            outputStream.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }

            Activator.openConfigurationEditor(file, uri);

            viewer.refresh(server);
        } catch (Exception e) {
            Trace.logError("Error creating new config dropins file: " + type.getFolder() + (fileName == null ? "" : "/" + fileName), e);
        }
    }
}
