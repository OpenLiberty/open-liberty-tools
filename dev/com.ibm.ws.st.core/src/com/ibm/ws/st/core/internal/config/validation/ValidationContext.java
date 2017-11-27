/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config.validation;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.IncludeConflictResolution;

/**
 * Generic context that allows different configuration file sources
 * to be used when validating. Also keeps track of some information while
 * validating (such as the current include).
 *
 * Currently the document is only valid after creation of the context
 * and before dispose() is called. Other parts of the context continue
 * to be valid after dispose() is called.
 */
public abstract class ValidationContext {

    private final ValidationContext parent;
    private Element currentInclude;
    private final IncludeConflictResolution conflictResolution;
    private boolean isDropin = false;

    public static ValidationContext createValidationContext(ConfigurationFile configFile, ValidationContext parent, IncludeConflictResolution conflictResolution) {
        return new ConfigFileValidationContext(configFile, parent, conflictResolution);
    }

    public static ValidationContext createValidationContext(IResource resource, ValidationContext parent, IncludeConflictResolution conflictResolution) {
        if (resource instanceof IFile) {
            try {
                URI uri = resource.getLocation().toFile().toURI();
                WebSphereServerInfo server = ConfigUtils.getServer(uri);
                UserDirectory userDir = server != null ? server.getUserDirectory() : ConfigUtils.getUserDirectory(uri, resource);
                return new DOMModelValidationContext((IFile) resource, server, userDir, parent, conflictResolution);
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Exception trying to create DOMModelValidationContext for " + resource + ".", e);
                }
            }
        }

        ConfigurationFile configFile = ConfigUtils.getConfigFile(resource);
        if (configFile != null)
            return createValidationContext(configFile, parent, conflictResolution);

        if (Trace.ENABLED) {
            Trace.trace(Trace.WARNING, "Failed to create model for " + resource + ".");
        }
        return null;
    }

    public static ValidationContext createValidationContext(Document document, IResource resource, ValidationContext parent, IncludeConflictResolution conflictResolution) {
        WebSphereServerInfo server = null;
        UserDirectory userDir = null;
        URI uri = null;
        if (resource != null) {
            uri = resource.getLocation().toFile().toURI();
        }
        if (uri == null) {
            try {
                String uriStr = document.getDocumentURI();
                uri = new URI(uriStr);
            } catch (Exception e) {
                // Editor doesn't give us a resource and the DOM it gives
                // us doesn't support getDocumentURI().  Should give us
                // the original IEditorInput.
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Exception trying to get the URI for a config file DOM.", e);
                }
            }
        }
        if (uri != null) {
            server = ConfigUtils.getServer(uri);
            userDir = server != null ? server.getUserDirectory() : ConfigUtils.getUserDirectory(uri, resource);
        }

        return new DOMValidationContext(document, resource, server, userDir, parent, conflictResolution);
    }

    public static ValidationContext createValidationContext(String path, URI base, UserDirectory context, ValidationContext parent,
                                                            IncludeConflictResolution conflictResolution) throws Exception {
        final URI uri = ConfigUtils.resolve(base, path, context);
        if (uri != null) {
            final WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
            for (WebSphereServerInfo server : servers) {
                ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
                if (configFile != null)
                    return createValidationContext(configFile, parent, conflictResolution);
            }

            final IFile iFile = ConfigUtils.getWorkspaceFile(context, uri);
            if (iFile != null && iFile.exists())
                return new DOMModelValidationContext(iFile, null, context, parent, conflictResolution);

            // Check to see if the actual file exists in the file system
            // (outside of the workspace)
            final File file = new File(uri);
            if (file.exists()) {
                final ConfigurationFile configFile = new ConfigurationFile(uri, context);
                return new ConfigFileValidationContext(configFile, parent, conflictResolution);
            }
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.WARNING, "Failed to create model for " + uri + ".");
        }
        return null;
    }

    public ValidationContext(ValidationContext parent, IncludeConflictResolution conflictResolution) {
        this.parent = parent;
        this.conflictResolution = conflictResolution;
    }

    public ValidationContext getParent() {
        return parent;
    }

    public IncludeConflictResolution getConflictResolution() {
        return conflictResolution;
    }

    public abstract Document getDocument();

    public abstract IResource getResource();

    public abstract URI getURI();

    public abstract WebSphereServerInfo getServer();

    public abstract UserDirectory getUserDirectory();

    public abstract ConfigurationFile getConfigFile();

    public void dispose() {
        // do nothing by default;
    }

    public Element getCurrentInclude() {
        return currentInclude;
    }

    public void setCurrentInclude(Element include) {
        currentInclude = include;
    }

    public boolean isDropin() {
        return isDropin;
    }

    public void setDropin(boolean isDropin) {
        this.isDropin = isDropin;
    }
}
