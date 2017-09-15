/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.provisional.dialogs;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.crypto.CustomPasswordEncryptionInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.config.PasswordComponent;

/**
 * Password dialog allows user to enter a password.
 */
public class PasswordDialog extends TitleAreaDialog {
    protected boolean isSupportHash;
    protected WebSphereRuntime wsRuntime;
    protected String encodedPassword;
    protected PasswordComponent password;
    protected IProgressMonitor monitor;
    protected Map<String, CustomPasswordEncryptionInfo> customEncryptionMap;
    protected String encodingMethod;

    public PasswordDialog(Shell parent, boolean curIsSupportHash, WebSphereRuntime wsRuntime, Map<String, CustomPasswordEncryptionInfo> customEncryptionList) {
        super(parent);
        isSupportHash = curIsSupportHash;
        this.wsRuntime = wsRuntime;
        this.customEncryptionMap = customEncryptionList;
    }

    public PasswordDialog(Shell parent, boolean curIsSupportHash, WebSphereRuntime wsRuntime) {
        super(parent);
        isSupportHash = curIsSupportHash;
        this.wsRuntime = wsRuntime;
        this.customEncryptionMap = null;
    }

    /**
     * Use in situations where clients do not want to know the implementation details of the
     * Liberty Runtime (specifically, WebSphereRuntime).
     *
     * @param parent - shell
     * @param curIsSupportHash - show hash encoding or not
     */
    public PasswordDialog(Shell parent, boolean curIsSupportHash) {
        super(parent);
        isSupportHash = curIsSupportHash;
        this.wsRuntime = getWebSphereRuntime();
        this.customEncryptionMap = null;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.passwordDialogTitle);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            try {
                encodedPassword = password.getEncodedPassword(monitor);
                encodingMethod = password.getPasswordEncoding();
            } catch (Exception e) {
                setErrorMessage(e.getLocalizedMessage());
                return;
            }
        }
        super.buttonPressed(buttonId);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.passwordDialogLabel);
        setTitleImage(Activator.getImage(Activator.IMG_WIZ_SERVER));
        setMessage(Messages.passwordDialogMessage);

        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 5;
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.minimumWidth = 300;
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());

        password = new PasswordComponent(composite, wsRuntime, isSupportHash, customEncryptionMap);
        password.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                enableOKButton(password.validate().getSeverity() != IStatus.ERROR);
            }
        });

        ProgressMonitorPart progressMonitor = new ProgressMonitorPart(composite, null);
        data = new GridData(GridData.FILL, GridData.END, true, false);
        data.horizontalSpan = 2;
        progressMonitor.setLayoutData(data);
        monitor = progressMonitor;

        return composite;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        enableOKButton(false);
        return control;
    }

    protected void enableOKButton(boolean value) {
        getButton(IDialogConstants.OK_ID).setEnabled(value);
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public String getPasswordEncoding() {
        return encodingMethod;
    }

    /**
     * Use this specialized open method to check if the Liberty runtime is present.
     * If not then an error message will be displayed.
     *
     * @return
     */
    public int openWithRuntimeCheck() {
        // If there is no runtime defined, open the error message instead.
        if (this.wsRuntime == null)
        {
            MessageDialog.openError(getShell(), Messages.title, Messages.passwordDialogNoRuntime);
            return CANCEL;
        }
        return super.open();
    }

    private WebSphereRuntime getWebSphereRuntime() {
        WebSphereRuntime[] webSphereRuntimes = WebSphereUtil.getWebSphereRuntimes();
        // Pick Liberty Runtime
        if (webSphereRuntimes.length > 0)
        {
            return webSphereRuntimes[0];
        }
        return null;
    }
}
