/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.LibertyRuntimeProvider;
import com.ibm.ws.st.core.internal.URIUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereRuntimeLocator;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/**
 *
 */
@SuppressWarnings("restriction")
public abstract class AbstractLibertyRuntimeProvider extends LibertyRuntimeProvider {

    /**
     *
     */
    public AbstractLibertyRuntimeProvider() {
        // Empty
    }

    /**
     * {@inheritDoc}
     *
     * If the server.xml file is located ANYWHERE within a supported Liberty Build Plugin project, then 'map' it to the one
     * that tools knows about. eg. in the build or target folder.
     */

    @Override
    public URI getTargetConfigFileLocation(URI uri, WebSphereServerInfo server) {
        if (uri != null) {
            IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(uri);
            if (containers != null && containers.length == 1) { // Should only be the one
                IContainer container = containers[0];
                IProject project = container.getProject();
                LibertyBuildPluginConfiguration config = getBuildPluginConfiguration(project);
                if (config == null) {
                    return null;
                }

                WebSphereRuntime webSphereRuntime = getWSRuntime(project, config);
                // It if is the same runtime, then the server is created (not a Ghost Runtime) so we found the correct server info
                if (webSphereRuntime != server.getWebSphereRuntime()) {
                    return null;
                }

                String serverOutputDirectoryValue = config.getConfigValue(ConfigurationType.serverOutputDirectory);
                try {
                    IPath path = new Path(serverOutputDirectoryValue);
                    IPath targetServerConfig = getTargetServerConfig(path);
                    IResource findMember = convertConfigValueToIResource(targetServerConfig.toFile().toURI(), project);
                    if (findMember != null && findMember.exists()) {
                        return findMember.getLocationURI();
                    }
                    // If not a workspace resource, then return the external URI as is.
                    // Might be using the installDir option where it points to any pre-installed Liberty
                    File externalFile = targetServerConfig.toFile();
                    return externalFile.toURI();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public WebSphereServerInfo getWebSphereServerInfo(URI uri) {
        try {
            if (uri != null) {
                IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(uri);
                if (containers != null && containers.length == 1) { // Should only be the one
                    IContainer container = containers[0];
                    final IProject project = container.getProject();
                    if (!project.isAccessible()) {
                        return null;
                    }

                    final LibertyBuildPluginConfiguration config = getBuildPluginConfiguration(project);
                    if (config == null) {
                        return null;
                    }

                    final UserDirectory userDirectory = getUserDirectory(project, config);
                    if (userDirectory != null) {
                        String serverName = config.getConfigValue(ConfigurationType.serverName);
                        userDirectory.getWebSphereRuntime();
                        final ConfigurationFile cfgFile = new ConfigurationFile(uri, userDirectory);

                        WebSphereServerInfo info = new WebSphereServerInfo(serverName, userDirectory, userDirectory.getWebSphereRuntime()) {

                            /**
                             * {@inheritDoc}
                             *
                             * Override the location of the bootstrap file, if applicable, otherwise return the original from the target/build folder
                             */
                            @Override
                            protected File getBootstrapFile() {
                                File bootstrapFileFromServer = super.getBootstrapFile();
                                String value = config.getConfigValue(ConfigurationType.bootstrapPropertiesFile);
                                if (value != null) {
                                    File bootstrapFile = new File(value);
                                    if (bootstrapFile.exists()) {
                                        return bootstrapFile;
                                    }
                                }
                                return bootstrapFileFromServer;
                            }

                            /**
                             * {@inheritDoc}
                             *
                             * Override the location of the server.env file, if applicable, otherwise return the original from the target/build folder
                             */

                            @Override
                            protected File getServerEnvFile() {
                                File serverEnvFileFromServer = super.getServerEnvFile();
                                String value = config.getConfigValue(ConfigurationType.serverEnvFile);
                                if (value != null) {
                                    File serverEnvFile = new File(value);
                                    if (serverEnvFile.exists()) {
                                        return serverEnvFile;
                                    }
                                }
                                return serverEnvFileFromServer;
                            }

                            @Override
                            /**
                             * {@inheritDoc}
                             *
                             * For a ghost runtime - no server
                             */
                            public ConfigurationFile getConfigRoot() {
                                return cfgFile;
                            }
                        };
                        return info;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Location of a configuration directory that contains files copied to the server configuration directory.
     * Any files in this directory take precedence over specified files of the same type. This attribute is useful for copying included configuration files
     * or a set of configuration files.
     */
    @Override
    public IFolder getConfigFolder(IResource resource) {
        IProject project = resource.getProject();
        LibertyBuildPluginConfiguration config = getBuildPluginConfiguration(project);
        if (config == null) {
            return null;
        }
        String configDirectory = config.getConfigValue(ConfigurationType.configDirectory);
        if (configDirectory != null) {
            try {
                IPath configDirPath = new Path(configDirectory);
                IResource configDir = convertConfigValueToIResource(configDirPath.toFile().toURI(), project);
                if (configDir != null && configDir.getType() == IResource.FOLDER) {
                    return (IFolder) configDir;
                }
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "The project is a Gradle or Maven Project with an existing build plugin file, but could not determine the config directory.");
                }
            }
        }

        return null;
    }

    protected abstract ILibertyBuildPluginImpl getBuildPlugin();

    protected abstract IServer getMappedServer(String projectName);

    protected LibertyBuildPluginConfiguration getBuildPluginConfiguration(IProject project) {
        ILibertyBuildPluginImpl buildPluginImpl = getBuildPlugin();
        if (!buildPluginImpl.isSupportedProject(project, null)) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO,
                            "The project is a Liberty Build Plugin project, but it does not match the criteria, or, the project is not applicable.  Some features of Liberty Tools such as quick fixes are disabled");
            }
            return null;
        }
        LibertyBuildPluginConfiguration config = buildPluginImpl.getLibertyBuildPluginConfiguration(project, new NullProgressMonitor());
        if (config == null) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO,
                            "The Project is a Liberty Build Project but does not have the liberty-plugin-config.xml file.  Some features of Liberty Tools such as quick fixes are disabled");
            }
            return null;
        }
        return config;
    }

    /*
     * @param project - The Project containing the Liberty Runtime
     *
     * @return UserDirectory
     */
    private UserDirectory getUserDirectory(IProject project, LibertyBuildPluginConfiguration config) {
        // Find the install dir of the runtime
        String installDir = config.getConfigValue(ConfigurationType.installDirectory);

        WebSphereRuntime wsRuntime = getWSRuntime(installDir);

        String userDirVal = config.getConfigValue(ConfigurationType.userDirectory);
        if (userDirVal != null && wsRuntime != null) {
            IPath userPath = new Path(userDirVal);
            return new UserDirectory(wsRuntime, userPath, project);
        }
        return null;
    }

    private WebSphereRuntime getWSRuntime(IProject project, LibertyBuildPluginConfiguration config) {
        // Find the install dir of the runtime
        String installDir = config.getConfigValue(ConfigurationType.installDirectory);
        if (installDir == null) {
            return null;
        }
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
            if (runtimeFromDir != null) {
                wsRuntime = (WebSphereRuntime) runtimeFromDir.loadAdapter(WebSphereRuntime.class, null);
            }
        }
        return wsRuntime;
    }

    /**
     * Returns the path to the 'actual' server.xml in the target or build folder, where the embedded runtime is
     *
     * @param serverOutputDirectory
     * @return
     */
    private IPath getTargetServerConfig(IPath serverOutputDirectory) {
        return serverOutputDirectory.append("server.xml");
    }

    private IResource convertConfigValueToIResource(URI pathURI, IProject project) {
        try {
            URI relativeURI = URIUtil.canonicalRelativize(project.getLocationURI(), pathURI); // Get the path relative to the project folder
            return project.findMember(relativeURI.getPath());
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Can't find the workspace resource for: " + pathURI.toString());
            }
        }
        return null;
    }
}
