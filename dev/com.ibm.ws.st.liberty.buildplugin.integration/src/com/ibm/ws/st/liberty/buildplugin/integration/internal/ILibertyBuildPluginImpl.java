/*
* IBM Confidential
*
* OCO Source Materials
*
* (C) Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;

public interface ILibertyBuildPluginImpl {

    AbstractLibertyProjectMapping getMappingHandler();

    IProjectInspector getProjectInspector(IProject project);

    boolean isSupportedProject(IProject project, IProgressMonitor monitor);

    /**
     * Updates the source configuration using a build plugin goal or task execution.
     *
     * @param projectLocation <code>IPath</code> of the project's location
     * @param config The build plugin configuration
     * @param monitor
     */
    void updateSrcConfig(IPath projectLocation, LibertyBuildPluginConfiguration config, IProgressMonitor monitor);

    LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project, IProgressMonitor monitor);

    /**
     * Returns the location of the build plugin's configuration file.
     *
     * @return the location as a <code>String</code>
     */
    String getRelativeBuildPluginConfigurationFileLocation();

    IStatus preServerSetup(IProject project, LibertyBuildPluginConfiguration config, IPath serverPath, IProgressMonitor monitor);

    String getServerType();

    public boolean isDependencyModule(IProject moduleProject, IServer server);

    void triggerAddProject(IProject project, IProgressMonitor monitor);
}
