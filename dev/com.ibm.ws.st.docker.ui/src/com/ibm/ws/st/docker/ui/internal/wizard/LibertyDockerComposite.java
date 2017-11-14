/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.ui.internal.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.util.SocketUtil;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.w3c.dom.Document;

import com.ibm.ws.st.common.core.ext.internal.AbstractServerSetup;
import com.ibm.ws.st.common.core.ext.internal.ServerSetupFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine.MachineType;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility.MountProperty;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerServer;
import com.ibm.ws.st.docker.ui.internal.Activator;
import com.ibm.ws.st.docker.ui.internal.ContextIds;
import com.ibm.ws.st.docker.ui.internal.Messages;
import com.ibm.ws.st.docker.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.wizard.AbstractRemoteServerComposite;
import com.ibm.ws.st.ui.internal.wizard.WebSphereServerWizardCommonFragment;

/**
 * New server wizard composite for liberty running on local docker.
 */
@SuppressWarnings("restriction")
public class LibertyDockerComposite extends AbstractRemoteServerComposite {

    private static final char PASSWORD_CHAR = '\u25CF';
    private static final String SERVER_TYPE = "LibertyDocker";
    private static final String DEFAULT_PORT = "9443";

    protected TaskModel taskModel;
    protected Group connectionInfo;
    // Contains the names of unused Liberty containers, mapping them to their respective containers
    protected Map<String, BaseDockerContainer> containerNameMap;
    protected Combo containerCombo;
    protected Text userText, passwordText, portText;
    protected Button connectButton;
    protected Button refreshButton;
    protected StyledText remoteServerOutputPath;
    protected String selectedContainerName;
    protected BaseDockerContainer selectedContainer;
    protected String userName, userPassword;
    protected String portNum = DEFAULT_PORT;
    protected boolean isComplete = false;
    protected AbstractServerSetup serverSetup = null;
    protected boolean serverSetupUpdateNeeded = false;
    protected Map<String, String> serviceInfo;
    protected Button enableLooseConfigButton;
    protected boolean looseConfigEnabled = false;
    private String hostname = null;
    protected boolean isLocalHost = false;
    protected Composite fileSyncComposite = null;

    // Maps the names of containers being used by workspace servers to the name of the corresponding server.
    protected Map<String, String> containersInUse;
    protected Link containersInUseLink;

