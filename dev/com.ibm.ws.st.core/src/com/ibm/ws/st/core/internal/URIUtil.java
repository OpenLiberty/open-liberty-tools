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
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class URIUtil {
    /**
     * Make sure the URI is canonical
     */
    public static URI getCanonicalURI(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty())
            return uri;
        try {
            File file = new File(path);
            file = new File(file.getCanonicalPath());
            return file.toURI();
        } catch (IOException e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to canonicalize the runtime path: " + path, e);
            }
        }
        return uri;
    }

    public static URI canonicalRelativize(URI relativeTo, URI beRelative) {
        return getCanonicalURI(relativeTo).relativize(getCanonicalURI(beRelative));
    }
}