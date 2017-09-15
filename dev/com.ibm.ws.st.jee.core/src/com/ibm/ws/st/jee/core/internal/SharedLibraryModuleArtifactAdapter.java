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
package com.ibm.ws.st.jee.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;

/**
 *
 */
public class SharedLibraryModuleArtifactAdapter extends ModuleArtifactAdapterDelegate {

    /** {@inheritDoc} */
    @Override
    public IModuleArtifact getModuleArtifact(Object obj) {
        if (obj instanceof IProject) {
            IModule[] modules = ServerUtil.getModules((IProject) obj);
            for (IModule module : modules) {
                if (SharedLibertyUtils.isSharedLibrary(module))
                    return new SharedLibraryArtifact(module);
            }
        }
        return null;
    }

}
