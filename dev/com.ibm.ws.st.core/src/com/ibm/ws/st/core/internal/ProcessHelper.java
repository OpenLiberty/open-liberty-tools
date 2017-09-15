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
package com.ibm.ws.st.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;

public class ProcessHelper {

    public static class ProcessResult {
        private final int exitValue;
        private final String sysOut;

        public ProcessResult(int exitValue, String sysOut) {
            this.exitValue = exitValue;
            this.sysOut = sysOut;
        }

        public int getExitValue() {
            return exitValue;
        }

        public String getOutput() {
            return sysOut.toString();
        }
    }

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
    public static ProcessResult waitForProcess(final Process p, int pollingDelay, float timeout, int totalWork, IProgressMonitor monitor) throws IOException, TimeoutException {
        final int BUFFER_STEP = 1024;
        byte[] buf = new byte[BUFFER_STEP];
        int len = 0;
        int worked = 0;
        InputStream in = null;
        int iter = (int) (timeout * 1000f / pollingDelay);
        try {
            in = p.getInputStream();
            for (int i = 0; i < iter; i++) {
                try {
                    Thread.sleep(pollingDelay);
                } catch (InterruptedException e) {
                    // ignore
                }

                // read data from the process
                int n = in.available();
                while (n > 0) {
                    if (len + n > buf.length) {
                        byte[] temp = new byte[buf.length + Math.max(n, BUFFER_STEP)];
                        System.arraycopy(buf, 0, temp, 0, len);
                        buf = temp;
                    }
                    int bytesRead = in.read(buf, len, n);
                    if (bytesRead > 0)
                        len += bytesRead;
                    n = in.available();
                }

                try {
                    int exitValue = p.exitValue();

                    // finish reading the output buffer
                    n = in.available();
                    while (n > 0) {
                        if (len + n > buf.length) {
                            byte[] temp = new byte[buf.length + n];
                            System.arraycopy(buf, 0, temp, 0, len);
                            buf = temp;
                        }
                        int bytesRead = in.read(buf, len, n);
                        if (bytesRead > 0)
                            len += bytesRead;
                        n = in.available();
                    }

                    return new ProcessResult(exitValue, new String(buf, 0, len, Charset.forName("UTF-8")));
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
            return new ProcessResult(0, "");

        throw new TimeoutException("Process did not complete and had to be terminated");
    }
}
