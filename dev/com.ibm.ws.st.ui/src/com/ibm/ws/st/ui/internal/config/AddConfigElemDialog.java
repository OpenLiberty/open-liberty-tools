/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PaintObjectEvent;
import org.eclipse.swt.custom.PaintObjectListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.internal.customization.CustomizationManager.Customization;
import com.ibm.xwt.dde.internal.util.ModelUtil;

/**
 * Dialog to replace the editor Add dialog for adding child elements.
 * Allows more flexibility in how the elements are displayed in the dialog.
 */
@SuppressWarnings("restriction")
public class AddConfigElemDialog extends TitleAreaDialog {

    private static final String CMELEMENT_DECLARATION_DATA = "cmelementDeclarationData";
    private static final String HISTORY_KEY = "com.ibm.ws.st.ui.internal.config.addConfigElementDialog.history";
    private static final int HISTORY_MAX_SIZE = 10;
    private static final int DESCRIPTION_HEIGHT = 80;
    private static final String[] HISTORY_DEFAULT_ELEMS = new String[] { Constants.QUICK_START_SECURITY, Constants.BASIC_USER_REGISTY,
                                                                         Constants.DATA_SOURCE, Constants.SHARED_LIBRARY, Constants.VARIABLE_ELEMENT };

    protected final Element element;
    protected final String elemPath;
    protected CMElementDeclaration newItem;
    protected SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
    protected Text filterText;
    protected Table itemTable;
    protected StyledText descriptionText;
    protected Customization customization = ConfigUIUtils.getCustomization();
    protected Color gray;
    protected Image selectedItemImage;

