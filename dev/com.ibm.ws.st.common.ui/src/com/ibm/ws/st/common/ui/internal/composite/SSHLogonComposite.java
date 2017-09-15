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
package com.ibm.ws.st.common.ui.internal.composite;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.common.ui.internal.ContextIds;
import com.ibm.ws.st.common.ui.internal.Messages;
import com.ibm.ws.st.common.ui.internal.commands.SetServerStringAttributeCommand;

/**
 *
 */
public class SSHLogonComposite extends AbstractRemoteComposite {

    IServerWorkingCopy serverWc;

    final RemoteServerInfo remoteInfo;

    protected Text sshKeyFileText;

    protected Button browseKeyFileBtn;

    protected Text sshIdText;

    protected Text sshPasswordText;

    protected Control[] sshIdPasswdCtrls = new Control[7];

    protected PropertyChangeListener propertyListener;

    BaseRemoteComposite remoteSetupComp;

    public SSHLogonComposite(Composite parent, BaseRemoteComposite remoteSetupHandler, IServerWorkingCopy serverWc, RemoteServerInfo remoteInfo) {
        super(parent);
        this.remoteSetupComp = remoteSetupHandler;
        this.serverWc = serverWc;
        this.remoteInfo = remoteInfo == null ? new RemoteServerInfo(RemoteServerInfo.RemoteServerType.Liberty) : remoteInfo;
        createControl();
    }

