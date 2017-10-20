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

package com.ibm.ws.st.liberty.buildplugin.integration.manager.internal;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.AbstractProjectMapXML;

/**
 * Project Mapping rules:
 *
 * P - project
 * C - liberty-plugin-config.xml
 * R - runtime
 * S - server
 *
 * y - files exist
 * n - files don't exist
 * m - mapped
 * u - unmapped
 *
 * ----------------------------------
 * | trackedLibertyProjects |
 * ----------------------------------
 * P/C/R/S | Project | Runtime | Server |
 * ----------------------------------------------------------------------------------------------------------------------
 * n/n/n/n | u | u | u | Nothing mapped when project doesn't exist
 * n/n/n/y | u | u | u | Nothing mapped when project doesn't exist
 * n/n/y/n | u | u | u | Nothing mapped when project doesn't exist
 * n/n/y/y | u | u | u | Nothing mapped when project doesn't exist
 * n/y/n/n | u | u | u | Nothing mapped when project doesn't exist
 * n/y/n/y | u | u | u | Nothing mapped when project doesn't exist
 * n/y/y/n | u | u | u | Nothing mapped when project doesn't exist
 * n/y/y/y | u | u | u | Nothing mapped when project doesn't exist
 * y/n/n/n | m/u | u | u | y if it was mapped before, n otherwise
 * y/n/n/y | u | u | u | Invalid state, should never have anything mapped without a config file
 * y/n/y/n | u | u | u | Invalid state, should never have anything mapped without a config file
 * y/n/y/y | u | u | u | Invalid state, should never have anything mapped without a config file
 * y/y/n/n | m | u | u | Configured runtime and server instances already exist in the workbench
 * y/y/n/y | m | u | m | Configured server instance already exists in the workbench
 * y/y/y/n | m | m | u | Configured runtime instance already exists in the workbench
 * y/y/y/y | m | m | m | Runtime and server mapped to project
 */
@SuppressWarnings("restriction")
public abstract class AbstractLibertyProjectMapping {

    private final ConcurrentHashMap<String, ProjectMapping> trackedLibertyProjects;
    private final Set<String> ignoredProjects;

    protected AbstractLibertyProjectMapping() {
        // singleton
        trackedLibertyProjects = new ConcurrentHashMap<String, ProjectMapping>();
        ignoredProjects = new HashSet<String>();
        initialize();
    }

    protected abstract IPath getPluginConfigPath();

    public abstract IPath getLibertyBuildProjectCachePath(String projectName);

    // Initialize metadata from metadata file
    synchronized void initialize() {
        try {
            getProjectMapXML().unmarshall(trackedLibertyProjects, ignoredProjects);
        } catch (FileNotFoundException e) {
            // file is not expected to exist if no projects have been mapped in this workspace before
            Trace.trace(Trace.WARNING, "The project mapping file does not exist.", e);
        } catch (Exception e) {
            Trace.logError("Failed to read Liberty project-server mapping metadata from file", e);
        }
    }

    protected abstract AbstractProjectMapXML getProjectMapXML();

    public ProjectMapping clearProjectMapping(IProject proj) {
        if (proj == null) {
            return null;
        }

        String projectName = proj.getName();
        Trace.trace(Trace.INFO, "Clearing project mapping for project: " + projectName);
        ProjectMapping currentMapping = trackedLibertyProjects.get(projectName);

        // replace with empty mapping
        if (currentMapping != null) {
            trackedLibertyProjects.put(projectName, new ProjectMapping());
            storeMap(proj);
        }

        return currentMapping;
    }

    private void cachePluginConfig(IProject proj) {
        IResource resource = proj.findMember(getPluginConfigPath());
        if (resource == null) {
            try {
                Trace.trace(Trace.INFO, "Deleting outdated cached liberty build plugin config file");
                getLibertyBuildProjectCachePath(proj.getName()).toFile().delete();
            } catch (Exception e) {
                Trace.logError("Failed to delete outdated cached liberty build plugin config file.", e);
            }
            return; // nothing to cache
        }

        URI configURI = resource.getLocationURI();
        try {
            FileUtil.copy(configURI.toURL(), getLibertyBuildProjectCachePath(proj.getName()));
        } catch (Exception e) {
            Trace.logError("Error caching liberty build plugin configuration", e);
        }
    }

