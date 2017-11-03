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

package com.ibm.etools.maven.liberty.integration.ui.rtexplorer.internal;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.AbstractLibertyBuildPluginRuntimeContentProvider;

public class MavenRuntimeContentProvider extends AbstractLibertyBuildPluginRuntimeContentProvider {

    @Override
    public String getRuntimeContentId() {
        return LibertyMavenConstants.LIBERTY_MAVEN_RUNTIME_CONTENT_ID;
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

}
