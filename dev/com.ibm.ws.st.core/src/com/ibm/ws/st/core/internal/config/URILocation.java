/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Represents a location of a config file, a resource or an external file or directory.
 * Used mainly for associating a reference in the configuration file to a location.
 * For example, a shared library reference could refer to one of the following:
 * <ul>
 * <li>A shared library declaration in the same configuration file or in an include</li>
 * <li>An automatic shared library directory in the workspace</li>
 * <li>An automatic shared library directory on the file system</li>
 * </ul>
 */
public class URILocation {

    private final URI uri;

    public URILocation(URI uri) {
        this.uri = uri;
    }

    public String getLocationString() {
        IResource resource = getResource();
        if (resource != null) {
            return resource.getFullPath().toString();
        }
        File file = new File(uri);
        return file.getAbsolutePath();
    }

    public URI getURI() {
        return uri;
    }

    public IResource getResource() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = root.findFilesForLocationURI(uri);
        if (files.length > 0 && files[0].exists()) {
            return files[0];
        }
        IContainer[] containers = root.findContainersForLocationURI(uri);
        if (containers.length > 0 && containers[0].exists()) {
            return containers[0];
        }
        return null;
    }
}
