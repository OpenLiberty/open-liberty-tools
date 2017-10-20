/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.etools.maven.liberty.integration.servertype.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.core.internal.ServerExtension;

@SuppressWarnings("restriction")
/**
 * Liberty Maven server extension implementation
 *
 * Note: currently not being used by anything, but required for the extension
 */
public class LibertyMavenServerExtension extends ServerExtension {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.st.core.internal.ServerExtension#getChildModules(org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IModule[] getChildModules(IModule[] module) {
        // Not currently used, so return the default null
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.st.core.internal.ServerExtension#getRootModules(org.eclipse.wst.server.core.IModule)
     */
    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        // Not currently used, so return the default null
        return null;
    }

}