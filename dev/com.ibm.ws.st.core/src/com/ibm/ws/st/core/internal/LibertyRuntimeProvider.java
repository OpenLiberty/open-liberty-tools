/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.net.URI;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

/**
 *
 */
public abstract class LibertyRuntimeProvider {

    /**
     * In situations where only the resource URI of the server.xml is available, along with a wsInfo, the 'target' or actual
     * server.xml is needed.
     *
     * @param uri - URI of any server.xml
     * @param server - the WSInfo that might reference the main target server.xml
     * @return
     */
    public abstract URI getTargetConfigFileLocation(URI uri, WebSphereServerInfo server);

    /**
     * For a URI to any server.xml, provide the WSInfo object even though the actual server is not created. This WSInfo can be used for
     * obtaining the User Directory for the purposes of validation.
     *
     * @param uri
     * @return
     */
    public abstract WebSphereServerInfo getWebSphereServerInfo(URI uri);

    /**
     * A runtime provider can provide a folder that holds all the include files included by the main server.xml server config file.
     * If specified, the files in this folder will be used to validate against the server config, and will override the existing
     * include-resolution mechanism.
     *
     * @param resource
     * @return
     */
    public abstract IFolder getConfigFolder(IResource resource);
}
