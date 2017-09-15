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
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Configuration editor input for configuration root document which locates in
 * Eclipse workspace
 */
public class FileConfigEditorInput extends FileEditorInput implements IConfigEditorInput {

    private final URI uri;
    private final String xpath;

    /**
     * @param file
     */
    public FileConfigEditorInput(IFile file, URI uri, String xpath) {
        super(file);
        this.uri = uri;
        this.xpath = xpath;
    }

    /** {@inheritDoc} */
    @Override
    public URI getURI() {
        return uri;
    }

    /** {@inheritDoc} */
    @Override
    public String getXPath() {
        return xpath;
    }
}
