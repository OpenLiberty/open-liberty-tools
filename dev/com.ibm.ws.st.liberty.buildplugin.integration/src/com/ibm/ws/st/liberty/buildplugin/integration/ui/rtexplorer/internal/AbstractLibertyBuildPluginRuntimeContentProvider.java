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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;

@SuppressWarnings("restriction")
public abstract class AbstractLibertyBuildPluginRuntimeContentProvider implements org.eclipse.jface.viewers.ITreeContentProvider, ILibertyBuildPluginImplProvider {

    /** {@inheritDoc} */
    @Override
    public Object[] getChildren(Object arg0) {
        return new Object[0];
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getElements(Object arg0) {
        Set<LibertyBuildPluginProjectNode> nodes = getAllBuildPluginProjectNodes();
        return nodes.toArray();
    }

    public Set<LibertyBuildPluginProjectNode> getAllBuildPluginProjectNodes() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        Set<IProject> allBuildPluginProjects = new HashSet<IProject>(projects.length);
        for (IProject project : projects) {
            if (project.isOpen() && getBuildPluginImpl().isSupportedProject(project, new NullProgressMonitor())) {
                allBuildPluginProjects.add(project);
            }
        }
        Set<LibertyBuildPluginProjectNode> nodes = new HashSet<LibertyBuildPluginProjectNode>(allBuildPluginProjects.size());

        Set<String> trackedProjectNames = getBuildPluginImpl().getMappingHandler().getMappedProjectSet();
        for (String p : trackedProjectNames) {
            IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(p);
            if (proj != null) {
                allBuildPluginProjects.remove(proj);
            } else {
                Trace.logError("Project " + p + " is tracked but was not found in the workspace", null);
            }
        }

        for (IProject proj : allBuildPluginProjects) {
            LibertyBuildPluginConfiguration config = getBuildPluginImpl().getLibertyBuildPluginConfiguration(proj, new NullProgressMonitor());
            String installDir = null;
            String description = "";
            String text = proj.getName();

            if (config != null) {
                installDir = config.getConfigValue(ConfigurationType.installDirectory);
                description = NLS.bind(Messages.createServerActionDescription, proj.getName(), installDir);
            }

            text = WebSphereUtil.getUniqueRuntimeName(NLS.bind(Messages.runtimeLabel, proj.getName()), ServerCore.getRuntimes());

            LibertyBuildPluginProjectNode node = new LibertyBuildPluginProjectNode(proj, text, installDir, description);
            nodes.add(node);
        }
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    public Object getParent(Object arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChildren(Object arg0) {
        return false;
    }

}
