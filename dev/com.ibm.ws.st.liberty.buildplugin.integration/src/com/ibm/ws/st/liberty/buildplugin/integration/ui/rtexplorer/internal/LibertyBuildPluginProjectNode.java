/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
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
