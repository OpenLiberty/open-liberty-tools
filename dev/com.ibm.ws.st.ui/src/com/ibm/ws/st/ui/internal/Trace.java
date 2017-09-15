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
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.ibm.ws.st.common.core.internal.TraceDebugListener;
import com.ibm.ws.st.common.core.internal.TraceLevel;
import com.ibm.ws.st.common.core.internal.TraceSpecification;
import com.ibm.ws.st.common.core.internal.TraceUtil;

/**
 * Helper class to route trace output.
 */
@SuppressWarnings("restriction")
public class Trace {
    private static final String CLASS_NAME = Trace.class.getName();

    public static final byte INFO = TraceLevel.INFO;
    public static final byte WARNING = TraceLevel.WARNING;
    public static final byte PERFORMANCE = TraceLevel.PERFORMANCE;
    public static final byte ERROR = TraceLevel.ERROR;

    public static boolean ENABLED = false;

    private static final byte[] TRACE_LEVELS = new byte[]
    {
     INFO, WARNING, PERFORMANCE, ERROR
    };
    private static final TraceDebugListener TDL = new TraceDebugListener() {
        @Override
        public void debugChanged(boolean value) {
            Trace.ENABLED = value;
        }
    };
    protected static final TraceSpecification TS = new TraceSpecification(Activator.PLUGIN_ID, TRACE_LEVELS, TDL);

    /**
     * Must use static fields and methods.
     */
    private Trace() {
        super();
    }

    /**
     * Trace the given message.
     * 
     * @param level the trace level
     * @param s a message
     */
    public static void trace(byte level, String s) {
        Trace.trace(level, s, null);
    }

    /**
     * Trace the given message and exception.
     * 
     * @param level the trace level
     * @param s a message
     * @param t a throwable
     */
    public static void trace(byte level, String s, Throwable t) {
        if (!TS.isLevelEnabled(level))
            return;

        TraceUtil.trace(level, s, t, CLASS_NAME);
    }

    /**
     * Helper method to output a performance trace at the end of an operation.
     * 
     * @param s a message
     * @param time the time the operation started, from System.currentTimeMillis()
     */
    public static void tracePerf(String s, long time) {
        if (!TS.isLevelEnabled(PERFORMANCE)) {
            return;
        }

        TraceUtil.perf(s, time, CLASS_NAME);
    }

    /**
     * Log an error to the .log file.
     * 
     * @param s a message
     * @param t a throwable (may be null)
     */
    public static void logError(String s, Throwable t) {
        Activator.getInstance().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, s, t));
    }
}
