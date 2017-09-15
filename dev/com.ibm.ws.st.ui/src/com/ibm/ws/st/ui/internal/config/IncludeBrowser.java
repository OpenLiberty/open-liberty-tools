/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.URIUtil;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;

public class IncludeBrowser implements IAdvancedCustomizationObject {
    @Override
    public String invoke(String value, Node node, Element closestAncestor, IEditorPart editorPart) {
        Shell shell = editorPart.getSite().getShell();
        IEditorInput input = editorPart.getEditorInput();
        IncludeDialog dialog = new IncludeDialog(shell, closestAncestor.getOwnerDocument(), input);
        if (dialog.open() == Window.OK) {
            String path = dialog.getAbsolutePath();
            File file = new File(path);
            if (!file.exists()) {
                if (!MessageDialog.openConfirm(shell, Messages.title, Messages.includeConfirmCreate))
                    return null;
                FileOutputStream stream = null;
                try {
                    // If the file path is inside the current project then create
                    // it as an IFile in that project.
                    IPath filePath = new Path(path);
                    IFile ifile = ConfigUIUtils.getFile(input);
                    if (ifile != null && ifile.getProject() != null) {
                        IProject project = ifile.getProject();
                        URI workspaceURI = project.getLocation().toFile().toURI();
                        URI includeURI = filePath.toFile().toURI();
                        URI relativeURI = URIUtil.canonicalRelativize(workspaceURI, includeURI);
                        if (!relativeURI.isAbsolute()) {
                            IFile newFile = project.getFile(new Path(relativeURI.getPath()));
                            createFile(newFile);
                            return dialog.getFullPath();
                        }
                    }

                    // Create the file out in the file system.
                    IPath dirPath = filePath.removeLastSegments(1);
                    File dir = new File(dirPath.toOSString());
                    if (!dir.exists() && !dir.mkdirs()) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to create directories for include file: " + path, null);
                        MessageDialog.openError(shell, Messages.includeCreateFailedTitle, NLS.bind(Messages.includeCreateDirsFailedMsg, path));
                        return null;
                    }
                    if (!file.createNewFile()) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to create include file: " + path, null);
                        MessageDialog.openError(shell, Messages.includeCreateFailedTitle, NLS.bind(Messages.includeCreateFileFailedMsg, path));
                        return null;
                    }
                    stream = new FileOutputStream(file);
                    stream.write(Constants.INITIAL_CONFIG_CONTENT.getBytes());
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to create include file: " + path, e);
                    MessageDialog.openError(shell, Messages.includeCreateFailedTitle, e.getLocalizedMessage());
                    return null;
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
            return dialog.getFullPath();
        }
        return null;
    }

    private void createFile(IFile file) throws Exception {
        if (!file.exists()) {
            ByteArrayInputStream stream = new ByteArrayInputStream(Constants.INITIAL_CONFIG_CONTENT.getBytes());
            createContainer(file.getParent());
            file.create(stream, IResource.NONE, null);
        }
    }

    private void createContainer(IContainer container) throws CoreException {
        if (container != null && !container.exists() && container instanceof IFolder) {
            IFolder folder = (IFolder) container;
            createContainer(folder.getParent());
            folder.create(IResource.NONE, true, null);
        }
    }
}