    // Liberty build plugin projects should be mapped as soon as the tools detect it contains the liberty plugin configuration xml and the user agrees to track the project
    protected synchronized void mapProject(IProject proj, IRuntime runtime, IServer server) {
        if (proj == null) {
            return;
        }

        String projectName = proj.getName();

        // If it's listed in the ignore list then remove it
        if (isIgnored(projectName)) {
            Trace.trace(Trace.INFO, "Removing project from ignore list: " + projectName);
            ignoredProjects.remove(projectName);
        }

        ProjectMapping currentMapping = trackedLibertyProjects.get(projectName);

        // Add new mapping
        if (currentMapping == null) {

            String runtimeID = runtime == null ? null : runtime.getId();
            String serverID = server == null ? null : server.getId();
            currentMapping = new ProjectMapping(runtimeID, serverID);
            trackedLibertyProjects.put(projectName, currentMapping);
            Trace.trace(Trace.INFO, "Mapped project: " + projectName);
            storeMap(proj);

        }
        // Update existing mapping
        else {

            currentMapping.runtimeID = runtime == null ? null : runtime.getId();
            currentMapping.serverID = server == null ? null : server.getId();
            Trace.trace(Trace.INFO, "Updated mapped project: " + projectName);
            storeMap(proj);

        }
        logMapping(projectName, currentMapping);
    }

    protected synchronized void ignoreProject(IProject proj) {
        if (proj == null)
            return;
        Trace.trace(Trace.INFO, "Adding project to ignore list: " + proj.getName());
        ignoredProjects.add(proj.getName());
        storeMap(proj);
    }

    protected synchronized void removeIgnoredProject(IProject proj) {
        if (proj == null || !ignoredProjects.contains(proj))
            return;
        Trace.trace(Trace.INFO, "Removing project from ignore list: " + proj.getName());
        ignoredProjects.remove(proj.getName());
        storeMap(proj);
    }

    // Liberty build plugin projects should be removed from the map once they've been deleted or closed in the workspace
    protected ProjectMapping unmapProject(IProject project) {
        if (project == null) {
            return null;
        }
        String projectName = project.getName();

        ProjectMapping mapping = trackedLibertyProjects.remove(projectName);
        ignoredProjects.remove(projectName);
        Trace.trace(Trace.INFO, "Removed project from tracking list: " + projectName);
        Trace.trace(Trace.INFO, "Removed project from ignore list: " + projectName);
        storeMap(project);

        return mapping;
    }

    // Liberty build plugin projects should be removed from the map once they've been deleted or closed in the workspace
    protected ProjectMapping unmapAndIgnoreProject(IProject project) {
        if (project == null) {
            return null;
        }
        String projectName = project.getName();

        ProjectMapping mapping = trackedLibertyProjects.remove(projectName);
        ignoredProjects.add(projectName);
        Trace.trace(Trace.INFO, "Removed project from tracking list: " + projectName);
        Trace.trace(Trace.INFO, "Added project to ignore list: " + projectName);
        storeMap(project);

        return mapping;
    }

    // Persist metadata to file so that it can be retrieved upon workbench restart
    private synchronized void storeMap(IProject proj) {
        try {
            cachePluginConfig(proj);
            getProjectMapXML().marshall(trackedLibertyProjects, ignoredProjects);
        } catch (Exception e) {
            Trace.logError("Failed to write Liberty build project-server mapping data to file", e);
        }
    }

    private void logMapping(String projectName, ProjectMapping currentMapping) {
        if (currentMapping == null) {
            Trace.logError("Liberty build project metadata error", null);
            return;
        }

        Trace.trace(Trace.INFO, "Project [ " + projectName + " ] is mapped to runtime with ID { " + currentMapping.runtimeID + " } and server with ID { "
                                + currentMapping.serverID + " }");
    }

    public boolean isEmpty() {
        return trackedLibertyProjects.isEmpty();
    }

    public Set<String> getMappedProjectSet() {
        return trackedLibertyProjects.keySet();
    }

    public boolean isTracked(String projectName) {
        return trackedLibertyProjects.keySet().contains(projectName);
    }

    public ProjectMapping getMapping(String projectName) {
        return trackedLibertyProjects.get(projectName);
    }

    public Set<String> getIgnoredProjectSet() {
        return ignoredProjects;
    }

    public synchronized boolean isIgnored(String projectName) {
        return ignoredProjects.contains(projectName);
    }

    public IProject getMappedProject(IServer server) {
        Set<String> mappedProjects = getMappedProjectSet();
        for (String project : mappedProjects) {
            ProjectMapping mapping = getMapping(project);
            if (mapping == null)
                continue;
            if (server.getId().equals(mapping.getServerID())) {
                return ResourcesPlugin.getWorkspace().getRoot().getProject(project);
            }
        }
        return null;
    }

    // ProjectMapping class
    public static class ProjectMapping {

        String runtimeID;
        String serverID;

        public ProjectMapping() {}

        public ProjectMapping(String runtimeID, String serverID) {
            this.runtimeID = runtimeID;
            this.serverID = serverID;
        }

        public String getRuntimeID() {
            return runtimeID;
        }

        public String getServerID() {
            return serverID;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object other) {
            if (other instanceof ProjectMapping) {
                ProjectMapping o = (ProjectMapping) other;
                return isEqual(runtimeID, o.runtimeID)
                       && isEqual(serverID, o.serverID);
            }
            return false;
        }

        private boolean isEqual(String one, String two) {
            if (one == two)
                return true;

            if (one == null || two == null)
                return false;

            return one.equals(two);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    } // end of ProjectMapping class
}
