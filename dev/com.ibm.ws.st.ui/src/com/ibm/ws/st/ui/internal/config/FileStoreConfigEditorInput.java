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

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * Configuration editor input for configuration root document which locates outside
 * Eclipse workspace
 */
public class FileStoreConfigEditorInput extends FileStoreEditorInput implements IConfigEditorInput {

    private final String xpath;

    /**
     * @param fileStore
     */
    public FileStoreConfigEditorInput(IFileStore fileStore, String xpath) {
        super(fileStore);
        this.xpath = xpath;
    }

    /** {@inheritDoc} */
    @Override
    public String getXPath() {
        return xpath;
    }

}
