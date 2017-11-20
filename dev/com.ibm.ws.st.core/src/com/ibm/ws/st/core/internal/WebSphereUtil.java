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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.Validator.V2;
import org.eclipse.wst.validation.internal.ValManager;
import org.eclipse.wst.validation.internal.ValPrefManagerProject;
import org.eclipse.wst.validation.internal.model.FilterGroup;
import org.eclipse.wst.validation.internal.model.FilterRule;
import org.eclipse.wst.validation.internal.model.GlobalPreferencesValues;
import org.eclipse.wst.validation.internal.model.ProjectPreferences;

import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

@SuppressWarnings("restriction")
public class WebSphereUtil {
    private static final String VALIDATOR_BUILDER = "org.eclipse.wst.validation.validationbuilder";

    private static final String[] VALIDATORS = new String[] {
                                                              "org.eclipse.wst.xsd.core.xsd",
                                                              "org.eclipse.wst.xml.core.xml",
                                                              "com.ibm.ws.st"
    };

    private static final String[] SERVER_EXCLUDE_FOLDERS = new String[] {
                                                                          "apps",
                                                                          "logs",
                                                                          "workarea",
                                                                          "dropins"
    };

    private static final String[] SERVERS_EXCLUDE_FOLDERS = new String[] {
                                                                           ".classCache",
                                                                           ".logs"
    };

    public static WebSphereServer[] getWebSphereServers() {
        IServer[] servers = ServerCore.getServers();
        List<WebSphereServer> list = new ArrayList<WebSphereServer>(3);
        for (IServer server : servers) {
            IServerType st = server.getServerType();
            if (st != null) {
                if (st.getId().startsWith(Constants.SERVER_ID_PREFIX)) {
                    WebSphereServer serv = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                    if (serv != null)
                        list.add(serv);
                }
            }
        }
        return list.toArray(new WebSphereServer[list.size()]);
    }

    public static WebSphereRuntime[] getWebSphereRuntimes() {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        List<WebSphereRuntime> list = new ArrayList<WebSphereRuntime>(3);
        for (IRuntime runtime : runtimes) {
            if (isWebSphereRuntime(runtime)) {
                WebSphereRuntime wrt = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                if (wrt != null) {
                    list.add(wrt);
                }
            }
        }
        return list.toArray(new WebSphereRuntime[list.size()]);
    }

    public static WebSphereRuntime getWebSphereRuntime(String id) {
        IRuntime runtime = ServerCore.findRuntime(id);
        if (runtime == null) {
            return null;
        }
        if (isWebSphereRuntime(runtime)) {
            WebSphereRuntime wrt = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            if (wrt != null) {
                return wrt;
            }
        }

        return null;
    }

    /**
     * Do not call this method from a thread that has a lock on a runtime as
     * this will try to get locks on the other runtimes which can lead to
     * deadlocks. If the thread has a lock on a runtime then it should know
     * what runtime and server it is working with and should not need to call
     * this method.
     */
    public static WebSphereServerInfo[] getWebSphereServerInfos() {
        IRuntime[] runtimes = ServerCore.getRuntimes();
        List<WebSphereServerInfo> list = new ArrayList<WebSphereServerInfo>(5);
        for (IRuntime runtime : runtimes) {
            if (isWebSphereRuntime(runtime)) {
                WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                if (wr != null) {
                    list.addAll(wr.getWebSphereServerInfos());
                }
            }
        }
        return list.toArray(new WebSphereServerInfo[list.size()]);
    }

    public static IPath getServerPath(IServer server) {
        if (server == null)
            return null;

        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        if (wsServer == null)
            return null;
        return wsServer.getServerPath();
    }

    public static WebSphereServer getWebSphereServer(WebSphereServerInfo serverInfo) {
        if (serverInfo == null)
            return null;
        WebSphereRuntime wsRuntime = serverInfo.getWebSphereRuntime();
        if (wsRuntime == null)
            return null;

        IRuntime runtime = wsRuntime.getRuntime();
        WebSphereServer[] servers = getWebSphereServers();
        for (WebSphereServer server : servers) {
            if (runtime.equals(server.getServer().getRuntime()) && serverInfo.getServerName().equals(server.getServerName())
                && serverInfo.getUserDirectory().equals(server.getUserDirectory())) {
                return server;
            }
        }
        return null;
    }

