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

package com.ibm.ws.st.liberty.gradle.manager.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.gradle.GradleScript;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.IProjectInspector;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.LibertyBuildPluginXMLConfigurationReader;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradleConstants;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradleConstants.ProjectType;
import com.ibm.ws.st.liberty.gradle.internal.Trace;

public class GradleProjectInspector implements IProjectInspector {

    private final IProject project;
    private LibertyBuildPluginConfiguration projectConfig;
    private LibertyBuildPluginConfiguration cachedProjectConfig;

    public GradleProjectInspector(IProject project) {
        this.project = project;
    }

    /** {@inheritDoc} */
    @Override
    public LibertyBuildPluginConfiguration getBuildPluginConfiguration(IProgressMonitor monitor) {
        try {
            File configFile = getLibertyBuildPluginConfigFile(monitor);
            if (configFile == null)
                return null;
            if (projectConfig == null)
                projectConfig = populateConfiguration(configFile, monitor);
        } catch (Exception e) {
            Trace.trace(Trace.INFO, "Error reading project configuration for project: " + project.getName(), e);
        }

        return projectConfig;
    }

    /** {@inheritDoc} */
    @Override
    public LibertyBuildPluginConfiguration getCachedBuildPluginConfiguration(IProgressMonitor monitor) {
        try {
            File configFile = getCachedLibertyBuildPluginConfigurationFile(monitor);
            if (configFile == null)
                return null;
            if (cachedProjectConfig == null)
                cachedProjectConfig = populateConfiguration(configFile, monitor);
        } catch (Exception e) {
            Trace.logError("Error reading project configuration for project: " + project.getName(), e);
        }

        return cachedProjectConfig;
    }

