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

package com.ibm.ws.st.liberty.gradle.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Activator;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.IProjectInspector;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;
import com.ibm.ws.st.liberty.gradle.manager.internal.GradleProjectInspector;
import com.ibm.ws.st.liberty.gradle.manager.internal.LibertyGradleProjectMapping;
import com.ibm.ws.st.liberty.gradle.manager.internal.LibertyManager;

@SuppressWarnings("restriction")
public class LibertyGradle implements ILibertyBuildPluginImpl {

    private static LibertyGradle instance = new LibertyGradle();

    private LibertyGradle() {
        // singleton
    }

    public static ILibertyBuildPluginImpl getInstance() {
        return instance;
    }

    public static boolean isGradleProject(IProject project, IProgressMonitor monitor) {
        return instance.getProjectInspector(project, monitor).isGradleProject();
    }

    /**
     * Retrieves the liberty Gradle configuration model for a given project.
     *
     * @param project a Gradle-enabled project that is expected to be configured with the correct version/configuration of the liberty-gradle-plugin in its POM
     * @param monitor
     * @return the {@code LibertyBuildPluginConfiguration} model or null if the model could not be generated
     */
    public static LibertyBuildPluginConfiguration getLibertyGradleProjectConfiguration(IProject project, IProgressMonitor monitor) {
        return instance.getProjectInspector(project, monitor).getBuildPluginConfiguration(monitor);
    }

    /**
     * Run a set of Gradle tasks
     * @param workingDir The base Gradle project directory
     * @param tasks The tasks to run
     * @param args The arguments for the tasks
     * @param monitor A progress monitor that can be used to cancel the tasks
     * @return True if the tasks ran successfully, false otherwise
     */
    public static boolean runGradleTask(final IPath workingDir, String[] tasks, String[] args, IProgressMonitor monitor) {
        boolean isSuccessful = true;

        // Add the arguments to skip the tests
        List<String> arguments = args != null ? Arrays.asList(args) : new ArrayList<String>();
        addArgs(arguments, LibertyGradleConstants.SKIP_TESTS_ARGS);
        addArgs(arguments, LibertyGradleConstants.SKIP_LIBERTY_PKG_ARGS);
        
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Running gradle tasks: " + Arrays.toString(tasks) + ", with arguments: " + arguments);
        }

        ProjectConnection connection = null;
        try {
            final Boolean[] result = new Boolean[1];
            CancellationTokenSource cancelTokenSrc = GradleConnector.newCancellationTokenSource();
            
            GradleConnector connector = GradleConnector.newConnector();
            String override = System.getProperty("GRADLE_VERSION_OVERRIDE");
            if (override != null) {
                connector.useGradleVersion(override);
            }
            connector = connector.forProjectDirectory(new File(workingDir.toOSString()));
            connection = connector.connect();

            BuildLauncher build = connection.newBuild();
            build.forTasks(tasks);
            build.withArguments(arguments.toArray(new String[arguments.size()]));
            build.withCancellationToken(cancelTokenSrc.token());
            ResultHandler<Void> handler = new ResultHandler<Void>() {
                @Override
                public void onComplete(Void arg0) {
                    result[0] = Boolean.TRUE;
                }

                @Override
                public void onFailure(GradleConnectionException e) {
                    result[0] = Boolean.FALSE;
                    Trace.logError("The gradle command failed for tasks: " + Arrays.toString(tasks) + ", and arguments: " + arguments, e);
                }
            };
            build.run(handler);
            
            while (result[0] == null && !monitor.isCanceled()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            if (monitor.isCanceled()) {
                cancelTokenSrc.cancel();
                return false;
            }

            if (result[0] != null) {
                isSuccessful = result[0].booleanValue();
            }
        } catch (Exception e) {
            isSuccessful = false;
            Trace.logError("The gradle command failed for tasks: " + Arrays.toString(tasks) + ", and arguments: " + arguments, e);
        } finally {
            if (connection != null) {
            connection.close();
            }
        }

        return isSuccessful;
    }
    
