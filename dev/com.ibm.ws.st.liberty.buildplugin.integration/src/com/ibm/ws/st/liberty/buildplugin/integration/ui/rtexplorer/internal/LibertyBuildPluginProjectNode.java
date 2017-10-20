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

package com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal;

import org.eclipse.core.resources.IProject;

public class LibertyBuildPluginProjectNode {

    private final IProject project;
    private final String text;
    private final String installDir;
    private final String description;

    public LibertyBuildPluginProjectNode(IProject project, String text, String installDir, String description) {
        this.project = project;
        this.text = text;
        this.installDir = installDir;
        this.description = description;
    }

    /**
     * @return the project
     */
    public IProject getProject() {
        return project;
    }

    public String getText() {
        return text;
    }

    public String getInstallDir() {
        return installDir;
    }

    public String getDescription() {
        return description;
    }

}
