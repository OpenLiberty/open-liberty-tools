/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.config.FeatureUI;

/**
 * Dialog that allows the user to chose replacement feature(s) for a superseded feature.
 */
public class SupersedeFeatureDialog extends TitleAreaDialog {
    protected WebSphereRuntime wsRuntime;
    protected WebSphereServerInfo serverInfo;
    protected String feature;
    protected List<String> replacements = new ArrayList<String>();
    protected Color gray;
    protected List<String> currentFeatures;
    protected Label enablesLabel2;
    protected Text enablesLabel;
    protected Text enabledByLabel;
    protected Text descriptionLabel;
    protected Table featureTable;
    private final ConfigurationFile configRoot;

    public SupersedeFeatureDialog(Shell parent, WebSphereRuntime wsRuntime, ConfigurationFile configRoot, String feature) {
        super(parent);
        this.feature = feature;

        this.wsRuntime = wsRuntime;
        this.configRoot = configRoot;

        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
    }

    public SupersedeFeatureDialog(Shell parent, WebSphereServerInfo serverInfo, String feature) {
        super(parent);
        this.feature = feature;

        this.wsRuntime = serverInfo.getWebSphereRuntime();
        this.configRoot = serverInfo.getConfigRoot();

        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.supersedeFeatureDialogTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.supersedeFeatureDialogTitle);
        setMessage(NLS.bind(Messages.supersededFeatureQuickFix, feature));

        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.minimumWidth = 450;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        Label label = new Label(composite, SWT.WRAP);
        label.setText(NLS.bind(Messages.supersedeFeatureDialogMessage, feature));
        data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
        label.setLayoutData(data);

        label = new Label(composite, SWT.WRAP);
        label.setText(NLS.bind(Messages.featureDisplayName, FeatureList.getFeatureDisplayName(feature, wsRuntime)));
        data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
        data.horizontalIndent = 15;
        label.setLayoutData(data);

        final ScrolledComposite descriptionScroll2 = new ScrolledComposite(composite, SWT.V_SCROLL);
        final Label descriptionLabel2 = new Label(descriptionScroll2, SWT.WRAP);
        descriptionLabel2.setText(NLS.bind(Messages.featureDescription, FeatureList.getFeatureDescription(feature, wsRuntime)));
        descriptionScroll2.setContent(descriptionLabel2);

        int lineHeight = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        data.horizontalIndent = 15;
        data.heightHint = lineHeight * 5;
        descriptionScroll2.setLayoutData(data);
        descriptionScroll2.getVerticalBar().setPageIncrement(lineHeight);
        descriptionScroll2.getVerticalBar().setIncrement(lineHeight);

        enablesLabel2 = new Label(composite, SWT.WRAP);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
        data.horizontalIndent = 15;
        enablesLabel2.setLayoutData(data);

        // label and table with features to supersede
        label = new Label(composite, SWT.WRAP);
        label.setText(NLS.bind(Messages.supersedeFeatureDialogMessage2, feature));
        data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
        data.verticalIndent = 7;
        label.setLayoutData(data);

        featureTable = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 100;
        featureTable.setLayoutData(data);

        FeatureUI.createColumns(featureTable);

