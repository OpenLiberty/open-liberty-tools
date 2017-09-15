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
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

public class WebSphereSourcePathComputerDelegate implements ISourcePathComputerDelegate {
    @Override
    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
        IServer server = ServerUtil.getServer(configuration);

        List<IProject> projects = new ArrayList<IProject>();
        List<IModule> modules = new ArrayList<IModule>();
        IModule[] mods = server.getModules();
        for (IModule m : mods)
            modules.add(m);

        while (!modules.isEmpty()) {
            IModule module = modules.remove(0);
            IModule[] children = server.getChildModules(new IModule[] { module }, monitor);
            for (IModule m : children)
                modules.add(m);
            IProject project = module.getProject();
            if (project != null && !projects.contains(project) && project.hasNature(JavaCore.NATURE_ID))
                projects.add(project);
        }

        List<IRuntimeClasspathEntry> runtimeClasspath = new ArrayList<IRuntimeClasspathEntry>();
        for (IProject project : projects) {
            IJavaProject javaProject = JavaCore.create(project);
            runtimeClasspath.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
        }

        runtimeClasspath.addAll(Arrays.asList(
                        JavaRuntime.computeUnresolvedSourceLookupPath(configuration)));
        IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(runtimeClasspath
                        .toArray(new IRuntimeClasspathEntry[runtimeClasspath.size()]), configuration);
        ArrayList<ISourceContainer> containersList = new ArrayList<ISourceContainer>();

        containersList.addAll(Arrays.asList(JavaRuntime.getSourceContainers(resolved)));

        ISourcePathComputerDelegate[] spcs = null;
        try {
            spcs = Activator.getInstance().getSourcePathComputers();
            for (ISourcePathComputerDelegate delegate : spcs) {
                ISourceContainer[] extendedContainers = delegate.computeSourceContainers(configuration, monitor);
                containersList.addAll(Arrays.asList(extendedContainers));
            }
        } catch (Exception e) {
            Trace.trace(Trace.INFO, "No source path extensions were found.");
        }
        ISourceContainer[] containers = new ISourceContainer[containersList.size()];
        containersList.toArray(containers);
        return containers;
    }
}