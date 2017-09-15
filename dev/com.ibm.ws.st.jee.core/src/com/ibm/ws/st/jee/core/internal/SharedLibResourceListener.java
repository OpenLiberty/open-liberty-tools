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
package com.ibm.ws.st.jee.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

public class SharedLibResourceListener implements IResourceChangeListener {
    private static SharedLibResourceListener resourceChangeListener;

    public synchronized static void start() {
        if (resourceChangeListener != null)
            return;

        resourceChangeListener = new SharedLibResourceListener();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace != null)
            workspace.addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.PRE_DELETE);
    }

    public synchronized static void stop() {
        if (resourceChangeListener == null)
            return;

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace != null)
            workspace.removeResourceChangeListener(resourceChangeListener);

        resourceChangeListener = null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
            IProject project = (IProject) event.getResource();
            if (SharedLibertyUtils.hasSharedLibSettingsFile(project, JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH) && SharedLibertyUtils.isPublished(project)) {
                SharedLibertyUtils.addSharedLibInfo(project);
            }
        }
    }
}
