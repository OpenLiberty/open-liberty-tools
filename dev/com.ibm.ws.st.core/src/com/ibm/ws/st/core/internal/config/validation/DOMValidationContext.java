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
package com.ibm.ws.st.core.internal.config.validation;

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.IncludeConflictResolution;

/**
 * Validation context given an existing DOM.
 */
public class DOMValidationContext extends ValidationContext {

    private final Document document;
    private final IResource resource;
    private final WebSphereServerInfo server;
    private final UserDirectory userDirectory;

    public DOMValidationContext(Document document, IResource resource, WebSphereServerInfo server, UserDirectory userDirectory, ValidationContext parent,
                                IncludeConflictResolution conflictResolution) {
        super(parent, conflictResolution);
        this.document = document;
        this.resource = resource;
        this.server = server;
        this.userDirectory = userDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public Document getDocument() {
        return document;
    }

    /** {@inheritDoc} */
    @Override
    public IResource getResource() {
        return resource;
    }

    /** {@inheritDoc} */
    @Override
    public URI getURI() {
        if (resource == null) {
            return null;
        }
        return resource.getLocation().toFile().toURI();
    }

    @Override
    public WebSphereServerInfo getServer() {
        return server;
    }

    /** {@inheritDoc} */
    @Override
    public UserDirectory getUserDirectory() {
        return userDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationFile getConfigFile() {
        if (resource == null) {
            return null;
        }
        return ConfigUtils.getConfigFile(resource);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DOM Validation Context: " + getURI();
    }

}
