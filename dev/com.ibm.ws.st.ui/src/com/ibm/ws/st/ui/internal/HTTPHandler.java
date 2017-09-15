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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.widgets.Display;

public class HTTPHandler {
    private static final int BUFFER = 2048;
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static AtomicInteger COUNTER = new AtomicInteger(0);
    private final int count = COUNTER.getAndIncrement();

    private final byte[] readBuffer = new byte[BUFFER];

    // buffer and index
    private byte[] buffer = new byte[0];
    private int bufferIndex = 0;

    private final InputStream in;
    private final String hash;

    private int contentLength = -1;
    private byte transferEncoding = -1;

    private static final String[] ENCODING_STRING = new String[]
        { "chunked", "identity", "gzip", "compressed", "deflate" };

    private static final byte ENCODING_IDENTITY = 1;

    public HTTPHandler(InputStream in, int hash) {
        this.in = in;
        this.hash = hash + "";

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Started: " + this);
    }

    /**
     * Read more data into the buffer.
     */
    private void fillBuffer() throws IOException {
        int n = in.read(readBuffer);

        if (n <= 0)
            throw new IOException("End of input");

        // add to full buffer
        int len = buffer.length - bufferIndex;
        if (len < 0)
            len = 0;
        byte[] x = new byte[n + len];
        System.arraycopy(buffer, bufferIndex, x, 0, len);
        System.arraycopy(readBuffer, 0, x, len, n);
        bufferIndex = 0;
        buffer = x;
    }

    /**
     * Returns the first location of a CRLF.
     * 
     * @return int
     */
    private int getFirstCRLF() {
        int size = buffer.length;
        int i = bufferIndex + 1;
        while (i < size) {
            if (buffer[i - 1] == CR && buffer[i] == LF)
                return i;
            i++;
        }
        return -1;
    }

    /**
     * Parse the HTTP body.
     * 
     * @throws IOException
     */
    private void parseBody() throws IOException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Parsing body for: " + this);

        if (contentLength != -1) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Parsing body");
            byte[] b2 = null;
            int b2Index = 0;
            if (contentLength < 1024 * 1024)
                b2 = new byte[contentLength];
            byte[] b = removeFromBuffer(Math.min(buffer.length, bufferIndex + contentLength));
            if (b2 != null) {
                System.arraycopy(b, 0, b2, 0, b.length);
                b2Index += b.length;
            }
            int bytesLeft = contentLength - b.length;

            int n = 0;
            while (bytesLeft > 0) {
                n = in.read(readBuffer, 0, Math.min(readBuffer.length, bytesLeft));
                bytesLeft -= n;
                if (b2 != null) {
                    System.arraycopy(readBuffer, 0, b2, b2Index, n);
                    b2Index += n;
                }
            }

