/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.UserDirectory;

/**
 *
 */
public class ConfigurationDropinsFolder extends ConfigurationFolder {

    private ConfigurationFolder defaultsFolder = null;
    private ConfigurationFolder overridesFolder = null;

    public ConfigurationDropinsFolder(UserDirectory userDir, IPath path, IFolder folder) {
        super(userDir, path, folder);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return Messages.serverConfigurationDropins;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized IConfigurationElement[] getChildren() {
        refresh(false);
        List<ConfigurationFolder> folders = new ArrayList<ConfigurationFolder>();
        if (defaultsFolder != null)
            folders.add(defaultsFolder);
        if (overridesFolder != null)
            folders.add(overridesFolder);
        return folders.toArray(new IConfigurationElement[folders.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean refresh(boolean force) {
        boolean changed = false;
        IPath path = getPath().append(Constants.CONFIG_DEFAULT_DROPINS_FOLDER);
        File dir = path.toFile();
        if (dir.exists() && defaultsFolder == null) {
            changed = true;
            IFolder folder = getFolder();
            if (folder != null) {
                folder = folder.getFolder(Constants.CONFIG_DEFAULT_DROPINS_FOLDER);
            }
            defaultsFolder = new ConfigurationFolder(getUserDirectory(), path, folder);
        } else if (!dir.exists() && defaultsFolder != null) {
            changed = true;
            defaultsFolder = null;
        } else if (defaultsFolder != null && force) {
            if (defaultsFolder.refresh(true))
                changed = true;
        }

        path = getPath().append(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);
        dir = path.toFile();
        if (dir.exists() && overridesFolder == null) {
            changed = true;
            IFolder folder = getFolder();
            if (folder != null) {
                folder = folder.getFolder(Constants.CONFIG_OVERRIDE_DROPINS_FOLDER);
            }
            overridesFolder = new ConfigurationFolder(getUserDirectory(), path, folder);
        } else if (!dir.exists() && overridesFolder != null) {
            changed = true;
            overridesFolder = null;
        } else if (overridesFolder != null && force) {
            if (overridesFolder.refresh(true))
                changed = true;
        }

        return changed;
    }

}
