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
package com.ibm.ws.st.common.core.internal;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A tracing utility
 */
public class TraceUtil {
    private static final String CLASS_NAME = TraceUtil.class.getName();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yy HH:mm.ss.SSS");

    public static final void perf(String s, long time, String traceClassName) {
        StringBuilder sb = new StringBuilder(s);
        sb.append(" <");
        sb.append(System.currentTimeMillis() - time);
        sb.append(">");
        trace(TraceLevel.PERFORMANCE, sb.toString(), null, traceClassName);
    }

    public static final void trace(byte level, String s, Throwable t, String traceClassName) {
        final String levelName = TraceLevel.getLevelName(level);
        final StringBuilder sb = new StringBuilder(TraceUtil.getFormattedDate());

        sb.append(" ");
        sb.append(levelName);

        final StackTraceElement caller = getCaller(traceClassName);
        if (caller != null) {
            sb.append(caller.getClassName());
            sb.append("::");
            sb.append(caller.getMethodName());
            sb.append('\t');
        }

        sb.append(s);

        System.out.println(sb.toString());
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    private static String getFormattedDate() {
        return SDF.format(new Date());
    }

    private static StackTraceElement getCaller(String traceClassName) {
        Exception ex = new Exception();
        StackTraceElement[] ste = ex.getStackTrace();
        for (int i = 0; i < ste.length; ++i) {
            final StackTraceElement s = ste[i];
            final String className = s.getClassName();
            // it must be our caller
            if (!className.equals(CLASS_NAME) && !className.equals(traceClassName)) {
                return s;
            }
        }

        return null;
    }
}
