/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xsd.contentmodel.internal.XSDImpl;
import org.eclipse.xsd.XSDAnnotation;
import org.eclipse.xsd.XSDAttributeDeclaration;
import org.eclipse.xsd.XSDAttributeUse;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.XSDVariety;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.generation.SchemaMetadata;

/**
 *
 */
@SuppressWarnings("restriction")
public class SchemaUtil {
    static final String EXT_XSD = "http://www.ibm.com/xmlns/dde/schema/annotation/ext";
    static final String SCHEMA_XSD = "http://www.w3.org/2001/XMLSchema";

    /**
     * getAttributeDefault
     *
     * @param doc the DOM
     * @param tags array of element tags as strings in schema nesting order (closest encloser last)
     * @param attrName
     * @return the schema type of the attribute as a string
     */
    @SuppressWarnings("deprecation")
    static public String getAttributeDefault(Document doc, String[] tags, String attrName, URI uri) {
        CMAttributeDeclaration a = getAttribute(doc, tags, attrName, uri);
        return a != null ? a.getDefaultValue() : null;
    }

    /**
     * getAttribute
     *
     * @param doc the DOM
     * @param tags array of element tags as strings in schema nesting order (closest encloser last)
     * @param attrName
     * @return the attribute declaration in the schema
     */
    static public CMAttributeDeclaration getAttribute(Document doc, String[] tags, String attrName, URI uri) {
        CMElementDeclaration element = getElement(doc, tags, uri);
        if (element != null) {
            return getAttr(element, attrName);
        }
        return null;
    }

    static public CMAttributeDeclaration getAttribute(Document doc, String[] tags, String attrName, URL schemaURL) {
        CMElementDeclaration element = getElement(doc, tags, schemaURL);
        if (element != null) {
            return getAttr(element, attrName);
        }
        return null;
    }

    /**
     * getElement
     *
     * @param doc the DOM
     * @param tags array of element tags as strings in schema nesting order (closest encloser last)
     * @return the element declaration in the schema
     */
    static public CMElementDeclaration getElement(Document doc, String[] tags, URI uri) {
        CMDocument cmDoc = getSchema(doc, uri);
        return cmDoc != null ? getElement(cmDoc, tags) : null;
    }

    static public CMElementDeclaration getElement(Document doc, String[] tags, URL schemaURL) {
        CMDocument cmDoc = (schemaURL == null) ? null : getCMDocument(schemaURL, doc);
        return cmDoc != null ? getElement(cmDoc, tags) : null;
    }

    /**
     * getAttributeType
     *
     * @param doc the DOM
     * @param tags array of element tags as strings in schema nesting order (closest encloser last)
     * @param attrName the attribute name
     * @return the type of the element in the schema
     */
    static public CMDataType getAttributeType(Document doc, String[] tags, String attrName, URI uri) {
        CMAttributeDeclaration attr = getAttribute(doc, tags, attrName, uri);
        if (attr != null) {
            return attr.getAttrType();
        }
        return null;
    }

    /**
     * getElementType
     *
     * @param doc the DOM
     * @param tags array of element tags as strings in schema nesting order (closest encloser last)
     * @return the type of the element in the schema
     */
    static public CMDataType getElementType(Document doc, String[] tags, URI uri) {
        CMElementDeclaration element = getElement(doc, tags, uri);
        if (element != null) {
            CMDataType type = element.getDataType();
            return type;
        }
        return null;
    }

    /**
     * getAttributeType
     *
     * @param parent The parent element
     * @param name The attribute name
     * @return the type of the element in the schema
     */
    static public CMDataType getAttributeType(Element parent, String name, Document doc, URI uri) {
        String[] tags = getTags(parent);
        return getAttributeType(doc, tags, name, uri);
    }

    /**
     * getEnumeratedValues
     *
     * @param doc the DOM
     * @param tags array of element tags as strings in schema nesting order (closest encloser last)
     * @return the enumerated values for the element in the schema
     */
    static public String[] getEnumeratedValues(Document doc, String[] tags, URI uri) {
        CMDataType type = getElementType(doc, tags, uri);
        return type != null ? type.getEnumeratedValues() : null;
    }

