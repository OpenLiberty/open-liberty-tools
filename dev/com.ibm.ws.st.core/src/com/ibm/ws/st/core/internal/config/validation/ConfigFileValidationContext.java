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

import com.ibm.ws.st.core.internal.URIUtil;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.IncludeConflictResolution;

/**
 * Validation context given a ConfigurationFile. Could use DOMValidationContext
 * unless we want to be able to synchronize on the ConfigurationFile.
 */
public class ConfigFileValidationContext extends ValidationContext {

    private final ConfigurationFile configFile;

    public ConfigFileValidationContext(ConfigurationFile configFile, ValidationContext parent, IncludeConflictResolution conflictResolution) {
        super(parent, conflictResolution);
        this.configFile = configFile;
    }

    /** {@inheritDoc} */
    @Override
    public Document getDocument() {
        return configFile.getDomDocument();
    }

    /** {@inheritDoc} */
    @Override
    public IResource getResource() {
        return configFile.getIFile();
    }

    /** {@inheritDoc} */
    @Override
    public URI getURI() {
        return URIUtil.getCanonicalURI(configFile.getURI());
    }

    @Override
    public WebSphereServerInfo getServer() {
        return configFile.getWebSphereServer();
    }

    /** {@inheritDoc} */
    @Override
    public UserDirectory getUserDirectory() {
        return configFile.getUserDirectory();
    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationFile getConfigFile() {
        return configFile;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Configuration File Validation Context: " + getURI();
    }

}
