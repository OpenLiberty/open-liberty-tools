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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLWriter extends PrintWriter {
    private static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final String lineSeparator;
    private int tab = 0;

    public XMLWriter(OutputStream stream, IProject project) {
        this(new PrintWriter(stream), project);
    }

    public XMLWriter(Writer writer, IProject project) {
        super(writer);
        lineSeparator = getLineSeparator(project);

        print(XML_VERSION);
        print(this.lineSeparator);
    }

    private static String encode(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 10);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String replacement = getEntity(c);
            if (replacement != null) {
                sb.append('&');
                sb.append(replacement);
                sb.append(';');
            } else
                sb.append(c);
        }
        return sb.toString();
    }

    private static String getEntity(char c) {
        switch (c) {
            case '<':
                return "lt";
            case '>':
                return "gt";
            case '"':
                return "quot";
            case '\'':
                return "apos";
            case '&':
                return "amp";
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static String getLineSeparator(IProject project) {
        String lineSeparator = null;

        if (Platform.isRunning()) {
            // line delimiter in project preference
            IScopeContext[] scopeContext;
            if (project != null) {
                scopeContext = new IScopeContext[] { new ProjectScope(project) };
                lineSeparator = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
                if (lineSeparator != null)
                    return lineSeparator;
            }

            // line delimiter in workspace preference
            scopeContext = new IScopeContext[] { new InstanceScope() };
            lineSeparator = Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
            if (lineSeparator != null)
                return lineSeparator;
        }

        // system line delimiter
        return System.getProperty("line.separator");
    }

    private void printTab() {
        for (int i = 0; i < tab; i++)
            print("    ");
    }

    public void print(Document d) {
        print(d.getDocumentElement());
    }

    public void print(Node e) {
        printTab();

        print('<');
        print(e.getNodeName());
        NamedNodeMap nnm = e.getAttributes();
        int attrSize = nnm.getLength();
        if (attrSize > 0) {
            Node[] attr = new Node[attrSize];
            for (int i = 0; i < attrSize; i++)
                attr[i] = nnm.item(i);

            Arrays.sort(attr, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    Node n1 = (Node) o1;
                    Node n2 = (Node) o2;
                    return n1.getNodeName().compareTo(n2.getNodeName());
                }
            });

            for (int i = 0; i < attrSize; i++) {
                print(' ');
                print(attr[i].getNodeName());
                print("=\"");
                print(encode(attr[i].getNodeValue()));
                print('\"');
            }
        }

        NodeList nodeList = e.getChildNodes();
        int childSize = nodeList.getLength();

        if (childSize == 0) {
            print("/>");
            print(lineSeparator);
            return;
        }
        print(">");
        tab++;
        print(lineSeparator);

        for (int i = 0; i < childSize; i++) {
            Node n = nodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE)
                print(n);
        }

        tab--;
        printTab();
        print("</");
        print(e.getNodeName());
        print(">");
        print(lineSeparator);
    }
}