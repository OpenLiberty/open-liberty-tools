/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;

public class ConfigurationFolder implements IConfigurationElement {
    private final IPath path;
    private final IFolder folder;
    private final UserDirectory userDir;
    private List<IConfigurationElement> children;

    public ConfigurationFolder(UserDirectory userDir, IPath path, IFolder folder) {
        this.userDir = userDir;
        this.path = path;
        this.folder = folder;
    }

    @Override
    public String getName() {
        return path.lastSegment();
    }

    @Override
    public IPath getPath() {
        return path;
    }

    public IFolder getFolder() {
        return folder;
    }

    public UserDirectory getUserDirectory() {
        return userDir;
    }

    public synchronized IConfigurationElement[] getChildren() {
        refresh(false);
        return children.toArray(new IConfigurationElement[children.size()]);
    }

    public synchronized boolean refresh(boolean force) {
        if (children == null)
            children = new ArrayList<IConfigurationElement>();

        boolean changed = false;
        List<IConfigurationElement> found = new ArrayList<IConfigurationElement>();

        // find all folders within this folder
        File[] files = path.toFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        // files can be null when a folder is removed
        if (files != null) {
            for (File f : files) {
                ConfigurationFolder existing = null;
                String name = f.getName();
                for (IConfigurationElement element : children) {
                    if (element instanceof ConfigurationFolder && element.getName().equals(name)) {
                        existing = (ConfigurationFolder) element;
                    }
                }

                if (existing != null && force) {
                    if (existing.refresh(true))
                        changed = true;
                }
                if (existing == null) {
                    IFolder childFolder = null;
                    if (folder != null)
                        childFolder = folder.getFolder(name);
                    existing = new ConfigurationFolder(userDir, path.append(name), childFolder);
                    children.add(existing);
                    changed = true;
                }
                found.add(existing);
            }
        }

        // files can be null when a folder is removed
        if (files != null) {
            // find all config files
            files = path.toFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return (file.isFile() && file.getName().endsWith("xml"));
                }
            });

            for (File f : files) {
                ConfigurationFile existing = null;
                for (IConfigurationElement element : children) {
                    if (element instanceof ConfigurationFile && element.getName().equals(f.getName())) {
                        existing = (ConfigurationFile) element;
                    }
                }

                if (existing != null && existing.hasChanged())
                    existing = null;

                if (existing == null) {
                    try {
                        existing = new ConfigurationFile(f.toURI(), userDir);
                        children.add(existing);
                    } catch (IOException e) {
                        Trace.logError("Error reading configuration file:" + f.toURI(), e);
                    }
                    changed = true;
                }
                if (existing != null)
                    found.add(existing);
            }

        }

        // remove old/removed files and folders
        List<IConfigurationElement> delete = new ArrayList<IConfigurationElement>(2);
        for (IConfigurationElement element : children) {
            if (!found.contains(element)) {
                delete.add(element);
                changed = true;
            }
        }
        for (IConfigurationElement element : delete)
            children.remove(element);

        return changed;
    }

    @Override
    public String toString() {
        return "Configuration Folder [" + getPath() + "]";
    }
}
