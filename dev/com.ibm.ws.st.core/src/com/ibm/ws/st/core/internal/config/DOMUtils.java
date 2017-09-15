/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;

/**
 * Set of DOM utilities for the configuration file.
 */
public class DOMUtils {

    private static DocumentBuilder builder = null;

    public static Element getFirstChildElement(Node start) {
        if (start == null)
            return null;

        Node node = start.getFirstChild();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start) {
        Node node = start.getNextSibling();
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    /**
     * Returns all child elements of the parent node
     * that match with the provided name.
     *
     * @param parent
     * @param name
     * @return
     */
    public static List<Element> getAllElements(Node parent, String name) {
        List<Element> elements = new ArrayList<Element>();

        for (Element elem = getFirstChildElement(parent, name); elem != null; elem = getNextElement(elem, name)) {
            elements.add(elem);
        }

        return elements;
    }

    public static Element getFirstChildElement(Node start, String name) {
        if (start == null)
            return null;

        Node node = start.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start, String name) {
        Node node = start.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getFirstChildElement(Node start, String ns, String name) {
        if (ns == null) {
            return getFirstChildElement(start, name);
        }
        Node node = start.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(name) && ns.equals(node.getNamespaceURI())) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextElement(Node start, String ns, String name) {
        if (ns == null) {
            return getNextElement(start, name);
        }
        Node node = start.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(name) && ns.equals(node.getNamespaceURI())) {
                break;
            }
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static String getAttributeValue(Element element, String name) {
        Attr attr = element.getAttributeNode(name);
        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    public static String getTextContent(Node parent) {
        // Some DOMs do not support Node.getTextContent().
        Node textNode = getTextNode(parent);
        if (textNode != null) {
            return textNode.getNodeValue();
        }
        return null;
    }

    public static Node getTextNode(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null && node.getNodeType() != Node.TEXT_NODE) {
            node = node.getNextSibling();
        }
        return node;
    }

    public static boolean isServerElement(Element element) {
        return element.getNodeName().equals(Constants.SERVER_ELEMENT);
    }

    public static boolean isInclude(Element element) {
        return element.getNodeName().equals(Constants.INCLUDE_ELEMENT);
    }

    /**
     * Given a node, create an XPath expression that will navigate to that
     * node from the start of the document. Only handles attributes and
     * elements (no comment nodes, processing instruction nodes, text nodes).
     *
     * @param node The node for which to build the XPath expression.
     * @return The XPath expression that navigates to the node.
     */
    public static String createXPath(Node node) {
        if (node == null)
            return null;
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE: {
                return "";
            }
            case Node.ATTRIBUTE_NODE: {
                String xpath = createXPath(((Attr) node).getOwnerElement());
                return xpath + "/@" + node.getNodeName();
            }
            case Node.ELEMENT_NODE: {
                String xpath = createXPath(node.getParentNode());
                int count = 1;
                for (Node sibling = node.getPreviousSibling(); sibling != null; sibling = sibling.getPreviousSibling()) {
                    if (sibling.getNodeName().equals(node.getNodeName())) {
                        count++;
                    }
                }
                return xpath + "/" + node.getNodeName() + "[" + count + "]";
            }
        }
        return null;
    }

    static class SchemaNamespaceContext implements NamespaceContext {
        private static HashMap<String, String> namespaces = new HashMap<String, String>();
        static {
            namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
            namespaces.put("ext", "http://www.ibm.com/xmlns/dde/schema/annotation/ext");
        }

        @Override
        public String getNamespaceURI(String prefix) {
            return namespaces.get(prefix);
        }

        @Override
        public String getPrefix(String namespace) {
            Set<Entry<String, String>> entries = namespaces.entrySet();
            for (Entry<String, String> entry : entries) {
                if (entry.getValue().equals(namespace))
                    return entry.getKey();
            }
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespace) {
            ArrayList<String> prefixes = new ArrayList<String>();
            Set<Entry<String, String>> entries = namespaces.entrySet();
            for (Entry<String, String> entry : entries) {
                if (entry.getValue().equals(namespace))
                    prefixes.add(entry.getKey());
            }
            return prefixes.iterator();
        }
    }

    /**
     * Evaluate the XPath expression and return the resulting node. If
     * the expression evaluates to more than one node, the first node
     * in document order is returned.
     *
     * @param doc The DOM document.
     * @param xpathStr The XPath expression.
     * @return The first matching node in document order or null if there
     *         are no matching nodes.
     */
    public static Node getNode(Document doc, String xpathStr) {
        return getNode(doc, xpathStr, false);
    }

    /**
     * Evaluate the XPath expression and return the resulting node. If
     * the expression evaluates to more than one node, the first node
     * in document order is returned.
     *
     * @param doc The DOM document of the schema.
     * @param xpathStr The XPath expression.
     * @return The first matching node in document order or null if there
     *         are no matching nodes.
     */
    public static Node getSchemaNode(Document doc, String xpathStr) {
        return getNode(doc, xpathStr, true);
    }

    private static Node getNode(Document doc, String xpathStr, boolean isSchema) {
        if (xpathStr == null || xpathStr.isEmpty()) {
            return null;
        }
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            if (isSchema)
                xpath.setNamespaceContext(new SchemaNamespaceContext());
            XPathExpression xpathExpr = xpath.compile(xpathStr);
            Object result = xpathExpr.evaluate(doc, XPathConstants.NODE);
            return (Node) result;
        } catch (XPathExpressionException e) {
            Trace.logError("Failed to evaluate xpath expression: " + xpathStr, e);
            return null;
        }
    }

