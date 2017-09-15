/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.IConfigurationElement;

public class UserDirectory {
    private final WebSphereRuntime runtime;

    // WLP_USER_DIR
    private final IPath userPath;

    //WLP_USER_DIR on remote machine
    protected IPath remoteUserPath;

    // WLP_USER_DIR in workspace (may be null if user dir is external to workspace)
    private final IProject userProject;

    // WLP_OUTPUT_DIR
    private final IPath outputPath;

    private ConfigurationFolder sharedConfigFolder;

    private final ConfigVars configVars;

    public UserDirectory(WebSphereRuntime runtime, IPath userPath, IProject userProject, IPath outputPath) {
        this(runtime, userPath, userProject, outputPath, null);
    }

    public UserDirectory(WebSphereRuntime runtime, IPath userPath, IProject userProject) {
        this(runtime, userPath, userProject, null, null);
    }

    public UserDirectory(WebSphereRuntime runtime, IPath userPath, IProject userProject, IPath outputPath, IPath remoteUserPath) {
        if (runtime == null)
            throw new IllegalArgumentException("Runtime cannot be null");
        if (userPath == null)
            throw new IllegalArgumentException("User path (WLP_USER_DIR) cannot be null");
        this.runtime = runtime;
        this.userPath = userPath;
        this.userProject = userProject;
        this.remoteUserPath = remoteUserPath;
        if (outputPath == null)
            this.outputPath = userPath.append(Constants.SERVERS_FOLDER);
        else
            this.outputPath = outputPath;
        configVars = new ConfigVars();
        getVariables(configVars);
    }

    public WebSphereRuntime getWebSphereRuntime() {
        return runtime;
    }

    public IPath getPath() {
        return userPath;
    }

    public void setRemoteUserPath(IPath remoteUserPath) {
        this.remoteUserPath = remoteUserPath;
    }

    public IPath getRemoteUserPath() {
        return remoteUserPath;
    }

    public IProject getProject() {
        return userProject;
    }

    public IPath getOutputPath() {
        return outputPath;
    }

    public IPath getServersPath() {
        return userPath.append(Constants.SERVERS_FOLDER);
    }

    public IPath getSharedPath() {
        return userPath.append(Constants.SHARED_FOLDER);
    }

    public IFolder getServersFolder() {
        if (userProject == null)
            return null;
        return userProject.getFolder(Constants.SERVERS_FOLDER);
    }

    public IFolder getSharedFolder() {
        if (userProject == null)
            return null;
        return userProject.getFolder(Constants.SHARED_FOLDER);
    }

    public IFolder getSharedConfigFolder() {
        if (userProject == null)
            return null;
        return userProject.getFolder(Constants.SHARED_FOLDER).getFolder(Constants.CONFIG_FOLDER);
    }

    public IFolder getSharedAppsFolder() {
        if (userProject == null)
            return null;
        return userProject.getFolder(Constants.SHARED_FOLDER).getFolder(Constants.APPS_FOLDER);
    }

    public IPath getSharedConfigPath() {
        return userPath.append(Constants.SHARED_FOLDER).append(Constants.CONFIG_FOLDER);
    }

    public IPath getSharedAppsPath() {
        return userPath.append(Constants.SHARED_FOLDER).append(Constants.APPS_FOLDER);
    }

    public IPath getSharedResourcesPath() {
        return userPath.append(Constants.SHARED_FOLDER).append(Constants.RESOURCES_FOLDER);
    }

    public URI getSharedConfigURI() {
        return getSharedConfigPath().toFile().toURI();
    }

    /**
     * Returns a list of all shared configuration elements, which may be configuration files or folders.
     *
     * @return an array of configuration elements
     */
    public synchronized IConfigurationElement[] getSharedConfiguration() {
        if (sharedConfigFolder == null) {
            IPath path = getSharedConfigPath();
            if (path == null || !path.toFile().exists())
                return new ConfigurationFile[0];

            sharedConfigFolder = new ConfigurationFolder(this, path, getSharedConfigFolder());
        }
        return sharedConfigFolder.getChildren();
    }

    /**
     * Refresh cached shared configuration folders & files.
     *
     * @return <code>true</code> if something has been updated, and <code>false</code> if nothing changed or
     *         no information had been cached.
     */
    protected synchronized boolean refreshSharedConfig() {
        if (sharedConfigFolder == null)
            return false;

        if (!sharedConfigFolder.getPath().equals(getSharedConfigPath())) {
            // runtime has moved on disk, throw everything out
            sharedConfigFolder = null;
            getWebSphereRuntime().fireRefreshEvent();
            return true;
        }
        boolean changed = sharedConfigFolder.refresh(true);
        if (changed)
            getWebSphereRuntime().fireRefreshEvent();
        return changed;
    }

