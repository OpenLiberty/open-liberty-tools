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
package com.ibm.ws.st.common.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IServer;

public class CommonServerUtil {

    public static String getSecurePreferenceNodeName(IServer server) {
        IServer s = server;
        String nodeName = s.getServerType().getId() + s.getId();
        // Remove invalid characters in node name. It can contains ASCII characters between 32 and 126 
        // (alphanumerics and printable characters). It cannot contain two or more consecutive
        // forward slashes ('/'). It cannot end with a trailing forward slash.
        char[] chars = nodeName.toCharArray();
        StringBuffer sb = new StringBuffer();
        boolean lastSlash = false;

        for (int i = 0; i < chars.length; i++) {
            if ((chars[i] <= 31) || (chars[i] >= 127))
                continue;
            boolean isSlash = (chars[i] == IPath.SEPARATOR);
            if (lastSlash && isSlash)
                continue;
            sb.append(chars[i]);
            lastSlash = isSlash;
        }
        nodeName = sb.toString();
        return nodeName;
    }

}
