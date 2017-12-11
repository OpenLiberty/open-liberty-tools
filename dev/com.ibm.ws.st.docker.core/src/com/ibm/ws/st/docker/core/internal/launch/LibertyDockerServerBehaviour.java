/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.core.internal.launch;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.common.core.ext.internal.util.FileUtil;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.PromptAction;
import com.ibm.ws.st.core.internal.PromptHandler;
import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.launch.WebSphereLaunchConfigurationDelegate;
import com.ibm.ws.st.docker.core.internal.Messages;
import com.ibm.ws.st.docker.core.internal.NewContainerPrompt;
import com.ibm.ws.st.docker.core.internal.Trace;

/**
 * Liberty Local Docker Server Behaviour Implementation
 */
public class LibertyDockerServerBehaviour extends AbstractServerBehaviourExtension {

    public int count = 1;

    IPath appsOverridePath = null;
    private Map<IPath, IPath> pathMappingHash = null;
    Boolean directUserDirMount = null;

    /** {@inheritDoc} */
    @Override
    public void stop(ServerBehaviourDelegate behaviour, boolean force, IProgressMonitor monitor) {

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Stopping the server");

        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);

        if (server != null) {
            Map<String, String> serviceInfo = server.getServiceInfo();
            WebSphereServerBehaviour websphereBehaviour = null;
            try {
                if (serviceInfo != null) {
                    websphereBehaviour = server.getWebSphereServerBehaviour();
                    if (websphereBehaviour != null) {
                        websphereBehaviour.setServerAndModuleState(IServer.STATE_STOPPING);

                        LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
                        BaseDockerContainer container = serverExt.getContainer(server);

                        websphereBehaviour.stopMonitorThread(); // while stopping the server the monitor should be stopped to prevent the monitor from trying to start the server

                        container.stop(monitor);
                    }
                } else {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not load the service info");
                }

            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not stop the server", e);

            } finally {
                // the monitor thread should always be running
                if (websphereBehaviour != null)
                    // startMonitorThread checks if the monitor is already running
                    websphereBehaviour.startMonitorThread();
            }
        }
    }

    @Override
    public IStatus canStop(ServerBehaviourDelegate behaviour) {
        WebSphereServerBehaviour websphereBehaviour = (WebSphereServerBehaviour) behaviour.getServer().loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());

        // Remote server checks
        if (websphereBehaviour != null && !websphereBehaviour.getWebSphereServer().isLocalHost()) {
            if (com.ibm.ws.st.common.core.ext.internal.Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) == null)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServerActionsUnavailable);

            if (!websphereBehaviour.getWebSphereServer().getIsRemoteServerStartEnabled())
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServerActionsDisabled);

        }

        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus canRestart(ServerBehaviourDelegate behaviour) {
        WebSphereServerBehaviour websphereBehaviour = (WebSphereServerBehaviour) behaviour.getServer().loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());

        // Remote server checks
        if (websphereBehaviour != null && !websphereBehaviour.getWebSphereServer().isLocalHost()) {
            if (com.ibm.ws.st.common.core.ext.internal.Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) == null)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServerActionsUnavailable);

            if (!websphereBehaviour.getWebSphereServer().getIsRemoteServerStartEnabled())
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServerActionsDisabled);
        }

        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canCleanOnStart() {
        return false;
    }

    @Override
    public Boolean isLooseConfigEnabled(ServerBehaviourDelegate behaviour) {
        WebSphereServerBehaviour websphereBehaviour = (WebSphereServerBehaviour) behaviour.getServer().loadAdapter(WebSphereServerBehaviour.class, new NullProgressMonitor());
        if (websphereBehaviour != null && websphereBehaviour.getWebSphereServer().isLocalHost()) {
            return new Boolean(true);
        }
        return new Boolean(false);
    }

    @Override
    public IPath getAppsOverride(ServerBehaviourDelegate behaviour) {
        if (appsOverridePath != null) {
            return appsOverridePath;
        }
        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (server != null) {
            LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
            IPath serverAppsPath = server.getServerInfo().getServerAppsPath();
            IPath path = server.getServerInfo().getUserDirectory().getPath();
            IPath makeRelativeTo = serverAppsPath.makeRelativeTo(path);
            IPath appsFolder = null;
            String sourceMount = null;
            if (serverExt != null) {
                Map<String, String> serviceInfo = server.getServiceInfo();
                String libertyUsrPath = LibertyDockerRunUtility.getLibertyUsrPath(serviceInfo);
                try {
                    BaseDockerContainer container = serverExt.getContainer(server);
                    if (container != null) {
                        sourceMount = container.getMountSourceForDestination(libertyUsrPath);
                        if (sourceMount != null) {
                            appsFolder = new Path(sourceMount).append(makeRelativeTo);
                            // For Windows non-native, the mounted volume is /c/Users/<path>.  It is a 'logical' mapping of the
                            // real local path c:/Users/<path>.   So, we need to convert it back to the usable form.
                            // On Mac and Linux, no conversion is necessary.
                            appsFolder = BaseDockerContainer.getContainerToLocalPath(appsFolder);
                        }
                    }
                } catch (UnsupportedServiceException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not get Docker container", e);

                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to get the Docker container mount source for: " + libertyUsrPath, e);
                }
                if (appsFolder != null) {
                    appsOverridePath = appsFolder;
                    return appsFolder;
                }
            }
        }
        // return null by default
        return super.getAppsOverride(behaviour);
    }

    @Override
    public IPath getMappedPath(IPath path, ServerBehaviourDelegate behaviour) {
        if (isLooseConfigEnabled(behaviour).booleanValue()) {
            Map<IPath, IPath> pathHash = getPathMappingHash(behaviour);
            if (pathHash != null) {
                IPath fullPath = FileUtil.getCanonicalPath(path);
                for (Map.Entry<IPath, IPath> entry : pathHash.entrySet()) {
                    IPath basePath = FileUtil.getCanonicalPath(entry.getKey());
                    if (basePath.isPrefixOf(fullPath)) {
                        IPath relPath = fullPath.makeRelativeTo(basePath);
                        IPath mappedPath = entry.getValue();
                        if (relPath != null && !relPath.isEmpty()) {
                            mappedPath = mappedPath.append(relPath);
                        }
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.INFO, "The " + path + " path was mapped to: " + mappedPath);
                        }
                        return mappedPath;
                    }
                }
            }
        }

        return super.getMappedPath(path, behaviour);
    }

    public synchronized Map<IPath, IPath> getPathMappingHash(ServerBehaviourDelegate behaviour) {
        if (pathMappingHash == null) {
            BaseDockerContainer container = getContainer(behaviour);
            if (container != null) {
                try {
                    pathMappingHash = container.getMountedVolumeHash();
                } catch (Exception e) {
                    Trace.logError("Failed to get the volumes for the " + container.getContainerName() + " container.", e);
                }
            } else {
                Trace.logError("Failed to get the container for the " + behaviour.getServer().getName() + " server.", null);
            }
        }

        return pathMappingHash;
    }

    private BaseDockerContainer getContainer(ServerBehaviourDelegate behaviour) {
        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (server != null) {
            LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
            if (serverExt != null) {
                try {
                    BaseDockerContainer container = serverExt.getContainer(server);
                    return container;
                } catch (UnsupportedServiceException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not get the Docker container for the " + behaviour.getServer().getName() + " server.", e);
                }
            }
        }
        return null;
    }

    private void doHandleLooseConfigChange(final ServerBehaviourDelegate behaviour, final boolean isLooseConfig) {
        final WebSphereServer websphereServer = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (websphereServer == null) {
            return;
        }

        // Lock workspace root, the server itself, the Liberty runtime workspace project
        final ISchedulingRule publishRule = MultiRule.combine(new ISchedulingRule[] { ResourcesPlugin.getWorkspace().getRoot(), websphereServer.getServer(),
                                                                                      websphereServer.getWebSphereRuntime().getProject() });

        Job job = new Job(isLooseConfig ? Messages.dockerEnablingLooseConfigSettingJob : Messages.dockerDisablingLooseConfigSettingJob) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    Job.getJobManager().beginRule(publishRule, monitor);

                    LibertyDockerServer serverExt = (LibertyDockerServer) websphereServer.getAdapter(LibertyDockerServer.class);
                    if (serverExt != null) {
                        Map<String, String> serviceInfo = websphereServer.getServiceInfo();
                        String containerName = serverExt.getContainerName(websphereServer);
                        serviceInfo.put(Constants.DOCKER_CONTAINER, containerName);

                        String currentMode = serverExt.getCurrentMode(websphereServer);

                        websphereServer.getWebSphereServerBehaviour().restart(currentMode);
                    }

                    websphereServer.getWebSphereServerBehaviour().waitForServerStart(websphereServer.getServer(), monitor);

                    // Clean out any cached information
                    resetCachedInfo();

                    // The auto publish thread will return and not publish because we're restarting the server
                    // Looks like we have to force a publish.  Should take into account the publish setting of the server editor
                    websphereServer.getWebSphereServerBehaviour().publish(IServer.PUBLISH_INCREMENTAL, monitor);

                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not switch loose config modes.  Restart server failed.", e);
                    if (e instanceof CoreException) {
                        return ((CoreException) e).getStatus();
                    }
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerCreateNewContainerError, e);
                } finally {
                    Job.getJobManager().endRule(publishRule);
                }

                return Status.OK_STATUS;
            }

            /** {@inheritDoc} */
            @Override
            public boolean belongsTo(Object family) {
                return WebSphereLaunchConfigurationDelegate.INTERNAL_LAUNCH_JOB_FAMILY.equals(family);
            }
        };
        job.setProperty(WebSphereLaunchConfigurationDelegate.INTERNAL_LAUNCH_SERVER_PROPERTY, behaviour.getServer());
        job.setRule(publishRule);
        job.schedule();
    }

    /**
     * On doSave of the server editor, this method is called.
     * {@inheritDoc}
     */
    @Override
    public IStatus preSaveLooseConfigChange(ServerBehaviourDelegate behaviour, boolean isLooseConfig) {
        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (server != null) {
            LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
            BaseDockerContainer container = null;
            if (serverExt != null) {
                try {
                    container = serverExt.getContainer(server);
                    if (container != null) {
                        String msg = isLooseConfig ? Messages.dockerLooseConfigChangePromptEnableDetails : Messages.dockerLooseConfigChangePromptDisableDetails;
                        NewContainerPrompt prompt = new NewContainerPrompt(msg);
                        prompt.getActionHandler().prePromptAction(null, null, new NullProgressMonitor());

                        PromptHandler promptHandler = Activator.getPromptHandler();
                        if (promptHandler != null && !PromptUtil.isSuppressDialog()) {
                            IPromptResponse response = promptHandler.getResponse(Messages.dockerLooseConfigChangePromptTitle, new PromptHandler.AbstractPrompt[] { prompt },
                                                                                 PromptHandler.STYLE_QUESTION | PromptHandler.STYLE_CANCEL);

                            if (response != null && response.getSelectedAction(prompt.getIssues()[0]) == (PromptAction.YES)) {

                                doHandleLooseConfigChange(behaviour, isLooseConfig);

                            } else {
                                return Status.CANCEL_STATUS;
                            }
                        } else {
                            // The code is being called headlessly, just do the operation
                            doHandleLooseConfigChange(behaviour, isLooseConfig);
                        }
                    }
                } catch (Exception e) {
                    return Status.CANCEL_STATUS;
                }
            }
        }
        return Status.OK_STATUS;
    }

    public IStatus restartServer(final ServerBehaviourDelegate behaviour, String jobMessage, final IProgressMonitor progressMonitor) {
        final WebSphereServer websphereServer = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (websphereServer == null) {
            return Status.CANCEL_STATUS;
        }

        Job job = new Job(jobMessage) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    Job.getJobManager().beginRule(behaviour.getServer(), monitor);

                    LibertyDockerServer serverExt = (LibertyDockerServer) websphereServer.getAdapter(LibertyDockerServer.class);
                    if (serverExt != null) {
                        Map<String, String> serviceInfo = websphereServer.getServiceInfo();
                        String containerName = serverExt.getContainerName(websphereServer);
                        serviceInfo.put(Constants.DOCKER_CONTAINER, containerName);

                        String currentMode = serverExt.getCurrentMode(websphereServer);
                        websphereServer.getWebSphereServerBehaviour().restart(currentMode, progressMonitor);
                    }

                    websphereServer.getWebSphereServerBehaviour().waitForServerStart(websphereServer.getServer(), monitor);

                    // Clean out any cached information
                    resetCachedInfo();
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to restart the " + behaviour.getServer().getName() + " server.", e);
                    if (e instanceof CoreException) {
                        return ((CoreException) e).getStatus();
                    }
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.dockerCreateNewContainerError, e);
                } finally {
                    Job.getJobManager().endRule(behaviour.getServer());
                }

                return Status.OK_STATUS;
            }

            /** {@inheritDoc} */
            @Override
            public boolean belongsTo(Object family) {
                return WebSphereLaunchConfigurationDelegate.INTERNAL_LAUNCH_JOB_FAMILY.equals(family);
            }
        };

        job.setProperty(WebSphereLaunchConfigurationDelegate.INTERNAL_LAUNCH_SERVER_PROPERTY, behaviour.getServer());
        job.schedule();
        try {
            job.join();
            IStatus result = job.getResult();
            return result;
        } catch (InterruptedException e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "The restart job for the " + behaviour.getServer().getName() + " server was interrupted.", e);
            }
            return Status.CANCEL_STATUS;
        }
    }

    @Override
    public boolean doUpdateRemoteAppFiles(ServerBehaviourDelegate behaviour, boolean isLooseConfig) {
        // We want to update the remote server for Docker.   For a typical remote server, loose config is not enabled anyway.
        // This is so that the non-loose config app will be uploaded to the Docker container.
        // For loose config, the XML file is 'copied' to the volume so it is not necessary to update.
        return !isLooseConfig;
    }

    @Override
    public void removeRemoteAppFiles(ServerBehaviourDelegate behaviour, IPath path, boolean isLooseConfig, IProgressMonitor monitor) {
        // The original XML file is in the original loose config docker container.   The new container also has the XML file.
        // (Everything under the usr folder is copied over to the new container).   So, when switching to non-loose config mode, the XML file
        // needs to be removed from the new container.

        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (server != null) {
            LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
            BaseDockerContainer container = null;
            if (serverExt != null) {
                try {
                    container = serverExt.getContainer(server);
                    if (container != null) {
                        Map<String, String> serviceInfo = server.getServiceInfo();
                        String serverConfigPath = serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_CONFIG_PATH);
                        IPath scPath = new Path(serverConfigPath);
                        IPath fileToRemove = scPath.append("apps").append(path.lastSegment());
                        container.deleteFile(fileToRemove.toString());
                    }
                } catch (Exception e) {
                    // Better error handling support
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not get delete the file from the Docker container", e);
                }
            }
        }
        return;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLocalUserDir(ServerBehaviourDelegate behaviour) {
        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (server != null) {
            if (server.isLooseConfigEnabled() && isDirectUserDirMount(behaviour)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDirectUserDirMount(ServerBehaviourDelegate behaviour) {
        if (directUserDirMount != null) {
            return directUserDirMount.booleanValue();
        }
        boolean result = false;
        WebSphereServer server = behaviour.getServer().getAdapter(WebSphereServer.class);
        if (server != null) {
            LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
            IPath userDirPath = server.getServerInfo().getUserDirectory().getPath();
            String sourceMount = null;
            if (serverExt != null) {
                try {
                    BaseDockerContainer container = serverExt.getContainer(server);
                    if (container != null) {
                        Map<String, String> serviceInfo = server.getServiceInfo();
                        sourceMount = container.getMountSourceForDestination(LibertyDockerRunUtility.getLibertyUsrPath(serviceInfo));
                        if (sourceMount != null) {
                            IPath sourcePath = BaseDockerContainer.getContainerToLocalPath(new Path(sourceMount));
                            if (userDirPath.toFile().getCanonicalPath().equals(sourcePath.toFile().getCanonicalPath())) {
                                directUserDirMount = Boolean.TRUE;
                            } else {
                                directUserDirMount = Boolean.FALSE;
                            }
                            result = directUserDirMount.booleanValue();
                        }
                    }
                } catch (UnsupportedServiceException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not get Docker container", e);

                } catch (IOException e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Comparison of the user directory to the mount source failed.", e);
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to determine if Docker container has a direct user directory mount.", e);
                }
            }
        }
        return result;
    }

    protected void resetCachedInfo() {
        appsOverridePath = null;
        pathMappingHash = null;
        directUserDirMount = null;
    }
}
