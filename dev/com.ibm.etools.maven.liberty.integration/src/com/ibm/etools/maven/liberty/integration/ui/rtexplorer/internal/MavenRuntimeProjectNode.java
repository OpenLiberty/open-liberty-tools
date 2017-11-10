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

import org.eclipse.core.resources.IProject;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.LibertyBuildPluginProjectNode;

/**
 *
 */
public class MavenRuntimeProjectNode extends LibertyBuildPluginProjectNode {

    /**
     * @param project
     * @param text
     * @param installDir
     * @param description
     */
    public MavenRuntimeProjectNode(IProject project, String text, String installDir, String description) {
        super(project, text, installDir, description);
    }

    /** {@inheritDoc} */
    @Override
    public String getRuntimeContentId() {
        return LibertyMavenConstants.LIBERTY_MAVEN_RUNTIME_CONTENT_ID;
    }

}
