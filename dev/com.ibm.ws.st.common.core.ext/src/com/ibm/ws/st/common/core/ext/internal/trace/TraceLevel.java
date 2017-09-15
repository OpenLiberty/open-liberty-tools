/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.trace;

/**
 * Trace levels
 */
public class TraceLevel {

    /**
     * Trace level constants
     * 
     * When adding a new trace level, make sure to update the level names,
     * the debug option names, as well as @see TraceSpecification
     */
    public static final byte INFO = 0;
    public static final byte WARNING = 1;
    public static final byte RESOURCE = 2;
    public static final byte EXTENSION_POINT = 3;
    public static final byte PERFORMANCE = 4;
    public static final byte JMX = 5;
    public static final byte ERROR = 6;
    public static final byte DETAILS = 7;
    public static final byte SSM = 8;
    public static final byte SECURITY = 9;

    private static final String LEVEL_NAMES[] = new String[]
    {
     "INFO      ",
     "WARNING   ",
     "RESOURCE  ",
     "EXTENSION ",
     "PERF      ",
     "JMX       ",
     "ERROR     ",
     "DETAILS   ",
     "SSM       ",
     "SECURITY  "
    };

    private static final String DEBUG_OPTION_NAMES[] = new String[]
    {
     "info", "warning", "resource", "extension", "perf", "jmx", "error", "details", "ssm", "security"
    };

    public static final String getLevelName(byte level) {
        if (level < INFO || level > SECURITY) {
            return "";
        }

        return LEVEL_NAMES[level];
    }

    public static final String getDebugOptionName(byte level) {
        if (level < INFO || level > SECURITY) {
            return null;
        }

        return DEBUG_OPTION_NAMES[level];
    }
}
