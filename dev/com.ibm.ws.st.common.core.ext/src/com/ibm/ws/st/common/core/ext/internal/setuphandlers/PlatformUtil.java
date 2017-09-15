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

import com.ibm.ws.st.common.core.ext.internal.Trace;

/**
 * Platform utility class. Provides utilities such as determining the
 * operating system.
 */
public class PlatformUtil {

    public enum OperatingSystem {
        LINUX,
        MAC,
        WINDOWS
    }

    public static OperatingSystem getOS(String osName) {
        if (osName == null || osName.isEmpty()) {
            Trace.logError("The operating system name is null or empty, defaulting to Linux.", null);
            return OperatingSystem.LINUX;
        }

        String name = osName.toLowerCase();
        if (name.contains("win"))
            return OperatingSystem.WINDOWS;
        if (name.contains("mac"))
            return OperatingSystem.MAC;
        if (name.contains("linux"))
            return OperatingSystem.LINUX;

        Trace.logError("The operating system name is not valid: " + osName + ", defaulting to Linux.", null);
        return OperatingSystem.LINUX;
    }

}
