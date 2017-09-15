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
package com.ibm.ws.st.common.ui.internal.composite;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.operations.IUndoableOperation;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo.CloudType;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo.RemoteServerType;
import com.ibm.ws.st.common.ui.internal.ContextIds;
import com.ibm.ws.st.common.ui.internal.Messages;
import com.ibm.ws.st.common.ui.internal.Trace;
import com.ibm.ws.st.common.ui.internal.commands.SetRemoteServerOSPwdCommand;
import com.ibm.ws.st.common.ui.internal.commands.SetServerBooleanAttributeCommand;
import com.ibm.ws.st.common.ui.internal.commands.SetServerIntAttributeCommand;
import com.ibm.ws.st.common.ui.internal.commands.SetServerStringAttributeCommand;

public abstract class AbstractRemoteServerStartupComposite extends BaseRemoteComposite {

    IServerWorkingCopy serverWc;

    protected PropertyChangeListener propertyListener;

    ArrayList<CommandListener> commandListeners = new ArrayList<CommandListener>(1);

    protected Button isEnableRemoteServerStartupCheckbox = null;

    protected boolean enableRemoteServerStartup = true;

    protected boolean isLocalhost = true;

    protected Text wasProfilePathText;

    protected Text libertyRuntimePathText;

    protected Text libertyConfigPathText;

    protected Button isWindowsBtn, isLinuxBtn, isMACBtn, isOtherBtn;

    protected List<Button> platformButtons = new ArrayList<Button>(4);

    protected Button isOSLogonBtn;

    protected Button isSSHBtn;

    protected Button browseKeyFileBtn;

    protected Text osIdText;

    public Text osPasswordText;

    public SSHLogonComposite sshComposite;

    protected Text debugPortText;

    /**
     * the label and the text field
     */
    protected Control[] installPathCtrls = new Control[4];

    /**
     * the label and 3 radio buttons
     */
    protected List<Control> platformCtrls = new ArrayList<Control>(5);

    /**
     * The label and 2 radio buttons
     */
    protected Control[] logonCtrls = new Control[3];

    /**
     * The label and text field of the id and passwd of the OS logon
     */
    protected Control[] osIdPasswdCtrls = new Control[4];

    /**
     * The labels and text field of the debug port that will be passed into the vmargs
     */
    protected Control[] debugCtrls = new Control[3];

    protected String[] validationErrors = new String[7];

    protected boolean isPageComplete = false;
    protected boolean enabledErrorCheck = false;

    protected static boolean isIBMJRE = false;

    // Keep track of the last instance type (eg. cloud or non-cloud)
    private CloudType lastInstanceType = CloudType.NON_CLOUD;

    boolean isCloudServer;

    boolean isDockerServer;

    final RemoteServerInfo remoteInfo;

    protected boolean updating;

