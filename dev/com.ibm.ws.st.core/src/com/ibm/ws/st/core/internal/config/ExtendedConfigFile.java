/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
import java.net.URI;

import org.eclipse.core.resources.IFile;

public class ExtendedConfigFile {
    public static final String BOOTSTRAP_PROPS_FILE = "bootstrap.properties";
    public static final String SERVER_ENV_FILE = "server.env";
    public static final String JVM_OPTIONS_FILE = "jvm.options";
    public static final String ETC_DIR = "etc";

    protected IFile ifile;
    protected File file;

    public ExtendedConfigFile(File file, IFile ifile) {
        this.file = file;
        this.ifile = ifile;
    }

    public static boolean isExtendedConfigFile(String fileName) {
        return ExtendedConfigFile.BOOTSTRAP_PROPS_FILE.equals(fileName)
               || ExtendedConfigFile.JVM_OPTIONS_FILE.equals(fileName) || ExtendedConfigFile.SERVER_ENV_FILE.equals(fileName);
    }

    public String getName() {
        return file.getName();
    }

    public File getFile() {
        return file;
    }

    public URI getURI() {
        return file.toURI();
    }

    public IFile getIFile() {
        return ifile;
    }

    @Override
    public String toString() {
        return "ExtendedConfig [" + file + "]";
    }
}
