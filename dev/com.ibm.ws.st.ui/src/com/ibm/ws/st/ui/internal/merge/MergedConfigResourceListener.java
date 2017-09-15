/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.merge;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Listens for files and folders being removed and cleans up associated
 * merged files.
 */
public class MergedConfigResourceListener implements IResourceChangeListener {

    private static MergedConfigResourceListener listener;

    public synchronized static void start() {
        if (listener != null)
            return;
        listener = new MergedConfigResourceListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
    }

    public synchronized static void stop() {
        if (listener == null)
            return;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace != null)
            workspace.removeResourceChangeListener(listener);
        listener = null;
    }

    /** {@inheritDoc} */
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null)
            return;

        try {
            delta.accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta visitorDelta) {
                    switch (visitorDelta.getKind()) {
                        case IResourceDelta.CHANGED:
                            // Keep going to see if anything removed
                            return true;
                        case IResourceDelta.REMOVED:
                            IResource resource = visitorDelta.getResource();
                            IPath location = ConfigUtils.getMergedConfigLocation(resource);
                            if (location != null && location.toFile().exists()) {
                                File file = location.toFile();
                                if (file.isDirectory()) {
                                    try {
                                        FileUtil.deleteDirectory(file.getCanonicalPath(), true);
                                    } catch (IOException e) {
                                        if (Trace.ENABLED)
                                            Trace.trace(Trace.WARNING, "Failed to delete merged configuration container: " + location, e);
                                    }
                                } else {
                                    if (!file.delete()) {
                                        if (Trace.ENABLED)
                                            Trace.trace(Trace.WARNING, "Failed to delete merged configuration file: " + location);
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    return false;
                }
            });
        } catch (CoreException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error in merged config resource listener.", e);
        }
    }

}
