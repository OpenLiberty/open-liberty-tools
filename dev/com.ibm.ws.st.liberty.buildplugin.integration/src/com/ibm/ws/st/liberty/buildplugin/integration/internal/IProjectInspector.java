/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

public interface IProjectInspector {

    public LibertyBuildPluginConfiguration getBuildPluginConfiguration(IProgressMonitor monitor);

    public LibertyBuildPluginConfiguration getCachedBuildPluginConfiguration(IProgressMonitor monitor);

    public File getCachedLibertyBuildPluginConfigurationFile(IProgressMonitor monitor);

    public boolean isSupportedProject(IProgressMonitor monitor);

    public File getLibertyBuildPluginConfigFile(IProgressMonitor mon) throws CoreException;

    LibertyBuildPluginConfiguration populateConfiguration(File configFile, IProgressMonitor monitor) throws IOException;

    public boolean isSupportedModule(IModule module);

    public IModule[] getProjectModules();

}
