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

package com.ibm.etools.maven.liberty.integration.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.etools.maven.liberty.integration.manager.internal.LibertyManager;
import com.ibm.etools.maven.liberty.integration.manager.internal.LibertyMavenProjectMapping;
import com.ibm.etools.maven.liberty.integration.manager.internal.MavenProjectInspector;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Activator;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.IProjectInspector;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;

@SuppressWarnings("restriction")
public class LibertyMaven implements ILibertyBuildPluginImpl {

    private static LibertyMaven instance = new LibertyMaven();

    private LibertyMaven() {
        // singleton
    }

    public static ILibertyBuildPluginImpl getInstance() {
        return instance;
    }

    public static boolean isMavenProject(IProject project, IProgressMonitor monitor) {
        return instance.getProjectInspector(project, monitor).isMavenProject();
    }

    /**
     * Retrieves the liberty maven configuration model for a given project.
     *
     * @param project a maven-enabled project that is expected to be configured with the correct version/configuration of the liberty-maven-plugin in its POM
     * @param monitor
     * @return the {@code LibertyMavenConfiguration} model or null if the model could not be generated
     */
    public static LibertyBuildPluginConfiguration getLibertyMavenProjectConfiguration(IProject project, IProgressMonitor monitor) {
        return instance.getProjectInspector(project, monitor).getBuildPluginConfiguration(monitor);
    }

    public static boolean runMavenGoal(final IPath workingDir, String specifiedGoal, List<String> profiles, IProgressMonitor monitor) {
        boolean isSuccessful = true;
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType("org.eclipse.m2e.Maven2LaunchConfigurationType");

        String goal = specifiedGoal + " -DskipTests=true";
        try {
            Trace.trace(Trace.INFO, "Running maven goal \'" + goal + "\' with working directory: " + workingDir.toOSString());
            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance((IContainer) null, workingDir.lastSegment());

            workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, workingDir.toOSString());
            workingCopy.setAttribute("M2_GOALS", goal);

            if (profiles != null && profiles.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < profiles.size(); i++) {
                    if (i > 0)
                        sb.append(',');
                    sb.append(profiles.get(i));
                }
                workingCopy.setAttribute("M2_PROFILES", sb.toString());
            }

            ILaunchConfiguration launchConfig = workingCopy.doSave();

            ILaunch launch = launchConfig.launch("run", monitor);
            Trace.trace(Trace.INFO, "Running maven goal \'" + goal + "\' with working directory: " + workingDir.toOSString());
            IProcess[] processes = launch.getProcesses();
            List<IStreamMonitor> streamMonitors = new ArrayList<IStreamMonitor>();
            IStreamListener streamListener = new IStreamListener() {
                @Override
                public void streamAppended(String text, IStreamMonitor monitor) {
                    Trace.trace(Trace.INFO, workingDir.lastSegment() + ": " + text);
                }
            };
            for (IProcess p : processes) {
                IStreamMonitor outStream = p.getStreamsProxy().getOutputStreamMonitor();
                outStream.addListener(streamListener);
                streamMonitors.add(outStream);
                IStreamMonitor errorStream = p.getStreamsProxy().getErrorStreamMonitor();
                errorStream.addListener(streamListener);
                streamMonitors.add(errorStream);
            }
            while (!launch.isTerminated() && !monitor.isCanceled()) {
                Thread.sleep(500);
            }
            for (IStreamMonitor m : streamMonitors) {
                try {
                    m.removeListener(streamListener);
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            isSuccessful = false;
            Trace.logError("The maven goal \'" + goal + "\' failed to execute using working directory: " + workingDir.toOSString(), e);
        }
        return isSuccessful;
    }

    /**
     * Returns the set of tracked projects.
     *
     * @return the tracked projects
     */
    public static Set<String> getTrackedProjects() {
        return LibertyMavenProjectMapping.getInstance().getMappedProjectSet();
    }

