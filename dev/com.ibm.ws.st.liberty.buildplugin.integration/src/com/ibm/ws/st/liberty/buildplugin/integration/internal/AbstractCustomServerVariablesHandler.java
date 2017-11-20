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

package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.Bootstrap;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ServerEnv;
import com.ibm.ws.st.core.internal.config.validation.ICustomServerVariablesHandler;

@SuppressWarnings("restriction")
public abstract class AbstractCustomServerVariablesHandler implements ICustomServerVariablesHandler {

    /**
     * Get the project that defines the given server in its liberty build plugin.
     *
     * @param server
     * @return
     */
    protected abstract IProject getMappedProject(IServer server);

    /**
     * Get the liberty build plugin configuration for the given project.
     *
     * @param project
     * @return
     */
    protected abstract LibertyBuildPluginConfiguration getLibertyBuildPluginConfiguration(IProject project);

    /**
     * Inline variables are defined in the configuration of a liberty build plugin instead of the bootstrap.properties, server.env or jvm.options files that the runtime
     * traditionally uses.
     *
     * @param project
     * @param configVars
     * @param libertyBuildProjectConfiguration
     */
    protected abstract void addInlineVars(IProject project, ConfigVars configVars, LibertyBuildPluginConfiguration libertyBuildProjectConfiguration);

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCustomServerVariables(ConfigVars configVars, IProject project) {

        LibertyBuildPluginConfiguration libertyBuildProjectConfiguration = getLibertyBuildPluginConfiguration(project);

        if (libertyBuildProjectConfiguration != null) {

            // Load variables from server.env file (when applicable)
            String serverEnvFile = libertyBuildProjectConfiguration.getConfigValue(ConfigurationType.serverEnv);
            if (serverEnvFile != null) {
                try {
                    ServerEnv serverEnv = new ServerEnv(new File(serverEnvFile), null);
                    serverEnv.getVariables(configVars);
                } catch (IOException e) {
                    Trace.logError("Could not read server.env file " + serverEnvFile, e);
                }
            }

            // Load variables from bootstrap.properties file (when applicable)
            // Overlapping variables from server.env file will be overridden (bootstrap.properties variables have higher priority)
            String bootstrapFile = libertyBuildProjectConfiguration.getConfigValue(ConfigurationType.bootstrapPropertiesFile);
            if (bootstrapFile != null) {
                try {
                    Bootstrap bootstrap = new Bootstrap(new File(bootstrapFile), null);
                    bootstrap.getVariables(configVars);
                } catch (IOException e) {
                    Trace.logError("Could not read bootstrap.properties file " + bootstrapFile, e);
                }
            }

            addInlineVars(project, configVars, libertyBuildProjectConfiguration);
        }
    }

    @Override
    public void addCustomServerVariables(ConfigVars configVars, WebSphereServerInfo serverInfo) {
        if (serverInfo == null)
            return;
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);
        if (wsServer == null)
            return;
        IProject project = getMappedProject(wsServer.getServer());
        if (project == null)
            return;

        addCustomServerVariables(configVars, project);
    }

}
