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
package com.ibm.ws.st.ui.internal.merge;

import java.io.File;

import com.ibm.xwt.dde.editor.ReadOnlyFileStorage;

/**
 * Extend ReadOnlyFileStorage so that the name can be controlled.
 */
public class MergedEditorFileStorage extends ReadOnlyFileStorage {
    String name;

    public MergedEditorFileStorage(File file, String name) {
        super(file);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
