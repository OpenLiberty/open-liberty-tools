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

package com.ibm.etools.maven.liberty.integration.servertype.internal;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.etools.maven.liberty.integration.internal.Activator;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.etools.maven.liberty.integration.internal.Trace;
import com.ibm.etools.maven.liberty.integration.manager.internal.LibertyMavenProjectMapping;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.PublishUnit;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ConfigurationType;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.LibertyBuildPluginConfiguration;
import com.ibm.ws.st.liberty.buildplugin.integration.manager.internal.AbstractLibertyProjectMapping.ProjectMapping;
import com.ibm.ws.st.liberty.buildplugin.integration.servertype.internal.AbstractLibertyBuildPluginJEEPublisher;

/*
 * Liberty Maven publishing implementation
 */
@SuppressWarnings("restriction")
public class LibertyMavenJEEPublisher extends AbstractLibertyBuildPluginJEEPublisher {

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

    @Override
    public void postPublishApplication(int kind, PublishUnit app, MultiStatus status, IProgressMonitor monitor) {
        WebSphereServer wsServer = getWebSphereServer();

        if (wsServer != null) {
            String type = wsServer.getServerType();
            if (Constants.SERVER_TYPE_LIBERTY_MAVEN.equals(type)) {
                super.postPublishApplication(kind, app, status, monitor);
            }
        }
    }

    @Override
    public void publishModuleAndChildren(int kind, PublishUnit unit, MultiStatus mStatus, IProgressMonitor monitor) {
        WebSphereServer wsServer = getWebSphereServer();

        if (wsServer != null) {
            String type = wsServer.getServerType();
            if (Constants.SERVER_TYPE_LIBERTY_MAVEN.equals(type)) {
                if (unit != null) {
                    IModule module = unit.getModule()[0];
                    IProject moduleProject = module.getProject();

                    ProjectMapping mapping = LibertyMavenProjectMapping.getInstance().getMapping(moduleProject.getName());

                    if (mapping != null && mapping.getServerID().equals(wsServer.getServer().getId())) {
                        LibertyBuildPluginConfiguration config = LibertyMaven.getLibertyMavenProjectConfiguration(moduleProject, monitor);

                        // For a project with a parent in non-loose config mode, do not run the publish on each child, otherwise the
                        // maven goal will be called multiple times. Only call once on the parent pom
                        String looseConfigValue = config.getConfigValue(ConfigurationType.looseApplication);
                        boolean isLC = Boolean.parseBoolean(looseConfigValue);
                        int publishUnitKind = unit.getDeltaKind();
                        String parentId = config.getConfigValue(ConfigurationType.aggregatorParentId);
                        String parentPath = config.getConfigValue(ConfigurationType.aggregatorParentBasedir);
                        if (!isLC && parentId != null && parentPath != null
                            && (ServerBehaviourDelegate.ADDED == publishUnitKind || ServerBehaviourDelegate.CHANGED == publishUnitKind)) {

                            publishOnParent(wsServer, moduleProject, config, "package",
                                            kind, unit, mStatus, monitor);

                            String pathToPublishedModule = getPathToPublishedModule(config);
                            if (!pathToPublishedModule.equals("")) {
                                getChangedResourceList().add(pathToPublishedModule);
                                try {
                                    notifyUpdatedApplicationResourcesViaJMX();
                                } catch (Exception e) {
                                    Trace.logError("Failed to notify app update via jmx", e);
                                }
                            }

                            // To avoid calling publishModulesAndChildren, exit publishModuleAndChildren
                            return;
                        }
                    }
                    super.publishModuleAndChildren(kind, unit, mStatus, monitor);
                }
            }
        }
    }

