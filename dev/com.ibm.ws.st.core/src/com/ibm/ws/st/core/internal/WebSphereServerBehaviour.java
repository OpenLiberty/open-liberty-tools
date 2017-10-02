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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.ResourceManager;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerBehaviourExtension;
import com.ibm.ws.st.common.core.ext.internal.servertype.ServerTypeExtensionFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.core.internal.LibertyConfigSyncConflictHandler.Pair;
import com.ibm.ws.st.core.internal.LibertyConfigSyncConflictHandler.Resolution;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;
import com.ibm.ws.st.core.internal.config.ConfigurationIncludeFilter;
import com.ibm.ws.st.core.internal.config.JVMOptions;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.LibertyRemoteUtilityExecutionDelegate;
import com.ibm.ws.st.core.internal.launch.RemoteStartServer;

@SuppressWarnings("restriction")
public class WebSphereServerBehaviour extends ServerBehaviourDelegate implements IAdaptable {
    private static final String EXTERNAL_APP_PREFIX = "was.external";
    private static final String EXTERNAL_APP_VERSION = "1.0";

    private boolean cleanOnStartup;
    private List<IModule> externalModules = new ArrayList<IModule>(2);

    protected AbstractMonitorThread monitorThread;

    private Properties configSyncInfo = null;
    private Properties remoteConfigSyncInfo = null;
    public static final String CONFIG_SYNC_FILENAME = Constants.CONFIG_SYNC_FILENAME;
    public static final String REMOTE_CONFIG_SYNC_FILENAME = Constants.REMOTE_CONFIG_SYNC_FILENAME;

    public static final int AUTO_PUBLISH_DISABLE = 1;
    public static final int AUTO_PUBLISH_RESOURCE = 2;
    public static final int AUTO_PUBLISH_BUILD = 3;

    private static final String PUBLISH_INFO_PROPERTIES = "publishInfo.properties";
    private static final String LAST_PUBLISH_LOOSE_CFG_SETTING = "last.publish.looseConfig";
    private boolean isPublishLooseCfgModeChanged;

    boolean shownFeaturePromptInLauncher = true;

    final ApplicationStateTracker appStateTracker = new ApplicationStateTracker(); // Tracks the states received from the console
    final Map<String, Integer> appTrackingMap = Collections.synchronizedMap(new HashMap<String, Integer>()); // Tracks the states that the apps are waiting for

    private PublishHelper publishHelper = null;

    protected Process serverCmdStopProcess = null;
    protected final Object syncObj1 = new Object();
    final Object serverStateSyncObj = new Object(); // needed for RTC 101887
    private final ArrayList<String> appsRequireStartCallAfterPublish = new ArrayList<String>();
    private boolean needSyncExteneralModulesAfterPublish = false;

    protected ArrayList<String> overriddenAppsInServerXML; // App entries added by the users to server.xml manually and are overridden in modifyModules()
    protected ArrayList<String> overriddenDropinsApps; // External apps in the dropins folder that are overridden during publish

    private IConfigurationElement[] configConflictHandlers = null;
    private static final String CONFLICT_HANDLER_XP_ID = "com.ibm.ws.st.core.libertyConfigConflictHandler";
    private static final String XP_ATTR_CLASS = "class";

    final HashMap<String, String> remoteConfigVarMap = new HashMap<String, String>(); // Caches the jmx metadata info

    private IDebugTarget debugTarget;
    private int debugPortNum = -1;

    protected AbstractServerBehaviourExtension behaviourExtension = null;

    private static final String WINDOWS_PATH_EXPR = "[a-zA-Z]:[\\\\/].*";
    private static final Pattern windowsPathPattern = Pattern.compile(WINDOWS_PATH_EXPR);

    protected HashMap<IModule, ExcludeSyncModuleInfo> excludeSyncModules = new HashMap<IModule, ExcludeSyncModuleInfo>();

    /**
     * @return the debugTarget
     */
    public IDebugTarget getDebugTarget() {
        return debugTarget;
    }

    /**
     * @param debugTarget the debugTarget to set
     */
    public void setDebugTarget(IDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    /**
     * @return the debugPortNum
     */
    public int getDebugPortNum() {
        return debugPortNum;
    }

    /**
     * @param debugPortNum the debugPortNum to set
     */
    public void setDebugPortNum(int debugPortNum) {
        this.debugPortNum = debugPortNum;
    }

    @Override
    public void initialize(IProgressMonitor monitor) {
        super.initialize(monitor);

        AbstractServerBehaviourExtension ext = getServerTypeExtension();
        if (ext != null) {
            behaviourExtension.initialize(this, monitor);
        }

        startMonitorThread();
        syncExternalModules();

        // 197730 - Initializing config sync files uses network connectivity and should be done in a separate thread
        Job initializeConfigFilesJob = new Job(NLS.bind(Messages.jobInitializeConfigSync, getServer().getName())) {
            @Override
            protected IStatus run(IProgressMonitor mon) {
                initializeConfigSyncPropertyFiles();
                /*
                 * Initialization isn't required because if it fails we'll still initialize
                 * when we do a config sync so just return OK status
                 */
                return Status.OK_STATUS;
            }
        };
        initializeConfigFilesJob.setPriority(Job.LONG);
        initializeConfigFilesJob.schedule();

    }

    /**
     *
     * For Remote servers, initialize the config timestamps to be used for configuration file synchronization
     */
    void initializeConfigSyncPropertyFiles() {
        /*
         * Defect 213613: Adding a module on the last part of the docker/remote server wizard causes error
         * Do not fill local config sync info because it prevents configSync() from detecting newly created config
         * files as updated files in some scenarios. One scenario is when a module is added to the server on the
         * last page of the new remote server wizard. The local configuration files already exist with the application
         * added to the server.xml and the timestamp is already cached so the file is never uploaded to the remote server.
         * This causes the publish after server creation to hang because the server.xml file does not contain the app config.
         */

        ConfigurationFile root = getWebSphereServer().getConfiguration();
        if (root != null) {
            JMXConnection jmx = null;
            try {
                if (!isLocalUserDir())
                    jmx = getWebSphereServer().createJMXConnection();

                //get all the include and config files
                String[] fileLocations = getConfigFileLocations(jmx);

                //applicable only to remote servers, store timestamps for remote files
                if (!isLocalUserDir() && !getTempDirectory().append(REMOTE_CONFIG_SYNC_FILENAME).toFile().exists()) {
                    remoteConfigSyncInfo = new Properties();
                    fillRemoteConfigSyncInfo(fileLocations, jmx);
                    saveProperties(remoteConfigSyncInfo, REMOTE_CONFIG_SYNC_FILENAME);
                }
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to initialize server config sync property files", e);
                }
                remoteConfigSyncInfo = null;
            } finally {
                if (jmx != null)
                    jmx.disconnect();
            }
        }
    }

