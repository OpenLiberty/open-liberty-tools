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
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import com.ibm.ws.st.core.internal.config.ConfigUtils;

/**
 * Tester that checks if a file is a server configuration file without
 * throwing an exception if the file is out of sync with the file system.
 * If it is out of sync, this returns false.
 */
public class ConfigurationFileTester extends PropertyTester {

    private static String CONFIG_FILE_PROPERTY = "isConfigFile";
    private static String XML_EXTENSION = "xml";

    /** {@inheritDoc} */
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (CONFIG_FILE_PROPERTY.equals(property) && receiver instanceof IFile) {
            IFile file = (IFile) receiver;
            String extension = file.getFullPath().getFileExtension();
            if (XML_EXTENSION.equalsIgnoreCase(extension) && file.isSynchronized(IResource.DEPTH_ZERO)) {
                return ConfigUtils.isServerConfigFile(file);
            }
        }
        return false;
    }

}
