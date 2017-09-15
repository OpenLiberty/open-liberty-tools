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
package com.ibm.ws.st.ui.internal.download;

import java.net.PasswordAuthentication;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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

public class PasswordDialog extends MessageDialog {

    protected String userName = "";
    protected String password = "";

    /**
     * @param parentShell
     */
    public PasswordDialog(Shell parentShell, String dialogMessage, PasswordAuthentication authentication) {
        super(parentShell, Messages.loginPasswordDialogTitle, null, dialogMessage, MessageDialog.QUESTION, new String[] {
                                                                                                                         IDialogConstants.OK_LABEL,
                                                                                                                         IDialogConstants.CANCEL_LABEL }, 0);

        if (authentication != null) {
            if (authentication.getUserName() != null)
                userName = authentication.getUserName();

            if (authentication.getPassword() != null)
                password = new String(authentication.getPassword());
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.loginPasswordDialogTitle);
    }

    @Override
    public void create() {
        super.create();
        enableOKButton(false);
    }

    @Override
    public Control createCustomArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.None);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label userLabel = new Label(composite, SWT.NONE);
        userLabel.setText(Messages.user);
        userLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        final Text userText = new Text(composite, SWT.BORDER);
        userText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        userText.setText(userName);

        Label passwordLabel = new Label(composite, SWT.NONE);
        passwordLabel.setText(Messages.password);
        passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        final Text passwordText = new Text(composite, SWT.BORDER);
        passwordText.setEchoChar('*');
        passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        passwordText.setText(password);

        userText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                userName = userText.getText();
                enableOKButton(isComplete());
            }
        });

        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                password = passwordText.getText();
                enableOKButton(isComplete());
            }
        });

        return composite;
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    protected boolean isComplete() {
        return !userName.isEmpty() && !password.isEmpty();
    }

    public String getUser() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
