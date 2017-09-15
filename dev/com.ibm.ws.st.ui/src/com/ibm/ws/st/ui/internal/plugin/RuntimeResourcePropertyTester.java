/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.plugin;

import java.util.Set;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Test that a resource belongs to a Liberty Profile runtime instance.
 */
public class RuntimeResourcePropertyTester extends PropertyTester {
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if ("runtimeResource".equals(property)) {
            IResource resource = (IResource) Platform.getAdapterManager().getAdapter(receiver, IResource.class);
            if (resource != null) {
                IProject project = resource.getProject();
                if (project == null)
                    return false;

                for (WebSphereRuntime runtime : WebSphereUtil.getWebSphereRuntimes()) {
                    for (UserDirectory userDir : runtime.getUserDirectories()) {
                        IProject userDirProject = userDir.getProject();
                        if (project == userDirProject)
                            return true;
                    }
                }
            }
            return false;
        } else if ("runtimeTarget".equals(property)) {
            IResource resource = (IResource) Platform.getAdapterManager().getAdapter(receiver, IResource.class);
            if (resource != null) {
                IProject project = resource.getProject();
                if (project == null)
                    return false;

                try {
                    IServer[] servers = ServerCore.getServers();
                    for (IServer server : servers) {
                        if (WebSphereUtil.isWebSphereRuntime(server.getRuntime())) {
                            IModule[] modules = server.getModules();
                            for (IModule m : modules)
                                if (project.equals(m.getProject()))
                                    return true;
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error checking server-project association", e);

                    // return true so that we show too often instead of too little
                    return true;
                }

                try {
                    IFacetedProject fp = ProjectFacetsManager.create(project);
                    if (fp == null)
                        return false;
                    Set<org.eclipse.wst.common.project.facet.core.runtime.IRuntime> runtimes = fp.getTargetedRuntimes();
                    for (org.eclipse.wst.common.project.facet.core.runtime.IRuntime rt : runtimes) {
                        IRuntime runtime = FacetUtil.getRuntime(rt);
                        if (WebSphereUtil.isWebSphereRuntime(runtime))
                            return true;
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error checking project runtime target", e);

                    // return true so that we show too often instead of too little
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