    /**
     * findAttribute
     *
     * @param a the attribute
     * @return the attribute declaration in the schema
     */
    // TODO I am not convinced this works since it fails in my unit test but that could be the test
    static public CMAttributeDeclaration findAttribute(Attr a, URI uri) {
        Document doc = a.getOwnerDocument();
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(doc);
        return (modelQuery != null) ? modelQuery.getCMAttributeDeclaration(a) : null;
    }

    // TODO I am not convinced this works since it fails in my unit test but that could be the test
    static public CMElementDeclaration getElement(Element e) {
        final Document doc = e.getOwnerDocument();
        final ModelQuery modelQuery = ModelQueryUtil.getModelQuery(doc);
        return (modelQuery == null) ? null : modelQuery.getCMElementDeclaration(e);
    }

    static public CMAttributeDeclaration getAttribute(Element elem, String attrName) {
        CMElementDeclaration elemDecl = getElement(elem);
        return elemDecl == null ? null : getAttr(elemDecl, attrName);
    }

    static public String getLabel(CMNode node) {
        return getAppInfo(node, EXT_XSD, "label");
    }

    static public String getLabel(Document doc, String[] elemTags, URI uri) {
        CMElementDeclaration elemDecl = getElement(doc, elemTags, uri);
        if (elemDecl == null) {
            return null;
        }
        return getLabel(elemDecl);
    }

    static public String getLabel(Document doc, String[] elemTags, String attrName, URI uri) {
        CMAttributeDeclaration attrDecl = getAttribute(doc, elemTags, attrName, uri);
        if (attrDecl == null) {
            return null;
        }
        return getLabel(attrDecl);
    }

    /**
     * Get the references types supported by the given node.
     *
     * @return The reference types or an empty array if none.
     */
    static public String[] getReferences(CMNode node) {
        return getAppInfo(node, EXT_XSD, "reference", false);
    }

    /**
     * Get the info for the given extension name.
     *
     * @return The extension info or null if none
     */
    static public String getExtInfo(CMNode node, String extName) {
        return getAppInfo(node, EXT_XSD, extName);
    }

    /**
     * Determine if the given element has extra properties defined.
     */
    public static boolean hasExtraProperties(CMNode node) {
        Element[] elems = getAppInfoElements(node, EXT_XSD, "extraProperties", true);
        return elems.length > 0;
    }

    static String getAppInfo(CMNode node, String ns, String tag) {
        String[] result = getAppInfo(node, ns, tag, true);
        if (result.length == 0)
            return null;
        return result[0];
    }

    private static String[] getAppInfo(CMNode node, String ns, String tag, boolean firstOnly) {
        final String APPINFO = "appinfo";
        XSDAnnotation annotation = getAnnotation(node);
        List<String> resultList = new ArrayList<String>();
        if (annotation != null) {
            Element e = annotation.getElement();
            for (Element e2 = DOMUtils.getFirstChildElement(e, SCHEMA_XSD, APPINFO); e2 != null; e2 = DOMUtils.getNextElement(e2, SCHEMA_XSD, APPINFO)) {
                for (Element e3 = DOMUtils.getFirstChildElement(e2, ns, tag); e3 != null; e3 = DOMUtils.getNextElement(e3, ns, tag)) {
                    String s = DOMUtils.getTextContent(e3);
                    if (s != null) {
                        if (firstOnly)
                            return new String[] { s.trim() };
                        resultList.add(s.trim());
                    }
                }
            }
        }
        return resultList.toArray(new String[resultList.size()]);
    }

    private static Element[] getAppInfoElements(CMNode node, String ns, String tag, boolean firstOnly) {
        final String APPINFO = "appinfo";
        XSDAnnotation annotation = getAnnotation(node);
        List<Element> resultList = new ArrayList<Element>();
        if (annotation != null) {
            Element e = annotation.getElement();
            for (Element e2 = DOMUtils.getFirstChildElement(e, SCHEMA_XSD, APPINFO); e2 != null; e2 = DOMUtils.getNextElement(e2, SCHEMA_XSD, APPINFO)) {
                for (Element e3 = DOMUtils.getFirstChildElement(e2, ns, tag); e3 != null; e3 = DOMUtils.getNextElement(e3, ns, tag)) {
                    if (firstOnly)
                        return new Element[] { e3 };
                    resultList.add(e3);
                }
            }
        }
        return resultList.toArray(new Element[resultList.size()]);
    }

