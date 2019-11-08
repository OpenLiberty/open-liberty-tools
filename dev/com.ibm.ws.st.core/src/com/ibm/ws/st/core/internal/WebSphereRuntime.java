/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.core.IJavaRuntime;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.model.RuntimeDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.st.core.internal.ProcessHelper.ProcessResult;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.config.SchemaHelper;
import com.ibm.ws.st.core.internal.crypto.CustomPasswordEncryptionInfo;
import com.ibm.ws.st.core.internal.generation.FeatureListCoreMetadata;
import com.ibm.ws.st.core.internal.generation.FeatureListExtMetadata;
import com.ibm.ws.st.core.internal.generation.IMetadataGenerator;
import com.ibm.ws.st.core.internal.generation.Metadata;
import com.ibm.ws.st.core.internal.generation.SchemaMetadata;
import com.ibm.ws.st.core.internal.launch.UtilityLaunchConfigurationDelegate;

public class WebSphereRuntime extends RuntimeDelegate implements IJavaRuntime, IMetadataGenerator {
    private static final String SHARED_FOLDER = Constants.USER_FOLDER + "/" + Constants.SHARED_FOLDER;
    private static final String SHARED_CONFIG_FOLDER = SHARED_FOLDER + "/" + Constants.CONFIG_FOLDER;
    private static final String SHARED_APP_FOLDER = SHARED_FOLDER + "/" + Constants.APPS_FOLDER;
    private static final String SHARED_RESOURCES_FOLDER = SHARED_FOLDER + "/" + Constants.RESOURCES_FOLDER;
    private static final String SERVERS_FOLDER = Constants.USER_FOLDER + "/" + Constants.SERVERS_FOLDER;
    private static final String SECURITY_UTILITY = "securityUtility";
    private static final String ENCODE = "encode";
    private static final String SU_OPTION_LISTCUSTOM = "--listCustom";

    public static final String RUNTIME_MARKER = "lib/versions/WebSphereApplicationServer.properties";
    public static final String OPEN_RUNTIME_MARKER = "lib/versions/openliberty.properties";

    protected static final String PROP_VM_INSTALL_TYPE_ID = "vm-install-type-id";
    protected static final String PROP_VM_INSTALL_ID = "vm-install-id";

    protected static final String PROP_USER_DIRS = "user-dirs";
    public static final String PROP_USER_DIRECTORY = "userDirectory";

    protected static final String PROP_USER_DIR_NAME_MAP = "user-dir-name-map";
    private static final String USER_DIR_METADATA_PATH = "OLT__UserDirectory__";
    private static byte userDirCounter = 0;

    protected List<WebSphereServerInfo> serverInfo;
    protected List<UserDirectory> userDirCache;
    protected List<String> customEncryptionAlgoList = null;
    protected Map<String, CustomPasswordEncryptionInfo> customPasswordEncryptionInfoMap = null;
    protected int serverCacheHash = -1;
    protected int userDirHash = -1;

    // Cache for runtime version.
    protected String runtimeVersion = null;

    // Cache for runtime edition
    protected String runtimeEdition = null;

    private SchemaHelper schemaHelper = null;

    private WebSphereRuntimeClasspathHelper classpathHelper;

    IPath savedMetadataDirectoryInCaseLocationIsMoved = null;

    //enum can be extended to add JAVA EE 8 support
    enum JAVAEESUPPORT {
        JAVAEE6(6.0f),
        JAVAEE7(7.0f),
        JAVAEE8(8.0f);

        private final float version;

        JAVAEESUPPORT(float version) {
            this.version = version;
        }

        public float getVersion() {
            return version;
        }
    }

    private JAVAEESUPPORT earSupported = null;

    public WebSphereRuntime() {
        SchemaLocationProvider.setTempRuntime(this);
    }

    protected String getVMInstallTypeId() {
        return getAttribute(PROP_VM_INSTALL_TYPE_ID, (String) null);
    }

    protected String getVMInstallId() {
        return getAttribute(PROP_VM_INSTALL_ID, (String) null);
    }

    @Override
    public boolean isUsingDefaultJRE() {
        return getVMInstallTypeId() == null;
    }

    @Override
    public IVMInstall getVMInstall() {
        if (getVMInstallTypeId() == null)
            return JavaRuntime.getDefaultVMInstall();
        try {
            IVMInstallType vmInstallType = JavaRuntime.getVMInstallType(getVMInstallTypeId());
            IVMInstall[] vmInstalls = vmInstallType.getVMInstalls();
            int size = vmInstalls.length;
            String id = getVMInstallId();
            for (int i = 0; i < size; i++) {
                if (id.equals(vmInstalls[i].getId()))
                    return vmInstalls[i];
            }
        } catch (Exception e) {
            Trace.logError("Could not resolve VM install", e);
        }
        return null;
    }

    @Override
    protected void initialize() {
        // save the current meta data directory path to eliminate
        // re-generating the meta data in runtimeChanged()
        if (metadataDirectoryExists()) {
            setSavedMetadataDirectory(buildMetadataDirectoryPath());
        }

        if (getRuntime().isWorkingCopy())
            return;

        // make sure this is not a deleted runtime. RTC 102272
        if (ServerCore.findRuntime(getRuntime().getId()) != null)
            createProject(null);

        initializeClasspathHelper();

    }

    //list will be initialized when the runtime is created. List can be re-initialized by clicking refresh button
    //on runtime button on advanced options page.
    public List<String> getSupportedCustomEncryption() {
        if (customEncryptionAlgoList != null)
            return customEncryptionAlgoList;
        if (customPasswordEncryptionInfoMap == null) {
            customPasswordEncryptionInfoMap = listCustomEncryption(new NullProgressMonitor());
        }
        if (customPasswordEncryptionInfoMap != null) {
            customEncryptionAlgoList = new ArrayList<String>();
            customEncryptionAlgoList.addAll(customPasswordEncryptionInfoMap.keySet());
        }
        return customEncryptionAlgoList;
    }

    public Map<String, CustomPasswordEncryptionInfo> listCustomEncryption() {
        if (customPasswordEncryptionInfoMap == null) {
            customPasswordEncryptionInfoMap = listCustomEncryption(new NullProgressMonitor());
        }
        return customPasswordEncryptionInfoMap;
    }

    /**
     * Verifies an installation directory.
     */
    protected static IStatus validateLocation(IPath path) {
        if (!isValidLocation(path))
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorInstallDirMissing, null);

