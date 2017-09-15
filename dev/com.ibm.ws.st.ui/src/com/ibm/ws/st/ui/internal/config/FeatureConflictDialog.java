/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.ibm.ws.st.core.internal.RuntimeFeatureResolver;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.ResolverResult;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Dialog that allows the user to enable an element by adding the appropriate feature.
 */
public class FeatureConflictDialog extends TitleAreaDialog {
    protected WebSphereRuntime wsRuntime;
    protected ConfigurationFile configFile;
    protected Set<FeatureConflict> conflicts;
    protected Map<String, List<String>> requiredFeatures;
    private final List<String> allFeatures;
    private final List<String> mainFeatures;
    private final List<String> includeFeatures;
    protected List<String> currentFeatures;
    protected Label appinfoLabel;
    protected Label includeLabel;
    protected Table featureTable;
    protected Text descriptionLabel;
    protected Text enablesLabel;
    protected Text enabledByLabel;
    protected Text conflictsWithLabel;
    protected Link conflictsLink;
    protected Button ignoreButton;
    protected boolean changed;
    private boolean showIgnoreButton = true;
    private final Color gray = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

    // Dialog that lists feature conflicts and allows the user to add/remove features using
    // a feature table (similar to that in the configuration editor).  Only features in the
    // main server configuration file are shown in the feature table (since don't want to be 
    // editing include files) but conflicts are determined based on the entire set of enabled
    // features.
    public FeatureConflictDialog(Shell parent, WebSphereServerInfo serverInfo, Map<String, List<String>> requiredFeatures, Set<FeatureConflict> conflicts) {
        super(parent);
        this.wsRuntime = serverInfo.getWebSphereRuntime();
        configFile = serverInfo.getConfigRoot();
        allFeatures = configFile.getAllFeatures();
        mainFeatures = configFile.getFeatures();
        includeFeatures = new ArrayList<String>(allFeatures);
        includeFeatures.removeAll(mainFeatures);
        currentFeatures = configFile.getAllFeatures();
        this.requiredFeatures = requiredFeatures;
        this.conflicts = conflicts;
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.featureConflictLabel);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.featureConflictTitle);
        setMessage(Messages.featureConflictDescription);

        final Shell shell = parent.getShell();

        final Composite composite = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.widthHint = 300;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // Add all the required features so that they will be available in the
        // table for the user to manipulate.
        configFile.addFeatures(requiredFeatures.keySet());
        mainFeatures.addAll(requiredFeatures.keySet());
        currentFeatures.addAll(requiredFeatures.keySet());
        changed = true;

        // Composite for the feature table.  Standard table with add and remove buttons
        // and informational section showing description, enables and enabled by.
        final Composite featureComposite = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 2;
        featureComposite.setLayout(layout);
        data = new GridData(GridData.FILL, GridData.FILL, true, true);
        featureComposite.setLayoutData(data);
        featureComposite.setFont(composite.getFont());

        if (requiredFeatures != null && !requiredFeatures.isEmpty()) {
            // get all apps that map to features being added due to required features
            Set<String> allApps = new HashSet<String>();
            for (Entry<String, List<String>> entry : requiredFeatures.entrySet()) {
                allApps.addAll(entry.getValue());
            }

            // get all modules causing features to be added
            String apps = FeatureUI.formatList(allApps);

            String appMessage = NLS.bind(Messages.featureConflictAddedAppsMessage, apps);
            appinfoLabel = new Label(featureComposite, SWT.WRAP);
            data = new GridData(SWT.FILL, SWT.FILL, true, false);
            data.widthHint = 500;
            data.horizontalSpan = 2;
            appinfoLabel.setLayoutData(data);
            appinfoLabel.setText(appMessage);
        }

        // Label to let the user know that the table shows features enabled in
        // <include> configuration files but shows them as greyed out and cannot be removed.
        includeLabel = new Label(featureComposite, SWT.WRAP);
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.widthHint = 500;
        data.horizontalSpan = 2;
        includeLabel.setLayoutData(data);
        includeLabel.setText(Messages.featureConflictIncludeMessage);

        featureTable = new Table(featureComposite, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = featureTable.getItemHeight() * 6;
        data.verticalSpan = 2;
        featureTable.setLayoutData(data);

        FeatureUI.createColumns(featureTable);
        initFeatureTable();
        FeatureUI.resizeColumns(featureTable);

        // Buttons
        final Button addButton = new Button(featureComposite, SWT.PUSH);
        addButton.setText(Messages.addButton);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        addButton.setLayoutData(data);

        final Button removeButton = new Button(featureComposite, SWT.PUSH);
        removeButton.setText(Messages.removeButton);
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        removeButton.setLayoutData(data);

        // info labels
        ScrolledComposite descriptionScroll = new ScrolledComposite(featureComposite, SWT.V_SCROLL);
        descriptionLabel = new Text(descriptionScroll, SWT.WRAP | SWT.READ_ONLY);
        descriptionLabel.setText(NLS.bind(Messages.featureDescription, ""));
        descriptionLabel.setBackground(parent.getBackground());
        descriptionScroll.setContent(descriptionLabel);

        enablesLabel = new Text(featureComposite, SWT.READ_ONLY);
        data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalSpan = 2;
        enablesLabel.setLayoutData(data);

        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        int lineHeight = enablesLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        data.heightHint = lineHeight * 4;
        data.horizontalSpan = 2;
        descriptionScroll.setLayoutData(data);
        descriptionScroll.getVerticalBar().setPageIncrement(lineHeight);
        descriptionScroll.getVerticalBar().setIncrement(lineHeight);

        enabledByLabel = new Text(featureComposite, SWT.READ_ONLY);
        data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.horizontalSpan = 2;
        enabledByLabel.setLayoutData(data);

        Composite conflictsComposite = new Composite(featureComposite, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 2;
        conflictsComposite.setLayout(layout);
        data = new GridData(GridData.FILL, GridData.FILL, false, false);
        conflictsComposite.setLayoutData(data);
        conflictsComposite.setFont(composite.getFont());

        conflictsWithLabel = new Text(conflictsComposite, SWT.READ_ONLY);
        data = new GridData(GridData.FILL, GridData.BEGINNING, false, false);
        data.horizontalSpan = 1;
        data.widthHint = 180;
        conflictsWithLabel.setLayoutData(data);

        conflictsLink = new Link(conflictsComposite, SWT.NONE);
        conflictsLink.setText("(<a>" + Messages.wizPromptDetailsLabel + "</a>)");
        data = new GridData(GridData.FILL, GridData.BEGINNING, false, false);
        data.horizontalSpan = 1;
        conflictsLink.setLayoutData(data);
        conflictsLink.pack();

        // Listeners
        featureTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                updateRemoveButton(removeButton, featureTable);
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, conflictsWithLabel, conflictsLink, featureTable, null, wsRuntime);
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

                // Get the configured features
                Set<String> configuredFeatures = new TreeSet<String>(new Comparator<String>() {
                    @Override
                    public int compare(String str1, String str2) {
                        return str1.compareToIgnoreCase(str2);
                    }
                });
                configuredFeatures.addAll(getFeatures());
                if (runtimeFeatures.size() == 0) {
                    MessageDialog.openInformation(shell, Messages.title, Messages.featureAllEnabledMsg);
                } else {
                    FeatureUI.AddDialog dialog = new FeatureUI.AddDialog(shell, runtimeFeatures, configuredFeatures, wsRuntime);
                    if (dialog.open() == IStatus.OK) {
                        Set<String> newFeatures = dialog.getFeatures();
                        for (String feature : newFeatures) {
                            currentFeatures.add(feature);
                        }
                        changed = true;
                        initFeatureTable();
                        updateRemoveButton(removeButton, featureTable);
                        FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, conflictsWithLabel, conflictsLink, featureTable, null, wsRuntime);
                    }
                }
                validate();
            }
        });

        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] items = featureTable.getSelection();
                for (TableItem item : items) {
                    currentFeatures.remove(((FeatureItemData) item.getData()).getFeatureName());
                }
                changed = true;
                initFeatureTable();
                updateRemoveButton(removeButton, featureTable);
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, conflictsWithLabel, conflictsLink, featureTable, null, wsRuntime);
                validate();
            }
        });

        validate();
        return composite;
    }

    protected void updateRemoveButton(Button removeButton, Table featureTable) {
        TableItem[] items = featureTable.getSelection();
        for (TableItem item : items) {
            if (item.getForeground().equals(gray)) {
                removeButton.setEnabled(false);
                return;
            }
        }
        removeButton.setEnabled(items.length > 0);
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        featureTable.setFocus();
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        if (showIgnoreButton)
            getButton(IDialogConstants.IGNORE_ID).setEnabled(true);
        else
            getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
        return control;
    }

    /** {@inheritDoc} */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        if (showIgnoreButton) {
            ignoreButton = createButton(parent, IDialogConstants.IGNORE_ID, IDialogConstants.IGNORE_LABEL, false);
            ignoreButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (MessageDialog.openConfirm(getShell(), Messages.featureConflictLabel, Messages.featureConflictIgnoreInfo + "\n\n" + Messages.featureConflictIgnoreInfoNote)) {
                        setReturnCode(IDialogConstants.IGNORE_ID);
                        close();
                        return;
                    }
                    validate();
                }
            });
        }
        else
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /** {@inheritDoc} */
    @Override
    protected void setReturnCode(int code) {
        super.setReturnCode(code);
    }

    /** {@inheritDoc} */
    @Override
    public void create() {
        super.create();
        FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, conflictsWithLabel, conflictsLink, featureTable, null, wsRuntime);
        // resize description since the UI isn't visible yet 
        descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    public boolean isChanged() {
        return changed;
    }

    public Set<FeatureConflict> getConflicts() {
        return conflicts;
    }

    protected void initFeatureTable() {

        ConflictInfo[] conflictInfos = getConflictInfos(currentFeatures);

        Collections.sort(currentFeatures);
        featureTable.removeAll();

        for (String feature : currentFeatures) {
            TableItem item = new TableItem(featureTable, SWT.NONE);
            item.setText(0, feature);
            setItemData(item, feature, conflictInfos);
            String name = FeatureList.getFeatureDisplayName(feature, wsRuntime);
            if (name != null) {
                item.setText(1, name);
            }

            // Append (New) to features that were newly added
            String featureName = item.getText();
            if (requiredFeatures.keySet().contains(featureName)) {
                featureName = featureName + " (" + Messages.featureConflictNewFeature + ")";
                item.setText(0, featureName);
            }

            if (FeatureList.isValidFeature(feature, wsRuntime)) {
                if (FeatureList.isFeatureSuperseded(feature, wsRuntime))
                    item.setImage(Activator.getImage(Activator.IMG_FEATURE_SUPERSEDED));
                else
                    item.setImage(new Image[] { Activator.getImage(Activator.IMG_FEATURE_ELEMENT) });

                // features from included files should be grayed out and not selectable
                if (includeFeatures.contains(feature)) {
                    item.setForeground(gray);
                }

                // features with errors need to have the error icon overlayed on the feature icon
                if (((FeatureItemData) item.getData()).getConflictInfo() != null) {
                    Image img = item.getImage() == null ? Activator.getImage(Activator.IMG_FEATURE_ELEMENT) : item.getImage();
                    DecorationOverlayIcon decoratedImage = new DecorationOverlayIcon(img, ImageDescriptor.createFromImage(Activator.getImage(Activator.IMG_ERROR_OVERLAY)), IDecoration.TOP_LEFT);
                    item.setImage(new Image[] { decoratedImage.createImage() });
                }
            } else {
                item.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
            }
        }
    }

    /**
     * @param feature
     * @param item
     * @param conflictInfos
     */
    private void setItemData(TableItem item, String feature, ConflictInfo[] conflictInfos) {
        List<ConflictInfo> conflictList = null;
        for (ConflictInfo conflict : conflictInfos) {
            if (feature.equals(conflict.feature) || feature.equals(conflict.enablingFeature)) {
                conflictList = item.getData() == null ? new ArrayList<ConflictInfo>(1) : ((FeatureItemData) item.getData()).getConflictInfo();
                if (conflictList != null && !conflictList.contains(conflict)) {
                    conflictList.add(conflict);
                }
            }
        }

        item.setData(new FeatureItemData(feature, conflictList));
    }

    protected List<String> getFeatures() {
        TableItem[] items = featureTable.getItems();
        List<String> features = new ArrayList<String>(items.length);
        for (TableItem item : items)
            features.add(((FeatureItemData) item.getData()).getFeatureName());
        return features;
    }

    private ConflictInfo[] getConflictInfos(List<String> allFeatures) {
        ResolverResult result = RuntimeFeatureResolver.resolve(wsRuntime, allFeatures);
        conflicts = result.getFeatureConflicts();
        if (conflicts == null || conflicts.isEmpty()) {
            return new ConflictInfo[0];
        }

        List<ConflictInfo> infos = new ArrayList<ConflictInfo>();
        // Base all matching on symbolic names since feature names may not be canonical
        Set<String> symbolicFeatures = new HashSet<String>();
        for (String feature : allFeatures) {
            symbolicFeatures.add(FeatureList.getFeatureSymbolicName(FeatureList.getCanonicalFeatureName(feature, wsRuntime), wsRuntime));
        }
        Map<String, List<String>> symbolicRequiredFeatures = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : requiredFeatures.entrySet()) {
            symbolicRequiredFeatures.put(FeatureList.getFeatureSymbolicName(FeatureList.getCanonicalFeatureName(entry.getKey(), wsRuntime), wsRuntime), entry.getValue());
        }
        for (FeatureConflict conflict : conflicts) {
            if (conflict.getDependencyChainA().isEmpty() || conflict.getDependencyChainB().isEmpty()) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Empty conflict dependency chain.  Chain A: " +
                                               RuntimeFeatureResolver.getDependencyChainString(conflict.getDependencyChainA(), wsRuntime) +
                                               ", Chain B: " + RuntimeFeatureResolver.getDependencyChainString(conflict.getDependencyChainB(), wsRuntime));
                continue;
            }
            String featureA = conflict.getDependencyChainA().get(0);
            String featureB = conflict.getDependencyChainB().get(0);
            String feature = null;
            List<String> conflictChain = null;
            // Look for required features first
            if (symbolicRequiredFeatures.containsKey(featureA)) {
                feature = featureA;
                conflictChain = conflict.getDependencyChainB();
            } else if (symbolicRequiredFeatures.containsKey(featureB)) {
                feature = featureB;
                conflictChain = conflict.getDependencyChainA();
            } else if (symbolicFeatures.contains(feature)) {
                feature = featureA;
                conflictChain = conflict.getDependencyChainB();
            } else {
                feature = featureB;
                conflictChain = conflict.getDependencyChainA();
            }
            String mainFeature = FeatureList.getPublicFeatureName(feature, wsRuntime);
            String conflictFeature = FeatureList.getPublicFeatureName(conflictChain.get(conflictChain.size() - 1), wsRuntime);
            String enablingFeature = conflictChain.size() > 1 ? FeatureList.getPublicFeatureName(conflictChain.get(0), wsRuntime) : conflictFeature;
            infos.add(new ConflictInfo(conflict, mainFeature, conflictFeature, enablingFeature, symbolicRequiredFeatures.get(feature)));
        }
        return infos.toArray(new ConflictInfo[infos.size()]);
    }

    static class FeatureItemData {
        private final String featureName;
        private final List<ConflictInfo> conflicts;

        public FeatureItemData(String featureName, List<ConflictInfo> conflicts) {
            this.featureName = featureName;
            this.conflicts = conflicts;
        }

        public String getFeatureName() {
            return featureName;
        }

        public List<ConflictInfo> getConflictInfo() {
            return conflicts;
        }
    }

    // Simple class that contains information about a conflict.
    static class ConflictInfo {
        FeatureConflict featureConflict;
        String feature;
        String conflictFeature;
        String enablingFeature;
        List<String> appNames;

        public ConflictInfo(FeatureConflict featureConflict, String feature, String conflictFeature, String enablingFeature, List<String> appNames) {
            this.featureConflict = featureConflict;
            this.feature = feature;
            this.conflictFeature = conflictFeature;
            this.enablingFeature = enablingFeature;
            this.appNames = appNames;
        }

        /**
         * @return
         */
        public FeatureConflict getFeatureConflict() {
            return featureConflict;
        }

        public String getMessage() {
            return NLS.bind(Messages.featureConflictDetails, feature, conflictFeature);
        }

        public String getDetails() {
            StringBuilder sb = new StringBuilder();
            if (appNames != null) {
                for (String appName : appNames) {
                    sb.append("\n - ");
                    sb.append(appName);
                }
            }

            String msg = null;
            if (enablingFeature != null && !enablingFeature.equals(feature)) {
                if (sb.length() > 0)
                    msg = NLS.bind(Messages.featureConflictDetailsWithDependencyAndModules, new String[] { feature, conflictFeature, enablingFeature, sb.toString() });
                else
                    msg = NLS.bind(Messages.featureConflictDetailsWithDependency, new String[] { feature, conflictFeature, enablingFeature });
            } else {
                if (sb.length() > 0)
                    msg = NLS.bind(Messages.featureConflictDetailsWithModules, new String[] { feature, conflictFeature, sb.toString() });
                else
                    msg = NLS.bind(Messages.featureConflictDetails, new String[] { feature, conflictFeature });
            }
            return msg;
        }
    }

    @Override
    protected void setShellStyle(int arg0) {
        // Hide the close button in the title bar
        super.setShellStyle(SWT.TITLE | SWT.RESIZE);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean canHandleShellCloseEvent() {
        // disable cancel and closing the window with the escape key
        return false;
    }

    public void setShowIgnoreButton(boolean value) {
        showIgnoreButton = value;
    }

    public int getRemainingConflictsSize() {
        try {
            List<String> allFeatures = configFile.getAllFeatures();
            ConflictInfo[] conflictInfos = getConflictInfos(allFeatures);
            return conflictInfos.length;
        } catch (Exception e) {
            // intentionally empty
        }
        return 0;
    }

    void validate() {
        if (ignoreButton != null) {
            ignoreButton.setEnabled(!conflicts.isEmpty());
        }

        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            boolean okEnable = conflicts.isEmpty();
            okButton.setEnabled(okEnable);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void okPressed() {
        for (String feature : mainFeatures)
            configFile.removeFeature(feature);
        currentFeatures.removeAll(includeFeatures);
        configFile.addFeatures(currentFeatures);
        super.okPressed();
    }

}
