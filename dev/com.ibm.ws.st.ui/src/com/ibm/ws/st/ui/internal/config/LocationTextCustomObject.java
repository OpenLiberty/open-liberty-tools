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
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.osgi.framework.Bundle;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.xwt.dde.customization.IAdvancedCustomizationObject;

/**
 * Custom control for locations.
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class LocationTextCustomObject extends BaseCustomObject {

    protected enum LocationType {
        FILE,
        FOLDER,
        ALL
    }

    protected LocationType getLocationType() {
        return LocationType.ALL;
    }

    protected String getClassName() {
        return "com.ibm.ws.st.ui.internal.config.GenericLocationBrowser";
    }

    protected final static HashMap<String, String> dialogHash = new HashMap<String, String>();
    protected final static HashMap<String, LocationType> dialogType = new HashMap<String, LocationType>();

    static {
        dialogHash.put("include", "com.ibm.ws.st.ui.internal.config.IncludeBrowser");
        dialogHash.put("applicationMonitor", "com.ibm.ws.st.ui.internal.config.AppMonitorBrowser");
        dialogHash.put("application", "com.ibm.ws.st.ui.internal.config.ApplicationLocationBrowser");
        dialogHash.put("logging", "com.ibm.ws.st.ui.internal.config.GenericDirBrowser");
        dialogHash.put("transaction", "com.ibm.ws.st.ui.internal.config.GenericDirBrowser");
        dialogHash.put("fileset", "com.ibm.ws.st.ui.internal.config.FilesetDirBrowser");
        dialogHash.put("keyStore", "com.ibm.ws.st.ui.internal.config.GenericFileBrowser");
        dialogHash.put("ltpa", "com.ibm.ws.st.ui.internal.config.GenericFileBrowser");
        dialogHash.put("file", "com.ibm.ws.st.ui.internal.config.SharedFileBrowser");
        dialogHash.put("folder", "com.ibm.ws.st.ui.internal.config.SharedFolderBrowser");

        dialogType.put("include", LocationType.FILE);
        dialogType.put("applicationMonitor", LocationType.FOLDER);
        dialogType.put("application", LocationType.ALL);
        dialogType.put("logging", LocationType.FOLDER);
        dialogType.put("transaction", LocationType.FOLDER);
        dialogType.put("fileset", LocationType.FOLDER);
        dialogType.put("keyStore", LocationType.FILE);
        dialogType.put("ltpa", LocationType.FILE);
        dialogType.put("file", LocationType.FILE);
        dialogType.put("folder", LocationType.FOLDER);
    }

    @Override
    public void createCustomControl(final Element input, final String itemName, final Composite composite, final IEditorPart editorPart, final EventListener listener) {

        // Don't include variables defined in the configuration files if this is
        // an include location
        boolean isIncludeLocation = Constants.INCLUDE_ELEMENT.equals(input.getNodeName()) && Constants.LOCATION_ATTRIBUTE.equals(itemName);
        ConfigVarComputer configVarComputer = getConfigVarComputer(isIncludeLocation ? null : input, isIncludeLocation ? null : itemName, editorPart);

        String attrType = null;
        String defaultValue = null;
        CMAttributeDeclaration attrDecl = getAttrDecl(input, itemName);
        if (attrDecl != null) {
            attrType = ConfigUtils.getTypeName(attrDecl);
            defaultValue = attrDecl.getDefaultValue();
        }

        Attr attr = input.getAttributeNode(itemName);
        String value = defaultValue;
        if (attr != null) {
            value = attr.getNodeValue();
        }

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Set the alignment of the label to center
        setLabelVerticalAlign(composite, GridData.CENTER);

        final Text textControl = widgetFactory.createText(composite, "");
        if (value != null && !value.isEmpty()) {
            textControl.setText(value);
            textControl.setToolTipText(value);
        }
        TextModifiers.addVariableContentProposalProvider(textControl, configVarComputer, attrType);
        TextModifiers.addVariableHyperlink(textControl, configVarComputer);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        data.horizontalIndent = LEFT_INDENT;
        textControl.setLayoutData(data);

        textControl.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = textControl.getText();
                Attr attr = input.getAttributeNode(itemName);
                if (text == null || text.isEmpty()) {
                    if (attr != null) {
                        input.removeAttributeNode(attr);
                    }
                } else {
                    input.setAttribute(itemName, text);
                }
                textControl.setToolTipText(text);
            }
        });

        String className = dialogHash.get(input.getNodeName());
        if (className == null) {
            className = getClassName();
        }

        final Class<?> dialogClass = getDialogClass(className);
        if (dialogClass == null) {
            fixTextSpan(textControl, 2);
            return;
        }

        LocationType type = dialogType.get(input.getNodeName());
        if (type == null)
            type = getLocationType();
        final LocationType locationType = type;

        final Composite buttonComposite = widgetFactory.createComposite(composite);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.numColumns = 2;
        buttonComposite.setLayout(layout);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = false;
        buttonComposite.setLayoutData(data);

        final Button browseButton = widgetFactory.createButton(buttonComposite, Messages.editorBrowse, SWT.PUSH);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = false;
        browseButton.setLayoutData(data);

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String result = openDialog(dialogClass, input, itemName, editorPart, textControl.getText());
                if (result != null && !result.isEmpty()) {
                    textControl.setText(result);
                }
            }
        });

        final Button dropdownButton = widgetFactory.createButton(buttonComposite, "", SWT.PUSH);
        setDropdownData(browseButton, dropdownButton);

        dropdownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Menu menu = new Menu(composite.getShell(), SWT.POP_UP);
                MenuItem item = new MenuItem(menu, SWT.PUSH);
                item.setText(Messages.relPathButton);
                item.addListener(SWT.Selection, new Listener() {
                    @Override
                    public void handleEvent(Event e) {
                        String result = openDialog(dialogClass, input, itemName, editorPart, textControl.getText());
                        if (result != null) {
                            textControl.setText(result);
                        }
                    }
                });
                menu.setDefaultItem(item);
                if (locationType == LocationType.ALL) {
                    item = new MenuItem(menu, SWT.PUSH);
                    item.setText(Messages.absFilePathButton);
                    item.addListener(SWT.Selection, new Listener() {
                        @Override
                        public void handleEvent(Event e) {
                            FileDialog fileDialog = new FileDialog(buttonComposite.getShell());
                            String path = fileDialog.open();
                            if (path != null && !path.isEmpty()) {
                                textControl.setText(path);
                            }
                        }
                    });
                    item = new MenuItem(menu, SWT.PUSH);
                    item.setText(Messages.absFolderPathButton);
                    item.addListener(SWT.Selection, new Listener() {
                        @Override
                        public void handleEvent(Event e) {
                            DirectoryDialog dirDialog = new DirectoryDialog(buttonComposite.getShell());
                            dirDialog.setMessage(Messages.absFolderPathMessage);
                            String path = dirDialog.open();
                            if (path != null && !path.isEmpty()) {
                                textControl.setText(path);
                            }
                        }
                    });
                } else {
                    item = new MenuItem(menu, SWT.PUSH);
                    item.setText(Messages.absPathButton);
                    item.addListener(SWT.Selection, new Listener() {
                        @Override
                        public void handleEvent(Event e) {
                            String path = null;
                            switch (locationType) {
                                case FILE:
                                    FileDialog fileDialog = new FileDialog(buttonComposite.getShell());
                                    path = fileDialog.open();
                                    break;
                                case FOLDER:
                                    DirectoryDialog dirDialog = new DirectoryDialog(buttonComposite.getShell());
                                    dirDialog.setMessage(Messages.absFolderPathMessage);
                                    path = dirDialog.open();
                                    break;
                                default:
                                    // This should never happen
                                    if (Trace.ENABLED) {
                                        Trace.trace(Trace.WARNING, "Encountered unexpected location type when creating drop down menu for location text: " + locationType);
                                    }
                                    break;
                            }
                            if (path != null && !path.isEmpty()) {
                                textControl.setText(path);
                            }
                        }
                    });
                }
                displayDropdownMenu(browseButton, menu, false);
                menu.dispose();
            }
        });

        textControl.setEnabled(!getReadOnly());
        browseButton.setEnabled(!getReadOnly());
        dropdownButton.setEnabled(!getReadOnly());
    }

    protected void fixTextSpan(Text textControl, int span) {
        GridData data = (GridData) textControl.getLayoutData();
        data.horizontalSpan = span;
    }

    protected Class<?> getDialogClass(String className) {
        Class<?> dialogClass = null;
        Bundle bundle = Activator.getInstance().getBundle();
        try {
            dialogClass = bundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            // This should never happen
            Trace.logError("Failed to load class for: " + className, e);
        }
        return dialogClass;
    }

    protected String openDialog(Class<?> dialogClass, Element input, String itemName, IEditorPart editorPart, String text) {
        String result = null;
        try {
            Object dialog = dialogClass.newInstance();
            if (dialog instanceof IAdvancedCustomizationObject) {
                Attr attr = input.getAttributeNode(itemName);
                result = ((IAdvancedCustomizationObject) dialog).invoke(text, attr, input, editorPart);
            } else {
                // This should never happen
                Trace.logError("Dialog class is not an instance of IAdvancedCustomizationObject: " + dialogClass.getName(), null);
            }
        } catch (IllegalAccessException e1) {
            Trace.logError("Failed to create dialog instance: " + dialogClass.getName(), e1);
        } catch (InstantiationException e1) {
            Trace.logError("Failed to create dialog instance: " + dialogClass.getName(), e1);
        }
        return result;
    }
}
