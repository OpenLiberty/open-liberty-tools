/*
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.core.internal;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 *
 */
public abstract class GhostRuntimeProvider {

    /**
     * Providers should return a non-null User Directory if the provided file in the project
     * is of interest to them, otherwise, return null.
     *
     * @param uri
     * @param project
     * @return UserDirectory
     */
    public abstract UserDirectory getUserDirectory(URI uri, IProject project);

    /**
     * Providers should return a non-null WebSphereRuntime if the project is
     * of interest to them and has a valid runtime, otherwise, return null.
     *
     * @param uri
     * @param project
     * @return WebSphereRuntime - the ghost runtime
     */
    public abstract WebSphereRuntime getWebSphereRuntime(IResource resource, IProject project);

}
