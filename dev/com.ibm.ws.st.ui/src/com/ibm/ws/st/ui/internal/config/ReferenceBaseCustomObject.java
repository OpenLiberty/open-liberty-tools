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
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;

/**
 * Base abstract class for reference custom controls.
 */
@SuppressWarnings("restriction")
public abstract class ReferenceBaseCustomObject extends BaseCustomObject {

    protected static final String INDENT_STRING = "    ";

    protected String[] getReferences(CMAttributeDeclaration attrDecl) {
        if (attrDecl != null) {
            return SchemaUtil.getReferences(attrDecl);
        }
        return null;
    }

    // Workaround for the case where there is more than one nested child with
    // the same reference type.  For example privateLibrary and commonLibrary
    // in the classloader element both have 'library' as their reference
    // type.
    protected String fixReferenceForNested(Element parent, String attrName, String reference) {
        CMElementDeclaration elemDecl = getElementDecl(parent);
        if (elemDecl == null) {
            return reference;
        }

        CMNamedNodeMap children = elemDecl.getLocalElements();

        // If there is a child whose name matches the reference then no
        // change needed.
        if (children.getNamedItem(reference) != null) {
            return reference;
        }

        // Otherwise look for a child where the reference name starts with the
        // child name.  For example the reference would be privateLibraryRef
        // and the child would be privateLibrary.
        for (int i = 0; i < children.getLength(); i++) {
            String name = children.item(i).getNodeName();
            if (attrName.startsWith(name)) {
                return name;
            }
        }

        // There are cases where a reference attribute can refer to a top
        // level element only (such as the JAAS login context attribute for
        // the login module reference) so return null if no matches.
        return null;
    }

    protected Element addNested(Element parent, String name) {
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

    protected Element addTopLevel(Document doc, String name, String id, Element referencingElem) {
        Element elem = doc.createElement(name);
        elem.setAttribute(Constants.FACTORY_ID, id);
        Node previousSibling = getTopLevelParent(referencingElem);
        if (previousSibling != null) {
            Node parent = previousSibling.getParentNode();
            parent.replaceChild(elem, previousSibling);
            parent.insertBefore(previousSibling, elem);
            parent.insertBefore(doc.createTextNode("\n\n" + INDENT_STRING), elem);
        } else {
            Node root = doc.getDocumentElement();
            root.appendChild(doc.createTextNode("\n" + INDENT_STRING));
            root.appendChild(elem);
            root.appendChild(doc.createTextNode("\n"));
        }
        return elem;
    }

    protected Node getTopLevelParent(Element elem) {
        Node node = elem;
        while (node.getParentNode() != null && node.getParentNode().getNodeType() == Node.ELEMENT_NODE) {
            if (Constants.SERVER_ELEMENT.equals(node.getParentNode().getNodeName()))
                return node;
            node = node.getParentNode();
        }
        return null;
    }

    protected int getNestedLevel(Element elem) {
        int i = 0;
        Node parent = elem.getParentNode();
        while (parent != null && parent.getNodeType() != Node.DOCUMENT_NODE) {
            i++;
            parent = parent.getParentNode();
        }
        return i;
    }

    protected String getIndent(int indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            builder.append(INDENT_STRING);
        }
        return builder.toString();
    }

    protected void openElement(IEditorPart editorPart, Element elem) {
        if (editorPart instanceof DDEMultiPageEditorPart) {
            ((DDEMultiPageEditorPart) editorPart).refresh();
            ((DDEMultiPageEditorPart) editorPart).setSelection(elem);
        }
    }

    /************************ IDDialog ***************************/
    protected static class IDDialog extends TitleAreaDialog {

        String label;
        Text idText;
        String id;

        public IDDialog(Shell parent, String label) {
            super(parent);
            this.label = label;
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(Messages.idDialogTitle);
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            setTitle(Messages.idDialogLabel);
            setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
            setMessage(NLS.bind(Messages.idDialogMessage, label));

            final Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 11;
            layout.marginWidth = 9;
            layout.horizontalSpacing = 5;
            layout.verticalSpacing = 7;
            layout.numColumns = 2;
            composite.setLayout(layout);
            GridData data = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(data);
            composite.setFont(parent.getFont());

            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.idLabel);
            data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
            label.setLayoutData(data);

            idText = new Text(composite, SWT.BORDER);
            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            idText.setLayoutData(data);

            idText.addModifyListener(new ModifyListener() {
                /** {@inheritDoc} */
                @Override
                public void modifyText(ModifyEvent arg0) {
                    String idStr = idText.getText();
                    enableOKButton(idStr != null && !idStr.isEmpty());
                }
            });

            return composite;
        }

        /** {@inheritDoc} */
        @Override
        protected Control createButtonBar(Composite parent) {
            Control control = super.createButtonBar(parent);
            enableOKButton(false);
            return control;
        }

        /** {@inheritDoc} */
        @Override
        public void create() {
            super.create();
            idText.setFocus();
        }

        @Override
        protected void okPressed() {
            id = idText.getText();
            super.okPressed();
        }

        protected void enableOKButton(boolean value) {
            getButton(IDialogConstants.OK_ID).setEnabled(value);
        }

        public String getId() {
            return id;
        }
    }

}
