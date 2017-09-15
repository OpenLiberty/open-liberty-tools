/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonViewer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/**
 * Provides content for configuration files in the project explorer (and other explorer)
 * views.
 */
public class ConfigurationResourceContentProvider extends DDETreeContentProvider {

    private IResourceChangeListener listener;

    /** {@inheritDoc} */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (!(viewer instanceof CommonViewer))
            return;

        commonViewer = (CommonViewer) viewer;

        if (listener == null) {
            listener = new IResourceChangeListener() {
                /** {@inheritDoc} */
                @Override
                public void resourceChanged(IResourceChangeEvent event) {
                    IResourceDelta delta = event.getDelta();
                    if (delta == null)
                        return;

                    // ignore clean builds
                    if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD)
                        return;

                    try {
                        // collect a list of changed config files
                        final List<IFile> files = new ArrayList<IFile>(10);

                        delta.accept(new IResourceDeltaVisitor() {
                            @Override
                            public boolean visit(IResourceDelta visitorDelta) {
                                IResource resource = visitorDelta.getResource();
                                if (visitorDelta.getKind() == IResourceDelta.CHANGED && resource != null && resource instanceof IFile) {
                                    IFile file = (IFile) resource;
                                    IContentType contentType = IDE.getContentType(file);
                                    if (contentType != null && "com.ibm.ws.st.configuration".equals(contentType.getId())) {
                                        if ((visitorDelta.getFlags() & IResourceDelta.CONTENT) == 0 &&
                                                (visitorDelta.getFlags() & IResourceDelta.REPLACED) == 0)
                                            return false;
                                        files.add(file);
                                    }
                                }
                                return true;
                            }
                        });

                        if (!files.isEmpty()) {
                            refreshFiles(files);
                        }
                    } catch (CoreException ce) {
                        Trace.trace(Trace.WARNING, "Error in resource listener", ce);
                    }
                }
            };
            ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
        }
    }

    protected void refreshFiles(final List<IFile> files) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (commonViewer != null) {
                    for (IFile file : files) {
                        commonViewer.refresh(file);
                    }
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IFile) {
            IFile file = (IFile) parentElement;
            URI uri = file.getLocation().toFile().toURI();
            Document doc = ConfigUtils.getDOM(uri);
            if (doc != null) {
                if (doc.getUserData(ConfigurationFile.USER_DATA_URI) == null) {
                    doc.setUserData(ConfigurationFile.USER_DATA_URI, uri.toString(), null);
                    doc.setUserData(ConfigurationFile.USER_DATA_USER_DIRECTORY, file.getProject().getLocation().toFile().toURI().toString(), null);
                }
                Node node = doc.getDocumentElement();
                return getChildren(node);
            }
        }

        if (parentElement instanceof Element) {
            if (Constants.INCLUDE_ELEMENT.equals(((Element) parentElement).getNodeName())) {
                return NO_CHILDREN;
            }
        }

        return super.getChildren(parentElement);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IFile)
            return true;
        return super.hasChildren(element);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        commonViewer = null;
        if (listener != null)
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
    }

}