    public static File createUserProject(File file, IProgressMonitor monitor) throws IOException {
        if (file.exists()) {
            File[] f = file.listFiles();
            if (f != null && f.length > 0)
                throw new IOException("Folder not empty");
        } else if (!file.mkdirs())
            throw new IOException("Could not create folders");

        File f = new File(file, Constants.SERVERS_FOLDER);
        if (!f.mkdir())
            throw new IOException("Could not create folders");

        File sharedFile = new File(file, Constants.SHARED_FOLDER);
        if (!f.mkdir())
            throw new IOException("Could not create folders");

        f = new File(sharedFile, Constants.CONFIG_FOLDER);
        if (!f.mkdir())
            throw new IOException("Could not create folders");

        f = new File(sharedFile, Constants.APPS_FOLDER);
        if (!f.mkdir())
            throw new IOException("Could not create folders");

        f = new File(sharedFile, Constants.RESOURCES_FOLDER);
        if (!f.mkdir())
            throw new IOException("Could not create folders");

        return file;
    }

    /**
     * Creates a user project (WLP_USER_DIR) with the given name.
     * If the project already exists, ensure it is setup correctly.
     *
     * @param name the project name
     * @param path the project location, or <code>null</code> to use the default project location
     * @param monitor a progress monitor, or <code>null</code> if progress reporting
     *            is not required
     * @return a basic (empty) WLP_USER_DIR contained in a project
     * @throws CoreException
     */
    public static IProject createUserProject(String name, IPath path, IProgressMonitor monitor) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);

        if (project.exists()) {
            if (!project.isOpen())
                project.open(monitor);
            return project;
        }

        // ensure it is marked as a server project
        if (!ServerPlugin.getProjectProperties(project).isServerProject())
            ServerPlugin.getProjectProperties(project).setServerProject(true, monitor);

        // ensure project is at the right path
        IProjectDescription pd = ResourcesPlugin.getWorkspace().newProjectDescription(name);
        if (path != null)
            pd.setLocation(path);

        // ensure project has the validator builder
        ICommand[] buildSpec = pd.getBuildSpec();
        boolean hasValidator = false;
        if (buildSpec != null) {
            for (ICommand command : buildSpec)
                if (VALIDATOR_BUILDER.equals(command.getBuilderName()))
                    hasValidator = true;
        }
        if (!hasValidator) {
            ICommand command = pd.newCommand();
            command.setBuilderName(VALIDATOR_BUILDER);

            if (buildSpec == null || buildSpec.length == 0) {
                buildSpec = new ICommand[] { command };
            } else {
                int size = buildSpec.length;
                ICommand[] newSpec = new ICommand[size + 1];
                System.arraycopy(buildSpec, 0, newSpec, 0, size);
                newSpec[size] = command;
                buildSpec = newSpec;
            }

            pd.setBuildSpec(buildSpec);
        }

        project.create(pd, monitor);

        if (!project.isOpen())
            project.open(monitor);

        // filter .* and workarea folders, if there are no existing filters
        // We must not exclude the .setttings folder. RTC 111601
        IResourceFilterDescription[] filter = project.getFilters();
        if (filter == null || filter.length == 0) {
            FileInfoMatcherDescription workareaFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-workarea");
            project.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.INHERITABLE, workareaFilter,
                                 IResource.BACKGROUND_REFRESH, monitor);

            FileInfoMatcherDescription dotSettingsFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-.settings");
            FileInfoMatcherDescription notDotSettingsFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.notFilterMatcher", new FileInfoMatcherDescription[] { dotSettingsFilter });

            FileInfoMatcherDescription dotProjectFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-.project");
            FileInfoMatcherDescription notDotProjectFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.notFilterMatcher", new FileInfoMatcherDescription[] { dotProjectFilter });

            FileInfoMatcherDescription dotJavaOverlayFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-.java-overlay");
            FileInfoMatcherDescription notDotJavaOverlayFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.notFilterMatcher", new FileInfoMatcherDescription[] { dotJavaOverlayFilter });

            FileInfoMatcherDescription dotStarFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-.*");
            FileInfoMatcherDescription andFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.andFilterMatcher", new FileInfoMatcherDescription[] { notDotSettingsFilter,
                                                                                                                                                            notDotProjectFilter,
                                                                                                                                                            notDotJavaOverlayFilter,
                                                                                                                                                            dotStarFilter });

            project.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.FILES
                                 | IResourceFilterDescription.INHERITABLE, andFilter,
                                 IResource.BACKGROUND_REFRESH, monitor);

            // WASRTC 127124 - Filter out the messageStore directory because it contains temporary files
            FileInfoMatcherDescription messageStoreFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-messageStore");

            project.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FOLDERS
                                 | IResourceFilterDescription.INHERITABLE, messageStoreFilter,
                                 IResource.BACKGROUND_REFRESH, monitor);

            // WASRTC 127124 - Filter out messages.log, as it will always be out of sync with the file system when the server is running (causing search to throw an out of sync error)
            FileInfoMatcherDescription messagesLogFilter = new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-messages*.log");

            project.createFilter(IResourceFilterDescription.EXCLUDE_ALL | IResourceFilterDescription.FILES
                                 | IResourceFilterDescription.INHERITABLE, messagesLogFilter,
                                 IResource.BACKGROUND_REFRESH, monitor);

        }

        // create default resources
        IFolder folder = project.getFolder(Constants.SERVERS_FOLDER);
        if (!folder.exists())
            folder.create(true, true, monitor);

        IFolder sharedFolder = project.getFolder(Constants.SHARED_FOLDER);
        if (!sharedFolder.exists())
            sharedFolder.create(true, true, monitor);

        folder = sharedFolder.getFolder(Constants.CONFIG_FOLDER);
        if (!folder.exists())
            folder.create(true, true, monitor);

        folder = sharedFolder.getFolder(Constants.APPS_FOLDER);
        if (!folder.exists())
            folder.create(true, true, monitor);

        folder = sharedFolder.getFolder(Constants.RESOURCES_FOLDER);
        if (!folder.exists())
            folder.create(true, true, monitor);

        return project;
    }

    /**
     * Creates a new file with the given name in the given folder. If the file is a 'known' file
     * like bootstrap.properties, jvm.options, or server.env, a template will be used to create
     * the initial contents of the file. Otherwise, the created file will be empty.
     *
     * @param folder a folder
     * @param name a file name
     * @param monitor a progress monitor, or <code>null</code> if progress reporting
     *            is not required
     * @return the created file
     * @throws IOException
     */
    public static IFile createFile(IFolder folder, String name, IProgressMonitor monitor) throws CoreException {
        IProject project = folder.getProject();

        if (project == null || !project.exists())
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project does not exist"));

        if (!project.isOpen())
            project.open(monitor);

        IFile file = folder.getFile(name);
        if (file.exists())
            return file;

        URL url = Activator.getInstance().getBundle().getEntry("template/" + name);
        if (url == null) {
            file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
        } else {
            try {
                file.create(url.openStream(), true, monitor);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not create file", e);
            }
        }

        return file;
    }

    /**
     * Creates a new file with the given name in the given folder. If the file is a 'known' file
     * like bootstrap.properties, jvm.options, or server.env, a template will be used to create
     * the initial contents of the file. Otherwise, the created file will be empty.
     *
     * @param folder a folder
     * @param name a file name
     * @param monitor a progress monitor, or <code>null</code> if progress reporting
     *            is not required
     * @return the created file
     * @throws IOException
     */
    public static File createFile(File folder, String name, IProgressMonitor monitor) throws IOException {
        if (folder == null || !folder.exists() || !folder.isDirectory())
            throw new IOException("Folder does not exist or is not a directory");

        File f = new File(folder, name);
        if (f.exists())
            return f;

        URL url = Activator.getInstance().getBundle().getEntry("template/" + name);
        if (url == null) {
            if (!f.createNewFile())
                throw new IOException("Could not create file");
        } else {
            BufferedReader br = null;
            PrintWriter pw = null;
            try {
                br = new BufferedReader(new InputStreamReader(url.openStream()));
                pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f)));
                String s = br.readLine();
                while (s != null) {
                    pw.println(s);
                    s = br.readLine();
                }
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not create file", e);
                throw new IOException("Could not create file");
            } finally {
                try {
                    if (br != null)
                        br.close();
                } catch (Exception e) {
                    // ignore
                }
                try {
                    if (pw != null)
                        pw.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return f;
    }

    /**
     * Compare the base version number with the compare version number that is coming
     * from the Liberty runtime version detection.
     * The version number is assumed in the form of xxx.xxx.xxx.xxx ,e.g. 5.0.1.1.
     *
     * @param baseVersion
     * @param compareVersion
     * @return Return true if the compareVersion is bigger than or equals to the baseVersion
     */
    public static boolean isGreaterOrEqualVersion(String baseVersion, String compareVersion) {
        // Take care of the null case first.
        if (compareVersion == null || baseVersion == null) {
            return false;
        }

        if (compareVersion.endsWith("-preview")) {
            return true;
        }

        StringTokenizer baseTokenizer = new StringTokenizer(baseVersion, ".");
        StringTokenizer compareTokenizer = new StringTokenizer(compareVersion, ".");
        int curBaseVersion = 0;
        int curCompareVersion = 0;
        while (baseTokenizer.hasMoreElements() && compareTokenizer.hasMoreElements()) {
            try {
                curCompareVersion = Integer.parseInt(compareTokenizer.nextToken());
            } catch (NumberFormatException e) {
                // Any non-number value is considered to be bigger than numbers since
                // alpha and beta versions will may have letters instead of numbers.
                curCompareVersion = Integer.MAX_VALUE;
            }
            try {
                curBaseVersion = Integer.parseInt(baseTokenizer.nextToken());
                if (curBaseVersion > curCompareVersion) {
                    return false;
                } else if (curCompareVersion > curBaseVersion) {
                    return true;
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }

        // If base has more tokens than compare, need to make sure they are
        // all 0, otherwise base is greater than compare
        while (baseTokenizer.hasMoreElements()) {
            try {
                curBaseVersion = Integer.parseInt(baseTokenizer.nextToken());
                if (curBaseVersion > 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }

        return true;
    }

    /**
     * Tests if the runtime is a WebSphere runtime
     *
     * @param runtime the runtime
     * @return true if it is a WebSphere runtime; false otherwise
     */
    public static boolean isWebSphereRuntime(IRuntime runtime) {
        if (runtime == null)
            return false;

        IRuntimeType runtimeType = runtime.getRuntimeType();
        return runtimeType != null && runtimeType.getId().startsWith(Constants.RUNTIME_ID_PREFIX);
    }

    protected static synchronized void configureValidatorsForRuntimeProject(IProject project) {
        long time = System.currentTimeMillis();
        if (project == null)
            return;
        IFolder serversFolder = project.getFolder("servers");
        IPath serversPath = serversFolder.getLocation();
        if (serversPath == null)
            return;

        // Depending on timing, the IResources APIs may not return servers that exist in the file system.
        // Use the io.File API instead.
        File servers = serversPath.toFile();
        if (!servers.exists())
            return;

        File[] dirs = servers.listFiles();
        String separater = ", ";
        StringBuilder sb = new StringBuilder();
        ArrayList<String> serverNames = new ArrayList<String>();
        for (File r : dirs) {
            if (isServerFolder(r)) {
                String s = r.getName();
                serverNames.add(s);
                sb.append(s).append(separater);
            }
        }

        if (serverNames.isEmpty())
            return;

        ProjectPrefs prefs = new ProjectPrefs(project);
        String oldServers = prefs.getRuntimeProjectValidationConfiguredServers();

        ArrayList<String> oldServerNames = new ArrayList<String>();
        if (oldServers != null) {
            StringTokenizer st = new StringTokenizer(oldServers, separater);
            while (st.hasMoreTokens())
                oldServerNames.add(st.nextToken());
        }

        if (oldServerNames.size() == serverNames.size()) {
            boolean equals = true;
            for (String oldServerName : oldServerNames) {
                if (!serverNames.contains(oldServerName)) {
                    equals = false;
                    break;
                }
            }
            if (equals) {
                if (Trace.ENABLED)
                    Trace.tracePerf("Update runtime project validation rules when there is no server change. " + project.getName(), time);
                return;
            }
        }

        ArrayList<String> removedServerNames = new ArrayList<String>(); //servers that have been removed since last update
        // After the loop, the serverNames contains the new servers after last update
        for (String serverName : oldServerNames) {
            if (serverNames.contains(serverName))
                serverNames.remove(serverName);
            else
                removedServerNames.add(serverName);
        }

        List<String> removedServerRulePaths = getServerExcludeFolders(removedServerNames);
        List<String> addedServerRulePaths = getServerExcludeFolders(serverNames);

        // We want it takes effect when the vpm.savePreferences(newPrefs) is called.
        // So, we do modification to the copy.
        Validator[] existingValidators = ValManager.getDefault().getValidators(project);
        Validator[] validators = new Validator[existingValidators.length];
        for (int i = 0; i < existingValidators.length; i++)
            validators[i] = existingValidators[i].copy();

        for (Validator val : validators) {
            boolean include = false;
            for (String s : VALIDATORS) {
                if (val.getId().startsWith(s)) {
                    include = true;
                    break;
                }
            }
            if (!include) {
                val.setBuildValidation(false);
                val.setManualValidation(false);
            } else {
                V2 v2 = val.asV2Validator();
                FilterGroup[] groups = v2.getGroups();

                FilterGroup exclude = null;
                for (FilterGroup group : groups) {
                    if (group.isExclude()) {
                        exclude = group;
                        break;
                    }
                }

                FilterRule[] addedRules = new FilterRule[addedServerRulePaths.size()];
                int i = 0;
                for (String path : addedServerRulePaths) {
                    addedRules[i] = FilterRule.createFile(path, true, FilterRule.File.FileTypeFolder);
                    i++;
                }

                Collection<FilterRule> modifiedRules = new ArrayList<FilterRule>();
                if (exclude == null) {
                    exclude = FilterGroup.create(true, addedRules);
                    v2.add(exclude);
                } else {
                    FilterRule[] existing = exclude.getRules();
                    for (FilterRule rule : existing) {
                        String pattern = rule.getPattern();
                        if (!removedServerRulePaths.contains(pattern))
                            modifiedRules.add(rule);
                    }
                    for (FilterRule rule : addedRules)
                        modifiedRules.add(rule);

                    FilterRule[] newRules = modifiedRules.toArray(new FilterRule[modifiedRules.size()]);
                    FilterGroup newExclude = FilterGroup.create(true, newRules);
                    v2.replaceFilterGroup(exclude, newExclude);
                }
            }
        }

        // turn on global setting to "allow project override"
        GlobalPreferencesValues globalPrefs = ValManager.getDefault().getGlobalPreferences().asValues();
        if (!globalPrefs.override) {
            globalPrefs.override = true;
            ValManager.getDefault().replace(globalPrefs);
        }

        // turn on new project's "override" setting and save the new preferences to make it in effect
        ProjectPreferences oldPrefs = ValManager.getDefault().getProjectPreferences(project);
        ProjectPreferences newPrefs = new ProjectPreferences(project, true, oldPrefs.getSuspend(), validators);
        ValPrefManagerProject vpm = new ValPrefManagerProject(project);
        vpm.savePreferences(newPrefs);

        prefs.setRuntimeProjectValidationConfiguredServers(sb.toString());
        prefs.save();

        if (Trace.ENABLED)
            Trace.tracePerf("Update runtime project validation rules when there are server changes. " + project.getName(), time);
    }

    protected static boolean isServerFolder(File folder) {
        if (folder != null && folder.isDirectory()) {
            File serverFile = new File(folder, Constants.SERVER_XML);
            if (serverFile.exists())
                return true;
        }
        return false;
    }

    private static List<String> getServerExcludeFolders(List<String> serverNames) {
        ArrayList<String> list = new ArrayList<String>();
        // server level
        for (String serverName : serverNames) {
            for (String s : SERVER_EXCLUDE_FOLDERS) {
                StringBuilder sbr = new StringBuilder("servers");
                sbr.append('/').append(serverName).append('/').append(s);
                list.add(sbr.toString());
            }
        }

        // servers level
        for (String s : SERVERS_EXCLUDE_FOLDERS) {
            StringBuilder sbr = new StringBuilder("servers");
            sbr.append('/').append(s);
            list.add(sbr.toString());
        }

        return list;
    }

    /**
     * Find which project this file belongs to and get the targeted runtime for
     * the project.
     *
     * @param fileURI The file for which to get the targeted runtime
     * @return The targeted runtime for the file or null if none
     * @throws CoreException
     */
    public static WebSphereRuntime getTargetedRuntime(URI fileURI) throws CoreException {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspaceRoot.findFilesForLocationURI(fileURI);
        if (files.length > 0 && ConfigUtils.isServerConfigFile(files[0])) {
            IProject project = files[0].getProject();
            if (project != null) {
                WebSphereRuntime wrt = WebSphereUtil.getTargetedRuntime(project);
                return wrt;
            }
        }
        return null;
    }

    /**
     * Get the targeted runtime for the given project.
     *
     * @param project The project for which to get the targeted runtime
     * @return The targeted runtime or null if none
     * @throws CoreException
     */
    public static WebSphereRuntime getTargetedRuntime(IProject project) throws CoreException {
        IFacetedProject fp = ProjectFacetsManager.create(project);
        if (fp != null) {
            Set<org.eclipse.wst.common.project.facet.core.runtime.IRuntime> targetRuntimes = fp.getTargetedRuntimes();
            if (!targetRuntimes.isEmpty()) {
                org.eclipse.wst.common.project.facet.core.runtime.IRuntime facetRuntime = targetRuntimes.iterator().next();
                IRuntime rt = FacetUtil.getRuntime(facetRuntime);
                if (rt != null) {
                    WebSphereRuntime wrt = (WebSphereRuntime) rt.loadAdapter(WebSphereRuntime.class, null);
                    return wrt;
                }
            }
        }
        return null;
    }

    // Returns the last folder or file name in path
    public static String getLastName(String path) {
        String filePath = path;
        String[] list = filePath.split("/");
        return list[list.length - 1];
    }

    // Return true if the remoteUserPath is under the wlp.install.dir and has the default user dir name
    public static boolean isDefaultRemoteUserDirectory(JMXConnection jmxConnection, String remoteUserPath) throws Exception {
        String wlpDirMetaData = (String) ((CompositeData) jmxConnection.getMetadata(Constants.WLP_INSTALL_VAR, "a")).get("fileName");
        if (WebSphereUtil.getLastName(remoteUserPath).equals(Constants.USER_FOLDER) && remoteUserPath.contains(wlpDirMetaData))
            return true;
        return false;
    }

    public static String getUniqueRuntimeName(String proposedName, IRuntime[] runtimes) {
        String name = proposedName;
        boolean found = true;
        int index = 2;
        while (found) {
            found = false;
            for (IRuntime runtime : runtimes) {
                if (name.equals(runtime.getName())) {
                    found = true;
                    name = NLS.bind(Messages.runtimeName, name, Integer.valueOf(index));
                    index++;
                    break;
                }
            }
        }
        return name;
    }

    public static String getUniqueServerName(String proposedName, IServer[] servers) {
        String name = proposedName;
        boolean found = true;
        int index = 2;
        while (found) {
            found = false;
            for (IServer server : servers) {
                if (name.equals(server.getName())) {
                    found = true;
                    name = NLS.bind(Messages.runtimeName, name, Integer.valueOf(index));
                    index++;
                    break;
                }
            }
        }
        return name;
    }

    /**
     * Get the WebSphereServer given the IServer
     * 
     * @param server The IServer - should not be null
     * @return The WebSphereServer or null if the server is not a WebSphere server
     */
    public static WebSphereServer getWebSphereServer(IServer server) {
        if (server == null) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "The server should not be null.");
            }
            return null;
        }
        WebSphereServer wsServer = server.getAdapter(WebSphereServer.class);
        if (wsServer == null) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "The server is not a WebSphereServer as expected: " + server.getName());
            }
        }
        return wsServer;
    }
}