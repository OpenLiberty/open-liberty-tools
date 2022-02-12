/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.buildplugin.integration.manager.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.ValidationResults;

import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereRuntimeLocator;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Activator;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ExcludeSyncModuleUtil;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.IProjectInspector;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping.ProjectMapping;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.internal.UIHelper;

@SuppressWarnings("restriction")
public abstract class AbstractLibertyManager implements IResourceChangeListener, ILibertyBuildPluginImplProvider {

    static final MutexRule generationRule = new MutexRule(); // should be used when scheduling any jobs for runtime/server creation/deletion/updates
    AbstractLibertyProjectMapping mappingHandler = getBuildPluginImpl().getMappingHandler();
    ILibertyBuildPluginImpl buildPluginHelper = getBuildPluginImpl();

    protected AbstractLibertyManager() {
        // singleton
        init();
    }

    private void init() {
        // TODO revalidate trackedLibertyProjects since the workspace may be missing some tracked projects or runtimes/servers after a relaunch
    }

    private void handleGeneration(IProject proj, IProgressMonitor monitor) {
        LibertyBuildPluginConfiguration config = buildPluginHelper.getLibertyBuildPluginConfiguration(proj, monitor);
        handleGeneration(proj, config);
    }

    void handleGeneration(IProject proj, LibertyBuildPluginConfiguration config) {
        // Immediately ignore the project to prevent the prompt from prompting again for a project that is already in the process of being mapped
        mappingHandler.ignoreProject(proj);
        if (config != null && isValidConfig(config)) {
            ISchedulingRule rule = MultiRule.combine(new ISchedulingRule[] { generationRule, ResourcesPlugin.getWorkspace().getRoot(), proj });
            CreationJob genJob = new CreationJob(NLS.bind(Messages.createJob, proj.getName()), proj, config);
            genJob.setRule(rule);
            genJob.schedule();
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Runtime and server generation job scheduled");
        }
    }

    boolean isValidConfig(LibertyBuildPluginConfiguration config) {
        if (config.getConfigValue(ConfigurationType.installDirectory) == null) {
            Trace.logError("A runtime installation directory is not configured. Ensure the liberty build plugin configuration is coorrect.", null);
            return false;
        } else if (config.getConfigValue(ConfigurationType.configFile) == null) {
            Trace.logError("A server configuration file is not configured. Ensure the liberty build plugin configuration is correct.", null);
            return false;
        }
        return true;
    }

    protected abstract boolean handleGenerationPrompt(String name);

    protected abstract boolean isSupportedProjectType(IProject project);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
     */
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta rootDelta = event.getDelta();
        if (rootDelta == null)
            return;

        IResourceDelta[] projectDeltas = rootDelta.getAffectedChildren();
        if (projectDeltas == null)
            return;

