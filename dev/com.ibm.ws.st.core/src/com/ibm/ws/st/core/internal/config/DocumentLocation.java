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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Trace;

/**
 * Document location.
 * 
 * Currently only IDOMModel has line and offset information.
 */
@SuppressWarnings("restriction")
public class DocumentLocation extends URILocation {

    private final Type type;
    private final String xpath;
    private final int line;
    private final int column;
    private final int startOffset;
    private final int endOffset;

    public enum Type {
        SERVER_XML,
        BOOTSTRAP,
        SERVER_ENV
    }

    private DocumentLocation(URI uri, Type type, String xpath) {
        this(uri, type, xpath, -1, -1, -1, -1);
    }

    private DocumentLocation(URI uri, Type type, String xpath, int line, int column, int startOffset, int endOffset) {
        super(uri);
        this.type = type;
        this.xpath = xpath;
        this.line = line;
        this.column = column;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    /**
     * Create a document location given the document URI, which must
     * not be null.
     */
    public static DocumentLocation createDocumentLocation(URI uri, Type type) {
        return new DocumentLocation(uri, type, null);
    }

    /**
     * Create a document location given the document URI, which must
     * not be null.
     */
    public static DocumentLocation createDocumentLocation(URI uri, Type type, int lineNo) {
        return new DocumentLocation(uri, type, null, lineNo, -1, -1, -1);
    }

    /**
     * Create a document location given a URI and a node, which must not
     * be null.
     */
    public static DocumentLocation createDocumentLocation(URI uri, Node node) {
        if (node instanceof IDOMNode) {
            return createDocumentLocation(uri, (IDOMNode) node);
        }
        return new DocumentLocation(uri, Type.SERVER_XML, DOMUtils.createXPath(node));
    }

    /**
     * Create a document location given a node, which must not be null.
     */
    public static DocumentLocation createDocumentLocation(Node node) {
        return createDocumentLocation(null, node);
    }

    private static DocumentLocation createDocumentLocation(URI uri, IDOMNode node) {
        URI locationURI = uri;
        String xpath = DOMUtils.createXPath(node);
        int line = -1;
        int column = -1;

        IDOMDocument document = (IDOMDocument) node.getOwnerDocument();
        final int startOffset = node.getStartOffset();
        int endOffset = node.getEndOffset();
        if (document != null) {
            if (locationURI == null) {
                String uriData = (String) document.getUserData(ConfigurationFile.USER_DATA_URI);
                if (uriData != null) {
                    try {
                        locationURI = new URI(uriData);
                    } catch (URISyntaxException e) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Invalid uri in DOM user data: " + uriData, e);
                        }
                    }
                }
            }

            IStructuredDocument structuredDocument = document.getStructuredDocument();
            if (structuredDocument != null) {
                try {
                    int sdLine = structuredDocument.getLineOfOffset(startOffset);
                    if (sdLine >= 0) {
                        // The line number from the structured document is zero based so add one.
                        line = sdLine + 1;
                        int lineOffset = structuredDocument.getLineOffset(sdLine);
                        column = startOffset - lineOffset + 1;
                    }

                    // for attribute nodes, ignore any trailing whitespaces
                    // and update endOffset accordingly
                    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                        final int length = endOffset - startOffset;
                        final String value = structuredDocument.get(startOffset, length);
                        int c;
                        int i = length - 1;
                        for (; i >= 0; --i) {
                            c = value.charAt(i);
                            if (c != 0x20 && c != 0x09 && c != 0x0A && c != 0x0D) {
                                break;
                            }
                        }
                        endOffset -= length - i - 1;
                    }
                } catch (BadLocationException e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Could not calculate line or column number", e);
                    }
                }
            }
        }

        return new DocumentLocation(locationURI, Type.SERVER_XML, xpath, line, column, startOffset, endOffset);
    }

    /**
     * Get the file type. Mainly to differentiate between configuration files that conform
     * to the schema and other types of files (such as bootstrap.properties).
     */
    public Type getType() {
        return type;
    }

    /**
     * Specialized version of getType. Returns simply if this is a configuration file or not.
     */
    public boolean isConfigFile() {
        return type == Type.SERVER_XML;
    }

    /**
     * Return an IFile for this document. Returns null if no IFile is
     * available.
     */
    public IFile getFile() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspaceRoot.findFilesForLocationURI(getURI());
        if (files != null && files.length > 0 && files[0].exists()) {
            return files[0];
        }
        return null;
    }

    /**
     * Get the xpath that describes the node in the document. Returns null
     * if no xpath is available.
     */
    public String getXPath() {
        return xpath;
    }

    /**
     * Get the line number for this document location. Returns -1 if no line
     * number is available.
     */
    public int getLine() {
        return line;
    }

    /**
     * Get the column number for this document location. Returns -1 if no column
     * number is available.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get the start offset for this document location. Returns -1 if no start
     * offset is available.
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * Get the end offset for this document location. Returns -1 if no end
     * offset is available.
     */
    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public IResource getResource() {
        return getFile();
    }

}
