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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.wst.server.core.util.SocketUtil;

public class ErrorPageListener {
    private static ErrorPageListener instance;
    protected int port;

    private ErrorPageListener() {
        // do nothing
    }

    public synchronized static ErrorPageListener getInstance() {
        if (instance == null) {
            instance = new ErrorPageListener();
            instance.init();
        }
        return instance;
    }

    protected void init() {
        port = SocketUtil.findUnusedPort(2000, 4000);

        Thread t = new Thread("WebSphere error page listener") {
            @Override
            public void run() {
                initImpl();
            }
        };
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.setDaemon(true);
        t.start();
    }

    public int getPort() {
        return port;
    }

    protected void initImpl() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    Socket socket = serverSocket.accept();
                    InetAddress addr = socket.getInetAddress();
                    String host = addr.getHostName();
                    if (!SocketUtil.isLocalhost(host))
                        continue;

                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    HTTPHandler httpHandler = new HTTPHandler(in, ErrorPageListener.this.hashCode());
                    httpHandler.parse();
                } catch (Exception e) {
                    Trace.logError("Error in connection", e);
                } finally {
                    if (in != null)
                        try {
                            in.close();
                        } catch (Exception e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Problem closing input stream", e);
                        }
                    if (out != null)
                        try {
                            out.close();
                        } catch (Exception e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Problem closing output stream", e);
                        }
                }
            }
        } catch (IOException e) {
            Trace.logError("Error with error page listener", e);
        }
    }

    protected void handleRequest(String s) {
        ConsoleLineTracker.openJavaEditor(s);
    }
}
