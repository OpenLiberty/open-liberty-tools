/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.eclipse.wst.server.core.IRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.IWebSphereMetadataListener;
import com.ibm.ws.st.core.internal.ServerListenerUtil;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * Custom control for features.
 */
public class FeatureCustomObject extends BaseCustomObject {
    protected static final String GRAYED = "grayed_table_item";
    protected static final String SAVE_SHOW_IMPLICIT = "save_show_implicit_features";
    protected static final String PREFERENCES_IMPLICIT_FEATURES = "ImplicitFeatures";

    protected WebSphereRuntime wsRuntime;

    /** {@inheritDoc} */
    @Override
    public void createCustomControl(final Element input, String itemName, Composite parent, IEditorPart editorPart, EventListener listener) {
        assert (itemName.equals(Constants.FEATURE));

        wsRuntime = ConfigUIUtils.getRuntime(editorPart);

        final Shell shell = parent.getShell();

        boolean showImplicit = false;
        Object obj = input.getUserData(SAVE_SHOW_IMPLICIT);
        if (obj != null && obj instanceof Boolean) {
            showImplicit = ((Boolean) obj).booleanValue();
        } else {
            showImplicit = Activator.getPreferenceBoolean(PREFERENCES_IMPLICIT_FEATURES, false);
        }

        // Map of explicitly enabled features to their corresponding DOM node.
        // This map is kept up to date as user adds/removes features.
        final HashMap<String, Node> featureMap = new HashMap<String, Node>();
        final NodeList featureNodes = input.getElementsByTagName(Constants.FEATURE);
        for (int i = 0; i < featureNodes.getLength(); i++) {
            Node node = featureNodes.item(i);
            String name = DOMUtils.getTextContent(node);
            if (name != null) {
                featureMap.put(name, node);
            }
        }

        TabbedPropertySheetWidgetFactory widgetFactory = new TabbedPropertySheetWidgetFactory();

        // Set the alignment of the label to beginning
        setLabelVerticalAlign(parent, GridData.BEGINNING);

        // Main composite
        Composite composite = widgetFactory.createComposite(parent, SWT.FLAT);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false);
        data.horizontalSpan = 2;
        composite.setLayoutData(data);

        // Multi-selection table for the features
        final Table featureTable = widgetFactory.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = featureTable.getItemHeight() * 8;
        // The table will not draw properly if there is no margin from the
        // composite so add smallest indent.
        data.horizontalIndent = 1;
        data.verticalIndent = LEFT_INDENT;
        data.verticalSpan = 2;
        featureTable.setLayoutData(data);

        FeatureUI.createColumns(featureTable);

        createItems(featureTable, featureMap.keySet(), showImplicit);

        FeatureUI.resizeColumns(featureTable);

