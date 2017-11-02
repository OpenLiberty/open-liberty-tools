/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.gradle.ui.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ibm.ws.st.liberty.gradle.internal.Activator;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradleConstants;
import com.ibm.ws.st.liberty.gradle.internal.Messages;

public class LibertyIntegrationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    Combo detectionCombo;
    String[] generationPromptOptions = { com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages.prefComboOptionAsk,
                                         com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages.yesLabel,
                                         com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages.noLabel };
    String selectedOption = null;
    IPreferenceStore store = Activator.getInstance().getPreferenceStore();

    /** {@inheritDoc} */
    @Override
    public void init(IWorkbench arg0) {
        // intentionally empty
    }

    /** {@inheritDoc} */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        composite.setLayoutData(gd);

        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = 25;
        layout.verticalSpacing = convertVerticalDLUsToPixels(10);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

        createDetectionControls(composite);

        initializeValues();

        return composite;
    }

    private void initializeValues() {
        if (detectionCombo != null) {
            detectionCombo.select(0);
            String val = store.getString(LibertyGradleConstants.PROMPT_PREFERENCE);
            for (int i = 0; i < generationPromptOptions.length; i++) {
                if (generationPromptOptions[i].equals(val))
                    detectionCombo.select(i);
            }
        }
    }

    private void createDetectionControls(Composite parent) {
        Text label = new Text(parent, SWT.READ_ONLY);
        label.setText(Messages.prefComboText);
        label.setBackground(parent.getBackground());

        detectionCombo = new Combo(parent, SWT.READ_ONLY);
        detectionCombo.setItems(generationPromptOptions);
        detectionCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent event) {
                int selection = detectionCombo.getSelectionIndex();
                if (selection >= 0)
                    selectedOption = generationPromptOptions[selection];
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    protected void performDefaults() {
        detectionCombo.select(0);
        selectedOption = generationPromptOptions[0];
        super.performDefaults();
    }

    /** {@inheritDoc} */
    @Override
    protected void performApply() {
        performOk();
    }

    /** {@inheritDoc} */
    @Override
    public boolean performOk() {
        if (selectedOption != null) {
            store.setValue(LibertyGradleConstants.PROMPT_PREFERENCE, selectedOption);
            selectedOption = null;
        }
        return super.performOk();
    }

}
