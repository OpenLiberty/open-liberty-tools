/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * This is a helper class to convert interesting.messages.to.tools.txt into
 * runtimeMessages.txt. Just replace your paths below and run as a Java main.
 * 
 * The processing is basically to break up messages with {0} substitution into its
 * various parts and strip the property-based keys from the front of all strings
 * (since the message id is already unique).
 * 
 * Both of these things could be done at runtime, but it seems unnecessary to include
 * the extra content in our bundles, or do the extra processing on every startup.
 */
public class RuntimeHelperUtil {
    public static void main(String[] args) {
        RuntimeHelperUtil rh = new RuntimeHelperUtil();
        rh.trimFile();
    }

    protected String trimFile() {
        BufferedReader in = null;
        BufferedWriter out = null;
        BufferedWriter out2 = null;
        try {
            in = new BufferedReader(new FileReader("C:\\interesting.messages.to.tools.txt"));
            out = new BufferedWriter(new FileWriter("C:\\runtimeMessages.txt"));
            out2 = new BufferedWriter(new FileWriter("C:\\utilityMessages.txt"));

            String s = in.readLine();
            while (s != null) {
                int ind = s.indexOf("=");
                s = s.substring(ind + 1);

                int ind2 = s.indexOf("{0}");
                String start = s;
                String rest = "";
                if (ind2 >= 0) {
                    start = s.substring(0, ind2);
                    rest = s.substring(ind2 + 3);
                }

                int ind3 = rest.indexOf("{1}");
                String middle = "";
                String end = rest;
                if (ind3 >= 0) {
                    middle = rest.substring(0, ind3);
                    end = rest.substring(ind3 + 3);
                }

                if (start.startsWith("CWWKZ00")) {
                    out.write(start + "\n");
                    out.write(middle + "\n");
                    out.write(end + "\n");
                } else {
                    out2.write(start + "\n");
                    out2.write(middle + "\n");
                    out2.write(end + "\n");
                }

                s = in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                if (out2 != null)
                    out2.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
}