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
package com.ibm.ws.st.core.internal.config;

import java.io.File;

import org.eclipse.core.resources.IFile;

public class JVMOptions extends ExtendedConfigFile {

    protected long lastModified = -1;

    public JVMOptions(File file, IFile ifile) {
        super(file, ifile);
        lastModified = file.lastModified();
    }

    public boolean hasChanged() {
        long timestamp = file.lastModified();
        return timestamp != lastModified;
    }
}
