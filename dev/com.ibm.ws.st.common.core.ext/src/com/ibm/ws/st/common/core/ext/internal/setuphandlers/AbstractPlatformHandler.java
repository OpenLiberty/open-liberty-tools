/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.setuphandlers;

public abstract class AbstractPlatformHandler {

    /**
     * @param property
     * @return
     */
    protected String ensureEndsWithPathSeparator(String path) {
        if (path == null)
            return path;
        String p = path;
        if (path.contains("/") && !path.endsWith("/"))
            p = path + "/";
        else if (path.contains("\\") && !path.endsWith("\\"))
            p = path + "\\";
        return p;
    }

}