        // add an additional column
        final TableColumn infoColumn = new TableColumn(featureTable, SWT.NONE);
        infoColumn.setText(Messages.supersedeFeatureDialogColumn);
        infoColumn.setWidth(125);
        infoColumn.setResizable(true);
        infoColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                FeatureUI.sortTable(featureTable, infoColumn);
            }
        });

        Set<String> set = FeatureList.getFeatureSupersededBy(feature, wsRuntime);
        List<String> featureList = new ArrayList<String>();
        FeatureSet installedFeatures = wsRuntime.getInstalledFeatures();
        if (set != null) {
            for (String s : set) {
                String f = s;
                if (s.startsWith("[") && s.endsWith("]"))
                    f = s.substring(1, s.length() - 1);
                if (!installedFeatures.supports(f)) {
                    // ignore features not supported by the current runtime
                } else
                    featureList.add(f);
            }
        }
        Collections.sort(featureList);

        // find features that are already added (or supported by) the current config
        List<String> configuredFeatures = configRoot.getAllFeatures();
        configuredFeatures.remove(feature);

        currentFeatures = new ArrayList<String>();
        for (String s : featureList) {
            if (configuredFeatures.contains(s)) {
                if (!currentFeatures.contains(s))
                    currentFeatures.add(s);
            } else {
                for (String f : configuredFeatures) {
                    if (wsRuntime.isContainedBy(s, f)) {
                        if (!currentFeatures.contains(s))
                            currentFeatures.add(s);
                        break;
                    }
                }
            }
        }

        for (String s : featureList) {
            TableItem item = new TableItem(featureTable, SWT.NONE);
            item.setText(0, s);
            String name = FeatureList.getFeatureDisplayName(s, wsRuntime);
            if (name != null)
                item.setText(1, name);
            item.setImage(Activator.getImage(Activator.IMG_FEATURE_ELEMENT));

            String status = null;
            if (currentFeatures.contains(s)) {
                if (gray == null) {
                    Color fg = item.getForeground();
                    Color bg = item.getBackground();
                    gray = new Color(fg.getDevice(), (fg.getRed() + bg.getRed()) / 2, (fg.getGreen() + bg.getGreen()) / 2, (fg.getBlue() + bg.getBlue()) / 2);
                }
                item.setForeground(gray);
                status = Messages.supersedeFeatureDialogExisting;
            } else if (set != null && set.contains(s)) {
                status = Messages.supersedeFeatureDialogRecommended;
                item.setChecked(true);
                replacements.add(s);
            } else {
                status = Messages.supersedeFeatureDialogOptional;
            }
            item.setText(2, status);
        }

        FeatureUI.resizeColumns(featureTable);

        // info labels
        ScrolledComposite descriptionScroll = new ScrolledComposite(composite, SWT.V_SCROLL);
        descriptionLabel = new Text(descriptionScroll, SWT.WRAP | SWT.READ_ONLY);
        descriptionLabel.setText(NLS.bind(Messages.featureDescription, ""));
        descriptionLabel.setBackground(parent.getBackground());
        descriptionScroll.setContent(descriptionLabel);

        enablesLabel = new Text(composite, SWT.READ_ONLY);
        enablesLabel.setText(NLS.bind(Messages.featureEnables, ""));
        enablesLabel.setBackground(parent.getBackground());
        enablesLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        lineHeight = enablesLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        data.heightHint = lineHeight * 4;
        descriptionLabel.setSize(300, lineHeight);
        descriptionLabel.setEnabled(false);
        descriptionScroll.setLayoutData(data);
        descriptionScroll.getVerticalBar().setPageIncrement(lineHeight);
        descriptionScroll.getVerticalBar().setIncrement(lineHeight);

        enabledByLabel = new Text(composite, SWT.READ_ONLY);
        enabledByLabel.setText(NLS.bind(Messages.featureEnabledBy, ""));
        enabledByLabel.setBackground(parent.getBackground());
        enabledByLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

        descriptionScroll2.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent event) {
                int width = descriptionScroll2.getClientArea().width;
                Point p = descriptionLabel2.computeSize(width, SWT.DEFAULT);
                descriptionLabel2.setSize(p);
                if (p.y < descriptionScroll2.getClientArea().height) {
                    GridData data = (GridData) descriptionScroll2.getLayoutData();
                    data.heightHint = p.y;
                    descriptionScroll2.getParent().layout(true);
                } else {
                    int newWidth = descriptionLabel2.getParent().getClientArea().width;
                    if (newWidth != width) {
                        descriptionLabel2.setSize(newWidth, descriptionLabel2.computeSize(newWidth, SWT.DEFAULT).y);
                    }
                }

                p = descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                descriptionLabel.setSize(p);
            }
        });

        featureTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                replacements.clear();
                for (TableItem ti : featureTable.getItems()) {
                    String feature = ti.getText(0);
                    if (ti.getChecked() && !currentFeatures.contains(feature)) {
                        replacements.add(feature);
                    }
                }
                FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
                validate(featureTable.getItems());
            }
        });

        featureTable.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent event) {
                if (gray != null)
                    gray.dispose();
            }
        });

        return composite;
    }

    @Override
    public void create() {
        super.create();
        String enables = FeatureUI.formatList(FeatureList.getFeatureChildren(feature, wsRuntime));
        String text = NLS.bind(Messages.featureEnables, enables);
        String shortenedText = SWTUtil.shortenText(enablesLabel2, text, Messages.featureEnables.length());
        enablesLabel2.setText(shortenedText);
        enablesLabel2.setToolTipText(enables);
        FeatureUI.updateInfo(enablesLabel, enabledByLabel, descriptionLabel, featureTable, null, wsRuntime);
    }

    protected void validate(TableItem[] tis) {
        Set<String> set = FeatureList.getFeatureSupersededBy(feature, wsRuntime);
        for (TableItem ti : tis) {
            String s = ti.getText(0);
            if (ti.getChecked() && currentFeatures.contains(s)) {
                setMessage(NLS.bind(Messages.supersedeFeatureDialogErrorExisting, s), IMessageProvider.WARNING);
                return;
            }
            if (!ti.getChecked() && set.contains(s) && !currentFeatures.contains(s)) {
                setMessage(NLS.bind(Messages.supersedeFeatureDialogErrorRecommended, s), IMessageProvider.ERROR);
                return;
            }
        }

        setMessage(NLS.bind(Messages.supersededFeatureQuickFix, feature));
    }

    public String[] getReplacementFeatures() {
        return replacements.toArray(new String[replacements.size()]);
    }
}