    public void createControl() {
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();

        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 2;
        layout.marginWidth = 10;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 15;
        setLayout(layout);
        setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL));

        Label curLabel = createLabel(this, Messages.L_RemoteServerAuth_SSH_KeyFile);
        sshIdPasswdCtrls[0] = curLabel;
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        data.horizontalIndent = 10;
        curLabel.setLayoutData(data);
        sshKeyFileText = createTextField(this, SWT.NONE);
        sshIdPasswdCtrls[1] = sshKeyFileText;
        data = new GridData(GridData.FILL_HORIZONTAL);
        sshKeyFileText.setLayoutData(data);
        sshKeyFileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (remoteSetupComp.isUpdating())
                    return;
                remoteSetupComp.setUpdating(true);
                remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, sshKeyFileText.getText());
                remoteSetupComp.execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartLogonSSHKeyFileCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, sshKeyFileText.getText()));
                remoteSetupComp.setUpdating(false);
                remoteSetupComp.showValidationError(VALIDATION_INDEX_SSH_LOG_ON);
            }
        });
        helpSystem.setHelp(sshKeyFileText, ContextIds.REMOTE_SERVER_STARTUP_SSH_PRIVATE_KEY);

        browseKeyFileBtn = createButton(this, Messages.L_Browse, SWT.PUSH);
        sshIdPasswdCtrls[2] = browseKeyFileBtn;
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        browseKeyFileBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell());
                dialog.setFileName(sshKeyFileText.getText());
                String selectedFile = dialog.open();
                if (selectedFile != null) {
                    if (remoteSetupComp.isUpdating())
                        return;
                    remoteSetupComp.setUpdating(true);
                    sshKeyFileText.setText(selectedFile);
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, selectedFile);
                    remoteSetupComp.execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartLogonSSHKeyFileCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, selectedFile));
                    remoteSetupComp.setUpdating(false);
                    remoteSetupComp.showValidationError(VALIDATION_INDEX_SSH_LOG_ON);
                }
            }
        });

        curLabel = createLabel(this, Messages.L_RemoteServerAuth_SSHId);
        sshIdPasswdCtrls[3] = curLabel;
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        data.horizontalIndent = 10;
        curLabel.setLayoutData(data);
        sshIdText = createTextField(this, SWT.NONE);
        sshIdPasswdCtrls[4] = sshIdText;
        data = new GridData(GridData.FILL_HORIZONTAL);
        sshIdText.setLayoutData(data);
        sshIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (remoteSetupComp.isUpdating())
                    return;
                remoteSetupComp.setUpdating(true);
                remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID, sshIdText.getText());
                remoteSetupComp.execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartLogonSSHIdCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID, sshIdText.getText()));
                remoteSetupComp.setUpdating(false);
                remoteSetupComp.showValidationError(VALIDATION_INDEX_SSH_LOG_ON);
            }
        });
        helpSystem.setHelp(sshIdText, ContextIds.REMOTE_SERVER_STARTUP_SSH_USER_NAME);
        createLabel(this, "");

        curLabel = createLabel(this, Messages.L_RemoteServerAuth_SSHPassphrase);
        sshIdPasswdCtrls[5] = curLabel;
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        data.horizontalIndent = 10;
        curLabel.setLayoutData(data);
        sshPasswordText = createTextField(this, SWT.NONE);
        sshIdPasswdCtrls[6] = sshPasswordText;
        data = new GridData(GridData.FILL_HORIZONTAL);
        sshPasswordText.setLayoutData(data);
        sshPasswordText.setEchoChar('*');
        sshPasswordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (remoteSetupComp.isUpdating())
                    return;
                remoteSetupComp.setUpdating(true);
                remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE, sshPasswordText.getText());
                remoteSetupComp.execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartLogonSSHPwdCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE, sshPasswordText.getText()));
                remoteSetupComp.setUpdating(false);
                remoteSetupComp.showValidationError(VALIDATION_INDEX_SSH_LOG_ON);
            }
        });
        helpSystem.setHelp(sshPasswordText, ContextIds.REMOTE_SERVER_STARTUP_SSH_PASSPHRASE);
    }

    public void initializeValues() {
        // The updating flag should already be set by the caller so don't set it here.
        if (sshKeyFileText != null) {
            sshKeyFileText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE));
        }
        if (sshIdText != null) {
            sshIdText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID));
        }
        if (sshPasswordText != null) {
            sshPasswordText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE));
        }
    }

    public void clearValues() {
        sshKeyFileText.setText("");
        sshIdText.setText("");
        sshPasswordText.setText("");
    }

    public void setEnablement(boolean enable) {
        for (Control ctrl : sshIdPasswdCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(enable);
            }
        }
    }

    public String validate() {
        String msg = null;
        String s;
        if (sshKeyFileText != null) {
            s = sshKeyFileText.getText();
            if (s != null && !s.trim().isEmpty()) {
                //good.  We don't verify if the key file exists.  The user may put the file there later.
            } else {
                msg = Messages.E_RemoteServer_SSH_LOGON;
                return msg;
            }
        }
        if (sshIdText != null) {
            s = sshIdText.getText();
            if (s != null && !s.trim().isEmpty()) {
                //good
            } else {
                msg = Messages.E_RemoteServer_SSH_LOGON;
                return msg;
            }
        }
        //don't need to verify passphrase
        return msg;
    }

    public void handlePropertyChange(PropertyChangeEvent event) {
        if (RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE.equals(event.getPropertyName())) {
            remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, event.getNewValue());
        } else if (RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID.equals(event.getPropertyName())) {
            String newValue = (String) event.getNewValue();
            remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID, newValue);
            if (newValue != null)
                sshIdText.setText(newValue);
        } else if (RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE.equals(event.getPropertyName())) {
            String newValue = (String) event.getNewValue();
            remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE, newValue);
            if (newValue != null)
                sshPasswordText.setText(newValue);
        }
    }

    // These values should always be updated whenever entering this page while
    // creating a cloud instance
    public void cloudAlwaysUpdateTheseValues(String sshPassphrase, String sshKeyFile) {
        //RTC 50755: Passphrase not updated in Cloud Wizard
        if (sshPasswordText != null) {
            sshPasswordText.setText(sshPassphrase);
        }
        if (sshKeyFileText != null) {
            sshKeyFileText.setText(sshKeyFile);
        }
    }
}
