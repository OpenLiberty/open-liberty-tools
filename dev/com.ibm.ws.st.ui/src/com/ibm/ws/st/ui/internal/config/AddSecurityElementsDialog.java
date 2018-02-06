/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.provisional.dialogs.PasswordDialog;

/**
 * Special case dialog for adding security elements required by the
 * JavaEE 7.0 default template. Asks the user to enter a password
 * for the keyStore element and optionally a user name and password
 * for a user registry.
 */
public class AddSecurityElementsDialog extends TitleAreaDialog {

    protected WebSphereRuntime wsRuntime;
    protected ConfigurationFile configFile;
    protected boolean appSecurityEnabled;
    protected Text keystorePassText;
    protected Button addUserRegistryButton;
    protected Text userNameText;
    protected Text userPasswordText;

    /**
     * For Use by Custom Liberty Runtime Providers Only.
     *
     * @param parent
     * @param wsRuntime
     * @param configFile
     * @param appSecurityEnabled
     */
    public AddSecurityElementsDialog(Shell parent, WebSphereRuntime wsRuntime, ConfigurationFile configFile, boolean appSecurityEnabled) {
        super(parent);
        this.wsRuntime = wsRuntime;
        this.configFile = configFile; // This is the src config file and it will be updated instead of the one in the target
        this.appSecurityEnabled = appSecurityEnabled;
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
    }

    public AddSecurityElementsDialog(Shell parent, WebSphereServerInfo serverInfo, boolean appSecurityEnabled) {
        super(parent);
        this.wsRuntime = serverInfo.getWebSphereRuntime();
        configFile = serverInfo.getConfigRoot();
        this.appSecurityEnabled = appSecurityEnabled;
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.addRequiredElementsLabel);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        if (appSecurityEnabled) {
            setTitle(Messages.addSecurityElementsTitle);
            setMessage(Messages.addSecurityElementsDescription);
        } else {
            setTitle(Messages.addKeystoreElementTitle);
            setMessage(Messages.addKeystoreElementDescription);
        }

        final Shell shell = parent.getShell();

        final Composite composite = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        layout.numColumns = 3;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
        data.widthHint = 300;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        // Add the text entry field for the keyStore password and Set button
        // for the password dialog
        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.addKeystoreElementPasswordLabel);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        data.widthHint = 130;
        label.setLayoutData(data);

        keystorePassText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        data = new GridData(GridData.FILL, GridData.FILL, true, false);
        keystorePassText.setLayoutData(data);

        final Button keystorePasswordSetButton = new Button(composite, SWT.PUSH);
        keystorePasswordSetButton.setText(Messages.keyStorePasswordSetButton);
        data = new GridData(GridData.FILL, GridData.FILL, false, false);
        keystorePasswordSetButton.setLayoutData(data);

        keystorePassText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                validate();
            }
        });

        keystorePasswordSetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PasswordDialog dialog = new PasswordDialog(shell, false, wsRuntime);
                if (dialog.open() == IStatus.OK) {
                    String password = dialog.getEncodedPassword();
                    if (password != null) {
                        keystorePassText.setText(password);
                        validate();
                    }
                }
            }
        });

        if (appSecurityEnabled) {
            // Add the button and user name and password fields for creating a user registry.
            addUserRegistryButton = new Button(composite, SWT.CHECK);
            addUserRegistryButton.setText(Messages.addUserRegistryButton);
            data = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
            data.horizontalSpan = 3;
            data.verticalIndent = 5;
            addUserRegistryButton.setLayoutData(data);

            final Composite userRegistryComposite = new Composite(composite, SWT.NONE);
            layout = new GridLayout();
            layout.marginHeight = 11;
            layout.marginWidth = 0;
            layout.horizontalSpacing = 5;
            layout.verticalSpacing = 7;
            layout.numColumns = 3;
            userRegistryComposite.setLayout(layout);
            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            data.horizontalSpan = 3;
            data.horizontalIndent = 50;
            userRegistryComposite.setLayoutData(data);
            userRegistryComposite.setFont(composite.getFont());

            label = new Label(userRegistryComposite, SWT.NONE);
            label.setText(Messages.addUserRegistryName);
            data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
            data.widthHint = 80;
            label.setLayoutData(data);

            userNameText = new Text(userRegistryComposite, SWT.BORDER);
            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            userNameText.setLayoutData(data);

            //dummy label to fill empty slot so that username and password text field end on same column
            Label dummy = new Label(userRegistryComposite, SWT.NONE);
            dummy.getAlignment();

            label = new Label(userRegistryComposite, SWT.NONE);
            label.setText(Messages.addUserRegistryPassword);
            data.widthHint = 80;
            data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
            label.setLayoutData(data);

            userPasswordText = new Text(userRegistryComposite, SWT.BORDER | SWT.PASSWORD);
            data = new GridData(GridData.FILL, GridData.FILL, true, false);
            userPasswordText.setLayoutData(data);

            final Button userPasswordSetButton = new Button(userRegistryComposite, SWT.PUSH);
            userPasswordSetButton.setText(Messages.basicUserRegistrySetButton);
            data = new GridData(GridData.FILL, GridData.FILL, false, false);
            userPasswordSetButton.setLayoutData(data);

            addUserRegistryButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setCompositeEnabled(userRegistryComposite, addUserRegistryButton.getSelection());
                    validate();
                }
            });

            userNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    validate();
                }
            });

            userPasswordText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent event) {
                    validate();
                }
            });

            userPasswordSetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    PasswordDialog dialog = new PasswordDialog(shell, false, wsRuntime);
                    if (dialog.open() == IStatus.OK) {
                        String password = dialog.getEncodedPassword();
                        if (password != null) {
                            userPasswordText.setText(password);
                            validate();
                        }
                    }
                }
            });

            addUserRegistryButton.setSelection(true);
            setCompositeEnabled(userRegistryComposite, true);
        }

        keystorePassText.setFocus();

        return composite;
    }

    protected void setCompositeEnabled(Composite composite, boolean enabled) {
        for (Control control : composite.getChildren()) {
            if (control instanceof Composite)
                setCompositeEnabled((Composite) control, enabled);
            else
                control.setEnabled(enabled);
        }
        composite.setEnabled(enabled);
    }

    void validate() {
        boolean valid = true;
        String password = keystorePassText.getText();
        if (password == null || password.isEmpty())
            valid = false;

        if (appSecurityEnabled && addUserRegistryButton.getSelection()) {
            String userName = userNameText.getText();
            String userPassword = userPasswordText.getText();
            if (userName == null || userName.isEmpty() || userPassword == null || userPassword.isEmpty())
                valid = false;
        }

        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(valid);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void create() {
        super.create();
        validate();
    }

    /** {@inheritDoc} */
    @Override
    protected void okPressed() {
        String password = keystorePassText.getText();
        Element elem = configFile.addElement(Constants.KEY_STORE);
        elem.setAttribute(Constants.INSTANCE_ID, Constants.DEFAULT_KEY_STORE);
        elem.setAttribute(Constants.PASSWORD_ATTRIBUTE, password);
        if (appSecurityEnabled && addUserRegistryButton.getSelection()) {
            elem = configFile.addElement(Constants.BASIC_USER_REGISTY);
            elem.setAttribute(Constants.INSTANCE_ID, "basic");
            elem.setAttribute("realm", "BasicRealm");
            Element userElem = configFile.addElement(elem, "user");
            userElem.setAttribute("name", userNameText.getText());
            userElem.setAttribute("password", userPasswordText.getText());
        }
        super.okPressed();
    }

}
