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
package com.ibm.ws.st.core.internal.config;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.ibm.ws.st.core.internal.Trace;

/**
 *
 */
public class SchemaHelper {
    private HashMap<String, Boolean> supportedApplicationElements;
    private final URL schemaURL;
    private Document document = null;

    /**
     * 
     */
    public SchemaHelper(URL schemaURL) {
        this.schemaURL = schemaURL;
    }

    private Document getDocument() {
        if (document == null && schemaURL != null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db;
            try {
                db = dbf.newDocumentBuilder();
                document = db.parse(schemaURL.openStream());
            } catch (ParserConfigurationException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not parse the schema.", e);
            } catch (SAXException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not parse the schema.", e);
            } catch (IOException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not parse the schema.", e);
            }
        }
        return document;
    }

    public boolean isSupportedApplicationElement(String appElementName) {
        if (supportedApplicationElements != null) {
            Boolean b = supportedApplicationElements.get(appElementName);
            if (b != null)
                return b.booleanValue();
        } else {
            supportedApplicationElements = new HashMap<String, Boolean>();
        }

        boolean b = false;
        Document doc = getDocument();
        if (doc != null) {
            StringBuilder sb = new StringBuilder("/xsd:schema/xsd:complexType[@name='serverType']/xsd:choice/xsd:element[@name='");
            sb = sb.append(appElementName);
            sb = sb.append("']");
// for debugging you can pass this string to the below call and it will return the node.  String xpathStr = "/xsd:schema/xsd:complexType[@name='serverType']/xsd:choice/xsd:element[@name='application']";
            Node node = DOMUtils.getSchemaNode(doc, sb.toString());
            b = node != null;
            supportedApplicationElements.put(appElementName, Boolean.valueOf(b));
        }
        return b;
    }

}