        List<IResourceDelta> applicableResourceDeltas = new ArrayList<IResourceDelta>();
        // Do preliminary check to see if the changes are only from projects that we are interested in before scheduling any ScanJobs.
        // Examples:
        // 1) Create Server and Runtime results in two project deltas.  One for the build plug-in project, and one for the
        // newly created Runtime project.   We are only interested in the first one.
        // 2) Multiple source file changes from one project results in one delta (with multiple children), but the project
        // is a Web project that we don't care about
        // So, without the prelim. check, for two build plugin implementations, for example 1, four scans jobs are scheduled, but two are applicable.
        // And, for example 2, two scan jobs are scheduled, but none are applicable.
        if (projectDeltas.length > 0) {
            for (int i = 0; i < projectDeltas.length; i++) {
                IResource resource = projectDeltas[i].getResource();
                if (resource instanceof IProject) {
                    IProject project = (IProject) resource;
                    // If the project is deleted, or the project just got closed, then checking the nature is not possible.
                    if (projectDeltas[i].getKind() == IResourceDelta.REMOVED || !project.isAccessible() || isSupportedProjectType(project)) {
                        try {
                            // Check if the project is removed in which case a scan job is needed to do the clean up
                            if (projectDeltas[i].getKind() == IResourceDelta.REMOVED) {
                                applicableResourceDeltas.add(projectDeltas[i]);
                                continue;
                            }
                            // If the project is ignored then skip it
                            if (mappingHandler.isIgnored(projectDeltas[i].getResource().getProject().getName())) {
                                continue;
                            }
                            // If the plugin config file has been updated a scan job is needed
                            IResourceDelta pluginConfigDelta = projectDeltas[i].findMember(new Path(buildPluginHelper.getRelativeBuildPluginConfigurationFileLocation()));
                            if (pluginConfigDelta != null && pluginConfigDelta.getResource() != null) {
                                applicableResourceDeltas.add(projectDeltas[i]);
                                continue;
                            }
                            // If the project doesn't have a valid server then skip it
                            ProjectMapping mapping = mappingHandler.getMapping(project.getName());
                            if (mapping == null || mapping.getServerID() == null) {
                                continue;
                            }
                            IServer server = ServerCore.findServer(mapping.getServerID());
                            if (server == null) {
                                continue;
                            }
                            WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                            if (wsServer == null) {
                                continue;
                            }
                            // Check if the server configuration has been updated
                            IProjectInspector pi = getBuildPluginImpl().getProjectInspector(project);
                            LibertyBuildPluginConfiguration config = pi.getBuildPluginConfiguration(null);
                            if (config != null) {
                                String configVal = config.getConfigValue(ConfigurationType.configFile);
                                if (configVal != null) {
                                    IPath srcConfigPath = new Path(configVal);
                                    if (srcConfigPath.toFile().exists() && project.getLocation().isPrefixOf(srcConfigPath)) {
                                        srcConfigPath = srcConfigPath.makeRelativeTo(project.getLocation());
                                        IResourceDelta srcConfigDelta = projectDeltas[i].findMember(srcConfigPath);
                                        if (srcConfigDelta != null && srcConfigDelta.getResource() != null) {
                                            applicableResourceDeltas.add(projectDeltas[i]);
                                            continue;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            if (Trace.ENABLED) {
                                Trace.trace(Trace.WARNING, "Checking resource deltas for relevant changes has encountered an error for project: " + project.getName(), e);
                            }
                        }
                    }
                }
            }
        }

        if (!applicableResourceDeltas.isEmpty()) {
            ISchedulingRule rule = MultiRule.combine(new ISchedulingRule[] { generationRule, ResourcesPlugin.getWorkspace().getRoot() });
            ScanJob scanJob = new ScanJob(Messages.scanJob, applicableResourceDeltas.toArray(new IResourceDelta[0]));
            scanJob.setRule(rule);
            // schedule the job after 5 seconds so that the other workspace jobs can be scheduled first, this is important because
            // we want the other jobs (git/m2e) to finish setting up the project before we scan it
            scanJob.schedule(5000);
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Liberty build plugin manager scanning job scheduled");
        }
    }

    static synchronized List<IServer> getServerList(IRuntime runtime) {
        IServer[] servers = ServerCore.getServers();
        if (servers == null || runtime == null) {
            return Collections.emptyList();
        }

        List<IServer> list = new ArrayList<IServer>();
        for (IServer server : servers) {
            if (runtime.equals(server.getRuntime()))
                list.add(server);
        }

        return list;
    }

    public synchronized void triggerAddProject(IProject project, IProgressMonitor monitor) {
        Trace.trace(Trace.INFO, "Generate Liberty runtime and server action invoked.");
        handleGeneration(project, monitor);
    }

    void printTrackedLibertyProjects() {
        // Only print to workspace log if trace is enabled
        if (mappingHandler.isEmpty()) {
            Trace.trace(Trace.INFO, "There are no tracked Liberty projects.");
            return;
        }
        StringBuffer sb = new StringBuffer();
        for (String p : mappingHandler.getMappedProjectSet()) {
            sb.append(p + "\n");
        }
        Trace.trace(Trace.INFO, "Tracked Liberty Projects:\n" + sb.toString());
    }

    private class ScanJob extends WorkspaceJob {
        IResourceDelta[] projectDeltas;

        public ScanJob(String name, IResourceDelta[] projectDeltas) {
            super(name);
            this.projectDeltas = projectDeltas;
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            try {
                // Check project deltas for plugin config file changes
                for (IResourceDelta projectDelta : projectDeltas) {
                    // If we're dealing with an ignored project then there's no need to do anything because the user would have to trigger the runtime and server generation manually
                    if (mappingHandler.isIgnored(projectDelta.getResource().getProject().getName())) {
                        // if it is ignored, then it is a project that is of our interest.
                        if (projectDelta.getKind() == IResourceDelta.REMOVED) {
                            handleProjectRemoved(projectDelta.getResource().getProject(), monitor);
                        }
                        return Status.OK_STATUS;
                    }

                    boolean isHandled = processPluginConfigDeltas(projectDelta, monitor);

                    // The following only applies to supported projects
                    if (buildPluginHelper.isSupportedProject(projectDelta.getResource().getProject(), monitor)) {
                        // If the runtime/server aren't being completely regenerated then we can look for src config updates
                        if (!isHandled)
                            lookForSrcConfigChanges(projectDelta, monitor);
                    }
                }
            } catch (Exception e) {
                Trace.trace(Trace.INFO, "Scanning for Liberty build plugin configuration changes has encountered an error", e);
            }
            return Status.OK_STATUS;
        }

        private boolean processPluginConfigDeltas(IResourceDelta projectDelta, IProgressMonitor monitor) {
            IResourceDelta pluginConfigDelta = projectDelta.findMember(new Path(buildPluginHelper.getRelativeBuildPluginConfigurationFileLocation()));
            if (pluginConfigDelta == null) {
                // There's no plugin config file so we can ignore the project
                return false;
            }

            IResource resource = pluginConfigDelta.getResource();
            if (resource == null)
                return false;

            if (pluginConfigDelta.getKind() == IResourceDelta.ADDED) {
                Trace.trace(Trace.INFO, "LibertyManager detected added resource: " + resource.getFullPath());
                handleProjectAdded(resource.getProject(), new NullProgressMonitor());
                return true;
            } else if (pluginConfigDelta.getKind() == IResourceDelta.CHANGED) {
                Trace.trace(Trace.INFO, "LibertyManager detected changed resource: " + resource.getFullPath());
                IProject project = resource.getProject();
                if (requiresUpdate(project)) {
                    handleProjectRemoved(project, monitor);
                    handleProjectAdded(project, monitor);
                    return true;
                }
            } else if (pluginConfigDelta.getKind() == IResourceDelta.REMOVED) {
                Trace.trace(Trace.INFO, "LibertyManager detected removed resource: " + resource.getFullPath());
                handleProjectRemoved(resource.getProject(), new NullProgressMonitor());
                return true;
            }
            return false;
        }

        private boolean lookForSrcConfigChanges(IResourceDelta projectDelta, IProgressMonitor monitor) {
            Set<String> mappedProjects = mappingHandler.getMappedProjectSet();
            IProject project = projectDelta.getResource().getProject();
            if (!mappedProjects.contains(project.getName()))
                return false;

            // Check whether the src config file is part of the update
            IProjectInspector pi = getBuildPluginImpl().getProjectInspector(project);
            LibertyBuildPluginConfiguration config = pi.getBuildPluginConfiguration(null);
            if (config == null)
                return false;
            String configVal = config.getConfigValue(ConfigurationType.configFile);
            if (configVal == null)
                return false;

            // If the project is not mapped to a server then there's nothing to do
            ProjectMapping m = mappingHandler.getMapping(project.getName());
            if (m == null)
                return false;

            // If the server or runtime are not mapped then there's nothing to do
            String serverID = m.getServerID();
            if (serverID == null)
                return false;
            IServer server = ServerCore.findServer(m.getServerID());
            if (server == null)
                return false;
            WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            if (wsServer == null)
                return false;

            IPath srcConfigPath = new Path(configVal);
            if (!srcConfigPath.toFile().exists())
                return false;

            if (project.getLocation().isPrefixOf(srcConfigPath))
                srcConfigPath = srcConfigPath.makeRelativeTo(project.getLocation());

            IResourceDelta srcConfigDelta = projectDelta.findMember(srcConfigPath);
            if (srcConfigDelta == null)
                return false;
            IResource resource = srcConfigDelta.getResource();
            if (resource == null)
                return false;

            // Delegate to the build plugin to copy the source file to the output directory
            buildPluginHelper.updateSrcConfig(project, config, monitor);

            // Any changes to the src server config have been copied over to the
            // user directory so make sure it is refreshed
            UserDirectory userDir = wsServer.getServerInfo().getUserDirectory();
            final IProject userDirProject = userDir.getProject();
            if (userDirProject != null && userDirProject.isAccessible()) {
                try {
                    userDirProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                } catch (Exception e) {
                    Trace.logError("Refreshing user directory project failed", e);
                }
            }

            // Change the server to republish state so it will process the changed file on the next publish operation
            WebSphereServerBehaviour wsb = wsServer.getWebSphereServerBehaviour();
            wsb.setWebSphereServerPublishState(IServer.PUBLISH_STATE_INCREMENTAL);
            return true;
        }

        private synchronized void handleProjectAdded(final IProject proj, IProgressMonitor monitor) {
            boolean isEnhanced = buildPluginHelper.isSupportedProject(proj, monitor);
            if (isEnhanced) {
                LibertyBuildPluginConfiguration config = buildPluginHelper.getLibertyBuildPluginConfiguration(proj, monitor);
                if (config != null && isValidConfig(config)) {
                    final boolean[] doGeneration = { false };
                    if (mappingHandler.isTracked(proj.getName())) {
                        doGeneration[0] = true;
                    } else {

                        // Open runtime explorer view
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                UIHelper.openRuntimeExplorerView();
                            }
                        });

                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Prompting for project mapping of project: " + proj.getName());
                        if (mappingHandler.isIgnored(proj.getName())) {
                            Trace.trace(Trace.INFO, "The project " + proj.getName() + " is on the ignore list");
                            // Don't prompt for previously ignored projects, users can view these projects in the
                            // runtime explorer view and generate a runtime/server via the view's actions later on
                            doGeneration[0] = false;
                        } else if (!PromptUtil.isSuppressDialog()) {
                            Display.getDefault().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    doGeneration[0] = handleGenerationPrompt(proj.getName());
                                    if (!doGeneration[0])
                                        mappingHandler.ignoreProject(proj);
                                }
                            });
                        } else
                            doGeneration[0] = true; // should only apply to headless tests

                    }

                    // user opted out of auto generation
                    if (!doGeneration[0]) {
                        mappingHandler.ignoreProject(proj);
                        // refresh the view so the placeholders in the Runtime Explorer will be visible
                        RefreshJob refreshJob = new RefreshJob();
                        refreshJob.setPriority(Job.SHORT);
                        refreshJob.schedule();

                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Skipping runtime and server creation");
                        return;
                    }

                    handleGeneration(proj, config);
                }
            } else {
                Trace.logError("The project " + proj.getName() + " uses a liberty build plugin but does not meet the requirements for automatic runtime and server management",
                               null);
            }
            printTrackedLibertyProjects();
        }

        private synchronized void handleProjectRemoved(IProject proj, IProgressMonitor progressMonitor) {
            Trace.trace(Trace.INFO, "Handling removal of project: " + proj.getName());
            SubMonitor monitor = SubMonitor.convert(progressMonitor, 30);
            String projName = proj.getName();
            if (projName == null)
                return;

            ProjectMapping mapping = mappingHandler.getMapping(projName);

            String runtimeId = null;
            String serverId = null;
            IRuntime mappedRuntime = null;
            IServer mappedServer = null;
            if (mapping != null) {
                Trace.trace(Trace.INFO, proj.getName() + " is being tracked");
                runtimeId = mapping.getRuntimeID();
                serverId = mapping.getServerID();
                Trace.trace(Trace.INFO, "runtimeId: " + runtimeId);
                Trace.trace(Trace.INFO, "serverId: " + serverId);
            }

            if (runtimeId == null && serverId == null) { // project is not mapped to anything so nothing to clean up
                Trace.trace(Trace.INFO, proj.getName() + " is not mapped to a runtime or server");
                // still unmap project to remove it from the ignored list
                mappingHandler.unmapProject(proj);
                // refresh the view so the placeholders in the Runtime Explorer will get updated
                RefreshJob refreshJob = new RefreshJob();
                refreshJob.setPriority(Job.SHORT);
                refreshJob.schedule();
                return;
            }

            if (runtimeId != null) {
                mappedRuntime = ServerCore.findRuntime(runtimeId);
                if (mappedRuntime == null) {
                    Trace.trace(Trace.INFO, "A runtime with location " + runtimeId + " was not found");
                }
            }

            if (serverId != null)
                mappedServer = ServerCore.findServer(serverId);

            List<IServer> serverList = getServerList(mappedRuntime);

            List<ISchedulingRule> allRules = new ArrayList<ISchedulingRule>();
            allRules.add(generationRule);
            allRules.add(ResourcesPlugin.getWorkspace().getRoot());
            for (IServer server : serverList) {
                allRules.add(server);
            }

            ISchedulingRule rule = MultiRule.combine(allRules.toArray(new ISchedulingRule[allRules.size()]));

            if (monitor.isCanceled())
                return;

            Trace.trace(Trace.INFO, "Schedule deletion job");
            DeletionJob genJob = new DeletionJob(NLS.bind(Messages.deleteJob, projName), proj, mappedRuntime, mappedServer);
            genJob.setRule(rule);
            genJob.schedule();
        }

        private boolean requiresUpdate(IProject project) {
            IProjectInspector pi = getBuildPluginImpl().getProjectInspector(project);
            File cachedFile = pi.getCachedLibertyBuildPluginConfigurationFile(null);
            if (cachedFile == null || !cachedFile.exists()) {
                return true;
            }

            ProjectMapping mapping = mappingHandler.getMapping(project.getName());
            if (mapping == null && project.isOpen())
                return true;

            else if (!project.isOpen() && mapping != null)
                return true;

            // ensure mapped runtime and server exist
            else if (mapping != null) {
                String runtimeID = mapping.getRuntimeID();
                String serverID = mapping.getServerID();
                IRuntime rt = ServerCore.findRuntime(runtimeID);
                if (rt == null)
                    return true;

                IServer sv = ServerCore.findServer(serverID);
                if (sv == null)
                    return true;
            }

            // Compare the cached config to the current config to see if anything changed that requires update
            try {
                LibertyBuildPluginConfiguration currentConfig = pi.getBuildPluginConfiguration(null);
                LibertyBuildPluginConfiguration cachedConfig = pi.getCachedBuildPluginConfiguration(null);
                Set<ConfigurationType> deltas = currentConfig.getDelta(cachedConfig);

                if (!deltas.isEmpty()) {
                    Set<ConfigurationType> updateTriggers = ConfigurationType.getAllUpdateTriggers();
                    for (ConfigurationType t : deltas) {
                        updateTriggers.contains(t);
                        return true;
                    }
                }
            } catch (Exception e) {
                Trace.logError("Error encountered while computing differences in project configuration", e);
            }
            return false;
        }
    }

    private class CreationJob extends WorkspaceJob {

        IProject project;
        LibertyBuildPluginConfiguration config;

        // existing runtimes within the workbench are not mapped to the lifecycle of the project
        IRuntime mappedRuntime = null;
        IRuntime runtime = null;
        IServer server = null;
        IServerWorkingCopy serverWc = null;

        public CreationJob(String name, IProject project, LibertyBuildPluginConfiguration config) {
            super(name);
            this.project = project;
            this.config = config;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            IStatus status = Status.OK_STATUS;
            try {
                status = createRuntime(config.getConfigValue(ConfigurationType.installDirectory), monitor);

                if (!status.isOK())
                    return status;

                status = createServer(monitor);

                if (!status.isOK())
                    return status;

                status = serverSetup(monitor);

                if (!status.isOK())
                    return status;
            } finally {
                if (status.isOK()) {
                    // Once the runtime and server creation are done we can update the mapping
                    mappingHandler.mapProject(project, mappedRuntime, server);
                } else {
                    cleanup();
                }

                // refresh the view so the placeholders in the Runtime Explorer will be visible
                RefreshJob refreshJob = new RefreshJob();
                refreshJob.setPriority(Job.SHORT);
                refreshJob.schedule();
            }
            return status;
        }

        private void cleanup() {
            if (serverWc != null) {
                try {
                    serverWc.delete();
                    serverWc = null;
                } catch (Exception e) {
                    Trace.trace(Trace.WARNING, "Problem during cleanup", e);
                }
            }
            if (server != null) {
                try {
                    server.delete();
                    server = null;
                } catch (Exception e) {
                    Trace.trace(Trace.WARNING, "Problem during cleanup", e);
                }
            }
            if (mappedRuntime != null) {
                try {
                    mappedRuntime.delete();
                    mappedRuntime = null;
                } catch (Exception e) {
                    Trace.trace(Trace.WARNING, "Problem during cleanup", e);
                }
            }
            /*
             * The runtime and server generation failed but we don't want to prompt the user again so keep ignoring the project.
             * They can execute the generate runtime and server action to trigger creation again.
             */
            mappingHandler.unmapAndIgnoreProject(project);
        }

        private IStatus createRuntime(String installDir, IProgressMonitor monitor) throws CoreException {
            if (installDir == null)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.invalidRuntime, installDir));

            IPath installDirectory = new Path(installDir);
            if (!WebSphereRuntime.isValidLocation(installDirectory))
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.invalidRuntime, installDir));

            // Check if another runtime already references this runtime location and reuse the instance if it does
            IRuntime[] runtimes = ServerCore.getRuntimes();
            for (IRuntime rt : runtimes) {
                if (WebSphereUtil.isWebSphereRuntime(rt) && installDirectory.equals(rt.getLocation())) {
                    // We'll use this existing instance but won't add it to the project mapping because the mapping only tracks
                    // runtimes that we need to create/delete corresponding to a project's lifecycle.
                    // If the runtime pre-exists then we shouldn't be automatically deleting or recreating it at any point.
                    runtime = rt;
                    Trace.trace(Trace.INFO, "Reusing runtime " + rt.getName() + " from " + installDirectory.toOSString());

                    // If this is the mapped runtime ensure it remains mapped, this could happen if the server needs to be generated
                    // but the runtime remains the same
                    ProjectMapping mapping = mappingHandler.getMapping(project.getName());
                    if (mapping != null && runtime.getId().equals(mapping.getRuntimeID())) {
                        mappedRuntime = runtime;
                    } else if (mappingHandler.getMappedProjects(rt).length > 0) {
                        // If the runtime is mapped to another project then share the runtime and map this project as well
                        mappedRuntime = runtime;
                    }
                    return Status.OK_STATUS;
                }
            }

            // If the runtime doesn't exist then try to create it
            String name = WebSphereUtil.getUniqueRuntimeName(NLS.bind(Messages.runtimeLabel, project.getName()), runtimes);
            Trace.trace(Trace.INFO, "Creating runtime: " + name + " from " + installDirectory.toOSString());
            IRuntimeWorkingCopy runtimeWc = WebSphereRuntimeLocator.getRuntimeFromDir(installDirectory, monitor);
            if (runtimeWc == null)
                return null;
            runtimeWc.setName(name);
            runtime = runtimeWc.save(true, null);
            mappedRuntime = runtime; // map this runtime to the project

            WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            IStatus status = wsRuntime.validate();

            if (!status.isOK())
                Trace.trace(Trace.WARNING, "The runtime validation failed: " + status.getMessage());

            return status;
        }

        private IStatus serverSetup(IProgressMonitor monitor) {
            try {
                // Get modules from project
                IProjectInspector pi = getBuildPluginImpl().getProjectInspector(project);
                IModule[] projectModules = pi.getProjectModules();

                for (IModule module : projectModules) {
                    if (pi.isSupportedModule(module)) {
                        // Module setup
                        IModule[] serverModules = server.getModules();
                        int len = serverModules.length;
                        boolean found = false;
                        for (int i = 0; i < len; i++) {
                            if (module.equals(serverModules[i])) {
                                found = true;
                            }
                        }

                        if (!found) {
                            boolean isWc = server.isWorkingCopy();
                            WebSphereServer wsServer = null;
                            if (!isWc) {
                                serverWc = server.createWorkingCopy();
                                wsServer = (WebSphereServer) serverWc.loadAdapter(WebSphereServer.class, null);
                            } else {
                                serverWc = (IServerWorkingCopy) server;
                                wsServer = (WebSphereServer) serverWc.loadAdapter(WebSphereServer.class, null);
                            }

                            if (wsServer != null && wsServer.canModifyModules(new IModule[] { module }, null).isOK()) {
                                LibertyBuildPluginConfiguration config = buildPluginHelper.getLibertyBuildPluginConfiguration(project, monitor);

                                // Need to add the local connector. For the non-loose dropins case, a jmx check
                                // will happen, but if the connector is missing, an error will come up
                                wsServer.addLocalConnectorFeature(monitor);
                                if (config != null) {
                                    // Need to include all project modules in the exclusion otherwise the other project modules will be treated as external modules
                                    ExcludeSyncModuleUtil.updateExcludeSyncModuleMapping(projectModules, config, wsServer.getWebSphereServerBehaviour());
                                    ConfigurationFile serverConfig = wsServer.getConfiguration();
                                    if (serverConfig == null) {
                                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.configFileNotFound, wsServer.getServerName()));
                                    }
//                                    Application[] configuredApps = serverConfig.getApplications();
//                                    boolean appConfigured = false;
//                                    for (Application app : configuredApps) {
//                                        if (app.getName().equals(module.getName())) {
//                                            appConfigured = true;
//                                            break;
//                                        }
//                                    }
//                                    if (!appConfigured) {
//                                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.cannotBindApplication,
//                                                                                                       new String[] { module.getProject().getName(), server.getName(),
//                                                                                                                      module.getName() }));
//                                    }
                                    serverWc.modifyModules(new IModule[] { module }, new IModule[] {}, monitor);
                                    server = serverWc.save(true, null);
                                }
                            }
                        } // module not found
                    } // supported module
                } // end of module for loop

                org.eclipse.wst.common.project.facet.core.runtime.IRuntime facetRuntime = FacetUtil.getRuntime(runtime);

                if (facetRuntime != null) {
                    Set<org.eclipse.wst.common.project.facet.core.runtime.IRuntime> runtimeSet = new HashSet<org.eclipse.wst.common.project.facet.core.runtime.IRuntime>();
                    runtimeSet.add(facetRuntime);

                    // Add the faceted project to prevent the validation error of the source server.xml from showing up in the project
                    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
                    facetedProject.setTargetedRuntimes(runtimeSet, monitor);
                    facetedProject.setPrimaryRuntime(facetRuntime, monitor);
                    Trace.trace(Trace.INFO, "Setting targeted runtime to: " + facetRuntime.getName());
                }

                // Trigger configuration validation on the src config file otherwise
                // the file will have a validation error that will cause problems during publish
                String configVal = config.getConfigValue(ConfigurationType.configFile);
                if (configVal != null) {
                    Path path = new Path(configVal);
                    IFile configFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
                    if (configFile != null) {
                        Trace.trace(Trace.INFO, "Validate file: " + path.toString());
                        ValidationResults results = ValidationFramework.getDefault().validate(configFile, monitor);
                        if (results.getSeverityError() > 0) {
                            Trace.trace(Trace.INFO, "Validation failed");
                        } else {
                            Trace.trace(Trace.INFO, "Validation passed");
                        }
                    }
                }

            } catch (CoreException e) {
                Trace.logError("Failed to link configured application to project: " + project.getName(), e);
            }
            return Status.OK_STATUS;
        }

        private IStatus createServer(IProgressMonitor monitor) {
            if (runtime == null)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.noRuntimeDetected);

            try {
                String hostname = "localhost";
                String serverName = config.getConfigValue(ConfigurationType.serverName);
                if (serverName == null)
                    serverName = runtime.getName();

                String looseConfigValue = config.getConfigValue(ConfigurationType.looseApplication);
                boolean isLC = Boolean.parseBoolean(looseConfigValue);

                WebSphereRuntime wrt = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);

                UserDirectory userDir = null;
                String userDirVal = config.getConfigValue(ConfigurationType.userDirectory);

                if (userDirVal != null) {
                    IPath userPath = new Path(userDirVal);
                    if (userPath.toFile().exists()) {
                        IProject userDirProject = null;
                        for (IProject proj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                            if (proj.getLocation().equals(userPath)) {
                                userDirProject = proj;
                            }
                        }
                        if (userDirProject == null)
                            userDirProject = WebSphereUtil.createUserProject(runtime.getName(), userPath, null);
                        userDir = new UserDirectory(wrt, userPath, userDirProject);
                    }
                }

                IRuntimeWorkingCopy runtimeWorkingCopy = runtime.createWorkingCopy();
                WebSphereRuntime wRuntimeWorkingCopy = (WebSphereRuntime) runtimeWorkingCopy.loadAdapter(WebSphereRuntime.class, null);
                if (userDir == null) {
                    //create default user directory which is wlp.user.dir inside the runtime
                    userDir = wRuntimeWorkingCopy.createDefaultUserDirectory(null);
                }

                IPath serverPath = userDir.getPath().append("servers").append(serverName);
                buildPluginHelper.preServerSetup(project, config, serverPath, monitor);

                // The server files don't exist so we cannot create a server for the project
                if (!serverPath.toFile().exists()) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The server files were not found in location " + serverPath.toOSString());
                }

                Trace.trace(Trace.INFO, "Creating server: " + serverName + " using User Dir: " + userDir.getPath() + " loose config: " + isLC);

                String serverTypeId = runtime.getId().endsWith(com.ibm.ws.st.core.internal.Constants.V85_ID_SUFFIX) ? com.ibm.ws.st.core.internal.Constants.SERVERV85_TYPE_ID : com.ibm.ws.st.core.internal.Constants.SERVER_TYPE_ID;
                IServerType st = ServerCore.findServerType(serverTypeId);
                IServerWorkingCopy wc;
                wc = st.createServer(null, null, runtime, null);

                wc.setHost(hostname);
                String instanceName = WebSphereUtil.getUniqueServerName(project.getName(), ServerCore.getServers());
                wc.setName(instanceName); // the value that will be used as the name of the server in the servers view and its id

                WebSphereServer wsServer = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);
                wsServer.setDefaults(new NullProgressMonitor());
                wsServer.setServerName(serverName); // the server name that matches the folder name in the usr/servers dir
                wsServer.setUserDir(userDir);
                wsServer.setLooseConfigEnabled(isLC);
                wsServer.setServerType(buildPluginHelper.getServerType());

                server = wc.save(true, null);
                wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                WebSphereServerInfo wsInfo = wsServer.getServerInfo();
                if (wsInfo == null) {
                    // If the user directory doesn't exist we should add it
                    boolean found = false;
                    for (UserDirectory usr : wRuntimeWorkingCopy.getUserDirectories()) {
                        if (usr.getPath().equals(userDir.getPath())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        wRuntimeWorkingCopy.addUserDirectory(userDir);
                    runtime = runtimeWorkingCopy.save(true, null);
                }

                WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                wsRuntime.updateServerCache(true);

                //update cache again to initialize server.env, jvm.options and bootstrap.properties file variables
                //if the corresponding files exist
                if (wsInfo != null)
                    wsInfo.updateCache();
            } catch (Exception e) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.createServerActionFailed, e);
            }
            return Status.OK_STATUS;
        }
    }

    private class DeletionJob extends WorkspaceJob {
        IRuntime runtime;
        IServer server;
        IProject proj;

        public DeletionJob(String name, IProject proj, IRuntime runtime, IServer server) {
            super(name);
            this.runtime = runtime;
            this.server = server;
            this.proj = proj;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            performDelete(monitor);
            return Status.OK_STATUS;
        }

        // This method is called when:
        // 1. The project is deleted - project mapping being removed from the metadata completely.
        // 2. The plugin config file is deleted (eg. mvn clean is called) - keep the project element of
        //    the mapping but remove the runtime and server child elements.
        // 3. The plugin config file is changed - If the project was previously mapped then just clear the mapping,
        //    otherwise just ensure the project is unmapped
        private void performDelete(IProgressMonitor monitor) {
            if (runtime == null) {
                mappingHandler.unmapProject(proj);
                return;
            }

            if (mappingHandler.getMappedProjects(runtime).length > 1) {
                // The runtime is being used by other projects so only delete the server
                Trace.trace(Trace.INFO, "The " + runtime.getName() + " is being used by other projects.");
                if (server != null) {
                    try {
                        ensureServersStopped(monitor, server);
                        Trace.trace(Trace.INFO, "Deleting server: " + server.getName());
                        server.delete();
                    } catch (Exception e) {
                        Trace.logError("A problem was encountered trying to remove the server: " + server.getName(), e);
                    }
                }
            } else {

                // check if runtime is targeted
                boolean inUse = false;
                try {
                    inUse = FacetUtil.isRuntimeTargeted(runtime);
                } catch (Throwable t) {
                    // ignore - facet framework not found
                }

                List<IServer> serverList = getServerList(runtime);
                if (!serverList.isEmpty()) {
                    try {
                        ensureServersStopped(monitor, serverList.toArray(new IServer[serverList.size()]));
                        Trace.trace(Trace.INFO, "Deleting servers associated with runtime: " + runtime.getName());
                        for (IServer s : serverList) {
                            Trace.trace(Trace.INFO, "Deleting server: " + s.getName());
                            s.delete();
                        }
                    } catch (Exception e) {
                        Trace.logError("Problem encountered while deleting servers", e);
                        return;
                    }

                    if (!inUse) {
                        try {
                            Trace.trace(Trace.INFO, "Removing target facets for runtime: " + runtime.getName());
                            FacetUtil.removeTargets(runtime, new NullProgressMonitor());
                        } catch (Throwable t) {
                            // facet framework failure, or may be missing entirely
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Error deleting facet targets", t);
                        }
                    }
                }

                try {
                    Trace.trace(Trace.INFO, "Deleting runtime: " + runtime.getName());
                    runtime.delete();
                } catch (CoreException ce) {
                    Trace.logError(ce.getLocalizedMessage(), ce);
                } catch (Exception e) {
                    Trace.logError("Error deleting runtime: " + runtime.getId(), e);
                }
            }

            if (proj.exists() && proj.isOpen()) {
                // This is most likely build scenario where the build deletes all the target files including
                // the runtime but we don't want to completely remove the mapping or else the user will be prompted again
                // when the build regenerates the target files at a later time. From the user perspective a clean should just wipe out the
                // runtime and server instances and a rebuild or other goal/task that generates the runtime should bring
                // them back without prompting.
                mappingHandler.clearProjectMapping(proj);
            } else {
                // Project is removed so we should completely remove the project from the mapping
                mappingHandler.unmapProject(proj);
            }

            printTrackedLibertyProjects();
        }

        private void ensureServersStopped(IProgressMonitor monitor, IServer... serverList) throws Exception {
            for (IServer server : serverList) {

                // First check if the runtime installation still exists, if it doesn't there's no need to attempt stopping any servers
                IRuntime runtime = server.getRuntime();
                if (runtime == null || runtime.getLocation() == null || !runtime.getLocation().toFile().exists()) {
                    if (runtime != null)
                        Trace.trace(Trace.INFO, "Runtime doesn't exist, skipping stop servers for runtime: " + runtime.getName());
                    else
                        Trace.trace(Trace.INFO, "The server does not have an associated runtime");
                    break;
                }

                // The server instance may still exist but the underlying files on the system may not exist.
                // So we need to check that the files exist before attempting to stop the server.
                WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                WebSphereServerInfo wsInfo = wsServer.getWebSphereServerBehaviour().getWebSphereServerInfo();
                if (wsInfo == null) {
                    Trace.trace(Trace.INFO, "Server doesn't exist, skipping stop server for server: " + server.getName());
                    continue;
                }

                IPath serverPath = wsInfo.getServerPath();
                if (!serverPath.toFile().exists()) {
                    Trace.trace(Trace.INFO, "Server folder is missing, skipping stop server for server: " + server.getName());
                    continue;
                }

                Trace.trace(Trace.INFO, "Stopping server: " + server.getName());
                wsServer.getWebSphereServerBehaviour().stop(true, monitor);
                boolean waitForServerStop = wsServer.getWebSphereServerBehaviour().waitForServerStop(server.getStopTimeout() * 1000); // Default is 15 seconds
                if (waitForServerStop) {
                    long endTime = System.currentTimeMillis() + 5000;
                    // Also check the IServer state but need to give it some time.
                    while (IServer.STATE_STOPPED != server.getServerState()) {
                        long currentTime = System.currentTimeMillis();
                        Thread.sleep(250);
                        if (currentTime >= endTime) {
                            if (Trace.ENABLED) {
                                Trace.trace(Trace.INFO, "Server state failed to change to stop: " + server.getName());
                            }
                            throw new Exception("Server state failed to change to stop: " + server.getName());
                        }
                    }
                } else {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Failed to stop server: " + server.getName());
                    }
                    throw new Exception("Failed to stop server: " + server.getName());
                }
            }
        }
    } // end of delete job

    private class RefreshJob extends WorkspaceJob {

        public RefreshJob() {
            super(Messages.refreshJob);
        }

        /** {@inheritDoc} */
        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    UIHelper.refreshRuntimeExplorerView();
                }
            });

            return Status.OK_STATUS;
        }

    }

    static class MutexRule implements ISchedulingRule {
        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return rule == this;
        }

        @Override
        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }
    }
}
