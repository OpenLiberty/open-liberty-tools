/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2016, 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.ws.st.liberty.buildplugin.integration.servertype.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.IProjectInspector;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ExcludeSyncModuleUtil;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping.ProjectMapping;
import com.ibm.ws.st.core.internal.BaseLibertyBehaviourExtension;
import com.ibm.ws.st.core.internal.PublishUnit;
import com.ibm.ws.st.core.internal.ServerExtensionWrapper;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;

/**
 * Liberty Maven Server Behaviour Implementation
 */
@SuppressWarnings("restriction")
public abstract class AbstractLibertyBuildPluginServerBehaviour extends BaseLibertyBehaviourExtension implements ILibertyBuildPluginImplProvider {

    ILibertyBuildPluginImpl buildPluginHelper = getBuildPluginImpl();
    AbstractLibertyProjectMapping mappingHandler = getBuildPluginImpl().getMappingHandler();

    @Override
    public boolean shouldRunPublisherForServerType(ServerBehaviourDelegate behaviour, Object publishUnit, Object publisher) {
        if (behaviour != null && publisher != null && publisher instanceof ServerExtensionWrapper) {

            if (behaviour instanceof WebSphereServerBehaviour) {
                WebSphereServerBehaviour wsBehaviour = (WebSphereServerBehaviour) behaviour;
                WebSphereServer wsServer = wsBehaviour.getWebSphereServer();

                // Always override the publisher when the module being published
                // is the project used to create the Liberty Maven server
                if (publishUnit != null && publishUnit instanceof PublishUnit) {
                    PublishUnit pu = (PublishUnit) publishUnit;

                    IModule module = pu.getModule()[0];

                    IProject moduleProject = module.getProject();

                    if (moduleProject != null) {

                        ProjectMapping mapping = mappingHandler.getMapping(moduleProject.getName());
                        boolean isModulePublishedByMavenServer = mapping != null && mapping.getServerID().equals(wsServer.getServer().getId());
                        if (!isModulePublishedByMavenServer) {
                            /*
                             * Check whether the server is mapped to a Maven project, if it is then this module might
                             * be a dependency module of that mapped project
                             */
                            if (buildPluginHelper.isDependencyModule(moduleProject, wsServer.getServer()))
                                isModulePublishedByMavenServer = true;
                        }
                        ServerExtensionWrapper serverExtWrapper = (ServerExtensionWrapper) publisher;

                        // Confirm that the module being published is the server
                        // The mapping can be null if the user publishes another module while the server is in
                        // non-loose config mode
                        if (isModulePublishedByMavenServer) {
                            // Invalid publishers excluded

                            if ((serverExtWrapper.getPublishDelegate() instanceof AbstractLibertyBuildPluginJEEPublisher)) {
                                return true;
                            }
                        } else {
                            // If the user publishes another module to the server, the default
                            // Liberty publishers should be used
                            if (!(serverExtWrapper.getPublishDelegate() instanceof AbstractLibertyBuildPluginJEEPublisher)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Boolean isLooseConfigEnabled(ServerBehaviourDelegate behaviour) {
        // loose configuration settings are configured in the pom.xml for Liberty build plugin servers
        return new Boolean(false);
    }

    @Override
    public void initialize(ServerBehaviourDelegate behaviour, IProgressMonitor monitor) {
        if (behaviour != null) {
            if (behaviour instanceof WebSphereServerBehaviour) {
                WebSphereServerBehaviour wsBehaviour = (WebSphereServerBehaviour) behaviour;
                WebSphereServer wsServer = wsBehaviour.getWebSphereServer();
                if (wsServer != null) {
                    IProject proj = mappingHandler.getMappedProject(wsServer.getServer());
                    if (proj != null) {
                        // Get modules from maven project
                        IProjectInspector pi = getBuildPluginImpl().getProjectInspector(proj);
                        IModule[] projectModules = pi.getProjectModules();

                        if (projectModules != null && projectModules.length != 0) {
                            LibertyBuildPluginConfiguration config = buildPluginHelper.getLibertyBuildPluginConfiguration(proj, monitor);
                            if (config != null) {
                                ExcludeSyncModuleUtil.updateExcludeSyncModuleMapping(projectModules, config, wsBehaviour);
                            }

                        }
                    }
                }
            }
        }
    }

}