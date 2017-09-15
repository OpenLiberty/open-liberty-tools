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

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;

/**
 *
 */
public class SharedLibraryArtifact implements IModuleArtifact {
    private final IModule module;

    public SharedLibraryArtifact(IModule module) {
        this.module = module;

    }

    /** {@inheritDoc} */
    @Override
    public IModule getModule() {
        return module;
    }

}
