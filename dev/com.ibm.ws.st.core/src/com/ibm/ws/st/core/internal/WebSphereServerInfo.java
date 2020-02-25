/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.config.Bootstrap;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigurationDropinsFolder;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationIncludeFilter;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.core.internal.config.JVMOptions;
import com.ibm.ws.st.core.internal.config.SchemaHelper;
import com.ibm.ws.st.core.internal.config.ServerEnv;
import com.ibm.ws.st.core.internal.generation.FeatureListCoreMetadata;
import com.ibm.ws.st.core.internal.generation.FeatureListExtMetadata;
import com.ibm.ws.st.core.internal.generation.IMetadataGenerator;
import com.ibm.ws.st.core.internal.generation.Metadata;
import com.ibm.ws.st.core.internal.generation.SchemaMetadata;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

public class WebSphereServerInfo implements IMetadataGenerator {

    public static final String SERVERS_METADATA_DIR = "serversMetadata";

    private final String serverName;
    private Bootstrap bootstrap;
    private final Map<IPath, JVMOptions> jvmOptionsFiles;
    private ServerEnv serverEnv;
    private ServerEnv sharedServerEnv;
    private ServerEnv etcServerEnv;

    private final UserDirectory userDir;
    private final WebSphereRuntime runtime;
    private ConfigurationFile file;
    private Map<URI, ConfigurationFile> map;
    private long lastUpdate = 0;

    private final Object infoLock;

    // All of the variables including the user directory and runtime variables
    private ConfigVars allVars;

    // Just the resolved variables
    private ConfigVars resolvedVars;

    private SchemaHelper schemaHelper = null;

    public WebSphereServerInfo(String serverName, UserDirectory userDir, WebSphereRuntime runtime) {
        this.serverName = serverName;
        this.userDir = userDir;
        this.runtime = runtime;
        this.jvmOptionsFiles = new HashMap<IPath, JVMOptions>();
        infoLock = runtime == null ? this : runtime;
    }

    public String getServerName() {
        return serverName;
    }

    public IFolder getServerFolder() {
        if (userDir.getProject() == null || serverName == null)
            return null;
        return userDir.getProject().getFolder(Constants.SERVERS_FOLDER).getFolder(serverName);
    }

    public IPath getServerPath() {
        if (serverName == null)
            return null;
        return userDir.getServersPath().append(serverName);
    }

    public IPath getServerAppsPath() {
        if (serverName == null)
            return null;
        return getServerPath().append(Constants.APPS_FOLDER);
    }

    public IPath getServerOutputPath() {
        if (serverName == null)
            return null;
        ConfigVars v = new ConfigVars();
        addServerEnvVars(v);
        String wlpOutputDir = v.getValue(Constants.ENV_VAR_PREFIX + Constants.WLP_OUTPUT_DIR);
        if (wlpOutputDir != null && !wlpOutputDir.isEmpty()) {
            return new Path(wlpOutputDir).append(serverName);
        }
        return userDir.getOutputPath().append(serverName);
    }

    public URI getServerURI() {
        if (serverName == null)
            return null;
        return getServerPath().toFile().toURI();
    }

    @Override
    public WebSphereRuntime getWebSphereRuntime() {
        return runtime;
    }

