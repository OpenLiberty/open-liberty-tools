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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.IncludeConflictResolution;

/**
 * ValidationContext given a resource. Gets model for the resource from
 * the model manager.
 */
@SuppressWarnings("restriction")
public class DOMModelValidationContext extends ValidationContext {

    private final IDOMModel domModel;
    private final IFile resource;
    private final WebSphereServerInfo server;
    private final UserDirectory userDirectory;

    public DOMModelValidationContext(IFile resource, WebSphereServerInfo server, UserDirectory userDirectory, ValidationContext parent, IncludeConflictResolution conflictResolution) throws Exception {
        super(parent, conflictResolution);
        this.resource = resource;
        this.server = server;
        this.userDirectory = userDirectory;
        this.domModel = getDOMModel(resource);
        if (this.domModel == null) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to create DOMModel."));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Document getDocument() {
        return domModel.getDocument();
    }

    /** {@inheritDoc} */
    @Override
    public IResource getResource() {
        return resource;
    }

    /** {@inheritDoc} */
    @Override
    public URI getURI() {
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
        return ConfigUtils.getConfigFile(resource);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        domModel.releaseFromRead();
    }

    private IDOMModel getDOMModel(IFile file) throws Exception {
        IModelManager manager = StructuredModelManager.getModelManager();
        IStructuredModel model = manager.getExistingModelForRead(file);

        if (model == null)
            model = manager.getModelForRead(file);

        if ((model == null || !(model instanceof IDOMModel))) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Validation failed. Unable to create DOM Model");
            return null;
        }

        return (IDOMModel) model;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DOM Model Validation Context: " + getURI();
    }

}
