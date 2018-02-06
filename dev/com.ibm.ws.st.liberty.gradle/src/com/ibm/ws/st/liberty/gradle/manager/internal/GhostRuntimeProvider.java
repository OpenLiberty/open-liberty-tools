/*
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.liberty.gradle.manager.internal;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereRuntimeLocator;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;

@SuppressWarnings("restriction")
public class GhostRuntimeProvider extends com.ibm.ws.st.core.internal.GhostRuntimeProvider {

	public GhostRuntimeProvider() {
		// Empty
	}
	
	@Override
	public UserDirectory getUserDirectory(URI uri, IProject project) {
		// Ensure it's a Gradle project
		ILibertyBuildPluginImpl buildPluginImpl = LibertyManager.getInstance().getBuildPluginImpl();
		if (!buildPluginImpl.isSupportedProject(project, new NullProgressMonitor())) {
			return null;
		}
		
		// We are interested in the server.xml in the src and build folders
		if (!(uri.getPath().contains("src") || uri.getPath().contains("build"))) {
			return null;
		}
			
		LibertyBuildPluginConfiguration config = buildPluginImpl.getLibertyBuildPluginConfiguration(project, new NullProgressMonitor());
		// Find the install dir of the runtime
		String installDir = config.getConfigValue(ConfigurationType.installDirectory);
		
        WebSphereRuntime wsRuntime = getWSRuntime(installDir);
        String userDirVal = config.getConfigValue(ConfigurationType.userDirectory);
        if (userDirVal != null) {
            IPath userPath = new Path(userDirVal);
            return new UserDirectory(wsRuntime, userPath, project);
        }
		return null;
	}

	@Override
	public WebSphereRuntime getWebSphereRuntime(IResource resource, IProject project) {
		// Ensure it's a Gradle project
		ILibertyBuildPluginImpl buildPluginImpl = LibertyManager.getInstance().getBuildPluginImpl();
		if (!buildPluginImpl.isSupportedProject(project, new NullProgressMonitor())) {
			return null;
		}
		
		// We are interested in the server.xml in the src and build folders
		String location = resource.getLocation().toString();
		if (!(location.contains("src") || location.contains("build"))) {
			return null;
		}
					
		LibertyBuildPluginConfiguration config = buildPluginImpl.getLibertyBuildPluginConfiguration(project, new NullProgressMonitor());
		// Find the install dir of the runtime
		String installDir = config.getConfigValue(ConfigurationType.installDirectory);
		return getWSRuntime(installDir);
	}
	
	
	private WebSphereRuntime getWSRuntime(String installDir) {
		
		// Check if the runtime already exists
		IRuntime[] runtimes = ServerCore.getRuntimes();		
		IPath installDirectory = new Path(installDir);
		IRuntime runtime = null;
        for (IRuntime rt : runtimes) {
            if (WebSphereUtil.isWebSphereRuntime(rt) && installDirectory.equals(rt.getLocation())) {
                runtime = rt;
            }
        }
        WebSphereRuntime wsRuntime = null;
        if (runtime != null) {
        		wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        } else {
        		// If there is no runtime created in the workspace, then provide our own ghost runtime 'instance'
        		IRuntimeWorkingCopy runtimeFromDir = WebSphereRuntimeLocator.getRuntimeFromDir(installDirectory, new NullProgressMonitor());
			wsRuntime = (WebSphereRuntime)runtimeFromDir.loadAdapter(WebSphereRuntime.class, null);
		}
        return wsRuntime;
	}

}