    /**
     * Get the most appropriate element for the given XPath expression.
     * If the result is an element then it is returned. If it is an
     * attribute, the parent element is returned. Otherwise the root
     * element is returned.
     *
     * @param doc The DOM document.
     * @param xpathStr The XPath expression.
     * @return The most appropriate element for the XPath expression.
     */
    public static Element getElement(Document doc, String xpathStr) {
        Node node = getNode(doc, xpathStr);
        if (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            } else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                return ((Attr) node).getOwnerElement();
            }
        }
        return doc.getDocumentElement();
    }

    /**
     * Get all available factory ids for the given element in the given document.
     * Searches includes as well as long as the uri and the context are not null.
     *
     * @param doc The DOM document.
     * @param uri The URI of the document. Used as the base URI for resolving includes.
     * @param context The resolution context to use for resolving includes.
     * @param elemName The name of the factory element from which to collect the ids.
     */
    public static String[] getIds(Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory context, String elemName) {
        Map<String, URILocation> idMap = new HashMap<String, URILocation>();
        addIds(idMap, doc, uri, serverInfo, context, elemName);
        return idMap.keySet().toArray(new String[idMap.size()]);
    }

    /**
     * Add all available factory elements to the map of Id -> URILocation.
     * Searches includes as well as long as the uri and the context are not null.
     *
     * @param idMap The map to update with the id/location pairs.
     * @param doc The DOM document.
     * @param uri The URI of the document. Used as the base URI for resolving includes.
     * @param context The resolution context to use for resolving includes.
     * @param elemName The name of the factory element for which to collect ids.
     */
    public static void addIds(Map<String, URILocation> idMap, Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory context, String elemName) {
        List<Element> elements = ConfigUtils.getResolvedElements(doc, uri, serverInfo, context, elemName, Constants.INSTANCE_ID, true);
        for (Element elem : elements) {
            String id = getAttributeValue(elem, Constants.INSTANCE_ID);
            if (id != null && !id.isEmpty()) {
                DocumentLocation location = (DocumentLocation) elem.getUserData(ConfigUtils.DOCUMENT_LOCATION_KEY);
                idMap.put(id, location);
            }
        }
    }

    /**
     * Get the full set of available factory ids grouped by element.
     * Searches includes as well as long as the uri and the context are not null.
     *
     * @param doc The DOM document.
     * @param uri The URI of the document. Used as the base URI for resolving includes.
     * @param context The resolution context to use for resolving includes.
     */
    public static Map<String, Set<String>> getAllIds(Document doc, URI uri, UserDirectory context) {
        final Map<String, Set<String>> ids = new HashMap<String, Set<String>>();
        final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();

        includeFilter.accept(uri);
        getIds(ids, doc, uri, context, includeFilter);
        return ids;
    }

    private static void getIds(Map<String, Set<String>> ids,
                               Document doc,
                               URI uri,
                               UserDirectory context,
                               IncludeFilter includeFilter) {

        ConfigurationFile configFile = ConfigUtils.getConfigFile(uri);
        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getDefaultDropins()) {
                getIds(ids, dropin.getDomDocument(), dropin.getURI(), context, includeFilter);
            }
        }

        for (Element elem = getFirstChildElement(doc.getDocumentElement()); elem != null; elem = getNextElement(elem)) {
            if (isInclude(elem) && context != null && uri != null) {
                String includePath = getAttributeValue(elem, Constants.LOCATION_ATTRIBUTE);
                if (includePath != null) {
                    URI includeURI = ConfigUtils.resolve(uri, includePath, context);
                    // process include if it's the first time we have seen it
                    if (includeURI != null && includeFilter.accept(includeURI)) {
                        Document include = ConfigUtils.getDOM(includeURI);
                        if (include != null) {
                            getIds(ids, include, includeURI, context, includeFilter);
                        }
                    }
                }
            } else if (elem.hasAttribute(Constants.INSTANCE_ID)) {
                String id = getAttributeValue(elem, Constants.INSTANCE_ID);
                if (id != null) {
                    Set<String> set = ids.get(elem.getNodeName());
                    if (set == null) {
                        set = new HashSet<String>();
                        ids.put(elem.getNodeName(), set);
                    }
                    set.add(id);
                }
            }
        }

        if (configFile != null) {
            for (ConfigurationFile dropin : configFile.getOverrideDropins()) {
                getIds(ids, dropin.getDomDocument(), dropin.getURI(), context, includeFilter);
            }
        }
    }

    /**
     * Get the full set of features. Searches includes as well as long as the uri and the context are not null.
     *
     * @param doc The DOM document
     * @param uri The URI of the document. Used as the base URI for resolving includes
     * @param context The resolution context to use for resolving includes
     */
    public static List<String> getAllFeatures(Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory context) {
        final List<String> list = new ArrayList<String>();
        List<Element> elements = ConfigUtils.getResolvedElements(doc, uri, serverInfo, context, Constants.FEATURE_MANAGER, null);
        for (Element elem : elements) {
            for (Element child = getFirstChildElement(elem, Constants.FEATURE); child != null; child = getNextElement(child, Constants.FEATURE)) {
                String name = DOMUtils.getTextContent(child);
                if (name != null && !name.isEmpty() && !list.contains(name))
                    list.add(name);
            }
        }
        return list;
    }

    /**
     * Get the full set of features mapped to the elements that define them. The elements will contain location
     * information.
     *
     * @param doc The DOM document
     * @param uri The URI of the document. Used as the base URI for resolving includes.
     * @param context The resolution context to use for resolving includes
     * @return
     */
    public static Map<String, Element> getFeatureElementMap(Document doc, URI uri, WebSphereServerInfo serverInfo, UserDirectory context) {
        final Map<String, Element> map = new HashMap<String, Element>();
        List<Element> elements = ConfigUtils.getResolvedElements(doc, uri, serverInfo, context, Constants.FEATURE_MANAGER, null, true);
        for (Element elem : elements) {
            for (Element child = getFirstChildElement(elem, Constants.FEATURE); child != null; child = getNextElement(child, Constants.FEATURE)) {
                String name = DOMUtils.getTextContent(child);
                if (name != null && !name.isEmpty() && !map.containsKey(name))
                    map.put(name, child);
            }
        }
        return map;
    }

    public static List<Element> getApplicationElements(Document doc) {
        ArrayList<Element> list = new ArrayList<Element>();
        for (Element appElem = getFirstChildElement(doc.getDocumentElement(), Constants.APPLICATION); appElem != null; appElem = getNextElement(appElem, Constants.APPLICATION)) {
            list.add(appElem);
        }
        return list;
    }

    /**
     * The temporary document is used for creating a merged view of an
     * element when necessary.
     */
    public static Document getTmpDoc() {
        Document tmpDoc = null;
        try {
            if (builder == null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                builder = factory.newDocumentBuilder();
            }
            tmpDoc = builder.newDocument();
        } catch (ParserConfigurationException e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Could not create a temporary document.", e);
            }
            throw new RuntimeException(e);
        }
        return tmpDoc;
    }
}
