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
package com.ibm.ws.st.core.internal.launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.ibm.ws.st.core.internal.Trace;

public class ConsoleReader {
    private static final String ERROR = "[ERROR   ]";
    private static final String FATAL = "[FATAL   ]";
    private static final String ERR = "[err]";

    private final StreamMonitor outListener;
    private final StreamMonitor errListener;

    private long size = -1;
    private File file = null;
    private BufferedReader reader = null;
    private String charset = null;

    public ConsoleReader(File file, StreamMonitor outListener, StreamMonitor errListener) {
        this.file = file;
        this.outListener = outListener;
        this.errListener = errListener;
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Monitoring " + file.getAbsolutePath());
    }

    protected void update() {
        try {
            // handle file roll-over or deletion
            if (reader != null && size != -1 && size > file.length()) {
                reader.close();
                reader = null;
            }
            if (reader == null && file.exists()) {
                if (charset != null)
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
                else
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            }

            if (reader == null)
                return;

            size = file.length();

            // read lines
            String s = reader.readLine();
            while (s != null) {
                append(s + "\n");
                s = reader.readLine();
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error updating stream monitor", e);
        }
    }

    private void append(String s) {
        if (s == null)
            return;

        boolean err = false;
        if (s.startsWith(ERROR))
            err = true;
        else if (s.startsWith(FATAL))
            err = true;
        else if (s.startsWith(ERR))
            err = true;

        if (err)
            errListener.streamAppended(s);
        else
            outListener.streamAppended(s);
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String aCharSet) {
        charset = aCharSet;
    }

    protected void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error closing stream monitor", e);
            }
            reader = null;
        }
    }
}
