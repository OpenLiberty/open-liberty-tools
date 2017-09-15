/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;

public class IgnoredFeatureConflictMetadataHandler {

    private static final String ELE_CONFLICTS = "conflicts";
    private static final String ELE_CONFLICT = "conflict";
    private static final String ELE_CONFLICT_CHAIN = "conflictChain";
    private static final String ELE_FEATURE = "feature";

    public static void generateMetadataFile(IPath path, Set<FeatureConflict> knownConflicts) throws ParserConfigurationException, IOException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element conflictsElement = doc.createElement(ELE_CONFLICTS);
        doc.appendChild(conflictsElement);

        for (FeatureConflict conflict : knownConflicts) {
            Element conflictElement = doc.createElement(ELE_CONFLICT);
            addElement(conflictsElement, conflictElement, true, true);

            // add conflict chain A
            Element conflictChainA = doc.createElement(ELE_CONFLICT_CHAIN);
            addElement(conflictElement, conflictChainA, true, true);
            for (String feature : conflict.getDependencyChainA()) {
                Element featureElement = doc.createElement(ELE_FEATURE);
                addElement(conflictChainA, featureElement, true, true);
                Text text = doc.createTextNode(feature);
                featureElement.appendChild(text);
            }

            // add conflict chain B
            Element conflictChainB = doc.createElement(ELE_CONFLICT_CHAIN);
            addElement(conflictElement, conflictChainB, true, true);
            for (String feature : conflict.getDependencyChainB()) {
                Element featureElement = doc.createElement(ELE_FEATURE);
                addElement(conflictChainB, featureElement, true, true);
                Text text = doc.createTextNode(feature);
                featureElement.appendChild(text);
            }
        }

        saveDocument(path, doc);
    }

    private static void saveDocument(IPath path, Document d) throws IOException {
        BufferedOutputStream w = null;
        try {
            File ignoreFile = path.toFile();
            w = new BufferedOutputStream(new FileOutputStream(ignoreFile));
            save(w, d);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            try {
                if (w != null)
                    w.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static void save(OutputStream os, Document document) throws IOException {
        Result result = new StreamResult(os);
        Source source = new DOMSource(document);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);
        } catch (Exception e) {
            throw (IOException) (new IOException().initCause(e));
        }
    }

    public static Set<FeatureConflict> read(File file) throws SAXException, IOException, ParserConfigurationException {

        InputStream in = new BufferedInputStream(new FileInputStream(file));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {
                // The source view will flag this as a warning
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Warning while reading ignored features file.\nReason: " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                // The source view will flag this error, so we can ignore it.
                // Adding it as a warning to tracing
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error while reading ignored features file.\nReason: " + e.getMessage());
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                // The source view will flag this error, so we can ignore it.
                // Adding it as a warning to tracing
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error while reading ignored features file.\nReason: " + e.getMessage());
            }
        });
        Document doc = parser.parse(new InputSource(in));

        Set<FeatureConflict> conflictsSet = new HashSet<FeatureConflict>();

        Element conflictsElement = doc.getDocumentElement();
        for (Element conflict = getFirstChildElement(conflictsElement); conflict != null; conflict = getNextElement(conflict)) {

            Element chainAElem = getFirstChildElement(conflict);
            List<String> chainA = getFeatures(chainAElem);

            Element chainBElem = getNextElement(chainAElem);
            List<String> chainB = getFeatures(chainBElem);

            conflictsSet.add(new FeatureConflict(chainA, chainB));
        }
        return conflictsSet;
    }

    /**
     * @param conflictsElement
     * @return
     */
    private static Element getFirstChildElement(Element element) {
        for (Node currNode = element.getFirstChild(); currNode != null; currNode = currNode.getNextSibling()) {
            if (currNode.getNodeType() == Node.ELEMENT_NODE)
                return (Element) currNode;
        }
        return null;
    }

    public static List<String> getFeatures(Element chain) {
        List<String> features = new ArrayList<String>();
        if (chain == null)
            return features;
        for (Element fElem = getFirstChildElement(chain); fElem != null; fElem = getNextElement(fElem)) {
            String feature = fElem.getTextContent();
            if (feature != null && !feature.isEmpty() && !features.contains(feature))
                features.add(feature);
        }
        return features;

    }

    private static Element getNextElement(Element element) {
        if (element == null)
            return null;
        Node node = element.getNextSibling();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private static void addElement(Element parent, Element child, boolean isTopLevel, boolean hasChildren) {
        addPreElementText(parent, isTopLevel, hasChildren);
        parent.appendChild(child);
        addPostElementText(parent);
    }

    private static void addPreElementText(Element parent, boolean isTopLevel, boolean hasChildren) {
        if (parent == null)
            return;

        StringBuilder builder = new StringBuilder();
        if (isTopLevel || !hasChildren) {
            builder.append("\n    ");
        }
        Node node = parent.getParentNode();
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            builder.append("    ");
            node = node.getParentNode();
        }
        final Text text = parent.getOwnerDocument().createTextNode(builder.toString());
        parent.appendChild(text);
    }

    private static void addPostElementText(Element parent) {
        if (parent == null)
            return;

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        Node node = parent.getParentNode();
        while (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            builder.append("    ");
            node = node.getParentNode();
        }
        Node text = parent.getOwnerDocument().createTextNode(builder.toString());
        parent.appendChild(text);
    }

}
