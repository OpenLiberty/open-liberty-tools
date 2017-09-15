/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.repository;

import java.io.File;
import java.net.PasswordAuthentication;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public abstract class AbstractInstaller {

    public static final String RUNTIME_LOCATION = "runtimeLocation";
    public static final String VM_INSTALL = "vmInstall";
    public static final String WEBSPHERE_SERVER = "server";
    public static final String REPO_PROPS_LOCATION = "repoPropsLocation";

    protected static final String TEMP_FILE_NAME = "download";

    public abstract IStatus install(IProduct product, PasswordAuthentication pa, Map<String, Object> settings, IProgressMonitor monitor2);

    public long getTickCount(IProduct product) {
        // check for local archive
        try {
            File f = new File(product.getSource().getLocation());
            if (f.exists()) {
                return (product.getSize() / 10240);
            }
        } catch (NullPointerException e) {
            // ignore
        }

        // remote archive
        return ((product.getSize() / 10240) * 11);
    }
}
