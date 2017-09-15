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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ibm.ws.st.ui.internal.Messages;

/**
 * Variable dialog allows a user to enter a variable name and a value.
 */
public class VariableDialog extends Dialog {

    protected boolean isOK = false;
    protected String name = "";
    protected String value = "";
    protected boolean nameDisabled = false;

    public VariableDialog(Shell parent) {
        super(parent);
    }

    public VariableDialog(Shell parent, String name) {
        super(parent);
        this.name = name;
        nameDisabled = name.length() > 0;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.variableDialogTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
        if (buttonId == IDialogConstants.OK_ID) {
            isOK = true;
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.minimumWidth = 250;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.nameDialogEnter);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Text nameText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        nameText.setLayoutData(data);
        if (nameDisabled) {
            nameText.setText(name);
            nameText.setEditable(false);
        }

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.valueDialogEnter);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        label.setLayoutData(data);

        final Text valueText = new Text(composite, SWT.BORDER);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        valueText.setLayoutData(data);

        ModifyListener listener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                final String nameStr = nameText.getText();
                final String valueStr = valueText.getText();
                if (valueStr.length() > 0 && nameStr.length() > 0) {
                    name = nameStr;
                    value = valueStr;
                    enableOKButton(true);
                } else {
                    enableOKButton(false);
                }
            }
        };

        nameText.addModifyListener(listener);
        valueText.addModifyListener(listener);

        return composite;
    }

    /** {@inheritDoc} */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        enableOKButton(false);
        return control;
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    public boolean isOK() {
        return isOK;
    }

    public String getName() {
        if (isOK) {
            return name;
        }
        return null;
    }

    public String getValue() {
        if (isOK) {
            return value;
        }
        return null;
    }
}
