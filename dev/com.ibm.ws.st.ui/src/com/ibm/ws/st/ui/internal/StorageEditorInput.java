/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.net.URI;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import com.ibm.ws.st.ui.internal.config.IConfigEditorInput;

public class StorageEditorInput implements IStorageEditorInput, IConfigEditorInput {
    private final IStorage storage;
    private final URI uri;

    StorageEditorInput(IStorage storage, URI uri) {
        this.storage = storage;
        this.uri = uri;
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return Activator.getImageDescriptor(Activator.IMG_CONFIG_FILE);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return storage.getName();
    }

    /** {@inheritDoc} */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getToolTipText() {
        return null;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class arg0) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public IStorage getStorage() throws CoreException {
        return storage;
    }

    /** {@inheritDoc} */
    @Override
    public URI getURI() {
        return uri;
    }

    /** {@inheritDoc} */
    @Override
    public String getXPath() {
        return null;
    }

}
