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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
 * Custom control for reference items.
 */
@SuppressWarnings("restriction")
public class ReferenceListCustomObject extends ReferenceBaseCustomObject {

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(final Element input, final String itemName, Composite parent, final IEditorPart editorPart, EventListener listener) {
        final Shell shell = parent.getShell();

        // Get the reference annotation from the schema.  It contains
        // the name of the element that this item can reference.
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        final String[] references = getReferences(attrDecl);
        if (references == null || references.length == 0) {
            Trace.logError("Have a reference type but could not get the reference name.", null);
            return;
        }
        final boolean multipleRefs = references.length > 1;

        final String[] nestedReferences = new String[references.length];
        for (int i = 0; i < references.length; i++) {
            nestedReferences[i] = fixReferenceForNested(input, itemName, references[i]);
        }

        final IEditorInput editorInput = editorPart.getEditorInput();
        final Document doc = input.getOwnerDocument();
        final URI docURI = ConfigUIUtils.getURI(editorInput, doc);
        WebSphereServerInfo serverInfo = ConfigUtils.getServerInfo(docURI);
        UserDirectory userDir = serverInfo != null ? serverInfo.getUserDirectory() : ConfigUtils.getUserDirectory(docURI);

        // Get the current references from the attribute value.
        final Set<String> referenceSet = new HashSet<String>();
        String value = DOMUtils.getAttributeValue(input, itemName);
        if (value != null) {
            StringTokenizer tokenizer = new StringTokenizer(value, ", ");
            while (tokenizer.hasMoreTokens()) {
                referenceSet.add(tokenizer.nextToken());
            }
        }

        // Get the current set of defined variables
        final ConfigVarComputer configVarComputer = getGlobalConfigVarComputer(input, editorPart);

        // Get the set of existing ids available for reference.
        final Map<String, URILocation> idMap = ConfigUIUtils.getIdMap(doc, docURI, serverInfo, userDir, references);

        // Get the number of nested elements of this reference type
        int count = 0;
        boolean showNestedCount = false;
        for (String ref : nestedReferences) {
            if (ref != null) {
                NodeList nestedList = input.getElementsByTagName(ref);
                count += nestedList.getLength();
                showNestedCount = true;
            }
        }
        final int nestedCount = count;

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Set the alignment of the label to beginning
        setLabelVerticalAlign(parent, GridData.BEGINNING);

        // Main composite
        final Composite composite = widgetFactory.createComposite(parent, SWT.FLAT);
        GridLayout layout = new GridLayout();
        // The table will not draw properly if there is no margin so add the
        // smallest amount.
        layout.marginHeight = 1;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.horizontalSpan = 2;
        composite.setLayoutData(data);

        // Table to display the current references
        final Table refTable = widgetFactory.createTable(composite, SWT.MULTI);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 5 * 16;
        data.horizontalIndent = LEFT_INDENT;
        data.verticalSpan = 3;
        refTable.setLayoutData(data);
        createItems(refTable, referenceSet, configVarComputer.getConfigVars());
        HoverHelper.addHoverHelp(refTable);

        // Browse Button
        final Button browseButton = widgetFactory.createButton(composite, Messages.browseButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        browseButton.setLayoutData(data);

        if (nestedReferences[0] != null || multipleRefs) {
            // Add a new button with a dropdown menu that allows the user to do one of the following:
            //    - create a new nested or top level (if single reference)
            //    - create a new top level of one of the reference types (for multiple references)

            // Add button composite
            final Composite buttonComposite = widgetFactory.createComposite(composite);
            layout = new GridLayout();
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 0;
            layout.numColumns = 2;
            buttonComposite.setLayout(layout);
            data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
            buttonComposite.setLayoutData(data);

            // Add button
            Button addButton;
            Button menuButton = null;
            if (multipleRefs) {
                addButton = widgetFactory.createButton(buttonComposite, Messages.addButton2, SWT.PUSH | SWT.RIGHT_TO_LEFT);
                addButton.setImage(Activator.getImage(Activator.IMG_MENU_DOWN));
                data = new GridData(GridData.FILL_HORIZONTAL);
                data.grabExcessHorizontalSpace = true;
                addButton.setLayoutData(data);
            } else {
                addButton = widgetFactory.createButton(buttonComposite, Messages.addButton2, SWT.PUSH);
                data = new GridData(GridData.FILL_HORIZONTAL);
                data.grabExcessHorizontalSpace = true;
                addButton.setLayoutData(data);

                addButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Element elem = addNested(input, nestedReferences[0]);
                        openElement(editorPart, elem);
                    }
                });

                menuButton = widgetFactory.createButton(buttonComposite, "", SWT.PUSH);
                setDropdownData(addButton, menuButton);
            }

            final Button mainButton = addButton;
            final Button dropdownButton = menuButton == null ? mainButton : menuButton;

            dropdownButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Menu menu = new Menu(shell, SWT.POP_UP);
                    if (!multipleRefs) {
                        MenuItem item = new MenuItem(menu, SWT.PUSH);
                        item.setText(Messages.referenceNestedButton);
                        item.addListener(SWT.Selection, new Listener() {
                            @Override
                            public void handleEvent(Event e) {
                                Element elem = addNested(input, nestedReferences[0]);
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
                                    referenceSet.add(id);
                                    updateAttr(input, itemName, referenceSet);
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
            data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
            addButton.setLayoutData(data);
            addButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    IDDialog dialog = new IDDialog(shell, typeLabel);
                    if (dialog.open() == IStatus.OK) {
                        String id = dialog.getId();
                        referenceSet.add(id);
                        updateAttr(input, itemName, referenceSet);
                        Element elem = addTopLevel(doc, reference, id, input);
                        openElement(editorPart, elem);
                    }
                }
            });

            addButton.setEnabled(!getReadOnly());
        }

        // Remove button
        final Button removeButton = widgetFactory.createButton(composite, Messages.removeButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        removeButton.setLayoutData(data);

        if (showNestedCount) {
            // Label for nested count
            final Label nestedLabel = widgetFactory.createLabel(composite, NLS.bind(Messages.referenceNestedCount, String.valueOf(nestedCount)));
            data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
            data.horizontalIndent = LEFT_INDENT;
            data.horizontalSpan = 2;
            nestedLabel.setLayoutData(data);
        }

        // Listeners
        refTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateRemoveButton(removeButton, refTable);
            }
        });

        refTable.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.keyCode == SWT.F3) {
                    TableItem[] selections = refTable.getSelection();
                    if (selections.length == 1) {
                        String name = selections[0].getText();
                        String varName = ConfigVarsUtils.getVariableName(name);
                        URILocation location = null;
                        if (varName != null && !varName.isEmpty()) {
                            location = configVarComputer.getConfigVars().getDocumentLocation(varName);
                        } else if (idMap.containsKey(name)) {
                            location = idMap.get(name);
                        }
                        if (location != null && location.getURI() != null) {
                            Activator.open(location);
                        }
                    }
                }
            }
        });

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Only show ids and variables that are not currently references.
                Set<String> ids = new HashSet<String>(idMap.keySet());
                ids.removeAll(referenceSet);
                Set<String> vars = new HashSet<String>(configVarComputer.getConfigVars().getVars(ConfigVars.STRING_TYPES, false));
                for (String ref : referenceSet) {
                    String varName = ConfigVarsUtils.getVariableName(ref);
                    if (varName != null && !varName.isEmpty()) {
                        vars.remove(varName);
                    }
                }
                ReferenceSelectionDialog dialog = new ReferenceSelectionDialog(shell, ids, vars, configVarComputer.getConfigVars(), true);
                if (dialog.open() == IStatus.OK) {
                    Set<String> newRefs = dialog.getRefs();
                    referenceSet.addAll(newRefs);
                    updateAttr(input, itemName, referenceSet);
                    createItems(refTable, referenceSet, configVarComputer.getConfigVars());
                    updateRemoveButton(removeButton, refTable);
                }
            }
        });

        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] items = refTable.getSelection();
                for (TableItem item : items) {
                    referenceSet.remove(item.getText());
                }
                updateAttr(input, itemName, referenceSet);
                createItems(refTable, referenceSet, configVarComputer.getConfigVars());
                updateRemoveButton(removeButton, refTable);
            }
        });

        browseButton.setEnabled(!getReadOnly());
        removeButton.setEnabled(false);
    }

    protected void createItems(Table table, Set<String> refs, ConfigVars vars) {
        // Create the items for the table.
        ArrayList<String> refList = new ArrayList<String>(refs);
        Collections.sort(refList);
        table.removeAll();
        for (String ref : refList) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(ref);
            String varName = ConfigVarsUtils.getVariableName(ref);
            if (varName != null) {
                item.setImage(Activator.getImage(Activator.IMG_VARIABLE_REF));
                String value = vars.getValue(varName);
                if (value != null && !value.isEmpty()) {
                    item.setData(HoverHelper.HOVER_DATA, NLS.bind(Messages.variableValue, new String[] { varName, "\"" + value + "\"" }));
                }
            } else {
                item.setImage(Activator.getImage(Activator.IMG_FACTORY_REF));
            }
        }
    }

    protected void updateRemoveButton(Button removeButton, Table refTable) {
        removeButton.setEnabled(!getReadOnly() && refTable.getSelectionCount() > 0);
    }

    protected void updateAttr(Element elem, String attrName, Set<String> refs) {
        // Update the attribute value.
        if (refs.isEmpty()) {
            elem.removeAttribute(attrName);
            return;
        }
        List<String> refList = new ArrayList<String>();
        refList.addAll(refs);
        Collections.sort(refList);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String ref : refList) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(ref);
        }
        elem.setAttribute(attrName, builder.toString());
    }
}