    /**
     * This method is called whenever the server or configuration change. It's task is
     * to update the list of external modules, i.e. apps that are configured on the server
     * but do not exist in the workspace.
     */
    protected void syncExternalModules() {
        ConfigurationFile cf = getWebSphereServer().getConfiguration();
        if (cf == null)
            return;
        Application[] apps = cf.getApplications();
        IModule[] modules = getServer().getModules();

        // Construct the list of modules to exclude
        HashSet<String> ignoredConfigFileModuleNames = new HashSet<String>();
        HashSet<String> ignoredDropinFileNames = new HashSet<String>();

        Set<IModule> keys = excludeSyncModules.keySet();
        for (IModule key : keys) {
            ExcludeSyncModuleInfo info = excludeSyncModules.get(key);
            HashMap<String, String> properties = info.getProperties();
            if (properties != null) {
                String appName = key.getName();

                if (appName != null) {
                    ignoredConfigFileModuleNames.add(appName);
                }

                String appsDir = properties.get(ExcludeSyncModuleInfo.APPS_DIR);
                if ("dropins".equals(appsDir)) {
                    String appFileName = properties.get(ExcludeSyncModuleInfo.APP_FILE_NAME);
                    if (appFileName != null) {
                        ignoredDropinFileNames.add(appFileName);
                    }
                }
            }
        }

        // build a list of external modules
        List<IModule> newExternalModules = new ArrayList<IModule>(2);
        for (Application app : apps) {
            boolean found = false;

            // In some cases an app may need to be excluded from the apps directory. One case is a Maven project that is integrated with
            // the liberty-maven-plugin. In this case a runtime and server are created specifically for this project and the module is added
            // to the server at the time of server creation so it is not to be confused with an external module.
            String currAppName = app.getName();

            if (ignoredConfigFileModuleNames.contains(currAppName)) {
                continue;
            }

            for (IModule m : modules) {
                if (!m.isExternal() && app.getName().equals(m.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String appName = app.getName();
                String appType = app.getType();

                String type = EXTERNAL_APP_PREFIX;
                if (appType != null && appType.length() > 0)
                    type += "." + appType;
                newExternalModules.add(createExternalModule(appName, appName, type, EXTERNAL_APP_VERSION, null));
            }
        }

        // add apps from dropins
        IPath path = getWebSphereServer().getServerPath().append("dropins"); // TODO - get from config
        File f = path.toFile();
        if (f.exists()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    String name = files[i].getName();

                    if (ignoredDropinFileNames.contains(name))
                        continue;

                    int ind = name.lastIndexOf(".");
                    if (ind == -1) {
                        String appType = name;
                        if (ServerExtensionWrapper.isValidApplicationType(appType)) {
                            String type = EXTERNAL_APP_PREFIX + "." + appType;
                            File[] files2 = files[i].listFiles();
                            if (files2 != null && files2.length > 0) {
                                for (int j = 0; j < files2.length; j++) {
                                    String appName = files2[j].getName();
                                    newExternalModules.add(createExternalModule(appName, appName, type, EXTERNAL_APP_VERSION, null));
                                }
                            }
                        }
                    } else if (ind > 0) {
                        String appName = name.substring(0, ind);
                        String appType = name.substring(ind + 1);
                        String type = EXTERNAL_APP_PREFIX;
                        if (ServerExtensionWrapper.isValidApplicationType(appType))
                            type += "." + appType;
                        newExternalModules.add(createExternalModule(appName, appName, type, EXTERNAL_APP_VERSION, null));
                    }
                }
            }
        }

        // only update external modules if there have been changes. Important to do
        // this because the method will be called on every change to the server and
        // it needs to react fast and not trigger events or UI updates when there
        // are no changes
        boolean changed = false;
        if (newExternalModules.size() != externalModules.size())
            changed = true;
        else {
            int size = externalModules.size();
            for (int i = 0; i < size; i++) {
                IModule m1 = externalModules.get(i);
                IModule m2 = newExternalModules.get(i);
                if (!m1.getName().equals(m2.getName())) {
                    changed = true;
                    break;
                } else if (!m1.getModuleType().equals(m2.getModuleType())) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            externalModules = newExternalModules;
            IModule[] externalModuleArray = externalModules.toArray(new IModule[externalModules.size()]);
            setExternalModules(externalModuleArray);

            // Need to call this so that the ServerPublishInfo gets updated
            if (externalModules.size() > 0)
                getPublishedResourceDelta(externalModuleArray);
        }
    }

    protected void removeExternalModule(IModule module, IProgressMonitor monitor, MultiStatus status) {
        // If this was an application in the config file then modifyModules
        // would have removed it so only need to check for dropins.
        String dropinsName = null;
        IPath path = getWebSphereServer().getServerPath().append("dropins");
        File f = path.toFile();
        if (f.exists()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                monitor.beginTask(NLS.bind(Messages.taskDeleteDropinsApplication, module.getName()), (files.length / 10) + 5);
                try {
                    String appName = module.getName();
                    String fullName = null;
                    String type = module.getModuleType().getId();
                    if (type.startsWith(EXTERNAL_APP_PREFIX)) {
                        type = type.substring(EXTERNAL_APP_PREFIX.length(), type.length());
                        fullName = appName + type;
                    }

                    monitor.worked(1);

                    for (int i = 0; i < files.length; i++) {
                        String name = files[i].getName();
                        if (name.equals(appName)) {
                            dropinsName = appName;
                            break;
                        } else if (name.equals(fullName)) {
                            dropinsName = fullName;
                            break;
                        }
                        if (i > 0 && i % 10 == 0) {
                            if (monitor.isCanceled()) {
                                status.add(Status.CANCEL_STATUS);
                                return;
                            }
                            monitor.worked(1);
                        }
                    }

                    if (monitor.isCanceled()) {
                        status.add(Status.CANCEL_STATUS);
                        return;
                    }

                    if (dropinsName != null) {
                        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
                        IContainer folder = workspaceRoot.getContainerForLocation(path);
                        IResource resource = folder != null ? folder.findMember(dropinsName) : null;
                        if (resource != null) {
                            try {
                                resource.delete(true, monitor);
                            } catch (CoreException e) {
                                status.add(e.getStatus());
                            }
                        } else {
                            File dropinsFile = path.append(dropinsName).toFile();
                            if (dropinsFile.isDirectory()) {
                                IStatus[] ss = org.eclipse.wst.server.core.util.PublishHelper.deleteDirectory(dropinsFile, new SubProgressMonitor(monitor, 4));
                                for (IStatus s : ss)
                                    status.add(s);
                            } else if (!dropinsFile.delete()) {
                                status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorDeleteDropinsApplication, module.getName())));
                            }
                        }
                    }
                } finally {
                    monitor.done();
                }
            }
        }
        return;
    }

    protected void setServerStateImpl(int state) {
        synchronized (serverStateSyncObj) {
            super.setServerState(state);
        }
    }

    protected void setModuleStateImpl(IModule[] module, int state) {
        if (module == null || module.length == 0)
            return;
        if (!ServerExtensionWrapper.isGenericApplicationType(module[0].getModuleType()))
            super.setModuleState(module, state);
    }

    public void setServerAndModuleState(int state) {
        setServerStateImpl(state);

        IModule[] modules = getServer().getModules();
        for (IModule module : modules)
            setModuleStateImpl(new IModule[] { module }, state);
    }

    public void stopMonitorThread() {
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Stop monitor thread");
        if (monitorThread != null) {
            monitorThread.stopMonitor();
            try {
                monitorThread.notify();
            } catch (Exception e) {
                // Do nothing
            }
        }
        monitorThread = null;
    }

    public void startMonitorThread() {
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Start monitor thread");
        // When a server is deleted, it will be removed from ResourceManager.
        // If that's the case, we should not start the monitor thread.
        IServer server = getServer();
        if (monitorThread != null || server.isWorkingCopy() || ResourceManager.getInstance().getServer(server.getId()) == null)
            return;

        if (getWebSphereServer().isLocalSetup())
            monitorThread = new ConsoleMonitorThread(this, serverStateSyncObj, "WebSphere status monitor(console)");
        else
            monitorThread = new JMXMonitorThread(this, serverStateSyncObj, "WebSphere status monitor(JMX)");

        monitorThread.setPriority(Thread.MIN_PRIORITY + 1);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void start(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        preLaunch(monitor);

        synchronized (serverStateSyncObj) {
            if (!getWebSphereServer().isLocalSetup()) {
                // if remote start isn't enabled, throw a core exception to trigger popup dialog informing user it is not installed/enabled
                if (com.ibm.ws.st.common.core.ext.internal.Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) == null) {
                    setServerStateImpl(IServer.STATE_STOPPED);
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptRemoteServerActionsUnavailable));
                }

                if (!getWebSphereServer().getIsRemoteServerStartEnabled()) {
                    setServerStateImpl(IServer.STATE_STOPPED);
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptRemoteServerSettingsDisabled));
                }

                try {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.SSM, "REMOTE EXECUTION START SERVER");
                    RemoteStartServer remoteLauncher = new RemoteStartServer(getServer().getStartTimeout() * 1000, new LibertyRemoteUtilityExecutionDelegate());
                    remoteLauncher.execute(getWebSphereServer(), launchMode, launch, monitor);
                } catch (CoreException ce) {
                    setServerState(IServer.STATE_STOPPED);
                    throw ce;
                }
            }
        }
    }

    public void preLaunch(IProgressMonitor monitor) {
        int curServerState = getServer().getServerState();
        if (curServerState != IServer.STATE_STOPPED) {
            // Do nothing since the server is not stopped.
            return;
        }
        if (monitor.isCanceled()) {
            return;
        }

        // resets the server state monitor and application state monitor
        // also sets server and module state to STOPPED
        stopImpl();

        setServerStateImpl(IServer.STATE_STARTING);
    }

    public void postLaunch(boolean launchSuccessful) {
        if (!launchSuccessful)
            setServerState(IServer.STATE_STOPPED);
    }

    @Override
    public void stop(boolean force) {
        stop(force, new NullProgressMonitor());
    }

    public void stop(boolean force, IProgressMonitor monitor) {
        AbstractServerBehaviourExtension ext = getServerTypeExtension();
        if (ext != null) {
            behaviourExtension.stop(this, force, monitor);
        } else {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unable to load the server stop extension");
        }
    }

    public void stopImpl() {
        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "Resetting JMX Monitor Thread");
        setShownFeaturePromptInLauncher(true);
        if (monitorThread instanceof ConsoleMonitorThread) {
            ((ConsoleMonitorThread) monitorThread).removeProcessListeners();
        }
        setServerAndModuleState(IServer.STATE_STOPPED);
        stopMonitorThread();
        appStateTracker.clear();
        // Start the monitor thread again so that if the server is started outside of the
        // tools we will detect it.
        startMonitorThread();
    }

    public void ensureMonitorRunning() {
        if (monitorThread == null || !monitorThread.isRunning()) {
            stopMonitorThread();
            startMonitorThread();
        }
    }

    /**
     * Terminates the server.
     */
    protected void terminateProcess() {
        try {
            ILaunch launch = getServer().getLaunch();
            if (launch != null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Killing the server process");
                launch.terminate();
            }
        } catch (Exception e) {
            Trace.logError("Error killing the server process", e);
        }
    }

    /**
     * Waits for the runtime server is stopped.
     *
     * @param timeout timeout in milliseconds
     * @return true if the server is in stopped state within the timeout period; false otherwise.
     */
    public boolean waitForServerStop(int timeout) {
        WebSphereServer ws = getWebSphereServer();
        WebSphereRuntime wr = getWebSphereRuntime();
        if (wr != null && ws != null) {
            WebSphereServerInfo info = ws.getServerInfo();
            if (info != null) {
                if (timeout > 0) {
                    long t = 0;
                    if (Trace.ENABLED)
                        t = System.currentTimeMillis();
                    try {
                        int iter = timeout / 50;
                        long lastPingTime = System.currentTimeMillis();
                        long endTime = lastPingTime + timeout;
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Ensure the server is stopped.");
                        while (iter > 0 && wr.getServerStatus(info, iter / 20f, null) != 1) {
                            long time1 = System.currentTimeMillis();
                            if (time1 >= endTime) {
                                iter = -1;
                                break;
                            }
                            long sleepTime = 300 - (time1 - lastPingTime); // we want to ping about once every 0.3 seconds
                            if (sleepTime > 0) {
                                try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException e) {
                                    // nothing to do
                                }
                            }
                            lastPingTime = System.currentTimeMillis();
                            iter = (int) ((endTime - lastPingTime) / 50);
                        }
                        if (Trace.ENABLED) {
                            if (iter <= 0)
                                Trace.trace(Trace.WARNING, "The server may not be fullly stopped after the server process is stopped.");
                            else {
                                Trace.tracePerf("Time spent on waiting for server stop.", t);
                                Trace.trace(Trace.INFO, "The server is stopped.");
                            }
                        }
                        if (iter > 0)
                            return true;
                    } catch (CoreException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "There is an error when determining the server status.", e);
                    }
                }
            }
        }
        return false;
    }

    protected WebSphereRuntime getWebSphereRuntime() {
        if (getServer().getRuntime() == null)
            return null;

        return (WebSphereRuntime) getServer().getRuntime().loadAdapter(WebSphereRuntime.class, null);
    }

    public WebSphereServer getWebSphereServer() {
        if (getServer() == null)
            return null;

        return (WebSphereServer) getServer().loadAdapter(WebSphereServer.class, null);
    }

    public WebSphereServerInfo getWebSphereServerInfo() {
        if (getServer() == null)
            return null;

        return getWebSphereServer().getServerInfo();
    }

    /**
     * Setup for starting the server.
     *
     * @param launch ILaunch
     * @param launchMode String
     * @param monitor IProgressMonitor
     * @throws CoreException if anything goes wrong
     */
    public void preLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        //if ("true".equals(launch.getLaunchConfiguration().getAttribute(ATTR_STOP, "false")))
        //      return;

        //TODO if (getWebSphereRuntime() == null)
        //      throw new CoreException();

        IStatus status = getWebSphereRuntime().validate();
        if (status != null && status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);

        //setRestartNeeded(false);

        // check that ports are free
        ServerPort[] ports = getWebSphereServer().getServerPorts();
        List<ServerPort> usedPorts = new ArrayList<ServerPort>();
        for (ServerPort sp : ports) {
            if (sp.getPort() < 0)
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorPortInvalid, null));
            InetAddress host = null;
            try {
                host = InetAddress.getByName(getServer().getHost());
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not determine InetAddress for host: " + getServer().getHost(), e);
            }
            if (host != null && SocketUtil.isPortInUse(host, sp.getPort(), 3)) {
                usedPorts.add(sp);
            }
        }
        if (usedPorts.size() == 1) {
            ServerPort port = usedPorts.get(0);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorPortInUse,
                                                                                               new String[] { port.getPort() + "", getServer().getName() }), null));
        } else if (usedPorts.size() > 1) {
            Iterator<ServerPort> iterator = usedPorts.iterator();
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            while (iterator.hasNext()) {
                if (!first)
                    sb.append(", ");
                first = false;
                ServerPort sp = iterator.next();
                sb.append(sp.getPort());
            }

            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorPortsInUse,
                                                                                               new String[] { sb.toString(), getServer().getName() }), null));
        }

        // TODO check that there is only one app for each context root
        /*
         * iterator = configuration.getWebModules().iterator();
         * List contextRoots = new ArrayList();
         * while (iterator.hasNext()) {
         * WebModule module = (WebModule) iterator.next();
         * String contextRoot = module.getPath();
         * if (contextRoots.contains(contextRoot))
         * throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorDuplicateContextRoot, new String[] { contextRoot }), null));
         *
         * contextRoots.add(contextRoot);
         * }
         */

        stopMonitorThread();
        setServerRestartState(false);

        setServerStateImpl(IServer.STATE_STARTING);
        IModule[] modules = getServer().getModules();
        for (IModule module : modules)
            setModuleStateImpl(new IModule[] { module }, IServer.STATE_STOPPED);
        setMode(launchMode);
    }

    /**
     * Protected method from superclass exposed so that it can be used by server extensions.
     *
     * @see ServerBehaviourDelegate#getResources(IModule[])
     */
    @Override
    public IModuleResource[] getResources(IModule[] module) {
        return super.getResources(module);
    }

    /**
     * Protected method from superclass exposed so that it can be used by server extensions.
     *
     * @see ServerBehaviourDelegate#getPublishedResourceDelta(IModule[])
     */
    @Override
    public IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module) {
        return super.getPublishedResourceDelta(module);
    }

    public final IPath getRootPublishFolder(boolean shared) {
        WebSphereServerInfo info = getWebSphereServerInfo();
        if (info == null)
            return null;

        if (shared)
            return info.getUserDirectory().getSharedAppsPath();
        return info.getServerAppsPath();
    }

    public boolean isChanged(PublishUnit pu) {
        if (pu.getDeltaKind() != ServerBehaviourDelegate.NO_CHANGE)
            return true;
        List<PublishUnit> children = pu.getChildren();
        if (children == null)
            return false;
        for (PublishUnit u : children) {
            if (isChanged(u))
                return true;
        }
        return false;
    }

    protected boolean isApplicationPublishRequired(int kind, PublishUnit app) {
        if (kind != IServer.PUBLISH_INCREMENTAL && kind != IServer.PUBLISH_AUTO)
            return true;
        return isChanged(app);
    }

    public void checkPublishedModules(IProgressMonitor monitor) {
        if (publishHelper == null) {
            publishHelper = new PublishHelper(this);
        }
        publishHelper.checkPublishedModules(monitor);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void publishModules(int kind, List modules, List deltaKind2, MultiStatus multi, IProgressMonitor monitor) {
        checkPublishedModules(monitor);
        shownFeaturePromptInLauncher = false;

        // Allow server extensions to do any preparation required for publish
        AbstractServerBehaviourExtension ext = getServerTypeExtension();
        ext.prePublishModules(this, monitor);

        int size = modules.size();
        if (size == 0) {
            // Make sure that any configuration changes get picked up
            handleAutoConfigSyncJob(0);
            return;
        }

        if (monitor.isCanceled())
            return;

        PublishUnit[] puList = new PublishUnit[size];
        ArrayList<PublishUnit> addedApps = new ArrayList<PublishUnit>(size);
        ArrayList<PublishUnit> deletedApps = new ArrayList<PublishUnit>(size);
        ArrayList<PublishUnit> otherApps = new ArrayList<PublishUnit>(size);

        for (int i = 0; i < size; i++)
            puList[i] = new PublishUnit((IModule[]) modules.get(i), ((Integer) deltaKind2.get(i)).intValue());

        // set parents & children
        int numberOfApps = 0;
        for (int i = 0; i < size; i++) {
            IModule[] module = (IModule[]) modules.get(i);

            if (shouldIgnorePublishRequest(module[module.length - 1]))
                continue;

            if (module.length == 1) {
                numberOfApps++;
                switch (puList[i].getDeltaKind()) {
                    case ServerBehaviourDelegate.ADDED:
                        addedApps.add(puList[i]);
                        break;
                    case ServerBehaviourDelegate.REMOVED:
                        deletedApps.add(puList[i]);
                        break;
                    default:
                        otherApps.add(puList[i]);
                }
            }

            for (int j = 0; j < size; j++) {
                if (i != j) {
                    IModule[] module2 = (IModule[]) modules.get(j);
                    if (module2.length == module.length + 1) {
                        if (shouldIgnorePublishRequest(module2[module2.length - 1]))
                            continue;
                        boolean found = true;
                        for (int k = 0; k < module.length; k++) {

                            try {
                                // When a project is deleted from the workspace, the module is a DeletedModule.
                                // If it is a child module, the parent is also a DeletedModule, so we need to check
                                // the id too.
                                if (module[k] != (module2[k]) && !module[k].getId().equals(module2[k].getId())) {
                                    found = false;
                                    break;
                                }
                            } catch (Throwable t) {
                                // 163296: SVT: NPE in Problem Occurred Dialog Box when Publishing to Liberty on zOS encountered a problem
                                // Due to defect 163296, add some log traces if we ever have
                                if (module[k] == null) {
                                    Trace.logError("module[k] is null", null);
                                } else {
                                    Trace.logError("module[k]'s id is " + module[k].getId(), null);
                                }

                                if (module2[k] == null) {
                                    Trace.logError("module2[k] is null", null);
                                } else {
                                    Trace.logError("module2[k]'s id is " + module2[k].getId(), null);
                                }
                                found = false;
                                break;
                            }
                        }
                        if (found) {
                            puList[i].addChild(puList[j]);
                            puList[j].setParent(puList[i]);
                        }
                    }
                }
            }
        }

        //This is how ServerBehaviourDelegate calculates the amount of work. int size = 2000 + 3500 * moduleList.size() + 500 * tasks.length;
        //Although each application should have at least 7000 ticks, we can only set around 3000 for each publisher.  Otherwise, it will reach
        //100% before the task is finished.

        int totalWork = 3500 * size;
        int perAppWork;
        if (numberOfApps != 0) {
            perAppWork = totalWork / numberOfApps;
        } else {
            perAppWork = totalWork;
        }

        Properties publishInfo = new Properties();
        FileUtil.loadProperties(publishInfo, getTempDirectory().append(PUBLISH_INFO_PROPERTIES));
        String sLooseCfg = publishInfo.getProperty(LAST_PUBLISH_LOOSE_CFG_SETTING);
        boolean isLooseConfig = getWebSphereServer().isLooseConfigEnabled();
        if (sLooseCfg == null) {
            if (isLooseConfig == true)
                isPublishLooseCfgModeChanged = false;
            else
                isPublishLooseCfgModeChanged = true;
        } else {
            boolean lastLoose = sLooseCfg.equalsIgnoreCase("true");
            isPublishLooseCfgModeChanged = isLooseConfig == lastLoose ? false : true;
        }
        if (isPublishLooseCfgModeChanged) {
            publishInfo.put(LAST_PUBLISH_LOOSE_CFG_SETTING, String.valueOf(isLooseConfig));
            FileUtil.saveCachedProperties(publishInfo, getTempDirectory().append(PUBLISH_INFO_PROPERTIES));
        }

        appTrackingMap.clear();
        boolean isRunOnServerWait = false;

        appsRequireStartCallAfterPublish.clear(); // should have been cleared at the end of publish.
        needSyncExteneralModulesAfterPublish = false;
        String appName;

        // Force a touch on the launch configuration file to force a refresh on the source path computer
        // by the debug framework in order to update the source path lookup.
        updateDebugSourcePath(deltaKind2);
        JMXConnection jmxConnection = null;
        try {
            if (getServer().getServerState() == IServer.STATE_STARTED) {
                try {
                    jmxConnection = getWebSphereServer().createJMXConnection();
                } catch (Exception e) {
                    Trace.logError("Could not connect to the server via JMX.  Exiting from publish.", e);
                    multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPublishJMX));
                    return;
                }
            }

            // can't publish to remote server if server isn't started or JMX connection unavailable
            if (jmxConnection == null && !isLocalUserDir()) {
                if (getServer().getServerState() == IServer.STATE_STARTED) {
                    multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPublishJMX));
                }
                return; // returning will leave the server in republish state so that the publish can occur when a connection becomes available
            }

            PublishWithErrorHandler handler = Activator.getPublishWithErrorHandler();
            if (!getWebSphereServer().isPublishWithError() && handler != null) {
                ArrayList<String> errorModules = new ArrayList<String>();
                ArrayList<PublishUnit> errorApps = new ArrayList<PublishUnit>();

                //check Added modules
                validatePublishUnits(addedApps, errorModules, errorApps);
                //check other Modules
                validatePublishUnits(otherApps, errorModules, errorApps);

                // when published Application has error call publishWithError handler
                if (!errorModules.isEmpty()) {

                    handler.handlePublishWithError(errorModules, getWebSphereServer());
                    // when user selects cancel remove only the Application with error
                    // when user select ok continue with publish .
                    if (handler.getReturnCode() != 0) {
                        addedApps.removeAll(errorApps);
                        otherApps.removeAll(errorApps);
                    }
                }
            }
            for (PublishUnit pu : addedApps) {
                isRunOnServerWait = true;
                appName = pu.getModuleName();
                addAppToTrackingMapIfNeeded(appName, ApplicationStateTracker.STARTED | ApplicationStateTracker.FAILED_START);
                MultiStatus mStatus = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, NLS.bind(Messages.publishModule, pu.getModule()[0].getName()), null);
                boolean needMonitorConsole = publishApplication(kind, pu, mStatus, perAppWork, jmxConnection, monitor);
                if (!needMonitorConsole) {
                    appTrackingMap.remove(appName);
                }
                multi.add(mStatus);
            }

            for (PublishUnit pu : otherApps) {
                appName = pu.getModuleName();
                appStateTracker.andOpAppState(appName, ApplicationStateTracker.NEED_RESTART_APP); // clear all states except 14W
                int expectedAppState = ApplicationStateTracker.UPDATED | ApplicationStateTracker.FAILED_UPDATE |
                                       ApplicationStateTracker.STARTED | ApplicationStateTracker.FAILED_START;
                addAppToTrackingMapIfNeeded(appName, expectedAppState);
                MultiStatus mStatus = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, NLS.bind(Messages.publishModule, pu.getModule()[0].getName()), null);
                boolean needMonitorConsole = publishApplication(kind, pu, mStatus, perAppWork, jmxConnection, monitor);
                if (!needMonitorConsole || !isChanged(pu) || getServer().getModuleState(pu.getModule()) != IServer.STATE_STARTED) {
                    appTrackingMap.remove(appName);
                } else {
                    isRunOnServerWait = true;
                }
                multi.add(mStatus);
            }

            for (PublishUnit pu : deletedApps) {
                appName = pu.getModuleName();
                MultiStatus mStatus = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, NLS.bind(Messages.publishModule, pu.getModule()[0].getName()), null);
                addAppToTrackingMapIfNeeded(appName, ApplicationStateTracker.STOPPED | ApplicationStateTracker.FAILED_STOP);
                boolean needMonitorConsole = publishApplication(kind, pu, mStatus, perAppWork, jmxConnection, monitor);
                if (!needMonitorConsole || !isChanged(pu) || getServer().getModuleState(pu.getModule()) != IServer.STATE_STARTED) {
                    appTrackingMap.remove(appName);
                }

                if (pu.getModule()[0].isExternal()) {
                    removeExternalModule(pu.getModule()[0], new SubProgressMonitor(monitor, perAppWork), mStatus);
                    needSyncExteneralModulesAfterPublish = true;
                }

                multi.add(mStatus);
            }

            if (needSyncExteneralModulesAfterPublish)
                syncExternalModules();

            appsRequireStartCallAfterPublish.addAll(getOverriddenDropinsApps());
            if (!appsRequireStartCallAfterPublish.isEmpty()) {
                if (jmxConnection != null) { // jmxConnection won't be null if the server is started
                    for (String name : appsRequireStartCallAfterPublish) {
                        try {
                            jmxConnection.startApplication(name);
                        } catch (Exception e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Cannot use JMX start applications.", e);
                            appTrackingMap.remove(name);
                            multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.warningApplicationNotStarted, name))); // need error to display the message
                        }
                    }
                } else {
                    for (String name : appsRequireStartCallAfterPublish) {
                        appTrackingMap.remove(name);
                        multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.warningApplicationNotStarted, name)));
                    }
                }
            }

            appsRequireStartCallAfterPublish.clear();
            getOverriddenAppsInServerXML().clear();
            getOverriddenDropinsApps().clear();

            SubProgressMonitor waitForStatusMonitor = new SubProgressMonitor(monitor, 2000);
            waitForStatusMonitor.beginTask(Messages.publishWaitingForStatus, 1000);
            waitForStatusMonitor.subTask(Messages.publishWaitingForStatus);
            waitForStatusMonitor.worked(100);

            int numberOfAppsToWait = appTrackingMap.size();
            int waitTicksPerApp = numberOfAppsToWait == 0 ? 900 : 900 / numberOfAppsToWait;

            // wait for all started, updated and stopped messages before exit
            long waitStartTime = System.currentTimeMillis();
            long waitTime = Activator.getInstance().getPreferencePublishWaitTimeMS();

            // When start the server, a publish is called.  We need to check if the server is started in the loop,
            // otherwise, it can never exit.
            int counter = 0; //print the trace once every 2 seconds.

            try {
                while (getServer().getServerState() == IServer.STATE_STARTED &&
                       multi.getSeverity() != IStatus.ERROR &&
                       !appTrackingMap.isEmpty() &&
                       (waitTime < 0 || System.currentTimeMillis() < (waitStartTime + waitTime)) &&
                       !monitor.isCanceled()) {

                    int appsProcessed = numberOfAppsToWait - appTrackingMap.size();
                    numberOfAppsToWait -= appsProcessed;
                    if (appsProcessed > 0)
                        waitForStatusMonitor.worked(appsProcessed * waitTicksPerApp);

                    try {
                        if (Trace.ENABLED) {
                            if (counter == 0) {
                                Trace.trace(Trace.INFO, "waiting for message from applications: " + appTrackingMap.toString());
                                counter = 20;
                            } else
                                counter--;
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // nothing to do
                    }
                }
            } finally {
                waitForStatusMonitor.done();
            }

            for (String key : appStateTracker.getAppNames()) {
                if (appStateTracker.hasApplicationState(key, ApplicationStateTracker.FAILED_START | ApplicationStateTracker.FAILED_UPDATE)) {
                    multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.warningApplicationNotStarted, key)));
                }
                if (appStateTracker.hasApplicationState(key, ApplicationStateTracker.FAILED_STOP)) {
                    multi.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, key + ": " + Messages.errorApplicationStop));
                }
            }

            long t = Activator.getInstance().getPreferenceRunOnServerDelayTimeMS();
            if (getServer().getServerState() == IServer.STATE_STARTED && isRunOnServerWait && t > 0 && multi.getSeverity() != IStatus.ERROR) {
                if (!monitor.isCanceled()) {
                    try {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Sleep " + t + " before do run on server.");
                        Thread.sleep(t);
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Finish sleep. May do run on server.");
                    } catch (InterruptedException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Interrupted during sleep", e);
                    }
                }
            }

            int sev = multi.getSeverity();
            if (sev != IStatus.ERROR && sev != IStatus.CANCEL)
                setServerPublishState(IServer.PUBLISH_STATE_NONE);
        } finally {
            if (jmxConnection != null)
                jmxConnection.disconnect();
        }
    }

    private void validatePublishUnits(List<PublishUnit> moduleList, List<String> errorModules, List<PublishUnit> errorApps) {
        for (PublishUnit pu : moduleList) {
            // check for errors only if it has changed or added
            if (isChanged(pu)) {
                errorModules.addAll(validatePublishUnit(pu));
                if (!errorModules.isEmpty()) {
                    errorApps.add(pu);
                }
            }
        }
    }

    /**
     * check if the publish unit and any of its referenced modules has validation error
     */
    private HashSet<String> validatePublishUnit(PublishUnit pu) {
        HashSet<String> errorModules = new HashSet<String>();
        try {
            IModule module = pu.getModule()[0];
            if (module != null && !module.isExternal())
                validate(module.getProject(), errorModules);
        } catch (CoreException e) {
            Trace.logError("Validation failed for Module" + pu.getModuleName(), e);
        }
        return errorModules;
    }

    private void validate(IProject project, HashSet<String> errorModules) throws CoreException {
        validate(project, errorModules, new HashSet<IProject>());
    }

    private void validate(IProject project, Set<String> errorModules, Set<IProject> visitedProjects) throws CoreException {

        //return if there is no IProject associated with the module
        if (project == null)
            return;

        // Protection from circular dependencies in projects
        if (visitedProjects.contains(project)) {
            return;
        }
        visitedProjects.add(project);

        // check the module
        // first check org.eclipse.wst.validation.problemmarkers
        IMarker[] markers = project.getProject().findMarkers("org.eclipse.wst.validation.problemmarker",
                                                             true, IResource.DEPTH_INFINITE);
        for (int i = 0; i < markers.length; i++) {
            if (((Integer) markers[i].getAttribute(IMarker.SEVERITY)).intValue() == IMarker.SEVERITY_ERROR) {
                if (!errorModules.contains(project.getName()))
                    errorModules.add(project.getName());
            }
        }
        // now check org.eclipse.jdt.core.problem
        markers = project.findMarkers("org.eclipse.jdt.core.problem", true,
                                      IResource.DEPTH_INFINITE);
        for (int i = 0; i < markers.length; i++) {
            if (((Integer) markers[i].getAttribute(IMarker.SEVERITY)).intValue() == IMarker.SEVERITY_ERROR) {
                if (!errorModules.contains(project.getName()))
                    errorModules.add(project.getName());
            }
        }
        IProject[] projects = project.getReferencedProjects();
        for (IProject projectz : projects) {
            validate(projectz, errorModules, visitedProjects);
        }
    }

    protected boolean publishApplication(int kind, PublishUnit pu, MultiStatus status, int perAppWork, JMXConnection jmxConnection, IProgressMonitor monitor) {
        AbstractServerBehaviourExtension ext = getServerTypeExtension();

        boolean requireConsoleOutputBeforeComplete = false;
        if (pu.getParent() == null && !shouldIgnorePublishRequest(pu.getModule()[0])) { // top level application
            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, perAppWork);

            // remove application notification listener
            if (ServerBehaviourDelegate.REMOVED == pu.getDeltaKind() && monitorThread instanceof JMXMonitorThread) {
                ((JMXMonitorThread) monitorThread).removeAppListener(pu.getModuleName());
            }

            ServerExtensionWrapper[] publishers = getWebSphereServer().getServerExtensions();
            String taskName = NLS.bind(Messages.publishingModule, pu.getModule()[0].getName());
            subMonitor.beginTask(taskName, 100);
            subMonitor.setTaskName(taskName);
            for (ServerExtensionWrapper se : publishers) {
                // Determine if the current publisher should be run
                if (ext != null) {
                    if (!ext.shouldRunPublisherForServerType(this, pu, se)) {
                        continue;
                    }
                }

                if (se.supportsApplicationType(pu.getModule()[0].getModuleType())) {
                    se.initServerBehaviour(this);
                    se.setJmxConnection(jmxConnection);
                    se.setIsLooseConfig(getWebSphereServer().isLooseConfigEnabled());
                    if (!isApplicationPublishRequired(kind, pu) && !isPublishLooseCfgModeChanged) {
                        setModulesPublishStates(pu, IServer.PUBLISH_STATE_NONE);
                        // not sure if we want to add a status to say it doesn't need publish
                        continue;
                    }

                    if (isPublishLooseCfgModeChanged && se.needToActOnLooseConfigModeChange(pu) && pu.getDeltaKind() != ServerBehaviourDelegate.ADDED) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Publish for loose config mode change.");
                        se.handleLooseConfigModeChange(kind, pu, status, subMonitor);
                    } else {
                        SubProgressMonitor childMonitor = new SubProgressMonitor(subMonitor, 15);
                        se.prePublishApplication(kind, pu, status, childMonitor);
                        childMonitor.done();
                        childMonitor = new SubProgressMonitor(subMonitor, 70);
                        se.publishModuleAndChildren(kind, pu, status, childMonitor);
                        childMonitor.done();
                        childMonitor = new SubProgressMonitor(subMonitor, 15);
                        se.postPublishApplication(kind, pu, status, childMonitor);

                        requireConsoleOutputBeforeComplete = requireConsoleOutputBeforeComplete || se.requireConsoleOutputBeforePublishComplete(kind, pu, status, childMonitor);
                    }
                }
            }

            // add application notification listener for newly added app
            /**
             * If there was an error then skip this because there won't be an application MBean
             * and the publish will be stuck waiting indefinitely for one to be created until the
             * user cancels the progress monitor.
             */
            if (status.getSeverity() != IStatus.ERROR && ServerBehaviourDelegate.ADDED == pu.getDeltaKind() && monitorThread instanceof JMXMonitorThread) {
                ((JMXMonitorThread) monitorThread).addAppListenerOnPublish(pu.getModuleName(), this, monitor);
            }

            appStateTracker.andOpAppState(pu.getModuleName(), ~ApplicationStateTracker.NEED_RESTART_APP); // clear the 14W message
            subMonitor.done();
        }
        return requireConsoleOutputBeforeComplete;
    }

    private void setModulesPublishStates(PublishUnit pu, int state) {
        if (pu == null)
            return;
        setModulePublishState(pu.getModule(), state);
        List<PublishUnit> children = pu.getChildren();
        if (children != null) {
            for (PublishUnit u : children)
                setModulesPublishStates(u, state);
        }
    }

    /**
     * This method is to test if a server extension supports any of the applications passed in.
     * It is checking the application, not individual module.
     *
     * @param sew
     * @param modules
     * @return
     */
    protected boolean isServerExtensionInterested(ServerExtensionWrapper sew, List<?> modules) {
        Iterator<?> itr = modules.iterator();
        while (itr.hasNext()) {
            IModule[] module = (IModule[]) itr.next();
            if (sew.supports(module[0].getModuleType()))
                return true;
        }
        return false;
    }

    /**
     * Returns whether the server is going to be cleaned on next startup or not.
     *
     * @return <code>true</code> if the server is going to be cleaned on next startup,
     *         and <code>false</code> otherwise
     */
    public boolean isCleanOnStartup() {
        return cleanOnStartup;
    }

    /**
     * Sets whether the server should be cleaned (--clean) on next startup. Automatically
     * reset to <code>false</code> during every launch.
     *
     * @param clean <code>true</code> if the server should be cleaned on next startup,
     *            and <code>false</code> otherwise.
     */
    public void setCleanOnStartup(boolean clean) {
        cleanOnStartup = clean;
    }

    public void setRestartState(boolean value) {
        setServerRestartState(value);
    }

    @Override
    public IPath getTempDirectory() {
        return super.getTempDirectory();
    }

    public final List<IModule[]> getPublishedModules() {
        return super.getAllModules();
    }

    @Override
    public void dispose() {
        if (monitorThread instanceof ConsoleMonitorThread) {
            ((ConsoleMonitorThread) monitorThread).removeProcessListeners();
        }

        terminateDebugTarget();

        stopMonitorThread();
    }

    public void setModulePublishState(int state, IModule[] module) {
        super.setModulePublishState(module, state);
    }

    public void setWebSphereServerPublishState(int state) {
        super.setServerPublishState(state);
    }

    protected synchronized IStatus syncConfig(JMXConnection jmxConnection) {
        long time = System.currentTimeMillis();
        if (configSyncInfo == null) { //very unlikely that configSyncInfo will be null here
            configSyncInfo = new Properties();
            FileUtil.loadProperties(configSyncInfo, getTempDirectory().append(CONFIG_SYNC_FILENAME));
        }

        List<String> allFeatures = getWebSphereServer().getConfiguration().getAllFeatures();

        if (!allFeatures.isEmpty()) {
            // Create a feature set and look for the JMX features instead of using WebSphereServer.isFeatureConfigured() since
            // this is more efficient than getting the list of features twice.
            FeatureSet fs = new FeatureSet(getWebSphereRuntime(), allFeatures);
            if (fs.resolve("localConnector") == null && fs.resolve("restConnector") == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "JMX feature is not enabled on the server for application update");

                // For local, the config file is saved directly and we don't make
                // JMX as a required if the user doesn't publish applications, so if JMX is not enabled,
                // we will return a warning.  The user will not see this warning. For remote,
                // we will need to throw an error.
                int level = isLocalUserDir() ? IStatus.WARNING : IStatus.ERROR;
                return new Status(level, Activator.PLUGIN_ID, Messages.publishConfigSyncNoJMXConnector);
            }
            try {
                List<String> updatedFiles = new ArrayList<String>(5);
                List<String> deletedFiles = new ArrayList<String>(5);
                String[] configFileLocations = getConfigFileLocations(jmxConnection);

                // remove the deleted includes in the cache first
                Enumeration<?> cachedSet = configSyncInfo.keys();
                while (cachedSet.hasMoreElements()) {
                    String k = (String) cachedSet.nextElement();
                    boolean found = false;
                    for (int i = 0; i < configFileLocations.length; i++) {
                        if (k.equals(configFileLocations[i])) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        configSyncInfo.remove(k);
                        deletedFiles.add(k);
                    }
                }

                // check if there is any update
                fillConfigSyncInfo(updatedFiles, configFileLocations);
                long currentTime = System.currentTimeMillis();
                if (Trace.ENABLED)
                    Trace.tracePerf("Sync config calculate", time);
                time = currentTime;

                if (jmxConnection != null && (!updatedFiles.isEmpty() || !deletedFiles.isEmpty())) {
                    if (isLocalUserDir()) {
                        deletedFiles = getMappedConfigPaths(deletedFiles, jmxConnection);
                        updatedFiles = getMappedConfigPaths(updatedFiles, jmxConnection);
                        notifyChanges(jmxConnection, updatedFiles, deletedFiles);
                    }
                    // for remote servers we need to upload or delete the config files
                    else {
                        try {
                            if (remoteConfigSyncInfo == null) {
                                remoteConfigSyncInfo = new Properties();
                                FileUtil.loadProperties(remoteConfigSyncInfo, getTempDirectory().append(REMOTE_CONFIG_SYNC_FILENAME));
                            }

                            // check if remote files were updated before we push updates ourselves
                            ArrayList<String> remotelyUpdatedFiles = fillRemoteConfigSyncInfo(configFileLocations, jmxConnection);
                            String localConfigRoot = null;
                            String remoteConfigRoot = null;
                            URI configRoot = getWebSphereServer().getConfiguration().getURI();
                            if (configRoot != null) {
                                localConfigRoot = new File(configRoot).toString().replace("\\", "/");
                                remoteConfigRoot = resolveRemoteFilePath(localConfigRoot, jmxConnection);
                            }
                            Resolution conflict = null;

                            // if config files were updated on the remote machine we need to decide how to proceed
                            // ie. overwrite the remote changes, overwrite the local changes, etc.
                            if (!remotelyUpdatedFiles.isEmpty()) {
                                if (Trace.ENABLED) {
                                    Trace.trace(Trace.INFO, "Detected remote configuration files were updated by external factors");
                                    Trace.trace(Trace.INFO, "Updated files on remote system: " + remotelyUpdatedFiles.toString());
                                }
                                if (configConflictHandlers == null) {
                                    configConflictHandlers = Platform.getExtensionRegistry().getConfigurationElementsFor(CONFLICT_HANDLER_XP_ID);
                                    for (IConfigurationElement configurationElement : configConflictHandlers) {
                                        if (Trace.ENABLED) {
                                            Trace.trace(Trace.INFO, "configurationElement class=[" + configurationElement.getAttribute(XP_ATTR_CLASS) + "]");
                                        }
                                    }
                                }
                                if (configConflictHandlers != null && configConflictHandlers.length > 0 && !Boolean.getBoolean("wtp.autotest.noninteractive")) {
                                    Object o = configConflictHandlers[0].createExecutableExtension("class");
                                    if (o instanceof LibertyConfigSyncConflictHandler) {
                                        ArrayList<Pair> conflictFiles = new ArrayList<Pair>(); // the list of local files that have conflicts
                                        for (String remoteFile : remotelyUpdatedFiles) {
                                            for (String localFile : updatedFiles) {
                                                if (resolveRemoteFilePath(localFile, jmxConnection).equals(remoteFile))
                                                    conflictFiles.add(new Pair(localFile, remoteFile));
                                            }
                                        }
                                        // Its very unlikely that conflictFiles will be empty at this point
                                        if (!conflictFiles.isEmpty()) {
                                            LibertyConfigSyncConflictHandler handler = ((LibertyConfigSyncConflictHandler) o);
                                            conflict = handler.handleConflict(conflictFiles, getTempDirectory(), jmxConnection, remoteConfigRoot);
                                        }
                                    }
                                }
                            }
                            String remoteUserPath = null;

                            if (getWebSphereServerInfo().getUserDirectory().getRemoteUserPath() != null)
                                remoteUserPath = getWebSphereServerInfo().getUserDirectory().getRemoteUserPath().toOSString().replace("\\", "/");
                            // its more safe to use getPath () instead of project.getLocation(), if the location of the runtime changes,
                            // existing default user directory project is deleted and recreated at the new location
                            // due to which project.getLocation() return null for the new location
                            String localUserDir = getWebSphereServer().getUserDirectory().getPath().toOSString().replace("\\", "/");

                            // if there was no conflict or if the user selected to overwrite the remote files then upload the remote files and do a notify
                            if (conflict == null || Resolution.OVERWRITE_REMOTE.equals(conflict) || Resolution.MERGE.equals(conflict)) {
                                ArrayList<String> updatedRemoteFiles = new ArrayList<String>(updatedFiles.size());
                                ArrayList<String> deletedRemoteFiles = new ArrayList<String>(deletedFiles.size());

                                for (String file : updatedFiles) {
                                    File f = new File(file);
                                    String remotePath = file.replace("\\", "/").replace(localUserDir, remoteUserPath);
                                    jmxConnection.uploadFile(f, remotePath, false);
                                    updatedRemoteFiles.add(remotePath);
                                }

                                //delete the files on remote server
                                for (String file : deletedFiles) {
                                    String remotePath = file.replace("\\", "/").replace(localUserDir, remoteUserPath);
                                    if (remotePath.startsWith(remoteUserPath)) {
                                        jmxConnection.deleteFile(remotePath);
                                    }
                                    deletedRemoteFiles.add(remotePath);
                                }

                                notifyChanges(jmxConnection, updatedRemoteFiles, deletedRemoteFiles);

                                //delete the remote file from remoteConfigSyncInfo
                                for (String filePath : deletedRemoteFiles) {
                                    if (remoteConfigSyncInfo.containsKey(filePath))
                                        remoteConfigSyncInfo.remove(filePath);
                                }

                                // refresh the local config sync info in case local files were modified during a merge
                                fillConfigSyncInfo(updatedFiles, configFileLocations);
                                Job refreshJob = new Job(Messages.promptUpdateServerConfiguration) {

                                    @Override
                                    protected IStatus run(IProgressMonitor arg0) {
                                        // Refresh user directory project
                                        IProject userDirProj = getWebSphereServer().getUserDirectory().getProject();
                                        try {
                                            if (userDirProj.exists())
                                                userDirProj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                                        } catch (Exception e) {
                                            Trace.logError("Refreshing user directory project failed", e);
                                            return new Status(IStatus.WARNING, Activator.PLUGIN_ID, NLS.bind(Messages.errorRefreshingFolder,
                                                                                                             userDirProj.getLocation().toOSString()), e);
                                        }
                                        return Status.OK_STATUS;
                                    }

                                    @Override
                                    public boolean belongsTo(Object family) {
                                        return Constants.JOB_FAMILY.equals(family);
                                    }
                                };
                                refreshJob.schedule(5000); // wait a few seconds before scheduling the job so we get out of the config sync operation
                            } else {
                                throw new Exception(Messages.publishConfigSyncCanceled);
                            }
                            // update our remote config info after we uploaded the config files
                            fillRemoteConfigSyncInfo(configFileLocations, jmxConnection);
                        } catch (Exception e) {
                            // wrap the exception for remote server problems with a more helpful message
                            throw new Exception(Messages.remotePublishConfigSyncFailed, e);
                        }
                    } // end remote config sync
                }
                if (Trace.ENABLED)
                    Trace.tracePerf("Sync config JMX", time);
            } catch (Exception e) {
                Trace.logError("Exception when syncing server config", e);
                configSyncInfo = null;
                remoteConfigSyncInfo = null;
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.publishConfigSyncError, e);
            }
        }

        saveProperties(configSyncInfo, CONFIG_SYNC_FILENAME);
        saveProperties(remoteConfigSyncInfo, REMOTE_CONFIG_SYNC_FILENAME);

        return new Status(IStatus.OK, Activator.PLUGIN_ID, Messages.publishConfigSyncSuccess);

    }

    private void fillConfigSyncInfo(List<String> updatedFileList, String[] fileLocations) {
        if (fileLocations == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "No configuration files detected");
            return;
        }
        if (Trace.ENABLED)
            Trace.trace(Trace.DETAILS, "Filling configuration file timestamps");
        for (String key : fileLocations) {
            File file = new File(key);
            key = key.replace("\\", "/");
            String cachedTS = configSyncInfo.getProperty(key);
            String s = Long.toString(file.lastModified());
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "File: " + key + " TS: " + s + " cached TS: " + cachedTS);
            if (cachedTS == null || !s.equals(cachedTS)) {
                configSyncInfo.put(key, s);
                if (updatedFileList != null)
                    updatedFileList.add(key);
            }
        }
    }

    private ArrayList<String> fillRemoteConfigSyncInfo(String[] fileLocations, JMXConnection jmx) {

        ArrayList<String> remoteUpdatedFileList = new ArrayList<String>(3);
        String remoteUserPath = null;
        if (getWebSphereServerInfo().getUserDirectory().getRemoteUserPath() != null)
            remoteUserPath = getWebSphereServerInfo().getUserDirectory().getRemoteUserPath().toOSString().replace("\\", "/"); //remote wlp.user.dir
        String localUserDir = getWebSphereServer().getUserDirectory().getPath().toOSString().replace("\\", "/"); //workspace dir

        if (fileLocations == null || jmx == null)
            return remoteUpdatedFileList;

        for (String key : fileLocations) {
            // Update this code if we need to handle config files that aren't located within the server config directory
            // Replace local path with remote path
            key = key.replace("\\", "/");
            if (key.startsWith(localUserDir))
                key = key.replace(localUserDir, remoteUserPath);
            String lastModified = null;
            try {
                CompositeData metadata = (CompositeData) jmx.getMetadata(key, "t");
                Date lastModifiedDate = null;
                if (metadata != null) {
                    lastModifiedDate = (Date) metadata.get("lastModified");
                    lastModified = lastModifiedDate.toString();
                }
            } catch (Exception e) {
                Trace.logError("Couldn't retrieve server user directory using JMX for: " + key, e);
            }

            String cachedTS = remoteConfigSyncInfo.getProperty(key);
            if (lastModified != null && (cachedTS == null || !lastModified.equals(cachedTS))) {
                remoteConfigSyncInfo.put(key, lastModified);
                remoteUpdatedFileList.add(key);
            }
        }

        return remoteUpdatedFileList;
    }

    private String[] getConfigFileLocations(JMXConnection jmx) throws Exception {
        Set<String> fileLocations = new HashSet<String>();
        List<String> includeFiles = new ArrayList<String>();
        ConfigurationFile root = getWebSphereServer().getConfiguration();
        List<ConfigurationFile> files = new ArrayList<ConfigurationFile>();
        final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
        root.getAllConfigFiles(files, includeFilter);

        String[] unresolvedIncludes = root.getAllUnresolvedIncludes();

        //add all includes list to the fileLocation list
        for (ConfigurationFile f : files) {
            URI uri = f.getURI();
            if (uri != null)
                includeFiles.add((new File(uri)).toString().replace('\\', '/'));
        }

        //add all unresolved includes list to the fileLocation list
        for (int i = 0; i < unresolvedIncludes.length; i++) {
            includeFiles.add(unresolvedIncludes[i].replace('\\', '/'));
        }

        WebSphereServerInfo serverInfo = getWebSphereServerInfo();
        UserDirectory userDir = serverInfo == null ? null : serverInfo.getUserDirectory();
        if (isLocalUserDir()) { // for local server just return all the resolved include files
            fileLocations.addAll(includeFiles);
            if (unresolvedIncludes.length > 0)
                fileLocations.removeAll(Arrays.asList(unresolvedIncludes));
        }
        // if we're dealing with a remote server resolve all the remote include variables and replace them with local paths
        else if (userDir != null && userDir.getRemoteUserPath() != null) {
            for (String file : includeFiles) {
                String fileName = resolveLocalFilePath(file, jmx);
                if (fileName != null) {
                    fileName = fileName.replace('\\', '/');
                    if (!fileLocations.contains(fileName) && (new File(fileName)).exists()) {
                        fileLocations.add(fileName);
                    }
                }
            }
        }

        //add server.xml file to fileLocation list
        URI rootURI = root.getURI();
        if (rootURI != null) {
            String serverXMLLocation = new File(rootURI).toString();
            if (serverXMLLocation != null) {
                serverXMLLocation = serverXMLLocation.replace('\\', '/');
                fileLocations.add(serverXMLLocation);
            }
        }

        //add all the config files i.e bootstrap.properties, jvm.options and server.env
        if (getWebSphereServer().getServerPath() != null && getWebSphereServer().getServerPath().toFile().exists() && serverInfo != null) {
            if (serverInfo.getBootstrap() != null) {
                URI fileLocation = serverInfo.getBootstrap().getURI();
                if (fileLocation != null)
                    fileLocations.add(new File(fileLocation).toString().replace("\\", "/"));
            }

            Collection<JVMOptions> jvmOptionsFiles = serverInfo.getJVMOptionsFiles();
            for (JVMOptions jvmOptionsFile : jvmOptionsFiles) {
                URI fileLocation = jvmOptionsFile.getURI();
                if (fileLocation != null)
                    fileLocations.add(new File(fileLocation).toString().replace("\\", "/"));
            }

            if (serverInfo.getServerEnv() != null) {
                URI fileLocation = serverInfo.getServerEnv().getURI();
                if (fileLocation != null)
                    fileLocations.add(new File(fileLocation).toString().replace("\\", "/"));
            }
        }
        return fileLocations.toArray(new String[fileLocations.size()]);
    }

    public static boolean isAbsolutePath(String path) {
        // Check if a path is absolute.  The Path.isAbsolute method does not work well
        // on linux if the path is a windows path so first check for an absolute windows
        // path.
        if (windowsPathPattern.matcher(path).matches()) {
            return true;
        }
        return (new Path(path)).isAbsolute();
    }

    public String resolveLocalFilePath(String remoteFileName, JMXConnection jmxConnection) {
        if (remoteFileName == null)
            return null;

        String fileName = remoteFileName;
        CompositeData metadata = null;
        String remoteUserPath = null;
        if (getWebSphereServerInfo().getUserDirectory().getRemoteUserPath() != null)
            remoteUserPath = getWebSphereServerInfo().getUserDirectory().getRemoteUserPath().toOSString().replace("\\", "/"); //remote wlp/user dir
        String localUserDir = getWebSphereServer().getUserDirectory().getPath().toOSString().replace("\\", "/"); // local workspace dir

        // if file is absolute path
        if (isAbsolutePath(remoteFileName)) {
            fileName = remoteFileName.replace("\\", "/");
            if (fileName.startsWith(remoteUserPath)) {
                fileName = fileName.replace(remoteUserPath, localUserDir);
            }
        }

        //case when file is a variable
        else if (jmxConnection != null && ConfigVarsUtils.containsReference(remoteFileName)) {
            try {
                fileName = resolveLocalFilePath(resolveConfigVar(remoteFileName, jmxConnection), jmxConnection);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.logError("Unable to resolve variable: " + metadata, e);
            }
        }

        //case when file is a relative path or name of the file
        else if (!(new File(remoteFileName).isAbsolute())) {
            //if its relative path
            if (remoteFileName.contains("/") || remoteFileName.startsWith("../")) {
                URI base = new File(localUserDir).toURI();
                //resolve the relative path against the metadata server config folder
                URI resolvedPath = base.resolve(remoteFileName);
                fileName = resolvedPath.getPath();
            }
        }
        return fileName;
    }

    private String resolveRemoteFilePath(String localFileName, JMXConnection jmxConnection) throws Exception {

        //if remoteUser directory is null then no point of going forward into the method
        if (getWebSphereServerInfo().getUserDirectory().getRemoteUserPath() == null || localFileName == null) {
            return null;
        }

        String fileName = localFileName;
        CompositeData metadata = null;
        String remoteUserDir = null;
        if (getWebSphereServerInfo().getUserDirectory().getRemoteUserPath() != null)
            remoteUserDir = getWebSphereServerInfo().getUserDirectory().getRemoteUserPath().toOSString().replace("\\", "/"); //remote wlp/user dir
        String localUserDir = getWebSphereServer().getUserDirectory().getPath().toOSString().replace("\\", "/"); // local workspace dir

        // if file is absolute path
        if (new File(localFileName).isAbsolute()) {
            fileName = localFileName.replace("\\", "/");
            if (fileName.startsWith(localUserDir)) {
                // convert the local path to a remote path
                fileName = fileName.replace(localUserDir, remoteUserDir);
            }
        }

        //case when file is a variable
        else if (jmxConnection != null && ConfigVarsUtils.containsReference(localFileName)) {
            try {
                // just get the remote path of the variable
                fileName = resolveConfigVar(localFileName, jmxConnection);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.logError("Unable to resolve variable: " + metadata, e);
            }
        }

        //case when file is a relative path or name of the file
        else if (!(new File(localFileName).isAbsolute())) {
            //if its relative path
            if (localFileName.contains("/") || localFileName.startsWith("../")) {
                URI base = new File(remoteUserDir).toURI();
                //resolve the relative path against the remote user directory
                URI resolvedPath = base.resolve(localFileName);
                fileName = resolvedPath.getPath();
            }
        }
        return fileName;
    }

    public void saveProperties(Properties prop, String fileName) {
        if (prop == null) // nothing needs to update
            return;
        OutputStream out = null;
        try {
            out = new FileOutputStream(getTempDirectory().append(fileName).toFile());
            prop.store(out, null);
        } catch (Exception e) {
            Trace.logError("Could not write to the properties file: " + fileName, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Trace.logError("Could not close the properties file: " + fileName, e);
                }
            }
        }
    }

    public void startAutoConfigSyncJob() {
        IServer server = getServer();
        int state = server.getServerPublishState();
        if (state != IServer.PUBLISH_STATE_FULL) {
            if (server.getServerState() != IServer.STATE_STARTED) {
                // With local setup or loose config enabled the user is editing the configuration directly
                // so the publish state should not be set to republish even though the server is stopped
                if (!getWebSphereServer().isLocalSetup() && !isLocalUserDir()) {
                    setWebSphereServerPublishState(IServer.PUBLISH_STATE_INCREMENTAL);
                }
                return;
            }
            setWebSphereServerPublishState(IServer.PUBLISH_STATE_INCREMENTAL);
            if (server.getAttribute(Server.PROP_AUTO_PUBLISH_SETTING, AUTO_PUBLISH_RESOURCE) == AUTO_PUBLISH_DISABLE)
                return;
            int time = server.getAttribute(Server.PROP_AUTO_PUBLISH_TIME, 15);
            handleAutoConfigSyncJob(time);
        }
    }

    /**
     * @param delayTime time delay in seconds. A negative number indicates to cancel the job.
     */
    synchronized void handleAutoConfigSyncJob(int delayTime) {
        Job[] jobs = Job.getJobManager().find(Constants.JOB_FAMILY);
        Job syncJob = null;
        if (jobs != null && jobs.length > 0) {
            for (Job j : jobs) {
                if (j instanceof AutoConfigSyncJob) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Reuse Auto Config Sync Job");
                    syncJob = j;
                    break;
                }
            }
        }
        if (delayTime < 0) {
            if (syncJob != null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Cancel Auto Config Sync Job");
                syncJob.cancel();
            }
            return;
        }

        if (syncJob == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Create a new Auto Config Sync Job");
            syncJob = new AutoConfigSyncJob();
        }
        int state = syncJob.getState();
        if (state == Job.SLEEPING) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Reschedule Auto Config Sync Job");
            syncJob.wakeUp(delayTime * 1000);
        } else if (state != Job.WAITING) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Schedule Auto Config Sync Job");
            syncJob.schedule(delayTime * 1000);
        }
    }

    public class AutoConfigSyncJob extends Job {
        public AutoConfigSyncJob() {
            super("WebSphere Configuration Sync Job");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Auto Config Sync job starting");

            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;

            if (getServer().getServerState() != IServer.STATE_STARTED)
                return Status.OK_STATUS;

            JMXConnection jmxConnection = null;
            IStatus status;
            try {
                jmxConnection = getWebSphereServer().createJMXConnection();
                status = syncConfig(jmxConnection);
            } catch (Exception e) {
                Trace.logError("Exception while syncing server config", e);
                status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.publishConfigSyncError, e);
            } finally {
                if (jmxConnection != null)
                    jmxConnection.disconnect();
            }

            if (status.getSeverity() != IStatus.ERROR && status.getSeverity() != IStatus.CANCEL) {
                setWebSphereServerPublishState(IServer.PUBLISH_STATE_NONE);
            }

            return status;
        }

        @Override
        public boolean belongsTo(Object family) {
            return Constants.JOB_FAMILY.equals(family);
        }
    }

    @Override
    public boolean canRestartModule(IModule[] module) {
        if (module == null || module.length != 1)
            return false;
        ServerExtensionWrapper[] extensions = getWebSphereServer().getServerExtensions();
        for (ServerExtensionWrapper se : extensions) {
            if (se.supportsApplicationType(module[0].getModuleType())) {
                se.initServerBehaviour(this);
                return se.canRestartModule(module);
            }
        }
        return false;
    }

    @Override
    public void startModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
        if (module == null || module.length == 0) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "startModule(): module is null or it is empty");
            return;
        }

        if (monitor.isCanceled())
            return;

        JMXConnection jmxConnection = null;
        try {
            jmxConnection = getWebSphereServer().createJMXConnection();
            jmxConnection.startApplication(module[0].getName());
        } catch (Exception e) {
            Trace.logError("Failed to start application: " + module[0].getName(), e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorApplicationStart, e));
        } finally {
            if (jmxConnection != null)
                jmxConnection.disconnect();
        }
    }

    @Override
    public void stopModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
        if (module == null || module.length == 0) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "stopModule(): module is null or it is empty");
            return;
        }

        if (monitor.isCanceled())
            return;

        JMXConnection jmxConnection = null;
        try {
            jmxConnection = getWebSphereServer().createJMXConnection();
            jmxConnection.stopApplication(module[0].getName());
        } catch (Exception e) {
            Trace.logError("Failed to stop application: " + module[0].getName(), e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorApplicationStop, e));
        } finally {
            if (jmxConnection != null)
                jmxConnection.disconnect();
        }
    }

    @Override
    public void restartModule(IModule[] module, IProgressMonitor monitor) throws CoreException {
        if (module == null || module.length == 0) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "restartModule(): module is null or it is empty");
            return;
        }

        if (monitor.isCanceled())
            return;

        JMXConnection jmxConnection = null;
        try {
            jmxConnection = getWebSphereServer().createJMXConnection();
            jmxConnection.restartApplication(module[0].getName());
        } catch (Exception e) {
            Trace.logError("Failed to restart application: " + module[0].getName(), e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorApplicationRestart, e));
        } finally {
            if (jmxConnection != null)
                jmxConnection.disconnect();
        }
    }

    public OutOfSyncModuleInfo checkModuleConfigOutOfSync(IModule module) {
        if (module == null) {
            return null;
        }

        if (excludeSyncModules.containsKey(module)) {
            return null;
        }

        ServerExtensionWrapper[] extensions = getWebSphereServer().getServerExtensions();
        for (ServerExtensionWrapper se : extensions) {
            if (se.supportsApplicationType(module.getModuleType())) {
                OutOfSyncModuleInfo info = se.checkModuleConfigOutOfSync(module);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Track the application state during a publish operation. Initially the state
     * is set to the expected state after the publish. Once the application
     * actually reaches that state the state is cleared.
     */
    public class ApplicationStateTracker {
        public static final int STARTED = 1;
        public static final int UPDATED = 2;
        public static final int STOPPED = 4;
        public static final int FAILED_START = 8;
        public static final int FAILED_STOP = 16;
        public static final int FAILED_UPDATE = 32;
        public static final int NEED_RESTART_APP = 64; // the app is picked up in the config before the file is copied

        private final Hashtable<String, Integer> appStates = new Hashtable<String, Integer>();

        public ApplicationStateTracker() {
            // do nothing
        }

        Set<String> getAppNames() {
            return appStates.keySet();
        }

        protected void addApplicationState(String applicationName, int state) {
            if (applicationName == null)
                return;

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Application message received: " + applicationName + ":" + state);

            synchronized (appTrackingMap) {
                if (!appTrackingMap.isEmpty()) { // meaning a publish is running

                    Integer trackState = appTrackingMap.get(applicationName);
                    if (trackState != null) {
                        int trackedValue = trackState.intValue();
                        if ((state & trackedValue) != 0) {
                            appTrackingMap.remove(applicationName);
                        }
                    }
                }

                Integer i = appStates.get(applicationName);
                if (i != null) {
                    appStates.put(applicationName, Integer.valueOf(i.intValue() | state));
                } else {
                    appStates.put(applicationName, Integer.valueOf(state));
                }
            }

        }

        protected boolean hasApplicationState(String applicationName, int state) {
            if (applicationName == null)
                return false;
            Integer temp = appStates.get(applicationName);
            if (temp == null)
                return false;
            return (temp.intValue() & state) != 0;
        }

        /**
         * Does an bit wise and operation on the application state
         *
         * @param applicationName
         * @param state
         */
        protected void andOpAppState(String applicationName, int state) {
            if (applicationName == null)
                return;
            Integer i = appStates.get(applicationName);
            if (i != null) {
                appStates.put(applicationName, Integer.valueOf(i.intValue() & state));
            }
        }

        protected void clear() {
            appStates.clear();
        }
    }

    private void addAppToTrackingMapIfNeeded(String appName, int state) {
        synchronized (appTrackingMap) {
            if (!appStateTracker.hasApplicationState(appName, state)) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Add application to trackingMap: " + appName);
                Integer i = appTrackingMap.get(appName);
                if (i != null) {
                    appTrackingMap.put(appName, Integer.valueOf(i.intValue() | state));
                } else {
                    appTrackingMap.put(appName, Integer.valueOf(state));
                }
            } else {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Application message already received: " + appName);
            }
        }
    }

    public boolean isAppStateTrackerHasState(String applicationName, int state) {
        if (appStateTracker == null)
            return false;
        return appStateTracker.hasApplicationState(applicationName, state);
    }

    boolean isServerCmdStopProcessRunning() {
        synchronized (syncObj1) {
            if (serverCmdStopProcess == null)
                return false;
            try {
                serverCmdStopProcess.exitValue();
                serverCmdStopProcess = null;
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            }
        }
    }

    public void addAppRequireCallStartAfterPublish(String appName) {
        if (appName != null)
            appsRequireStartCallAfterPublish.add(appName);
    }

    public void setSyncExternalModulesAfterPublish() {
        needSyncExteneralModulesAfterPublish = true;
    }

    @Override
    public void restart(String launchMode) throws CoreException {
        restart(launchMode, new NullProgressMonitor());
    }

    public void restart(String launchMode, IProgressMonitor progressMonitor) throws CoreException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, 100);
        IStatus status = getServerTypeExtension().canRestart(this);
        if (!status.isOK()) {
            throw new CoreException(status);
        }

        monitor.subTask(Messages.taskGenericStopServer);
        stop(true, monitor.newChild(20));

        IServer cServer = getServer();
        int stopTimeOut = getServer().getStopTimeout() * 4; // we loop every 0.25 second.
        int i = 8; // Trace every 2 seconds
        // Canceling the restart job will not be able get out of this loop, but we may be safe that
        // the server will stop and it should exit.  We also use the stopTimeOut to make sure it exits.
        // If bug  https://bugs.eclipse.org/bugs/show_bug.cgi?id=388030 is fixed, we don't need to
        // override this method.
        while (cServer.getServerState() != IServer.STATE_STOPPED && stopTimeOut > 0) {
            if (Trace.ENABLED) {
                if (i == 0) {
                    i = 8;
                    Trace.trace(Trace.INFO, "Waiting for server to stop during server restart.");
                } else {
                    i--;
                }
            }
            stopTimeOut--;
            try {
                Thread.sleep(250);
                if (monitor.isCanceled()) {
                    throw new CoreException(Status.CANCEL_STATUS);
                }
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        if (monitor.isCanceled()) {
            throw new CoreException(Status.CANCEL_STATUS);
        }
        monitor.worked(10);

        shownFeaturePromptInLauncher = true;
        monitor.subTask(Messages.taskGenericStartServer);
        ILaunchConfiguration launchConfig = cServer.getLaunchConfiguration(true, monitor.newChild(5));
        launchConfig.launch(launchMode, monitor.newChild(65));
    }

    @SuppressWarnings("rawtypes")
    protected void updateDebugSourcePath(List deltaKind) {
        // Force a touch on the launch configuration file to force a refresh on the source path computer
        // by the debug framework in order to update the source path lookup.
        ILaunch launch = getServer().getLaunch();
        if (launch == null || deltaKind == null || !launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE)) {
            return;
        }
        Iterator deltaKindIter = deltaKind.iterator();
        while (deltaKindIter.hasNext()) {
            int curDeltaKind = ((Integer) deltaKindIter.next()).intValue();
            if (curDeltaKind == ADDED || curDeltaKind == REMOVED) {
                // Touch the launch configuration.
                ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
                if (launchConfig != null) {
                    try {
                        ILaunchConfigurationWorkingCopy launchConfigWc = launchConfig.getWorkingCopy();
                        launchConfigWc.doSave();
                        // Exit the loop here since we only need to update the launch configuration once.
                        break;
                    } catch (CoreException e) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Cannot force a touch on the launch configuration. Source path will not updated.");
                        }
                    }
                }
            }
        }
    }

    public boolean shouldShownFeaturePromptInLauncher() {
        return shownFeaturePromptInLauncher;
    }

    /**
     * @param shownFeaturePromptInLauncher the shownFeaturePromptInLauncher to set
     */
    public void setShownFeaturePromptInLauncher(boolean shownFeaturePromptInLauncher) {
        this.shownFeaturePromptInLauncher = shownFeaturePromptInLauncher;
    }

    public List<String> getOverriddenAppsInServerXML() {
        if (overriddenAppsInServerXML == null)
            overriddenAppsInServerXML = new ArrayList<String>();
        return overriddenAppsInServerXML;
    }

    public List<String> getOverriddenDropinsApps() {
        if (overriddenDropinsApps == null)
            overriddenDropinsApps = new ArrayList<String>();
        return overriddenDropinsApps;
    }

    public void addProcessListeners(final IProcess newProcess) {
        // Monitor thread needs to be started here so that we can add the process listeners
        if (monitorThread == null)
            startMonitorThread();

        if (monitorThread instanceof ConsoleMonitorThread)
            ((ConsoleMonitorThread) monitorThread).addProcessListeners(newProcess);
    }

    /**
     * Uses JMX to query the server for config variable values.
     *
     * @param var variable to resolve (eg. ${server.config.dir})
     * @param jmx jmx connection to use
     * @return the current value assigned to the variable
     * @throws Exception
     */
    public String resolveConfigVar(String var, JMXConnection jmx) throws Exception {
        String host = jmx.getHost();
        String port = jmx.getPort();
        String key = (host == null || port == null) ? null : host + ":" + port + ":" + var;
        synchronized (remoteConfigVarMap) {
            if (key != null && remoteConfigVarMap.containsKey(key)) {
                String val = remoteConfigVarMap.get(key);
                if (val != null)
                    return val;
            }

            String path = null;
            try {
                CompositeData metadata = (CompositeData) jmx.getMetadata(var, "a");
                path = (String) metadata.get("fileName");
            } catch (Exception e) {
                throw new Exception(NLS.bind(Messages.errorRemoteConfigResolution, var), e);
            }

            if (path == null) {
                throw new Exception(NLS.bind(Messages.errorRemoteConfigResolution, var));
            }
            path = path.replace("\\", "/");
            if (key != null) {
                remoteConfigVarMap.put(key, path);
            }
            return path;
        }
    }

    protected void clearRemoteConfigVarMap() {
        synchronized (remoteConfigVarMap) {
            if (!remoteConfigVarMap.isEmpty())
                remoteConfigVarMap.clear();
        }
    }

    public void setInternalMode(String curMode) {
        setMode(curMode);
    }

    public boolean isDebugAttached(int curDebugPort, IServer server) throws Exception {

        if (getDebugTarget() != null && !getDebugTarget().isDisconnected()
            && !getDebugTarget().isTerminated()) {
            return true;
        }

        boolean isDebugAttached = false;

        String curHost = server.getHost();

        if (curDebugPort > 0 && curHost != null) {
            isDebugAttached = isDebugConnected(curDebugPort);
        }
        return isDebugAttached;
    }

    public boolean isDebugConnected(int curDebugPort) {
        if (curDebugPort <= 0) {
            return false;
        }

        ILaunch[] curLaunches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
        if (curLaunches != null) {
            for (int i = 0; i < curLaunches.length; i++) {
                ILaunch curLaunch = curLaunches[i];

                // Make sure the launch is for this server
                ILaunchConfiguration curLaunchConfig = curLaunch.getLaunchConfiguration();
                if (curLaunchConfig == null) {
                    continue;
                }
                try {
                    IServer curServer = ServerUtil.getServer(curLaunchConfig);
                    if (curServer == null || !curServer.equals(getServer())) {
                        continue;
                    }
                } catch (CoreException e) {
                    Trace.logError("Failed trying get the launch configuration server to detect if debug already connected: " + curLaunchConfig.getName(), e);
                    continue;
                }

                IDebugTarget[] debugTargets = curLaunch.getDebugTargets();

                // There should only be one debugger attached at a time to a server, therefore
                // if a debug target exists and is not disconnected, then

                for (int j = 0; j < debugTargets.length; j++) {
                    IDebugTarget curDebugTarget = debugTargets[j];
                    if (curDebugTarget != null && !curDebugTarget.isTerminated() && !curDebugTarget.isDisconnected()) {
                        if (getDebugPortNum() >= 0 && curDebugPort == getDebugPortNum()) {
                            // Find if the debug port is in use to determine if the debugger
                            // is attached.
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    //case when server instance is deleted without stopping
    public void terminateDebugTarget() {
        try {
            if (debugTarget != null) {
                Activator.getInstance().removeDebugTarget(debugTarget);
                if (debugTarget.canDisconnect()) {
                    debugTarget.disconnect();
                    debugTarget.terminate();
                    debugTarget = null;
                }
            }
        } catch (DebugException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not disconnect debug target", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public IStatus canStop() {
        AbstractServerBehaviourExtension ext = getServerTypeExtension();
        IStatus result = null;
        if (ext != null) {
            result = behaviourExtension.canStop(this);

            // If the IStatus was not null, perform behaviour, otherwise use the default behaviour
            if (result != null) {
                return result;
            }
        } else {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not load the can stop server extension");
        }
        return super.canStop();
    }

    public boolean canCleanOnStart() {
        AbstractServerBehaviourExtension ext = getServerTypeExtension();
        if (ext != null) {
            return ext.canCleanOnStart();
        }
        return true;
    }

    private void notifyChanges(JMXConnection jmxConnection, final List<String> updatedFiles, final List<String> deletedFiles) throws Exception {
        // If there were no changes, then just return
        // The updatedFiles list includes new and changed files
        if (updatedFiles.isEmpty() && deletedFiles.isEmpty()) {
            return;
        }

        // if the server supports configuration update notifications register a listener for better status
        if (!jmxConnection.isLocalConnection() && jmxConnection.isMBeanExists(ConfigurationListener.OBJECT_NAME)) {
            ConfigurationFile root = getWebSphereServerInfo().getConfigRoot();
            boolean addConfigListener = false;
            List<String> combinedList = new ArrayList<String>();
            combinedList.addAll(updatedFiles);
            combinedList.addAll(deletedFiles);

            // add configListener only if the file modified is server.xml or any of the include files
            for (String file : combinedList) {
                if (!addConfigListener) {
                    String fileName = new Path(file).lastSegment();
                    if (fileName.equals(root.getName())) {
                        addConfigListener = true;
                        break;
                    }
                    //if root not found, check the include files
                    for (ConfigurationFile includeFile : root.getAllIncludedFiles()) {
                        if (includeFile.getName().equals(fileName)) {
                            addConfigListener = true;
                            break;
                        }
                    }
                }
            }

            if (!addConfigListener) {
                jmxConnection.notifyFileChanges(null, updatedFiles, deletedFiles);
            } else {
                final ArrayList<Map<String, Object>> notificationsList = new ArrayList<Map<String, Object>>(3);
                final ConfigurationListener configListener = new ConfigurationListener();
                configListener.setNotificationList(notificationsList);
                JMXConnection jmx = null;
                try {
                    jmx = getWebSphereServer().createJMXConnection();
                    jmx.addConfigListener(configListener);
                    jmx.notifyFileChanges(null, updatedFiles, deletedFiles);
                    final JMXConnection connection = jmx;
                    Job configUpdateNotificationJob = new Job(Messages.jobRefreshRuntimeMetadata) {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                boolean isDone = false;
                                long timeout = System.currentTimeMillis() + 40000; // 40 secs allocated for sync operation
                                while (!isDone && System.currentTimeMillis() < timeout) {
                                    for (Map<String, Object> userData : notificationsList) {
                                        if (Trace.ENABLED)
                                            Trace.trace(Trace.DETAILS, "Config notification -> Name: " + userData.get("name") + " Status: " + userData.get("status"));
                                        isDone = ((Boolean) userData.get("status")).booleanValue();
                                    }
                                    Thread.sleep(500);
                                }

                                // config sync SUCCESSFUL
                                if (isDone) {
                                    if (Trace.ENABLED)
                                        Trace.trace(Trace.DETAILS, "Configuration sync has successfully completed.");
                                }
                                // config sync TIMED OUT
                                else if (System.currentTimeMillis() > timeout) {
                                    if (Trace.ENABLED)
                                        Trace.trace(Trace.DETAILS, "Configuration sync has timed out. ");
                                }
                                // config sync FAILED
                                else {
                                    if (Trace.ENABLED)
                                        Trace.trace(Trace.DETAILS, "Configuration sync has failed");
                                    MessageHandler handler = Activator.getMessageHandler();
                                    if (handler != null) {
                                        handler.handleMessage(MessageHandler.MessageType.ERROR, Messages.publishErrorTitle, Messages.publishConfigSyncError);
                                    }
                                }
                            } catch (Exception e) {
                                if (Trace.ENABLED) {
                                    Trace.trace(Trace.WARNING, "An exception was thrown while waiting for configuration update notification from the server.", e);
                                }
                            } finally {
                                try {
                                    connection.removeConfigListener(configListener);
                                } catch (Exception e) {
                                    if (Trace.ENABLED)
                                        Trace.trace(Trace.DETAILS, "Configuration listener was not found during remove");
                                } finally {
                                    connection.disconnect();
                                }
                            }
                            return Status.OK_STATUS;
                        }
                    };

                    configUpdateNotificationJob.setPriority(Job.LONG);
                    configUpdateNotificationJob.schedule();
                } catch (Exception e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "An exception was thrown setting up the configuration listener.", e);
                    }
                    // If there is no exception, the JMXConnection will be cleaned up in the job
                    if (jmx != null) {
                        jmx.disconnect();
                    }
                }
            }
        } else {
            jmxConnection.notifyFileChanges(null, updatedFiles, deletedFiles);
        }

    }

    private List<String> getMappedConfigPaths(List<String> paths, JMXConnection jmx) {
        if (getWebSphereServer().isLocalSetup()) {
            return paths;
        }
        List<String> mappedPaths = new ArrayList<String>(paths.size());
        for (String path : paths) {
            String mappedPath = null;
            try {
                mappedPath = resolveRemoteFilePath(path, jmx);
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to map the following local path to a remote path: " + path, e);
                }
            }
            if (mappedPath == null) {
                // mapping failed (the path may not be within the user directory)
                mappedPath = path;
            }
            mappedPaths.add(mappedPath);
        }
        return mappedPaths;
    }

    private AbstractServerBehaviourExtension getServerTypeExtension() {
        if (behaviourExtension == null) {
            behaviourExtension = ServerTypeExtensionFactory.getServerBehaviourExtension(getWebSphereServer().getServerType());
            if (behaviourExtension == null) {
                behaviourExtension = new BaseLibertyBehaviourExtension();
            }
        }

        return behaviourExtension;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        AbstractServerBehaviourExtension behaviourExt = getServerTypeExtension();
        if (adapter.isInstance(behaviourExt)) {
            return behaviourExt;
        }
        return null;
    }

    public boolean isLocalUserDir() {
        if (getWebSphereServer().isLocalSetup()) {
            return true;
        }
        AbstractServerBehaviourExtension ext = getServerTypeExtension();
        if (ext != null) {
            return ext.isLocalUserDir(this);
        }
        return false;
    }

    public HashMap<IModule, ExcludeSyncModuleInfo> getExcludeSyncModules() {
        return excludeSyncModules;
    }

    public void waitForServerStart(IServer server, IProgressMonitor monitor) {
        int timeout = 0;
        int MAX = server.getStartTimeout() * 2;
        while (timeout++ < MAX && !monitor.isCanceled()) {
            if (server.getServerState() == IServer.STATE_STARTED) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e2) {
                timeout = MAX;
            }
        }
    }

}