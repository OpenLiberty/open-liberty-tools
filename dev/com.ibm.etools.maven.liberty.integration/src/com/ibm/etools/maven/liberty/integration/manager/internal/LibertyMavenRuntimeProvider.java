/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.etools.maven.liberty.integration.manager.internal;

import org.eclipse.wst.server.core.IServer;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.AbstractLibertyRuntimeProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;

public class LibertyMavenRuntimeProvider extends AbstractLibertyRuntimeProvider {

    public LibertyMavenRuntimeProvider() {
        // Empty
    }

    // Maven specific

    /** {@inheritDoc} */
    @Override
    protected ILibertyBuildPluginImpl getBuildPlugin() {
        return LibertyManager.getInstance().getBuildPluginImpl();
    }

    /** {@inheritDoc} */
    @Override
    protected IServer getMappedServer(String projectName) {
        return LibertyMaven.getMappedServer(projectName);
    }

}
