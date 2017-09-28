/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.ContentModelManager;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.xwt.dde.internal.util.DetailsViewerAdapter;

/**
 *
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class SchemaPropertiesCustomObject extends BaseCustomObject {

    public static final String PARENT_ELEM_DATA = "schema.properties.parent.elem";
    public static final String EDITOR_PART_DATA = "schema.properties.editor.part";

    protected Element propertiesElem = null;
    protected Text text = null;
    protected Button setButton = null;
    protected Button editButton = null;
    protected Button clearButton = null;

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(final Element input, final String itemName, Composite parent, final IEditorPart editorPart, EventListener listener) {

        // Get the available elements for the property
        String schemaPath = getSchemaPath(input, itemName, null);
        if (schemaPath != null) {
            HashMap<String, CMElementDeclaration> cmElemMap = getValidElements(schemaPath, input, itemName, null);

            // Get the current element if any
            Iterator elemNameIterator = cmElemMap.keySet().iterator();
            while (elemNameIterator.hasNext()) {
                String name = (String) elemNameIterator.next();
                NodeList nodeList = input.getElementsByTagName(name);
                if (nodeList != null && nodeList.getLength() > 0) {
                    propertiesElem = (Element) nodeList.item(0);
                    break;
                }
            }
        }

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();
        final Shell shell = parent.getShell();

        // Set the alignment of the label to beginning
        setLabelVerticalAlign(parent, GridData.BEGINNING);

        text = widgetFactory.createText(parent, "", SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = text.getLineHeight() * 3;
        data.verticalIndent = LEFT_INDENT;
        text.setLayoutData(data);

        // Button composite
        Composite buttonComposite = widgetFactory.createComposite(parent, SWT.FLAT);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 1;
        buttonComposite.setLayout(layout);
        data = new GridData(GridData.FILL, GridData.BEGINNING, false, false);
        data.horizontalSpan = 1;
        buttonComposite.setLayoutData(data);

        // Buttons
        setButton = widgetFactory.createButton(buttonComposite, Messages.setButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        setButton.setLayoutData(data);

        editButton = widgetFactory.createButton(buttonComposite, Messages.editButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        editButton.setLayoutData(data);

        clearButton = widgetFactory.createButton(buttonComposite, Messages.clearButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        clearButton.setLayoutData(data);

        // Listeners

        // Hide the scroll bars on the text box if not needed
        Listener scrollBarListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                Rectangle r1 = text.getClientArea();
                Rectangle r2 = text.computeTrim(r1.x, r1.y, r1.width, r1.height);
                Point p = text.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                text.getHorizontalBar().setVisible(r2.width <= p.x);
                text.getVerticalBar().setVisible(r2.height <= p.y);
                if (event.type == SWT.Modify) {
                    text.getParent().layout(true);
                    text.showSelection();
                }
            }
        };
        text.addListener(SWT.Resize, scrollBarListener);
        text.addListener(SWT.Modify, scrollBarListener);

        setButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String schemaPath = getSchemaPath(input, itemName, shell);
                if (schemaPath == null)
                    return;
                HashMap<String, CMElementDeclaration> cmElemMap = getValidElements(schemaPath, input, itemName, shell);
                if (cmElemMap.isEmpty())
                    return;
                URL schemaURL = getURL(schemaPath);

                SchemaPropertiesData propertiesData = new SchemaPropertiesData(schemaURL, cmElemMap, null, null, input, editorPart);
                SchemaPropertiesWizard wizard = new SchemaPropertiesWizard(propertiesData);
                WizardDialog dialog = new WizardDialog(shell, wizard);
                dialog.setPageSize(425, 275);
                if (dialog.open() == Window.CANCEL)
                    return;

                // Since this is a set operation, remove the old element
                if (propertiesElem != null)
                    ConfigUIUtils.removeNode(propertiesElem);
                propertiesElem = updatePropertiesElem(input, null, propertiesData);
                editButton.setEnabled(true);
                clearButton.setEnabled(true);
                try {
                    String xml = getXMLString(propertiesElem);
                    text.setText(xml);
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to build document fragment for properties.", e);
                }
            }
        });

        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String schemaPath = getSchemaPath(input, itemName, shell);
                if (schemaPath == null)
                    return;
                HashMap<String, CMElementDeclaration> cmElemMap = getValidElements(schemaPath, input, itemName, shell);
                if (cmElemMap.isEmpty())
                    return;
                URL schemaURL = getURL(schemaPath);

                SchemaPropertiesData propertiesData = new SchemaPropertiesData(schemaURL, cmElemMap, propertiesElem.getLocalName(), getProperties(propertiesElem), input, editorPart);
                SchemaPropertiesWizard wizard = new SchemaPropertiesWizard(propertiesData);
                WizardDialog dialog = new WizardDialog(shell, wizard);
                dialog.setPageSize(425, 275);
                if (dialog.open() == Window.CANCEL)
                    return;

                propertiesElem = updatePropertiesElem(input, propertiesElem, propertiesData);
                try {
                    String xml = getXMLString(propertiesElem);
                    text.setText(xml);
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to build document fragment for properties.", e);
                }
            }
        });

        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                removePropertiesElem(propertiesElem);
                propertiesElem = null;
                text.setText("");
                editButton.setEnabled(false);
                clearButton.setEnabled(false);
            }
        });

        text.setEnabled(!getReadOnly());
        setButton.setEnabled(!getReadOnly());
        editButton.setEnabled(!getReadOnly());
        clearButton.setEnabled(!getReadOnly());
    }

    @Override
    public void postLayoutProcessing() {
        if (propertiesElem != null) {
            try {
                text.setText(getXMLString(propertiesElem));
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to serialize the existing properties node: " + propertiesElem.getLocalName(), e);
            }
        }
        editButton.setEnabled(propertiesElem != null);
        clearButton.setEnabled(propertiesElem != null);
    }

    protected HashMap<String, CMElementDeclaration> getValidElements(String schemaPath, Element input, String itemName, Shell shell) {
        // Get the elements in the schema that are valid for the current parent and
        // the current properties id
        HashMap<String, CMElementDeclaration> elemMap = new HashMap<String, CMElementDeclaration>();
        String propertiesId = getPropertiesId(input, itemName, shell);
        CMDocument doc = getSchema(schemaPath);
        if (propertiesId != null && doc != null) {
            CMNamedNodeMap elements = doc.getElements();
            String elemName = input.getLocalName();
            for (int i = 0; i < elements.getLength(); i++) {
                CMElementDeclaration element = (CMElementDeclaration) elements.item(i);
                String propertiesIdVal = SchemaUtil.getExtInfo(element, "propertiesId");
                if (propertiesId.equals(propertiesIdVal)) {
                    String parentVal = SchemaUtil.getExtInfo(element, "parent");
                    if (elemName.equals(parentVal))
                        elemMap.put(element.getElementName(), element);
                }
            }
        }
        if (elemMap.isEmpty())
            Trace.logError("No valid schema elements for parent: " + input.getLocalName() + ", and properties id: " + propertiesId, null);
        return elemMap;
    }

    protected String getSchemaPath(Element input, String itemName, Shell shell) {
        // Get the schema attribute
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        String attrName = SchemaUtil.getExtInfo(attrDecl, "schemaAttr");
        if (attrName == null) {
            Trace.logError("Have a schema properties type but can't get schema attribute.", null);
            return null;
        }
        final String schemaGenInfo = input.getAttribute(attrName);
        if ((schemaGenInfo == null || schemaGenInfo.isEmpty()) && shell != null) {
            MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.schemaPropsRequiredAttrNoValue, attrName));
            return null;
        }
        // TODO: generate the schema based on the attribute

        String schemaPath = "C:/home/eclipse431/workspace_oltx/SchemaTest/properties.xsd";
        return schemaPath;
    }

    protected URL getURL(String path) {
        try {
            File file = new File(path);
            return file.toURI().toURL();
        } catch (Exception e) {
            Trace.logError("Invalid schema path, cannot convert to URI: " + path, e);
        }
        return null;
    }

    protected CMDocument getSchema(String schemaPath) {
        File file = new File(schemaPath);
        CMDocument cmDocument = null;
        if (file.exists()) {
            cmDocument = ContentModelManager.getInstance().createCMDocument(file.toURI().toString(), null);
        }
        if (cmDocument == null) {
            Trace.logError("Could not load the schema: " + schemaPath, null);
        }
        return cmDocument;
    }

    protected String getPropertiesId(Element input, String itemName, Shell shell) {
        // Get the properties id attribute
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        String attrName = SchemaUtil.getExtInfo(attrDecl, "propertiesId");
        if (attrName == null) {
            Trace.logError("Have a schema properties type but can't get schema attribute.", null);
            return null;
        }
        String id = input.getAttribute(attrName);
        if ((id == null || id.isEmpty()) && shell != null) {
            MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.schemaPropsRequiredAttrNoValue, attrName));
            return null;
        }
        return id;
    }

    protected Element updatePropertiesElem(Element parent, Element currentElem, SchemaPropertiesData data) {
        String elemName = data.getChosenElem();
        Map<String, String> properties = data.getUpdatedProperties();
        // Modify the existing element if they are the same
        if (currentElem != null && currentElem.getLocalName().equals(elemName)) {
            Set<Entry<String, String>> entries = properties.entrySet();
            for (Entry<String, String> entry : entries) {
                String name = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.isEmpty()) {
                    if (currentElem.hasAttribute(name))
                        currentElem.removeAttribute(name);
                } else {
                    currentElem.setAttribute(name, value);
                }
            }
            return currentElem;
        }

        // Remove the old element and create a new one if they are different
        if (currentElem != null)
            ConfigUIUtils.removeNode(currentElem);
        Element newElem = ConfigUIUtils.addNode(elemName, parent);
        Set<Entry<String, String>> entries = properties.entrySet();
        for (Entry<String, String> entry : entries) {
            if (entry.getValue() != null && !entry.getValue().isEmpty())
                newElem.setAttribute(entry.getKey(), entry.getValue());
        }
        return newElem;
    }

    protected void removePropertiesElem(Element currentElem) {
        if (currentElem != null)
            ConfigUIUtils.removeNode(currentElem);
    }

    protected String getXMLString(Node node) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Result result = new StreamResult(os);
        Source source = new DOMSource(node);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(source, result);
        return os.toString();
    }

    protected Map<String, String> getProperties(Element elem) {
        Map<String, String> properties = new HashMap<String, String>();
        NamedNodeMap attrs = elem.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            properties.put(attr.getLocalName(), attr.getValue());
        }
        return properties;
    }

    public static class SchemaPropertiesData {
        URL schemaURL;
        Map<String, CMElementDeclaration> elemMap;
        String chosenElem;
        Map<String, String> properties;
        Element parentElem;
        IEditorPart editorPart;
        DetailsViewerAdapter dvAdapter;

        public SchemaPropertiesData(URL schemaURL, Map<String, CMElementDeclaration> elemMap, String chosenElem, Map<String, String> properties, Element parentElem,
                                    IEditorPart editorPart) {
            this.schemaURL = schemaURL;
            this.elemMap = elemMap;
            this.chosenElem = chosenElem;
            this.properties = properties;
            if (this.chosenElem == null && elemMap.size() == 1) {
                this.chosenElem = elemMap.keySet().iterator().next();
            }
            if (this.properties == null) {
                this.properties = new HashMap<String, String>();
            }
            this.parentElem = parentElem;
            this.editorPart = editorPart;
            dvAdapter = new DetailsViewerAdapter();
        }

        public URL getSchemaURL() {
            return schemaURL;
        }

        public String[] getElems() {
            return elemMap.keySet().toArray(new String[elemMap.size()]);
        }

        public String getChosenElem() {
            return chosenElem;
        }

        public void setChosenElem(String chosenElem) {
            this.chosenElem = chosenElem;
        }

        public CMElementDeclaration getElementDecl() {
            return elemMap.get(chosenElem);
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public Element getParentElem() {
            return parentElem;
        }

        public IEditorPart getEditorPart() {
            return editorPart;
        }

        public DetailsViewerAdapter getDVAdapter() {
            return dvAdapter;
        }

        public Map<String, String> getUpdatedProperties() {
            return dvAdapter.getElementProperties();
        }
    }

}
