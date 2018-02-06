/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.config.URILocation;
import com.ibm.ws.st.ui.internal.config.ConfigUIUtils;

@SuppressWarnings("restriction")
public class ConfigHyperlinkDetector extends BaseHyperlinkDetector {
    public ConfigHyperlinkDetector() {
        // do nothing
    }

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        IDocument document = textViewer.getDocument();
        if (document == null)
            return null;

        Node node = getCurrentNode(document, region.getOffset());
        if (node == null)
            return null;

        Document dom = node.getOwnerDocument();
        if (dom == null || !isConfigDocument(dom))
            return null;

        // Look for include link first
        IHyperlink[] includeHyperlink = getIncludeHyperlink(node, region);
        if (includeHyperlink != null) {
            return includeHyperlink;
        }

        IStructuredDocumentRegion sdRegion = getStructuredDocumentRegion(document, region.getOffset());
        if (sdRegion == null) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "VariableHyperlinkDetector: could not identify the IStructuredDocumentRegion");
            }
            return null;
        }

        // Get the current attribute value or element content
        ITextRegion textRegion = sdRegion.getRegionAtCharacterOffset(region.getOffset());
        if (textRegion == null || (!DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(textRegion.getType()) && !DOMRegionContext.XML_CONTENT.equals(textRegion.getType())))
            return null;

        // Get the text and offsets
        String textString = sdRegion.getFullText(textRegion);
        if (textString == null || textString.isEmpty()) {
            return null;
        }
        int startOffset = region.getOffset() - sdRegion.getStartOffset(textRegion);
        int endOffset = startOffset + (region.getLength() == 0 ? 0 : region.getLength() - 1);
        if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(textRegion.getType())) {
            // If this is not the last attribute on the element then any spaces
            // between the two attributes will be included in the text so the
            // text needs to be trimmed.
            textString = textString.trim();

            // Trim any quotes
            if (textString.charAt(0) == '"') {
                textString = textString.substring(1);
                startOffset = startOffset > 0 ? startOffset - 1 : startOffset;
                endOffset = endOffset > 0 ? endOffset - 1 : endOffset;
            }
            if (textString.charAt(textString.length() - 1) == '"') {
                textString = textString.substring(0, textString.length() - 1);
                startOffset = startOffset == textString.length() ? startOffset - 1 : startOffset;
                endOffset = endOffset == textString.length() ? endOffset - 1 : endOffset;
            }
        }

        Attr attr = getCurrentAttr(node, sdRegion, textRegion);

        // Get any variable hyperlinks
        IHyperlink[] varHyperlink = getVariableHyperlink(textString, startOffset, endOffset, region, dom, node, attr);
        if (varHyperlink != null) {
            return varHyperlink;
        }

        if (attr != null) {
            node = attr;
        }

        CMNode cmNode = getCMNode(node);
        if (cmNode == null) {
            return null;
        }

        String type = ConfigUtils.getTypeName(cmNode);
        if (type == null) {
            return null;
        }

        // Get other hyperlinks as appropriate
        if (ConfigVars.getTypeSet(type) == ConfigVars.REFERENCE_TYPES) {
            String[] references = SchemaUtil.getReferences(cmNode);
            for (String reference : references) {
                IHyperlink[] idHyperlink = getFactoryIdHyperlink(textString, startOffset, endOffset, reference, region, dom);
                if (idHyperlink != null) {
                    return idHyperlink;
                }
            }
        }

        return null;
    }

    private IHyperlink[] getIncludeHyperlink(Node node, IRegion region) {
        final IEditorInput editorInput = ConfigUIUtils.getActiveEditorInput();
        if (editorInput == null) {
            return null;
        }

        final URI baseURI = ConfigUIUtils.getURI(editorInput);
        if (baseURI == null) {
            return null;
        }

        if (node.getNodeType() == Node.ELEMENT_NODE && Constants.INCLUDE_ELEMENT.equals(node.getNodeName())) {
            String include = ((Element) node).getAttribute(Constants.LOCATION_ATTRIBUTE);
            if (include != null && !include.trim().isEmpty()) {
                URI uri = null;
                WebSphereServerInfo serverInfo = ConfigUtils.getServerInfo(baseURI);

                // We must check the Custom Runtime Provider extensions FIRST because they can override
                // the location of the include file.   eg. location="fileA" may not necessarily mean that fileA is in the
                // current directory as the server.xml.   fileA could be located anywhere in the project.
                if (editorInput instanceof IFileEditorInput) {
                    IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
                    IFile file = fileEditorInput.getFile();
                    IFolder mappedConfigFolder = ConfigUtils.getMappedConfigFolder(file);
                    if (mappedConfigFolder != null) {
                        IResource includeFile = mappedConfigFolder.findMember(include);
                        uri = includeFile.getLocationURI();
                        if (uri != null) {
                            return new IHyperlink[] { new ConfigHyperlink(region, DocumentLocation.createDocumentLocation(uri, DocumentLocation.Type.SERVER_XML), include) };
                        }
                    }
                }

                // Regular behavior

                if (serverInfo != null) {
                    uri = serverInfo.resolve(baseURI, include);
                } else {
                    UserDirectory userDir = ConfigUtils.getUserDirectory(baseURI);
                    if (userDir != null) {
                        uri = userDir.resolve(baseURI, include);
                    }
                }
                if (uri != null) {
                    return new IHyperlink[] { new ConfigHyperlink(region, DocumentLocation.createDocumentLocation(uri, DocumentLocation.Type.SERVER_XML), include) };
                }
            }
        }
        return null;
    }

    private IHyperlink[] getVariableHyperlink(String textString, int startOffset, int endOffset, IRegion region, Document dom, Node node, Attr attr) {
        // See if the current region is within a variable reference
        String varName = ConfigVarsUtils.getVariableName(textString, startOffset, endOffset);
        if (varName == null || varName.isEmpty())
            return null;

        ConfigVars configVars;
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            configVars = ConfigUIUtils.getConfigVars(ConfigUIUtils.getActiveEditorInput(), (Element) node, attr == null ? null : attr.getName());
        } else {
            configVars = ConfigUIUtils.getConfigVars(ConfigUIUtils.getActiveEditorInput(), dom);
        }

        DocumentLocation location = configVars.getDocumentLocation(varName);
        if (location != null && location.getURI() != null) {
            return new IHyperlink[] { new ConfigHyperlink(region, location, varName) };
        }

        return null;
    }

    private IHyperlink[] getFactoryIdHyperlink(String text, int startOffset, int endOffset, String reference, IRegion region, Document dom) {
        URI uri = null;
        WebSphereServerInfo serverInfo = null;
        UserDirectory userDir = null;
        IEditorInput editorInput = ConfigUIUtils.getActiveEditorInput();
        if (editorInput != null) {
            uri = ConfigUIUtils.getURI(editorInput);
            if (uri != null) {
                serverInfo = ConfigUtils.getServerInfo(uri);
                userDir = serverInfo != null ? serverInfo.getUserDirectory() : ConfigUtils.getUserDirectory(uri);
            }
        }

        // Get the set of ids for this reference
        Map<String, URILocation> idMap = ConfigUIUtils.getIdMap(dom, uri, serverInfo, userDir, reference);
        if (idMap.size() == 0) {
            return null;
        }

        String str = ConfigUIUtils.getListItem(text, startOffset, endOffset);
        if (str == null || str.isEmpty()) {
            return null;
        }

        URILocation location = idMap.get(str);
        if (location != null && location.getURI() != null) {
            return new IHyperlink[] { new ConfigHyperlink(region, location, str) };
        }

        return null;
    }

    private IStructuredDocumentRegion getStructuredDocumentRegion(IDocument document, int offset) {
        if (document instanceof IStructuredDocument) {
            IStructuredDocument doc = (IStructuredDocument) document;
            return doc.getRegionAtCharacterOffset(offset);
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.WARNING, "VariableHyperlinkDetector: document is not an IStructuredDocument");
        }
        return null;
    }
}
