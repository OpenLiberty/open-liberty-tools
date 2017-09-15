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

import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.w3c.dom.Element;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.xwt.dde.customization.ICustomControlObject3;
import com.ibm.xwt.dde.internal.validation.IValidateNotifier;

/**
 * Custom control with variable support.
 */
@SuppressWarnings("restriction")
public abstract class BaseCustomObject implements ICustomControlObject3 {

    // This is the amount the editor indents its controls.
    protected static final int LEFT_INDENT = 2;

    private boolean readOnly = false;

    protected CMElementDeclaration getElementDecl(Element elem) {
        return ConfigUIUtils.getElementDecl(elem);
    }

    protected CMAttributeDeclaration getAttrDecl(Element elem, String attrName) {
        CMAttributeDeclaration attrDecl = null;
        CMElementDeclaration elemDecl = getElementDecl(elem);
        if (elemDecl != null) {
            CMNamedNodeMap attrs = elemDecl.getAttributes();
            CMNode node = attrs.getNamedItem(attrName);
            if (node != null && node.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
                attrDecl = (CMAttributeDeclaration) node;
            }
        }
        return attrDecl;
    }

    protected Set<String> getAttributes(Element elem) {
        Set<String> attrSet = new HashSet<String>();
        CMElementDeclaration elemDecl = getElementDecl(elem);
        if (elemDecl != null) {
            CMNamedNodeMap attrs = elemDecl.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                attrSet.add(attrs.item(i).getNodeName());
            }
        }
        return attrSet;
    }

    protected ConfigVarComputer getConfigVarComputer(Element elem, String attrName, IEditorPart editorPart) {
        return getConfigVarComputer(elem, attrName, editorPart, false);
    }

    protected ConfigVarComputer getGlobalConfigVarComputer(Element elem, IEditorPart editorPart) {
        return getConfigVarComputer(elem, null, editorPart, true);
    }

    protected ConfigVarComputer getConfigVarComputer(Element elem, String attrName, IEditorPart editorPart, boolean globalOnly) {
        Object editorPartData = elem != null ? elem.getUserData(SchemaPropertiesCustomObject.EDITOR_PART_DATA) : null;
        if (editorPartData != null && elem != null) {
            // We are dealing with extended schema properties - the variables should come
            // from the main document
            IEditorInput editorInput = ((IEditorPart) editorPartData).getEditorInput();
            Element parentElem = (Element) elem.getUserData(SchemaPropertiesCustomObject.PARENT_ELEM_DATA);
            return new ConfigVarComputer(editorInput, parentElem, null, true);
        }
        return new ConfigVarComputer(editorPart.getEditorInput(), elem, attrName, globalOnly);
    }

    /*
     * Fix up the vertical alignment of the label. For large custom controls that
     * span more than one line, GridData.BEGINNING should be passed in for the second
     * argument. For custom controls that only take up one line, GridData.CENTER
     * should be passed in for the second argument.
     * 
     * In future there will be a method on ICustomControlObject so that each custom
     * control can tell the editor what alignment it wants.
     */
    protected void setLabelVerticalAlign(Composite composite, int verticalAlign) {
        Control[] controls = composite.getChildren();
        if (controls.length > 0) {
            Control label = controls[controls.length - 1];
            if (label instanceof Label || label instanceof Hyperlink) {
                ((GridData) label.getLayoutData()).verticalAlignment = verticalAlign;
            }
        }
    }

    /*
     * Set the layout data for the dropdown button based on the match control.
     */
    protected void setDropdownData(Control match, Button dropdownButton) {
        dropdownButton.setImage(Activator.getImage(Activator.IMG_MENU_DOWN));
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = false;
        data.heightHint = match.getSize().y;
        dropdownButton.setLayoutData(data);
    }

    /*
     * Display the dropdown menu below the anchor control.
     */
    protected void displayDropdownMenu(Control anchor, Menu menu, boolean subtractWidth) {
        Point size = anchor.getSize();
        Point point = anchor.toDisplay(0, size.y);
        menu.setLocation(point.x - (subtractWidth ? size.x : 0), point.y);
        menu.setVisible(true);
        while (!menu.isDisposed() && menu.isVisible()) {
            Display display = menu.getShell().getDisplay();
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    @Override
    public void postLayoutProcessing() {
        // Do nothing by default
    }

    @Override
    public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, EventListener listener, boolean readOnlyMode) {
        readOnly = readOnlyMode;
        this.createCustomControl(input, itemName, composite, editorPart, listener);
    }

    @Override
    public void createCustomControl(Element input, String itemName, Composite composite, IEditorPart editorPart, IValidateNotifier listener, boolean readOnlyMode) {
        readOnly = readOnlyMode;
        this.createCustomControl(input, itemName, composite, editorPart, listener);
    }

    @Override
    public List<Control> getControls() {
        return null;
    }

    public boolean getReadOnly() {
        return readOnly;
    }
}
