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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

public interface IProjectInspector {

    /**
     * Get the liberty build plugin configuration for the given project.
     *
     * @param project
     * @return
     */
    public LibertyBuildPluginConfiguration getBuildPluginConfiguration(IProgressMonitor monitor);

    /**
     * Get the liberty build plugin configuration file for the given project.
     *
     * @param monitor
     * @return
     * @throws CoreException
     */
    public File getLibertyBuildPluginConfigFile(IProgressMonitor monitor) throws CoreException;

    /**
     * Get the cached version of the liberty build plugin configuration for the given project.
     *
     * @param project
     * @return an instance of {@code LibertyBuildPluginConfiguration} or {@code null} if it hasn't been cached
     */
    public LibertyBuildPluginConfiguration getCachedBuildPluginConfiguration(IProgressMonitor monitor);

    /**
     * Get the cache file of the liberty build plugin configuration for the given project.
     *
     * @param monitor
     * @return
     */
    public File getCachedLibertyBuildPluginConfigurationFile(IProgressMonitor monitor);

    /**
     * Determines whether this project contains the necessary configuration for liberty build plugin integration with the development environment.
     *
     * @param monitor
     * @return
     */
    public boolean isSupportedProject(IProgressMonitor monitor);

    /**
     * Parses the given file and stores the values in an {@code LibertyBuildPluginConfiguration} object for easy consumption.
     *
     * @param configFile the file containing the Liberty build plugin configuration
     * @param monitor
     * @return
     * @throws IOException
     */
    LibertyBuildPluginConfiguration populateConfiguration(File configFile, IProgressMonitor monitor) throws IOException;

    /**
     * Determine whether the given module type is supported by the build plugin integration.
     *
     * @param module
     * @return
     */
    public boolean isSupportedModule(IModule module);

    /**
     * Get all the modules that are associated with this project.
     *
     * @return
     */
    public IModule[] getProjectModules();

    /**
     * Gets the liberty-maven-plugin version.
     *
     * @param monitor
     * @return the version of the plugin
     * @throws CoreException
     */
    public String getLibertyPluginVersion(IProgressMonitor monitor) throws CoreException;

    /**
     * Determine whether the legacy liberty maven plugin goal for installing the app should be used.
     *
     * @param project
     * @param monitor
     * @return
     */
    public boolean useLegacyMvnGoal(IProgressMonitor monitor);

}