    public static String getDocumentation(CMNode node) {
        XSDAnnotation annotation = getAnnotation(node);
        return getDocumentation(annotation);
    }

    public static String getDocumentation(XSDAnnotation annotation) {
        final String DOCUMENTATION = "documentation";
        if (annotation != null) {
            Element e = annotation.getElement();
            Element documentation = DOMUtils.getFirstChildElement(e, SCHEMA_XSD, DOCUMENTATION);
            if (documentation != null) {
                String s = DOMUtils.getTextContent(documentation);
                if (s != null) {
                    return s.trim();
                }
            }
        }
        return null;
    }

    private static XSDAnnotation getAnnotation(CMNode node) {
        XSDAnnotation annotation = null;
        if (node instanceof XSDImpl.XSDElementDeclarationAdapter) {
            XSDImpl.XSDElementDeclarationAdapter adapter = (XSDImpl.XSDElementDeclarationAdapter) node;
            Notifier target = adapter.getTarget();
            if (target instanceof XSDElementDeclaration) {
                XSDElementDeclaration edecl = (XSDElementDeclaration) target;
                // If the element has an annotation use it, otherwise try the type
                annotation = edecl.getAnnotation();
                if (annotation == null) {
                    XSDTypeDefinition type = edecl.getTypeDefinition();
                    annotation = type.getAnnotation();
                    while (annotation == null && type.getBaseType() != null && !type.isCircular()) {
                        type = type.getBaseType();
                        annotation = type.getAnnotation();
                    }
                }
            }
        } else if (node instanceof XSDImpl.XSDAttributeUseAdapter) {
            XSDImpl.XSDAttributeUseAdapter adapter = (XSDImpl.XSDAttributeUseAdapter) node;
            Notifier target = adapter.getTarget();
            if (target instanceof XSDAttributeUse) {
                XSDAttributeUse use = (XSDAttributeUse) target;
                XSDAttributeDeclaration attr = use.getAttributeDeclaration();
                annotation = attr.getAnnotation();
            }
        }
        return annotation;
    }

    static CMElementDeclaration getElement(CMDocument cmDoc, String[] tags) {
        CMNamedNodeMap map = cmDoc.getElements();
        CMElementDeclaration element = (CMElementDeclaration) map.getNamedItem(tags[0]);
        for (int i = 1; element != null && i < tags.length; ++i) {
            element = getElement(element, tags[i]);
        }
        return element;
    }

    static public CMElementDeclaration getElement(CMElementDeclaration element, String tag) {
        CMNamedNodeMap map = element.getLocalElements();
        return map != null ? (CMElementDeclaration) map.getNamedItem(tag) : null;
    }

    static public CMAttributeDeclaration getAttr(CMElementDeclaration element, String name) {
        CMNamedNodeMap attrs = element.getAttributes();
        return (CMAttributeDeclaration) attrs.getNamedItem(name);
    }

    static boolean hasAttr(Element element, String attrName, URI uri) {
        String[] tags = getTags(element);
        CMElementDeclaration elemDecl = getElement(element.getOwnerDocument(), tags, uri);
        if (elemDecl != null) {
            CMNamedNodeMap attrs = elemDecl.getAttributes();
            if (attrs.getNamedItem(attrName) != null) {
                return true;
            }
        }
        return false;
    }

    static public String[] getTags(Element element) {
        ArrayList<String> tagList = new ArrayList<String>();
        tagList.add(element.getNodeName());
        Node parent = element.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            tagList.add(parent.getNodeName());
            parent = parent.getParentNode();
        }