    /**
     * Copy the file at the given URL into the shared configuration folder.
     *
     * @param folder the configuration folder to add the shared file to, or
     *            <code>null</code> to add it to the root.
     * @param configURL the config file to copy
     * @return a status indicating success or failure
     */
    public IStatus addSharedConfigFile(ConfigurationFolder folder, URL configURL) {
        String name = configURL.getFile();
        int last = name.lastIndexOf('/');
        if (last >= 0)
            name = name.substring(last + 1);
        IPath to = null;
        if (folder != null)
            to = folder.getPath().append(name);
        else
            to = getSharedConfigPath().append(name);

        try {
            IStatus status = FileUtil.copy(configURL, to);
            if (status != null && !status.isOK())
                return status;
        } catch (IOException e) {
            Trace.logError("Error adding shared config file: " + configURL, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorAddingSharedConfig, e);
        }

        // refresh workspace
        if (userProject != null) {
            try {
                getSharedConfigFolder().refreshLocal(IResource.DEPTH_INFINITE, null);
            } catch (CoreException ce) {
                Trace.logError("Couldn't refresh shared config folder: " + getSharedConfigFolder(), ce);
            }
        } else
            refreshSharedConfig();

        return Status.OK_STATUS;
    }

    /**
     * Returns the URI of an included file, based on the URI of the parent document and include text.
     *
     * @param uri
     * @param include
     * @return the URI of the included document
     */
    public URI resolve(URI baseUri, String include) {
        if (include == null)
            return null;

        // Resolve any variables
        String resolvedInclude = configVars.resolve(include);

        // Check if this is an absolute path name first (otherwise
        // the URI class will think that the device is the scheme).
        File f = new File(resolvedInclude);
        if (f.isAbsolute()) {

            //Remote support: if the include is an absolute path on remote server, then replace it with corresponding path on local dir
            if (getRemoteUserPath() != null && f.toString().startsWith(remoteUserPath.toOSString()))
                f = new File(f.toString().replace(remoteUserPath.toOSString(), userPath.toString()));

            return f.toURI();
        }
        try {
            // If include is already an absolute URI then resolve
            // will just return it.
            URI result = baseUri.resolve(resolvedInclude);
            return result;
        } catch (IllegalArgumentException e) {
            // ignore
        }

        // Try relative path
        if (f.exists()) {
            return f.toURI();
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.WARNING, "Could not resolve: " + include);
        return null;
    }

    /**
     * Add the predefined variables to the provided container.
     */
    public void getVariables(ConfigVars vars) {
        getVariables(vars, false);
    }

    /**
     * Add the predefined variables to the provided container.
     * If resolvedOnly is true then only add variables that can be resolved,
     * for example, this user directory may be used by more than one runtime so the
     * runtime install variable can't be resolved.
     */
    public void getVariables(ConfigVars vars, boolean resolvedOnly) {
        vars.add(ConfigVars.WLP_USER_DIR, getPath().toOSString(), ConfigVars.LOCATION_TYPE);
        vars.add(ConfigVars.SHARED_APP_DIR, getSharedAppsPath().toOSString(), ConfigVars.LOCATION_TYPE);
        vars.add(ConfigVars.SHARED_CONFIG_DIR, getSharedConfigPath().toOSString(), ConfigVars.LOCATION_TYPE);
        vars.add(ConfigVars.SHARED_RESOURCE_DIR, getSharedResourcesPath().toOSString(), ConfigVars.LOCATION_TYPE);
        vars.add(ConfigVars.USR_EXTENSION_DIR, getPath().append(Constants.EXTENSION_FOLDER).toOSString(), ConfigVars.LOCATION_TYPE);
        if (!resolvedOnly) {
            runtime.getVariables(vars);
        }
    }

    public void addDropInLibPaths(List<IPath> paths) {
        paths.add(getSharedConfigPath().append(Constants.LIB_FOLDER));
    }

    public String getUniqueId() {
        IProject project = getProject();
        if (project != null) {
            return project.getName();
        }
        return getPath().toPortableString();
    }

    public boolean matchesId(String id) {
        IProject project = getProject();
        if (project != null) {
            return project.getName().equals(id);
        }
        return getPath().toPortableString().equals(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserDirectory))
            return false;

        UserDirectory rc = (UserDirectory) obj;
        if (!userPath.equals(rc.userPath))
            return false;

        if ((userProject == null && rc.userProject != null) ||
            (userProject != null && !userProject.equals(rc.userProject)))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += userPath.hashCode();
        if (userProject != null)
            hash += userProject.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("User directory [");
        sb.append(userPath.toPortableString());
        if (userProject != null)
            sb.append("|" + userProject);

        return sb.append("]").toString();
    }
}