    static {
        if (System.getProperty("java.vendor").indexOf("IBM") > -1) {
            isIBMJRE = true;
        } else {
            // With hybrid JDKs, the vendor may not be IBM.  Check for the com.ibm.security.util.ObjectIdentifier class.
            try {
                if (Class.forName("com.ibm.security.util.ObjectIdentifier", false, AbstractRemoteServerStartupComposite.class.getClassLoader()) != null) {
                    isIBMJRE = true;
                }
            } catch (Throwable t) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "The JRE does not appear to be an IBM JRE", t);
                }
            }
        }
    }

    public AbstractRemoteServerStartupComposite(Composite parent, IServerWorkingCopy serverWc, RemoteServerInfo remoteInfo, boolean isCloudServer) {
        this(parent, serverWc, remoteInfo, isCloudServer, false);
    }

    public AbstractRemoteServerStartupComposite(Composite parent, IServerWorkingCopy serverWc, RemoteServerInfo remoteInfo, boolean isCloudServer, boolean isDockerServer) {
        super(parent);
        // default is TWAS mode
        this.serverWc = serverWc;
        this.remoteInfo = remoteInfo == null ? new RemoteServerInfo(RemoteServerInfo.RemoteServerType.Liberty) : remoteInfo;
        this.isCloudServer = isCloudServer;
        this.isDockerServer = isDockerServer;
        createControl();
    }

    public void createControl() {
        // top level group
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 5;
        layout.marginWidth = 10;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 15;
        setLayout(layout);
        setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
//no help on the whole section		helpSystem.setHelp(this, ContextIds.INSTANCE_EDITOR_REMOTE_SERVER_STARTUP_SECTION);

        GridData data;

        // Is enable remote server start up
        isEnableRemoteServerStartupCheckbox = new Button(this, SWT.CHECK);
        isEnableRemoteServerStartupCheckbox.setText(Messages.L_RemoteServerEnableStart);
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        data.horizontalSpan = 2;
        isEnableRemoteServerStartupCheckbox.setLayoutData(data);
        isEnableRemoteServerStartupCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                updating = true;
                remoteInfo.putBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, isEnableRemoteServerStartupCheckbox.getSelection());
                execute(new SetServerBooleanAttributeCommand(Messages.L_SetRemoteServerStartupEnableCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, isEnableRemoteServerStartupCheckbox.getSelection()));
                setCtrlEnablement(isEnableRemoteServerStartupCheckbox.getSelection());
                updating = false;
                showValidationError(VALIDATION_STARTUP_ENABLED);
            }
        });
        helpSystem.setHelp(isEnableRemoteServerStartupCheckbox, ContextIds.REMOTE_SERVER_STARTUP_IS_ENABLED);

        if (isDockerServer) {
            // RTC 217076: ensure remote settings section of server editor is disabled for a local
            // docker server.
            if (remoteInfo.getBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, true)) {
                isEnableRemoteServerStartupCheckbox.setSelection(true);
            }
            isEnableRemoteServerStartupCheckbox.setVisible(false);
            data.exclude = true;
        }

        Label curLabel = null;
        Text curText = createText(this, Messages.L_RemoteServerPlatform, 0, 2);
        platformCtrls.add(curText);
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        curText.setLayoutData(data);

        Composite platformComposite = createSubComposite(this, 1);
        layout = new GridLayout();

        if (!isDockerServer) {
            isWindowsBtn = createButton(platformComposite, Messages.L_RemoteServerPlatform_WIN, SWT.RADIO);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalIndent = 10;
            isWindowsBtn.setLayoutData(data);
            platformButtons.add(isWindowsBtn);
            isWindowsBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (updating || !isWindowsBtn.getSelection())
                        return;
                    updating = true;
                    setPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
                    remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
                    execute(new SetServerIntAttributeCommand(Messages.L_SetRemoteServerStartPlatformCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS));
                    showValidationError(VALIDATION_INDEX_PLATFORM);
                    updating = false;
                }
            });
            helpSystem.setHelp(isWindowsBtn, ContextIds.REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_WINDOWS);
        }

        if (remoteInfo.getMode().equals(RemoteServerType.TWAS) || isDockerServer) {
            isLinuxBtn = createButton(platformComposite, Messages.L_RemoteServerPlatform_LINUX, SWT.RADIO);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalIndent = 10;
            isLinuxBtn.setLayoutData(data);
            platformButtons.add(isLinuxBtn);
            isLinuxBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (updating || !isLinuxBtn.getSelection())
                        return;
                    updating = true;
                    setPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX);
                    remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX);
                    execute(new SetServerIntAttributeCommand(Messages.L_SetRemoteServerStartPlatformCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX));
                    updating = false;
                    showValidationError(VALIDATION_INDEX_PLATFORM);
                }
            });
            helpSystem.setHelp(isLinuxBtn, ContextIds.REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_LINUX);
        }

        if (isDockerServer) {
            isMACBtn = createButton(platformComposite, Messages.L_RemoteServerPlatform_MAC, SWT.RADIO);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalIndent = 10;
            isMACBtn.setLayoutData(data);
            platformButtons.add(isMACBtn);
            isMACBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (updating || !isMACBtn.getSelection())
                        return;
                    updating = true;
                    setPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC);
                    remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC);
                    execute(new SetServerIntAttributeCommand(Messages.L_SetRemoteServerStartPlatformCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC));
                    updating = false;
                    showValidationError(VALIDATION_INDEX_PLATFORM);
                }
            });
            helpSystem.setHelp(isMACBtn, ContextIds.REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_MAC);
        }

        if (remoteInfo.getMode().equals(RemoteServerType.Liberty) && !isDockerServer) {
            isOtherBtn = createButton(platformComposite, Messages.L_RemoteServerPlatform_Other, SWT.RADIO);
            isOtherBtn.setToolTipText(Messages.L_RemoteServerPlatform_OtherTooltip);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalIndent = 10;
            isOtherBtn.setLayoutData(data);
            platformButtons.add(isOtherBtn);
            isOtherBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (updating || !isOtherBtn.getSelection())
                        return;
                    updating = true;
                    setPlatform(RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER);
                    remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER);
                    execute(new SetServerIntAttributeCommand(Messages.L_SetRemoteServerStartPlatformCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER));
                    updating = false;
                    showValidationError(VALIDATION_INDEX_PLATFORM);
                }
            });
            helpSystem.setHelp(isOtherBtn, ContextIds.REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_MAC);
        }

        platformCtrls.addAll(platformButtons);

        if (!isDockerServer) {

            // Server path
            Composite pathComposite = new Composite(this, SWT.NONE);
            layout = new GridLayout();
            layout.numColumns = 2;
            layout.marginHeight = 2;
            layout.marginWidth = 5;
            layout.verticalSpacing = 5;
            layout.horizontalSpacing = 15;
            pathComposite.setLayout(layout);
            data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
            pathComposite.setLayoutData(data);

            if (remoteInfo.getMode().equals(RemoteServerType.TWAS)) {
                curLabel = createLabel(pathComposite, Messages.L_RemoteServerPath);
                installPathCtrls[0] = curLabel;
                data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
                curLabel.setLayoutData(data);
                wasProfilePathText = createTextField(pathComposite, SWT.NONE);
                installPathCtrls[1] = wasProfilePathText;
                data = new GridData(GridData.FILL_HORIZONTAL);
                wasProfilePathText.setLayoutData(data);
                wasProfilePathText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        if (updating)
                            return;
                        updating = true;
                        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH, wasProfilePathText.getText());
                        execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartProfilePathCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH, wasProfilePathText.getText()));
                        updating = false;
                        showValidationError(VALIDATION_INDEX_PATHS);
                    }
                });
                helpSystem.setHelp(wasProfilePathText, ContextIds.REMOTE_SERVER_STARTUP_SERVER_PROFILE_PATH);
            } else {
                curLabel = createLabel(pathComposite, Messages.L_RemoteServerLibertyRuntimePath);
                installPathCtrls[0] = curLabel;
                data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
                curLabel.setLayoutData(data);
                libertyRuntimePathText = createTextField(pathComposite, SWT.NONE);
                installPathCtrls[1] = libertyRuntimePathText;
                data = new GridData(GridData.FILL_HORIZONTAL);
                libertyRuntimePathText.setLayoutData(data);
                libertyRuntimePathText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        if (updating)
                            return;
                        updating = true;
                        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH, libertyRuntimePathText.getText());
                        execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartRuntimePathCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH, libertyRuntimePathText.getText()));
                        updating = false;
                        showValidationError(VALIDATION_INDEX_PATHS);
                    }
                });
                helpSystem.setHelp(libertyRuntimePathText, ContextIds.REMOTE_SERVER_STARTUP_LIBERTY_RUNTIME_PATH);

                curLabel = createLabel(pathComposite, Messages.L_RemoteServerLibertyConfigPath);
                installPathCtrls[2] = curLabel;
                data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
                curLabel.setLayoutData(data);
                libertyConfigPathText = createTextField(pathComposite, SWT.NONE);
                installPathCtrls[3] = libertyConfigPathText;
                data = new GridData(GridData.FILL_HORIZONTAL);
                libertyConfigPathText.setLayoutData(data);
                libertyConfigPathText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        if (updating)
                            return;
                        updating = true;
                        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH, libertyConfigPathText.getText());
                        execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartConfigPathCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH, libertyConfigPathText.getText()));
                        updating = false;
                        showValidationError(VALIDATION_INDEX_PATHS);
                    }
                });
                helpSystem.setHelp(libertyConfigPathText, ContextIds.REMOTE_SERVER_STARTUP_LIBERTY_CONFIG_PATH);
            }

        }

        curText = createText(this, Messages.L_RemoteServerAuthMethod, 0, 2);
        curText.setLayoutData(data);
        logonCtrls[0] = curText;
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        curText.setLayoutData(data);

        // logon method
        Composite logonComposite = createSubComposite(this, 1);

        isOSLogonBtn = createButton(logonComposite, Messages.L_RemoteServerAuth_OS, SWT.RADIO);
        logonCtrls[1] = isOSLogonBtn;
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 10;
        isOSLogonBtn.setLayoutData(data);
        isOSLogonBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                updating = true;
                setLogonCtrls(true);
                remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, Integer.valueOf(RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS));
                execute(new SetServerIntAttributeCommand(Messages.L_SetRemoteServerStartLogonMethodCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS));
                updating = false;
                showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
            }
        });
        helpSystem.setHelp(isOSLogonBtn, ContextIds.REMOTE_SERVER_STARTUP_OS_AUTHENTICATION);

        Composite osLogonComposite = createSubComposite(logonComposite, 2);
        curLabel = createLabel(osLogonComposite, Messages.L_RemoteServerAuth_LogonId);
        osIdPasswdCtrls[0] = curLabel;
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        data.horizontalIndent = 10;
        curLabel.setLayoutData(data);
        osIdText = createTextField(osLogonComposite, SWT.NONE);
        osIdPasswdCtrls[1] = osIdText;
        data = new GridData(GridData.FILL_HORIZONTAL);
        osIdText.setLayoutData(data);
        osIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                updating = true;
                remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID, osIdText.getText());
                execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartLogonOSIdCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID, osIdText.getText()));
                updating = false;
                showValidationError(VALIDATION_INDEX_OS_LOG_ON);
            }
        });
        helpSystem.setHelp(osIdText, ContextIds.REMOTE_SERVER_STARTUP_OS_USER_NAME);

        curLabel = createLabel(osLogonComposite, Messages.L_RemoteServerAuth_LogonPassword);
        osIdPasswdCtrls[2] = curLabel;
        data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        data.horizontalIndent = 10;
        curLabel.setLayoutData(data);
        osPasswordText = createTextField(osLogonComposite, SWT.NONE);
        osIdPasswdCtrls[3] = osPasswordText;
        data = new GridData(GridData.FILL_HORIZONTAL);
        osPasswordText.setLayoutData(data);
        osPasswordText.setEchoChar('*');
        osPasswordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                updating = true;
                remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD, osPasswordText.getText());
                //OS password for Liberty server must be stored in SecuredPreferences file rather than server object
                if (remoteInfo.getMode().equals(RemoteServerInfo.RemoteServerType.Liberty))
                    execute(new SetRemoteServerOSPwdCommand(Messages.L_SetRemoteServerStartLogonOSPwdCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD, osPasswordText.getText(), remoteInfo));
                else
                    execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartLogonOSPwdCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD, osPasswordText.getText()));
                updating = false;
                showValidationError(VALIDATION_INDEX_OS_LOG_ON);
            }
        });
        helpSystem.setHelp(osPasswordText, ContextIds.REMOTE_SERVER_STARTUP_OS_PASSWORD);

        // SSH
        isSSHBtn = createButton(logonComposite, Messages.L_RemoteServerAuth_SSH, SWT.RADIO);
        logonCtrls[2] = isSSHBtn;
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalIndent = 10;
        isSSHBtn.setLayoutData(data);
        isSSHBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (updating)
                    return;
                updating = true;
                setLogonCtrls(false);
                remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_SSH);
                execute(new SetServerIntAttributeCommand(Messages.L_SetRemoteServerStartLogonMethodCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_SSH));
                updating = false;
                showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
            }
        });
        helpSystem.setHelp(isSSHBtn, ContextIds.REMOTE_SERVER_STARTUP_SSH_AUTHENTICATION);

        sshComposite = new SSHLogonComposite(logonComposite, this, serverWc, remoteInfo);

        if (remoteInfo.getMode().equals(RemoteServerType.Liberty) && this instanceof ServerEditorRemoteStartupComposite) {
            curText = createText(this, Messages.L_RemoteServerDebugPortDescription, 2, 0);
            debugCtrls[0] = curText;
            data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
            curText.setLayoutData(data);

            Composite debugComposite = new Composite(this, SWT.NONE);
            layout = new GridLayout();
            layout.numColumns = 2;
            layout.marginHeight = 2;
            layout.marginWidth = 20;
            layout.verticalSpacing = 5;
            layout.horizontalSpacing = 15;
            debugComposite.setLayout(layout);
            data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
            debugComposite.setLayoutData(data);

            curLabel = createLabel(debugComposite, Messages.L_RemoteServerDebugPortLabel);
            debugCtrls[1] = curLabel;
            data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
            curLabel.setLayoutData(data);
            debugPortText = createTextField(debugComposite, SWT.NONE);
            debugCtrls[2] = debugPortText;
            data = new GridData(GridData.FILL_HORIZONTAL);
            debugPortText.setLayoutData(data);
            debugPortText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    if (updating)
                        return;
                    updating = true;
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT, debugPortText.getText());
                    execute(new SetServerStringAttributeCommand(Messages.L_SetRemoteServerStartDebugPortCommandDescription, serverWc, RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT, debugPortText.getText()));

                    updating = false;
                    showValidationError(VALIDATION_INDEX_PORT);
                }
            });
            helpSystem.setHelp(debugPortText, ContextIds.REMOTE_SERVER_STARTUP_DEBUG_PORT);
        }
    }

    public PropertyChangeListener getChangeListener() {

        if (propertyListener != null)
            return null;
        propertyListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (updating)
                    return;
                updating = true;
                if (RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED.equals(event.getPropertyName())) {
                    Boolean b = (Boolean) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, b);
                    isEnableRemoteServerStartupCheckbox.setSelection(b.booleanValue());
                    handleSetEnableRemoteStartup();
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH, newValue);
                    if (newValue != null)
                        wasProfilePathText.setText(newValue);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH, newValue);
                    if (newValue != null)
                        libertyRuntimePathText.setText(newValue);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH, newValue);
                    if (newValue != null)
                        libertyConfigPathText.setText(newValue);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM.equals(event.getPropertyName())) {
                    Integer platform = ((Integer) event.getNewValue());
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, platform);
                    setPlatform(platform);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD.equals(event.getPropertyName())) {
                    Integer method = ((Integer) event.getNewValue());
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, method);
                    boolean isOSLogon = method == RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS ? true : false;
                    setLogonCtrls(isOSLogon);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID, newValue);
                    if (newValue != null)
                        osIdText.setText(newValue);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD.equals(event.getPropertyName())) {
                    String newValue = (String) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD, newValue);
                    if (newValue != null)
                        osPasswordText.setText(newValue);
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE.equals(event.getPropertyName())) {
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, event.getNewValue());
                } else if (RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT.equals(event.getPropertyName())) {
                    Integer newValue = (Integer) event.getNewValue();
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT, newValue);
                    if (newValue != null)
                        debugPortText.setText(newValue.toString());
                } else if ("hostname".equals(event.getPropertyName())) {
                    if (enableRemoteServerStartup) {
                        String curHostName = (String) event.getNewValue();
                        isLocalhost = SocketUtil.isLocalhost(curHostName);
                        remoteInfo.putBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, !isLocalhost);
                        isEnableRemoteServerStartupCheckbox.setEnabled(!isLocalhost);
                        handleSetEnableRemoteStartup();
                    } else {
                        isEnableRemoteServerStartupCheckbox.setEnabled(false);
                    }
                } else {
                    sshComposite.handlePropertyChange(event);
                }
                showValidationError(VALIDATION_INDEX_PATHS);
                showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
                showValidationError(VALIDATION_INDEX_OS_LOG_ON);
                updating = false;
            }
        };
        return propertyListener;
    }

    public void setEnableRemoteServerStartup(boolean setting) {
        enableRemoteServerStartup = setting;
    }

    protected Composite createSubComposite(Composite rootComposite, int numColumn) {
        GridLayout layout;
        GridData data;
        Composite platformComposite = new Composite(rootComposite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = numColumn;
        layout.marginHeight = 2;
        layout.marginWidth = 10;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 15;
        platformComposite.setLayout(layout);
        data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
        platformComposite.setLayoutData(data);
        return platformComposite;
    }

    protected int getPlatform() {
        int i = RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS;
        if (remoteInfo != null) {
            i = remoteInfo.getIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
        }
        return i;
    }

    protected boolean isOSLogon() {
        boolean b = true;
        if (remoteInfo != null) {
            int i = remoteInfo.getIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS);
            if (i == RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS) {
                b = true;
            } else {
                b = false;
            }
        }
        return b;
    }

    /**
     * Save the current wizard user input values.
     */
    public void initializeValues() {
        updating = true;
        if (isEnableRemoteServerStartupCheckbox != null) {
            if (enableRemoteServerStartup) {
                boolean isLocalhost = SocketUtil.isLocalhost(serverWc.getHost());

                boolean remoteStartSelected = !isLocalhost && serverWc.getAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, true);
                isEnableRemoteServerStartupCheckbox.setEnabled(!isLocalhost);
                // if the remote info doesn't have it set then default is true
                if (!remoteInfo.containsKey(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED))
                    remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, Boolean.valueOf(remoteStartSelected));
                isEnableRemoteServerStartupCheckbox.setSelection(remoteInfo.getBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, remoteStartSelected));
            } else {
                isEnableRemoteServerStartupCheckbox.setEnabled(false);
            }
        }

        if (isWindowsBtn != null || (isDockerServer && isLinuxBtn != null)) {
            int platform = getPlatform();
            setPlatform(platform);
        }

        if (wasProfilePathText != null) {
            wasProfilePathText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH));
        }

        if (libertyRuntimePathText != null) {
            libertyRuntimePathText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH));
        }

        if (libertyConfigPathText != null) {
            libertyConfigPathText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH));
        }

        setLogonCtrls(isOSLogon());
        if (osIdText != null) {
            osIdText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID));
        }

        if (osPasswordText != null) {
            osPasswordText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD));
            remoteInfo.setTempRemoteOSPassword(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD));
        }

        if (sshComposite != null) {
            sshComposite.initializeValues();
        }

        if (debugPortText != null) {
            int debugPort = serverWc.getAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT, 7777);
            debugPortText.setText(Integer.toString(debugPort));
        }

        //We don't want to do error check until something is changed.
        enabledErrorCheck = true;

        // Check if the instance being created is for cloud use or not. If it is,
        // use cloud defaults and disable from making changes
        if (isCreatingCloudServerInstance())
            initializeForCloudServerInstance();

        setCtrlEnablement(isEnableRemoteServerStartupCheckbox.getSelection());
        updating = false;

        showValidationError(VALIDATION_INDEX_PATHS);
        showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
        showValidationError(VALIDATION_INDEX_OS_LOG_ON);
    }

    protected void setLogonCtrls(boolean isOSLogon) {
        for (Control c : osIdPasswdCtrls) {
            if (c == null)
                return;
        }
        if (sshComposite == null) {
            return;
        }
        if (isOSLogonBtn == null || isSSHBtn == null)
            return;

        isOSLogonBtn.setSelection(isOSLogon);
        isSSHBtn.setSelection(!isOSLogon);
        for (Control c : osIdPasswdCtrls) {
            c.setEnabled(isOSLogon);
        }
        sshComposite.setEnablement(!isOSLogon);
    }

    protected String validate(int key) {
        String msg = null;
        switch (key) {
            case VALIDATION_STARTUP_ENABLED:
                if (isEnableRemoteServerStartupCheckbox != null && !isEnableRemoteServerStartupCheckbox.getSelection()) {
                    return null;
                }
                break;
            case VALIDATION_INDEX_PATHS:
                msg = validatePaths();
                validationErrors[key] = msg;
                break;
            case VALIDATION_INDEX_PLATFORM:
                msg = validatePlatform();
                validationErrors[key] = msg;

                // RTC 105335: the break statement is intentionally removed so that
                // when the OS type is switched, validation is done to ensure that Windows OS
                // login authentication is blocked when using non-IBM JRE
                //$FALL_THROUGH$
            case VALIDATION_INDEX_LOG_ON_METHOD:
                msg = validateLogonMethod();
                validationErrors[key] = msg;
                if (msg == null) { // need to clear the error
                    if (isOSLogonBtn != null && isOSLogonBtn.getSelection()) {
                        validationErrors[VALIDATION_INDEX_SSH_LOG_ON] = null;
                        msg = validate(VALIDATION_INDEX_OS_LOG_ON);
                    } else if (isSSHBtn != null && isSSHBtn.getSelection()) { //either one will be selected, but it will be called when the other is selected.
                        validationErrors[VALIDATION_INDEX_OS_LOG_ON] = null;
                        msg = validate(VALIDATION_INDEX_SSH_LOG_ON);
                    }
                }
                break;
            case VALIDATION_INDEX_OS_LOG_ON:
                msg = validateOSLogon();
                validationErrors[key] = msg;
                break;
            case VALIDATION_INDEX_SSH_LOG_ON:
                msg = validateSSHLogon();
                validationErrors[key] = msg;
                break;
            case VALIDATION_INDEX_PORT:
                msg = validatePorts();
                validationErrors[key] = msg;
                break;
            default:
        }

        if (isEnableRemoteServerStartupCheckbox != null && !isEnableRemoteServerStartupCheckbox.getSelection()) {
            return null;
        }

        if (msg == null) {
            for (int i = 0; i < validationErrors.length; i++) {
                if (validationErrors[i] != null) {
                    msg = validationErrors[i];
                    break;
                }
            }
        }
        return msg;
    }

    @Override
    protected abstract void showValidationError(int key);

    protected String validatePaths() {
        String msg = null;
        if (wasProfilePathText != null) {
            String s = wasProfilePathText.getText();
            if (s != null && !s.trim().isEmpty()) {
                // it is good
            } else {
                msg = Messages.E_RemoteServer_WAS_PATH;
            }
        }
        if (libertyRuntimePathText != null) {
            String s = libertyRuntimePathText.getText();
            if (s != null && !s.trim().isEmpty()) {
                // it is good
            } else {
                msg = Messages.E_RemoteServer_Liberty_Runtime_PATH;
            }
        }
        if (libertyConfigPathText != null) {
            String s = libertyConfigPathText.getText();
            if (s != null && !s.trim().isEmpty()) {
                // it is good
            } else {
                msg = Messages.E_RemoteServer_Liberty_Config_PATH;
            }
        }
        return msg;
    }

    protected String validatePlatform() {
        String msg = null;
        if ((isWindowsBtn != null && isWindowsBtn.getSelection() ||
             (isLinuxBtn != null && isLinuxBtn.getSelection()) ||
             (isMACBtn != null && isMACBtn.getSelection()) || (isOtherBtn != null && isOtherBtn.getSelection()))) {
            //good
        } else {
            msg = Messages.E_RemoteServer_WAS_PLATFORM;
        }
        return msg;
    }

    protected String validateLogonMethod() {
        String msg = null;
        if (isOSLogonBtn != null && isSSHBtn != null && (isOSLogonBtn.getSelection() || isSSHBtn.getSelection())) {
            //good
        } else {
            msg = Messages.E_RemoteServer_LOGON_METHOD;
        }
        return msg;
    }

    protected String validateOSLogon() {
        String msg = null;
        if (isOSLogonBtn != null && isOSLogonBtn.getSelection()) {
            // RTC 105335: block Windows OS login authentication when
            // using non-IBM JRE since remote utility does not support this scenario
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "isIBMJRE=" + isIBMJRE);
            }

            if (!isIBMJRE && isWindowsBtn != null && isWindowsBtn.getSelection()) {
                msg = Messages.E_RemoteServer_OS_LOGON_WIN_NON_IBM_JRE;
                return msg;
            }

            String s;
            if (osIdText != null) {
                s = osIdText.getText();
                if (s != null && !s.trim().isEmpty()) {
                    //good
                } else {
                    msg = Messages.E_RemoteServer_OS_LOGON;
                    return msg;
                }
            }
            if (osPasswordText != null) {
                s = osPasswordText.getText();
                if (s != null && !s.trim().isEmpty()) {
                    //good
                } else {
                    msg = Messages.E_RemoteServer_OS_LOGON;
                    return msg;
                }
            }
        }
        return msg;
    }

    protected String validateSSHLogon() {
        String msg = null;
        if (isSSHBtn != null && isSSHBtn.getSelection()) {
            if (sshComposite != null) {
                msg = sshComposite.validate();
            }
        }
        //don't need to verify passphrase
        return msg;
    }

    protected String validatePorts() {
        String msg = null;
        if (debugPortText == null || debugPortText.isVisible() == false || debugPortText.getEnabled() == false)
            return msg;
        try {
            if (!debugPortText.getText().isEmpty()
                && Integer.parseInt(debugPortText.getText()) > 0) {
                //good
            } else {
                msg = Messages.E_RemoteServerDebugValidation;
            }
        } catch (Exception e) {
            msg = Messages.E_RemoteServerDebugValidation;
        }
        return msg;
    }

    public boolean isPageComplete() {
        if (remoteInfo.isEmpty())
            return false;

        if (wasProfilePathText != null && (remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH)).compareTo(wasProfilePathText.getText()) != 0) {
            // this means the model is out of sync with the gui, so we will reset our values
            if (isCloudServer)
                initializeForCloudServerInstance();
            else
                reinitializeNonCloudDefaultValues();
            return false;
        }

        // when we switch between cloud and non-cloud the page is not complete
        if (isCloudServer && lastInstanceType.compareTo(CloudType.CLOUD) != 0) {
            return false;
        }

        if (!isCloudServer && lastInstanceType.compareTo(CloudType.CLOUD) == 0) {
            return false;
        }
        return isPageComplete;
    }

    @Override
    protected boolean isUpdating() {
        return updating;
    }

    @Override
    protected void setUpdating(boolean value) {
        updating = value;
    }

    private Text createText(Composite parent, String message, int span, int indent) {
        return createText(parent, message, span, indent, true);
    }

    private Text createText(Composite parent, String message, int span, int indent, boolean fillHorizontal) {
        Text txt = new Text(parent, SWT.NONE | SWT.WRAP);
        txt.setText(message);
        txt.setBackground(parent.getBackground());
        txt.setEditable(false);
        GridData data = null;
        if (fillHorizontal) {
            data = new GridData(GridData.FILL_HORIZONTAL);
        } else {
            data = new GridData();
        }
        if (span != 0) {
            data.horizontalSpan = span;
            txt.setLayoutData(data);
        }
        return txt;
    }

    ///// Handle defining cloud server instances
    // if use cloud is not selected but was at some point through the wizard pages
    // we need to remove any defaults we set for cloud and (re)initialize the page
    public boolean isCreatingCloudServerInstance() {
        if (isCloudServer) {
            lastInstanceType = CloudType.CLOUD;
            return true;
        }
        lastInstanceType = CloudType.NON_CLOUD;
        return false;
    }

    public void initializeForCloudServerInstance() {
        updating = true;
        isWindowsBtn.setSelection(false);
        isLinuxBtn.setSelection(true);
        isOSLogonBtn.setSelection(false);
        isSSHBtn.setSelection(true);
        for (Control c : platformCtrls) {
            if (c != null) {
                c.setEnabled(false);
            }
        }
        for (Control c : installPathCtrls) {
            if (c != null) {
                c.setEnabled(true);
            }
        }
        for (Control c : logonCtrls) {
            if (c != null) {
                c.setEnabled(false);
            }
        }

        for (Control ctrl : debugCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(false);
            }
        }

        for (Control c : osIdPasswdCtrls) {
            if (c != null) {
                c.setEnabled(false);
            }
        }

        if (sshComposite != null) {
            sshComposite.setEnablement(false);
        }

        if (remoteInfo != null) {
            if (wasProfilePathText != null)
                wasProfilePathText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_TWAS_PROFILE_PATH));
            if (libertyRuntimePathText != null)
                libertyRuntimePathText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH));
            if (libertyConfigPathText != null)
                libertyConfigPathText.setText(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH));
            if (sshComposite != null) {
                sshComposite.initializeValues();
            }
            if (osIdText != null)
                osIdText.setText("");
            if (osPasswordText != null)
                osPasswordText.setText("");
        }
        updating = false;
        isPageComplete = true;
    }

    public void reinitializeNonCloudDefaultValues() {
        if (wasProfilePathText != null)
            wasProfilePathText.setText("");
        if (libertyRuntimePathText != null)
            libertyRuntimePathText.setText("");
        if (libertyConfigPathText != null)
            libertyConfigPathText.setText("");
        sshComposite.clearValues();
        osIdText.setText("");
        osPasswordText.setText("");
        isSSHBtn.setSelection(false);
        isOSLogonBtn.setSelection(true);
        for (Control c : platformCtrls) {
            if (c != null) {
                c.setEnabled(true);
            }
        }
        for (Control c : installPathCtrls) {
            if (c != null) {
                c.setEnabled(true);
            }
        }
        for (Control c : logonCtrls) {
            if (c != null) {
                c.setEnabled(true);
            }
        }

        for (Control ctrl : debugCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(true);
            }
        }

        for (Control c : osIdPasswdCtrls) {
            if (c != null) {
                c.setEnabled(true);
            }
        }

        if (sshComposite != null) {
            sshComposite.setEnablement(false);
        }

        showValidationError(VALIDATION_INDEX_PATHS);
        showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
    }

    // These values should always be updated whenever entering this page while
    // creating a cloud instance
    public void cloudAlwaysUpdateTheseValues(String sshPassphrase, String sshKeyFile) {
        if (sshComposite != null) {
            sshComposite.cloudAlwaysUpdateTheseValues(sshPassphrase, sshKeyFile);
        }
    }

    protected void setCtrlEnablement(boolean b) {
        // only enable stuff here if we're not dealing with cloud otherwise let
        // the initializeForCloudServerInstance() take care of enabling the controls
        if (!b || (b && !isCreatingCloudServerInstance())) {
            for (Control c : platformCtrls) {
                if (c != null) {
                    c.setEnabled(b);
                }
            }
            for (Control c : installPathCtrls) {
                if (c != null) {
                    c.setEnabled(b);
                }
            }
            for (Control c : logonCtrls) {
                if (c != null) {
                    c.setEnabled(b);
                }
            }

            for (Control ctrl : debugCtrls) {
                if (ctrl != null) {
                    ctrl.setEnabled(b);
                }
            }

            if (!b) {
                for (Control c : osIdPasswdCtrls) {
                    if (c != null) {
                        c.setEnabled(b);
                    }
                }
                if (sshComposite != null) {
                    sshComposite.setEnablement(b);
                }
            } else {
                setLogonCtrls(isOSLogon());
            }
        }
        //handle cloud case
        if (isCreatingCloudServerInstance() && b) {
            initializeForCloudServerInstance();
        }
    }

    protected void handleSetEnableRemoteStartup() {
        if (isEnableRemoteServerStartupCheckbox != null) {
            boolean enabled = isEnableRemoteServerStartupCheckbox.getSelection() && remoteInfo.getBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, true);

            for (Control ctrl : installPathCtrls) {
                if (ctrl != null) {
                    ctrl.setEnabled(enabled);
                }
            }

            for (Control ctrl : platformCtrls) {
                if (ctrl != null) {
                    ctrl.setEnabled(enabled);
                }
            }

            for (Control ctrl : logonCtrls) {
                if (ctrl != null) {
                    ctrl.setEnabled(enabled);
                }
            }

            for (Control ctrl : debugCtrls) {
                if (ctrl != null) {
                    ctrl.setEnabled(enabled);
                }
            }
            handleCredentialCtrlEnablement();
        }
    }

    protected void handleCredentialCtrlEnablement() {
        boolean selected;
        boolean enabled = true;
        if (isEnableRemoteServerStartupCheckbox != null) {
            enabled = isEnableRemoteServerStartupCheckbox.getSelection() && remoteInfo.getBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, true);
        }

        if (isOSLogonBtn != null) {
            selected = isOSLogonBtn.getSelection();
            for (Control ctrl : osIdPasswdCtrls) {
                if (ctrl != null) {
                    ctrl.setEnabled(selected && enabled);
                }
            }
        }

        if (isSSHBtn != null) {
            selected = isSSHBtn.getSelection();
            if (sshComposite != null) {
                sshComposite.setEnablement(selected && enabled);
            }
        }
    }

    protected void setAllReadOnly() {
        boolean enabled = false;
        if (isEnableRemoteServerStartupCheckbox != null) {
            isEnableRemoteServerStartupCheckbox.setEnabled(enabled);
        }

        for (Control ctrl : installPathCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(enabled);
            }
        }

        for (Control ctrl : platformCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(enabled);
            }
        }

        for (Control ctrl : logonCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(enabled);
            }
        }

        for (Control ctrl : debugCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(enabled);
            }
        }

        for (Control ctrl : osIdPasswdCtrls) {
            if (ctrl != null) {
                ctrl.setEnabled(enabled);
            }
        }

        if (sshComposite != null) {
            sshComposite.setEnablement(enabled);
        }
    }

    @Override
    protected void execute(IUndoableOperation op) {
        for (CommandListener l : commandListeners) {
            l.handleCommand(op);
        }
    }

    public String[] getValidationErrors() {
        if (isEnableRemoteServerStartupCheckbox == null || !isEnableRemoteServerStartupCheckbox.getSelection())
            return new String[0];
        return validationErrors;
    }

    protected void addCommandListener(CommandListener c) {
        commandListeners.add(c);
    }

    public RemoteServerInfo getRemoteServerInfo() {
        return remoteInfo;
    }

    abstract class CommandListener {
        public abstract void handleCommand(IUndoableOperation op);
    }

    protected void setPlatform(int platform) {
        boolean disableOSLogon = false;
        switch (platform) {
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_OTHER:
                setButtons(isOtherBtn);
                break;
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS:
                setButtons(isWindowsBtn);
                if (isDockerServer) {
                    disableOSLogon = true;
                }
                break;
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX:
                setButtons(isLinuxBtn);
                break;
            case RemoteServerInfo.REMOTE_SERVER_STARTUP_MAC:
                setButtons(isMACBtn);
                break;
            default:
                if (isDockerServer) {
                    setButtons(isLinuxBtn);
                } else {
                    setButtons(isWindowsBtn);
                }
                break;
        }
        if (isSSHBtn != null && isOSLogonBtn != null) {
            if (disableOSLogon) {
                isSSHBtn.setSelection(true);
                isOSLogonBtn.setSelection(false);
                isOSLogonBtn.setEnabled(false);
            } else {
                isOSLogonBtn.setEnabled(true);
            }
            setLogonCtrls(isOSLogonBtn.getSelection());
        }
    }

    protected void setButtons(Button activeButton) {
        for (Button button : platformButtons) {
            button.setSelection(button == activeButton);
        }
    }
}