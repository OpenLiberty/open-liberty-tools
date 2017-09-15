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
package com.ibm.ws.st.ui.internal.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Utility class to parse translated utility messages.
 * See com.ibm.ws.st.core.tests.util.RuntimeHelperUtil to generate utilityMessages.txt.
 */
public class UtilityMessageHelper {
    private static char[] convtBuf = new char[30];
    private static String[] messages;

    static {
        loadMessages();
    }

    private static void loadMessages() {
        BufferedReader br = null;
        List<String> list = new ArrayList<String>();
        try {
            br = new BufferedReader(new InputStreamReader(Activator.getInstance().getClass().getResourceAsStream("utility/utilityMessages.txt")));

            String s = br.readLine();
            while (s != null) {
                list.add(convertUnicode(s));
                s = br.readLine();
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not read utility message file", e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not close stream", e);
            }
        }
        messages = list.toArray(new String[list.size()]);
    }

    protected static int[] getPath(String message) {
        try {
            for (int i = 0; i < messages.length; i += 3) {
                String start = messages[i];
                String middle = messages[i + 1];
                String end = messages[i + 2];
                if (message.startsWith(start) && message.endsWith(end)) {
                    String s = message.substring(start.length(), message.length() - end.length());
                    if (middle != null && middle.length() > 0) {
                        int ind = s.indexOf(middle);
                        int st = start.length() + ind + middle.length();
                        return new int[] { st, st + s.length() - middle.length() - ind };
                    }
                    return null;
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not determine path", e);
        }
        return null;
    }

    private static String convertUnicode(String s) {
        int len = s.length();
        if (convtBuf.length < len) {
            int newLen = len * 2;
            if (newLen < 0)
                newLen = Integer.MAX_VALUE;

            convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf;
        int outLen = 0;
        int off = 0;

        while (off < len) {
            aChar = s.charAt(off++);
            if (aChar == '\\' && off < len) {
                aChar = s.charAt(off++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = s.charAt(off++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                        }
                    }
                    out[outLen++] = (char) value;
                } else {
                    out[outLen++] = aChar;
                }
            } else {
                out[outLen++] = aChar;
            }
        }
        return new String(out, 0, outLen);
    }
}
