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

package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;

/**
 * Defines the minimal set of methods required for a Liberty build plugin to integrate with the development environment.
 */
public interface ILibertyBuildPluginImpl {

    /**
     * Provides the object that is used to define and handle the mapping between projects and managed servers.
     *
     * @return
     */
    AbstractLibertyProjectMapping getMappingHandler();

    /**
     * Returns a project inspector that can be used to retrieve information about a project in the context of the build plugin.
     *
     * @param project the project to inspect
     * @return
     */
    IProjectInspector getProjectInspector(IProject project);

    /**
     * A shorthand to determine whether this project contains the necessary configuration for liberty build plugin integration.
     *
     * @param project
     * @param monitor
     * @return
     */
    boolean isSupportedProject(IProject project, IProgressMonitor monitor);

    /**
     * Updates the source configuration using a build plugin goal or task execution.
     *
     * @param projectLocation <code>IPath</code> of the project's location
     * @param config The build plugin configuration
     * @param monitor
     */
    void updateSrcConfig(IPath projectLocation, LibertyBuildPluginConfiguration config, IProgressMonitor monitor);

    /**
     * Get the liberty build plugin configuration for the given project.
     *
     * @param project
     * @param monitor
     * @return
     */
    LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project, IProgressMonitor monitor);

    /**
     * Returns the location of the build plugin's configuration file.
     *
     * @return the location as a <code>String</code>
     */
    String getRelativeBuildPluginConfigurationFileLocation();

    /**
     * Execute any tasks that are prerequisite to server setup.
     *
     * @param project
     * @param config
     * @param serverPath
     * @param monitor
     * @return
     */
    IStatus preServerSetup(IProject project, LibertyBuildPluginConfiguration config, IPath serverPath, IProgressMonitor monitor);

    /**
     * The identifier used for the server.
     *
     * @return
     */
    String getServerType();

    /**
     * Validates whether the project is a dependency module that should be deployed to the server.
     *
     * @param moduleProject the project to check
     * @param server the server that it could be deployed to
     * @return
     */
    public boolean isDependencyModule(IProject moduleProject, IServer server);

    /**
     * Trigger runtime and server creation.
     *
     * @param project the project that defines the runtime and server
     * @param monitor
     */
    void triggerAddRuntimeAndServer(IProject project, IProgressMonitor monitor);
}
