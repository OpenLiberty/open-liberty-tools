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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.internal.config.ConfigVarsUtils;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.config.URILocation;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Custom control for a singleton reference item.
 */
@SuppressWarnings("restriction")
public class ReferenceSingleCustomObject extends ReferenceBaseCustomObject {

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(final Element input, final String itemName, Composite composite, final IEditorPart editorPart, EventListener listener) {
        final Shell shell = composite.getShell();

        // Get the reference annotation from the schema.  It contains
        // the name of the element that this item can reference.
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        final String[] references = getReferences(attrDecl);
        if (references == null || references.length == 0) {
            Trace.logError("Have a reference type but could not get the reference name.", null);
            return;
        }
        final boolean multipleRefs = references.length > 1;

        final String nestedReference = multipleRefs ? null : fixReferenceForNested(input, itemName, references[0]);

        final Document doc = input.getOwnerDocument();
        final URI docURI = ConfigUIUtils.getURI(editorPart.getEditorInput(), doc);
        WebSphereServerInfo serverInfo = ConfigUtils.getServerInfo(docURI);
        UserDirectory userDir = serverInfo != null ? serverInfo.getUserDirectory() : ConfigUtils.getUserDirectory(docURI);

        // Get the current reference from the attribute value.
        String refValue = DOMUtils.getAttributeValue(input, itemName);

        final ConfigVarComputer configVarComputer = getGlobalConfigVarComputer(input, editorPart);

        // Get the set of existing ids available for reference.
        final Map<String, URILocation> idMap = ConfigUIUtils.getIdMap(doc, docURI, serverInfo, userDir, references);
        List<String> ids = new ArrayList<String>(idMap.keySet());
        Collections.sort(ids);

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Set the alignment of the label to beginning
        setLabelVerticalAlign(composite, GridData.CENTER);

        // Combo box for the current reference
        final CCombo refCombo = widgetFactory.createCCombo(composite);
        refCombo.setEditable(true);
        List<String> itemList = new ArrayList<String>(ids);
        List<String> vars = configVarComputer.getConfigVars().getSortedVars(ConfigVars.STRING_TYPES, false);
        for (String var : vars) {
            itemList.add(ConfigVarsUtils.getVarRef(var));
        }
        refCombo.setItems(itemList.toArray(new String[itemList.size()]));
        if (refValue != null && !refValue.isEmpty()) {
            refCombo.setText(refValue);
        }
        addFactoryIdHyperlink(refCombo, idMap, configVarComputer.getConfigVars());
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalIndent = LEFT_INDENT - 1;
        data.verticalIndent = 1;
        refCombo.setLayoutData(data);

        refCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = refCombo.getText();
                updateAttr(input, itemName, text);
            }
        });

        if (nestedReference != null || multipleRefs) {
            // Add a new button with a dropdown menu that allows the user to do one of the following:
            //    - create a new nested or top level (if single reference)
            //    - create a new top level of one of the reference types (for multiple references)

            // Add button composite
            final Composite addButtonComposite = widgetFactory.createComposite(composite);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 0;
            layout.numColumns = 2;
            addButtonComposite.setLayout(layout);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = false;
            addButtonComposite.setLayoutData(data);

            // Add button
            Button addButton;
            Button menuButton = null;
            if (multipleRefs) {
                addButton = widgetFactory.createButton(addButtonComposite, Messages.addButton2, SWT.PUSH | SWT.RIGHT_TO_LEFT);
                addButton.setImage(Activator.getImage(Activator.IMG_MENU_DOWN));
                data = new GridData(GridData.FILL_HORIZONTAL);
                addButton.setLayoutData(data);
            } else {
                addButton = widgetFactory.createButton(addButtonComposite, Messages.addButton2, SWT.PUSH);
                data = new GridData(GridData.FILL_HORIZONTAL);
                addButton.setLayoutData(data);

                addButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Element elem = addNested(input, nestedReference);
                        openElement(editorPart, elem);
                    }
                });

                menuButton = widgetFactory.createButton(addButtonComposite, "", SWT.PUSH);
                setDropdownData(addButton, menuButton);
            }

            final Button mainButton = addButton;
            final Button dropdownButton = menuButton == null ? mainButton : menuButton;

            dropdownButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Menu menu = new Menu(shell, SWT.POP_UP);
                    if (nestedReference != null) {
                        MenuItem item = new MenuItem(menu, SWT.PUSH);
                        item.setText(Messages.referenceNestedButton);
                        item.addListener(SWT.Selection, new Listener() {
                            @Override
                            public void handleEvent(Event e) {
                                Element elem = addNested(input, nestedReference);
                                openElement(editorPart, elem);
                            }
                        });
                        menu.setDefaultItem(item);
                    }
                    for (String reference : references) {
                        final String ref = reference;
                        String label = SchemaUtil.getLabel(doc, new String[] { Constants.SERVER_ELEMENT, ref }, docURI);
                        final String typeLabel = (label == null ? ref : label) + "...";
                        MenuItem item = new MenuItem(menu, SWT.PUSH);
                        item.setText(multipleRefs ? typeLabel : Messages.referenceTopLevelButton);
                        item.addListener(SWT.Selection, new Listener() {
                            @Override
                            public void handleEvent(Event e) {
                                IDDialog dialog = new IDDialog(shell, typeLabel);
                                if (dialog.open() == IStatus.OK) {
                                    String id = dialog.getId();
                                    refCombo.setText(id);
                                    updateAttr(input, itemName, id);
                                    Element elem = addTopLevel(doc, ref, id, input);
                                    openElement(editorPart, elem);
                                }
                            }
                        });
                    }
                    displayDropdownMenu(mainButton, menu, mainButton == dropdownButton);
                    menu.dispose();
                }
            });

            addButton.setEnabled(!getReadOnly());
            dropdownButton.setEnabled(!getReadOnly());
        } else {
            // Add an Add button only that creates a top level element

            final String reference = references[0];
            String label = SchemaUtil.getLabel(doc, new String[] { Constants.SERVER_ELEMENT, reference }, docURI);
            final String typeLabel = label == null ? reference : label;
            final Button addButton = widgetFactory.createButton(composite, Messages.addButton2, SWT.PUSH);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = false;
            addButton.setLayoutData(data);
            addButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    IDDialog dialog = new IDDialog(shell, typeLabel);
                    if (dialog.open() == IStatus.OK) {
                        String id = dialog.getId();
                        refCombo.setText(id);
                        updateAttr(input, itemName, id);
                        Element elem = addTopLevel(doc, reference, id, input);
                        openElement(editorPart, elem);
                    }
                }
            });

            addButton.setEnabled(!getReadOnly());
        }

        refCombo.setEnabled(!getReadOnly());
    }

    protected void updateAttr(Element elem, String attrName, String ref) {
        // Update the attribute value.
        if (ref == null || ref.isEmpty()) {
            elem.removeAttribute(attrName);
            return;
        }
        elem.setAttribute(attrName, ref);
    }

    protected void addFactoryIdHyperlink(final CCombo combolControl, final Map<String, URILocation> idMap, final ConfigVars configVars) {
        combolControl.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.keyCode == SWT.F3) {
                    String idName = combolControl.getText();
                    String varName = idName == null ? null : ConfigVarsUtils.getVariableName(idName);
                    URILocation location = null;
                    if (varName != null && !varName.isEmpty()) {
                        location = configVars.getDocumentLocation(varName);
                    } else if (idName != null && !idName.isEmpty() && idMap.containsKey(idName)) {
                        location = idMap.get(idName);
                    }
                    if (location != null && location.getURI() != null) {
                        Activator.open(location);
                    }
                }
            }
        });
    }

}
