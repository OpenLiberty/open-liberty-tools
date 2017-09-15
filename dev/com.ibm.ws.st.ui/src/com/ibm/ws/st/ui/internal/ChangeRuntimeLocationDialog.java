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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ChangeRuntimeLocationDialog extends MessageDialog {

    private final boolean promptDeleteServers;
    protected boolean isDeleteServers = false;

    public ChangeRuntimeLocationDialog(Shell parentShell, boolean promptDeleteServers) {
        super(parentShell,
              Messages.changeRuntimeLocationTitle,
              null,
              Messages.changeRuntimeLocationMessage,
              MessageDialog.WARNING,
              new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        this.promptDeleteServers = promptDeleteServers;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.changeRuntimeLocationTitle);
    }

    @Override
    public Control createCustomArea(Composite parent) {
        if (!promptDeleteServers) {
            return null;
        }

        Composite composite = new Composite(parent, SWT.None);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 1;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Button deleteServersButton = new Button(composite, SWT.CHECK);
        deleteServersButton.setText(Messages.deleteServersLabel);
        deleteServersButton.setSelection(true);
        isDeleteServers = true;

        deleteServersButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                isDeleteServers = deleteServersButton.getSelection();
            }
        });

        return composite;
    }

    public boolean isDeleteServers() {
        return isDeleteServers;
    }
}