            if (b2 != null)
                openEditor(new String(b2));
        } else if (transferEncoding != -1 && transferEncoding != ENCODING_IDENTITY) {
            parseChunk();
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Done parsing body for: " + this);
        return;

    }

    /**
     * Parse an HTTP chunk.
     * 
     * @throws IOException
     */
    private void parseChunk() throws IOException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Parsing chunk");
        boolean done = false;
        byte[] body = new byte[0];

        while (!done) {
            // read chunk size
            byte[] b = readLine();

            String s = new String(b);
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Chunk-length: " + s);
            int index = s.indexOf(" ");
            int length = -1;
            try {
                if (index > 0)
                    s = s.substring(0, index);
                length = Integer.parseInt(s.trim(), 16);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Error chunk for: " + this, e);
            }

            if (length <= 0)
                done = true;
            else {
                // read and output chunk data plus CRLF
                b = readBytes(length + 2);

                // copy to HTTP body
                byte[] temp = new byte[body.length + b.length - 2];
                System.arraycopy(body, 0, temp, 0, body.length);
                System.arraycopy(b, 0, temp, body.length, b.length - 2);
                body = temp;
            }
        }

        // read trailer
        byte[] b = readLine();
        while (b.length > 2) {
            b = readLine();
        }

        openEditor(new String(body));
    }

    /**
     * Parse an HTTP header.
     * 
     * @throws IOException
     */
    private void parseHeader() throws IOException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Parsing header for: " + this);

        // read until first blank line
        byte[] b = readLine();
        boolean first = true;
        while (b.length > 5) {
            String s = new String(b);
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Header: " + traceBytes(s));
            if (first) {
                int index = s.indexOf(" ");
                if (index >= 0) {
                    String t = s.substring(index + 1);
                    index = t.indexOf(" ");
                    t = t.substring(0, index);
                    if (t.startsWith("/" + hash + "/")) {
                        t = t.substring(2 + hash.length());
                        openEditor(t);
                    }
                }
                first = false;
            }

            parseHeaderLine(s);
            b = readLine();
        }

        if (Trace.ENABLED) {
            String s = new String(b);
            Trace.trace(Trace.INFO, "Final header: " + traceBytes(s));
        }
    }

    private static String traceBytes(String s) {
        String t = s.replace("\n", "/n");
        t = t.replace("\r", "/r");
        return t;
    }

    /**
     * Read bytes from the stream.
     * 
     * @return byte[]
     */
    private byte[] readBytes(int n) throws IOException {
        while (buffer.length - bufferIndex < n)
            fillBuffer();

        return removeFromBuffer(bufferIndex + n);
    }

    /**
     * Read and return the next full line.
     * 
     * @return byte[]
     */
    private byte[] readLine() throws IOException {
        int n = getFirstCRLF();
        while (n < 0) {
            fillBuffer();
            n = getFirstCRLF();
        }
        return removeFromBuffer(n + 1);
    }

    /**
     * Remove data from the buffer up to the absolute index n.
     * Return the data from between bufferIndex and n.
     * 
     * @param n the bytes to remove
     * @return a byte array
     */
    private byte[] removeFromBuffer(int n) {
        // copy line out of buffer
        byte[] b = new byte[n - bufferIndex];
        System.arraycopy(buffer, bufferIndex, b, 0, n - bufferIndex);

        if (buffer.length > BUFFER * 2 || bufferIndex > BUFFER) {
            // remove line from buffer
            int size = buffer.length;
            byte[] x = new byte[size - n];
            System.arraycopy(buffer, n, x, 0, size - n);
            buffer = x;
            bufferIndex = 0;
        } else
            bufferIndex = n;

        return b;
    }

    /**
     * Listen for input, save it, and pass to the output stream.
     * Philosophy: Read a single line separately and translate.
     * When blank line is reached, just pass all other data through.
     */
    public void parse() {
        try {
            contentLength = -1;
            transferEncoding = -1;

            parseHeader();
            parseBody();

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Done HTTP request for " + this);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Error in: " + this, e);
        }
    }

    /**
     * Parse an individual header line.
     * 
     * @return byte[]
     * @param b byte[]
     */
    private void parseHeaderLine(String s) {
        if (s.toLowerCase().startsWith("content-length: ")) {
            try {
                contentLength = Integer.parseInt(s.substring(16).trim());
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Content length: " + this + " " + contentLength);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Content length error", e);
            }
        } else if (s.toLowerCase().startsWith("transfer-encoding: ")) {
            String t = s.substring(19).trim();
            int size = ENCODING_STRING.length;
            for (int i = 0; i < size; i++) {
                if (ENCODING_STRING[i].equalsIgnoreCase(t)) {
                    transferEncoding = (byte) i;
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Transfer encoding: " + ENCODING_STRING[i]);
                }
            }
        }
    }

    private void openEditor(final String s) {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Opening Java editor on: " + s);
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                ConsoleLineTracker.openJavaEditor(s);
            }
        });
    }

    @Override
    public String toString() {
        return "HTTPHandler " + count;
    }
}
