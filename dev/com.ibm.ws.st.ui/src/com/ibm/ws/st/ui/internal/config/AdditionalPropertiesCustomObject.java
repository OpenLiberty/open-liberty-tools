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
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.internal.util.ModelUtil;

/**
 * Custom control for additional properties.
 */
@SuppressWarnings("restriction")
public class AdditionalPropertiesCustomObject extends BaseCustomObject {

    private static final int KEY = 0;
    private static final int VALUE = 1;

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(final Element element, String itemName, Composite parent, IEditorPart editorPart, EventListener listener) {
        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        Set<String> predefinedAttrs = getAttributes(element);

        // Set the alignment of the label to beginning
        setLabelVerticalAlign(parent, GridData.BEGINNING);

        final Composite tableComposite = widgetFactory.createComposite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        tableComposite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.horizontalIndent = LEFT_INDENT;
        tableComposite.setLayoutData(data);
        tableComposite.setFont(parent.getFont());

        final Composite buttonComposite = widgetFactory.createComposite(parent, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonComposite.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        buttonComposite.setLayoutData(data);
        buttonComposite.setFont(parent.getFont());

        // Table
        final Table propertyTable = widgetFactory.createTable(tableComposite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        propertyTable.setLinesVisible(true);
        propertyTable.setHeaderVisible(true);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 100;
        propertyTable.setLayoutData(data);

        final TableColumn keyColumn = new TableColumn(propertyTable, SWT.NONE);
        keyColumn.setText(Messages.additionalPropsKeyColumn);
        keyColumn.setWidth(100);

        final TableColumn valueColumn = new TableColumn(propertyTable, SWT.NONE);
        valueColumn.setText(Messages.additionalPropsValueColumn);
        valueColumn.setWidth(100);

        NamedNodeMap attrMap = element.getAttributes();
        if (attrMap != null) {
            for (int i = 0; i < attrMap.getLength(); i++) {
                Attr attr = (Attr) attrMap.item(i);
                if (!predefinedAttrs.contains(attr.getName())) {
                    TableItem item = new TableItem(propertyTable, SWT.NONE);
                    item.setText(KEY, attr.getName());
                    item.setText(VALUE, attr.getValue());
                }
            }
        }

        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && "property".equals(node.getNodeName())) {
                Element childElement = (Element) node;
                TableItem item = new TableItem(propertyTable, SWT.NONE);
                item.setText(KEY, childElement.getAttribute("name"));
                item.setText(VALUE, childElement.getAttribute("value"));
            }
        }

        // Buttons
        final Button addButton = widgetFactory.createButton(buttonComposite, Messages.addButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        addButton.setLayoutData(data);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EntryDialog dialog = createNewEntryDialog(buttonComposite.getShell(), element);
                dialog.open();
                if (dialog.isOK()) {
                    TableItem item = new TableItem(propertyTable, SWT.NONE);
                    String key = dialog.getKey();
                    String value = dialog.getValue();
                    item.setText(KEY, key);
                    item.setText(VALUE, value);
                    setValue(element, key, value);
                }
            }
        });

        final Button editButton = widgetFactory.createButton(buttonComposite, Messages.editButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        editButton.setLayoutData(data);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] items = propertyTable.getSelection();
                if (items.length > 0) {
                    TableItem item = items[0];
                    String origKey = item.getText(KEY);
                    EntryDialog dialog = createEditEntryDialog(buttonComposite.getShell(), element);
                    dialog.setKeyValuePair(origKey, item.getText(VALUE));
                    dialog.open();
                    if (dialog.isOK()) {
                        String key = dialog.getKey();
                        String value = dialog.getValue();
                        item.setText(KEY, key);
                        item.setText(VALUE, value);
                        if (!origKey.equalsIgnoreCase(key)) {
                            removeValue(element, origKey);
                        }
                        setValue(element, key, value);
                    }
                }
            }
        });

        final Button removeButton = widgetFactory.createButton(buttonComposite, Messages.removeButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        removeButton.setLayoutData(data);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int[] indices = propertyTable.getSelectionIndices();
                for (int index : indices) {
                    String key = propertyTable.getItem(index).getText(KEY);
                    removeValue(element, key);
                }
                propertyTable.remove(indices);
                editButton.setEnabled(false);
                removeButton.setEnabled(false);
            }
        });

        propertyTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (getReadOnly())
                    return;
                int count = propertyTable.getSelectionCount();
                if (count == 1) {
                    editButton.setEnabled(true);
                    removeButton.setEnabled(true);
                } else if (count > 1) {
                    editButton.setEnabled(false);
                    removeButton.setEnabled(true);
                } else {
                    editButton.setEnabled(false);
                    removeButton.setEnabled(false);
                }
            }
        });

        tableComposite.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle area = tableComposite.getClientArea();
                Point preferredSize = propertyTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                int width = area.width - 2 * propertyTable.getBorderWidth();
                if (preferredSize.y > area.height + propertyTable.getHeaderHeight()) {
                    // Subtract the scrollbar width from the total column width
                    // if a vertical scrollbar will be required
                    Point vBarSize = propertyTable.getVerticalBar().getSize();
                    width -= vBarSize.x;
                }
                Point oldSize = propertyTable.getSize();
                if (oldSize.x > area.width) {
                    // table is getting smaller so make the columns 
                    // smaller first and then resize the table to
                    // match the client area width
                    keyColumn.setWidth(width / 2);
                    valueColumn.setWidth(width - keyColumn.getWidth());
                    propertyTable.setSize(area.width, area.height);
                } else {
                    // table is getting bigger so make the table 
                    // bigger first and then make the columns wider
                    // to match the client area width
                    propertyTable.setSize(area.width, area.height);
                    keyColumn.setWidth(width / 2);
                    valueColumn.setWidth(width - keyColumn.getWidth());
                }
            }
        });

        addButton.setEnabled(!getReadOnly());
        editButton.setEnabled(false);
        removeButton.setEnabled(false);

    }

    protected boolean childPropertyElementsSupported(Element element) {
        // Return true if the element declaration contains a child element named "property"
        CMElementDeclaration cmElementDeclaration = getElementDecl(element);
        if (cmElementDeclaration != null) {
            CMNamedNodeMap localElements = cmElementDeclaration.getLocalElements();
            Iterator<?> iterator = localElements.iterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object instanceof CMElementDeclaration) {
                    CMElementDeclaration childCMElementDeclaration = (CMElementDeclaration) object;
                    if ("property".equals(childCMElementDeclaration.getNodeName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void setValue(Element element, String key, String value) {
        // Check attributes
        NamedNodeMap attributes = element.getAttributes();
        boolean attributeFound = false;
        for (int i = 0; i < attributes.getLength(); i++) {
            String nodeName = attributes.item(i).getNodeName();
            if (key.equalsIgnoreCase(nodeName) && !key.equals(nodeName)) {
                attributeFound = true;
                break;
            }
        }
        if (!childPropertyElementsSupported(element) || !attributeFound) {
            // If property child elements are not supported or an attribute with the key is not found, set the attribute value
            element.setAttribute(key, value);
        } else {
            // if an attribute with the given key is found, check child elements
            boolean childElementFound = false;
            for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() == Node.ELEMENT_NODE && "property".equals(node.getNodeName())) {
                    Element childElement = (Element) node;
                    if (key.equals(childElement.getAttribute("name"))) {
                        // If a child element with a matching key is found, update it
                        childElement.setAttribute("value", value);
                        childElementFound = true;
                        break;
                    }
                }
            }
            if (!childElementFound) {
                // If no matching child element is found, create it
                Element propertyElement = element.getOwnerDocument().createElement("property");
                propertyElement.setAttribute("name", key);
                propertyElement.setAttribute("value", value);
                element.appendChild(propertyElement);

                // Format node
                ModelUtil.formatXMLNode(element);
            }
        }
    }

    protected void removeValue(Element element, String key) {
        String attribute = element.getAttribute(key);
        if (attribute != null && attribute.length() > 0) {
            element.removeAttribute(key);
        } else {
            for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) node;
                    if (key.equals(childElement.getAttribute("name"))) {
                        // Remove element
                        ModelUtil.removePrecedingText(childElement);
                        element.removeChild(childElement);

                        // Format node
                        ModelUtil.formatXMLNode(element);
                        break;
                    }
                }
            }
        }
    }

    protected EntryDialog createNewEntryDialog(Shell parent, Element element) {
        return new EntryDialog(parent, Messages.additionalPropsNewTitle,
                        Messages.additionalPropsNewLabel,
                        Messages.additionalPropsNewMessage, false,
                        element);
    }

    protected EntryDialog createEditEntryDialog(Shell parent, Element element) {
        return new EntryDialog(parent, Messages.additionalPropsEditTitle,
                        Messages.additionalPropsEditLabel,
                        Messages.additionalPropsEditMessage, true,
                        element);
    }

    /************************ EntryDialog ***************************/
    private static class EntryDialog extends TitleAreaDialog {
        protected String dialogTitle;
        protected String dialogLabel;
        protected String dialogMessage;
        protected boolean isEdit;
        protected Element element;
        protected boolean isOK = false;
        protected String origKey = "";
        protected String key = "";
        protected String value = "";

        public EntryDialog(Shell parent, String title, String label, String message, boolean isEdit, Element element) {
            super(parent);
            this.dialogTitle = title;
            this.dialogLabel = label;
            this.dialogMessage = message;
            this.isEdit = isEdit;
            this.element = element;
        }

        public void setKeyValuePair(String key, String value) {
            this.origKey = key;
            this.key = key;
            this.value = value;
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(dialogTitle);
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected void buttonPressed(int buttonId) {
            super.buttonPressed(buttonId);
            if (buttonId == IDialogConstants.OK_ID) {
                isOK = true;
            }
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            setTitle(dialogLabel);
            setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
            setMessage(dialogMessage);

            final Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 11;
            layout.marginWidth = 9;
            layout.horizontalSpacing = 5;
            layout.verticalSpacing = 7;
            layout.numColumns = 2;
            composite.setLayout(layout);
            GridData data = new GridData(GridData.FILL_BOTH);
            data.minimumWidth = 300;
            composite.setLayoutData(data);
            composite.setFont(parent.getFont());

            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.additionalPropsKeyLabel);
            data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
            label.setLayoutData(data);

            final Text keyText = new Text(composite, SWT.BORDER);
            if (!key.isEmpty()) {
                keyText.setText(key);
            }
            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            keyText.setLayoutData(data);

            label = new Label(composite, SWT.NONE);
            label.setText(Messages.additionalPropsValueLabel);
            data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
            label.setLayoutData(data);

            final Text valueText = new Text(composite, SWT.BORDER);
            if (!value.isEmpty()) {
                valueText.setText(value);
            }
            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            valueText.setLayoutData(data);

            ModifyListener listener = new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    String keyStr = keyText.getText();
                    // Give error if the attribute already exists unless the user
                    // is editing an existing property and the entered key matches
                    // the original key.
                    if (!(isEdit && keyStr.equals(origKey)) && keyPresent(element, keyStr)) {
                        setErrorMessage(Messages.additionalPropsKeyError);
                        enableOKButton(false);
                    } else {
                        setErrorMessage(null);
                        String valueStr = valueText.getText();
                        if (!keyStr.isEmpty() && !valueStr.isEmpty()) {
                            key = keyStr;
                            value = valueStr;
                            enableOKButton(true);
                        } else {
                            key = "";
                            value = "";
                            enableOKButton(false);
                        }
                    }
                }

                private boolean keyPresent(Element element, String keyStr) {
                    if (element.hasAttribute(keyStr)) {
                        return true;
                    }
                    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                        if (node.getNodeType() == Node.ELEMENT_NODE && "property".equals(node.getNodeName())) {
                            Element childElement = (Element) node;
                            if (keyStr.equals(childElement.getAttribute("name"))) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

            };

            keyText.addModifyListener(listener);
            valueText.addModifyListener(listener);

            if (isEdit) {
                valueText.selectAll();
                valueText.setFocus();
            } else {
                keyText.setFocus();
            }

            return composite;
        }

        /** {@inheritDoc} */
        @Override
        protected Control createButtonBar(Composite parent) {
            Control control = super.createButtonBar(parent);
            enableOKButton(false);
            return control;
        }

        protected void enableOKButton(boolean value) {
            getButton(IDialogConstants.OK_ID).setEnabled(value);
        }

        public boolean isOK() {
            return isOK;
        }

        public String getKey() {
            if (isOK) {
                return key;
            }
            return null;
        }

        public String getValue() {
            if (isOK) {
                return value;
            }
            return null;
        }
    }

}