        // Copy to an array, reversing the order.
        int len = tagList.size();
        String[] tags = new String[len];
        for (int i = 0; i < tagList.size(); i++) {
            tags[i] = tagList.get(len - i - 1);
        }
        return tags;
    }

    static CMDocument getSchema(Document dom, URI uri) {
        CMDocument cmDoc = null;
        URL grammarURL = getSchemaURL(uri);
        if (grammarURL != null) {
            cmDoc = getCMDocument(grammarURL, dom);
        }
        return cmDoc;
    }

    public static URL getSchemaURLNoFallback(URI uri) {
        WebSphereRuntime[] runtimes = WebSphereUtil.getWebSphereRuntimes();
        URL grammarURL = null;
        if (runtimes != null) {
            for (WebSphereRuntime runtime : runtimes) {
                List<WebSphereServerInfo> serverInfos = runtime.getWebSphereServerInfos();
                for (int i = 0; i < serverInfos.size() && grammarURL == null; i++) {
                    grammarURL = serverInfos.get(i).getConfigurationSchemaURL(uri);
                }
                if (grammarURL == null) {
                    grammarURL = runtime.getConfigurationSchemaURL(uri);
                }
            }
        }

        // See if the file is in a project with a targeted runtime
        if (grammarURL == null) {
            try {
                WebSphereRuntime wrt = WebSphereUtil.getTargetedRuntime(uri);
                if (wrt != null) {
                    grammarURL = wrt.getConfigurationSchemaURL();
                }
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.logError("Failed to get schema from targeted runtime for file " + uri.toString(), e);
                }
            }
        }
        return grammarURL;
    }

    public static URL getSchemaURL(URI uri) {
        URL grammarURL = getSchemaURLNoFallback(uri);
        if (grammarURL == null) {
            grammarURL = SchemaMetadata.getFallbackSchema();
        }
        return grammarURL;
    }

    private static CMDocument getCMDocument(URL grammarURL, Document dom) {
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(dom);
        if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
            final CMDocumentManager manager = modelQuery.getCMDocumentManager();

            CMDocumentCache cache = manager.getCMDocumentCache();
            if (cache != null) {
                String s = grammarURL.toString();
                CMDocument cmDoc = cache.getCMDocument(s);
                // The cache is not populated, so let's ask the model for
                // the node of the root element -- update the cache
                if (cmDoc == null && dom != null && dom.getDocumentElement() != null) {
                    CMNode cmNode = modelQuery.getCMNode(dom.getDocumentElement());
                    // Should update the cache
                    if (cmNode != null) {
//                      cmDoc = (CMDocument) cmNode.getProperty("CMDocument"); //$NON-NLS-1$
                        cmDoc = modelQuery.getCorrespondingCMDocument(dom.getDocumentElement());
                        cache.putCMDocument(s, cmDoc);
                    }
                }
                if (cmDoc != null) {
                    return cmDoc;
                }
            }
        }
        if (Trace.ENABLED)
            Trace.trace(Trace.WARNING, "The CMDocument could not be found for: " + grammarURL);
        return null;
    }

    /**
     * Get the type definition for the given node.
     */
    public static XSDTypeDefinition getTypeDefinitionFromSchema(CMNode node) {
        XSDTypeDefinition type = null;
        if (node instanceof XSDImpl.XSDElementDeclarationAdapter) {
            XSDImpl.XSDElementDeclarationAdapter adapter = (XSDImpl.XSDElementDeclarationAdapter) node;
            Notifier target = adapter.getTarget();
            if (target instanceof XSDElementDeclaration) {
                XSDElementDeclaration edecl = (XSDElementDeclaration) target;
                type = edecl.getTypeDefinition();
            }
        } else if (node instanceof XSDImpl.XSDAttributeUseAdapter) {
            XSDImpl.XSDAttributeUseAdapter adapter = (XSDImpl.XSDAttributeUseAdapter) node;
            Notifier target = adapter.getTarget();
            if (target instanceof XSDAttributeUse) {
                XSDAttributeUse use = (XSDAttributeUse) target;
                XSDAttributeDeclaration attr = use.getAttributeDeclaration();
                type = attr.getTypeDefinition();
            }
            if (target instanceof XSDAttributeDeclaration) {
                XSDAttributeDeclaration attr = (XSDAttributeDeclaration) target;
                type = attr.getTypeDefinition();
            }
        }
        return type;
    }

    /**
     * If this node's type is an anonymous union, get the member types.
     */
    public static List<XSDSimpleTypeDefinition> getMemberTypesFromUnion(CMNode node) {
        XSDTypeDefinition type = getTypeDefinitionFromSchema(node);
        // Only look at anonymous simple types
        if (type instanceof XSDSimpleTypeDefinition && type.getName() == null) {
            XSDSimpleTypeDefinition st = (XSDSimpleTypeDefinition) type;
            if (st.getVariety().equals(XSDVariety.UNION_LITERAL)) {
                return st.getMemberTypeDefinitions();
            }
        }
        return null;
    }

}
