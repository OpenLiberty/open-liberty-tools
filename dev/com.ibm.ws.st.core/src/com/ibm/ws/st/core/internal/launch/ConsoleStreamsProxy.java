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

import java.io.File;
import java.io.IOException;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy2;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

public class ConsoleStreamsProxy implements IStreamsProxy2 {
    private static final int DELAY = 500;
    private static final int JMX_DELAY = 5000;

    final ConsoleReader consoleMonitor;
    private final StreamMonitor outMonitor;
    private final StreamMonitor errMonitor;
    private final File consoleFile;
    private final boolean useConsoleLog;

    protected Thread streamThread;
    protected boolean done = false;
    protected JMXConnection jmx;

    public ConsoleStreamsProxy(File consoleFile, boolean useConsoleLog, JMXConnection jmx) {
        if (consoleFile == null)
            throw new IllegalArgumentException("Console file cannot be null");
        this.consoleFile = consoleFile;
        this.useConsoleLog = useConsoleLog;
        outMonitor = new StreamMonitor(useConsoleLog);
        errMonitor = new StreamMonitor(useConsoleLog);
        consoleMonitor = new ConsoleReader(consoleFile, outMonitor, errMonitor);
        this.jmx = jmx;

        startMonitoring();
    }

    @Override
    public IStreamMonitor getErrorStreamMonitor() {
        return errMonitor;
    }

    @Override
    public IStreamMonitor getOutputStreamMonitor() {
        return outMonitor;
    }

    @Override
    public void write(String input) throws IOException {
        // no stdin support, ignore
    }

    public void close() {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Terminating console monitor");
        done = true;
        consoleMonitor.close();
    }

    protected void update() {
        if (jmx != null)
            updateLogFileViaJMX();
        consoleMonitor.update();
    }

    private void startMonitoring() {
        if (streamThread != null)
            return;

        streamThread = new Thread("WebSphere console monitor") {
            @Override
            public void run() {
                // check the charset before attempting to read log files
                checkAndSetCharset();
                while (!done) {
                    update();

                    try {
                        int delay = jmx == null ? DELAY : JMX_DELAY;
                        sleep(delay);
                    } catch (Exception e) {
                        // do nothing
                    }
                }
                streamThread = null;
            }

            private void checkAndSetCharset() {
                if (jmx == null)
                    return;
                try {
                    String osNameAttr = (String) jmx.getMBeanAttribute("java.lang:type=OperatingSystem", "Name");
                    if (osNameAttr != null && osNameAttr.equals("z/OS"))
                        consoleMonitor.setCharset("IBM1047");
                } catch (Exception e) {
                    Trace.logError("Problem while checking the charset", e);
                }
            }
        };
        streamThread.setPriority(Thread.MIN_PRIORITY);
        streamThread.setDaemon(true);
        streamThread.start();
    }

    @Override
    public void closeInputStream() throws IOException {
        // no stdin support, ignore
    }

    private void updateLogFileViaJMX() {
        if (jmx == null)
            return;
        try {
            // TODO: use jmx getMetadata to look up messages file name and location since they 
            // can be configured in server.xml and bootsrap.properties once 146254: Unable to resolve logging variables correctly
            // is resolved
//            CompositeData metadata = (CompositeData) jmx.getMetadata(Constants.MESSAGES_FILENAME_VAR, "a");
//            String messagesFileName = metadata == null ? Constants.MESSAGES_LOG : (String) metadata.get("fileName");
            /**
             * jmx.isConnected() is to test if jmx connection is there or not to the remote server
             * since jmx connection could have dropped in between.
             */
            String messagesFileName = Constants.MESSAGES_LOG;
            String logFileName = useConsoleLog ? Constants.CONSOLE_LOG : messagesFileName;
            if (jmx.isConnected()) {
                CompositeData metadata = (CompositeData) jmx.getMetadata(Constants.LOGGING_DIR_VAR, "a");
                String logFileDirectory = (String) metadata.get("fileName");
                IPath consoleFilePath = new Path(consoleFile.getAbsolutePath());
                if (!consoleFile.exists())
                    FileUtil.makeDir(consoleFilePath.removeLastSegments(1));
                jmx.downloadFile(logFileDirectory + "/" + logFileName, consoleFilePath.toOSString());
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Failed to download latest console file.", e);
        }
    }
}