        // Buttons
        final Button addButton = widgetFactory.createButton(composite, Messages.addButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        addButton.setLayoutData(data);

        final Button removeButton = widgetFactory.createButton(composite, Messages.removeButton, SWT.PUSH);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        removeButton.setLayoutData(data);

        // Make sure no border is drawn.
        // Should be handled by setting FormToolkit.KEY_DRAW_BORDER to false
        // on the widget, but this doesn't work on some machines so need to set
        // the border style.  Just setting the border style also doesn't work
        // on some machines so both the border style and KEY_DRAW_BORDER need to be
        // set.
        // Eclipse bug 410763 is open for this problem.
        int borderStyle = widgetFactory.getBorderStyle();
        widgetFactory.setBorderStyle(SWT.NONE);

        final Text descriptionLabel = widgetFactory.createText(composite, NLS.bind(Messages.featureDescription, ""), SWT.READ_ONLY);
        descriptionLabel.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalSpan = 2;
        descriptionLabel.setLayoutData(data);

        final Text enablesLabel = widgetFactory.createText(composite, NLS.bind(Messages.featureEnables, ""), SWT.READ_ONLY);
        enablesLabel.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalSpan = 2;
        enablesLabel.setLayoutData(data);

        final Text enabledByLabel = widgetFactory.createText(composite, NLS.bind(Messages.featureEnabledBy, ""), SWT.READ_ONLY);
        enabledByLabel.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalSpan = 2;
        enabledByLabel.setLayoutData(data);

        // Restore the border style.
        widgetFactory.setBorderStyle(borderStyle);

        // Checkbox for showing implicitly enabled features
        final Button showImplicitButton = widgetFactory.createButton(composite, Messages.featureShowImplicit, SWT.CHECK);
        showImplicitButton.setForeground(widgetFactory.getColors().getColor(IFormColors.TITLE));
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalIndent = LEFT_INDENT;
        data.horizontalSpan = 2;
        showImplicitButton.setLayoutData(data);

        // Listeners
        featureTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                updateRemoveButton(removeButton, featureTable);
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, featureMap.keySet(), wsRuntime);
            }
        });

        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Set<String> runtimeFeatures = new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String str1, String str2) {
                        return str1.compareToIgnoreCase(str2);
                    }
                });
                runtimeFeatures.addAll(FeatureList.getRuntimeFeatureSet(wsRuntime));

                Set<String> configuredFeatures = new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String str1, String str2) {
                        return str1.compareToIgnoreCase(str2);
                    }
                });
                configuredFeatures.addAll(featureMap.keySet());

                FeatureUI.AddDialog dialog = new FeatureUI.AddDialog(shell, runtimeFeatures, configuredFeatures, wsRuntime);
                if (dialog.open() == IStatus.OK) {
                    Set<String> newFeatures = dialog.getFeatures();
                    for (String feature : newFeatures) {
                        addNode(feature, input, featureMap);
                    }
                    createItems(featureTable, featureMap.keySet(), showImplicitButton.getSelection());
                    updateRemoveButton(removeButton, featureTable);
                    FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, featureMap.keySet(), wsRuntime);
                }
            }
        });

        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] items = featureTable.getSelection();
                for (TableItem item : items) {
                    removeNode(item.getText(), input, featureMap);
                }
                createItems(featureTable, featureMap.keySet(), showImplicitButton.getSelection());
                updateRemoveButton(removeButton, featureTable);
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, featureMap.keySet(), wsRuntime);
            }
        });

        showImplicitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean showImplicit = showImplicitButton.getSelection();
                Activator.setPreferenceBoolean(PREFERENCES_IMPLICIT_FEATURES, showImplicit);
                createItems(featureTable, featureMap.keySet(), showImplicit);
                input.setUserData(SAVE_SHOW_IMPLICIT, showImplicit ? Boolean.TRUE : Boolean.FALSE, null);
                updateRemoveButton(removeButton, featureTable);
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, featureMap.keySet(), wsRuntime);
            }
        });

        final IPreferenceChangeListener prefListener = new IPreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent event) {
                if (PREFERENCES_IMPLICIT_FEATURES.equals(event.getKey())) {
                    Object newValue = event.getNewValue();
                    if (newValue != null) {
                        boolean newSelection = Boolean.parseBoolean((String) newValue);
                        if (newSelection != showImplicitButton.getSelection()) {
                            showImplicitButton.setSelection(newSelection);
                        }
                    }
                }
            }
        };
        Activator.addPreferenceChangeListener(prefListener);

        showImplicitButton.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent arg0) {
                Activator.removePreferenceChangeListener(prefListener);
            }
        });

        // Handle a runtime change that updates the feature list
        final IWebSphereMetadataListener metadataListener = new IWebSphereMetadataListener() {
            /** {@inheritDoc} */
            @Override
            public void runtimeMetadataChanged(IRuntime runtime) {
                if (wsRuntime == null || !runtime.equals(wsRuntime.getRuntime())) {
                    return;
                }

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        // Save off the current selection
                        Set<String> features = new HashSet<String>();
                        for (TableItem item : featureTable.getSelection()) {
                            features.add(item.getText(0));
                        }

                        // Create the new items
                        createItems(featureTable, featureMap.keySet(), showImplicitButton.getSelection());

                        // Restore the selection
                        List<TableItem> selectedItems = new ArrayList<TableItem>();
                        for (TableItem item : featureTable.getItems()) {
                            if (features.contains(item.getText(0))) {
                                selectedItems.add(item);
                            }
                        }
                        featureTable.setSelection(selectedItems.toArray(new TableItem[selectedItems.size()]));

                        // Update the feature info
                        FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, featureMap.keySet(), wsRuntime);
                    }
                });
            }
        };

        ServerListenerUtil.getInstance().addMetadataListener(metadataListener);

        featureTable.addDisposeListener(new DisposeListener() {
            /** {@inheritDoc} */
            @Override
            public void widgetDisposed(DisposeEvent arg0) {
                ServerListenerUtil.getInstance().removeMetadataListener(metadataListener);
            }
        });

        addButton.setEnabled(!getReadOnly());
        removeButton.setEnabled(false);
        FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, featureMap.keySet(), wsRuntime);
        showImplicitButton.setSelection(showImplicit);
    }

    protected void createItems(Table table, Set<String> features, boolean showImplicit) {
        // Create the items for the table.
        ArrayList<String> featureList = new ArrayList<String>(features.size());
        if (showImplicit) {
            Set<String> allFeatures = getAllFeatures(features);
            featureList.addAll(allFeatures);
        } else {
            featureList.addAll(features);
        }
        Collections.sort(featureList);
        table.removeAll();

        Color gray = table.getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        for (String feature : featureList) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, feature);
            String name = FeatureList.getFeatureDisplayName(feature, wsRuntime);
            if (name != null)
                item.setText(1, name);
            if (FeatureList.isValidFeature(feature, wsRuntime)) {
                if (FeatureList.isFeatureSuperseded(feature, wsRuntime))
                    item.setImage(Activator.getImage(Activator.IMG_FEATURE_SUPERSEDED));
                else
                    item.setImage(Activator.getImage(Activator.IMG_FEATURE_ELEMENT));
                if (!FeatureList.containsIgnoreCase(features, feature)) {
                    item.setForeground(gray);
                    item.setData(GRAYED, Boolean.TRUE);
                }
            } else {
                item.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
            }
        }
    }

    protected void updateRemoveButton(Button removeButton, Table featureTable) {
        // Remove button is enabled only when all selected features are
        // explicitly enabled (can't remove implicitly enabled features).
        TableItem[] items = featureTable.getSelection();
        removeButton.setEnabled(!getReadOnly() && items.length > 0 && !containsGrayed(items));
    }

    protected boolean containsGrayed(TableItem[] items) {
        // Check if any items are implicitly enabled (shown as grayed
        // in the table).
        for (TableItem item : items) {
            Object grayed = item.getData(GRAYED);
            if (grayed != null && grayed instanceof Boolean && ((Boolean) grayed).booleanValue()) {
                return true;
            }
        }
        return false;
    }

    protected Set<String> getAllFeatures(Set<String> explicitFeatures) {
        // Given a set of explicit features get all of the features
        // that are enabled including implicit ones.
        Set<String> allFeatures = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String str1, String str2) {
                return str1.compareToIgnoreCase(str2);
            }
        });
        allFeatures.addAll(explicitFeatures);
        for (String feature : explicitFeatures) {
            allFeatures.addAll(FeatureList.getFeatureChildren(feature, wsRuntime));
        }
        return allFeatures;
    }

    protected void addNode(String feature, Element parent, HashMap<String, Node> featureMap) {
        // Add a node to the document along with appropriate text nodes to align
        // it properly.  Also add the node to the feature map.
        Document document = parent.getOwnerDocument();

        Node beforeText = document.createTextNode(featureMap.isEmpty() ? "\n\t\t" : "\t");
        parent.appendChild(beforeText);

        Element elem = document.createElement(Constants.FEATURE);
        Node text = document.createTextNode(feature);
        elem.appendChild(text);
        parent.appendChild(elem);

        Node afterText = document.createTextNode("\n\t");
        parent.appendChild(afterText);

        featureMap.put(feature, elem);
    }

    protected void removeNode(String feature, Element parent, HashMap<String, Node> featureMap) {
        // Remove a node from the document.  Remove text nodes as required to fix up
        // the alignment.  Also remove the node from the feature map.
        Node node = featureMap.get(feature);
        featureMap.remove(feature);

        Node beforeText = node.getPreviousSibling();
        while (beforeText != null && beforeText.getNodeType() == Node.TEXT_NODE) {
            Node tmpText = beforeText.getPreviousSibling();
            parent.removeChild(beforeText);
            beforeText = tmpText;
        }
        if (featureMap.isEmpty()) {
            Node afterText = node.getNextSibling();
            while (afterText != null && afterText.getNodeType() == Node.TEXT_NODE) {
                Node tmpText = afterText.getNextSibling();
                parent.removeChild(afterText);
                afterText = tmpText;
            }
        }

        parent.removeChild(node);
    }
}