    /**
     * Given a server, returns the project that controls it or {@code null} if it is not controlled by any project.
     *
     * @param server {@code IServer} to look for in project mapping
     * @return the controlling project
     */
    public static IProject getMappedProject(IServer server) {
        if (server == null) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "The server argument is null.");
            }
            return null;
        }
        return LibertyMavenProjectMapping.getInstance().getMappedProject(server);
    }

    /**
     * Given a project, returns the associated server or {@code null} if it is not associated with any server.
     *
     * @param projectName the name of the project
     * @return the associated server
     */
    public static IServer getMappedServer(String projectName) {
        LibertyMavenProjectMapping.ProjectMapping mapping = LibertyMavenProjectMapping.getInstance().getMapping(projectName);
        if (mapping == null || mapping.getServerID() == null)
            return null;
        return ServerCore.findServer(mapping.getServerID());
    }

    private MavenProjectInspector getProjectInspector(IProject project, IProgressMonitor monitor) {
        return new MavenProjectInspector(project);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedProject(IProject project, IProgressMonitor monitor) {
        return instance.getProjectInspector(project, monitor).isSupportedProject(monitor);
    }

    /** {@inheritDoc} */
    @Override
    public void updateSrcConfig(IPath location, LibertyBuildPluginConfiguration config, IProgressMonitor monitor) {
        String goal = "package liberty:install-apps";
        runMavenGoal(location, goal, config.getActiveBuildProfiles(), monitor);
    }

    /** {@inheritDoc} */
    @Override
    public LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project, IProgressMonitor monitor) {
        return getLibertyMavenProjectConfiguration(project, monitor);
    }

    /** {@inheritDoc} */
    @Override
    public String getRelativeBuildPluginConfigurationFileLocation() {
        return LibertyMavenConstants.LIBERTY_PLUGIN_CONFIG_PATH;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus preServerSetup(IProject project, LibertyBuildPluginConfiguration config, IPath serverPath, IProgressMonitor monitor) {
        /*
         * If the project has parent aggregator project then we need to call maven install to make sure all dependencies are
         * installed prior to server creation.
         */
        String parentId = config.getConfigValue(ConfigurationType.aggregatorParentId);
        String parentBaseDir = config.getConfigValue(ConfigurationType.aggregatorParentBasedir);

        List<String> profiles = config.getActiveBuildProfiles();
        if (parentId != null && parentBaseDir != null) {
            IPath parentPath = new Path(parentBaseDir);
            if (!parentPath.toFile().exists()) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The parent project could not be found " + parentId + " : " + parentBaseDir);
            }
            runMavenGoal(parentPath, "install", profiles, monitor);
        }

        // Check if the server directory exists in the user directory before attempting to create it
        if (!serverPath.toFile().exists()) {
            // if the path doesn't exist then run the maven goal to create the server files
            String goal = "package liberty:install-apps";
            Trace.trace(Trace.INFO, "Running " + goal + " goal on project: " + project.getName());
            runMavenGoal(project.getLocation(), goal, profiles, monitor);
        }
        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public String getServerType() {
        return Constants.SERVER_TYPE_LIBERTY_MAVEN;
    }

    /**
     * Checks whether the given project is a dependency of a liberty maven project.
     *
     * @param moduleProject the module to check
     * @param server a server that is mapped to a liberty maven project
     * @return
     */
    @Override
    public boolean isDependencyModule(IProject moduleProject, IServer server) {
        IProject project = LibertyMaven.getMappedProject(server);
        if (project != null) {
            try {
                MavenProjectInspector pi = new MavenProjectInspector(project);
                IModule[] modules = pi.getProjectModules();
                for (IModule m : modules) {
                    if (m.getProject().equals(moduleProject)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Trace.logError("Liberty Maven module lookup hit an error for project " + project.getName(), e);
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractLibertyProjectMapping getMappingHandler() {
        return LibertyMavenProjectMapping.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public IProjectInspector getProjectInspector(IProject project) {
        return new MavenProjectInspector(project);
    }

    /** {@inheritDoc} */
    @Override
    public void triggerAddRuntimeAndServer(IProject project, IProgressMonitor monitor) {

        LibertyManager.getInstance().triggerAddProject(project, monitor);

    }

}