    /**
     * Attempt to publish on parent.
     *
     * @param wsServer
     * @param moduleProject
     * @param config
     * @param mvnCommand
     * @param kind
     * @param unit
     * @param mStatus
     * @param monitor
     */
    protected void publishOnParent(WebSphereServer wsServer, IProject moduleProject, LibertyBuildPluginConfiguration config,
                                   String mvnCommand, int kind, PublishUnit unit,
                                   MultiStatus mStatus, IProgressMonitor monitor) {

        IPath mvnCommandLocation = null;
        String parentPath = config.getConfigValue(ConfigurationType.aggregatorParentBasedir);
        if (parentPath != null) {
            // It is possible for the user to delete the parent project, so it doesn't exist
            // For testing this if statement, you can rename the parent directory for verifying
            if (new File(parentPath).exists()) {
                mvnCommandLocation = new Path(parentPath);

            } else {
                // Parent does not exist, display warning
                mStatus.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(com.ibm.etools.maven.liberty.integration.internal.Messages.errorParentPOMLocationDoesNotExist,
                                                                                    parentPath)));
                return;
            }
        } else {
            // Plugin does not contain parent. Although not standard, the EAR can have no
            // parent POM. In that case, the ConfigurationType.aggregatorParentBasedir does not exist

            // TODO: Need to figure out what is expected in this situation. Fails silently for now.
            return;
        }

        LibertyMaven.runMavenGoal(mvnCommandLocation, mvnCommand, config.getActiveBuildProfiles(), monitor);

        // Ensure the local connector exists
        wsServer.ensureLocalConnectorAndAppMBeanConfig(monitor);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.st.core.internal.ApplicationPublisher#publishModule(int, com.ibm.ws.st.core.internal.PublishUnit, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus publishModule(int kind, PublishUnit publishUnit, IProgressMonitor monitor) {

        // Only run this custom publisher for a non-loose Liberty maven server
        WebSphereServer wsServer = getWebSphereServer();
        WebSphereServerBehaviour wsBehaviour = getWebSphereServerBehaviour();

        if (wsServer != null && wsBehaviour != null) {
            String type = wsServer.getServerType();
            if (Constants.SERVER_TYPE_LIBERTY_MAVEN.equals(type)) {

                if (publishUnit != null) {
                    IModule module = publishUnit.getModule()[0];

                    if (!module.isExternal()) {
                        IProject moduleProject = module.getProject();
                        if (moduleProject != null) {

                            ProjectMapping mapping = LibertyMavenProjectMapping.getInstance().getMapping(moduleProject.getName());

                            if (mapping != null && mapping.getServerID().equals(wsServer.getServer().getId())) {
                                if (wsServer.isLooseConfigEnabled()) {
                                    // In the loose config case, call the JEEPublisher implementation
                                    super.publishModule(kind, publishUnit, monitor);
                                } else {
                                    // Deal with added and changed via maven goal
                                    LibertyBuildPluginConfiguration config = LibertyMaven.getLibertyMavenProjectConfiguration(moduleProject, monitor);

                                    String serverDir = config.getConfigValue(ConfigurationType.serverDirectory);
                                    String appsDir = config.getConfigValue(ConfigurationType.appsDirectory);
                                    String appName = config.getConfigValue(ConfigurationType.applicationFilename);

                                    Trace.trace(Trace.INFO, "serverDir: " + serverDir + " appsDir:" + appsDir + " appName:" + appName);

                                    String pathToPublishedModule = "";
                                    if (serverDir != null && appsDir != null && appName != null) {
                                        StringBuffer buffer = new StringBuffer();
                                        buffer.append(serverDir);
                                        buffer.append("/");
                                        buffer.append(appsDir);
                                        buffer.append("/");
                                        buffer.append(appName);
                                        pathToPublishedModule = buffer.toString();
                                    }

                                    int publishUnitKind = publishUnit.getDeltaKind();
                                    if (ServerBehaviourDelegate.ADDED == publishUnitKind || ServerBehaviourDelegate.CHANGED == publishUnitKind) {
                                        final String mvnRepublishCmd = "war:war liberty:deploy";

                                        // In the unlikely event that any of these values are null, it should be handled
                                        // It is possible the user removes some entries from the liberty plugin config to reach this state
                                        if (serverDir == null || appsDir == null || appName == null) {
                                            LibertyMaven.runMavenGoal(moduleProject.getLocation(), mvnRepublishCmd, config.getActiveBuildProfiles(), monitor);

                                        } else {
                                            LibertyMaven.runMavenGoal(moduleProject.getLocation(), mvnRepublishCmd, config.getActiveBuildProfiles(), monitor);

                                            // Calling the mvn goal "war:war liberty:deploy" will reset the server.xml, so the
                                            // local connector and mbean have to be added back
                                            wsServer.ensureLocalConnectorAndAppMBeanConfig(monitor);

                                            // Add the delta to the change list. Since the entire file is copied over
                                            // (not individual files), calling computeDelta is not necessary
                                            getChangedResourceList().add(pathToPublishedModule);

                                        }
                                    } else if (ServerBehaviourDelegate.REMOVED == publishUnitKind) {
                                        File f = new File(pathToPublishedModule);
                                        if (f.exists()) {
                                            if (!f.delete()) {
                                                Trace.trace(Trace.WARNING, "Failed to cleanup published module");
                                            }
                                        }
                                    }
                                }
                                // Note: Remove in the loose config case is handled by the postPublishApplication method

                                // Cleanup the config dropins for both the loose and non-loose case
                                if (ServerBehaviourDelegate.REMOVED == publishUnit.getDeltaKind()) {
                                    removePublishedAppFiles(publishUnit, true, new MultiStatus(Activator.PLUGIN_ID, 0, null, null), monitor);
                                }

                            } // end module mapping check
                            else if (getBuildPluginImpl().isDependencyModule(moduleProject, wsServer.getServer())) {
                                /*
                                 * The module project isn't mapped directly but it could be a dependency project.
                                 * In that case we should call deploy on the parent for the non-loose config case and for loose config call the JEEPublisher.
                                 */
                                if (wsServer.isLooseConfigEnabled()) {
                                    // In the loose config case, call the JEEPublisher implementation
                                    super.publishModule(kind, publishUnit, monitor);
                                } else {
                                    IProject mappedProject = LibertyMaven.getMappedProject(wsServer.getServer());
                                    LibertyBuildPluginConfiguration config = LibertyMaven.getLibertyMavenProjectConfiguration(mappedProject, monitor);

                                    String parentId = config.getConfigValue(ConfigurationType.aggregatorParentId);
                                    String parentBaseDir = config.getConfigValue(ConfigurationType.aggregatorParentBasedir);
                                    if (parentId != null && parentBaseDir != null) {
                                        IPath parentPath = new Path(parentBaseDir);
                                        if (!parentPath.toFile().exists()) {
                                            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The parent project could not be found " + parentId + " : " + parentBaseDir);
                                        }
                                        String installGoal = "install";
                                        LibertyMaven.runMavenGoal(moduleProject.getLocation(), installGoal, config.getActiveBuildProfiles(), monitor);
                                    }

                                    String installGoal = "liberty:deploy";
                                    Trace.trace(Trace.INFO, "Running " + installGoal + " goal on project: " + mappedProject.getName());
                                    LibertyMaven.runMavenGoal(mappedProject.getLocation(), installGoal, config.getActiveBuildProfiles(), monitor);
                                    wsServer.ensureLocalConnectorAndAppMBeanConfig(monitor);
                                }
                            }

                        } // end module project null check
                    } // end module external check
                } // end publishUnit null check
            }
        }
        return Status.OK_STATUS;
    }

    @Override
    public void prePublishApplication(int kind, PublishUnit app, MultiStatus status, IProgressMonitor monitor) {
        WebSphereServer wsServer = getWebSphereServer();

        if (wsServer != null) {
            String type = wsServer.getServerType();
            if (Constants.SERVER_TYPE_LIBERTY_MAVEN.equals(type)) {
                super.prePublishApplication(kind, app, status, monitor);
            }
        }
    }
}