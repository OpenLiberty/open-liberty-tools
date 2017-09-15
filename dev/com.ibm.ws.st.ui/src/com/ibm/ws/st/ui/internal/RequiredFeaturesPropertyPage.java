/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.core.internal.FeatureResolverWrapper;
import com.ibm.ws.st.core.internal.ProjectPrefs;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.ui.internal.config.FeatureUI;

public class RequiredFeaturesPropertyPage extends PropertyPage {
    protected IProject project;
    protected Label propertyPageDescriptionLabel;
    protected Table requiredFeaturesTable;
    protected ProjectPrefs prefs;

    public RequiredFeaturesPropertyPage() {
        super();
    }

    protected void findFeatures(IProject project, WebSphereRuntime wr, IProgressMonitor monitor) {
        requiredFeaturesTable.removeAll();
        if (com.ibm.ws.st.core.internal.Activator.isAutomaticFeatureDetectionEnabled()) {

            propertyPageDescriptionLabel.setText(Messages.propertyPageFeatureRequired);
            requiredFeaturesTable.setVisible(true);

            List<String> requiredFeatures2 = FeatureResolverWrapper.findFeatures(project, wr, false, monitor);
            for (String s : requiredFeatures2) {
                TableItem item = new TableItem(requiredFeaturesTable, SWT.NONE);
                item.setText(0, s);
                String name = FeatureList.getFeatureDisplayName(s, wr);
                if (name != null)
                    item.setText(1, name);
                item.setText(2, Messages.propertyPageFeatureAppRequired);
                if (FeatureList.isFeatureSuperseded(s, wr))
                    item.setImage(Activator.getImage(Activator.IMG_FEATURE_SUPERSEDED));
                else
                    item.setImage(Activator.getImage(Activator.IMG_FEATURE_ELEMENT));
            }

            final TableItem[] items = requiredFeaturesTable.getItems();
            for (TableItem item : items) {
                TableEditor editor = new TableEditor(requiredFeaturesTable);
                final CCombo combo = new CCombo(requiredFeaturesTable, SWT.READ_ONLY | SWT.FLAT);
                combo.setBackground(requiredFeaturesTable.getBackground());

                String[] s = new String[] {
                                            Messages.propertyPageFeatureAlways,
                                            Messages.propertyPageFeaturePrompt,
                                            Messages.propertyPageFeatureNever
                };
                combo.setItems(s);
                final String feature = item.getText(0);
                int b = prefs.getFeaturePrompt(feature);
                if (b == ProjectPrefs.ADD_FEATURE_PROMPT)
                    combo.select(1);
                else if (b == ProjectPrefs.ADD_FEATURE_NEVER)
                    combo.select(2);
                else
                    // if (b == ProjectPrefs.ADD_FEATURE_ALWAYS) or none
                    combo.select(0);

                combo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        int c = combo.getSelectionIndex();
                        if (c == 0)
                            prefs.setFeaturePrompt(feature, ProjectPrefs.ADD_FEATURE_ALWAYS);
                        else if (c == 1)
                            prefs.setFeaturePrompt(feature, ProjectPrefs.ADD_FEATURE_PROMPT);
                        else if (c == 2)
                            prefs.setFeaturePrompt(feature, ProjectPrefs.ADD_FEATURE_NEVER);
                    }
                });
                editor.grabHorizontal = true;
                editor.setEditor(combo, item, 3);
            }

            FeatureUI.resizeColumns(requiredFeaturesTable);
        } else {
            propertyPageDescriptionLabel.setText(Messages.propertyPageFeatureRequiredDisabled);
            requiredFeaturesTable.setVisible(false);
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        try {
            IAdaptable element = getElement();
            project = element.getAdapter(IProject.class);
            prefs = new ProjectPrefs(project);

            Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.numColumns = 2;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            propertyPageDescriptionLabel = new Label(composite, SWT.WRAP);
            GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
            data.horizontalSpan = 2;
            propertyPageDescriptionLabel.setLayoutData(data);

            requiredFeaturesTable = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
            data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            data.heightHint = 200;
            data.verticalSpan = 3;
            data.horizontalSpan = 2;
            requiredFeaturesTable.setLayoutData(data);

            FeatureUI.createColumns(requiredFeaturesTable);

            final TableColumn statusColumn = new TableColumn(requiredFeaturesTable, SWT.NONE);
            statusColumn.setText(Messages.propertyPageFeatureAppColumn);
            statusColumn.setResizable(true);
            statusColumn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    FeatureUI.sortTable(requiredFeaturesTable, statusColumn);
                }
            });

            final TableColumn actionColumn = new TableColumn(requiredFeaturesTable, SWT.NONE);
            actionColumn.setText(Messages.propertyPageFeatureActionColumn);
            actionColumn.setResizable(true);
            actionColumn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    FeatureUI.sortTable(requiredFeaturesTable, actionColumn);
                }
            });

            init();

            Dialog.applyDialogFont(composite);

            return composite;
        } catch (Exception e) {
            Trace.logError("Error creating property page", e);
            return null;
        }
    }

    protected void init() {
        // find runtime
        try {
            IFacetedProject fp = ProjectFacetsManager.create(project);
            org.eclipse.wst.common.project.facet.core.runtime.IRuntime rt = fp.getPrimaryRuntime();
            if (rt != null) {
                IRuntime runtime = FacetUtil.getRuntime(rt);
                WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                if (wr != null) {
                    findFeatures(project, wr, null);
                }
            }
        } catch (Exception e) {
            Trace.logError("Error initializing property page", e);
        }
    }

    @Override
    protected void performDefaults() {
        // TODO
        init();
    }

    protected boolean save() {
        return prefs.save();
    }

    @Override
    protected void performApply() {
        save();
    }

    @Override
    public boolean performOk() {
        return save();
    }
}
