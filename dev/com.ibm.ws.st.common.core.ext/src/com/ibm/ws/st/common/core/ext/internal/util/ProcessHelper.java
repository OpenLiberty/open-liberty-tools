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
package com.ibm.ws.st.common.core.ext.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;

public class ProcessHelper {

    private final static int BUFFER_STEP = 1024;

    /**
     * Wait for the given process to finish, and return the exit value and output. Checks for status every
     * <code>pollingDelay</code> ms, and terminates the process if it takes more than <code>timeout</code>
     * seconds. The progress monitor passed in must be <code>null</code> or already begun, and this method
     * will add <code>totalWork</code> to it's progress.
     * 
     * @param p the process to monitor
     * @param pollingDelay the delay between polling the process, in ms
     * @param timeout the process timeout, in seconds
     * @param totalWork the total amount of progress work that should be used
     * @param monitor a progress monitor, must not be null
     * @return the exit value
     * @throws IOException if the process fails to exit normally and cannot be terminated
     */
    public static ExecutionOutput waitForProcess(final Process p, int pollingDelay, float timeout, int totalWork, IProgressMonitor monitor) throws IOException, TimeoutException {

        int worked = 0;
        InputStream in = null;
        InputStream error = null;
        int iter = (int) (timeout * 1000f / pollingDelay);
        try {
            in = p.getInputStream();
            error = p.getErrorStream();
            StreamData stdData = new StreamData(in);
            StreamData errorData = new StreamData(error);
            for (int i = 0; i < iter; i++) {
                try {
                    Thread.sleep(pollingDelay);
                } catch (InterruptedException e) {
                    // ignore
                }

                // read standard output from the process
                readOutput(stdData);

                // read error output from the process
                readOutput(errorData);

                try {
                    int exitValue = p.exitValue();

                    // finish reading the output buffers
                    readOutput(stdData);
                    readOutput(errorData);

                    String outputStr = new String(stdData.buf, 0, stdData.len, Charset.forName("UTF-8"));
                    String errorStr = new String(errorData.buf, 0, errorData.len, Charset.forName("UTF-8"));

                    return new ExecutionOutput(exitValue, outputStr, errorStr);

                } catch (IllegalThreadStateException e) {
                    // process has not terminated yet
                }

                if (monitor != null && monitor.isCanceled())
                    break;

                int work = (int) (Math.sqrt((i + 1.0f) / iter) * totalWork);
                if (work > worked) {
                    if (monitor != null)
                        monitor.worked(work - worked);
                    worked = work;
                }
            }
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                // ignore
            }
            if (monitor != null && worked < totalWork) {
                monitor.worked(totalWork - worked);
            }
        }

        p.destroy();

        if (monitor != null && monitor.isCanceled())
            return new ExecutionOutput(0, "", null);

        throw new TimeoutException("Process did not complete and had to be terminated");
    }

    private static void readOutput(StreamData sd) throws IOException {
        int n = sd.in.available();
        while (n > 0) {
            if (sd.len + n > sd.buf.length) {
                byte[] temp = new byte[sd.buf.length + Math.max(n, BUFFER_STEP)];
                System.arraycopy(sd.buf, 0, temp, 0, sd.len);
                sd.buf = temp;
            }
            int bytesRead = sd.in.read(sd.buf, sd.len, n);
            if (bytesRead > 0)
                sd.len += bytesRead;
            n = sd.in.available();
        }
    }

    private static class StreamData {
        byte[] buf = new byte[BUFFER_STEP];
        int len = 0;
        InputStream in;

        StreamData(InputStream in) {
            this.in = in;
        }
    }
}
