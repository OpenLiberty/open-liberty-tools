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
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.xsd.XSDEnumerationFacet;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.LocalConfigVars;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.config.URILocation;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.xwt.dde.customization.ICustomIconObject;
import com.ibm.xwt.dde.internal.customization.CustomizationManager;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.customization.DetailItemCustomization;

/**
 * General UI related configuration utilities.
 */
@SuppressWarnings("restriction")
public class ConfigUIUtils {

    protected static final String INDENT_STRING = "    ";

    /**
     * Try to locate the current active editor and get its IEditorInput.
     */
    public static IEditorInput getActiveEditorInput() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null)
            return null;
        IWorkbenchPage page = window.getActivePage();
        if (page == null)
            return null;
        IWorkbenchPart part = page.getActivePart();
        if (part == null || !(part instanceof IEditorPart))
            return null;

        IEditorPart editorPart = (IEditorPart) part;
        IEditorInput editorInput = editorPart.getEditorInput();

        return editorInput;
    }

    /**
     * Get the URI given an editor input.
     */
    public static URI getURI(IEditorInput editorInput) {
        if (editorInput instanceof IPathEditorInput) {
            IPath path = ((IPathEditorInput) editorInput).getPath();
            return path.toFile().toURI();
        } else if (editorInput instanceof IURIEditorInput) {
            return ((IURIEditorInput) editorInput).getURI();
        }
        return null;
    }

    /**
     * Get the URI given an editor input and a document.
     */
    public static URI getURI(IEditorInput editorInput, Document doc) {
        URI uri = getURI(editorInput);
        if (uri == null) {
            String uriStr = null;
            try {
                uriStr = doc.getDocumentURI();
                if (uriStr != null) {
                    uri = new URI(uriStr);
                }
            } catch (UnsupportedOperationException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to get document URI", e);
            } catch (URISyntaxException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to creat URI from string: " + uriStr, e);
            }
        }
        return uri;
    }

    /**
     * Get the IFile given an editor input.
     *
     * @return The IFile or null if none.
     */
    public static IFile getFile(IEditorInput editorInput) {
        if (editorInput instanceof FileEditorInput) {
            return ((FileEditorInput) editorInput).getFile();
        }
        return null;
    }

    /**
     * Get enabled features for the given editor input and DOM
     */
    public static List<String> getFeatures(IEditorInput editorInput, Document document) {
        URI uri = null;
        WebSphereServerInfo serverInfo = null;
        UserDirectory userDir = null;
        if (editorInput != null) {
            uri = ConfigUIUtils.getURI(editorInput);
            if (uri != null) {
                serverInfo = ConfigUtils.getServer(uri);
                if (serverInfo != null) {
                    userDir = serverInfo.getUserDirectory();
                } else {
                    userDir = ConfigUtils.getUserDirectory(uri);
                }
            }
        }
        if (document != null) {
            List<String> features = DOMUtils.getAllFeatures(document, uri, serverInfo, userDir);
            return features;
        }
        return null;
    }

    /**
     * Get global configuration variables for the given editor input and DOM
     */
    public static ConfigVars getConfigVars(IEditorInput editorInput, Document document) {
        ConfigVars configVars = new ConfigVars();
        URI uri = null;
        ConfigurationFile configFile = null;
        WebSphereServerInfo server = null;
        UserDirectory userDir = null;
        if (editorInput != null) {
            uri = ConfigUIUtils.getURI(editorInput);
            if (uri != null) {
                server = ConfigUtils.getServerInfo(uri);
                if (server != null) {
                    server.getVariables(configVars, true);
                    configFile = server.getConfigurationFileFromURI(uri);
                    userDir = server.getUserDirectory();
                } else {
                    // If no server, see if the file is in a user directory
                    userDir = ConfigUtils.getUserDirectory(uri);
                    if (userDir != null) {
                        userDir.getVariables(configVars, true);
                    }
                }
            }
        }
        if (document != null) {
            ConfigUtils.getVariables(configFile, document, uri, server, userDir, configVars);
        }
        return configVars;
    }

    /**
     * Get the full set of configuration variables for the given element including
     * implicit local variables. If the element is null then just the predefined
     * variables will be retrieved.
     * If attrExclude is not null then it will not be included in the variable list.
     */
    public static ConfigVars getConfigVars(IEditorInput editorInput, Element elem, String attrExclude) {
        Document document = elem == null ? null : elem.getOwnerDocument();
        ConfigVars vars = getConfigVars(editorInput, document);
        vars = getConfigVars(editorInput, elem, attrExclude, vars);
        return vars;
    }

    /**
     * Get the full set of configuration variables given the global variables
     * (adds in the implicit local variables)
     */
    public static ConfigVars getConfigVars(IEditorInput editorInput, Element elem, String attrExclude, ConfigVars globalVars) {
        if (elem != null) {
            URI uri = editorInput == null ? null : ConfigUIUtils.getURI(editorInput);
            ConfigVars localVars = new LocalConfigVars(globalVars);
            ConfigUtils.getLocalVariables(elem, attrExclude, uri, localVars);
            return localVars;
        }
        return globalVars;
    }

    /**
     * Get sorted subset of candidates that start with the given match string.
     *
     * @param match The match string.
     * @param candidates The set of candidate names.
     * @return The candidates that start with the match string.
     */
    public static List<String> getSortedMatches(String match, List<String> candidates) {
        List<String> matches = getMatches(match, candidates);
        Collections.sort(matches);
        return matches;
    }

    /**
     * Get subset of candidates that start with the given match string.
     *
     * @param match The match string.
     * @param candidates The set of candidate names.
     * @return The candidates that start with the match string.
     */
    public static List<String> getMatches(String match, List<String> candidates) {
        List<String> matches = new ArrayList<String>();
        for (String candidate : candidates) {
            if (match.isEmpty()) {
                matches.add(candidate);
            } else if (candidate.length() >= match.length()) {
                String startString = candidate.substring(0, match.length());
                if (match.equalsIgnoreCase(startString)) {
                    matches.add(candidate);
                }
            }
        }
        return matches;
    }

    /**
     * Get the runtime for the given editor part.
     */
    public static WebSphereRuntime getRuntime(IEditorPart editorPart) {
        if (editorPart != null) {
            IEditorInput editorInput = editorPart.getEditorInput();
            if (editorInput != null) {
                URI uri = getURI(editorInput);
                if (uri != null) {
                    UserDirectory userDir = ConfigUtils.getUserDirectory(uri);
                    if (userDir != null) {
                        return userDir.getWebSphereRuntime();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find the item for the current selection in a list of comma/space separated
     * items.
     */
    public static String getListItem(String text, int startOffset, int endOffset) {
        int start = 0;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            switch (c) {
                case ',':
                case ' ':
                    if (index < startOffset) {
                        start = index + 1;
                    } else {
                        if (endOffset > index) {
                            // Selection is across multiple list items so return null
                            return null;
                        }
                        return text.substring(start, index);
                    }
                    break;
                default:
                    break;
            }
        }
        String str = text.substring(start, text.length());
        return str;
    }

    public static Map<String, URILocation> getIdMap(Document doc, URI docURI, WebSphereServerInfo server, UserDirectory userDir, String reference) {
        if (reference != null) {
            Map<String, URILocation> idMap = new HashMap<String, URILocation>();
            DOMUtils.addIds(idMap, doc, docURI, server, userDir, reference);
            if (Constants.LIBRARY.equals(reference)) {
                ConfigUtils.addDropInLibIds(idMap, server, userDir);
            }
            return idMap;
        }
        return Collections.emptyMap();
    }

    public static Map<String, URILocation> getIdMap(Document doc, URI docURI, WebSphereServerInfo server, UserDirectory userDir, String[] references) {
        if (references != null) {
            Map<String, URILocation> idMap = new HashMap<String, URILocation>();
            for (String reference : references) {
                DOMUtils.addIds(idMap, doc, docURI, server, userDir, reference);
                if (Constants.LIBRARY.equals(reference)) {
                    ConfigUtils.addDropInLibIds(idMap, server, userDir);
                }
            }
            return idMap;
        }
        return Collections.emptyMap();
    }

    public static Customization getCustomization() {
        return CustomizationManager.getInstance().getCustomization(Activator.CONFIGURATION_EDITOR);
    }

    public static String getTreeLabel(Element element, boolean isCreation) {
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
        if (modelQuery != null) {
            CMElementDeclaration elemDecl = modelQuery.getCMElementDeclaration(element);
            if (elemDecl != null) {
                String path = getElementFullPath(element);
                return getTreeLabel(elemDecl, path, getCustomization(), false);
            }
        }
        return element.getNodeName();
    }

    public static String getTreeLabel(CMElementDeclaration node, String path, Customization customization, boolean isCreation) {
        DetailItemCustomization nodeCustomization = customization == null ? null : customization.getItemCustomization(getNamespaceURI(node), path);
        String label = null;
        if (nodeCustomization != null) {
            if (isCreation)
                label = nodeCustomization.getCreationLabel();
            if (label == null || label.isEmpty())
                label = nodeCustomization.getLabel();
        }
        if (label == null || label.isEmpty())
            label = SchemaUtil.getLabel(node);
        if (label == null || label.isEmpty())
            label = node.getElementName();
        return label;
    }

    public static Image getTreeIcon(Element element) {
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
        if (modelQuery != null) {
            CMElementDeclaration elemDecl = modelQuery.getCMElementDeclaration(element);
            if (elemDecl != null) {
                String path = getElementFullPath(element);
                return getTreeIcon(elemDecl, path, getCustomization());
            }
        }
        return Activator.getImage(Activator.IMG_CONFIG_ELEMENT);
    }

    public static Image getTreeIcon(CMElementDeclaration elemDecl, String path, Customization customization) {
        if (customization == null)
            return Activator.getImage(Activator.IMG_CONFIG_ELEMENT);

        DetailItemCustomization nodeCustomization = customization.getItemCustomization(ConfigUIUtils.getNamespaceURI(elemDecl), path);
        if (nodeCustomization != null) {
            Image image = nodeCustomization.getIcon();
            if (image != null)
                return image;
            if (nodeCustomization.getIconClass() != null) {
                try {
                    Object iconClass = nodeCustomization.getIconClass().newInstance();
                    if (iconClass instanceof ICustomIconObject) {
                        ICustomIconObject customIconObject = (ICustomIconObject) iconClass;
                        image = customIconObject.getIcon(elemDecl, null);
                        if (image != null)
                            return image;
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to instantiate custom icon class", e);
                }
            }
        }
        if (customization.getIconClass() != null) {
            try {
                Object iconClass = customization.getIconClass().newInstance();
                if (iconClass instanceof ICustomIconObject) {
                    ICustomIconObject customIconObject = (ICustomIconObject) iconClass;
                    Image image = customIconObject.getIcon(elemDecl, null);
                    if (image != null)
                        return image;
                }
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to instantiate top level custom icon class", e);
            }
        }
        return Activator.getImage(Activator.IMG_CONFIG_ELEMENT);
    }

    public static String getNamespaceURI(CMNode cmNode) {
        CMDocument cmDocument = (CMDocument) cmNode.getProperty("CMDocument"); //$NON-NLS-1$
        if (cmDocument != null) {
            return (String) cmDocument.getProperty("http://org.eclipse.wst/cm/properties/targetNamespaceURI"); //$NON-NLS-1$
        }
        return null;
    }

    public static String getElementFullPath(Element element) {
        String path = element.getLocalName();
        for (Node parentNode = element.getParentNode(); parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE; parentNode = parentNode.getParentNode()) {
            path = parentNode.getLocalName() + '/' + path;
        }
        return '/' + path;
    }

    public static Element addNode(String name, Element parent) {
        // Add a node to the document.  Add text nodes as required to fix up the
        // alignment.
        Document doc = parent.getOwnerDocument();
        int nestedLevel = getNestedLevel(parent);
        Element elem = doc.createElement(name);
        if (parent.hasChildNodes()) {
            parent.appendChild(doc.createTextNode(INDENT_STRING));
        } else {
            parent.appendChild(doc.createTextNode("\n" + getIndent(nestedLevel + 1)));
        }
        parent.appendChild(elem);
        parent.appendChild(doc.createTextNode("\n" + getIndent(nestedLevel)));
        return elem;
    }

    public static void removeNode(Element node) {
        // Remove a node from the document.  Remove text nodes as required to fix up
        // the alignment.
        Node parent = node.getParentNode();
        Node beforeText = node.getPreviousSibling();
        while (beforeText != null && beforeText.getNodeType() == Node.TEXT_NODE) {
            Node tmpText = beforeText.getPreviousSibling();
            parent.removeChild(beforeText);
            beforeText = tmpText;
        }
        if (!parent.hasChildNodes()) {
            Node afterText = node.getNextSibling();
            while (afterText != null && afterText.getNodeType() == Node.TEXT_NODE) {
                Node tmpText = afterText.getNextSibling();
                parent.removeChild(afterText);
                afterText = tmpText;
            }
        }
        parent.removeChild(node);
    }

    private static int getNestedLevel(Element elem) {
        int i = 0;
        Node parent = elem.getParentNode();
        while (parent != null && parent.getNodeType() != Node.DOCUMENT_NODE) {
            i++;
            parent = parent.getParentNode();
        }
        return i;
    }

    private static String getIndent(int indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            builder.append(INDENT_STRING);
        }
        return builder.toString();
    }

    // If the type is an enumeration, get a map of the enumerated value to the
    // enumeration facet.
    public static Map<String, XSDEnumerationFacet> getEnumerationMap(XSDSimpleTypeDefinition type) {
        if (type == null)
            return null;
        List<XSDEnumerationFacet> facets = type.getEnumerationFacets();
        Map<String, XSDEnumerationFacet> map = new LinkedHashMap<String, XSDEnumerationFacet>(facets.size());
        for (XSDEnumerationFacet facet : facets) {
            map.put(facet.getLexicalValue(), facet);
        }
        return map;
    }

    // Get the element declaration given the element.
    public static CMElementDeclaration getElementDecl(Element elem) {
        CMElementDeclaration elemDecl = null;
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(elem.getOwnerDocument());
        if (modelQuery != null) {
            elemDecl = modelQuery.getCMElementDeclaration(elem);
        }
        if (Trace.ENABLED && elemDecl == null) {
            if (modelQuery == null) {
                Trace.trace(Trace.WARNING, "Could not get the model based on the element passed to the custom control object by the editor: " + elem.getNodeName());
            } else {
                Trace.trace(Trace.WARNING, "Could not get the element declaration based on the element passed to the custom control object by the editor: " + elem.getNodeName());
            }
        }
        return elemDecl;
    }

}
