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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeLifecycleListener;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.util.RuntimeLifecycleAdapter;
import org.eclipse.wst.server.core.util.ServerLifecycleAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.st.common.core.internal.CommonServerUtil;
import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.generation.FeatureListExtMetadata;
import com.ibm.ws.st.core.internal.generation.MetaDataRemover;
import com.ibm.ws.st.core.internal.jmx.JMXConnectionInfo;
import com.ibm.ws.st.core.internal.launch.AbstractServerStartupExtension;

/**
 * The activator class controls the bundles's life cycle.
 */
public class Activator extends Plugin {
    // the bundle id
    public static final String PLUGIN_ID = "com.ibm.ws.st.core";

    public static final String SOURCE_PATH_EXTENSION_POINT = "websphereSourcePathComputers";
    // the shared instance
    private static Activator instance;

    // preference keys
    private static final String PREFERENCE_KEY_ENABLE_AUTOMATIC_FEATURE_DETECTION = "enable.automatic.feature.detection";
    private static final String PREFERENCE_KEY_DEFAULT_CLASS_SCANNING = "default.class.scanning";

    // preference default values
    private static final boolean PREFERENCE_DEFAULT_VALUE_ENABLE_AUTOMATIC_FEATURE_DETECTION = true;
    private static final int PREFERENCE_DEFAULT_VALUE_DEFAULT_CLASS_SCANNING = getDefaultClassScanningDefaultValue();

    private IRuntimeLifecycleListener runtimeLifeCycleListener;
    private IServerLifecycleListener serverLifeCycleListener;
    private IWebSphereMetadataListener metadataListener;

    private ClasspathExtension[] classpathExtensions;
    private ClasspathExtension[] emptyContainerExtensions;
    private ISourcePathComputerDelegate[] sourcePathComputerExtensions;
    private AbstractServerStartupExtension[] preStartExtensions;

    private static PromptHandler promptHandler;
    private static FeatureConflictHandler featureConflictHandler;
    private static MissingKeystoreHandler missingKeystoreHandler;
    private static PublishWithErrorHandler publishWithErrorHandler;
    private static MessageHandler messageHandler;

    Properties serverMessageReplacementKey = null;
    private static final String SERVER_MESSAGE_REPLACEMENT_KEY_FILENAME = "serverMessageReplaceKey.properties";
    boolean isServerMessageReplacementKeyInited = false;
    private final List<IDebugTarget> debugTargets = new ArrayList<IDebugTarget>();

    public ServiceTracker<FeatureResolver, FeatureResolver> resolverTracker;

    public Activator() {
        // do nothing
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        Trace.ENABLED = isDebugging();

        resolverTracker = new ServiceTracker<FeatureResolver, FeatureResolver>(context, FeatureResolver.class, null);
        resolverTracker.open();

        // register and initialize the tracing class
        final Hashtable<String, String> props = new Hashtable<String, String>(4);
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, Activator.PLUGIN_ID);
        context.registerService(DebugOptionsListener.class.getName(), Trace.TS, props);

        runtimeLifeCycleListener = new RuntimeLifecycleAdapter() {
            @Override
            public void runtimeAdded(final IRuntime runtime) {
                final WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                Job runtimeAddedJob = new Job(Messages.jobRefreshRuntimeMetadata) {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            monitor.beginTask(Messages.jobRefreshRuntimeMetadata, 100);
                            monitor.worked(25);
                            if (wsRuntime != null) {
                                ConfigurationResourceChangeListener.start();
                                wsRuntime.createMetadata(null);
                                wsRuntime.createDefaultUserDirectory(null);
                                wsRuntime.initializeClasspathHelper();
                            }
                        } finally {
                            monitor.done();
                        }
                        return Status.OK_STATUS;
                    }
                };
                runtimeAddedJob.setPriority(Job.SHORT);
                runtimeAddedJob.schedule();