    protected LibertyDockerComposite(Composite parent, IWizardHandle wizard, TaskModel taskModel) {
        super(parent, wizard);

        this.taskModel = taskModel;

        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));

        if (taskModel != null) {
            IServerWorkingCopy serverWorkingCopy = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
            if (serverWorkingCopy != null) {
                hostname = serverWorkingCopy.getHost();
                isLocalHost = SocketUtil.isLocalhost(hostname);
            }
        }
        createControl();
    }

    @Override
    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        setLayoutData(data);

        connectionInfo = new Group(this, SWT.NONE);
        connectionInfo.setText(Messages.wizDockerServerConnectionInfo);
        // set layout for the connection info group
        int numCols = 5;
        layout = new GridLayout(numCols, false);
        layout.marginHeight = 11;
        layout.marginWidth = 9;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        connectionInfo.setLayout(layout);

        // set layout data for the connection info group
        data = new GridData(GridData.FILL_HORIZONTAL);
        connectionInfo.setLayoutData(data);

        // Docker container identification label
        StyledText text = new StyledText(connectionInfo, SWT.NONE);
        text.setText(Messages.wizDockerServerContainerInfo);
        text.setBackground(connectionInfo.getBackground());
        text.setEditable(false);
        text.setCaret(null);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        data.horizontalSpan = 5;
        text.setLayoutData(data);

        int indentSize = 3;

        // Docker container
        createDummyLabel(connectionInfo, indentSize);

        Label label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizDockerContainerName);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));

        containerCombo = new Combo(connectionInfo, SWT.READ_ONLY);
        data = new GridData(GridData.FILL, GridData.CENTER, true, false);
        data.horizontalSpan = 2;
        containerCombo.setLayoutData(data);
        containerNameMap = createContainerNameMap();
        Set<String> nameSet = containerNameMap.keySet();
        String[] containerNames = nameSet.toArray(new String[nameSet.size()]);
        Arrays.sort(containerNames);
        containerCombo.setItems(containerNames);

        containerCombo.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(SelectionEvent event) {
                int index = containerCombo.getSelectionIndex();
                String newContainerName = containerCombo.getItem(index);
                // Refresh only if the container has been changed
                if (selectedContainerName != null && !newContainerName.equals(selectedContainerName)) {
                    selectedContainerName = newContainerName;
                    selectedContainer = getDockerContainer(newContainerName);
                    serverSetupUpdateNeeded = true;
                    isComplete = false;
                    validate();
                }
            }
        });

        // Refresh button to update the wlp container list
        refreshButton = new Button(connectionInfo, SWT.NONE);
        refreshButton.setText(Messages.wizDockerRefresh);
        refreshButton.setToolTipText(Messages.wizDockerRefreshTooltip);
        refreshButton.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshContainers();
                validate();
            }
        });

        createDummyLabel(connectionInfo, indentSize);

        // Create a link which opens a dialog, listing any containers already running a workspace server
        // This link is only visible if there are any containers in use - this is updated by the Refresh button
        containersInUseLink = new Link(connectionInfo, SWT.NONE);
        containersInUseLink.setText(Messages.wizDockerViewInUseContainersLink);
        containersInUseLink.setToolTipText(Messages.wizDockerInUseContainersCannotCreateNewServer);

        containersInUseLink.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 4, 1));

        containersInUseLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // The link does not appear if containersInUse is empty
                new ContainersInUseDialog(getShell(), containersInUse).open();
            }
        });

        containersInUseLink.setVisible(!containersInUse.isEmpty());

        // Liberty server security credentials label
        text = new StyledText(connectionInfo, SWT.NONE);
        text.setText(Messages.wizDockerServerLibertySecurityInfo);
        text.setBackground(connectionInfo.getBackground());
        text.setEditable(false);
        text.setCaret(null);
        data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        data.horizontalSpan = 5;
        text.setLayoutData(data);

        // User name
        createDummyLabel(connectionInfo, indentSize);

        label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizDockerUserLabel);

        userText = new Text(connectionInfo, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        userText.setLayoutData(data);
        userText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                userName = userText.getText();
                isComplete = false;
                validate();
            }
        });

        createDummyLabel(connectionInfo, 0);

        // User password
        createDummyLabel(connectionInfo, indentSize);

        label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizDockerPasswordLabel);

        passwordText = new Text(connectionInfo, SWT.BORDER);
        passwordText.setText("");
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        passwordText.setLayoutData(data);
        passwordText.setEchoChar(PASSWORD_CHAR);
        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                userPassword = passwordText.getText();
                isComplete = false;
                validate();
            }
        });

        final Button showPassword = new Button(connectionInfo, SWT.CHECK);
        showPassword.setText(Messages.wizDockerPasswordShowButton);
        data = new GridData(GridData.END, GridData.CENTER, false, false);
        showPassword.setLayoutData(data);
        showPassword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (showPassword.getSelection())
                    passwordText.setEchoChar('\0');
                else
                    passwordText.setEchoChar(PASSWORD_CHAR);
            }
        });

        createDummyLabel(connectionInfo, 0);

        // Port number
        createDummyLabel(connectionInfo, indentSize);

        label = new Label(connectionInfo, SWT.NONE);
        label.setText(Messages.wizDockerSecurePort);

        portText = new Text(connectionInfo, SWT.BORDER);
        portText.setMessage(DEFAULT_PORT);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.horizontalSpan = 2;
        portText.setLayoutData(data);
        portText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                portNum = portText.getText();
                if (portNum == null || portNum.isEmpty()) {
                    portNum = DEFAULT_PORT;
                }
                isComplete = false;
                validate();
            }
        });

        if (isLocalHost) {
            createDummyLabel(connectionInfo, indentSize);

            enableLooseConfigButton = new Button(connectionInfo, SWT.CHECK);
            enableLooseConfigButton.setText(Messages.wizDockerLooseConfigButton);
            enableLooseConfigButton.setToolTipText(com.ibm.ws.st.ui.internal.Messages.editorGeneralLooseConfigMessage);
            data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
            data.horizontalSpan = 4;
            enableLooseConfigButton.setLayoutData(data);
            enableLooseConfigButton.setSelection(true);
            looseConfigEnabled = true;

            enableLooseConfigButton.addSelectionListener(new SelectionAdapter() {
                /** {@inheritDoc} */
                @Override
                public void widgetSelected(SelectionEvent e) {
                    looseConfigEnabled = enableLooseConfigButton.getSelection();
                    fileSyncComposite.setVisible(!looseConfigEnabled);
                }
            });
        }

        // connect to server button
        connectButton = new Button(connectionInfo, SWT.NONE);
        connectButton.setText(Messages.wizDockerConnect);
        connectButton.setToolTipText(Messages.wizDockerConnectTooltip);

        connectButton.addSelectionListener(new SelectionAdapter() {
            /** {@inheritDoc} */
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
//                    container.startSession();
                    handleConnect(selectedContainer);
                } catch (Exception e) {
                    Trace.logError("Failed to connect with Docker container: " + selectedContainer.getContainerName(), e);
                } finally {
//                    try {
//                        container.endSession();
//                    } catch (ConnectException e) {
//                        if (Trace.ENABLED) {
//                            Trace.trace(Trace.WARNING, "Failed to end session for container: " + container.getContainerName(), e);
//                        }
//                    }
                }
            }
        });
        // Add Context Sensitive Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getParent(), ContextIds.VERIFY_CONTAINER);

        createConfigControl(this);

        // Server directory label
        text = new StyledText(this, SWT.NONE);
        text.setText(Messages.wizDockerDirectoryLabel);
        text.setBackground(this.getBackground());
        text.setEditable(false);
        text.setCaret(null);

        // label that shows the remote configuration directory
        remoteServerOutputPath = new StyledText(this, SWT.NONE);
        remoteServerOutputPath.setText("");
        remoteServerOutputPath.setBackground(this.getBackground());
        remoteServerOutputPath.setEditable(false);
        remoteServerOutputPath.setCaret(null);

        label = new Label(this, SWT.NONE);

        fileSyncComposite = new Composite(this, SWT.NONE);
        layout = new GridLayout(numCols, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        fileSyncComposite.setLayout(layout);
        data = new GridData(GridData.FILL_HORIZONTAL);
        fileSyncComposite.setLayoutData(data);

        label = new Label(fileSyncComposite, SWT.WRAP);
        label.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
        data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
        label.setLayoutData(data);

        Text infoText = new Text(fileSyncComposite, SWT.WRAP | SWT.READ_ONLY | SWT.NO_TRIM);
        infoText.setBackground(fileSyncComposite.getBackground());
        infoText.setForeground(fileSyncComposite.getForeground());
        infoText.setText(Messages.infoOnlyServerConfigSynchronized);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.widthHint = 500;
        infoText.setLayoutData(data);

        fileSyncComposite.setVisible(!looseConfigEnabled);

        if (containerNames.length > 0) {
            selectedContainerName = containerNames[0];
            selectedContainer = getDockerContainer(selectedContainerName);
            containerCombo.setText(selectedContainerName);
            userText.setFocus();
        } else {
            containerCombo.setFocus();
        }

        Dialog.applyDialogFont(this);
    }

    protected void createDummyLabel(Composite composite, int length) {
        Label label = new Label(connectionInfo, SWT.NONE); // blank space
        if (length > 0) {
            char[] charArray = new char[length];
            Arrays.fill(charArray, ' ');
            label.setText(new String(charArray));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void init() {
        wizard.setTitle(Messages.wizDockerServerTitle);
        wizard.setDescription(Messages.wizDockerServerDescription);

        if (server == null)
            return;

        server.setDefaults(new NullProgressMonitor());
        server.setLooseConfigEnabled(false);
    }

    protected void initialValidate() {

        Map<String, String> info = getCommandServiceInfo();
        IPlatformHandler handler = null;
        try {
            handler = PlatformHandlerFactory.getPlatformHandler(info, PlatformType.COMMAND);
            handler.startSession();
        } catch (Exception e) {
            wizard.setMessage(Messages.wizDockerMachineConnectionError, IMessageProvider.ERROR);
            wizard.update();
            this.setEnabled(false);
            Trace.logError("Failed to start session with  machine: " + info.get(LibertyDockerUtil.HOSTNAME), e);
            return;
        } finally {
            try {
                if (handler != null) {
                    handler.endSession();
                }
            } catch (ConnectException e) {
                Trace.logError("Failed to end session with machine: " + info.get(LibertyDockerUtil.HOSTNAME), e);
            }
        }

        if (!this.isEnabled()) {
            this.setEnabled(true);
        }

        // valid connection exists, update containers list if necessary and validate accordingly
        if (containerNameMap == null) {
            refreshContainers();
        }
        if (containerNameMap.isEmpty()) {
            wizard.setMessage(Messages.wizDockerNoContainersFound, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
    }

    protected void clearContainers() {
        containerNameMap = null;
        containersInUse = null;
        containerCombo.removeAll();
    }

    /** {@inheritDoc} */
    @Override
    public void validate() {
        // Don't gather containers when validating, if they have not been gathered yet then skip validation
        if (containerNameMap == null) {
            isComplete = false;
            return;
        }

        isComplete = isComplete || false;
        if (containerNameMap.isEmpty()) {
            wizard.setMessage(Messages.wizDockerNoContainersFound, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        if (selectedContainer == null) {
            wizard.setMessage(Messages.wizDockerContainerNameNotSet, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        if (userName == null || userName.isEmpty()) {
            wizard.setMessage(Messages.wizDockerUserNotSet, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        if (userPassword == null || userPassword.isEmpty()) {
            wizard.setMessage(Messages.wizDockerPasswordNotSet, IMessageProvider.ERROR);
            wizard.update();
            return;
        }
        if (portNum == null || portNum.isEmpty()) {
            portNum = DEFAULT_PORT;
        } else {
            try {
                int port = Integer.parseInt(portNum);
                if (port < 1 || port > 65535) {
                    wizard.setMessage(Messages.wizDockerPortNotSet, IMessageProvider.ERROR);
                    wizard.update();
                    return;
                }
            } catch (Throwable t) {
                // Exception indicates user set an invalid string for port
                wizard.setMessage(Messages.wizDockerPortNotSet, IMessageProvider.ERROR);
                wizard.update();
                return;
            }
        }
        wizard.setMessage(null, IMessageProvider.NONE);
        connectButton.setEnabled(true);
        wizard.update();
    }

    protected void refreshContainers() {
        this.refreshContainers(null);
    }

    private void refreshContainers(String selectContainerName) {
        containerNameMap = createContainerNameMap();
        Set<String> nameSet = containerNameMap.keySet();
        String[] containerNames = nameSet.toArray(new String[nameSet.size()]);
        Arrays.sort(containerNames);
        containerCombo.setItems(containerNames);

        if (containerNames.length > 0) {
            if (selectContainerName != null) {
                containerCombo.setText(selectContainerName);
            } else {
                selectedContainerName = containerNames[0];
                selectedContainer = getDockerContainer(selectedContainerName);
                containerCombo.setText(selectedContainerName);
            }
            userText.setFocus();
        } else {
            containerCombo.setFocus();
        }

        // containersInUse is updated by the above call to createContainerNameMap
        containersInUseLink.setVisible(!containersInUse.isEmpty());
    }

    @SuppressWarnings("boxing")
    protected void handleConnect(BaseDockerContainer container) {
        validate(); // pre-connection validation to ensure required parameters are good
        if (wizard.getMessageType() != IMessageProvider.NONE)
            return;

        if (serverSetup == null || serverSetupUpdateNeeded) {
            serverSetup = getServerSetup(container);
            try {
                if (serverSetup.getServerXML() != null) {
                    serverSetupUpdateNeeded = false;
                }
            } catch (Exception e) {
                Trace.logError("Failed to get the server.xml from Docker container", e);
                return;
            }
        }

        if (serverSetup.getServerXML() != null) {

            ArrayList<Integer> validation = remoteSecurityValidation(serverSetup);
            switch (validation.get(0)) {
                case -1:
                    // Server.xml null?
                    wizard.setMessage(Messages.wizDockerMissingServerXML, IMessageProvider.ERROR);
                    return;
                case 0: // Everything is good
                    break;
                case 1: // No basicRegistry or no user defined under basicRegistry
                    MessageDialog dg = new MessageDialog(this.getShell(), Messages.wizDockerSecuDiaTitle, null, NLS.bind(Messages.wizDockerNoRegOrUser,
                                                                                                                         getUserId()), MessageDialog.QUESTION_WITH_CANCEL, new String[] {
                                                                                                                                                                                          Messages.wizDockerNoUserCreateButton,
                                                                                                                                                                                          Messages.wizDockerNoUserProceedButton,
                                                                                                                                                                                          IDialogConstants.CANCEL_LABEL }, 0);
                    switch (dg.open()) {
                        case 0:
                            //yes
                            remoteSecurityUpdate(serverSetup, 1);
                            break;
                        case 1:
                            //no
                            break;
                        case 2:
                            //cancel
                            return;
                    }
                    break;
                case 2: // No matching user under basicRegistry
                    wizard.setMessage(Messages.wizDockerUserMismatch, IMessageProvider.ERROR);
                    return;
                case 4: // Matching user but password mismatches
                    wizard.setMessage(Messages.wizDockerPWMismatch, IMessageProvider.ERROR);
                    return;
                case 8: // Credentials correct but user does not have administrator role
                    boolean addAdmin = MessageDialog.openQuestion(this.getShell(), Messages.wizDockerSecuDiaTitle, Messages.wizDockerNotAdmin);
                    if (addAdmin) {
                        remoteSecurityUpdate(serverSetup, 8);
                        break;
                    }
                    return;
            }
        } else {
            wizard.setMessage(Messages.wizDockerMissingServerXML, IMessageProvider.ERROR);
            return;
        }

        MultiStatus multiStatus = remoteConfigSetup(serverSetup);

        if (serverConfigDir != null)
            remoteServerOutputPath.setText(serverConfigDir);
        else
            remoteServerOutputPath.setText("");

        remoteServerOutputPath.getParent().layout(); //ensure the layout recalculates the size for this label

        if (multiStatus.isOK()) {
            isComplete = true;
            connectButton.setEnabled(false);
            setTreeInput();
        } else {
            isComplete = false;
            connectButton.setEnabled(true);
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

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        IPath workspacePath = getWorkspacePath(selectedContainer);
        boolean createNewContainer = false;

        LibertyDockerRunUtility.MountProperty existingVolumeStatus = LibertyDockerRunUtility.checkContainerForLooseConfigMountVolume(selectedContainer, workspacePath);

        // If necessary, ask the user if they want to create a new container before doing anything (in case they select cancel)
        if (looseConfigEnabled && needsNewContainerForLooseConfig(selectedContainer, workspacePath, existingVolumeStatus)) {
            final int[] answer = new int[1];
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    MessageDialog dg = new MessageDialog(getShell(), Messages.wizDockerSupportLooseConfigTitle, null, Messages.wizDockerContainerNotEnabledForLooseConfig, MessageDialog.QUESTION_WITH_CANCEL, new String[] { IDialogConstants.YES_LABEL,
                                                                                                                                                                                                                              IDialogConstants.NO_LABEL,
                                                                                                                                                                                                                              IDialogConstants.CANCEL_LABEL }, 0);
                    answer[0] = dg.open();
                }
            });
            if (answer[0] == 0) {
                createNewContainer = true;
            } else if (answer[0] == 1) {
                looseConfigEnabled = false;
            } else if (answer[0] == 2) {
                throw new CoreException(Status.CANCEL_STATUS);
            }
        }
        SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
        saveCredentials();
        UserDirectory userDir = null;
        // Determine if the container already has the /opt/ibm/wlp/usr mount.  If it already has one, we will use it and create the runtime project
        // folder as an external project (linked to that location in the filesystem).
        if (!existingVolumeStatus.equals(MountProperty.OTHER_USR_MOUNT)) {
            userDir = createUserDir(generateUserDirName(), subMonitor.newChild(createNewContainer ? 30 : 100));
        } else {
            // The container has a mount to another path or workspace that is not the same as this current eclipse workspace.
            // We need to save and track all the workspace volume mounts because they need to be added whenever we create a new container
            // for loose config.  Otherwise, we will lose track of all the workspaces, and the external apps will not work.
            addMandatoryVolumes();
            userDir = createExternalUserDir(null, subMonitor.newChild(createNewContainer ? 30 : 100));
        }
        LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        if (createNewContainer) {
            try {
                String newContainerName = LibertyDockerRunUtility.setupNewContainer(userDir, workspacePath, selectedContainer, getServiceInfo(selectedContainer), server,
                                                                                    existingVolumeStatus,
                                                                                    subMonitor.newChild(70));
                if (newContainerName != null && !newContainerName.isEmpty()) {
                    // We need to set the property on the server too.  It will be checked later to determine if loose config mode has changed.
                    serverExt.setCurrentRunStatus(newContainerName, ILaunchManager.RUN_MODE, false, true, server);
                } else {
                    // This will likely not reach here because we needed to create a new container for loose config, but somehow it failed, and exceptions
                    // should have been thrown to prevent the wizard from finishing.
                    server.setLooseConfigEnabled(false);
                    serverExt.setLooseConfigEnabled(false, server);
                }
            } catch (ConnectException e) {
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.wizDockerConnectExceptionDialogDetails, e);
                if (Trace.ENABLED) {
                    Trace.logError("Failed to create new container for supporting loose config.", e);
                }
                // throw to prevent completion of wizard
                throw new CoreException(status);
            } catch (IOException e) {
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.wizDockerIOExceptionDialogDetails, e);
                if (Trace.ENABLED) {
                    Trace.logError("Failed to create new container for supporting loose config.", e);
                }
                // throw to prevent completion of wizard
                throw new CoreException(status);
            } catch (Exception e) {
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.wizDockerExceptionDialogDetails, e);
                if (Trace.ENABLED) {
                    Trace.logError("Failed to create new container for supporting loose config.", e);
                }
                // throw to prevent completion of wizard
                throw new CoreException(status);
            }
        } else {
            // If a new container is not needed, then the container is a user container and it could be already enabled for loose config.
            server.setLooseConfigEnabled(looseConfigEnabled);
            serverExt.setLooseConfigEnabled(looseConfigEnabled, server);
        }
    }

    protected IPath getWorkspacePath(BaseDockerContainer container) {
        IPath workspacePath = null;
        if (server != null && server.getWebSphereRuntime() != null) {
            workspacePath = server.getWebSphereRuntime().getProject().getWorkspace().getRoot().getLocation();
        }
        if (workspacePath == null) { // Perhaps the project folder got deleted??  If so, try alternate means to get the workspace path
            workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        }
        // For Windows non-native, the mounted volume is /c/Users/<path>.  It is a 'logical' mapping of the
        // real local path c:/Users/<path>.   So, in this case, we need to convert it to the container path form because
        // this will be used to test against the existing volumes of the container to see if the container matches the
        // current workspace.  (ie. it is valid).
        // On Mac and Linux, no conversion is necessary.
        workspacePath = BaseDockerContainer.getLocalToContainerPath(workspacePath);

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Workspace path is " + workspacePath);
        }
        return workspacePath;
    }

    protected boolean needsNewContainerForLooseConfig(BaseDockerContainer container, IPath workspacePath, MountProperty existingVolumeStatus) {
        // Get the current usr mount volume of the running container
        try {
            String usrMount = container.getMountSourceForDestination(LibertyDockerRunUtility.DOCKER_LIBERTY_USR_PATH);
            IPath looseConfigPathMount = container.getMountDestinationForSource(workspacePath);
            // If the container already has mount volumes, and has the expected user folder and workspace volume mounts, then we can simply reuse the container.
            if (usrMount != null && looseConfigPathMount != null && !looseConfigPathMount.equals("") && existingVolumeStatus.equals(MountProperty.SAME_USR_MOUNT)) { // better validation?
                return false;
            }
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.logError("Failed to determine if the " + container.getContainerName() + " container is already set up for running applications directly from the workspace",
                               e);
            }
        }
        return true;
    }

    private String generateUserDirName() {
        String userDirId = selectedContainer.getContainerName();
        AbstractDockerMachine machine = selectedContainer.getDockerMachine();
        if (machine.isRealMachine()) {
            userDirId = NLS.bind(Messages.wizDockerContainerNameFormat, new String[] { userDirId, machine.getMachineName() });
        }
        if (!isLocalHost) {
            if (hostname != null && !hostname.isEmpty()) {
                userDirId = NLS.bind(Messages.dockerUserDirName, new String[] { userDirId, hostname });
            }
        }
        return RemoteUtils.generateRemoteUsrDirName(server.getWebSphereRuntime(), userDirId);
    }

    @Override
    protected void performCancel() {
        discardTemporaryFiles();
    }

    @Override
    protected String getHost() {
        return getHost(selectedContainer);
    }

    protected String getHost(BaseDockerContainer container) {
        String host = null;
        if (server != null && SocketUtil.isLocalhost(server.getServer().getHost())) {
            // This only applies to localhost.  For remote Docker the connection is
            // made directly to the host and not the docker machine.
            try {
                host = container.getHostMappedIP(portNum);
            } catch (Exception e) {
                String name = container != null ? container.getContainerName() : "unknown";
                Trace.logError("Could not get host mapped IP for the " + name + " container and port " + portNum, e);
            }
        }
        if (host == null && server != null) {
            host = server.getServer().getHost();
        }
        return host;
    }

    // Can only be called before the page is exited as the container name map
    // gets cleaned up on exit
    protected BaseDockerContainer getDockerContainer(String name) {
        if (name == null || containerNameMap == null) {
            return null;
        }
        return containerNameMap.get(name);
    }

    /** {@inheritDoc} */
    @Override
    protected String getUserId() {
        return userName;
    }

    /** {@inheritDoc} */
    @Override
    protected String getUserPassword() {
        return userPassword;
    }

    /** {@inheritDoc} */
    @Override
    protected String getPort() {
        String port = null;
        try {
            port = selectedContainer.getHostMappedPort(portNum);
        } catch (Exception e) {
            String name = selectedContainer != null ? selectedContainer.getContainerName() : "unknown";
            String traceError = "Could not get port mapping for the " + name + " container and port " + portNum;
            Trace.logError(traceError, e);

            // Display an error dialog to the user with a friendlier message
            String errMsg = NLS.bind(Messages.wizDockerMappedPortErrorMsg, portNum, name);
            MessageDialog.openError(getShell(), Messages.wizDockerConnectExceptionDialogTitle, errMsg);

            // Cancel the Connect operation
            throw new OperationCanceledException(traceError);
        }
        if (port == null) {
            port = portNum;
        }
        return port;
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
                treeViewer.setInput(getEmptyConfigMsg());
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

    private void saveCredentials() {
        server.setLooseConfigEnabled(looseConfigEnabled);
        server.setServerUserName(userName);
        server.setServerPassword(userPassword);
        server.setServerSecurePort(portNum);
        server.setServerType(SERVER_TYPE);
        server.setServiceInfo(serviceInfo);
        server.setStopTimeout(60);
        setAdditionalVolumes();
    }

    private void setAdditionalVolumes() {
        LibertyDockerServer dockerServer = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        if (dockerServer != null) {
            try {
                @SuppressWarnings("unchecked")
                List<IModule[]> moduleList = (List<IModule[]>) taskModel.getObject(TaskModel.TASK_MODULES);
                if (moduleList != null && !moduleList.isEmpty()) {
                    Set<IPath> newVolumes = new HashSet<IPath>();
                    Map<IPath, IPath> containerVolumes = selectedContainer.getMountedVolumeHash();
                    for (IModule[] module : moduleList) {
                        LibertyDockerRunUtility.addModuleLocations(server, containerVolumes, newVolumes, module[module.length - 1]);
                    }
                    dockerServer.setAdditionalVolumes(server, newVolumes);
                }
            } catch (Exception e) {
                Trace.logError("Failed to calculate any additional volumes needed for added modules to the " + server.getServer().getName() + " server.", e);
            }
        }
    }

    /*
     * Add mount volumes when switching back to loose config.
     * Since switching to non-loose config dismounts all mount volumes, we must add back any mandatory volumes
     *
     */
    private void addMandatoryVolumes() {
        LibertyDockerServer dockerServer = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        if (dockerServer != null) {
            try {
                Map<IPath, IPath> containerVolumes = selectedContainer.getMountedVolumeHash();
                Collection<IPath> volumes = containerVolumes.keySet();
                List<IPath> workspaceMountVolumes = new ArrayList<IPath>();
                for (IPath volume : volumes) {
                    // Interested only in the workspace mounts, not the usr mount
                    String aVolume = containerVolumes.get(volume).toString();
                    if (!aVolume.equals(LibertyDockerRunUtility.DOCKER_LIBERTY_USR_PATH) && aVolume.startsWith(LibertyDockerRunUtility.DOCKER_LIBERTY_STDEV_PATH)) {
                        workspaceMountVolumes.add(volume);
                    }
                }
                dockerServer.addMandatoryVolumes(server, workspaceMountVolumes);
            } catch (Exception e) {
                Trace.logError("Failed to calculate any additional volumes needed for added modules to the " + server.getServer().getName() + " server.", e);
            }
        }
    }

    protected List<BaseDockerContainer> getDockerContainers(List<AbstractDockerMachine> machines, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
        subMonitor.beginTask(null, 100);
        List<BaseDockerContainer> libertyContainers = new ArrayList<BaseDockerContainer>();
        if (machines.isEmpty()) {
            subMonitor.worked(100);
            return libertyContainers;
        }
        int machineWork = 100 / machines.size();
        for (AbstractDockerMachine machine : machines) {
            try {
                List<BaseDockerContainer> containers = machine.getContainers(false);
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "All containers for machine " + machine + ": " + containers);
                }
                if (containers.isEmpty()) {
                    subMonitor.worked(machineWork);
                    continue;
                }
                int containerWork = machineWork / containers.size();
                for (BaseDockerContainer container : containers) {
                    if (LibertyDockerUtil.isLibertyContainer(container))
                        libertyContainers.add(container);
                    if (monitor.isCanceled())
                        return libertyContainers;
                    subMonitor.worked(containerWork);
                }
            } catch (Exception e) {
                if (machine.isRealMachine()) {
                    Trace.logError("Could not get the list of liberty containers for machine: " + machine.getMachineName() + ".", e);
                } else {
                    Trace.logError("Could not get the list of liberty containers.", e);
                }
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "All liberty containers: " + libertyContainers);
        }
        return libertyContainers;
    }

    /**
     * Populates containerNameMap and busyContainerNameMap with Liberty Docker containers.
     *
     * @return containerNameMap
     */
    protected Map<String, BaseDockerContainer> createContainerNameMap() {
        containerNameMap = new HashMap<String, BaseDockerContainer>();
        containersInUse = new HashMap<String, String>();
        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor mon) throws InvocationTargetException {
                IPlatformHandler handler = null;
                try {
                    IProgressMonitor monitor = mon;
                    if (monitor == null)
                        monitor = new NullProgressMonitor();
                    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
                    subMonitor.beginTask(Messages.wizDockerServerGatheringContainers, 100);
                    Map<String, String> info = getCommandServiceInfo();
                    handler = PlatformHandlerFactory.getPlatformHandler(info, PlatformType.COMMAND);
//                        handler.startSession();
                    List<AbstractDockerMachine> machines = AbstractDockerMachine.getDockerMachines(handler);
                    if (mon.isCanceled())
                        return;
                    monitor.worked(30);
                    List<BaseDockerContainer> containers = getDockerContainers(machines, subMonitor.newChild(60));
                    if (mon.isCanceled())
                        return;
                    for (BaseDockerContainer container : containers) {

                        String containerName = container.getContainerName();
                        if (machines.size() > 1) {
                            String machineName = container.getDockerMachine().getMachineName();
                            if (machineName != null) {
                                containerName = NLS.bind(Messages.wizDockerContainerNameFormat,
                                                         new String[] { containerName, machineName });
                            }
                        }
                        // The Server currently using this container - null if none
                        WebSphereServer serverUsing = getServerUsingContainer(container);
                        if (serverUsing != null) {
                            // Container is in use - map the container name to the displayed server name
                            // to be displayed in the ContainersInUseDialog
                            containersInUse.put(containerName, serverUsing.getServer().getName());
                        } else {
                            // Add to the list of available containers (to be displayed in the drop-down)
                            containerNameMap.put(containerName, container);
                        }
                    }
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Container name map: " + containerNameMap);
                    }
                    if (mon.isCanceled())
                        return;
                    monitor.worked(10);
                    monitor.done();
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
//                        if (handler != null) {
//                            try {
//                                handler.endSession();
//                            } catch (ConnectException e) {
//                                if (Trace.ENABLED) {
//                                    Trace.trace(Trace.WARNING, "Failed to end session for platform handler.", e);
//                                }
//                            }
//                        }
                }
            }
        };
        try {
            wizard.run(true, true, runnable);
        } catch (Exception e) {
            Trace.logError("Exception thrown while trying to obtain Docker containers running WebSphere Liberty.", e);
        }
        return containerNameMap;
    }

    /**
     * Checks to see if the given container is already in use by a workspace server.
     *
     * @param container
     * @return The workspace server running in the container,
     *         or <i>null</i> if no workspace server is using the container.
     */
    protected WebSphereServer getServerUsingContainer(BaseDockerContainer container) {
        for (WebSphereServer server : WebSphereUtil.getWebSphereServers()) {
            if (SERVER_TYPE.equals(server.getServerType())) {
                String serverHost = server.getServer().getHost();
                if ((hostname != null && hostname.equals(serverHost)) || (hostname == null && serverHost == null)) {
                    Map<String, String> serviceInfo = server.getServiceInfo();
                    String containerName = serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_CONTAINER);
                    String machineName = serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_MACHINE);
                    String machineType = serviceInfo.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_MACHINE_TYPE);
                    if (containerMatch(container, containerName, machineName, machineType)) {
                        return server;
                    }

                    // Check the current working container too
                    LibertyDockerServer dockerServer = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
                    if (!dockerServer.isUserContainer(server)) {
                        containerName = dockerServer.getContainerName(server);
                        if (containerMatch(container, containerName, machineName, machineType)) {
                            return server;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected boolean containerMatch(BaseDockerContainer container, String containerName, String machineName, String machineType) {
        if (container.getContainerName().equals(containerName)) {
            String machineName2 = container.getDockerMachine().getMachineName();
            if ((machineName == null && machineName2 == null) || (machineName != null && machineName.equals(machineName2))) {
                MachineType type1 = AbstractDockerMachine.getMachineType(machineType, machineName);
                MachineType type2 = container.getDockerMachine().getMachineType();
                if (type1 == type2) {
                    return true;
                }
            }
        }
        return false;
    }

    protected AbstractServerSetup getServerSetup(BaseDockerContainer container) {
        serviceInfo = getServiceInfo(container);
        try {
            AbstractServerSetup serverSetup = ServerSetupFactory.getServerSetup("LibertyDockerLocal", serviceInfo,
                                                                                server.getWebSphereRuntime().getRuntime());
            return serverSetup;
        } catch (Exception e) {
            Trace.logError("Failed to create LibertyDockerLocal server setup.", e);
        }
        return null;
    }

    public void clearMessage() {
        wizard.setMessage(null, IMessageProvider.NONE);
    }

    protected Map<String, String> getServiceInfo(BaseDockerContainer container) {
        Map<String, String> serviceInfo = LibertyDockerUtil.getServiceInfo(container, server, serverWC.getHost(), getPort());
        serviceInfo.putAll(getCommandServiceInfo());
        return serviceInfo;
    }

    protected Map<String, String> getCommandServiceInfo() {
        Map<String, String> serviceInfo = new HashMap<String, String>();
        if (taskModel != null) {
            IServerWorkingCopy server = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
            serviceInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.HOSTNAME, server.getHost());
            Map<String, String> info = getTaskModelServiceInfo();
            serviceInfo.putAll(info);
        }
        return serviceInfo;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> getTaskModelServiceInfo() {
        Map<String, String> serviceInfo = null;
        if (taskModel != null) {
            serviceInfo = (Map<String, String>) taskModel.getObject(WebSphereServerWizardCommonFragment.SERVICE_INFO);
        }
        if (serviceInfo == null) {
            serviceInfo = Collections.emptyMap();
        }
        return serviceInfo;
    }

    /*
     * Create the external runtime project folder for containers with existing usr mount volumes to paths external to the current workspace
     *
     * This is 'customized' code based on com.ibm.ws.st.ui.internal.wizard.AbstractRemoteServerComposite#createUserDir
     *
     */
    private UserDirectory createExternalUserDir(String userDirName, IProgressMonitor monitor) throws CoreException {
        UserDirectory userDir = null;

        server.setServerName(serverName);
        WebSphereRuntime wsRuntime = server.getWebSphereRuntime();
        String usrMount = null;

        // The project in the other workspace was created to be unique (by calling generateUserDirName)
        // and it may have the (1) or (2)... strings append to it.
        // For example:
        //    a)   /workspaceA/Liberty Server (wlp)
        // or b)   /workspaceA/Liberty Server (wlp) (1)
        // Try to use the same name because the local project name will match the 'external' project name.
        // Typically, projects that link to external content (from Import Projects wizard) will have the same name as that of the target.
        // This can be seen by the Properties page of the project.
        // Also, there can't be overlap of the external project.
        try {
            // This contains the project name from the other workspace
            usrMount = selectedContainer.getMountSourceForDestination(LibertyDockerRunUtility.DOCKER_LIBERTY_USR_PATH);
        } catch (Exception e) {
            Trace.logError("Failed to get the user path of the container ", e);
            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.wizDockerExceptionDialogDetails, e);
            throw new CoreException(status);
        }
        if (usrMount == null) {
            Trace.logError("Expecting a non-null usr mount volume, otherwise, the container will be created the usual way.", null);
            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.wizDockerExceptionDialogDetails, null);
            throw new CoreException(status);
        }
        // Parse this string to get the project name.
        // If there is an existing project with the same name already we must throw an error because we can't have two
        // projects linked to the same external project.  Overlaps are not allowed.
        String projectName = usrMount.substring(usrMount.lastIndexOf("/") + 1);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        if (project.exists()) {
            Trace.logError("An existing project link to the target external project already exists.", null);
            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.wizDockerExistingProjectOverlaps, projectName), null);
            throw new CoreException(status); // prevent wizard completion
        }

        usrMount = BaseDockerContainer.getContainerToLocalPath(new Path(usrMount)).toString();
        IProject externalUsrProject = WebSphereUtil.createUserProject(projectName, new Path(usrMount), monitor);
        IPath outputPath = externalUsrProject.getLocation().append(com.ibm.ws.st.core.internal.Constants.SERVERS_FOLDER);
        userDir = new UserDirectory(wsRuntime, new Path(usrMount), externalUsrProject, outputPath, new Path(remoteUserPath));

        IRuntimeWorkingCopy runtimeWorkingCopy = wsRuntime.getRuntime().createWorkingCopy();
        WebSphereRuntime wRuntimeWorkingCopy = (WebSphereRuntime) runtimeWorkingCopy.loadAdapter(WebSphereRuntime.class, null);

        //
        // The following was also done in the normal createUserDir scenario.  So, add it here too.
        //

        // If the user directory doesn't exist we should add it
        boolean found = false;
        for (UserDirectory usr : wRuntimeWorkingCopy.getUserDirectories()) {
            if (usr.getPath().equals(userDir.getPath())) {
                found = true;
                break;
            }
        }
        if (!found) {
            wRuntimeWorkingCopy.addUserDirectory(userDir);
        }
        try {
            runtimeWorkingCopy.save(true, null);
        } catch (CoreException ce) {
            Trace.logError(ce.getMessage(), ce);
        }
        wsRuntime.updateServerCache(true);

        /*
         * Should get the WebSphereRuntime after RemoteUtils.createUserDir since the runtime will change when it is saved.
         * If the runtime is retrieved before the user directory is created then the server cache won't be updated correctly.
         */
        wsRuntime = server.getWebSphereRuntime();

        server.setUserDir(userDir);
        server.setLooseConfigEnabled(true);

        return userDir;
    }
}
