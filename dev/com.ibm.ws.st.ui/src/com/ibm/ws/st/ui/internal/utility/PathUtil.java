/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.utility;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import com.ibm.ws.st.ui.internal.Trace;

public class PathUtil {

    public static IFile getBestIFileMatchForURI(URI uri) {

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IFile[] files = workspace.getRoot().findFilesForLocationURI(uri);

        int bestMatchCount = 0;
        IFile bestMatchFile = null;

        for (int i = 0; i < files.length; i++) {
            int segmentsMatched = segmentsMatched(uri.getPath().split("/"), files[i].getFullPath().segments());
            if (bestMatchCount == 0 || segmentsMatched > bestMatchCount) {
                bestMatchCount = segmentsMatched;
                bestMatchFile = files[i];
            }
        }

        return bestMatchFile;
    }

    private static int segmentsMatched(String[] uriSegments, String[] iFileSegments) {
        int matches = 0;

        int uriSegmentCount = uriSegments.length;
        int iFileSegmentCount = iFileSegments.length;

        while (uriSegmentCount > 0 && iFileSegmentCount > 0) {
            if (uriSegments[uriSegmentCount - 1].equals(iFileSegments[iFileSegmentCount - 1])) {
                matches++;
            }
            uriSegmentCount--;
            iFileSegmentCount--;
        }

        return matches;
    }

    public static URI getURIForFilePath(String filePath) {
        URI uri = null;
        try {
            uri = new URI("file:///" + filePath.replace("+", "%20").replace('\\', '/'));
        } catch (Exception exception) {
            Trace.logError("Could not create URI for file path " + filePath, exception);
        }
        return uri;
    }

}
