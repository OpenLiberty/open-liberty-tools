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

package com.ibm.etools.maven.liberty.integration.manager.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMavenConstants.ProjectType;
import com.ibm.etools.maven.liberty.integration.internal.Trace;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.IProjectInspector;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.LibertyBuildPluginXMLConfigurationReader;

public class MavenProjectInspector implements IProjectInspector {

    private final IProject project;
    private LibertyBuildPluginConfiguration projectConfig;
    private LibertyBuildPluginConfiguration cachedProjectConfig;

    public MavenProjectInspector(IProject project) {
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
        IPath cachePath = LibertyMavenProjectMapping.getInstance().getLibertyBuildProjectCachePath(project.getName());
        if (cachePath == null)
            return null;
        return cachePath.toFile();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedProject(IProgressMonitor monitor) {
        try {
            // Check if it's a maven project first
            if (!isMavenProject())
                return false;

            IFile pom = project.getFile(LibertyMavenConstants.POM_FILE_NAME);

            Model model = getModelObject(pom);
            if (model == null)
                return false;

            // Only supported versions of the liberty-maven-plugin will generate the liberty-plugin-config.xml so we
            // only need to check for the existence of that file to determine whether the liberty maven plugin version is supported.
            File configFile = getLibertyBuildPluginConfigFile(monitor);

            if (configFile == null)
                return false;

            return configFile.exists();
        } catch (Exception e) {
            Trace.trace(Trace.INFO, "Error encountered while checking whether project " + project.getName() + " is a Liberty Maven enhanced project", e);
        }

        return false;
    }

    public boolean isMavenProject() {
        IFile pom = project.getFile(LibertyMavenConstants.POM_FILE_NAME);
        if (pom == null || !pom.exists()) {
            Trace.trace(Trace.INFO, "The project " + project.getName() + " is not a Maven Project");
            return false;
        }
        return true;
    }

    private Model getModelObject(IFile pomFile) {
        try {
            if (pomFile == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "A pom file was not specified before attempting to create the model");
                return null;
            }
            if (!pomFile.exists()) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "The pom file cannot be found: " + pomFile.getLocation().toOSString());
                return null;
            }
            MavenXpp3Reader reader = new MavenXpp3Reader();

            return reader.read(new InputStreamReader(new FileInputStream(pomFile.getLocation().toFile())));
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("The pom file could not be read", e);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public File getLibertyBuildPluginConfigFile(IProgressMonitor mon) throws CoreException {
        // In the majority of the cases the liberty-plugin-config.xml file will be in the target folder so check there first
        IFile f = project.getFile("target/" + LibertyMavenConstants.LIBERTY_PLUGIN_CONFIG_XML);
        if (f != null && f.exists())
            return f.getLocation().toFile();

        // If it wasn't in the default output location then try to read the maven model to retrieve the actual output location and check for the file
        MavenProject mavenProject = getMavenProject(mon);
        if (mavenProject == null)
            return null;

        Build build = mavenProject.getBuild();
        if (build == null)
            return null;

        String outputDirectory = build.getDirectory();
        if (outputDirectory == null)
            return null;

        IPath outputPath = new Path(outputDirectory);

        // This file is only generated when the project uses the liberty-maven-plugin dependency at version 3.0 and above
        IPath libertyPluginConfigPath = outputPath.append(LibertyMavenConstants.LIBERTY_PLUGIN_CONFIG_XML);

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

    private MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException {
        IFile pom = project.getFile(LibertyMavenConstants.POM_FILE_NAME);
        if (pom == null)
            return null;

        IPath location = pom.getLocation();
        if (location == null || !location.toFile().exists())
            return null;

        return MavenPlugin.getMaven().readProject(location.toFile(), monitor);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupportedModule(IModule module) {
        if (module == null) {
            Trace.trace(Trace.INFO, "The maven project " + project.getName() + " does not map to a supported module type.");
            return false;
        }

        String id = module.getModuleType().getId();
        if ("jst.web".equals(id) || "jst.ear".equals(id)) {
            return true;
        }

        Trace.trace(Trace.INFO,
                    "The maven project " + project.getName() + " is detected as a module but does not specify a supported packaging type (eg. war packaging type).");
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public IModule[] getProjectModules() {
        IModule[] modules = new IModule[0];

        LibertyMavenConstants.ProjectType projectType = getProjectType();
        try {
            switch (projectType) {
                case LIBERTY_ASSEMBLY:
                    modules = getLibertyAssemblyModules();
                    break;
                default:
                    IModule m = ServerUtil.getModule(project);
                    if (m != null)
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

    private IModule[] getLibertyAssemblyModules() {
        Trace.trace(Trace.INFO, "Looking up modules for project " + project.getName());
        LibertyBuildPluginConfiguration config = getBuildPluginConfiguration(null);
        Set<String> dependencies = config.getProjectCompileDependencies();
        List<IModule> modules = new ArrayList<IModule>();
        for (String dependency : dependencies) {
            String[] gav = dependency.split(":");
            if (gav.length == 3) {
                IMavenProjectFacade p = MavenPlugin.getMavenProjectRegistry().getMavenProject(gav[0], gav[1], gav[2]);
                if (p != null) {
                    IModule[] serverModules = ServerUtil.getModules(p.getProject());
                    if (serverModules != null) {
                        for (IModule m : serverModules)
                            modules.add(m);
                    }
                }
            }
        }
        return modules.toArray(new IModule[modules.size()]);
    }

    public ProjectType getProjectType() {
        LibertyBuildPluginConfiguration config = getBuildPluginConfiguration(null);
        String projectType = config.getConfigValue(ConfigurationType.projectType);
        if (LibertyMavenConstants.LIBERTY_ASSEMBLY_PROJECT_TYPE.equals(projectType))
            return ProjectType.LIBERTY_ASSEMBLY;

        return ProjectType.STANDARD;
    }

}
