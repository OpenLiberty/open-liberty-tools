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
package com.ibm.ws.st.core.internal.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.ws.st.core.internal.Trace;

public class ConfigurationSchema {
    protected URL url;

    public ConfigurationSchema(URL url) throws IOException {
        this.url = url;
        load();
    }

    private void load() throws IOException {
        InputStream in = null;
        long time = System.currentTimeMillis();
        try {
            File file = new File(url.toURI());
            in = new BufferedInputStream(new FileInputStream(file));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            parser.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) throws SAXException {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Warning while reading configuration schema", e);
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error while reading configuration schema", e);
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error while reading configuration schema", e);
                }
            });
            parser.parse(new InputSource(in));
        } catch (IOException e) {
            Trace.logError("Could not load configuration schema from " + url, e);
            throw e;
        } catch (Exception e) {
            Trace.logError("Could not load configuration schema from " + url, e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                // ignore
            }
            if (Trace.ENABLED)
                Trace.tracePerf("Configuration schema load", time);
        }
    }

    /*
     * public List<ServerPort> getPorts() {
     * List<ServerPort> portList = new ArrayList<ServerPort>();
     * 
     * try {
     * XPathFactory factory = XPathFactory.newInstance();
     * XPath xpath = factory.newXPath();
     * XPathExpression expr = xpath.compile("//element[starts-with(@name, 'endpoint.')]");
     * Object result = expr.evaluate(document, XPathConstants.NODESET);
     * NodeList list = (NodeList) result;
     * int size = list.getLength();
     * for (int i = 0; i < size; i++) {
     * Node n = list.item(i);
     * if (n instanceof Element) {
     * Element element = (Element) n;
     * String id = element.getAttribute("name");
     * String text = "port=9080";
     * int port = -1;
     * if (text.startsWith("port=")) {
     * try {
     * port = Integer.parseInt(text.substring(5));
     * } catch (NumberFormatException nfe) {
     * // ignore
     * }
     * }
     * if (port > 0) {
     * ServerPort sp = new ServerPort(id, id.substring(9), port, "http");
     * portList.add(sp);
     * }
     * }
     * }
     * } catch (Exception e) {
     * Trace.trace(Trace.ERROR, "Error getting server ports", e);
     * }
     * 
     * return portList;
     * }
     */

    @Override
    public String toString() {
        return "WAS Configuration Schema [" + url + "]";
    }
}
