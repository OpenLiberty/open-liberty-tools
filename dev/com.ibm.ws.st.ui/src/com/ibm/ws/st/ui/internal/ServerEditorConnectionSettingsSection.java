/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.IServerEditorPartInput;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.ibm.ws.st.core.internal.SetServerPasswordCommand;
import com.ibm.ws.st.core.internal.SetServerPortCommand;
import com.ibm.ws.st.core.internal.SetServerUserNameCommand;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;

/**
 * This class introduces Server Connection Settings information in the ServerEditor
 */
public class ServerEditorConnectionSettingsSection extends ServerEditorSection {

    protected WebSphereServer wsServer;
    protected Text userName, password, port;
    protected Button verifyButton;

    protected boolean updating;
    protected FormToolkit toolkit;

    protected enum STATE {
        NOT_STARTED, COMPLETE, CONNECTION_FAILED;
    }

    protected STATE validity = STATE.NOT_STARTED;
    protected PropertyChangeListener listener;
    protected boolean hostNameChanged = false;

    public ServerEditorConnectionSettingsSection() {
        super();
    }

    protected void addChangeListener() {
        listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (updating)
                    return;
                updating = true;
                if (wsServer.PROP_USER_NAME.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    if (newValue != null)
                        userName.setText(newValue);
                } else if (wsServer.SECURE_SERVER_CONNECTION_PASSWORD_KEY.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    if (newValue != null)
                        password.setText(newValue);
                } else if (wsServer.PROP_SECURE_PORT.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    if (newValue != null)
                        port.setText(newValue);
                } else if ("hostname".equals(event.getPropertyName())) {
                    hostNameChanged = true;
                }
                validatePage();
                updating = false;
            }
        };
        server.addPropertyChangeListener(listener);
        wsServer.addPropertyChangeListener(listener);
    }

    @Override
    /** {@inheritDoc} */
    public void createSection(Composite parent) {
        super.createSection(parent);

        toolkit = getFormToolkit(parent.getDisplay());

        Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION
                                                        | ExpandableComposite.FOCUS_TITLE);
        section.setText(Messages.editorServerConnectionSettingsTitle);
        section.setDescription(Messages.editorServerConnectionSettingsDescription);
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        Composite composite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginHeight = 15;
        layout.marginWidth = 15;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 5;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
        section.setClient(composite);

        //userName label 1st row
        Label label = new Label(composite, SWT.NONE);
        // reusing the remote server wizard labels
        label.setText(Messages.wizRemoteUserLabel);

        userName = new Text(composite, SWT.BORDER);
        userName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        userName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                updating = true;
                execute(new SetServerUserNameCommand(wsServer, userName.getText()));
                updating = false;
                validatePage();
            }
        });
        label = new Label(composite, SWT.NONE); // blank space

        //password label 2nd row
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.wizRemotePasswordLabel);

        password = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        password.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        password.setEchoChar('*');
        password.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                updating = true;
                execute(new SetServerPasswordCommand(wsServer, password.getText()));
                updating = false;
                validatePage();
            }
        });

        label = new Label(composite, SWT.NONE); // blank space

        //port label 3rd row
        label = new Label(composite, SWT.NONE);
        label.setText(Messages.wizRemotePortLabel);

        port = new Text(composite, SWT.BORDER);
        port.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        port.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                updating = true;
                execute(new SetServerPortCommand(wsServer, port.getText()));
                updating = false;
                validatePage();
            }
        });

        //verify button
        verifyButton = new Button(composite, SWT.NONE);
        verifyButton.setText(Messages.wizRemoteConnect);
        verifyButton.setToolTipText(Messages.editorVerifyConnectTooltip);

        final Composite comp = composite;
        verifyButton.addSelectionListener(new SelectionAdapter() {

            /** {@inheritDoc} */
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                validatePage();
                if (getErrorMessage() != null)
                    return;

                IRunnableWithProgress runnable = new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        IProgressMonitor mon = monitor;
                        if (mon == null)
                            mon = new NullProgressMonitor();
                        mon.beginTask(Messages.taskConnecting, 100);
                        mon.worked(10);
                        if (mon.isCanceled())
                            return;
                        mon.worked(10);
                        // attempt JMX connection
                        try {
                            JMXConnection connection = new JMXConnection(wsServer.getConnectionHost(), wsServer.getConnectionPort(), wsServer.getServerUserName(), wsServer.getTempServerConnectionPassword());
                            connection.connect();
                            validity = STATE.COMPLETE; // connection was successful
                        } catch (Exception e) {
                            validity = STATE.CONNECTION_FAILED;
                            if (Trace.ENABLED) {
                                Trace.trace(Trace.INFO, "Failed to connect to the server", e);
                            }
                        }
                        mon.done();
                    }
                };
                ProgressMonitorDialog dialog = new ProgressMonitorDialog(comp.getShell());
                try {
                    dialog.run(true, true, runnable);
                } catch (Exception ex) {
                    validity = STATE.CONNECTION_FAILED;
                    Trace.logError("An unexpected exception occured while connecting to the server", ex);
                }
                //display connection successful message
                if (validity.equals(STATE.COMPLETE))
                    MessageDialog.openInformation(getShell(), Messages.editorVerifyConnection, Messages.editorConnectionSuccessful);
                //display error message
                else if (validity.equals(STATE.CONNECTION_FAILED) || validity.equals(STATE.NOT_STARTED))
                    MessageDialog.openError(getShell(), Messages.editorVerifyConnectionError, com.ibm.ws.st.core.internal.Messages.remoteJMXConnectionFailure);
            }
        });
        initialize();
    }

    @Override
    public void dispose() {
        if (toolkit != null) {
            toolkit.dispose();
        }
        super.dispose();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        if (server != null) {
            wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            if (input instanceof IServerEditorPartInput) {
                readOnly = ((IServerEditorPartInput) input).isServerReadOnly();
            }
        }
        addChangeListener();
        initialize();
    }

    protected void initialize() {
        if (userName == null || password == null || port == null || wsServer == null)
            return;

        updating = true;
        if (!wsServer.isLocalSetup()) {
            String serverConnectionPassword = wsServer.getServerPassword();
            password.setText(serverConnectionPassword);
            wsServer.setTempServerConnectionPassword(serverConnectionPassword);
            userName.setText(wsServer.getServerUserName());
            port.setText(wsServer.getServerSecurePort());
        } else {
            //disable the fields if its not remote server
            userName.setEnabled(false);
            password.setEnabled(false);
            port.setEnabled(false);
            verifyButton.setEnabled(false);
        }
        validatePage();
        updating = false;
    }

    protected void validatePage() {
        //validate only for remote server
        if (!wsServer.isLocalSetup()) {
            if (hostNameChanged) { //if the hostname is changed to a remote host, enable the connection fields
                password.setEnabled(true);
                userName.setEnabled(true);
                port.setEnabled(true);
                verifyButton.setEnabled(true);
            }
            if (userName.getText() == null || userName.getText().length() < 1) {
                //reusing the error messages in Remote Wizard
                setErrorMessage(Messages.editorConnectionInfoValidationError);
                //disable the verify button if the validation on any of the fields fail
                verifyButton.setEnabled(false);
                return;
            }
            if (password.getText() == null || password.getText().length() < 1) {
                setErrorMessage(Messages.editorConnectionInfoValidationError);
                verifyButton.setEnabled(false);
                return;
            }

            try {
                if (port.getText() == null || port.getText().length() < 1 || Integer.parseInt(port.getText()) < 1) {
                    setErrorMessage(Messages.editorConnectionInfoValidationError);
                    verifyButton.setEnabled(false);
                    return;
                }
            } catch (NumberFormatException e) {
                setErrorMessage(NLS.bind(Messages.editorConnectionInfoPortValidationError, port.getText()));
                verifyButton.setEnabled(false);
                return;
            }
            hostNameChanged = false;
            verifyButton.setEnabled(true);
        } else { //when hostname is changed again to localhost, reset the fields server connection settings and disable them
            userName.setText("");
            password.setText("");
            port.setText("");
            password.setEnabled(false);
            userName.setEnabled(false);
            port.setEnabled(false);
            verifyButton.setEnabled(false);
        }
        setErrorMessage(null);
    }

    @Override
    public IStatus[] getSaveStatus() {
        String errorMessage = getErrorMessage();
        IStatus[] status = null;
        if (errorMessage != null) {
            status = new Status[1];
            status[0] = new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMessage);
        } else {
            // if there are no errors, save the password to secure storage once the editor is saved
            wsServer.setServerPassword(password.getText());
        }
        return status;
    }

}