    public AddConfigElemDialog(Shell parent, Element element) {
        super(parent);
        this.element = element;
        this.elemPath = ConfigUIUtils.getElementFullPath(element);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.addElementTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.addElementLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(Messages.addElementMessage);

        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // Context label
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
        String label = ConfigUIUtils.getTreeLabel(modelQuery.getCMElementDeclaration(element), elemPath, customization, false);
        Label contextLabel = new Label(composite, SWT.NONE);
        contextLabel.setText(NLS.bind(Messages.addElementContextLabel, label));
        contextLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        // Filter text
        filterText = new Text(composite, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        filterText.setLayoutData(data);
        filterText.setMessage(Messages.filterMessage);

        // Item table
        itemTable = new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 300;
        itemTable.setLayoutData(data);

        // Initialize history with defaults if necessary
        IDialogSettings settings = Activator.getInstance().getDialogSettings();
        String[] history = settings.getArray(HISTORY_KEY);
        if (history == null)
            settings.put(HISTORY_KEY, HISTORY_DEFAULT_ELEMS);

        Color bg = itemTable.getBackground();
        Color fg = itemTable.getForeground();
        gray = new Color(bg.getDevice(), (bg.getRed() + fg.getRed()) / 2, (bg.getGreen() + fg.getGreen()) / 2, (bg.getBlue() + fg.getBlue()) / 2);

        createItems(itemTable, element, "");

        descriptionText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        descriptionText.setTopMargin(3);
        descriptionText.setLeftMargin(5);
        descriptionText.setRightMargin(5);
        descriptionText.setAlwaysShowScrollBars(false);
        data = new GridData(GridData.FILL_BOTH);
        data.minimumHeight = DESCRIPTION_HEIGHT;
        descriptionText.setLayoutData(data);
        descriptionText.setBackground(parent.getBackground());

        descriptionText.addPaintObjectListener(new PaintObjectListener() {
            @Override
            public void paintObject(PaintObjectEvent event) {

                // Render icon in description
                GC gc = event.gc;
                StyleRange style = event.style;
                int x = event.x;
                int y = event.y + event.ascent - style.metrics.ascent;
                gc.drawImage(selectedItemImage, x, y + 3);
            }
        });

        // Listeners
        filterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = filterText.getText();
                if (text == null) {
                    text = "";
                }
                createItems(itemTable, element, text);

                TableItem[] allItems = itemTable.getItems();
                if (allItems.length > 0) {
                    int i = -1;
                    TableItem item = null;
                    for (;;) {
                        item = allItems[++i];
                        String label = item.getText();
                        if (label.startsWith("--- ") && (label.contains(Messages.addElementRecentlyAddedElementsLabel) ||
                                                         label.contains(Messages.addElementAllElementsLabel))) {
                            if (i + 1 >= allItems.length)
                                break;
                        } else {
                            break;
                        }
                    }
                    itemTable.select(i);
                }

                updateInfo(itemTable);
                TableItem[] items = itemTable.getSelection();
                enableOKButton(items.length > 0 && items[0].getData(CMELEMENT_DECLARATION_DATA) != null);
            }
        });

        filterText.addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.keyCode == SWT.ARROW_DOWN) {
                    if (itemTable.getItemCount() > 0) {
                        itemTable.setSelection(0);
                        itemTable.setFocus();
                    }
                    event.doit = false;
                }
            }
        });

        itemTable.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TableItem[] items = itemTable.getSelection();
                enableOKButton(items.length > 0 && items[0].getData(CMELEMENT_DECLARATION_DATA) != null);
                updateInfo(itemTable);
            }
        });

        itemTable.addListener(SWT.DefaultSelection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                okPressed();
            }
        });

        itemTable.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent notUsed) {
                gray.dispose();
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
        updateInfo(itemTable);
        filterText.setFocus();
    }

    @Override
    protected void okPressed() {
        TableItem[] items = itemTable.getSelection();
        newItem = (CMElementDeclaration) items[0].getData(CMELEMENT_DECLARATION_DATA);
        if (newItem != null) {
            updateHistory(newItem.getElementName());
            super.okPressed();
        }
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    protected void createItems(Table table, Element element, String filter) {
        // Get the schema element declaration
        ModelQuery modelQuery = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
        CMElementDeclaration cmElementDeclaration = modelQuery.getCMElementDeclaration(element);

        // Get a map of the elements that are available
        List<ModelQueryAction> modelQueryActions = new ArrayList<ModelQueryAction>();
        HashMap<String, CMElementDeclaration> labelMap = new HashMap<String, CMElementDeclaration>();
        final HashMap<String, String> nameMap = new HashMap<String, String>();
        modelQuery.getInsertActions(element, cmElementDeclaration, -1, ModelQuery.INCLUDE_CHILD_NODES, ModelQuery.VALIDITY_STRICT, modelQueryActions);
        Iterator<ModelQueryAction> iterator = modelQueryActions.iterator();
        while (iterator.hasNext()) {
            ModelQueryAction modelQueryAction = iterator.next();
            CMNode node = modelQueryAction.getCMNode();
            if (node instanceof CMElementDeclaration) {
                CMElementDeclaration elemDecl = (CMElementDeclaration) node;
                if (ModelUtil.elementMustAppearInTree(customization, element, cmElementDeclaration, elemDecl)) {
                    String label = ConfigUIUtils.getTreeLabel(elemDecl, getChildPath(node), customization, true);
                    labelMap.put(label, elemDecl);
                    nameMap.put(elemDecl.getElementName(), label);
                }
            }
        }

        // Sort the keys (labels for the elements)
        Set<String> labelSet = labelMap.keySet();
        ArrayList<String> labelList = new ArrayList<String>(labelSet);
        Collections.sort(labelList, new Comparator<String>() {
            @Override
            public int compare(String str1, String str2) {
                return str1.compareToIgnoreCase(str2);
            }
        });

        table.removeAll();
        pattern.setPattern("*" + filter + "*");

        // If top level, add history items in order if the filter is matched
        int separatorIndex = -1;
        List<String> history = Collections.emptyList();
        if (Constants.SERVER_ELEMENT.equals(element.getNodeName())) {
            history = getHistory();
            Collections.sort(history, new Comparator<String>() {
                @Override
                public int compare(String str1, String str2) {

                    String labelA = nameMap.get(str1);
                    if (labelA == null) {
                        labelA = str1;
                    }
                    String labelB = nameMap.get(str2);
                    if (labelB == null) {
                        labelB = str2;
                    }

                    return labelA.compareToIgnoreCase(labelB);
                }
            });
            int count = 0;
            for (String elem : history) {
                String label = nameMap.get(elem);
                if (label != null) {
                    if (addItem(label, labelMap.get(label), table))
                        count++;
                }
            }
            // Add a separators
            if (count > 0) {
                TableItem item = new TableItem(table, SWT.NONE, 0);
                item.setText("--- " + Messages.addElementRecentlyAddedElementsLabel + " ---");
                item.setForeground(gray);

                separatorIndex = table.getItemCount();

                item = new TableItem(table, SWT.NONE);
                item.setText("--- " + Messages.addElementAllElementsLabel + " ---");
                item.setForeground(gray);
            }
        }

        // Add items in order of sorted labels if the filter is matched
        int count = 0;
        for (String label : labelList) {
            if (addItem(label, labelMap.get(label), table))
                count++;
        }

        if (count == 0 && separatorIndex != -1)
            table.remove(separatorIndex);
    }

    protected boolean addItem(String label, CMElementDeclaration node, Table table) {
        String name = node.getElementName();
        String description = SchemaUtil.getDocumentation(node);
        if (pattern.matches(name) || (label != null && pattern.matches(label)) || (description != null && pattern.matches(description))) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(label);
            item.setImage(ConfigUIUtils.getTreeIcon(node, getChildPath(node), customization));
            item.setData(CMELEMENT_DECLARATION_DATA, node);
            return true;
        }
        return false;
    }

    protected void updateHistory(String elemName) {
        IDialogSettings settings = Activator.getInstance().getDialogSettings();
        String[] history = settings.getArray(HISTORY_KEY);
        ArrayList<String> newHistory = new ArrayList<String>(HISTORY_MAX_SIZE);
        newHistory.add(elemName);
        int size = 1;
        for (int i = 0; i < history.length && size < HISTORY_MAX_SIZE; i++, size++) {
            if (!newHistory.contains(history[i]))
                newHistory.add(history[i]);
        }
        settings.put(HISTORY_KEY, newHistory.toArray(new String[newHistory.size()]));
    }

    protected List<String> getHistory() {
        IDialogSettings settings = Activator.getInstance().getDialogSettings();
        String[] history = settings.getArray(HISTORY_KEY);
        return Arrays.asList(history);
    }

    protected String getChildPath(CMNode node) {
        return elemPath + "/" + node.getNodeName();
    }

    protected void updateInfo(Table itemTable) {
        // Update the description based on the currently selected element
        TableItem[] items = itemTable.getSelection();
        CMElementDeclaration node = null;
        if (items.length > 0) {
            node = (CMElementDeclaration) items[0].getData(CMELEMENT_DECLARATION_DATA);
        }
        if (node != null) {
            String description = SchemaUtil.getDocumentation(node);
            if (description == null || description.isEmpty())
                description = Messages.featureNoDescriptionAvailable;

            // Update text
            String styledDescription = NLS.bind(Messages.featureRichFormatDescription, new String[] { items[0].getText(), node.getNodeName(), description });
            descriptionText.setText(styledDescription);

            // Update image
            selectedItemImage = items[0].getImage();

            // Apply bold style to first line
            StyleRange style = new StyleRange();
            style.start = 1;
            style.length = styledDescription.indexOf('\n');
            style.fontStyle = SWT.BOLD;
            descriptionText.setStyleRange(style);

            // Apply color style to element name
            style = new StyleRange();
            style.start = styledDescription.indexOf('(');
            style.length = node.getNodeName().length() + 2;
            style.foreground = getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
            style.fontStyle = SWT.BOLD;
            descriptionText.setStyleRange(style);

        } else {

            // Set generic information message and icon
            descriptionText.setText(Messages.featureRichFormatEmptySelection);
            selectedItemImage = Activator.getImage(Activator.IMG_INFORMATION);

        }

        // Set image placeholder
        StyleRange style = new StyleRange();
        style.start = 0;
        style.length = 1;
        style.metrics = new GlyphMetrics(16, 0, 16);
        descriptionText.setStyleRange(style);

    }

    public CMElementDeclaration getNewItem() {
        return newItem;
    }
}
