/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.ProjectPrefs;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class LibertyServerPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

    protected Composite preferencesComposite;
    protected Button enableAutomaticFeatureDetectionCheckbox;
    protected Label defaultClassScanningLabel;
    protected Combo defaultClassScanningCombo;

    @Override
    protected Control createContents(Composite parent) {
        preferencesComposite = new Composite(parent, SWT.NONE);
        preferencesComposite.setLayout(new GridLayout(2, false));

        // Enable automatic feature detection check box
        enableAutomaticFeatureDetectionCheckbox = new Button(preferencesComposite, SWT.CHECK);
        enableAutomaticFeatureDetectionCheckbox.setText(Messages.enableAutomaticFeatureDetection);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        enableAutomaticFeatureDetectionCheckbox.setLayoutData(gridData);
        enableAutomaticFeatureDetectionCheckbox.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                updateWidgetEnablement();
            }

        });

        // Default class scanning label
        defaultClassScanningLabel = new Label(preferencesComposite, SWT.NONE);
        defaultClassScanningLabel.setText(Messages.requiredFeatureDefaultAction);
        gridData = new GridData();
        gridData.horizontalIndent = 18;
        defaultClassScanningLabel.setLayoutData(gridData);

        // Default class scanning combo
        defaultClassScanningCombo = new Combo(preferencesComposite, SWT.READ_ONLY);
        String[] comboItems = { Messages.propertyPageFeatureAlways, Messages.propertyPageFeaturePrompt, Messages.propertyPageFeatureNever };
        defaultClassScanningCombo.setItems(comboItems);

        initializeValues();
        updateWidgetEnablement();

        return preferencesComposite;
    }

    protected void initializeValues() {
        enableAutomaticFeatureDetectionCheckbox.setSelection(Activator.isAutomaticFeatureDetectionEnabled());
        switch (Activator.getDefaultClassScanning()) {
            case ProjectPrefs.ADD_FEATURE_ALWAYS:
                defaultClassScanningCombo.select(0);
                break;
            case ProjectPrefs.ADD_FEATURE_PROMPT:
                defaultClassScanningCombo.select(1);
                break;
            case ProjectPrefs.ADD_FEATURE_NEVER:
                defaultClassScanningCombo.select(2);
                break;
        }
    }

    protected void updateWidgetEnablement() {
        boolean enablement = enableAutomaticFeatureDetectionCheckbox.getSelection();
        defaultClassScanningLabel.setEnabled(enablement);
        defaultClassScanningCombo.setEnabled(enablement);
    }

    @Override
    public void init(IWorkbench workbench) {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    protected void performDefaults() {
        enableAutomaticFeatureDetectionCheckbox.setSelection(Activator.getAutomaticFeatureDetectionEnabledDefault());
        updateWidgetEnablement();
        switch (Activator.getDefaultClassScanningDefaultValue()) {
            case ProjectPrefs.ADD_FEATURE_ALWAYS:
                defaultClassScanningCombo.select(0);
                break;
            case ProjectPrefs.ADD_FEATURE_PROMPT:
                defaultClassScanningCombo.select(1);
                break;
            case ProjectPrefs.ADD_FEATURE_NEVER:
                defaultClassScanningCombo.select(2);
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean performOk() {
        Activator.setAutomaticFeatureDetection(enableAutomaticFeatureDetectionCheckbox.getSelection());
        switch (defaultClassScanningCombo.getSelectionIndex()) {
            case 0:
                Activator.setDefaultClassScanning(ProjectPrefs.ADD_FEATURE_ALWAYS);
                break;
            case 1:
                Activator.setDefaultClassScanning(ProjectPrefs.ADD_FEATURE_PROMPT);
                break;
            case 2:
                Activator.setDefaultClassScanning(ProjectPrefs.ADD_FEATURE_NEVER);
                break;
        }
        return true;
    }

}
