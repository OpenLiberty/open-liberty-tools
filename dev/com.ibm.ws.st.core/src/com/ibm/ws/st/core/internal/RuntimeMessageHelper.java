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
package com.ibm.ws.st.core.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Utility class to parse translated runtime messages for the application name.
 * See com.ibm.ws.st.core.tests.util.RuntimeHelperUtil to generate runtimeMessages.txt.
 */
public class RuntimeMessageHelper {
    private static char[] convtBuf = new char[30];
    private static String[] messages;
    private static int KEY_LENGTH = 12; // e.g. "CWWKZ0003I: "

    static {
        loadMessages();
    }

    protected static void loadMessages() {
        BufferedReader in = null;
        List<String> list = new ArrayList<String>();
        try {
            in = new BufferedReader(new InputStreamReader(Activator.getInstance().getClass().getResourceAsStream("runtimeMessages.txt")));

            String s = in.readLine();
            while (s != null) {
                list.add(convertUnicode(s));
                s = in.readLine();
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not read runtime messages file", e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not close stream", e);
            }
        }
        messages = list.toArray(new String[list.size()]);
    }

    protected static String getAppName(String message) {
        try {
            for (int i = 0; i < messages.length; i += 3) {
                String start = messages[i];
                String end = messages[i + 2];
                // There are cases where the timing information is right after
                // the message key, so we just match the key and end, then we
                // look for the rest of start (excluding the key) in message
                int indexStart = message.indexOf(start);
                if (message.regionMatches(indexStart, start, 0, KEY_LENGTH) && message.endsWith(end)) {
                    start = start.substring(KEY_LENGTH);
                    int index = message.indexOf(start, indexStart + KEY_LENGTH);
                    if (index != -1) {
                        int startIndex = start.length() + index;
                        int endIndex = message.length() - end.length();
                        if (startIndex < endIndex) {
                            String s = message.substring(startIndex, endIndex);
                            String middle = messages[i + 1];
                            if (middle != null && middle.length() > 0) {
                                int ind = s.indexOf(middle);
                                return s.substring(0, ind);
                            }
                            return s;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not determine app name", e);
        }
        return null;
    }

    protected static Set<String> getAllSubstitutionText(String message, IServer server) {

        HashSet<String> set = new HashSet<String>();

        try {
            for (int i = 0; i < messages.length; i += 3) {
                String start = messages[i];
                String end = messages[i + 2];
                // There are cases where the timing information is right after
                // the message key, so we just match the key and end, then we
                // look for the rest of start (excluding the key) in message
                int indexStart = message.indexOf(start);
                if (message.regionMatches(indexStart, start, 0, KEY_LENGTH) && message.endsWith(end)) {
                    start = start.substring(KEY_LENGTH);
                    int index = message.indexOf(start, indexStart + KEY_LENGTH);
                    if (index != -1) {
                        int startIndex = start.length() + index;
                        int endIndex = message.length() - end.length();
                        if (startIndex < endIndex) {
                            String s = message.substring(startIndex, endIndex);
                            String middle = messages[i + 1];
                            if (middle != null && middle.length() > 0) {
                                int ind = s.indexOf(middle);
                                String name = s.substring(0, ind);
                                set.add(name);
                                name = s.substring(ind + middle.length(), s.length());
                                set.add(name);
                            } else {
                                set.add(s);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not get message substitution text from: " + message, e);
        }
        return set;
    }

    protected static String matchAppNameFromWorkspaceProjects(String message, IServer server) {

        try {
            for (int i = 0; i < messages.length; i += 3) {
                String start = messages[i];
                String end = messages[i + 2];
                // There are cases where the timing information is right after
                // the message key, so we just match the key and end, then we
                // look for the rest of start (excluding the key) in message
                int indexStart = message.indexOf(start);
                if (message.regionMatches(indexStart, start, 0, KEY_LENGTH) && message.endsWith(end)) {
                    start = start.substring(KEY_LENGTH);
                    int index = message.indexOf(start, indexStart + KEY_LENGTH);
                    if (index != -1) {
                        int startIndex = start.length() + index;
                        int endIndex = message.length() - end.length();
                        if (startIndex < endIndex) {
                            String s = message.substring(startIndex, endIndex);
                            String middle = messages[i + 1];
                            if (middle != null && middle.length() > 0) {
                                int ind = s.indexOf(middle);
                                String name = s.substring(0, ind);
                                // check the first substitute variable
                                if (matchesProjectName(name, server))
                                    return name;
                                // if the first variable didn't meatch check the second substitute variable
                                name = s.substring(ind + middle.length(), s.length());
                                if (matchesProjectName(name, server))
                                    return name;
                            }
                            if (matchesProjectName(s, server))
                                return s;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not determine app name", e);
        }
        return null;
    }

    private static boolean matchesProjectName(String text, IServer server) {
        try {
            IModule[] modules = server.getModules();
            for (IModule module : modules) {
                if (module.getName().equals(text)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not match app name \"" + text + "\" in message to workspace project names", t);
        }
        return false;
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
            if (aChar == '\\') {
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