    private static void addArgs(List<String> argumentList, String... argsToAdd) {
    	for (String arg : argsToAdd) {
    		argumentList.add(arg);
    	}
    }

    /**
     * Returns the set of tracked projects.
     *
     * @return the tracked projects
     */
    public static Set<String> getTrackedProjects() {
        return LibertyGradleProjectMapping.getInstance().getMappedProjectSet();
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
        return LibertyGradleProjectMapping.getInstance().getMappedProject(server);
    }

    /**
     * Given a project, returns the associated server or {@code null} if it is not associated with any server.
     *
     * @param projectName the name of the project
     * @return the associated server
     */
    public static IServer getMappedServer(String projectName) {
        LibertyGradleProjectMapping.ProjectMapping mapping = LibertyGradleProjectMapping.getInstance().getMapping(projectName);
        if (mapping == null || mapping.getServerID() == null)
            return null;
        return ServerCore.findServer(mapping.getServerID());
    }

    private GradleProjectInspector getProjectInspector(IProject project, IProgressMonitor monitor) {
           return new GradleProjectInspector(project);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedProject(IProject project, IProgressMonitor monitor) {
        return instance.getProjectInspector(project, monitor).isSupportedProject(monitor);
    }

    /** {@inheritDoc} */
    @Override
    public void updateSrcConfig(IProject project, LibertyBuildPluginConfiguration config, IProgressMonitor monitor) {
        String[] tasks = new String[] {LibertyGradleConstants.LIBERTY_CREATE_TASK};
        IPath location = project.getLocation();
        runGradleTask(location, tasks, null, monitor);
    }

    /** {@inheritDoc} */
    @Override
    public LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project, IProgressMonitor monitor) {
        return getLibertyGradleProjectConfiguration(project, monitor);
    }

    /** {@inheritDoc} */
    @Override
    public String getRelativeBuildPluginConfigurationFileLocation() {
        return LibertyGradleConstants.LIBERTY_PLUGIN_CONFIG_PATH;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus preServerSetup(IProject project, LibertyBuildPluginConfiguration config, IPath serverPath, IProgressMonitor monitor) {
        /*
         * If the project has parent aggregator project then we need to call gradle install to make sure all dependencies are
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
            runGradleTask(parentPath, LibertyGradleConstants.ASSEMBLE_INSTALL_APPS_TASKS, null, monitor);
        }

        // Check if the server directory exists in the user directory before attempting to create it
        if (!serverPath.toFile().exists()) {
            // if the path doesn't exist then run the gradle tasks to create the server files
            runGradleTask(project.getLocation(), LibertyGradleConstants.ASSEMBLE_INSTALL_APPS_TASKS, null, monitor);
        }
        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public String getServerType() {
        return Constants.SERVER_TYPE_LIBERTY_GRADLE;
    }

    /**
     * Checks whether the given project is a dependency of a liberty gradle project.
     *
     * @param moduleProject the module to check
     * @param server a server that is mapped to a liberty gradle project
     * @return
     */
    @Override
    public boolean isDependencyModule(IProject moduleProject, IServer server) {
        IProject project = LibertyGradle.getMappedProject(server);
        if (project != null) {
            try {
                GradleProjectInspector pi = new GradleProjectInspector(project);
                IModule[] modules = pi.getProjectModules();
                for (IModule m : modules) {
                    if (m.getProject().equals(moduleProject)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Trace.logError("Liberty Gradle module lookup hit an error for project " + project.getName(), e);
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractLibertyProjectMapping getMappingHandler() {
        return LibertyGradleProjectMapping.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public IProjectInspector getProjectInspector(IProject project) {
       return new GradleProjectInspector(project);
    }

    /** {@inheritDoc} */
    @Override
    public void triggerAddRuntimeAndServer(IProject project, IProgressMonitor monitor) {

        LibertyManager.getInstance().triggerAddProject(project, monitor);

    }

}
