/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class SchemaPropertiesSelectPage extends WizardPage {

    protected SchemaPropertiesCustomObject.SchemaPropertiesData propertiesData;

    public SchemaPropertiesSelectPage(SchemaPropertiesCustomObject.SchemaPropertiesData propertiesData) {
        super(Messages.schemaPropsSelectPageName, Messages.schemaPropsSelectPageTitle, null);
        this.propertiesData = propertiesData;
        setDescription(Messages.schemaPropsSelectPageDescription);
        setPageComplete(false);
    }

    /** {@inheritDoc} */
    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 7;
        layout.numColumns = 2;
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.schemaPropsSelectPageTypeLabel);
        GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
        label.setLayoutData(data);

        final Combo combo = new Combo(composite, SWT.READ_ONLY);
        combo.setItems(propertiesData.getElems());
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        combo.setLayoutData(data);

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                propertiesData.setChosenElem(combo.getItem(combo.getSelectionIndex()));
                setPageComplete(true);
            }
        });

        Dialog.applyDialogFont(composite);
        setControl(composite);
        setPageComplete(false);
    }

}
