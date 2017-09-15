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
package com.ibm.ws.st.ui.internal;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;

@SuppressWarnings("restriction")
public abstract class BaseHyperlinkDetector extends AbstractHyperlinkDetector {

    protected Node getCurrentNode(IDocument document, int offset) {
        // get the current node at the offset (returns either: element, doctype, text)
        IndexedRegion inode = null;
        IStructuredModel sModel = null;
        try {
            sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
            if (sModel != null) {
                inode = sModel.getIndexedRegion(offset);
                if (inode == null)
                    inode = sModel.getIndexedRegion(offset - 1);
            }
        } finally {
            if (sModel != null)
                sModel.releaseFromRead();
        }

        if (inode instanceof Node)
            return (Node) inode;

        return null;
    }

    protected Attr getCurrentAttr(Node node, IStructuredDocumentRegion docRegion, ITextRegion valueRegion) {
        Attr attr = null;
        switch (node.getNodeType()) {
            case Node.ATTRIBUTE_NODE:
                attr = (Attr) node;
                break;
            case Node.ELEMENT_NODE:
                if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(valueRegion.getType())) {
                    Element elem = (Element) node;
                    ITextRegionList textRegions = docRegion.getRegions();
                    int index = textRegions.indexOf(valueRegion) - 1;
                    while (index >= 0) {
                        ITextRegion textRegion = textRegions.get(index--);
                        if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(textRegion.getType())) {
                            String name = docRegion.getFullText(textRegion);
                            attr = elem.getAttributeNode(name);
                            break;
                        }
                    }
                }
                break;
            default:
                break;
        }
        return attr;
    }

    protected boolean isConfigDocument(Document dom) {
        // Checks if this is a server configuration file
        Element root = dom.getDocumentElement();
        if (root != null && Constants.SERVER_ELEMENT.equals(root.getNodeName()) &&
            (root.getNamespaceURI() == null || "".equals(root.getNamespaceURI()))) {
            return true;
        }
        return false;
    }

    protected CMNode getCMNode(Node node) {
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(node.getOwnerDocument());
        if (modelQuery != null) {
            switch (node.getNodeType()) {
                case Node.ATTRIBUTE_NODE: {
                    return modelQuery.getCMAttributeDeclaration((Attr) node);
                }
                case Node.ELEMENT_NODE: {
                    return modelQuery.getCMElementDeclaration((Element) node);
                }
                default:
                    // Do nothing
                    break;
            }
        }
        return null;
    }

}
