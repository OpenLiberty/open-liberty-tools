/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;

/**
 * Resource change listener that triggers an update job whenever xml files are changed
 * in the workspace. Not an ideal approach, but simpler and safer than the alternatives.
 */
public class ConfigurationResourceChangeListener implements IResourceChangeListener {
    private static final String XML = "xml";

    private static ConfigurationResourceChangeListener resourceChangeListener;

    public synchronized static void start() {
        if (resourceChangeListener != null)
            return;
        resourceChangeListener = new ConfigurationResourceChangeListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    public synchronized static void stop() {
        if (resourceChangeListener == null)
            return;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace != null)
            workspace.removeResourceChangeListener(resourceChangeListener);
        resourceChangeListener = null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta == null)
            return;

        // ignore clean builds
        if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD)
            return;

        long time = System.currentTimeMillis();

        try {
            // collect a list of changed files and folders
            final List<IFolder> folders = new ArrayList<IFolder>(2);
            final List<IFile> files = new ArrayList<IFile>(10);

            delta.accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta visitorDelta) {
                    IResource resource = visitorDelta.getResource();
                    if (resource instanceof IProject) {
                        IProject proj = (IProject) resource;
                        // visit server folders if they exist or are configured by a runtime
                        return proj.getFolder(Constants.SERVERS_FOLDER).exists() || isConfiguredUserDirectory(proj);
                    }
                    int kind = visitorDelta.getKind();

                    if (resource != null && resource instanceof IFile) {
                        IFile file = (IFile) resource;
                        if (XML.equals(file.getFileExtension()) || ExtendedConfigFile.isExtendedConfigFile(file.getName())) {
                            switch (kind) {
                                case IResourceDelta.ADDED:
                                    files.add(file);
                                    break;
                                case IResourceDelta.REMOVED:
                                    files.add(file);
                                    break;
                                default:
                                    if ((visitorDelta.getFlags() & IResourceDelta.CONTENT) == 0 &&
                                        (visitorDelta.getFlags() & IResourceDelta.OPEN) == 0 &&
                                        (visitorDelta.getFlags() & IResourceDelta.REPLACED) == 0)
                                        return false;
                                    files.add(file);
                            }
                        }
                    }
                    if (resource != null && resource instanceof IFolder) {
                        if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
                            IPath path = resource.getProjectRelativePath();
                            if (path.segmentCount() == 2 && Constants.SERVERS_FOLDER.equals(path.segment(0))) {
                                IContainer serverFolder = resource.getParent();
                                if (serverFolder instanceof IFolder && !folders.contains(serverFolder))
                                    folders.add((IFolder) serverFolder);
                            } else if (path.segmentCount() > 2 && Constants.SERVERS_FOLDER.equals(path.segment(0)) &&
                                       Constants.CONFIG_DROPINS_FOLDER.equals(path.segment(2))) {
                                // Handle added/removed config dropins folders
                                folders.add((IFolder) resource);
                            }
                        }
                    }
                    return true;
                }

                private boolean isConfiguredUserDirectory(IProject proj) {
                    WebSphereRuntime[] runtimes = WebSphereUtil.getWebSphereRuntimes();
                    for (WebSphereRuntime r : runtimes) {
                        for (UserDirectory ud : r.getUserDirectories()) {
                            if (proj.equals(ud.getProject())) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

            if (!folders.isEmpty() || !files.isEmpty())
                refreshChanges(folders, files);
        } catch (CoreException ce) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error in resource listener", ce);
        } finally {
            if (Trace.ENABLED)
                Trace.tracePerf("Resource change listener", time);
        }
    }

    protected void refreshChanges(final List<IFolder> folders, final List<IFile> files) {
        // If there are any changes significant to liberty, clear the schema location provider
        // cache.  This is done outside of the job since it is very quick and we don't want the
        // user to get the wrong editor for a file if the job takes a while to get scheduled.
        SchemaLocationProvider.clearCache();

        Job job = new Job("WebSphere Configuration Updater") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    long time = System.currentTimeMillis();
                    refreshChangesImpl(folders, files);
                    if (Trace.ENABLED)
                        Trace.tracePerf("Configuration update job", time);
                } catch (Exception e) {
                    Trace.logError("Error in WebSphere Configuration Updater", e);
                }
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return Constants.JOB_FAMILY.equals(family);
            }
        };
        job.setSystem(true);
        job.setPriority(Job.LONG);
        job.schedule();
    }

    protected static void refreshChangesImpl(final List<IFolder> folders, final List<IFile> files) {
        WebSphereRuntime[] runtimes = WebSphereUtil.getWebSphereRuntimes();
        if (!folders.isEmpty()) {
            for (WebSphereRuntime runtime : runtimes) {
                List<UserDirectory> userDirs = runtime.getUserDirectories();
                for (UserDirectory ud : userDirs) {
                    IFolder folder = ud.getServersFolder();
                    if (folders.contains(folder)) {
                        WebSphereUtil.configureValidatorsForRuntimeProject(ud.getProject());
                        runtime.updateServerCache(true);
                    }
                }
            }
        }

        Set<WebSphereServerInfo> serverSet = new HashSet<WebSphereServerInfo>();
        Set<UserDirectory> userDirSet = new HashSet<UserDirectory>();

        // loop through to see if anything needs refreshing
        for (WebSphereRuntime runtime : runtimes) {
            List<UserDirectory> userDirs = runtime.getUserDirectories();
            for (UserDirectory ud : userDirs) {
                List<WebSphereServerInfo> servers = runtime.getWebSphereServerInfos(ud);

                // Handle changes in shared config folder
                IFolder folder = ud.getSharedConfigFolder();
                if (folder != null && folder.exists()) {
                    for (IFile file : files) {
                        if (folder.getFullPath().isPrefixOf(file.getFullPath())) {
                            userDirSet.add(ud);
                            // for remote server, there is only 1 server per user directory. To sync shared files, we need to add that particular server
                            // in the "serverList" variable so that sync job is triggered when files in shared folder are changed as well.
                            for (WebSphereServerInfo server : servers) {
                                WebSphereServer ws = WebSphereUtil.getWebSphereServer(server);
                                if (ws != null && !ws.isLocalSetup())
                                    serverSet.add(server);
                            }
                            break;
                        }
                    }

                    IPath sharedJVMPath = ud.getSharedFolder().getFullPath().append(ExtendedConfigFile.JVM_OPTIONS_FILE);
                    for (IFile file : files) {
                        if (sharedJVMPath.equals(file.getFullPath())) {
                            serverSet.addAll(servers);
                            break;
                        }
                    }
                }

                // Handle changes in the server folder
                for (WebSphereServerInfo server : servers) {
                    folder = server.getServerFolder();
                    if (folder == null) {
                        continue;
                    }
                    for (IFile file : files) {
                        if (folder.getFullPath().isPrefixOf(file.getFullPath())) {
                            serverSet.add(server);
                            break;
                        }
                    }

                    // Handle added/removed config dropins folders
                    IPath configDropinsPath = folder.getFullPath().append(Constants.CONFIG_DROPINS_FOLDER);
                    for (IFolder changedFolder : folders) {
                        if (configDropinsPath.isPrefixOf(changedFolder.getFullPath())) {
                            serverSet.add(server);
                        }
                    }
                }
            }
        }

        // refresh affected user directories
        for (UserDirectory ud : userDirSet)
            ud.refreshSharedConfig();

        // refresh affected servers
        for (WebSphereServerInfo server : serverSet) {
            // Make sure the server cache is updated.  Don't rely on its return code because
            // other resource change listeners could have updated it already (such as the OSGi
            // one).  The saved config sync info will detect if something has changed or not.
            server.updateCache();
            WebSphereServer ws = WebSphereUtil.getWebSphereServer(server);
            if (ws != null) {
                if (ws.isLocalSetup())
                    ws.serverConfigChanged(null);
                ws.getWebSphereServerBehaviour().startAutoConfigSyncJob();
            }

            // Make sure views are updated
            ServerListenerUtil.getInstance().fireServerChangedEvent(server);
        }

    }
}