    public UserDirectory getUserDirectory() {
        return userDir;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public ExtendedConfigFile getJVMOptions(IPath label) {
        return jvmOptionsFiles.get(label);
    }

    public void putJVMOptions(IPath label, JVMOptions location) {
        jvmOptionsFiles.put(label, location);
    }

    public Collection<JVMOptions> getJVMOptionsFiles() {
        Collection<JVMOptions> jvmOptionsFilesList = jvmOptionsFiles.values();
        jvmOptionsFilesList.removeAll(Collections.singleton(null));
        return jvmOptionsFilesList;
    }

    public ExtendedConfigFile getServerEnv() {
        return serverEnv;
    }

    public ExtendedConfigFile getSharedServerEnv() {
        return sharedServerEnv;
    }

    public ExtendedConfigFile getEtcServerEnv() {
        return etcServerEnv;
    }

    public ConfigurationFile getConfigRoot() {
        synchronized (infoLock) {
            updateConfigurationCache();
            return file;
        }
    }

    public URI[] getConfigurationURIs() {
        synchronized (infoLock) {
            updateConfigurationCache();
            Set<URI> set = map.keySet();
            return set.toArray(new URI[set.size()]);
        }
    }

    public ConfigurationFile[] getConfigurationFiles() {
        synchronized (infoLock) {
            updateConfigurationCache();
            Collection<ConfigurationFile> files = map.values();
            return files.toArray(new ConfigurationFile[files.size()]);
        }
    }

    /**
     * Refresh the server's configuration cache.
     * If anything changed, a change event will be fired and <code>true</code> is returned.
     *
     * @return <code>true</code> if the cache was affected, and <code>false</code> otherwise
     */
    public boolean updateCache() {
        boolean changed = false;

        // Synchronizing this block since the file, jvmOptions and serverEnv objects are being
        // read/modified and this will only take longer on the first time. After that
        // the results are cached so we won't be blocking for too long.
        synchronized (infoLock) {
            if (updateConfigurationCache())
                changed = true;

            // If there's no configuration file for some reason then we cannot update the cache
            else if (file == null)
                return false;

            // we could have updated the configuration cache in a different
            // thread, e.g. validation, so we check to see if the configuration
            // file is newer than the last update time stamp
            else if (file.isNewerThan(lastUpdate, true))
                changed = true;

            lastUpdate = System.currentTimeMillis();

            if (updateBootstrap())
                changed = true;

            IPath[] paths = { getServerPath(), getConfigDefaultDropinsPath(), getConfigOverrideDropinsPath(),
                              getUserDirectory().getSharedPath() };
            for (IPath path : paths) {
                IPath jvmOptionsPath = path.append(ExtendedConfigFile.JVM_OPTIONS_FILE);
                File jvmOptionsFile = jvmOptionsPath.toFile();
                JVMOptions currJvmOptions = (JVMOptions) getJVMOptions(path);

                if (jvmOptionsFile.exists()) {
                    boolean flag = false;
                    //for Remote Server also check if the file has changed
                    if (currJvmOptions != null && getUserDirectory().getRemoteUserPath() != null)
                        flag = currJvmOptions.hasChanged();

                    if (currJvmOptions == null || flag) {
                        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
                        IFile file = workspaceRoot.getFileForLocation(jvmOptionsPath);
                        currJvmOptions = new JVMOptions(jvmOptionsFile, file);
                        changed = true;
                    }
                } else {
                    if (currJvmOptions != null) {
                        changed = true;
                        currJvmOptions = null;
                    }
                }

                putJVMOptions(path, currJvmOptions);
            }

            if (updateServerEnv())
                changed = true;
        }

        // This is purposely left unsynchronized since server changed events can trigger
        // time-consuming operations and we don't need to block during those operations.
        if (changed) {
            ServerListenerUtil.getInstance().fireServerChangedEvent(this);
        }

        return changed;
    }

    private boolean isConfigCacheDirty() {
        synchronized (infoLock) {
            if (map == null)
                return false;

            Collection<ConfigurationFile> files = map.values();
            for (ConfigurationFile file : files) {
                if (file.hasChanged() || file.hasOutOfSyncLocalIncludes())
                    return true;
            }
            return false;
        }
    }

    /**
     * Allow extenders to provide their own BootstrapFile
     *
     * @return
     */
    protected File getBootstrapFile() {
        return getServerPath().append(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE).toFile();
    }

    private boolean updateBootstrap() {
        synchronized (infoLock) {
            boolean changed = false;
            try {
                // get the bootstrap.properties file
                final File bootstrapFile = getBootstrapFile();

                // we have a boostrap.properties file
                if (bootstrapFile.exists()) {
                    // if we did not load the file or the contents have changed,
                    // we load the bootstrap properties
                    if (bootstrap == null || bootstrap.hasChanged()) {
                        IFile file = null;
                        if (userDir.getProject() != null)
                            file = getServerFolder().getFile(ExtendedConfigFile.BOOTSTRAP_PROPS_FILE);
                        bootstrap = new Bootstrap(bootstrapFile, file);
                        // reset the configuration vars - reloaded upon request
                        allVars = null;
                        resolvedVars = null;
                        changed = true;
                    }
                }
                // we do not have a bootstrap.properties file, but we seem to have
                // a previously loaded version
                else if (bootstrap != null) {
                    bootstrap = null;
                    // reset the configuration vars - reloaded upon request
                    allVars = null;
                    resolvedVars = null;
                    changed = true;
                }
            } catch (IOException ioe) {
                Trace.logError("Error updating server cache configuration: " + serverName, ioe);
            }
            return changed;
        }
    }

    /**
     * Allow extenders to provide their own server.env file
     *
     * @return
     */
    protected File getServerEnvFile() {
        return getServerPath().append(ExtendedConfigFile.SERVER_ENV_FILE).toFile();
    }

    private boolean updateServerEnv() {
        synchronized (infoLock) {
            boolean changed = false;
            try {
                File serverEnvFile = getServerEnvFile();
                if (serverEnvFile.exists()) {
                    if (serverEnv == null || serverEnv.hasChanged()) {
                        changed = true;
                        IFile file = null;
                        if (userDir.getProject() != null)
                            file = getServerFolder().getFile(ExtendedConfigFile.SERVER_ENV_FILE);
                        serverEnv = new ServerEnv(serverEnvFile, file);
                    }
                } else if (serverEnv != null) {
                    changed = true;
                    serverEnv = null;
                }

                File sharedServerEnvFile = getUserDirectory().getSharedPath().append(ExtendedConfigFile.SERVER_ENV_FILE).toFile();
                if (sharedServerEnvFile.exists()) {
                    if (sharedServerEnv == null || sharedServerEnv.hasChanged()) {
                        changed = true;
                        sharedServerEnv = new ServerEnv(sharedServerEnvFile, null);
                    }
                } else if (sharedServerEnv != null) {
                    changed = true;
                    sharedServerEnv = null;
                }

                File etcServerEnvFile = runtime.getRuntimeLocation().append(ExtendedConfigFile.ETC_DIR).append(ExtendedConfigFile.SERVER_ENV_FILE).toFile();
                if (etcServerEnvFile.exists()) {
                    if (etcServerEnv == null || etcServerEnv.hasChanged()) {
                        changed = true;
                        etcServerEnv = new ServerEnv(etcServerEnvFile, null);
                    }
                } else if (etcServerEnv != null) {
                    changed = true;
                    etcServerEnv = null;
                }
            } catch (IOException e) {
                Trace.logError("Error updating server cache configuration: " + serverName, e);
            }

            if (changed) {
                allVars = null;
                resolvedVars = null;
            }
            return changed;
        }
    }

    private boolean updateConfigurationCache() {
        synchronized (infoLock) {
            if (map != null && !isConfigCacheDirty())
                return false;

            // TODO incremental refresh - should only reload parts of the config tree
            // from changed files and down
            try {
                IPath path = getServerPath();

                URI configRoot = path.append(Constants.SERVER_XML).toFile().toURI();

                map = new HashMap<URI, ConfigurationFile>(4);
                file = new ConfigurationFile(configRoot, userDir, this);

                List<ConfigurationFile> files = new ArrayList<ConfigurationFile>();
                final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
                file.getAllConfigFiles(files, includeFilter);

                for (ConfigurationFile cf : files) {
                    URI uri = URIUtil.getCanonicalURI(cf.getURI());
                    map.put(uri, cf);
                }
            } catch (IOException ioe) {
                Trace.logError("Error updating server cache configuration: " + serverName, ioe);
            }
            return true;
        }
    }

    /**
     * Set up predefined and bootstrap configuration variables.
     */
    private void createConfigVars() {
        allVars = new ConfigVars();
        resolvedVars = new ConfigVars();

        userDir.getVariables(allVars, false);
        userDir.getVariables(resolvedVars, true);

        addServerVars(allVars);
        addServerVars(resolvedVars);
    }

    private void addServerVars(ConfigVars vars) {
        if (serverName != null) {
            vars.add(ConfigVars.SERVER_CONFIG_DIR, getServerPath().toOSString(), ConfigVars.LOCATION_TYPE);
            vars.add(ConfigVars.SERVER_OUTPUT_DIR, getServerOutputPath().toOSString(), ConfigVars.LOCATION_TYPE);
            vars.add(ConfigVars.WLP_SERVER_NAME, getServerName(), ConfigVars.STRING_TYPE);
        }
        if (bootstrap != null) {
            Set<Map.Entry<String, String>> entries = bootstrap.getVariables(vars);

            String remoteUserPath = null;
            if (getUserDirectory().getRemoteUserPath() != null)
                remoteUserPath = getUserDirectory().getRemoteUserPath().toString();
            String userDir = getUserDirectory().getPath().toOSString();

            //replace the remoteUserDir with workspaceDir in local machine
            if (remoteUserPath != null && entries != null) {
                for (Map.Entry<String, String> entry : entries) {
                    String value = vars.getValue(entry.getKey());
                    if (value.startsWith(remoteUserPath)) {
                        value = value.replace(remoteUserPath, userDir);
                        vars.add(entry.getKey(), value, ConfigVars.STRING_TYPE);
                    }
                }
            }
        }

        addServerEnvVars(vars);
    }

    private void addServerEnvVars(ConfigVars vars) {
        // Do the env vars from the <runtime install dir>/etc directory first
        // since it is the lowest priority server.env file
        if (etcServerEnv != null) {
            etcServerEnv.getVariables(vars);
        }

        // Followed by env vars from the <user dir>/shared directory
        if (sharedServerEnv != null) {
            sharedServerEnv.getVariables(vars);
        }

        // And finally the env vars from the config dir
        if (serverEnv != null) {
            serverEnv.getVariables(vars);
        }
    }

    public ConfigurationFile getConfigurationFileFromURI(URI uri) {
        synchronized (infoLock) {
            updateConfigurationCache();
            ConfigurationFile cf = map.get(uri);
            if (cf == null) {
                cf = map.get(URIUtil.getCanonicalURI(uri));
            }
            return cf;
        }
    }

    /**
     * Returns the URI of an included file, based on the URI of the parent document and include text.
     *
     * @param uri
     * @param include
     * @return the URI of the included document
     */
    public URI resolve(URI baseUri, String include) {
        synchronized (infoLock) {
            if (include == null)
                return null;

            // fill the config vars
            fillConfigVars();

            // resolve any variables in the include path
            String resolvedInclude = allVars.resolve(include);

            //Remote support: if the include is an absolute path on remote server, then replace it with corresponding path on local dir
            if (getUserDirectory().getRemoteUserPath() != null) {
                String remoteUserDir = getUserDirectory().getRemoteUserPath().toString();
                if (remoteUserDir != null && resolvedInclude.startsWith(remoteUserDir))
                    resolvedInclude = resolvedInclude.replace(remoteUserDir, getUserDirectory().getPath().toOSString());
            }

            return userDir.resolve(baseUri, resolvedInclude);
        }
    }

    /**
     * Try to find an existing IFile for a given URI.
     */
    public IFile getIFile(URI uri) {
        if (uri == null)
            return null;

        // check if URI is already a workspace URI
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = workspaceRoot.getFileForLocation(new Path(uri.getPath()));
        if (file != null)
            return file;

        // if not a workspace URI, try mapping to the workspace
        // TODO
        if (userDir.getProject() != null && userDir.getProject().exists()) {
            URI relative = URIUtil.canonicalRelativize(getServerURI(), uri);
            if (!relative.isAbsolute()) {
                file = userDir.getProject().getFile(relative.getPath());
                if (file != null)
                    return file;
            }
        }
        return null;
    }

    /**
     * Add the predefined and bootstrap variables to the provided container.
     */
    public void getVariables(ConfigVars vars) {
        getVariables(vars, false);
    }

    /**
     * Add the predefined and bootstrap variables to the provided container.
     * If resolvedOnly is true then only add variables that can be resolved,
     * for example, this server may be used by more than one runtime so the
     * runtime install variable can't be resolved.
     *
     * @param vars
     * @param resolvedOnly
     */
    public void getVariables(ConfigVars vars, boolean resolvedOnly) {
        synchronized (infoLock) {
            fillConfigVars();
            if (resolvedOnly) {
                resolvedVars.copyInto(vars);
            } else {
                allVars.copyInto(vars);
            }
        }
    }

    /**
     * Fill the configuration vars, if needed
     *
     * When we need the configuration variables to resolve an include
     * we only care about the predefined variables and the bootstrap variables
     *
     * Once we have created the list of variables, we need to rebuilt the
     * list only if there were changes to the boostrap.properties file
     *
     * NOTE: we have separated filling the configuration variables from
     * the operation of filling the configuration cache, because
     * we could be filling the configuration cache and in the process
     * resolving an include, and we only need to fill the list if needed
     * and once it's filled we know the data won't change unless there was a
     * change to bootsrap.properties
     */
    protected void fillConfigVars() {
        synchronized (infoLock) {
            updateBootstrap();
            updateServerEnv();
            if (allVars == null)
                createConfigVars();
        }
    }

    /**
     * Creates and returns a connected JMX connection
     *
     * @return the JMX connection
     * @throws Exception if there is any error when establish the connection
     */
    public JMXConnection createLocalJMXConnection() throws Exception {
        JMXConnection jmxConnection = new JMXConnection(getServerOutputPath().append("workarea"));
        jmxConnection.connect();
        return jmxConnection;
    }

    public void generatePluginConfig() throws Exception {
        List<String> allFeatures = getConfigRoot().getAllFeatures();
        if (!allFeatures.contains(Constants.FEATURE_LOCAL_JMX))
            throw new Exception("No local JMX support");

        JMXConnection jmxConnection = null;
        try {
            jmxConnection = createLocalJMXConnection();
            jmxConnection.generateDefaultPluginConfig();
        } finally {
            if (jmxConnection != null)
                jmxConnection.disconnect();
        }
    }

    public void addDropInLibPaths(List<IPath> paths) {
        paths.add(getServerPath().append(Constants.LIB_FOLDER));
        userDir.addDropInLibPaths(paths);
    }

    @Override
    public String toString() {
        return "WebSphereServerInfo [" + getServerName() + "/" + userDir.toString() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public String getGeneratorId() {
        return getGeneratorId(getServerName(), getWebSphereRuntime().getMetaDataDirName(getUserDirectory()), getWebSphereRuntime().getGeneratorId());
    }

    /** {@inheritDoc} */
    @Override
    public IPath getBasePath(IPath root) {
        return buildMetadataDirectoryPath();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsFeatureListGeneration() {
        return false;
    }

    @Override
    public boolean metadataExists() {
        synchronized (infoLock) {
            return SchemaMetadata.getInstance().metadataExists(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void generateMetadata(IJobChangeListener listener, boolean isRegenInfoCache) {
        synchronized (infoLock) {
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
    }

    private void generateMetadata(IJobChangeListener listener, boolean isRegenInfoCache, int metadataTypes) {
        synchronized (infoLock) {
            final WebSphereRuntime wsr = getWebSphereRuntime();
            final IMetadataGenerator metadataGen = this;
            IJobChangeListener restoreListener = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    // do not have to check status since getPayload will always
                    // succeed (it returns the fallback if there was a problem)
                    try {
                        SchemaMetadata schemaMetadata = SchemaMetadata.getInstance();
                        FeatureListCoreMetadata coreMetadata = FeatureListCoreMetadata.getInstance();
                        FeatureListExtMetadata[] extMetadata = FeatureListExtMetadata.getInstances(wsr);

                        String generatorId = getGeneratorId();
                        schemaMetadata.generationComplete(generatorId, schemaMetadata.getPayload(metadataGen));
                        coreMetadata.generationComplete(generatorId, coreMetadata.getPayload(metadataGen));
                        for (FeatureListExtMetadata exts : extMetadata) {
                            exts.generationComplete(generatorId, exts.getPayload(metadataGen));
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

            boolean cacheNeededUpdate = updateCache();

            // To clear the cache in the SchemaLocationProvider
            // Otherwise, if cancel the server creation wizard, URIs in the cache will still point
            // to the runtime's metadata directory.
            if (!cacheNeededUpdate) {
                ServerListenerUtil.getInstance().fireServerChangedEvent(this);
            }
        }
    }

    public IPath buildMetadataDirectoryPath() {
        IPath path = getWebSphereRuntime().getBasePath(Activator.getInstance().getStateLocation());
        return path.append(getMetadataRelativePath());
    }

    public String getMetadataRelativePath() {
        String userDir = runtime.getMetaDataDirName(getUserDirectory());
        return SERVERS_METADATA_DIR + "/" + userDir + "/" + getServerName();
    }

    public static String getGeneratorId(String serverName, String userDir, String runtimeId) {
        return runtimeId + "_" + userDir + "_" + serverName;
    }

    /** {@inheritDoc} */
    @Override
    public void generateSchema(String file, IProgressMonitor monitor, int timeout) throws CoreException {
        getWebSphereRuntime().generateSchema(file, monitor, timeout);
    }

    /** {@inheritDoc} */
    @Override
    public void generateFeatureList(String file, IProgressMonitor monitor, int timeout, String... options) throws CoreException {
        // Do nothing, feature list generation not supported.
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadyToGenerateMetadata() {
        return getWebSphereRuntime().isReadyToGenerateMetadata();
    }

    /** {@inheritDoc} */
    @Override
    public void removeMetadata(IPath dir, boolean deleteDirectory, boolean destroy) {
        IPath metadataDirectory = dir != null ? dir : buildMetadataDirectoryPath();
        removeFile(metadataDirectory, SchemaMetadata.SCHEMA_XSD);
        Metadata.removeMetadata(getGeneratorId(), destroy);
        if (deleteDirectory) {
            File file = metadataDirectory.toFile();
            if (file.exists() && !file.delete()) {
                Trace.logError("Unable to delete metadata directory " + metadataDirectory.toString(), null);
            }
        }
        // To clear the cache in the SchemaLocationProvider
        // Otherwise, if cancel the server creation wizard, URIs in the cache will still point
        // to the metadata directory deleted above.
        ServerListenerUtil.getInstance().fireServerChangedEvent(this);
    }

    public void removeFile(IPath dir, String fileName) {
        synchronized (infoLock) {
            IPath path = dir.append(fileName);
            File file = path.toFile();
            FileUtil.deleteFile(file);
        }
    }

    /**
     * If the server-specific metadata exists, it is returned; otherwise the runtime schema IRL is returned.
     * Calling this method does not cause the server-specific metadata to be generated.
     */
    public URL getConfigurationSchemaURL() {

        // Only return the server specific metadata if it exists
        if (metadataExists()) {
            return SchemaMetadata.getInstance().getSchemaPath(this);
        }

        // Otherwise return the _runtime_ URL
        return runtime.getConfigurationSchemaURL();
    }

    /**
     * Returns the schema to use for the configuration file at the given path. Returns
     * <code>null</code> if the given file URI isn't associated with or used by this server.
     *
     * @param fileURI
     * @return
     */
    public URL getConfigurationSchemaURL(URI fileURI) {
        synchronized (infoLock) {
            if (getConfigurationFileFromURI(fileURI) != null) {
                return getConfigurationSchemaURL();
            }

            return null;
        }
    }

    /**
     * @return the schemaHelper
     */
    public SchemaHelper getSchemaHelper() {
        if (schemaHelper == null) {
            schemaHelper = new SchemaHelper(getConfigurationSchemaURL());
        }
        return schemaHelper;
    }

    // Go through the server metadata folders and remove any that do not
    // have a matching WebSphereServerInfo
    public static void removeOutOfSyncMetadata(final WebSphereRuntime runtime) {
        IPath path = runtime.getBasePath(Activator.getInstance().getStateLocation());
        path = path.append(SERVERS_METADATA_DIR);
        File metadataDir = path.toFile();
        if (metadataDir.exists()) {
            final File[] userDirs = metadataDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });

            if (userDirs != null && userDirs.length > 0) {
                String runtimeId = runtime.getGeneratorId();
                for (File userDir : userDirs) {
                    final UserDirectory userDirectory = runtime.getUserDirForMetadataDir(userDir.getName());
                    if (userDirectory != null) {
                        final File[] serverDirs = userDir.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return runtime.getServerInfo(name, userDirectory) == null;
                            }
                        });
                        if (serverDirs != null && serverDirs.length > 0) {
                            for (File serverDir : serverDirs) {
                                String id = getGeneratorId(serverDir.getName(), userDir.getName(), runtimeId);
                                Metadata.removeMetadata(id, true);
                                try {
                                    FileUtil.deleteDirectory(userDir.getAbsolutePath(), true);
                                } catch (IOException e) {
                                    if (Trace.ENABLED)
                                        Trace.trace(Trace.WARNING, "Failed to delete directory: " + userDir.getAbsolutePath(), e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Remove any metadata that may have been generated for a canceled
    // server creation
    public static void removeCancelledMetaData(IPath basePath, String runtimeId) {
        IPath path = basePath.append(SERVERS_METADATA_DIR);
        File metadataDir = path.toFile();
        if (metadataDir.exists()) {
            File[] userDirs = metadataDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File dir) {
                    return dir.isDirectory();
                }
            });
            if (userDirs != null && userDirs.length > 0) {
                for (File userDir : userDirs) {
                    File[] serverDirs = userDir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File dir) {
                            return dir.isDirectory();
                        }
                    });
                    if (serverDirs != null && serverDirs.length > 0) {
                        for (File serverDir : serverDirs) {
                            String id = getGeneratorId(serverDir.getName(), userDir.getName(), runtimeId);
                            Metadata.removeMetadata(id, true);
                            try {
                                FileUtil.deleteDirectory(serverDir.getAbsolutePath(), true);
                            } catch (IOException e) {
                                if (Trace.ENABLED)
                                    Trace.trace(Trace.WARNING, "Failed to delete directory: " + serverDir.getAbsolutePath(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    public IPath getConfigDefaultDropinsPath() {
        return getConfigDropinsPath().append(Constants.CONFIG_DEFAULT_DROPINS_FOLDER);
    }

    public IPath getConfigOverrideDropinsPath() {
        return getConfigDropinsPath().append(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);
    }

    public IPath getConfigDropinsPath() {
        return getServerPath().append(Constants.CONFIG_DROPINS_FOLDER);
    }

    public IFolder getConfigDefaultDropinsFolder() {
        return getConfigDropinsFolder(Constants.CONFIG_DEFAULT_DROPINS_FOLDER);
    }

    public IFolder getConfigOverrideDropinsFolder() {
        return getConfigDropinsFolder(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);
    }

    private IFolder getConfigDropinsFolder(String folderName) {
        IFolder folder = getConfigDropinsFolder();
        if (folder != null) {
            return folder.getFolder(folderName);
        }
        return null;
    }

    public IFolder getConfigDropinsFolder() {
        IFolder folder = getServerFolder();
        if (folder != null) {
            return folder.getFolder(Constants.CONFIG_DROPINS_FOLDER);
        }
        return null;
    }

    public ConfigurationDropinsFolder getConfigurationDropinsFolder() {
        IPath path = getConfigDropinsPath();
        if (path == null || !path.toFile().exists())
            return null;
        return new ConfigurationDropinsFolder(getUserDirectory(), path, getConfigDropinsFolder());
    }

    /**
     * retrieves messages.log file name specified in server.xml or bootstrap.properties
     * Default value returned is messages.log
     *
     * @return
     */

    public String getMessageFileName() {
        ConfigurationFile configFile = getConfigRoot();
        if (configFile == null)
            return null;
        String messageFileName = configFile.getResolvedAttributeValue(Constants.LOGGING, Constants.MESSAGES_FILE);
        if (messageFileName != null) {
            return messageFileName;
        }
        Bootstrap bootstrap = getBootstrap();
        if (bootstrap != null) {
            messageFileName = bootstrap.getMessagesFile();
            if (messageFileName != null) {
                return messageFileName;
            }
        }
        return Constants.MESSAGES_LOG;
    }

    /**
     * retrieves trace filename mentioned in server.xml or bootstrap.properties
     * default value returned is trace.log
     *
     * @return
     */
    public String getTraceFileName() {
        ConfigurationFile configFile = getConfigRoot();
        if (configFile == null)
            return null;
        String s = configFile.getResolvedAttributeValue(Constants.LOGGING, Constants.TRACE_FILE);
        if (s != null) {
            return s;
        }
        Bootstrap bootstrap = getBootstrap();
        if (bootstrap != null) {
            s = bootstrap.getTraceFile();
            if (s != null) {
                return s;
            }
        }
        return Constants.TRACE_LOG;
    }

    public IPath getMessagesFile() {
        IPath relativePath = getRelativeLogPath();
        if (relativePath == null)
            return null;
        return relativePath.append(getMessageFileName());
    }

    // get Log directory from config files first.
    // if log directory is not specified use the default directory in case of local server
    public IPath getRelativeLogPath() {
        IPath relativePath = null;
        IPath logDir = getLogDirectory();
        if (logDir != null)
            relativePath = logDir;
        else {
            relativePath = getServerOutputPath();
            if (relativePath == null)
                return null;
            relativePath = relativePath.append("logs");
        }
        return relativePath;
    }

    public IPath getLogStateDirectory() {
        return getRelativeLogPath().append("state");
    }

    public IPath getTraceLogFile() {
        IPath relativePath = getRelativeLogPath();
        if (relativePath == null)
            return null;
        return relativePath.append(getTraceFileName());
    }

    /**
     * Log directory can be specified in following ways
     * LogDirectory attribute in logging element on server.xml
     * Using com.ibm.ws.logging.log.directory in bootstrap.properties
     * Using environment variable LOG_DIR in server.env or at System level
     * If a log directory is specified in both the bootstrap.properties file and the server.xml file
     * Value specified in server.xml overrides boostrap.properties and environment variables.
     *
     * @return
     */
    public IPath getLogDirectory() {
        ConfigurationFile configFile = getConfigRoot();
        if (configFile == null)
            return null;
        String messageloc = configFile.getResolvedAttributeValue(Constants.LOGGING, Constants.LOG_DIR);
        if (messageloc != null) {
            ConfigVars cv = new ConfigVars();
            getVariables(cv);
            messageloc = cv.resolve(messageloc);
            return new Path(messageloc);
        }
        Bootstrap bootstrap = getBootstrap();
        if (bootstrap != null) {
            messageloc = bootstrap.getLogDir();
            if (messageloc != null)
                return new Path(messageloc);
        }
        String logDir = null;
        ConfigVars v = new ConfigVars();
        addServerEnvVars(v);
        //When both LOG_DIR and WLP_OUTPUT_DIR are specified . Logs will be created under LOG_DIR.
        logDir = v.getValue(Constants.ENV_VAR_PREFIX + Constants.ENV_LOG_DIR);
        if (logDir != null && !logDir.isEmpty()) {
            return new Path(logDir);
        }
        logDir = v.getValue(Constants.ENV_VAR_PREFIX + Constants.WLP_OUTPUT_DIR);
        if (logDir != null && !logDir.isEmpty()) {
            return new Path(logDir).append(getServerName()).append("logs");
        }
        logDir = System.getenv(Constants.ENV_LOG_DIR);
        if (logDir != null && !logDir.isEmpty()) {
            return new Path(logDir);
        }
        logDir = System.getenv(Constants.WLP_OUTPUT_DIR);
        if (logDir != null && !logDir.isEmpty()) {
            return new Path(logDir).append(getServerName()).append("logs");
        }
        return null;
    }

    /*
     * The server path needs to be writable (for editing the server.xml and creating the
     * apps directory). The apps path needs to be writable for deploying the application.
     * The output path needs to be writable since the tools start the server and depend
     * on the logs for the server state.
     */
    public String[] getServerErrors() {
        List<String> errors = new ArrayList<String>();

        IPath serverPath = getServerPath();
        if (serverPath != null) {
            java.nio.file.Path path = Paths.get(serverPath.toOSString());
            if (!Files.isWritable(path)) {
                errors.add(NLS.bind(Messages.errorServerFolderNotAccessible, serverPath.toOSString()));
            } else {
                IPath appsPath = getServerAppsPath();
                // The apps directory may not exist yet if no applications have been deployed.
                // If the apps directory cannot be created it will be caught by checking if the server
                // path is accessible.
                if (appsPath != null && appsPath.toFile().exists()) {
                    path = Paths.get(appsPath.toOSString());
                    if (!Files.isWritable(path)) {
                        errors.add(NLS.bind(Messages.errorAppsFolderNotAccessible, appsPath.toOSString()));
                    }
                }
            }
        }

        IPath outputPath = getServerOutputPath();
        if (outputPath != null) {
            java.nio.file.Path path = Paths.get(outputPath.toOSString());
            if (!outputPath.equals(serverPath) && !Files.isWritable(path)) {
                errors.add(NLS.bind(Messages.errorOutputFolderNotAccessible, outputPath.toOSString()));
            }
        }

        return errors.toArray(new String[errors.size()]);
    }

}