        return Status.OK_STATUS;
    }

    public static boolean isValidLocation(IPath path) {
        return getRuntimePropertiesPath(path) != null;
    }

    public static List<IPath> findValidLocations(IPath path) {
        if (path == null)
            return null;

        List<IPath> paths = new ArrayList<IPath>();

        // Check parent
        IPath parentPath = path.removeLastSegments(1);
        if (isValidLocation(parentPath))
            paths.add(parentPath);

        // Check for type-ahead within current folder
        String lastSegment = path.lastSegment();
        String[] files = parentPath.toFile().list();
        if (files != null && lastSegment != null) {
            lastSegment = lastSegment.toLowerCase();
            for (String s : files) {
                if (s.toLowerCase().startsWith(lastSegment)) {
                    IPath childPath = parentPath.append(s);
                    if (isValidLocation(childPath))
                        paths.add(childPath);

                    childPath = parentPath.append(s).append(Constants.ROOT_FOLDER);
                    if (isValidLocation(childPath))
                        paths.add(childPath);
                }
            }
        }

        return paths;
    }

    /*
     * Validate the runtime
     */
    @Override
    public IStatus validate() {
        IStatus status = super.validate();
        if (!status.isOK())
            return status;

        status = validateLocation(getRuntime().getLocation());
        if (!status.isOK())
            return status;

        // don't accept trailing space since that can cause startup problems
        if (getRuntime().getLocation().hasTrailingSeparator())
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorInstallDirTrailingSlash, null);
        if (getVMInstall() == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorJRE, null);

        // ensure we have a Java 6+ VM
        IVMInstall vmInstall = getVMInstall();
        if (vmInstall instanceof IVMInstall2) {
            String javaVersion = ((IVMInstall2) vmInstall).getJavaVersion();
            if (javaVersion != null && javaVersion.compareTo("1.6") < 0)
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorJRE60, null);
        }

        return Status.OK_STATUS;
    }

    @Override
    public void setDefaults(IProgressMonitor monitor) {
        IRuntimeType type = getRuntimeWorkingCopy().getRuntimeType();
        String lastLocation = Activator.getPreference(type.getId() + ".folder", null);
        IRuntimeWorkingCopy wc = getRuntimeWorkingCopy();
        if (lastLocation != null) {
            int i = lastLocation.indexOf(",");
            if (i >= 0)
                lastLocation = lastLocation.substring(0, i);
            wc.setLocation(new Path(lastLocation));
        }
        //check if a project with the same name is in the workspace already
        String name = wc.getName();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String newName = name;
        int i = 2;
        while (root.exists(new Path(newName))) {
            newName = name + " (" + i + ')';
            i++;
        }
        if (i != 2)
            wc.setName(newName);
    }

    public void setVMInstall(IVMInstall vmInstall) {
        if (vmInstall == null)
            setVMInstall(null, null);
        else
            setVMInstall(vmInstall.getVMInstallType().getId(), vmInstall.getId());
    }

    protected void setVMInstall(String typeId, String id) {
        if (typeId == null)
            setAttribute(PROP_VM_INSTALL_TYPE_ID, (String) null);
        else
            setAttribute(PROP_VM_INSTALL_TYPE_ID, typeId);

        if (id == null)
            setAttribute(PROP_VM_INSTALL_ID, (String) null);
        else
            setAttribute(PROP_VM_INSTALL_ID, id);
    }

    public synchronized List<UserDirectory> getUserDirectories() {
        int hash = getRuntimeHash();
        if (userDirCache != null && hash == userDirHash)
            return userDirCache;

        userDirHash = hash;

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();

        List<UserDirectory> userDirs = new ArrayList<UserDirectory>(2);
        IPath runtimeLocation = getRuntime().getLocation();
        if (runtimeLocation == null)
            return userDirs;

        IPath runtimeUserPath = runtimeLocation.append(Constants.USER_FOLDER);
        if (runtimeUserPath.toFile().exists()) {
            IProject project = null;

            for (IProject p : projects) {
                if (runtimeUserPath.equals(p.getLocation())) {
                    project = p;
                    break;
                }
            }

            if (project == null)
                project = getProject();

            userDirs.add(new UserDirectory(this, runtimeUserPath, project));
        }

        List<?> list = getAttribute(PROP_USER_DIRS, (List<String>) null);
        if (list == null || list.isEmpty()) {
            userDirCache = userDirs;
            return userDirCache;
        }

        for (Object o : list) {
            if (o instanceof String) {
                String s = (String) o;
                if (!s.contains(IPath.SEPARATOR + "")) {
                    IProject project = root.getProject(s);
                    // get location, and assume default location if project doesn't exist
                    IPath location = project.getLocation();
                    if (location == null)
                        location = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(project.getName());
                    userDirs.add(new UserDirectory(this, location, project));
                } else {
                    IPath path = new Path(s);
                    IProject project = null;

                    for (IProject p : projects) {
                        if (p.getLocation().equals(path)) {
                            project = p;
                            break;
                        }
                    }
                    userDirs.add(new UserDirectory(this, path, project));
                }
            }
        }
        userDirCache = userDirs;
        return userDirCache;
    }

    public UserDirectory getUserDir(String id) {
        List<UserDirectory> userDirs = getUserDirectories();
        for (UserDirectory userDir : userDirs) {
            if (userDir.matchesId(id)) {
                return userDir;
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Could not find user directory for id: " + id + " and runtime: " + getRuntime().getName());
        }
        return null;
    }

    /*
     * Returns array of String containing server templates for the runtime
     */
    public synchronized String[] getServerTemplates() {

        if (getRuntime().getLocation() == null)
            return new String[0];

        File[] fList = getRuntime().getLocation().append(Constants.TEMPLATES_FOLDER).toFile().listFiles();
        if (fList == null)
            return new String[0];

        ArrayList<String> tplList = new ArrayList<String>();
        File f = null;
        for (int i = 0; i < fList.length; i++) {
            f = new File(fList[i].getAbsolutePath() + File.separator + "server.xml");
            if (fList[i].isDirectory() && f.exists()) {
                tplList.add(fList[i].getName());
            }
        }
        Collections.sort(tplList);

        String[] ret = new String[tplList.size()];
        for (int i = 0; i < tplList.size(); i++)
            ret[i] = tplList.get(i);
        return ret;
    }

    public synchronized void addUserDirectory(IProject project) {
        if (project == null)
            throw new IllegalArgumentException("Project cannot be null");
        addUserDirectory(project.getLocation(), project, null);
    }

    public synchronized void addUserDirectory(IPath path) {
        if (path == null)
            throw new IllegalArgumentException("Path cannot be null");
        addUserDirectory(path, null, null);
    }

    public void addUserDirectory(UserDirectory userDir) {
        if (userDir == null)
            throw new IllegalArgumentException("UserDir cannot be null");
        addUserDirectory(userDir.getPath(), userDir.getProject(), userDir);
    }

    private void addUserDirectory(IPath path, IProject project, UserDirectory userDir) {
        List<?> list = getAttribute(PROP_USER_DIRS, (List<String>) null);
        int size = list == null ? 2 : list.size() + 1;
        List<String> newList = new ArrayList<String>(size);
        if (list != null) {
            for (Object o : list) {
                if (o instanceof String)
                    newList.add((String) o);
            }
        }
        UserDirectory userDirectory = null;
        if (project != null)
            newList.add(project.getName());
        else
            newList.add(path.toPortableString());

        //reuse existing user Dir
        userDirectory = (userDir != null) ? userDir : new UserDirectory(this, path, project);

        setAttribute(PROP_USER_DIRS, newList);

        // make sure user directories is cached, then add
        getUserDirectories();
        userDirCache.add(userDirectory);
    }

    public synchronized void removeUserDirectory(UserDirectory userDir) {
        List<?> list = getAttribute(PROP_USER_DIRS, (List<String>) null);
        if (list == null || list.isEmpty())
            return;
        List<String> newList = new ArrayList<String>(list.size());
        for (Object o : list) {
            if (o instanceof String) {
                String s = (String) o;
                boolean ok = true;
                if (userDir.getProject() != null && !s.contains(IPath.SEPARATOR + "")) {
                    if (userDir.getProject().getName().equals(s))
                        ok = false;
                } else if (userDir.getPath().equals(new Path(s)))
                    ok = false;

                if (ok)
                    newList.add((String) o);
            }
        }
        setAttribute(PROP_USER_DIRS, newList);

        // make sure user directories is cached, then remove
        getUserDirectories();
        userDirCache.remove(userDir);
    }

    interface ServerIterator2 {
        public void iter(IPath path, File bootstrap);
    }

    /**
     * Returns a list of all server names for this runtime.
     *
     * @return an array of server names
     */
    public synchronized String[] getServerNames() {
        updateServerCache(false);
        int length = serverInfo.size();
        String[] serverNames = new String[length];
        for (int i = 0; i < length; i++) {
            serverNames[i] = serverInfo.get(i).getServerName();
        }

        // sort
        Arrays.sort(serverNames, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        return serverNames;
    }

    /**
     * Refresh all cached information for this runtime.
     *
     * @param force
     */
    public synchronized void refresh() {
        updateServerCache(true);
        schemaHelper = null;
        customEncryptionAlgoList = null;
        customPasswordEncryptionInfoMap = null;
        fireRefreshEvent();
    }

    private int getRuntimeHash() {
        IPath runtimeLocation = getRuntime().getLocation();
        if (runtimeLocation == null)
            return 0;

        int hash = runtimeLocation.hashCode() + getRuntime().getName().hashCode() * 7;
        if (runtimeLocation.append(Constants.USER_FOLDER).toFile().exists())
            hash += 1;
        return hash;
    }

    /**
     * Refresh the servers within this runtime.
     *
     * @param force
     */
    public synchronized void updateServerCache(boolean force) {

        int hash = getRuntimeHash();
        if (serverInfo != null && !force && serverCacheHash == hash)
            return;

        long time = System.currentTimeMillis();

        if (serverInfo == null)
            serverInfo = new ArrayList<WebSphereServerInfo>(5);

        IPath runtimePath = getRuntime().getLocation();
        serverCacheHash = hash;
        if (runtimePath == null)
            return;

        boolean changed = false;
        List<WebSphereServerInfo> found = new ArrayList<WebSphereServerInfo>(5);

        List<UserDirectory> userDirs = getUserDirectories();

        for (UserDirectory userDir : userDirs) {
            IPath path = userDir.getServersPath();

            if (!path.toFile().exists())
                continue;

            File[] folders = path.toFile().listFiles();

            for (File f : folders) {

                if (f.isDirectory()) {

                    File serverFile = new File(f, Constants.SERVER_XML);
                    if (serverFile.exists()) {
                        String serverName = f.getName();

                        // find existing server info to update, or create a new one
                        WebSphereServerInfo info = null;
                        for (WebSphereServerInfo existing : serverInfo) {
                            if (existing.getServerName().equals(serverName) && existing.getUserDirectory().equals(userDir)) {
                                info = existing;
                            }
                        }
                        if (info == null) {
                            info = new WebSphereServerInfo(serverName, userDir, this);
                            serverInfo.add(info);
                            changed = true;
                        }
                        found.add(info);

                        // update server info
                        try {
                            info.updateCache();
                        } catch (Exception e) {
                            Trace.logError("Error updating runtime cache: " + info.getServerName(), e);
                        }
                    }
                }
            }
        }

        // remove old/removed servers
        List<WebSphereServerInfo> delete = new ArrayList<WebSphereServerInfo>(1);
        for (WebSphereServerInfo info : serverInfo) {
            if (!found.contains(info)) {
                delete.add(info);
                changed = true;
            }
        }
        for (WebSphereServerInfo info : delete)
            serverInfo.remove(info);

        if (changed) {
            fireRefreshEvent();
            refreshProjectView();
        }

        if (Trace.ENABLED)
            Trace.tracePerf("Update runtime cache", time);
    }

    // Refresh view in project explorer to show any servers that
    // were added/removed outside the tool
    private void refreshProjectView() {
        Job job = new Job("Server Project View Updater") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final List<UserDirectory> userDirs = getUserDirectories();
                for (UserDirectory userDir : userDirs) {
                    IFolder servers = userDir.getServersFolder();
                    if (servers != null && servers.exists()) {
                        try {
                            servers.refreshLocal(IResource.DEPTH_INFINITE, null);
                        } catch (CoreException ce) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Couldn't refresh workspace after updating runtime: " + getRuntime().getId(), ce);
                        }
                    }
                }
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return Constants.JOB_FAMILY.equals(family);
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    public void fireRefreshEvent() {
        IRuntime theRuntime = getRuntime();
        if (theRuntime.isWorkingCopy()) {
            IRuntimeWorkingCopy wc = (IRuntimeWorkingCopy) theRuntime;
            IRuntime runtime = wc.getOriginal();
            if (runtime != null)
                theRuntime = runtime;
        }

        ServerListenerUtil.getInstance().fireRuntimeChangedEvent(theRuntime);
    }

    public void fireMetadataRefreshEvent() {
        IRuntime theRuntime = getRuntime();
        if (theRuntime.isWorkingCopy()) {
            IRuntimeWorkingCopy wc = (IRuntimeWorkingCopy) theRuntime;
            IRuntime runtime = wc.getOriginal();
            if (runtime != null)
                theRuntime = runtime;
        }

        ServerListenerUtil.getInstance().fireMetadataChangedEvent(theRuntime);
    }

    public synchronized WebSphereServerInfo getServerInfo(String serverName, UserDirectory userDir) {
        updateServerCache(false);
        for (WebSphereServerInfo info : serverInfo) {
            if (info.getServerName().equals(serverName) &&
                userDir != null && userDir.equals(info.getUserDirectory())) {
                return info;
            }
        }
        return null;
    }

    public synchronized List<WebSphereServerInfo> getWebSphereServerInfos(UserDirectory userDir) {
        updateServerCache(false);
        List<WebSphereServerInfo> list = new ArrayList<WebSphereServerInfo>(serverInfo.size());
        for (WebSphereServerInfo info : serverInfo) {
            if (info.getUserDirectory().equals(userDir)) {
                list.add(info);
            }
        }
        return list;
    }

    public synchronized boolean hasServers() {
        updateServerCache(false);
        return serverInfo.size() > 0;
    }

    public synchronized List<WebSphereServerInfo> getWebSphereServerInfos() {
        updateServerCache(false);
        return new ArrayList<WebSphereServerInfo>(serverInfo);
    }

    /**
     * Create a process builder for launching "wlp/bin/server [option] serverName" with the given arguments.
     *
     * @param option
     * @param server  a server
     * @param command command arguments
     * @return the process builder, ready for launch
     */
    public ProcessBuilder createProcessBuilder(String option, WebSphereServerInfo server, String... command) {
        File workDir = server.getServerOutputPath().toFile();
        UserDirectory userDir = server.getUserDirectory();
        if (!workDir.exists())
            workDir = userDir.getPath().toFile();

        String[] command2 = new String[command.length + 3];
        command2[0] = Constants.BATCH_SCRIPT;
        command2[1] = option;
        command2[2] = server.getServerName();
        System.arraycopy(command, 0, command2, 3, command.length);
        return createProcessBuilder(userDir, workDir, null, command2);
    }

    /**
     * Create a process builder for launching WLP with the given arguments.
     *
     * @param userDir a user directory, may be null
     * @param workDir a working directory
     * @param command the command arguments
     * @return the process builder, ready for launch
     */
    public ProcessBuilder createProcessBuilder(UserDirectory userDir, File workDir, String extraJvmArgs, String... command) {
        String batch = command[0];
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            batch += ".bat";
        command[0] = getRuntime().getLocation().append("bin").append(batch).toOSString();

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(workDir);

        Map<String, String> env = builder.environment();
        if (userDir != null) {
            env.put(Constants.WLP_USER_DIR, userDir.getPath().toOSString());
            env.put(Constants.WLP_OUTPUT_DIR, userDir.getOutputPath().toOSString());
        }

        IVMInstall vmInstall = getVMInstall();
        if (vmInstall != null) {
            File javaHome = LaunchUtil.getJavaHome(vmInstall.getInstallLocation());
            env.put("JAVA_HOME", javaHome.getAbsolutePath());
            String jvmArgs = extraJvmArgs;
            if (LaunchUtil.isIBMJRE(vmInstall)) {
                jvmArgs = jvmArgs == null ? LaunchUtil.VM_QUICKSTART : jvmArgs + " " + LaunchUtil.VM_QUICKSTART;
            }
            if (jvmArgs != null) {
                env.put("JVM_ARGS", jvmArgs);
            }
        }
        env.put("EXIT_ALL", "1");
        builder.command(command);
        return builder;
    }

    /**
     * Create a process builder for launching a utility from wlp/bin with the given arguments. The first argument
     * must be the utility name, which will be changed into an absolute path before launching.
     *
     * @param command command and arguments
     * @return the process builder, ready for launch
     */
    protected ProcessBuilder createProcessBuilder(String... command) {
        String batch = command[0];
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            batch += ".bat";
        IPath location = getRuntime().getLocation().append("bin");
        command[0] = location.append(batch).toOSString();

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(location.toFile());
        builder.redirectErrorStream(true);

        Map<String, String> env = builder.environment();

        IVMInstall vmInstall = getVMInstall();
        if (vmInstall != null) {
            File javaHome = LaunchUtil.getJavaHome(vmInstall.getInstallLocation());
            env.put("JAVA_HOME", javaHome.getAbsolutePath());
            if (LaunchUtil.isIBMJRE(vmInstall))
                env.put("JVM_ARGS", LaunchUtil.VM_QUICKSTART);
        }
        builder.command(command);
        return builder;
    }

    /**
     * Create a process builder for launching a utility from wlp/bin with the given arguments. The first argument
     * must be the utility name, which will be changed into an absolute path before launching.
     *
     * @param command command and arguments
     * @return the process builder, ready for launch
     */
    protected ProcessBuilder createJavaProcessBuilder(String jar, String[] options) {
        String javaCmd;
        IVMInstall vmInstall = getVMInstall();
        if (vmInstall != null) {
            File javaHome = LaunchUtil.getJavaHome(vmInstall.getInstallLocation());
            IPath javaPath = new Path(javaHome.getAbsolutePath());
            javaPath = javaPath.append("bin").append("java");
            javaCmd = javaPath.toOSString();
        } else
            javaCmd = "java";

        int size = options != null ? options.length : 0;

        String[] command = new String[3 + size];
        command[0] = javaCmd;
        command[1] = "-jar";
        command[2] = jar;
        if (options != null) {
            for (int i = 0; i < size; i++)
                command[i + 3] = options[i];
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(getRuntime().getLocation().append("bin").append("tools").toFile());
        builder.redirectErrorStream(true);

        Map<String, String> env = builder.environment();
        if (vmInstall != null) {
            File javaHome = LaunchUtil.getJavaHome(vmInstall.getInstallLocation());
            env.put("JAVA_HOME", javaHome.getAbsolutePath());
            if (LaunchUtil.isIBMJRE(vmInstall))
                env.put("JVM_ARGS", LaunchUtil.VM_QUICKSTART);
        }
        builder.command(command);
        return builder;
    }

    public void createServer(String serverName, String template, IPath userDirPath, IProgressMonitor monitor) throws CoreException {
        if (serverName == null)
            throw new IllegalArgumentException("Server name cannot be null");

        // make sure server doesn't exist already
        List<WebSphereServerInfo> existing = getWebSphereServerInfos();
        for (WebSphereServerInfo info : existing) {
            if (info.getUserDirectory().getPath().equals(userDirPath) && info.getServerName().equals(serverName))
                throw new IllegalArgumentException("Server already exists");
        }

        long time = System.currentTimeMillis();
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();
        monitor2.beginTask(NLS.bind(Messages.taskServerCreate, serverName), 230);

        // launch process to create the server and wait up to 10s
        try {
            WebSphereServerInfo info = new WebSphereServerInfo(serverName, new UserDirectory(this, userDirPath, null), this);
            Process p = null;

            // The 8.5.0.x runtime does not accept --template and there is only the default configuration
            String version = getRuntimeVersion();
            if (template == null || (version != null && version.startsWith("8.5.0")) || (version == null && template.equals(Constants.TEMPLATES_DEFAULT_NAME)))
                p = createProcessBuilder("create", info).start();
            else
                p = createProcessBuilder("create", info, "--template=" + template).start();
            monitor2.worked(30);

            ProcessResult result = ProcessHelper.waitForProcess(p, 150, 10f, 200, monitor2);
            if (result.getExitValue() != 0)
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorServerCreate, Integer.valueOf(result.getExitValue()) + ": "
                                                                                                                            + result.getOutput())));
        } catch (TimeoutException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorTimeout));
        } catch (IOException e) {
            Trace.logError("Error creating server: " + serverName, e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorServerCreate, e.getLocalizedMessage())));
        } finally {
            updateServerCache(true);
            monitor2.done();
            if (Trace.ENABLED)
                Trace.tracePerf("Create server", time);
        }
    }

    public void deleteServer(WebSphereServerInfo server, IProgressMonitor monitor) throws CoreException {
        if (server == null)
            throw new IllegalArgumentException("Server cannot be null");

        IPath path = getRuntime().getLocation();
        if (path == null)
            return;

        // Delete the server's metadata
        server.removeMetadata(null, true, true);

        try {
            path = server.getServerPath();
            if (!path.toFile().exists())
                throw new IllegalArgumentException("Server does not exist");

            IStatus[] status = PublishHelper.deleteDirectory(path.toFile(), monitor);

            if (status != null && status.length > 0) {
                if (status.length == 1)
                    throw new CoreException(status[0]);

                throw new CoreException(new MultiStatus(Activator.PLUGIN_ID, 0, status, Messages.errorDeletingServer, null));
            }
        } catch (CoreException ce) {
            Trace.logError("Error deleting server: " + server.getServerName(), ce);
            throw ce;
        } catch (Exception e) {
            Trace.logError("Error deleting server: " + server.getServerName(), e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage()));
        } finally {
            updateServerCache(true);
        }
    }

    /**
     * Returns <code>true</code> if the given server is started, and <code>false</code> otherwise.
     *
     * @param server  a server
     * @param monitor a progress monitor, or <code>null</code>
     * @return <code>true</code> if the server is started, and <code>false</code> otherwise
     * @throws CoreException if the server state could not be determined
     */
    public boolean isServerStarted(WebSphereServerInfo server, IProgressMonitor monitor) throws CoreException {
        return getServerStatus(server, 8f, monitor) == 0;
    }

    /**
     * @param server  the server
     * @param iter    number of times to loop
     * @param timeout the timeout, in seconds
     * @param monitor a progress monitor
     * @return the exit value. -1 if timeout; 0 if the server is running; 1 if the server is stopped; 2 if the status is unknown.
     * @throws CoreException
     */
    public int getServerStatus(WebSphereServerInfo server, float timeout, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        verifyServerExists(server);

        String serverName = server.getServerName();
        monitor2.beginTask(NLS.bind(Messages.taskServerStatus, serverName), 230);

        int r;
        long time = 0;
        if (Trace.ENABLED_DETAILS)
            time = System.currentTimeMillis();

        try {
            Process p = createProcessBuilder("status:fast", server).start();
            monitor2.worked(30);
            r = ProcessHelper.waitForProcess(p, 50, timeout, 200, monitor2).getExitValue();
        } catch (TimeoutException t) {
            r = -1;
        } catch (Throwable t) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorServerStatus, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
        }
        if (Trace.ENABLED_DETAILS) {
            Trace.trace(Trace.SSM, "runtime server status: " + r);
            Trace.tracePerf("get runtime server status", time);
        }
        return r;
    }

    public ILaunchConfiguration createUtilityLaunchConfig(WebSphereServerInfo serverInfo, String jvmArgs, String... command) throws CoreException {
        return createUtilityLaunchConfig("com.ibm.ws.st.core.utilityLaunchConfiguration", serverInfo, jvmArgs, command);
    }

    public ILaunchConfiguration createUtilityLaunchConfig(String launchConfigType, WebSphereServerInfo serverInfo, String jvmArgs, String... command) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);
        String serverID = wsServer.getServer().getId();

        ILaunchConfigurationType lct = launchManager.getLaunchConfigurationType(launchConfigType);
        ILaunchConfigurationWorkingCopy wc = lct.newInstance(null, getRuntime().getName());

        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_RUNTIME, getRuntime().getId());
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_WORK_DIR, serverInfo.getServerOutputPath().toOSString());
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_USER_DIR, getUserDirectories().indexOf(serverInfo.getUserDirectory()));
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_LABEL,
                        LaunchUtil.getProcessLabelAttr(serverInfo.getWebSphereRuntime().getRuntime().getName(), serverInfo.getServerName()));
        if (jvmArgs != null) {
            wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_JVM_ARGS, jvmArgs);
        }
        List<String> list = new ArrayList<String>();
        for (String s : command)
            list.add(s);
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_COMMAND, list);
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_SERVER_ID, serverID);
        return wc.doSave();
    }

    public ILaunchConfiguration createRemoteUtilityLaunchConfig(WebSphereServerInfo serverInfo, Map<String, String> commandVariables) throws CoreException {
        return createRemoteUtilityLaunchConfig("com.ibm.ws.st.core.utilityLaunchConfiguration", serverInfo, commandVariables);
    }

    public ILaunchConfiguration createRemoteUtilityLaunchConfig(String launchConfigType, WebSphereServerInfo serverInfo,
                                                                Map<String, String> commandVariables) throws CoreException {

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);
        String serverID = wsServer.getServer().getId();

        ILaunchConfigurationType lct = launchManager.getLaunchConfigurationType(launchConfigType);
        ILaunchConfigurationWorkingCopy wc = lct.newInstance(null, getRuntime().getName());
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_COMMAND, commandVariables);
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_SERVER_ID, serverID);
        return wc.doSave();
    }

    /**
     * Launches a server command line process to package up the given server.
     * The server must exist and be stopped.
     *
     * @param serverName the server name
     * @param zipFile    the file to output to
     * @param include    "all", "usr", or <code>null</code> to leave the default
     * @param monitor    a progress monitor, or <code>null</code>
     * @throws CoreException
     */
    public void callPackageServerCommand(final WebSphereServerInfo server, final File zipFile, final String include, IProgressMonitor monitor) throws CoreException {
        List<String> command = new ArrayList<String>();
        command.add(Constants.BATCH_SCRIPT);
        command.add("package");
        command.add(server.getServerName());
        command.add("--archive=" + zipFile.getPath());
        if (include != null)
            command.add("--include=" + include);
        ILaunchConfiguration lc = createUtilityLaunchConfig(server, null, command.toArray(new String[command.size()]));
        ILaunch launch = lc.launch(ILaunchManager.RUN_MODE, monitor);

        while (!monitor.isCanceled() && !launch.isTerminated()) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                //do nothing
            }
        }
        monitor.done();
    }

    /**
     * Launches a utility process to package up the given server.
     * The server must exist and be stopped.
     *
     * @param serverName  the server name
     * @param archiveFile the file to output to
     * @param include     "all", "usr", or <code>null</code> to leave the default
     * @param monitor     a progress monitor, or <code>null</code>
     * @throws CoreException
     */

    public void packageServer(final WebSphereServerInfo server, final File archiveFile, final String include, final boolean publishServer,
                              IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        verifyServerExists(server);

        final WebSphereServer ws = WebSphereUtil.getWebSphereServer(server);

        if (ws == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        if (ws.isLocalSetup()) {
            if (isServerStarted(server, monitor2))
                throw new IllegalArgumentException(Messages.serverMustBeStopped);
        } else {
            if (ws.getServer().getServerState() != IServer.STATE_STOPPED) {
                throw new IllegalArgumentException(Messages.serverMustBeStopped);
            }
        }

        if (archiveFile == null || archiveFile.isDirectory())
            throw new IllegalArgumentException(Messages.invalidZipFile);

        final String serverName = server.getServerName();

        Job job = new Job(NLS.bind(Messages.taskPackageServer, server.getServerName())) {
            IStatus opStatus = null;
            //save the publish settings
            boolean isAutoPublish = ServerCore.isAutoPublishing();
            int autoPublishSettings = ws.getServer().getAttribute(org.eclipse.wst.server.core.internal.Server.PROP_AUTO_PUBLISH_SETTING,
                                                                  WebSphereServerBehaviour.AUTO_PUBLISH_RESOURCE);

            @Override
            public boolean belongsTo(Object family) {
                return Constants.JOB_FAMILY.equals(family);
            }

            @SuppressWarnings("restriction")
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(NLS.bind(Messages.taskPackageServer, serverName), 100);

                boolean republishAndChangeConfig = false;
                String version = getRuntimeVersion();

                if ((version == null || "8.5.0.0".equals(version)) && ws.isLooseConfigEnabled())
                    republishAndChangeConfig = true;

                monitor.worked(10);

                try {
                    //republish the server if required
                    IStatus status = null;
                    // for runtime version 8.5.0.0 or null republish only of loose config is enabled
                    if (republishAndChangeConfig) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "The runtime version is " + version + ". Republishing the server with non loose config ");
                        status = republishServer(false, new SubProgressMonitor(monitor, 20));
                    } else if (publishServer) { // for other versions republish only if users says yes to the  UI dialog
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "The runtime version is " + version + ". Republishing with loose config: " + ws.isLooseConfigEnabled());
                        status = republishServer(ws.isLooseConfigEnabled(), new SubProgressMonitor(monitor, 20));
                    }

                    if (status != null && (status.getSeverity() == IStatus.CANCEL || status.getSeverity() == IStatus.ERROR))
                        return status;

                    // package the server
                    monitor.subTask(Messages.taskPackaging);
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Start to use the launcher to call the package command. ");

                    ILaunchConfiguration lc = null;
                    ILaunch launch = null;
                    if (!ws.isLocalSetup()) {
                        Map<String, String> commandVariables = new HashMap<String, String>();
                        commandVariables.put(CommandConstants.UTILITY_TYPE, CommandConstants.PACKAGE_SERVER);
                        commandVariables.put(CommandConstants.GENERAL_ARCHIVE, archiveFile.getPath());
                        commandVariables.put(CommandConstants.GENERAL_INCLUDE, include);
                        lc = createRemoteUtilityLaunchConfig(server, commandVariables);
                    } else {
                        List<String> command = new ArrayList<String>();
                        command.add(Constants.BATCH_SCRIPT);
                        command.add(CommandConstants.PACKAGE_SERVER);
                        command.add(server.getServerName());
                        command.add(CommandConstants.GENERAL_ARCHIVE + archiveFile.getPath());
                        if (include != null)
                            command.add(CommandConstants.GENERAL_INCLUDE + include);
                        lc = createUtilityLaunchConfig(server, null, command.toArray(new String[command.size()]));
                    }

                    if (lc != null)
                        launch = lc.launch(ILaunchManager.RUN_MODE, monitor);
                    int count = 0;
                    int cmdRemain = 50;
                    monitor.worked(cmdRemain);
                } catch (Throwable t) {
                    Trace.logError("Exception when running package server " + server.getServerName(), t);
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorPackagingServer, t.getLocalizedMessage()));
                } finally {
                    try {
                        //republish again with looseConfig true if version is null or 8.5.0.0
                        if (republishAndChangeConfig) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.INFO, "Done packaging the server. The server version is " + version + ". Republish the server again with looseConfig");
                            IStatus status = republishServer(true, new SubProgressMonitor(monitor, 20));
                            if (status.getSeverity() == IStatus.CANCEL || status.getSeverity() == IStatus.ERROR)
                                return status;
                        }
                    } catch (Throwable t) {
                        Trace.logError("Exception when running package server " + server.getServerName(), t);
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorPackagingServer, t.getLocalizedMessage()));
                    } finally {
                        //reset the publishing settings to the original
                        IServerWorkingCopy wc = ws.getServer().createWorkingCopy();
                        wc.setAttribute(org.eclipse.wst.server.core.internal.Server.PROP_AUTO_PUBLISH_SETTING, autoPublishSettings);
                        try {
                            wc.save(true, monitor);
                            ws.getWebSphereServerBehaviour().setWebSphereServerPublishState(IServer.PUBLISH_STATE_NONE);
                            org.eclipse.wst.server.core.internal.ServerPreferences.getInstance().setAutoPublishing(isAutoPublish);
                        } catch (CoreException e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Failed to change the auto publish setting back.");
                        } finally {
                            monitor.done();
                        }
                    }
                }
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Server package completed.");
                return Status.OK_STATUS;
            }

            @SuppressWarnings("restriction")
            private IStatus republishServer(boolean looseConfig, IProgressMonitor monitor2) throws CoreException {

                IProgressMonitor monitor = monitor2;
                if (monitor2 == null)
                    monitor = new NullProgressMonitor();

                monitor.beginTask(Messages.taskPackagePublish, 20);
                monitor.subTask(Messages.taskPackagePublish);
                monitor.worked(5);

                IServer iServer = ws.getServer();

                // change loose config mode
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Change the loose config mode for server packaging.");

                org.eclipse.wst.server.core.internal.ServerPreferences.getInstance().setAutoPublishing(false);
                IServerWorkingCopy wc = iServer.createWorkingCopy();
                wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, looseConfig);
                if (!looseConfig)
                    wc.setAttribute(org.eclipse.wst.server.core.internal.Server.PROP_AUTO_PUBLISH_SETTING, WebSphereServerBehaviour.AUTO_PUBLISH_DISABLE);
                iServer = wc.save(true, monitor);

                if (monitor.isCanceled())
                    return Status.CANCEL_STATUS;

                monitor.worked(5);

                IJobManager jobManager = Job.getJobManager();
                ISchedulingRule packageRule = MultiRule.combine(new ISchedulingRule[] { ResourcesPlugin.getWorkspace().getRoot(), iServer });
                jobManager.beginRule(packageRule, monitor);

                // do a publish
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Publish the server for server packaging.");

                opStatus = iServer.publish(IServer.PUBLISH_INCREMENTAL, monitor);

                if (monitor.isCanceled())
                    return Status.CANCEL_STATUS;
                if (opStatus.getSeverity() == IStatus.ERROR)
                    return opStatus;

                jobManager.endRule(packageRule);

                monitor.worked(10);

                if (monitor.isCanceled())
                    return Status.CANCEL_STATUS;

                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * Launches a utility process to generate a server dump.
     * The server must exist.
     *
     * @param server      the server
     * @param archiveFile the archive file, or <code>null</code>
     * @param include     include options, or <code>null</code>
     * @param monitor     a progress monitor, or <code>null</code>
     * @throws CoreException
     */
    public ILaunch dumpServer(WebSphereServerInfo server, File archiveFile, String include, IProgressMonitor monitor) throws CoreException {
        verifyServerExists(server);
        if (archiveFile != null && archiveFile.isDirectory())
            throw new IllegalArgumentException(Messages.invalidZipFile);

        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(server);

        if (wsServer == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        LibertyServerDumpUtility utility = new LibertyServerDumpUtility();
        return utility.dumpServer(this, server, archiveFile, include, monitor);
    }

    /**
     * Launches a utility process to generate a server javadump.
     * The server must exist.
     *
     * @param server  the server
     * @param include include options, or <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     * @throws CoreException
     */
    public ILaunch javadumpServer(WebSphereServerInfo server, String include, IProgressMonitor monitor) throws CoreException {
        verifyServerExists(server);

        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(server);

        if (wsServer == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        LibertyServerDumpUtility utility = new LibertyServerDumpUtility();
        return utility.javadumpServer(this, server, include, monitor);
    }

    /**
     * Launches the security utility to generate an SSL certificate.
     *
     * (Command line: securityUtility createSSLCertificate --server=name --password[=pwd] [--validity=days] [--subject=dn])
     *
     * @param serverName       the server name
     * @param password         a password
     * @param passwordEncoding a password encoding, e.g. "xor" or "aes"
     * @param passwordKey      a password key (if required by the encoding) or <code>null</code>
     * @param validity         # of days the certificate should be valid, or -1 for default
     * @param subject          the subject, or <code>null</code> for default
     * @param monitor          a progress monitor, or <code>null</code>
     * @throws CoreException
     */
    public ILaunch createSSLCertificate(WebSphereServerInfo serverInfo, String password, String passwordEncoding, String passwordKey, int validity, String subject,
                                        String includeFileName, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        verifyServerExists(serverInfo);

        String serverName = serverInfo.getServerName();
        monitor2.beginTask(NLS.bind(Messages.taskCreateSSLCertificate, serverName), 200);

        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);

        if (wsServer == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        LibertySSLCreateCertificationUtility utility = new LibertySSLCreateCertificationUtility();
        return utility.createSSLCertificate(this, serverInfo, password, passwordEncoding, passwordKey, validity, subject, includeFileName, monitor2);
    }

    /**
     * Launches the security utility to encode a password.
     *
     * (Command line: securityUtility encode [plainText])
     *
     * @param password a plain text password
     * @param monitor  a progress monitor, or <code>null</code>
     * @return the encoded password
     * @throws CoreException
     */
    public String encodePassword(String password, IProgressMonitor monitor) throws CoreException {
        return encodePassword("xor", null, password, monitor);
    }

    /**
     * Launches the security utility to encrypt a password.
     *
     * (Command line: securityUtility encode --encoding=[algorithm] --key=[key] plainText)
     *
     * @param method   the encryption algorithm
     * @param key      an encoding key
     * @param password a plain text password
     * @param monitor  a progress monitor, or <code>null</code>
     * @return the encrypted password
     * @throws CoreException
     */
    public String encryptPassword(String algorithm, String key, String password, IProgressMonitor monitor) throws CoreException {
        return encodePassword(algorithm, key, password, monitor);
    }

    /**
     * Launches the security utility to encode a password.
     *
     * (Command line: securityUtility encode --encoding=[encoding] --key=[key] [plainText])
     *
     * @param encoding an encoding, e.g. "xor", "aes" or "hash"
     * @param key      a key (optional, only needed for AES or hash)
     * @param password a password
     * @param monitor  a progress monitor, or <code>null</code>
     * @return the encoded password
     * @throws CoreException
     */
    private String encodePassword(String encoding, String key, String password, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        long time = System.currentTimeMillis();
        if ("xor".equals(encoding))
            monitor2.beginTask(Messages.taskEncodePassword, 130);
        else
            monitor2.beginTask(Messages.taskEncryptPassword, 130);

        try {
            List<String> list = new ArrayList<String>();
            list.add("securityUtility");
            list.add("encode");
            if (getRuntimeVersion().startsWith("8.5.0")) {
                if (!"xor".equals(encoding))
                    throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorEncodePasswordUnsupportedEncoding, encoding)));
            } else {
                if (encoding != null)
                    list.add("--encoding=" + encoding);
            }
            if (key != null && !key.trim().isEmpty())
                list.add("--key=" + key);
            list.add(password);
            ProcessBuilder builder = createProcessBuilder(list.toArray(new String[list.size()]));
            Process p = builder.start();
            monitor2.worked(30);

            ProcessResult result = ProcessHelper.waitForProcess(p, 100, 10f, 100, monitor2);
            int exitValue = result.getExitValue();
            String sb = result.getOutput();
            if (exitValue != 0) {
                Trace.logError("Error running securityUtility. exitValue=" + Integer.valueOf(exitValue) + ": " + sb, null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorEncodePassword, Integer.valueOf(exitValue) + ": " + sb)));
            }

            return sb.toString().trim();
        } catch (CoreException ce) {
            throw ce;
        } catch (Throwable t) {
            Trace.logError("Error running securityUtility", t);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorEncodePassword, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
            Trace.tracePerf("Encode password", time);
        }
    }

    /**
     * Launches the security utility to list supported custom encryption.
     *
     * (Command line: securityUtility encode --listCustom)
     *
     *
     * @param monitor a progress monitor, or <code>null</code>
     * @return Map of crypto algorithm name and CustomPasswordEncryptionInfo object. it is empty if either no custom encryption or error.
     *
     */
    public Map<String, CustomPasswordEncryptionInfo> listCustomEncryption(IProgressMonitor monitor) {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();
        Map<String, CustomPasswordEncryptionInfo> cpeiMap = new LinkedHashMap<String, CustomPasswordEncryptionInfo>();

        long time = System.currentTimeMillis();
        monitor2.beginTask(Messages.taskListEncryption, 130);

        try {
            List<String> list = new ArrayList<String>();
            list.add(SECURITY_UTILITY);
            list.add(ENCODE);
            list.add(SU_OPTION_LISTCUSTOM);
            ProcessBuilder builder = createProcessBuilder(list.toArray(new String[list.size()]));
            Process p = builder.start();
            monitor2.worked(30);
            ProcessResult result = ProcessHelper.waitForProcess(p, 100, 10f, 100, monitor2);
            int exitValue = result.getExitValue();
            String sb = result.getOutput();
            if (exitValue != 0) {
                if (Trace.ENABLED)
                    Trace.logError("Error running securityUtility. exitValue=" + Integer.valueOf(exitValue) + ": " + sb, null);
            }
            cpeiMap = parseListCustom(cpeiMap, JSONArray.parse(sb));
        } catch (IOException e) {
            // no custom encoding listed or runtime doesn't support --listCustom option. In either case do nothing
        } catch (TimeoutException e) {
            if (Trace.ENABLED)
                Trace.logError("Error running securityUtility", e);
        } finally {
            monitor2.done();
            Trace.tracePerf("List custom Encryption", time);
        }
        return cpeiMap;
    }

    /**
     * Parse the JSONArray to get the list of supported custom encryption
     *
     * @param customEncryptions the output of the listCustom.
     * @return Map of the crypto algorith name and CustomPasswordEncryptionInfo
     *
     */
    protected Map<String, CustomPasswordEncryptionInfo> parseListCustom(Map<String, CustomPasswordEncryptionInfo> map, JSONArray customEncryptions) {
        if (customEncryptions != null) {
            for (int i = 0; i < customEncryptions.size(); i++) {
                JSONObject object = (JSONObject) customEncryptions.get(i);
                map.put((String) object.get("name"),
                        new CustomPasswordEncryptionInfo((String) object.get("name"), (String) object.get("featurename"), (String) object.get("description")));
            }
        }
        return map;
    }

    /**
     * Generate the configuration schema.
     *
     * (Command line: java -jar ws-schemagen.jar [file])
     *
     * @param file    the file to generate to
     * @param monitor a progress monitor, or <code>null</code>
     * @param timeout the timeout in seconds
     * @throws CoreException
     */
    @Override
    public void generateSchema(String file, IProgressMonitor monitor, int timeout) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        monitor2.beginTask(NLS.bind(Messages.jobRuntimeCache, getRuntime().getName()), 330);
        long time = System.currentTimeMillis();

        try {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Generating schema to " + file);

            String locale = "--locale=" + Locale.getDefault().toString();
            String version = getRuntimeVersion();
            String[] params;
            if (version == null || WebSphereUtil.isGreaterOrEqualVersion("8.5.5.2", version))
                params = new String[] { locale, "--schemaVersion=1.1", "--outputVersion=2", file };
            else
                params = new String[] { locale, file };

            ProcessBuilder builder = createJavaProcessBuilder("ws-schemagen.jar", params);
            Process p = builder.start();
            monitor2.worked(30);

            ProcessResult result = ProcessHelper.waitForProcess(p, 100, timeout, 300, monitor2);
            int exitValue = result.getExitValue();
            String sb = result.getOutput();
            if (exitValue != 0) {
                Trace.logError("Error generating schema. exitValue=" + Integer.valueOf(exitValue) + ": " + sb, null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, Integer.valueOf(exitValue) + ": " + sb)));
            }
        } catch (CoreException ce) {
            throw ce;
        } catch (Throwable t) {
            Trace.logError("Error generating schema", t);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
            Trace.tracePerf("Schema gen", time);
        }
    }

    /**
     * Generate the feature list.
     *
     * (Command line: java -jar ws-featurelist.jar [file])
     *
     * @param file    the file to generate to
     * @param monitor a progress monitor, or <code>null</code>
     * @param timeout the timeout in seconds
     * @throws CoreException
     */
    @Override
    public void generateFeatureList(String file, IProgressMonitor monitor, int timeout, String... options) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        monitor2.beginTask(NLS.bind(Messages.jobRuntimeCache, getRuntime().getName()), 330);
        long time = System.currentTimeMillis();

        try {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Generating feature list to " + file);

            String locale = "--locale=" + Locale.getDefault().toString();
            ProcessBuilder builder;
            if (options == null || options.length == 0)
                builder = createJavaProcessBuilder("ws-featurelist.jar", new String[] { locale, file });
            else {
                ArrayList<String> cmds = new ArrayList<String>(options.length + 2);
                cmds.add(locale);
                for (String o : options) {
                    cmds.add(o);
                }
                cmds.add(file);
                builder = createJavaProcessBuilder("ws-featurelist.jar", cmds.toArray(new String[cmds.size()]));
            }
            Process p = builder.start();
            monitor2.worked(30);

            ProcessResult result = ProcessHelper.waitForProcess(p, 100, timeout, 300, monitor2);
            int exitValue = result.getExitValue();
            String sb = result.getOutput();
            if (exitValue != 0) {
                Trace.logError("Error generating feature list. exitValue=" + Integer.valueOf(exitValue) + ": " + sb, null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, Integer.valueOf(exitValue) + ": " + sb)));
            }
        } catch (CoreException ce) {
            throw ce;
        } catch (Throwable t) {
            Trace.logError("Error generating feature list", t);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.metadataGenerationFailedDetails, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
            Trace.tracePerf("Feature list gen", time);
        }
    }

    /**
     * Launches the product info command.
     *
     * (Command line: productInfo [command])
     *
     * @param command the parameter
     * @param monitor a progress monitor, or <code>null</code>
     * @return the product info, or null if it is not supported on this runtime
     * @throws CoreException
     */
    public String getProductInfo(String command, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        long time = System.currentTimeMillis();
        monitor2.beginTask(Messages.taskProductInfo, 230);

        try {
            String runtimeVersion = getRuntimeVersion();
            if (runtimeVersion != null && runtimeVersion.startsWith("8.5.0.0")) // not supported until 8.5.0.1
                return null;

            List<String> list = new ArrayList<String>();
            list.add("productInfo");
            list.add(command);
            ProcessBuilder builder = createProcessBuilder(list.toArray(new String[list.size()]));
            Process p = builder.start();
            monitor2.worked(30);

            ProcessResult result = ProcessHelper.waitForProcess(p, 100, 30f, 200, monitor2);
            int exitValue = result.getExitValue();
            String sb = result.getOutput();
            if (exitValue != 0) {
                Trace.logError("Error running productInfo " + command + ". exitValue=" + Integer.valueOf(exitValue) + ": " + sb, null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorProductInfo, Integer.valueOf(exitValue) + ": " + sb.toString())));
            }

            return sb.toString().trim();
        } catch (CoreException ce) {
            throw ce;
        } catch (Throwable t) {
            Trace.logError("Error running productInfo " + command, t);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorProductInfo, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
            Trace.tracePerf("Product info " + command, time);
        }
    }

    public void verifyServerExists(WebSphereServerInfo server) {
        if (server == null || server.getServerName() == null)
            throw new IllegalArgumentException("Server cannot be null");

        IPath path = getRuntime().getLocation();
        if (path == null)
            throw new IllegalArgumentException("Runtime does not exist");

        if (!server.getServerPath().toFile().exists())
            throw new IllegalArgumentException("Server does not exist");
    }

    /**
     * Returns the schema to use for the configuration file at the given path. Returns
     * <code>null</code> if the given file URI isn't associated with or used by this runtime.
     *
     * @param fileURI
     * @return
     */
    public synchronized URL getConfigurationSchemaURL(URI fileURI) {
        updateServerCache(false);

        for (WebSphereServerInfo info : serverInfo) {
            if (info.getConfigurationFileFromURI(fileURI) != null) {
                return getConfigurationSchemaURL();
            }
        }

        IPath path = new Path(fileURI.getPath());
        if ("xml".equals(path.getFileExtension())) {
            // handle any xml file in a user dir but not included by any server
            List<UserDirectory> userDirs = getUserDirectories();
            for (UserDirectory ud : userDirs) {
                URI userDirURI = ud.getPath().toFile().toURI();
                URI relative = URIUtil.canonicalRelativize(userDirURI, fileURI);
                if (!relative.isAbsolute() && ConfigUtils.isServerConfigFile(fileURI)) {
                    return getConfigurationSchemaURL();
                }
            }

            // handle file in the runtime template directory
            IPath runtimePath = getRuntime().getLocation();
            if (runtimePath != null) {
                runtimePath = runtimePath.append("templates");
                if (runtimePath.isPrefixOf(path))
                    return getConfigurationSchemaURL();
            }

            // handle generated file in runtime metadata (for merged config files)
            if (buildMetadataDirectoryPath().isPrefixOf(path)) {
                return getConfigurationSchemaURL();
            }
        }

        return null;
    }

    public synchronized void createMetadata(IJobChangeListener listener) { //currently it is used for runtimeAdded
        SchemaMetadata.getInstance().generateIfMissingOrRemoved(this, listener);
        setSavedMetadataDirectory(buildMetadataDirectoryPath());
    }

    public synchronized void runtimeChanged() {
        IPath oldDir = getSavedMetadataDirectory();
        // Clear the runtime version cache and edition.
        runtimeVersion = null;
        runtimeEdition = null;
        IPath newDir = buildMetadataDirectoryPath();
        if (oldDir == null || !oldDir.equals(newDir)) {
            removeMetadata(oldDir, oldDir != null && !oldDir.equals(newDir), false);
        } else {
            // Remove metadata for any servers that no longer exist.
            WebSphereServerInfo.removeOutOfSyncMetadata(this);
            // Remove the metadata for each server so it will be regenerated.
            List<WebSphereServerInfo> serverInfos = getWebSphereServerInfos();
            for (WebSphereServerInfo serverInfo : serverInfos) {
                IPath metadataDirectory = newDir.append(serverInfo.getMetadataRelativePath());
                serverInfo.removeMetadata(metadataDirectory, true, true);
            }
        }
        // For now, we regenerate the metadata when there is anything changed in the runtime.
        // There is not significant performance impact.  In the future, we can regen the
        // metadata only when the runtime location is changed.
        generateMetadata(null, true);
        setSavedMetadataDirectory(newDir);
        if (classpathHelper != null) {
            classpathHelper.refresh();
        }
        schemaHelper = null;
    }

    public void setSavedMetadataDirectory(IPath dir) {
        savedMetadataDirectoryInCaseLocationIsMoved = dir;
    }

    public IPath getSavedMetadataDirectory() {
        return savedMetadataDirectoryInCaseLocationIsMoved;
    }

    public synchronized void generateMetadata(IJobChangeListener listener, boolean isRegenInfoCache, int metadataTypes) {
        final WebSphereRuntime wsr = this;
        IJobChangeListener restoreListener = new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                // do not have to check status since getPayload will always
                // succeed (it returns the fallback if there was a problem)
                try {
                    String generatorId = getGeneratorId();

                    SchemaMetadata schemaMetadata = SchemaMetadata.getInstance();
                    FeatureListCoreMetadata coreMetadata = FeatureListCoreMetadata.getInstance();
                    FeatureListExtMetadata[] extMetadata = FeatureListExtMetadata.getInstances(wsr);

                    schemaMetadata.generationComplete(generatorId, schemaMetadata.getPayload(wsr));
                    coreMetadata.generationComplete(generatorId, coreMetadata.getPayload(wsr));
                    for (FeatureListExtMetadata exts : extMetadata) {
                        exts.generationComplete(generatorId, exts.getPayload(wsr));
                    }

                } finally {
                    event.getJob().removeJobChangeListener(this);
                }
            }

        };
        IJobChangeListener[] listeners;
        if (listener != null) {
            listeners = new IJobChangeListener[] { listener, restoreListener };
        } else {
            listeners = new IJobChangeListener[] { restoreListener };
        }

        // clear the metadata instances for this runtime to force regeneration
        if (isRegenInfoCache) {
            FeatureListExtMetadata.clearRuntimeInstances(wsr.getRuntime().getId());
        }
        Metadata.generateMetadata(this, listeners, metadataTypes);
        if (isRegenInfoCache)
            WebSphereRuntimeProductInfoCacheUtil.saveProductInfoCache(this, null);
        //reset earSupportLevel for the runtime since the metadata was regenerated
        earSupported = null;
    }

    public IPath buildMetadataDirectoryPath() {
        return Activator.getInstance().getStateLocation().append(getRuntime().getId());
    }

    public boolean metadataDirectoryExists() {
        IPath dirPath = buildMetadataDirectoryPath();
        File dir = dirPath.toFile();
        return dir.exists();
    }

    /** {@inheritDoc} */
    @Override
    public String getGeneratorId() {
        IRuntime runtime = getRuntime();
        return runtime.getId();
    }

    /** {@inheritDoc} */
    @Override
    public IPath getBasePath(IPath root) {
        return root.append(getGeneratorId());
    }

    /** {@inheritDoc} */
    @Override
    public WebSphereRuntime getWebSphereRuntime() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsFeatureListGeneration() {
        IVMInstall vmInstall = getVMInstall();
        String version = getRuntimeVersion();
        if (version == null || "8.5.0.0".equals(version)) { // should not be null
            if (vmInstall != null) {
                if (!LaunchUtil.isIBMJRE(vmInstall)) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Skip feature list generation for non-IBM JRE for 8.5.0.0.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param isRegenInfoCacheKey generate the product information cache if it is true
     */
    @Override
    public synchronized void generateMetadata(IJobChangeListener listener, boolean isRegenInfoCache) {
        // creates the metadata directory if it does not already exist
        IPath dirPath = buildMetadataDirectoryPath();
        File dir = dirPath.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            if (Trace.ENABLED) {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                Trace.trace(Trace.WARNING, "Unable to create " + dir.getAbsolutePath() + " so metadata generation aborted.", t);
            }
            return;
        }
        generateMetadata(listener, isRegenInfoCache, Metadata.ALL_METADATA);
    }

    @Override
    public synchronized void removeMetadata(IPath dir, boolean deleteDirectory, boolean destroy) {
        // Remove metadata for any servers that no longer exist.
        WebSphereServerInfo.removeOutOfSyncMetadata(this);
        // Remove metadata for existing servers.
        List<WebSphereServerInfo> serverInfos = getWebSphereServerInfos();
        for (WebSphereServerInfo serverInfo : serverInfos) {
            IPath metadataDirectory = dir == null ? null : dir.append(serverInfo.getMetadataRelativePath());
            serverInfo.removeMetadata(metadataDirectory, deleteDirectory, destroy);
        }
        IPath metadataDirectory = dir != null ? dir : buildMetadataDirectoryPath();

        removeFile(metadataDirectory, SchemaMetadata.SCHEMA_XSD);
        removeFile(metadataDirectory, FeatureListCoreMetadata.FEATURELIST_XML);
        // must do this before calling Metadata.removeMetadata() since that will clear the instances
        for (FeatureListExtMetadata ext : FeatureListExtMetadata.getInstances(this)) {
            removeFile(metadataDirectory, ext.getFeatureListXMLName());
        }
        Metadata.removeMetadata(getGeneratorId(), destroy);
        WebSphereRuntimeProductInfoCacheUtil.deleteProductInfoCache(this);
        if (deleteDirectory) {
            IPath serversMetadataDir = metadataDirectory.append(WebSphereServerInfo.SERVERS_METADATA_DIR);
            File file = serversMetadataDir.toFile();
            if (file.exists() && !file.delete()) {
                Trace.logError("Unable to delete servers metadata directory " + serversMetadataDir, null);
            }
            file = metadataDirectory.toFile();
            if (file.exists() && !file.delete()) {
                Trace.logError("Unable to delete metadata directory " + metadataDirectory, null);
            }
        }
        // To clear the cache in the SchemaLocationProvider
        // Otherwise, if cancel the server creation wizard, URIs in the cache will still point
        // to the metadata directory deleted above.
        ServerListenerUtil.getInstance().fireRuntimeChangedEvent(getRuntime());
    }

    public synchronized void removeFile(IPath dir, String fileName) {
        IPath path = dir.append(fileName);
        File file = path.toFile();
        FileUtil.deleteFile(file);
    }

    static public boolean rename(File fromFile, File toFile) {
        // File renaming proved to be unreliable because the file would still be locked sometimes.
        // Since it was discovered to be a timing issue we now have a retry mechanism to mitigate the problem.
        final int retryLimit = 4;
        int count = 1;
        while (!fromFile.renameTo(toFile) && count < retryLimit) {
            if (Trace.ENABLED) {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                Trace.trace(Trace.WARNING, "Unable to rename file " + fromFile.getAbsolutePath() + " to " + toFile.getAbsolutePath(), t);
                Trace.trace(Trace.WARNING, "Retry attempt: " + count);
            }
            count++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        if (count < retryLimit) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Done renaming file " + fromFile.getAbsolutePath() + " to " + toFile.getAbsolutePath());
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the complete schema for this runtime.
     * Calling this method causes the metadata to be generated, if it does not already exist.
     *
     * @return the complete schema
     */
    public URL getConfigurationSchemaURL() {
        return SchemaMetadata.getInstance().getSchemaPath(this);
    }

    static public URL getFallbackSchema() {
        return SchemaMetadata.getFallbackSchema();
    }

    public IPath getDefaultUserDirPath() {
        return getRuntimePath(Constants.USER_FOLDER);
    }

    public IPath getDefaultServersPath() {
        return getRuntimePath(SERVERS_FOLDER);
    }

    public IPath getDefaultSharedApplicationPath() {
        return getRuntimePath(SHARED_APP_FOLDER);
    }

    public IPath getDefaultSharedConfigurationPath() {
        return getRuntimePath(SHARED_CONFIG_FOLDER);
    }

    public IPath getDefaultSharedResourcesPath() {
        return getRuntimePath(SHARED_RESOURCES_FOLDER);
    }

    private IPath getRuntimePath(String folder) {
        IPath path = getRuntime().getLocation();
        if (path == null)
            return null;

        return path.append(folder);
    }

    /**
     * Returns the version of the runtime
     *
     * @return the runtime version or <code>null</code> (if the version cannot be determined)
     */
    public String getRuntimeVersion() {
        if (runtimeVersion != null) {
            return runtimeVersion;
        }

        IPath path = getRuntimePropertiesPath();
        if (path == null) {
            return null;
        }
        Properties prop = new Properties();
        FileUtil.loadProperties(prop, path);
        String s = prop.getProperty("com.ibm.websphere.productVersion");
        if (s == null)
            return null;
        return (runtimeVersion = s.trim());
    }

    /**
     * Retrieves the WebSphere Liberty runtime edition.
     *
     * @return The runtime edition.
     */
    public String getRuntimeEdition() {
        if (runtimeEdition != null) {
            return runtimeEdition;
        }

        IPath path = getRuntimePropertiesPath();
        if (path == null) {
            return null;
        }
        Properties prop = new Properties();
        FileUtil.loadProperties(prop, path);
        String s = prop.getProperty("com.ibm.websphere.productEdition");
        if (s == null)
            return null;
        return (runtimeEdition = s.trim());
    }

    /**
     * Returns the default workspace project for this runtime, which may not exist.
     *
     * @return the default project used to hold resources for this runtime
     * @throws CoreException
     */
    public IProject getProject() {
        // check to see if we have an existing project in the runtime
        // location, if we find one we return it
        IPath runtimeLocation = getRuntime().getLocation();
        if (runtimeLocation != null) {
            IPath runtimeUserPath = runtimeLocation.append(Constants.USER_FOLDER);
            if (runtimeUserPath.toFile().exists()) {
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                IProject[] projects = root.getProjects();

                for (IProject p : projects) {
                    if (runtimeUserPath.equals(p.getLocation())) {
                        return p;
                    }
                }
            }
        }

        return ResourcesPlugin.getWorkspace().getRoot().getProject(getRuntime().getName());
    }

    public String getProperty(String id, String defaultValue) {
        return getAttribute(id, defaultValue);
    }

    public void setProperty(String id, String value) {
        setAttribute(id, value);
    }

    public FeatureSet getInstalledFeatures() {
        List<String> features = FeatureList.getFeatures(false, this);
        return new FeatureSet(this, features);
    }

    public List<String> getInstalledFeaturesList() {
        return FeatureList.getFeatures(false, this);
    }

    /**
     * Returns true if feature1 is a subset of feature2.
     *
     * @param feature1
     * @param feature2
     * @return
     */
    public boolean isContainedBy(String feature1, String feature2) {
        return FeatureList.isContainedBy(feature1, feature2, this);
    }

    /**
     * Create the default user directory.
     *
     * @param monitor
     * @return The user directory or null if it could not be created.
     */
    public UserDirectory createDefaultUserDirectory(IProgressMonitor monitor) {
        UserDirectory userDir = getDefaultUserDir();
        if (userDir != null) {
            // Default user directory has already been created, make sure the
            // project has also been created
            createProject(monitor);
            return userDir;
        }

        IPath path = getDefaultUserDirPath();
        if (path == null) {
            Trace.logError("The default user directory path is null. ", null);
            return null;
        }

        File file = path.toFile();
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Trace.logError("Failed to create default user directory: " + file.getPath(), null);
                return null;
            }
        } else if (!file.isDirectory()) {
            Trace.logError("The default user directory already exists but is not a directory: " + file.getPath(), null);
            return null;
        }
        createProject(monitor);
        addUserDirectory(getProject());
        return getDefaultUserDir();
    }

    public UserDirectory getDefaultUserDir() {
        IPath path = getDefaultUserDirPath();
        if (path == null)
            return null;
        List<UserDirectory> userDirs = getUserDirectories();
        for (UserDirectory userDir : userDirs) {
            if (userDir.getPath().equals(path))
                return userDir;
        }
        return null;
    }

    public void createProject(IProgressMonitor monitor) {
        try {
            IProject project = getProject();
            if (project.isAccessible())
                return;

            if (!getDefaultUserDirPath().toFile().exists())
                return;

            ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                @Override
                public void run(IProgressMonitor monitor2) throws CoreException {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Creating default user dir project");
                    try {
                        IRuntime runtime = getRuntime();
                        WebSphereUtil.createUserProject(runtime.getName(), getDefaultUserDirPath(), monitor2);
                        updateServerCache(true);
                    } catch (Exception e) {
                        Trace.logError("Could not create default user dir project", e);
                    }
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Done creating default user dir project");
                }
            }, monitor);
        } catch (CoreException ce) {
            Trace.logError("Error creating default user dir project", ce);
        }
    }

    public void deleteProject(IProgressMonitor monitor) {
        try {
            final IProject project = getProject();
            if (!project.exists())
                return;

            ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                @Override
                public void run(IProgressMonitor monitor2) throws CoreException {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Deleting default user dir project");
                    try {
                        project.delete(false, true, monitor2);
                    } catch (Exception e) {
                        Trace.logError("Could not delete default user dir project", e);
                    }
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Done deleting default user dir project");
                }
            }, monitor);
        } catch (CoreException ce) {
            Trace.logError("Error deleting default user dir project", ce);
        }
    }

    public void getVariables(ConfigVars vars) {
        IPath installPath = getRuntime().getLocation();
        if (installPath != null) {
            vars.add(ConfigVars.WLP_INSTALL_DIR, installPath.toOSString(), ConfigVars.LOCATION_TYPE);
        }
    }

    /**
     * Set the id to match the name and enable constant id. Used when a new runtime
     * is being created to ensure that the id matches the name and that after the
     * runtime is created the id will not change.
     */
    public void setConstantId(String name) {
        if (name != null) {
            setAttribute("id", name);
        }
        setAttribute("id-set", true);
    }

    @Override
    public String toString() {
        return "WebSphereRuntime[" + getRuntime().getLocation() + "]";
    }

    /**
     * @return the schemaHelper
     */
    public SchemaHelper getSchemaHelper() {
        if (schemaHelper == null)
            schemaHelper = new SchemaHelper(getConfigurationSchemaURL());
        return schemaHelper;
    }

    public WebSphereRuntimeClasspathHelper getClasspathHelper() {
        // The classpathHelper is initialized in the initialize() method, but initialize() is not always
        // called. For example, is not called when you use IRuntime.loadAdapter() and the returned value is a
        // WorkingCopy.
        // TODO: IRuntime.loadAdapter() returns a working copy after the liberty runtime is changed, for example,
        // when you add a second user folder. Investigate if this is the correct behavior.
        if (classpathHelper == null) {
            classpathHelper = new WebSphereRuntimeClasspathHelper(this);
            classpathHelper.refresh();
        }
        return classpathHelper;
    }

    public void updateFeature(IPath featureArchivePath, int ticks, String existingFileOption, IProgressMonitor monitor) throws CoreException {
        String option = isValidExistsOption(existingFileOption) ? existingFileOption.toLowerCase() : "ignore";
        installFeature(featureArchivePath, ticks, new String[] { "featureManager", "install", "--acceptLicense", "--when-file-exists=" + option, featureArchivePath.toOSString() },
                       monitor);
    }

    public void installFeature(IPath featureArchivePath, int ticks, IProgressMonitor monitor) throws CoreException {
        installFeature(featureArchivePath, ticks, new String[] { "featureManager", "install", "--acceptLicense", featureArchivePath.toOSString() }, monitor);
    }

    private boolean isValidExistsOption(String option) {
        return "ignore".equalsIgnoreCase(option) || "replace".equalsIgnoreCase(option);
    }

    private void installFeature(IPath featureArchivePath, int ticks, String[] command, IProgressMonitor monitor) throws CoreException {
        long time = System.currentTimeMillis();
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        monitor2.beginTask(Messages.taskInstallFeature, ticks);
        monitor2.subTask(Messages.taskInstallFeature);
        ProcessResult result;
        try {
            ProcessBuilder builder = createProcessBuilder(null, getRuntime().getLocation().append("bin").toFile(), null,
                                                          command);
            Process p = builder.start();
            monitor2.worked(30);
            result = ProcessHelper.waitForProcess(p, 100, 60f, ticks, monitor2);
            int exitValue = result.getExitValue();
            if (exitValue != 0) {
                Trace.logError("Error installing feature. exitValue=" + Integer.valueOf(exitValue) + ": " + result.getOutput(), null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.featureInstallFailedMsg,
                                                                                                Integer.valueOf(exitValue) + ": " + result.getOutput())));
            }
        } catch (CoreException ce) {
            Trace.logError("CoreException during install feature: " + featureArchivePath, ce);
            throw ce;
        } catch (Throwable t) {
            Trace.logError("Error installing feature: " + featureArchivePath, t);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.featureInstallFailedMsg, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
            Trace.tracePerf("Feature install", time);
        }
    }

    /**
     * Reset the server info of the runtime (to the initial state for the class); this forces it to be reread by updateServerCache(...) and getUserDirectories(...).
     *
     * This call (and other fields in this class) are not thread safe; this should only be called when the WebSphereRuntime object instance
     * is known not to potentially be in use by other threads. For example, this method was created for access by the Server Creation Wizard where
     * the runtime object has not (yet) been made available for general use.
     *
     * Added as part of WI 124142.
     */
    public void resetRuntimeServerInfo() {
        serverInfo = new ArrayList<WebSphereServerInfo>();
        serverCacheHash = -1;
        userDirCache = new ArrayList<UserDirectory>();
        userDirHash = -1;

    }

    /** Return the runtime location */
    public IPath getRuntimeLocation() {
        return getRuntime().getLocation();
    }

    public IPath getRemoteUsrMetadataPath() {
        return buildMetadataDirectoryPath().append(Constants.REMOTE_USR_FOLDER);
    }

    public void initializeClasspathHelper() {
        if (classpathHelper == null) {
            classpathHelper = new WebSphereRuntimeClasspathHelper(this);
            classpathHelper.refresh();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadyToGenerateMetadata() {
        return getRuntimeLocation().append("bin").toFile().exists();
    }

    public boolean isOnPremiseSupported() {
        String batch = Constants.INSTALL_UTILITY_CMD;
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            batch += ".bat";

        return getRuntimeLocation().append("bin").append(batch).toFile().exists();
    }

    public void setJAVAEESupportLevel() {
        List<String> features = FeatureList.getFeatures(true, this);
        for (String feature : features) {
            Set<String> categoryElements = FeatureList.getFeatureCategory(feature, this);
            if (categoryElements != null && !categoryElements.isEmpty()) {
                if (categoryElements.contains("JavaEE8Application")) {
                    earSupported = JAVAEESUPPORT.JAVAEE8;
                    // Currently the latest version so break out of the loop
                    break;
                }
                if (categoryElements.contains("JavaEE7Application")) {
                    earSupported = JAVAEESUPPORT.JAVAEE7;
                    // Keep looking for later version
                }
            }
        }

        if (earSupported == null) {
            earSupported = JAVAEESUPPORT.JAVAEE6;
        }
    }

    /**
     * Determines if the EAR with given version is supported by runtime or not
     *
     * @param EAR version
     * @return true if EAR is supported by runtime, false otherwise
     */
    public boolean isEARSupported(String version) {
        if (version == null)
            return false;

        float f = Float.parseFloat(version);

        if (f >= 1.2f) {
            if (earSupported == null) {
                setJAVAEESupportLevel();
            }
            if (f <= earSupported.getVersion()) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean metadataExists() {
        return SchemaMetadata.getInstance().metadataExists(this);
    }

    public String getRuntimeName() {
        return getRuntime().getName();
    }

    @SuppressWarnings("unchecked")
    public String getMetaDataDirName(UserDirectory userDir) {
        IProject project = userDir.getProject();
        if (project != null) {
            return project.getName();
        }

        Map<String, String> map = getAttribute(PROP_USER_DIR_NAME_MAP, (Map<String, String>) null);
        String name = null;
        if (map == null) {
            map = new HashMap<String, String>(5);
        } else {
            name = map.get(userDir.getUniqueId());
        }
        if (name == null) {
            String dirName;
            do {
                dirName = USER_DIR_METADATA_PATH + (++userDirCounter);
            } while (map.containsValue(dirName));
            map.put(userDir.getUniqueId(), dirName);
            name = dirName;
            if (getRuntime().isWorkingCopy()) {
                setAttribute(PROP_USER_DIR_NAME_MAP, map);
            } else {
                IRuntimeWorkingCopy wc = getRuntime().createWorkingCopy();
                WebSphereRuntime wsRuntime = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
                wsRuntime.setAttribute(PROP_USER_DIR_NAME_MAP, map);
                try {
                    wc.save(true, null);
                } catch (CoreException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to save the user directory name map.", e);
                }
            }
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    public UserDirectory getUserDirForMetadataDir(String dirName) {
        for (UserDirectory userDir : getUserDirectories()) {
            if (userDir.getProject() != null && dirName.equals(userDir.getProject().getName())) {
                return userDir;
            }
        }
        UserDirectory userDir = getUserDir(dirName);
        if (userDir == null) {
            Map<String, String> map = getAttribute(PROP_USER_DIR_NAME_MAP, (Map<String, String>) null);
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (dirName.equals(entry.getValue())) {
                        userDir = getUserDir(entry.getKey());
                        break;
                    }
                }
            }
        }
        return userDir;
    }

    public boolean supportsInstallingAdditionalContent() {
        // Newer versions of the runtime should have both the installUtility
        // command and jar, older versions will have the featureManager command.
        String cmdSuffix = getCommandSuffixForOS();
        String cmd = Constants.INSTALL_UTILITY_CMD + cmdSuffix;

        IPath binPath = getRuntimeLocation().append("bin");
        boolean hasInstallUtilCmd = binPath.append(cmd).toFile().exists();
        boolean hasInstallUtilJar = binPath.append("tools").append(Constants.INSTALL_UTILITY_JAR).toFile().exists();
        if (hasInstallUtilCmd && hasInstallUtilJar) {
            return true;
        }

        cmd = Constants.FEATURE_MANAGER_CMD + cmdSuffix;
        if (binPath.append(cmd).toFile().exists()) {
            return true;
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Installing additional content not supported for runtime at: " + getRuntimeLocation().toOSString());
        }
        return false;
    }

    public static boolean supportsInstallingAdditionalContent(String archive) {
        // Newer versions of the runtime should have both the installUtility
        // command and jar, older versions will have the featureManager command.
        File file = new File(archive);
        if (file.exists()) {
            ZipFile zip = null;
            try {
                String cmdSuffix = getCommandSuffixForOS();
                zip = new ZipFile(file);
                String cmd = Constants.INSTALL_UTILITY_CMD + cmdSuffix;
                ZipEntry installUtilCmd = zip.getEntry("wlp/bin/" + cmd);
                ZipEntry installUtilJar = zip.getEntry("wlp/bin/tools/" + Constants.INSTALL_UTILITY_JAR);
                if (installUtilCmd != null && installUtilJar != null) {
                    return true;
                }
                cmd = Constants.FEATURE_MANAGER_CMD + cmdSuffix;
                ZipEntry featureManagerCmd = zip.getEntry("wlp/bin/" + cmd);
                if (featureManagerCmd != null) {
                    return true;
                }
            } catch (Exception e) {
                Trace.logError("Could not read the runtime archive in order to determine if installing additional content is supported", e);
            } finally {
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Installing additional content not supported for: " + archive);
        }
        return false;
    }

    private static String getCommandSuffixForOS() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return ".bat";
        }
        return "";
    }

    // The first path in the returned list is the primary path
    public List<IPath> getRuntimePropertiesPaths() {
        List<IPath> paths = new ArrayList<IPath>();
        IPath runtimeLocation = getRuntime().getLocation();
        if (runtimeLocation == null || runtimeLocation.isEmpty()) {
            return paths;
        }
        IPath path = runtimeLocation.append(RUNTIME_MARKER);
        if (path.toFile().exists()) {
            paths.add(path);
        }
        path = runtimeLocation.append(OPEN_RUNTIME_MARKER);
        if (path.toFile().exists()) {
            paths.add(path);
        }
        return paths;
    }

    private IPath getRuntimePropertiesPath() {
        return getRuntimePropertiesPath(getRuntime().getLocation());
    }

    private static IPath getRuntimePropertiesPath(IPath runtimeLocation) {
        if (runtimeLocation == null || runtimeLocation.isEmpty())
            return null;
        IPath markerPath = runtimeLocation.append(RUNTIME_MARKER);
        if (markerPath.toFile().exists()) {
            return markerPath;
        }
        markerPath = runtimeLocation.append(OPEN_RUNTIME_MARKER);
        if (markerPath.toFile().exists()) {
            return markerPath;
        }
        return null;
    }
}