                // Rebuild any non-Java projects that already have a
                // dependency on this runtime (such as EAR projects).
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                IProject[] projects = root.getProjects();
                final List<IProject> projectsToBuild = new ArrayList<IProject>();
                for (IProject project : projects) {
                    try {
                        if (project.hasNature(JavaCore.NATURE_ID))
                            continue;
                    } catch (CoreException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Test for Java nature failed for project: " + project, e);
                    }
                    IFacetedProject facetedProject = null;
                    try {
                        facetedProject = ProjectFacetsManager.create(project);
                    } catch (CoreException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to create faceted project for: " + project, e);
                    }
                    if (facetedProject == null)
                        continue;
                    Set<org.eclipse.wst.common.project.facet.core.runtime.IRuntime> runtimes = facetedProject.getTargetedRuntimes();
                    org.eclipse.wst.common.project.facet.core.runtime.IRuntime facetRuntime = FacetUtil.getRuntime(runtime);
                    if (facetRuntime != null && runtimes.contains(facetRuntime))
                        projectsToBuild.add(project);
                }

                if (!projectsToBuild.isEmpty()) {
                    Job rebuildProjectsJob = new Job(Messages.jobBuildNonJavaProjects) {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                monitor.beginTask(Messages.jobBuildNonJavaProjects, 125);
                                monitor.worked(25);
                                int work = 100 / projectsToBuild.size();
                                for (IProject project : projectsToBuild) {
                                    try {
                                        project.build(IncrementalProjectBuilder.CLEAN_BUILD, new SubProgressMonitor(monitor, work));
                                    } catch (CoreException e) {
                                        if (Trace.ENABLED)
                                            Trace.trace(Trace.WARNING, "Project build failed for: " + project, e);
                                    }
                                }
                                monitor.worked(100 % projectsToBuild.size());
                            } finally {
                                monitor.done();
                            }
                            return Status.OK_STATUS;
                        }
                    };
                    rebuildProjectsJob.setPriority(Job.LONG);
                    rebuildProjectsJob.schedule();
                }
            }

            @Override
            public void runtimeChanged(final IRuntime runtime) {
                Job runtimeChangedJob = new Job(Messages.jobRefreshRuntimeMetadata) {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            monitor.beginTask(Messages.jobRefreshRuntimeMetadata, 100);
                            WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                            monitor.worked(25);
                            if (wsRuntime != null) {
                                wsRuntime.runtimeChanged();
                                // The location of the runtime has changed, so we need to
                                // delete the existing default user directory project and
                                // recreate it at the new location
                                IPath newLoc = runtime.getLocation().append(Constants.USER_FOLDER);
                                if (!newLoc.equals(wsRuntime.getProject().getLocation())) {
                                    wsRuntime.deleteProject(null);
                                    wsRuntime.createProject(null);
                                }
                            }
                        } finally {
                            monitor.done();
                        }
                        return Status.OK_STATUS;
                    }

                };
                runtimeChangedJob.setPriority(Job.SHORT);
                runtimeChangedJob.schedule();
            }

            @Override
            public void runtimeRemoved(IRuntime runtime) {
                WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                if (wsRuntime != null) {
                    wsRuntime.removeMetadata(null, true, false);
                    wsRuntime.deleteProject(null);
                }
            }
        };
        ServerCore.addRuntimeLifecycleListener(runtimeLifeCycleListener);

        serverLifeCycleListener = new ServerLifecycleAdapter() {
            @Override
            public void serverRemoved(IServer server) {
                IServerType st = server.getServerType();
                if (st != null) {
                    if (st.getId().startsWith(Constants.ID_PREFIX)) {
                        WebSphereServer serv = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                        if (serv != null) {
                            serv.cleanup();
                            if (!serv.isLocalSetup()) {
                                removeRemoteServerSecureStorageValues(serv);
                                removeTempRemoteServerUsrDirectoryFiles(serv);
                                removeDisableUtilityPromptPrefs(serv);

                                WebSphereRuntime wsRuntime = serv.getWebSphereRuntime();

                                // Delete the server metadata directory
                                IPath path = serv.getServerInfo().buildMetadataDirectoryPath();
                                if (path.toFile().exists()) {
                                    try {
                                        FileUtil.deleteDirectory(path.toOSString(), true);
                                    } catch (IOException e) {
                                        Trace.logError("Failed to remove the server metadata directory: " + path.toOSString(), e);
                                    }
                                }

                                // handle deleting remote server files
                                UserDirectory userDir = serv.getUserDirectory();
                                if (userDir != null) {
                                    serv.getServerInfo().updateCache();

                                    // see 147738: Prompt user when deleting remote server for details on why we are using
                                    // runtime working copy here and saving it
                                    try {
                                        if (wsRuntime != null) {
                                            IRuntime runtime = wsRuntime.getRuntime();
                                            IRuntimeWorkingCopy runtimeWc = runtime.createWorkingCopy();
                                            WebSphereRuntime webSphereRuntime = (WebSphereRuntime) runtimeWc.loadAdapter(WebSphereRuntime.class, null);
                                            webSphereRuntime.removeUserDirectory(userDir);
                                            runtimeWc.save(true, null);
                                        }
                                    } catch (Exception e1) {
                                        Trace.logError("Failed to remove runtime user directory for: " + userDir.getPath().toOSString(), e1);
                                    }

                                    IProject proj = userDir.getProject();
                                    if (proj != null) {
                                        try {
                                            boolean doDeleteProjectFiles = false;
                                            PromptHandler promptHandler = Activator.getPromptHandler();
                                            if (promptHandler != null && !PromptUtil.isSuppressDialog()) {
                                                AbstractPrompt[] prompts = { new DeleteRemoteServersPrompt(server, proj) };
                                                IPromptIssue issue = prompts[0].getIssues()[0];

                                                String selectedActionAlways = Activator.getPreference(Constants.DELETE_PROJECT_FILES_ALWAYS_ACTION, null);
                                                boolean applyAlways = Activator.getPreference(Constants.DELETE_PROJECT_FILES_APPLY_ALWAYS, false);

                                                // Need to get a new response if:
                                                // 1. selectedActionAlways is not defined
                                                // 2. applyAlways is false
                                                if (selectedActionAlways == null || !applyAlways) {
                                                    IPromptResponse response = promptHandler.getResponse(Messages.remoteServerDeletePromptMessage, prompts,
                                                                                                         PromptHandler.STYLE_CANCEL | PromptHandler.STYLE_HELP
                                                                                                                                                            | PromptHandler.STYLE_QUESTION);
                                                    applyAlways = response.getApplyAlways(issue);
                                                    selectedActionAlways = response.getSelectedAction(issue).toString();

                                                    Activator.setPreference(Constants.DELETE_PROJECT_FILES_ALWAYS_ACTION, selectedActionAlways);
                                                    Activator.setPreference(Constants.DELETE_PROJECT_FILES_APPLY_ALWAYS, applyAlways);
                                                }

                                                doDeleteProjectFiles = selectedActionAlways != null && !selectedActionAlways.equals(PromptAction.IGNORE.toString());
                                            }
                                            proj.delete(doDeleteProjectFiles, true, new NullProgressMonitor());
                                        } catch (CoreException e) {
                                            Trace.logError("Failed to remove deleted server's user directory project", e);
                                        }
                                    }
                                }
                                if (wsRuntime != null)
                                    wsRuntime.updateServerCache(true);
                            }
                        }
                    }
                }
            }

            @Override
            public void serverChanged(IServer server) {
                IServerType st = server.getServerType();
                if (st != null) {
                    if (st.getId().startsWith(Constants.ID_PREFIX)) {
                        // if the behaviour is already loaded, sync external modules
                        WebSphereServerBehaviour servB = server.getAdapter(WebSphereServerBehaviour.class);
                        if (servB != null) {
                            servB.syncExternalModules();

                            if (servB.monitorThread instanceof JMXMonitorThread) {
                                try {
                                    // if the jmx connection information has changed reset the JMX Connection
                                    WebSphereServer wsServer = servB.getWebSphereServer();
                                    JMXConnectionInfo oldJMXInfo = wsServer.getJMXConnectionInfo();
                                    JMXConnectionInfo curJMXInfo = new JMXConnectionInfo(wsServer);
                                    wsServer.setJMXConnectionInfo(curJMXInfo);
                                    if (oldJMXInfo != null && !curJMXInfo.equals(oldJMXInfo)) {
                                        servB.stopMonitorThread();
                                        servB.startMonitorThread();
                                    }
                                } catch (Exception e) {
                                    if (Trace.ENABLED) {
                                        Trace.trace(Trace.WARNING, "There was a problem determining if jmx connection should be reset.", e);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void serverAdded(IServer server) {
                // do nothing
            }

            private void removeRemoteServerSecureStorageValues(WebSphereServer server) {
                try {
                    ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
                    String nodeName = CommonServerUtil.getSecurePreferenceNodeName(server.getServer());
                    if (preferences.nodeExists(nodeName)) {
                        ISecurePreferences node = preferences.node(nodeName);
                        node.removeNode();
                    }
                    preferences.flush();
                } catch (Throwable t) {
                    Trace.logError("Failed to remove secure storage values for server: " + server.getServerName(), t);
                }
            }

            private void removeTempRemoteServerUsrDirectoryFiles(WebSphereServer server) {
                try {
                    UserDirectory usrDir = server.getUserDirectory();
                    if (usrDir != null && usrDir.getPath().toOSString().startsWith(Activator.getInstance().getStateLocation().toOSString())) {
                        IPath serverDir = usrDir.getPath().append(Constants.SERVERS_FOLDER).append(server.getServerName());
                        if (serverDir.toFile().exists()) {
                            FileUtil.deleteDirectory(serverDir.toOSString(), true);
                        }
                    }
                } catch (Throwable t) {
                    Trace.logError("Failed to remove remote usr directory files for server: " + server.getServerName(), t);
                }
            }

            private void removeDisableUtilityPromptPrefs(WebSphereServer server) {
                IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
                String remoteSettingsDisabled = server.getServer().getId() + "_" + Constants.REMOTE_SETTINGS_DISABLED_PROMPT;
                String utilityNotSupported = server.getServer().getId() + "_" + Constants.UTILITY_NOT_SUPPORTED_PROMPT;
                try {
                    prefs.remove(remoteSettingsDisabled);
                    prefs.remove(utilityNotSupported);
                    prefs.flush();
                } catch (Exception e) {
                    Trace.logError("Error removing keys: " + remoteSettingsDisabled + ", " + utilityNotSupported, e);
                }
            }
        };
        ServerCore.addServerLifecycleListener(serverLifeCycleListener);

        metadataListener = new IWebSphereMetadataListener() {
            /** {@inheritDoc} */
            @Override
            public void runtimeMetadataChanged(final IRuntime runtime) {
                // Mark config files associated with this runtime as changed so that validation can be
                // re-run with the new schema.
                final String msg = NLS.bind(Messages.jobRefreshConfigurationFiles, runtime.getName());
                Job refreshConfigFiles = new Job(msg) {
                    @Override
                    protected IStatus run(IProgressMonitor progressMonitor) {
                        int totalWork = 100;
                        SubMonitor monitor = SubMonitor.convert(progressMonitor, msg, totalWork);
                        try {
                            WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                            if (wsRuntime != null) {
                                List<WebSphereServerInfo> serverInfos = wsRuntime.getWebSphereServerInfos();
                                if (serverInfos.size() > 0) {
                                    int serverWork = totalWork / serverInfos.size();
                                    for (WebSphereServerInfo info : serverInfos) {
                                        ConfigurationFile[] configFiles = info.getConfigurationFiles();
                                        if (configFiles.length > 0) {
                                            int fileWork = serverWork / configFiles.length;
                                            for (ConfigurationFile file : configFiles) {
                                                IFile iFile = file.getIFile();
                                                if (iFile != null) {
                                                    try {
                                                        // An IFile.touch will tell eclipse that the file has changed so
                                                        // that it gets revalidated but it does not update the timestamp
                                                        // on the file.
                                                        iFile.touch(monitor.newChild(fileWork));
                                                    } catch (CoreException e) {
                                                        if (Trace.ENABLED) {
                                                            Trace.trace(Trace.WARNING, "Touch failed on file: " + iFile.getLocation().toOSString(), e);
                                                        }
                                                    }
                                                }
                                                if (monitor.isCanceled()) {
                                                    return Status.CANCEL_STATUS;
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        } finally {
                            monitor.done();
                        }
                        return Status.OK_STATUS;
                    }

                };
                refreshConfigFiles.setPriority(Job.LONG);
                refreshConfigFiles.schedule();
            }
        };

        ServerListenerUtil.getInstance().addMetadataListener(metadataListener);

        List<ClasspathExtension> includeList = new ArrayList<ClasspathExtension>();
        List<ClasspathExtension> emptyContainerlist = new ArrayList<ClasspathExtension>();

        ClasspathExtension.createClasspathExtensions(includeList, emptyContainerlist);

        classpathExtensions = includeList.toArray(new ClasspathExtension[includeList.size()]);
        emptyContainerExtensions = emptyContainerlist.toArray(new ClasspathExtension[emptyContainerlist.size()]);

        checkRuntimeMetadata();

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // make a sweep of current meta data and remove all that
        // are out of sync
        MetaDataRemover.removeOutOfSyncMetaData();

        resolverTracker.close();

        ServerCore.removeRuntimeLifecycleListener(runtimeLifeCycleListener);
        ServerCore.removeServerLifecycleListener(serverLifeCycleListener);
        ServerListenerUtil.getInstance().removeMetadataListener(metadataListener);
        instance = null;
        ConfigurationResourceChangeListener.stop();

        //terminate all the debugTargets if the workbench is closed with an active debug session to avoid VMDisconnected exception
        terminateDebugTargets();

        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getInstance() {
        return instance;
    }

    public ClasspathExtension[] getClasspathExtensions() {
        return classpathExtensions;
    }

    public ClasspathExtension[] getEmptyContainerExtensions() {
        return emptyContainerExtensions;
    }

    public static String getPreference(String key, String defaultValue) {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(key, defaultValue);
    }

    public static boolean getPreference(String key, boolean defaultValue) {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID).getBoolean(key, defaultValue);
    }

    public static int getPreference(String key, int defaultValue) {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID).getInt(key, defaultValue);
    }

    public static void setPreference(String key, boolean value) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
            prefs.putBoolean(key, value);
            prefs.flush();
        } catch (Exception e) {
            Trace.logError("Error setting preference " + key, e);
        }
    }

    public static void setPreference(String key, String value) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
            if (value == null)
                prefs.remove(key);
            else
                prefs.put(key, value);
            prefs.flush();
        } catch (Exception e) {
            Trace.logError("Error setting preference " + key, e);
        }
    }

    public static void setPreference(String key, int value) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
            prefs.putInt(key, value);
            prefs.flush();
        } catch (Exception e) {
            Trace.logError("Error setting preference " + key, e);
        }
    }

    public static void setPromptHandler(PromptHandler newPromptHandler) {
        promptHandler = newPromptHandler;
    }

    public static PromptHandler getPromptHandler() {
        return promptHandler;
    }

    public static void setFeatureConflictHandler(FeatureConflictHandler handler) {
        featureConflictHandler = handler;
    }

    public static FeatureConflictHandler getFeatureConflictHandler() {
        return featureConflictHandler;
    }

    public static void setMissingKeystoreHandler(MissingKeystoreHandler handler) {
        missingKeystoreHandler = handler;
    }

    public static MissingKeystoreHandler getMissingKeystoreHandler() {
        return missingKeystoreHandler;
    }

    public static String getBundleVersion() {
        return getInstance().getBundle().getVersion().toString();
    }

    /**
     * @return the publishWithErrorHandler
     */
    public static PublishWithErrorHandler getPublishWithErrorHandler() {
        return publishWithErrorHandler;
    }

    /**
     * @param publishWithErrorHandler
     */
    public static void setPublishWithErrorHandler(PublishWithErrorHandler errorHandler) {
        publishWithErrorHandler = errorHandler;
    }

    public static MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public static void setMessageHandler(MessageHandler handler) {
        messageHandler = handler;
    }

    /**
     * -2: has not been initialized
     * -1: default value, wait forever
     */
    private long publishWaitTime = -2;

    /**
     * It will wait until the message appears before exit if it is not specified, an
     * invalid number or a negative value
     *
     * @return
     */
    public long getPreferencePublishWaitTimeMS() {
        if (publishWaitTime == -2) {
            try {
                publishWaitTime = Long.parseLong(getPreference("publish.exit.timeout", "-1"));
            } catch (NumberFormatException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Hidden preference publish.exit.timeout" + " is invalid");
                publishWaitTime = -1;
            }
        }
        return publishWaitTime;
    }

    private boolean useDynamiClasspathContainer = true;
    private boolean useDynamiClasspathContainerInitialized = false;

    /**
     * The preference "dynamic.classpath.container" indicates if the Liberty classpath container should be filtered based on
     * the contents of the project or not.
     *
     * There is no UI to change this preference. If needed, the file metadata\.plugins\org.eclipse.core.runtime\.settings\com.ibm.ws.st.core.prefs can be edited
     * to change the value of this property.
     *
     * The value of this property is read during startup. If its value is changed, Eclipse needs to be restarted
     * for this change to take effect
     *
     *
     * @return true if the Liberty classpath container should be filtered based on the contents of
     *         the project, or false otherwise. The default is true.
     */
    public boolean getPreferenceUseDynamicClasspathContainer() {
        if (!useDynamiClasspathContainerInitialized) {
            useDynamiClasspathContainer = Boolean.parseBoolean(getPreference("dynamic.classpath.container", "true"));
            useDynamiClasspathContainerInitialized = true;
        }
        return useDynamiClasspathContainer;
    }

    /**
     * The preference "dynamic.classpath.container" indicates if the Liberty classpath container should be filtered based on
     * the contents of the project or not.
     *
     * This method sets the preference "dynamic.classpath.container" in the file the file metadata\.plugins\org.eclipse.core.runtime\.settings\com.ibm.ws.st.core.prefs
     *
     * @param value the value to set in the preference
     */
    public void setPreferenceUseDynamicClasspathContainer(boolean value) {
        useDynamiClasspathContainer = value;
        setPreference("dynamic.classpath.container", Boolean.toString(useDynamiClasspathContainer));
        useDynamiClasspathContainerInitialized = false;
    }

    /**
     * -1: has not been initialized
     * 0: default value, doesn't wait
     */
    private long runOnServerDelay = -1;

    public long getPreferenceRunOnServerDelayTimeMS() {
        if (runOnServerDelay == -1) {
            try {
                runOnServerDelay = Long.parseLong(getPreference("run.on.server.delay", "0"));
            } catch (NumberFormatException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Hidden preference run.on.server.delay" + " is invalid");
                runOnServerDelay = 0;
            }
        }
        return runOnServerDelay;
    }

    public Properties getServerMessageReplacementKey() {
        if (!isServerMessageReplacementKeyInited) {
            Properties props = new Properties();
            FileUtil.loadProperties(props, getStateLocation().append(SERVER_MESSAGE_REPLACEMENT_KEY_FILENAME));
            serverMessageReplacementKey = props.isEmpty() ? null : props;
            isServerMessageReplacementKeyInited = true;
        }
        return serverMessageReplacementKey;
    }

    private void checkRuntimeMetadata() {
        Job job = new Job(Messages.jobRefreshRuntimeMetadata) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                WebSphereRuntime[] wsRuntimes = WebSphereUtil.getWebSphereRuntimes();
                monitor.beginTask(Messages.jobRefreshRuntimeMetadata, wsRuntimes.length);

                if (wsRuntimes.length > 0)
                    ConfigurationResourceChangeListener.start();

                for (WebSphereRuntime wsRuntime : wsRuntimes) {
                    monitor.setTaskName(NLS.bind(Messages.jobRuntimeCache, wsRuntime.getRuntime().getName()));
                    Properties info = WebSphereRuntimeProductInfoCacheUtil.getChangedProductInfo(wsRuntime);
                    if (info != null) {
                        // product info has changed, need to regenerate metadata
                        wsRuntime.generateMetadata(null, false);
                        WebSphereRuntimeProductInfoCacheUtil.saveProductInfoCache(wsRuntime, info);
                    } else {
                        // If we didn't regenerate the metadata then we should initialize the product extension
                        // instances, we do this here because it takes too long to run on demand.
                        FeatureListExtMetadata.getInstances(wsRuntime);
                    }

                    monitor.worked(1);

                    if (monitor.isCanceled())
                        return Status.CANCEL_STATUS;
                }
                monitor.done();
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return Constants.JOB_FAMILY.equals(family);
            }
        };
        job.schedule(2000);
    }

    private ISourcePathComputerDelegate[] loadSourcePathComputers() {
        List<ISourcePathComputerDelegate> delegates = new ArrayList<ISourcePathComputerDelegate>(1);
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, SOURCE_PATH_EXTENSION_POINT);

        for (IConfigurationElement ce : cf) {
            ISourcePathComputerDelegate delegate;
            try {
                delegate = (ISourcePathComputerDelegate) ce.createExecutableExtension("class");
                delegates.add(delegate);
            } catch (CoreException e) {
                Trace.logError("Error while creating executable extension for source path computer", e);
            }
        }

        return delegates.toArray(new ISourcePathComputerDelegate[delegates.size()]);
    }

    public ISourcePathComputerDelegate[] getSourcePathComputers() {
        if (sourcePathComputerExtensions == null) {
            sourcePathComputerExtensions = loadSourcePathComputers();
        }
        return sourcePathComputerExtensions;
    }

    //add newly created debug target to the list of debugTargets maintained by Activator
    public void addDebugTarget(IDebugTarget debugTarget) {
        if (debugTarget == null) {
            return;
        }
        synchronized (debugTargets) {
            debugTargets.add(debugTarget);
        }
    }

    //terminate the specified debug target only for a particular server
    public void removeDebugTarget(IDebugTarget debugTarget) {
        if (debugTarget == null) {
            return;
        }
        synchronized (debugTargets) {
            Iterator<IDebugTarget> iterator = debugTargets.iterator();
            while (iterator.hasNext()) {
                IDebugTarget target = iterator.next();
                if (target == debugTarget) {
                    iterator.remove();
                }
            }
        }
    }

    //terminate all the active debug targets
    public void terminateDebugTargets() {
        synchronized (debugTargets) {
            for (IDebugTarget target : debugTargets) {
                try {
                    target.disconnect();
                    target.terminate();
                } catch (DebugException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not disconnect debug target", e);
                }
            }
            debugTargets.clear();
        }
    }

    public static FeatureResolver getFeatureResolver() {
        return instance.resolverTracker.getService();
    }

    public AbstractServerStartupExtension[] getPreStartExtensions() {
        if (preStartExtensions != null)
            return preStartExtensions;

        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.core.serverPreStart");
        List<AbstractServerStartupExtension> startupExtList = new ArrayList<AbstractServerStartupExtension>(1);

        for (IConfigurationElement elem : configElements) {
            AbstractServerStartupExtension startup;
            try {
                startup = (AbstractServerStartupExtension) elem.createExecutableExtension("class");
                startupExtList.add(startup);
            } catch (CoreException e) {
                Trace.logError("Error while creating executable extension for startup", e);
            }
        }

        preStartExtensions = startupExtList.toArray(new AbstractServerStartupExtension[startupExtList.size()]);
        return preStartExtensions;
    }

    public static boolean isAutomaticFeatureDetectionEnabled() {
        return getPreference(PREFERENCE_KEY_ENABLE_AUTOMATIC_FEATURE_DETECTION, PREFERENCE_DEFAULT_VALUE_ENABLE_AUTOMATIC_FEATURE_DETECTION);
    }

    public static void setAutomaticFeatureDetection(boolean enabled) {
        setPreference(PREFERENCE_KEY_ENABLE_AUTOMATIC_FEATURE_DETECTION, enabled);
    }

    public static int getDefaultClassScanning() {
        return getPreference(PREFERENCE_KEY_DEFAULT_CLASS_SCANNING, PREFERENCE_DEFAULT_VALUE_DEFAULT_CLASS_SCANNING);
    }

    public static void setDefaultClassScanning(int value) {
        setPreference(PREFERENCE_KEY_DEFAULT_CLASS_SCANNING, value);
    }

    public static boolean getAutomaticFeatureDetectionEnabledDefault() {
        return PREFERENCE_DEFAULT_VALUE_ENABLE_AUTOMATIC_FEATURE_DETECTION;
    }

    public static int getDefaultClassScanningDefaultValue() {
        String prop = System.getProperty("com.ibm.ws.st.addFeatureDefault", "alwaysAdd");
        if (prop.equals("neverAdd")) {
            return ProjectPrefs.ADD_FEATURE_NEVER;
        } else if (prop.equals("prompt")) {
            return ProjectPrefs.ADD_FEATURE_PROMPT;
        } else {
            return ProjectPrefs.ADD_FEATURE_ALWAYS;
        }
    }
}