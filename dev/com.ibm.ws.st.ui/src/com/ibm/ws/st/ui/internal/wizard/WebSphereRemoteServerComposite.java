/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

public class WebSphereRemoteServerComposite extends AbstractRemoteServerComposite {

    Group connectionInfo;
    Text userText;
    Text passwordText;
    Text portText;
    Button connectButton;
    Label remoteServerOutputPath;
    String userName, pass, portNum;

    boolean isComplete = false;

    protected WebSphereRemoteServerComposite(Composite parent, IWizardHandle wizard) {
        super(parent, wizard);

        wizard.setTitle(Messages.wizRemoteServerTitle);
        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));

        createControl();
    }

    /** {@inheritDoc} */
    @Override
    protected void setServer(IServerWorkingCopy newServer, UserDirectory userDir) {
        super.setServer(newServer, userDir);
        wizard.setDescription(NLS.bind(Messages.wizRemoteServerDescription, server.getServer().getHost()));
        connectionInfo.pack(true);
        connectionInfo.redraw();
        layout(true);
    }

    @Override
    protected void createControl() {
        setLayout(new GridLayout());
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        setLayoutData(layoutData);

        connectionInfo = new Group(this, SWT.NONE);
        connectionInfo.setText(Messages.wizRemoteConnectionGroup);
        // set layout for the connection info group
        int numCols = 3;
        GridLayout layout = new GridLayout(numCols, false);
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        connectionInfo.setLayout(layout);

        // set layout data for the connection info group
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        connectionInfo.setLayoutData(layoutData);

        // username label
        Label label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizRemoteUserLabel);

        // username text field
        userText = new Text(connectionInfo, SWT.BORDER);
        userText.setText("");
        userText.forceFocus();

        layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        userText.setLayoutData(layoutData);
        userText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                isComplete = false;
                validate();
            }
        });

        label = new Label(connectionInfo, SWT.NONE); // blank space

        // password label
        label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizRemotePasswordLabel);

        // password text
        passwordText = new Text(connectionInfo, SWT.BORDER | SWT.PASSWORD);
        passwordText.setText("");
        layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        passwordText.setLayoutData(layoutData);
        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                isComplete = false;
                validate();
            }
        });

        label = new Label(connectionInfo, SWT.NONE); // blank space

        // port label
        label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizRemotePortLabel);

        // port text
        portText = new Text(connectionInfo, SWT.BORDER);
        portText.setText("");
        layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        portText.setLayoutData(layoutData);
        portText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                isComplete = false;
                validate();
            }
        });

        // connect to server button
        connectButton = new Button(connectionInfo, SWT.NONE);
        connectButton.setText(Messages.wizRemoteConnect);
        connectButton.setToolTipText(Messages.wizRemoteConnectTooltip);

        connectButton.addSelectionListener(new SelectionAdapter() {

            /** {@inheritDoc} */
            @Override
            public void widgetSelected(SelectionEvent e) {
                validate(); // pre-connection validation to ensure required parameters are good
                if (wizard.getMessage() != null)
                    return;

                MultiStatus multiStatus = remoteConfigSetup(null);

                if (serverConfigDir != null)
                    remoteServerOutputPath.setText(serverConfigDir);
                else
                    remoteServerOutputPath.setText("");

                remoteServerOutputPath.getParent().layout(); //ensure the layout recalculates the size for this label

                if (multiStatus.isOK()) {
                    isComplete = true;
                    userName = userText.getText();
                    pass = passwordText.getText();
                    portNum = portText.getText();
                    setTreeInput();
                } else {
                    isComplete = false;
                    for (IStatus status : multiStatus.getChildren()) {
                        if (status != null && !status.isOK()) {
                            wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
                            break;
                        }
                    }
                    setTreeInput(null);
                }

                wizard.update(); // call update wizard to check isComplete and enable finish
            }
        });

        createConfigControl(this);

        // remote directory label
        label = new Label(this, SWT.NONE);
        label.setText(Messages.wizRemoteDirectoryLabel);

        // label that shows the remote configuration directory
        remoteServerOutputPath = new Label(this, SWT.NONE);
        remoteServerOutputPath.setText("");

        label = new Label(this, SWT.NONE);

        Composite fileSyncComposite = new Composite(this, SWT.NONE);
        layout = new GridLayout(numCols, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        fileSyncComposite.setLayout(layout);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        fileSyncComposite.setLayoutData(layoutData);

        label = new Label(fileSyncComposite, SWT.WRAP);
        label.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
        layoutData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
        label.setLayoutData(layoutData);

        Text text = new Text(fileSyncComposite, SWT.WRAP | SWT.READ_ONLY | SWT.NO_TRIM);
        text.setBackground(fileSyncComposite.getBackground());
        text.setForeground(fileSyncComposite.getForeground());
        text.setText(Messages.infoOnlyServerConfigSynchronized);
        layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        layoutData.widthHint = 500;
        text.setLayoutData(layoutData);
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        createUserDir(RemoteUtils.generateRemoteUsrDirName(server.getWebSphereRuntime()), monitor);
        saveCredentials();
    }

    @Override
    protected String getHost() {
        return server.getServer().getHost();
    }

    /** {@inheritDoc} */
    @Override
    protected String getUserId() {
        return userText.getText();
    }

    /** {@inheritDoc} */
    @Override
    protected String getUserPassword() {
        return passwordText.getText();
    }

    /** {@inheritDoc} */
    @Override
    protected String getPort() {
        return portText.getText();
    }

    @Override
    protected void performCancel() {
        discardTemporaryFiles();
    }

    private void saveCredentials() {
        server.setLooseConfigEnabled(false);
        server.setServerPassword(pass);
        server.setServerUserName(userName);
        server.setServerSecurePort(portNum);
    }

    /** {@inheritDoc} */
    @Override
    protected void init() {
        if (server == null)
            return;
        server.setDefaults(new NullProgressMonitor());
        server.setLooseConfigEnabled(false);
        server.setStopTimeout(60); // remote server takes longer than local server to stop
    }

    @Override
    public void validate() {
        String user = userText.getText();
        if (user == null || user.length() < 1) {
            wizard.setMessage(Messages.wizRemoteUserNotSet, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        String password = passwordText.getText();
        if (password == null || password.length() < 1) {
            wizard.setMessage(Messages.wizRemotePasswordNotSet, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        String port = portText.getText();
        try {
            if (port == null || port.length() < 1 || Integer.parseInt(portText.getText()) < 1) {
                wizard.setMessage(Messages.wizRemotePortNotSet, IMessageProvider.ERROR);
                wizard.update();
                return;
            }
        } catch (Throwable t) {
            // Exception indicates user set an invalid string for port
            wizard.setMessage(Messages.wizRemotePortNotSet, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        wizard.setMessage(null, IMessageProvider.NONE);
        wizard.update();
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    protected void setTreeInput() {
        if (serverWC == null)
            return;

        Document document = getServerConfigDocument();
        setTreeInput(document);
    }

    protected void setTreeInput(Document document) {
        try {
            if (document == null) {
                treeViewer.setInput(Messages.configNone);
                return;
            }
            // this may cause the generation of the metadata so figure out if we need to clean up
            final WebSphereRuntime wrt = (server == null) ? null : server.getWebSphereRuntime();
            final boolean metadataDirExistsBefore = (wrt == null) ? false : wrt.metadataDirectoryExists();

            treeViewer.setInput(document.getDocumentElement());

            // meta data did not exist before and was generated, so keep track
            // of the runtime id
            if (!metadataDirExistsBefore && (wrt != null && wrt.metadataDirectoryExists())) {
                addMetaDataRuntimeId(wrt.getRuntime());
            }
        } catch (Throwable t) {
            Trace.logError("Error loading config tree", t);
        }
    }
} // end of class