    /** {@inheritDoc} */
    @Override
    public File getCachedLibertyBuildPluginConfigurationFile(IProgressMonitor monitor) {
        IPath cachePath = LibertyGradleProjectMapping.getInstance().getLibertyBuildProjectCachePath(project.getName());
        if (cachePath == null)
            return null;
        return cachePath.toFile();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedProject(IProgressMonitor monitor) {
        try {
            // Check if it's a Gradle project first
            if (!isGradleProject())
                return false;

            // Only supported versions of the liberty-gradle-plugin will generate the liberty-plugin-config.xml so we
            // only need to check for the existence of that file to determine whether the liberty gradle plugin version is supported.
            File configFile = getLibertyBuildPluginConfigFile(monitor);

            if (configFile == null)
                return false;

            return configFile.exists();
        } catch (Exception e) {
            Trace.trace(Trace.INFO, "Error encountered while checking whether project " + project.getName() + " is a Liberty Gradle enhanced project", e);
        }

        return false;
    }

    public boolean isGradleProject() {
		try {
			return project.hasNature(LibertyGradleConstants.BUILDSHIP_GRADLE_PROJECT_NATURE);
		} catch (CoreException e) {
			Trace.trace(Trace.INFO, "Error getting the description for the project " + project.getName(), e);
		}
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public File getLibertyBuildPluginConfigFile(IProgressMonitor mon) throws CoreException {
        // In the majority of the cases the liberty-plugin-config.xml file will be in the Gradle build folder so check there first
    	
        // Note the Gradle build can be configurable.  The default value is 'build'.
        IFile f = project.getFile(LibertyGradleConstants.LIBERTY_PLUGIN_CONFIG_PATH);
        // Check the if the Gradle build has the file.
        
        if (!f.exists()) {
           	Trace.trace(Trace.INFO, "The gradle project " + project.getName() + " does not have the liberty plugin config path.  Liberty Tools will not recognize this as a Liberty Gradle project.");
        }
        
        if (f != null && f.exists())
            return f.getLocation().toFile();
        
        IFile buildGradleFile = project.getFile("build.gradle");
        IFile settingsGradleFile = project.getFile("settings.gradle"); // Groovy file
        
		// Bug fix: Gradle Connector taking a long time to connect on "invalid" projects.  The project
	    // has the Gradle nature but it has no Liberty Build Plugin file (except the one from Liberty Maven, but it is in a different
	    // location/path), and no build.gradle file and no settings.gradle file.   Need to short-cut these checks.
        if (!buildGradleFile.exists() && !settingsGradleFile.exists()) {
        	    // For the sample, both of these conditions are true, so we return to avoid invoking the Gradle connector.
        	    // If the build.gradle file does exist or the settings file exists, then proceed with establishing the Gradle connectior
        		return null;
        }

        // If we can't find it, we will have to inspect.
        // Check if it is a Gradle Project
        GradleProject gradleProject = getGradleProject(mon);
        if (gradleProject == null)
            return null;

        GradleScript build = gradleProject.getBuildScript();
        // TODO: Possible check of one criteria in determining whether or not the project is a Liberty Gradle project 
        if (build == null)
            return null;

        File outputDirectory = gradleProject.getBuildDirectory();
        // TODO: If the build folder doesn't exist, do we have to build it ourselves?
        if (outputDirectory == null)
            return null;

        IPath outputPath = new Path(outputDirectory.toString());

        // The liberty-gradle-plugin config file
        IPath libertyPluginConfigPath = outputPath.append(LibertyGradleConstants.LIBERTY_PLUGIN_CONFIG_XML);

        return libertyPluginConfigPath.toFile();
    }

    /** {@inheritDoc} */
    @Override
    public LibertyBuildPluginConfiguration populateConfiguration(File configFile, IProgressMonitor monitor) throws IOException {
        if (monitor != null && monitor.isCanceled() || configFile == null || !configFile.exists())
            return null;
        Trace.trace(Trace.INFO, "Reading configFile from" + configFile.getAbsolutePath());
        LibertyBuildPluginXMLConfigurationReader reader = new LibertyBuildPluginXMLConfigurationReader();
        return reader.load(configFile.toURI());
    }
    
	private GradleProject getGradleProject(IProgressMonitor monitor) {
		ProjectConnection connection = null;
		try {
			GradleConnector gradleConnector = GradleConnector.newConnector();
			String override = System.getProperty("GRADLE_VERSION_OVERRIDE");
			if (override != null) {
				gradleConnector.useGradleVersion(override);
			}
			// The workspace project could be deleted so getLocation is null.
			// If we can't get GradleProject for any other reason, then log the error.
			if (project.getLocation() != null) {
				connection = gradleConnector.forProjectDirectory(new File(project.getLocation().toString())).connect();
				ModelBuilder<GradleProject> model = connection.model(GradleProject.class);
				GradleProject gradleProject = model.get();
				return gradleProject;
			}
		} catch (Exception e) {
			Trace.logError("Could not get Gradle project for project: " + project.getName() + ", at location: "
					+ project.getLocation(), e);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}

		return null;
	}

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedModule(IModule module) {
        if (module == null) {
            Trace.trace(Trace.INFO, "The gradle project " + project.getName() + " does not map to a supported module type.");
            return false;
        }

        String id = module.getModuleType().getId();
        if ("jst.web".equals(id) || "jst.ear".equals(id)) {
            return true;
        }

        Trace.trace(Trace.INFO,
                    "The gradle project " + project.getName() + " is detected as a module but does not specify a supported packaging type (eg. war packaging type).");
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public IModule[] getProjectModules() {
        IModule[] modules = new IModule[0];
        try {
			IModule m = ServerUtil.getModule(project);
			if (m != null) {
				modules = new IModule[] { m };
			}
            StringBuffer moduleMsg = new StringBuffer("Found the following dependency modules for project " + project.getName() + ": {");
            for (int i = 0; i < modules.length; i++) {
                if (i > 0)
                    moduleMsg.append(", ");
                moduleMsg.append(modules[i].getName());
            }
            moduleMsg.append("}");
            Trace.trace(Trace.INFO, moduleMsg.toString());

        } catch (Exception e) {
            Trace.trace(Trace.WARNING,
                        "Unable to retrieve modules for project " + project.getName() + ".", e);
        }
        return modules;
    }

    public ProjectType getProjectType() {
        return ProjectType.STANDARD;
    }

}