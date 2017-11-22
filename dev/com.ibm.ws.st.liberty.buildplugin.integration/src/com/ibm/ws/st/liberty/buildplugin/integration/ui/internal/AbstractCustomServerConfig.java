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

package com.ibm.ws.st.liberty.buildplugin.integration.ui.internal;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.Bootstrap;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.internal.config.JVMOptions;
import com.ibm.ws.st.core.internal.config.ServerEnv;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;
import com.ibm.ws.st.ui.internal.CustomServerConfigTreeNode;
import com.ibm.ws.st.ui.internal.DomXmlDocumentFileCache;
import com.ibm.ws.st.ui.internal.custom.ICustomServerConfig;
import com.ibm.ws.st.ui.internal.utility.PathUtil;

@SuppressWarnings("restriction")
public abstract class AbstractCustomServerConfig implements ICustomServerConfig, ILibertyBuildPluginImplProvider {

    @Override
    public List<Object> getCustomServerElements(IServer server) {
        List<Object> serverConfigElements = new ArrayList<Object>();

        WebSphereServer webSphereServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        WebSphereServerInfo webSphereServerInfo = webSphereServer.getServerInfo();

        // If WebSphereServerInfo is null then there is likely a problem with the server (eg. vital files deleted) and
        // we can just return an empty list.
        if (webSphereServerInfo == null)
            return serverConfigElements;

        IProject mappedProject = getBuildPluginImpl().getMappingHandler().getMappedProject(server);
        if (mappedProject != null) {
            LibertyBuildPluginConfiguration libertyBuildPluginProjectConfiguration = getBuildPluginImpl().getLibertyBuildPluginConfiguration(mappedProject,
                                                                                                                                             new NullProgressMonitor());
            if (libertyBuildPluginProjectConfiguration != null) {
                String configValue = libertyBuildPluginProjectConfiguration.getConfigValue(ConfigurationType.configFile);
                if (configValue != null) {
                    try {

                        // server.xml in source
                        URI fileURI = PathUtil.getURIForFilePath(configValue);
                        if (fileURI != null) {
                            ConfigurationFile configurationFile = new ConfigurationFile(fileURI, webSphereServer.getUserDirectory(), webSphereServerInfo);
                            Document document = DomXmlDocumentFileCache.getInstance().getDocument(configurationFile);
                            if (document != null) {
                                serverConfigElements.add(document.getDocumentElement());
                            }
                        }

                        // bootstrap.properties in source
                        String bootstrapPropertiesFile = libertyBuildPluginProjectConfiguration.getConfigValue(ConfigurationType.bootstrapPropertiesFile);
                        if (bootstrapPropertiesFile != null) {
                            URI bootstrapFileURI = PathUtil.getURIForFilePath(bootstrapPropertiesFile);
                            if (bootstrapFileURI != null) {
                                Bootstrap bootstrap = new Bootstrap(new File(bootstrapPropertiesFile), PathUtil.getBestIFileMatchForURI(bootstrapFileURI));
                                serverConfigElements.add(bootstrap);
                            }
                        }

                        // server.env in source
                        String serverEnvFile = libertyBuildPluginProjectConfiguration.getConfigValue(ConfigurationType.serverEnv);
                        if (serverEnvFile != null) {
                            URI serverEnvFileURI = PathUtil.getURIForFilePath(serverEnvFile);
                            if (serverEnvFileURI != null) {
                                ServerEnv serverEnv = new ServerEnv(new File(serverEnvFile), PathUtil.getBestIFileMatchForURI(serverEnvFileURI));
                                serverConfigElements.add(serverEnv);
                            }
                        }

                        // jvm.options in source
                        String jvmOptionsFile = libertyBuildPluginProjectConfiguration.getConfigValue(ConfigurationType.jvmOptionsFile);
                        if (jvmOptionsFile != null) {
                            URI jvmOptionsFileURI = PathUtil.getURIForFilePath(jvmOptionsFile);
                            if (jvmOptionsFileURI != null) {
                                JVMOptions jvmOptions = new JVMOptions(new File(jvmOptionsFile), PathUtil.getBestIFileMatchForURI(jvmOptionsFileURI));
                                serverConfigElements.add(jvmOptions);
                            }
                        }

                        // build plugin folder
                        List<Object> children = new ArrayList<Object>();
                        CustomServerConfigTreeNode customServerConfigTreeNode = new CustomServerConfigTreeNode(getCustomConfigurationNodeLabel(), getCustomConfigurationNodeImage(), children);
                        serverConfigElements.add(customServerConfigTreeNode);

                        // server.xml in target
                        ConfigurationFile configFile = webSphereServer.getConfiguration();
                        if (configFile != null) {
                            Document targetServerConfig = DomXmlDocumentFileCache.getInstance().getDocument(configFile);
                            children.add(targetServerConfig.getDocumentElement());
                        }

                        // bootstrap.properties in target
                        ExtendedConfigFile targetBootstrapPropertiesFile = webSphereServerInfo.getBootstrap();
                        if (targetBootstrapPropertiesFile != null) {
                            children.add(targetBootstrapPropertiesFile);
                        }

                        // server.env in target
                        ExtendedConfigFile targetServerEnvFile = webSphereServerInfo.getServerEnv();
                        if (targetServerEnvFile != null) {
                            children.add(targetServerEnvFile);
                        }

                        // jvm.options in target
                        // only the jvm.options under the server config folder should be shown at the root level
                        ExtendedConfigFile jvmOptions = webSphereServerInfo.getJVMOptions(webSphereServerInfo.getServerPath());
                        if (jvmOptions != null)
                            children.add(jvmOptions);

                        // config dropins in target
                        ConfigurationFolder dropinsFolder = webSphereServerInfo.getConfigurationDropinsFolder();
                        if (dropinsFolder != null)
                            children.add(dropinsFolder);

                    } catch (Exception exception) {
                        Trace.logError("Error encountered while attempting to create build plugin objects in servers view", exception);
                    }
                }
            }
        }
        return serverConfigElements;
    }

    protected abstract Image getCustomConfigurationNodeImage();

    protected abstract String getCustomConfigurationNodeLabel();

}
