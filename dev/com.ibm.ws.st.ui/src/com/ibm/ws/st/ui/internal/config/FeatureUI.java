/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.TaskModel;

import com.ibm.ws.st.core.internal.RuntimeFeatureResolver;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.config.FeatureConflictDialog.ConflictInfo;
import com.ibm.ws.st.ui.internal.config.FeatureConflictDialog.FeatureItemData;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite;
import com.ibm.ws.st.ui.internal.download.DownloadHelper;
import com.ibm.ws.st.ui.internal.download.DownloadUI;
import com.ibm.ws.st.ui.internal.download.SiteHelper;

/**
 * Helper class for UIs that show features.
 */
public class FeatureUI {
    /**
     * Format a set of features to show in enables/enabledBy labels.
     *
     * @param features
     * @return a display string
     */
    public static String formatList(Set<String> features) {
        // Format a list of features to show in the enables/enabledBy labels
        ArrayList<String> featureList = new ArrayList<String>(features.size());
        featureList.addAll(features);
        Collections.sort(featureList);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String feature : featureList) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(feature);
        }
        return builder.toString();
    }

    public static void createColumns(final Table featureTable) {
        final TableColumn featureColumn = new TableColumn(featureTable, SWT.NONE);
        featureColumn.setText(Messages.featureNameColumn);
        featureColumn.setResizable(true);
        featureColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                sortTable(featureTable, featureColumn);
            }
        });
        final TableColumn nameColumn = new TableColumn(featureTable, SWT.NONE);
        nameColumn.setText(Messages.featureDisplayNameColumn);
        nameColumn.setResizable(true);
        nameColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                sortTable(featureTable, nameColumn);
            }
        });

        featureTable.setHeaderVisible(true);
        featureTable.setLinesVisible(false);
        featureTable.setSortDirection(SWT.DOWN);
        featureTable.setSortColumn(featureColumn);
    }

    /**
     * Setup some nice initial column widths: proportional to the minimum width of each column, but
     * expanded to fill the full width. Minimum widths are respected, so there may be a scroll bar
     * if the table is too narrow.
     *
     * @param table the table
     */
    public static void resizeColumns(Table table) {
        TableLayout tableLayout = new TableLayout();

        int numColumns = table.getColumnCount();
        for (int i = 0; i < numColumns; i++)
            table.getColumn(i).pack();

        for (int i = 0; i < numColumns; i++) {
            int w = Math.max(75, table.getColumn(i).getWidth());
            tableLayout.addColumnData(new ColumnWeightData(w, w, true));
        }

        table.setLayout(tableLayout);
    }

    public static void sortTable(Table table, TableColumn column) {
        TableItem[] items = table.getItems();
        int rows = items.length;
        int dir = table.getSortDirection() == SWT.DOWN ? 1 : -1;
        TableColumn currentColumn = table.getSortColumn();
        int columnNum = 0;
        for (int j = 0; j < table.getColumnCount(); j++) {
            if (table.getColumn(j).equals(column)) {
                columnNum = j;
                break;
            }
        }
        if (column.equals(currentColumn))
            dir = -dir;
        else
            dir = 1;

        // sort an index map, then move the actual rows
        int[] map = new int[rows];
        for (int i = 0; i < rows; i++)
            map[i] = i;

        for (int i = 0; i < rows - 1; i++) {
            for (int j = i + 1; j < rows; j++) {
                TableItem a = items[map[i]];
                TableItem b = items[map[j]];
                if ((a.getText(columnNum).compareTo(b.getText(columnNum)) * dir > 0)) {
                    int t = map[i];
                    map[i] = map[j];
                    map[j] = t;
                }
            }
        }

        // can't move existing items or delete first, so append new items to the end and then delete existing rows
        for (int i = 0; i < rows; i++) {
            int n = map[i];
            TableItem item = new TableItem(table, SWT.NONE);
            for (int j = 0; j < table.getColumnCount(); j++)
                item.setText(j, items[n].getText(j));

            item.setImage(items[n].getImage());
            item.setForeground(items[n].getForeground());
            item.setBackground(items[n].getBackground());
            item.setGrayed(items[n].getGrayed());
            item.setChecked(items[n].getChecked());
            item.setData(items[n].getData());
            items[n].dispose();
        }

        table.setSortDirection(dir == 1 ? SWT.DOWN : SWT.UP);
        table.setSortColumn(column);
    }

    private static String getEnables(TableItem[] items, WebSphereRuntime wsRuntime) {
        // Get all the features enabled by the given set of selected items.
        Set<String> enables = new HashSet<String>();
        for (TableItem item : items)
            enables.addAll(FeatureList.getFeatureChildren(getItemText(item), wsRuntime));

        // remove the selected features
        for (TableItem item : items) {
            Iterator<String> itr = enables.iterator();
            while (itr.hasNext()) {
                String feature = itr.next();
                if (feature.equalsIgnoreCase(getItemText(item))) {
                    itr.remove();
                }
            }
        }

        return formatList(enables);
    }

    private static String getEnabledBy(TableItem[] items, WebSphereRuntime wsRuntime) {
        // Get all the features that enable the given set of selected items.
        Set<String> enabledBy = new HashSet<String>();
        for (TableItem item : items)
            enabledBy.addAll(FeatureList.getFeatureParents(getItemText(item), wsRuntime));

        // remove the selected features
        for (TableItem item : items) {
            Iterator<String> itr = enabledBy.iterator();
            while (itr.hasNext()) {
                String feature = itr.next();
                if (feature.equalsIgnoreCase(getItemText(item))) {
                    itr.remove();
                }
            }
        }

        return formatList(enabledBy);
    }

    private static String getConflictsWith(TableItem[] items) {
        // Get all the features that conflict with the given set of selected items.
        Set<String> conflictsWith = new HashSet<String>();
        for (TableItem item : items) {
            List<ConflictInfo> conflictList;
            if (item.getData() != null && (conflictList = ((FeatureItemData) item.getData()).getConflictInfo()) != null) {
                for (ConflictInfo conflict : conflictList) {
                    if (conflict.enablingFeature != null)
                        conflictsWith.add(conflict.enablingFeature);
                    if (conflict.feature != null)
                        conflictsWith.add(conflict.feature);
                }
            }
        }

        // remove the selected features
        for (TableItem item : items) {
            Iterator<String> itr = conflictsWith.iterator();
            while (itr.hasNext()) {
                String feature = itr.next();
                if (feature.equalsIgnoreCase(((FeatureItemData) item.getData()).getFeatureName())) {
                    itr.remove();
                }
            }
        }

        return formatList(conflictsWith);
    }

    private static String getDescription(TableItem[] items, WebSphereRuntime wsRuntime) {
        if (items.length != 1)
            return null;

        String text = getItemText(items[0]);
        return FeatureList.getFeatureDescription(text, wsRuntime);
    }

    public static void updateInfo(Text enablesLabel, Text enabledByLabel, Text descriptionLabel, Table featureTable, Set<String> featureSet,
                                  WebSphereRuntime wsRuntime) {
        updateInfo(enablesLabel, enabledByLabel, descriptionLabel, null, null, featureTable, featureSet, wsRuntime);
    }

    public static void updateInfo(Text enablesLabel, Text enabledByLabel, Text descriptionLabel, Text conflictsWithLabel, Link conflictLink, Table featureTable,
                                  Set<String> featureSet,
                                  WebSphereRuntime wsRuntime) {
        // Update the enables and enabled by info
        TableItem[] items = featureTable.getSelection();
        String enables = "";
        String enabledBy = "";
        String description = "";
        String conflictsWith = "";
        boolean enabled = false;
        if (items.length > 0) {
            enabled = true;
            enables = getEnables(items, wsRuntime);
            enabledBy = getEnabledBy(items, wsRuntime);
            conflictsWith = getConflictsWith(items);
            description = getDescription(items, wsRuntime);
            enables = enables.isEmpty() ? Messages.featureNone : enables;
            enabledBy = enabledBy.isEmpty() ? Messages.featureNone : enabledBy;
            if (description == null || description.isEmpty())
                description = Messages.featureNone;
        }
        String text = NLS.bind(Messages.featureEnables, enables);
        String shortenedText = SWTUtil.shortenText(enablesLabel, text, Messages.featureEnables.length());
        enablesLabel.setText(shortenedText);
        //set width same as shortened line width
        enablesLabel.setToolTipText(SWTUtil.formatTooltip(enables, shortenedText.length()));
        enablesLabel.setEnabled(enabled);
        text = NLS.bind(Messages.featureEnabledBy, enabledBy);
        shortenedText = SWTUtil.shortenText(enabledByLabel, text, Messages.featureEnabledBy.length());
        enabledByLabel.setText(shortenedText);
        enabledByLabel.setToolTipText(SWTUtil.formatTooltip(enabledBy, shortenedText.length()));
        enabledByLabel.setEnabled(enabled);
        text = NLS.bind(Messages.featureDescription, description);
        //default tool tip width size  70
        int toolTipWidth = 70;
        if ((descriptionLabel.getStyle() & SWT.WRAP) == 0) {
            shortenedText = SWTUtil.shortenText(descriptionLabel, text, Messages.featureDescription.length());
            descriptionLabel.setText(shortenedText);
            toolTipWidth = shortenedText.length();
        } else {
            descriptionLabel.setText(text);

            // resize label to make scroll bars appear
            int width = descriptionLabel.getParent().getClientArea().width;
            descriptionLabel.setSize(width, descriptionLabel.computeSize(width, SWT.DEFAULT).y);

            // resize again if scroll bar added or removed
            int newWidth = descriptionLabel.getParent().getClientArea().width;

            if (newWidth != width) {
                descriptionLabel.setSize(newWidth, descriptionLabel.computeSize(newWidth, SWT.DEFAULT).y);
            }
        }
        descriptionLabel.setToolTipText(SWTUtil.formatTooltip(description, toolTipWidth));
        descriptionLabel.setEnabled(enabled);

        if (conflictsWithLabel != null && conflictLink != null) {
            if (items.length == 1 && !conflictsWith.isEmpty()) {
                text = NLS.bind(Messages.featureConflictsWith, conflictsWith);
                conflictsWithLabel.setText(text);
                shortenedText = SWTUtil.shortenText(enabledByLabel, text, Messages.featureConflictsWith.length());
                conflictsWithLabel.setToolTipText(conflictsWith);
                conflictsWithLabel.setEnabled(enabled);
                conflictsWithLabel.setVisible(true);

                List<ConflictInfo> infos = ((FeatureItemData) items[0].getData()).getConflictInfo();
                setupConflictLink(conflictLink, infos, wsRuntime);
                conflictLink.setVisible(true);
            } else {
                conflictsWithLabel.setVisible(false);
                conflictLink.setVisible(false);
            }
        }
    }

    /**
     * @param conflictLink
     */
    private static void setupConflictLink(final Link link, final List<ConflictInfo> infos, final WebSphereRuntime wsRuntime) {

        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Display.getDefault().asyncExec(new Runnable() {
                    final String detailsText = getDetails(infos);
                    final String infoText = getInfo(infos, wsRuntime);

                    @Override
                    public void run() {
                        new PopupDialog(link.getShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, true, false, false, false, false, null, null) {
                            private static final int OFFSET_X = 30;
                            private static final int OFFSET_Y = 5;

                            @Override
                            protected Point getInitialLocation(Point initialSize) {
                                Display display = getShell().getDisplay();
                                Point location = display.getCursorLocation();
                                location.x += OFFSET_X;
                                location.y += OFFSET_Y;
                                return location;
                            }

                            @Override
                            protected Control createDialogArea(Composite parent) {
                                Label label = new Label(parent, SWT.WRAP);
                                label.setText(detailsText + "\n\n" + infoText);
                                label.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent event) {
                                        close();
                                    }
                                });
                                GridData data = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
                                data.widthHint = 500;
                                data.grabExcessVerticalSpace = true;
                                data.horizontalIndent = PopupDialog.POPUP_HORIZONTALSPACING;
                                data.verticalIndent = PopupDialog.POPUP_VERTICALSPACING;
                                label.setLayoutData(data);
                                return label;
                            }
                        }.open();
                    }
                });
            }
        });
    }

    /**
     * @param infos
     * @return
     */
    static String getInfo(List<ConflictInfo> infos, WebSphereRuntime wsRuntime) {
        StringBuffer sb = new StringBuffer();
        for (ConflictInfo info : infos) {
            String dependencyStringA = RuntimeFeatureResolver.getDependencyChainString(info.getFeatureConflict().getDependencyChainA(), wsRuntime);
            String dependencyStringB = RuntimeFeatureResolver.getDependencyChainString(info.getFeatureConflict().getDependencyChainB(), wsRuntime);
            sb.append(Messages.featureConflictDependencyChainMessage + ":\n");
            sb.append(dependencyStringA);
            sb.append("\n");
            sb.append(dependencyStringB);
            sb.append("\n\n");
        }
        return sb.toString();

    }

    static String getDetails(List<ConflictInfo> infos) {
        StringBuffer sb = new StringBuffer();
        for (ConflictInfo info : infos) {
            sb.append(info.getDetails() + "\n");
        }
        return sb.toString();
    }

    private static String getItemText(TableItem tableItem) {
        String text = tableItem.getText();
        Object obj = tableItem.getData();
        if (obj != null && obj instanceof FeatureItemData) {
            text = ((FeatureItemData) obj).getFeatureName();
        }
        return text;
    }

    /************************ AddDialog ***************************/
    public static class AddDialog extends TitleAreaDialog {

        protected final Set<String> configuredFeatures;
        protected Set<String> availableFeatures;
        protected WebSphereRuntime wsRuntime;
        protected Set<String> newFeatures;
        protected SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
        protected Text filterText;
        protected Table featureTable;
        protected Text descriptionLabel;
        protected Text enablesLabel;
        protected Text enabledByLabel;
        protected Link addAdditionalContentLink;

        /*
         * Note that the runtime can be null. If it is null, the fallback feature list will
         * be used to get the features. Any code that uses the runtime should check for null
         * first.
         */
        public AddDialog(Shell parent, Set<String> runtimeFeatures, Set<String> configuredFeatures, WebSphereRuntime wsRuntime) {
            super(parent);
            this.availableFeatures = new TreeSet<String>(new Comparator<String>() {
                @Override
                public int compare(String str1, String str2) {
                    return str1.compareToIgnoreCase(str2);
                }
            });
            availableFeatures.addAll(runtimeFeatures);
            availableFeatures.removeAll(configuredFeatures);
            this.configuredFeatures = configuredFeatures;
            this.wsRuntime = wsRuntime;
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(Messages.addFeatureTitle);
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            setTitle(Messages.addFeatureLabel);
            setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
            setMessage(Messages.addFeatureMessage);

            final Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 11;
            layout.marginWidth = 9;
            layout.horizontalSpacing = 5;
            layout.verticalSpacing = 7;
            layout.numColumns = 1;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setFont(parent.getFont());

            // Filter text
            filterText = new Text(composite, SWT.BORDER);
            filterText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            filterText.setMessage(Messages.filterMessage);

            // Feature table
            featureTable = new Table(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
            GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
            data.heightHint = 300;
            featureTable.setLayoutData(data);

            FeatureUI.createColumns(featureTable);

            createItems(featureTable, availableFeatures, "");

            FeatureUI.resizeColumns(featureTable);

            // Install additional content link
            // If there is no runtime, there is nothing to install on.  The editor still works
            // if there is no runtime as it uses the fallback schema and feature list.
            if (SiteHelper.downloadAndInstallSupported() && wsRuntime != null && wsRuntime.supportsInstallingAdditionalContent()) {
                final IRuntime runtime = wsRuntime.getRuntime();
                final IRuntimeInfo core = DownloadHelper.getRuntimeCore(runtime);
                if (core.getVersion() != null && !core.getVersion().startsWith("8.5.0")) {
                    addAdditionalContentLink = new Link(composite, SWT.NONE);
                    addAdditionalContentLink.setText("<a>" + Messages.actionInstallFeatures + "</a>");
                    addAdditionalContentLink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
                    addAdditionalContentLink.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            HashMap<String, Object> map = new HashMap<String, Object>();
                            map = new HashMap<String, Object>();
                            map.put(AbstractDownloadComposite.FOLDER, runtime.getLocation().toOSString());
                            map.put(AbstractDownloadComposite.RUNTIME_TYPE_ID, runtime.getRuntimeType().getId());
                            map.put(AbstractDownloadComposite.RUNTIME_CORE, core);

                            ArrayList<IProduct.Type> types = new ArrayList<IProduct.Type>(1);
                            types.add(IProduct.Type.FEATURE);
                            map.put(AbstractDownloadComposite.TYPE_FILTER_PRESET, types);
                            TaskModel taskModel = new TaskModel();
                            taskModel.putObject(TaskModel.TASK_RUNTIME, runtime);
                            taskModel.putObject(AbstractDownloadComposite.ADDON_MAP, map);

                            int result = DownloadUI.launchAddonsDialog(composite.getShell(), taskModel);
                            if (result == Window.OK) {
                                availableFeatures.clear();
                                availableFeatures.addAll(FeatureList.getRuntimeFeatureSet(wsRuntime));
                                availableFeatures.removeAll(configuredFeatures);
                                String text = filterText.getText();
                                if (text == null) {
                                    text = "";
                                }
                                createItems(featureTable, availableFeatures, text);
                            }
                        }
                    });
                }
            }

            ScrolledComposite descriptionScroll = new ScrolledComposite(composite, SWT.V_SCROLL);
            descriptionLabel = new Text(descriptionScroll, SWT.WRAP | SWT.READ_ONLY);
            descriptionLabel.setText(NLS.bind(Messages.featureDescription, ""));
            descriptionLabel.setBackground(parent.getBackground());
            descriptionScroll.setContent(descriptionLabel);

            // Info labels
            enablesLabel = new Text(composite, SWT.READ_ONLY);
            enablesLabel.setText(NLS.bind(Messages.featureEnables, ""));
            enablesLabel.setBackground(parent.getBackground());
            enablesLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            int lineHeight = enablesLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
            data.heightHint = lineHeight * 4;
            descriptionScroll.setLayoutData(data);
            descriptionScroll.getVerticalBar().setPageIncrement(lineHeight);
            descriptionScroll.getVerticalBar().setIncrement(lineHeight);

            enabledByLabel = new Text(composite, SWT.READ_ONLY);
            enabledByLabel.setText(NLS.bind(Messages.featureEnabledBy, ""));
            enabledByLabel.setBackground(parent.getBackground());
            enabledByLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

            // Listeners
            filterText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    String text = filterText.getText();
                    if (text == null) {
                        text = "";
                    }
                    createItems(featureTable, availableFeatures, text);
                    if (featureTable.getItemCount() > 0)
                        featureTable.select(0);
                    FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
                    enableOKButton(featureTable.getSelectionCount() > 0);
                }
            });

            filterText.addListener(SWT.KeyDown, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (event.keyCode == SWT.ARROW_DOWN) {
                        if (featureTable.getItemCount() > 0) {
                            featureTable.setSelection(0);
                            featureTable.setFocus();
                        }
                        event.doit = false;
                    }
                }
            });

            featureTable.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    TableItem[] items = featureTable.getSelection();
                    enableOKButton(items.length > 0);
                    FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
                }
            });

            featureTable.addListener(SWT.DefaultSelection, new Listener() {
                @Override
                public void handleEvent(Event e) {
                    okPressed();
                    close();
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
            FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
            // resize description since the UI isn't visible yet
            descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            filterText.setFocus();
        }

        @Override
        protected void okPressed() {
            TableItem[] items = featureTable.getSelection();
            newFeatures = new HashSet<String>(items.length);
            for (TableItem item : items) {
                newFeatures.add(item.getText());
            }
            super.okPressed();
        }

        protected void enableOKButton(boolean value) {
            getButton(IDialogConstants.OK_ID).setEnabled(value);
        }

        protected void createItems(Table table, Set<String> features, String filter) {
            // Create the items for the table.
            List<String> featureList = new ArrayList<String>(features.size());
            featureList.addAll(features);
            Collections.sort(featureList);
            table.removeAll();
            pattern.setPattern("*" + filter + "*");
            for (String feature : featureList) {
                String name = FeatureList.getFeatureDisplayName(feature, wsRuntime);
                String description = FeatureList.getFeatureDescription(feature, wsRuntime);
                if (pattern.matches(feature) || (name != null && pattern.matches(name)) || (description != null && pattern.matches(description))) {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setText(0, feature);
                    if (name != null)
                        item.setText(1, name);
                    if (FeatureList.isFeatureSuperseded(feature, wsRuntime))
                        item.setImage(Activator.getImage(Activator.IMG_FEATURE_SUPERSEDED));
                    else
                        item.setImage(Activator.getImage(Activator.IMG_FEATURE_ELEMENT));
                    if (!features.contains(feature)) {
                        item.setGrayed(true);
                    }
                }
            }
        }

        public Set<String> getFeatures() {
            return newFeatures;
        }
    }
}
