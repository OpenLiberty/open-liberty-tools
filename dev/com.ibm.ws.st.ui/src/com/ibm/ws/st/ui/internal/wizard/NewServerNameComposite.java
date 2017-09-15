/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContextIds;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Wizard page to create a new Liberty server.
 */
public class NewServerNameComposite extends Composite {
    protected IRuntime runtime;
    protected WebSphereRuntime websphereRuntime;

    protected IWizardHandle wizard;

    protected Combo userDir;
    protected Text serverNameText;

    protected String[] serverNames;
    protected String createServerName = null;
    protected List<UserDirectory> userDirs;
    protected UserDirectory selectedUserDir;
    protected boolean fillDefaultServerName;

    protected Combo template;
    protected String[] serverTemplates;
    protected String selectedTemplate;
    protected String archivePath;

    private static HashMap<String, String> templateCache = new HashMap<String, String>();

    /**
     * NewServerNameComposite constructor comment.
     *
     * @param parent the parent composite
     * @param wizard the wizard handle
     */
    protected NewServerNameComposite(Composite parent, IWizardHandle wizard, boolean fillDefaultServerName) {
        super(parent, SWT.NONE);
        this.wizard = wizard;
        this.fillDefaultServerName = fillDefaultServerName;

        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));

        createControl();
    }

    protected void setRuntime(IRuntime newRuntime, UserDirectory userDir, String archivePath) {
        if (newRuntime == null) {
            runtime = null;
            websphereRuntime = null;
            selectedUserDir = null;
            this.archivePath = null;
        } else {
            runtime = newRuntime;
            websphereRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            selectedUserDir = userDir;
            this.archivePath = archivePath;
        }

        init();
        validate();
    }

    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        setLayout(layout);
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ContextIds.RUNTIME_COMPOSITE);

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.userDirectory);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        userDir = new Combo(this, SWT.READ_ONLY);
        userDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        userDir.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // No op for the dummy default user directory
                if (!userDirs.isEmpty()) {
                    selectedUserDir = userDirs.get(userDir.getSelectionIndex());
                    validate();
                }
            }
        });

        label = new Label(this, SWT.NONE);
        label.setText(Messages.serverName);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        serverNameText = new Text(this, SWT.BORDER);
        serverNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        serverNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                createServerName = serverNameText.getText();
                validate();
            }
        });

        label = new Label(this, SWT.NONE);
        label.setText(Messages.template);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        template = new Combo(this, SWT.READ_ONLY);
        template.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        template.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                selectedTemplate = template.getItem(template.getSelectionIndex());
            }
        });

        init();
        validate();

        Dialog.applyDialogFont(this);

        serverNameText.forceFocus();
    }

    protected void init() {
        wizard.setTitle(Messages.wizServerNameTitle);
        wizard.setDescription(Messages.wizServerNameDescription);

        if (serverNameText == null || runtime == null)
            return;

        if (websphereRuntime != null) {
            List<String> userPathStr = new ArrayList<String>();

            userDirs = websphereRuntime.getUserDirectories();
            if (userDirs.isEmpty()) {
                selectedUserDir = null;
                // We have not yet created the runtime, so dummy in the
                // default user directory
                userDir.setItems(new String[] { websphereRuntime.getRuntime().getName() });
                userDir.select(0);
            } else {
                int userDirIndex = 0;
                if (selectedUserDir == null) {
                    selectedUserDir = userDirs.get(0);
                } else {
                    for (int i = 0; i < userDirs.size(); ++i) {
                        if (userDirs.get(i).equals(selectedUserDir)) {
                            userDirIndex = i;
                        }
                    }
                }

                for (UserDirectory ud : userDirs) {
                    if (ud.getProject() != null) {
                        userPathStr.add(ud.getProject().getName());
                    } else {
                        userPathStr.add(ud.getPath().toOSString());
                    }
                }

                userDir.setEnabled(true);
                userDir.setItems(userPathStr.toArray(new String[userPathStr.size()]));
                userDir.select(userDirIndex);
            }
            serverNames = websphereRuntime.getServerNames();

            if (fillDefaultServerName && createServerName == null) {
                String defaultName = "defaultServer";
                if (selectedUserDir != null) {
                    IPath serverPath = selectedUserDir.getServersPath();
                    int i = 2;
                    while (serverPath.append(defaultName).toFile().exists()) {
                        defaultName = "defaultServer_" + i;
                        i++;
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "Calculating default server name: " + defaultName);
                    }
                }
                serverNameText.setText(defaultName);
                createServerName = defaultName;
            }

            serverTemplates = getServerTemplates();
            template.setItems(serverTemplates);

            if (serverTemplates.length > 0) {
                int index = -1;
                if (NewServerNameComposite.templateCache.containsKey(websphereRuntime.getRuntime().getId()))
                    index = template.indexOf(NewServerNameComposite.templateCache.get(websphereRuntime.getRuntime().getId()));
                else
                    index = template.indexOf(Constants.TEMPLATES_DEFAULT_NAME);

                if (index == -1)
                    index = 0;

                template.select(index);
                selectedTemplate = template.getItem(index);
            }

            if (serverTemplates.length <= 1) {
                template.setEnabled(false);
            }

        }
    }

    String[] getServerTemplates() {
        String[] templates = websphereRuntime.getServerTemplates();
        if ((templates != null && templates.length > 0) || archivePath == null)
            return templates;

        File archiveFile = new File(archivePath);
        if (!archiveFile.exists())
            return templates;

        ZipFile jar = null;
        try {
            jar = new ZipFile(archiveFile);
            Enumeration<? extends ZipEntry> enu = jar.entries();
            List<String> templateList = new ArrayList<String>();
            while (enu.hasMoreElements()) {
                ZipEntry entry = enu.nextElement();
                String name = entry.getName().toLowerCase();
                if (!name.contains("templates/servers/") || !name.endsWith("server.xml"))
                    continue;

                int index1 = name.lastIndexOf('/');
                name = name.substring(0, index1);
                if (!name.endsWith("templates/servers")) {
                    int index2 = name.lastIndexOf('/');
                    templateList.add(entry.getName().substring(index2 + 1, index1));
                }
            }
            return templateList.isEmpty() ? new String[0] : templateList.toArray(new String[templateList.size()]);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Failed to get server templates from " + archivePath, e);
            return new String[0];
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Trouble closing zip file", e);
                }
            }
        }
    }

    protected void validate() {
        if (runtime == null) {
            wizard.setMessage("", IMessageProvider.ERROR);
            return;
        }

        // ignore if we are creating a new runtime in a new directory, since
        // we have not yet created the runtime content
        if (userDirs.isEmpty()) {
            File f = runtime.getLocation().toFile();
            boolean isExistingFolder = f.exists();
            if (isExistingFolder) {
                String[] s = f.list();
                isExistingFolder = s != null && s.length > 0;
            }

            if (isExistingFolder) {
                wizard.setMessage(NLS.bind(Messages.infoNoUsrDirs, websphereRuntime.getDefaultUserDirPath().toOSString()),
                                  IMessageProvider.INFORMATION);
                return;
            }
        }

        String serverName = serverNameText.getText();
        IStatus status = WebSphereServer.validateServerName(serverName);
        if (status == null || status.isOK()) {
            // check for empty name
            if (serverName == null || serverName.trim().isEmpty()) {
                wizard.setMessage("", IMessageProvider.ERROR);
                return;
            }

            // check to see if a variation of the server name
            // (different lower/upper case) exists in the selected
            // user directory or the default user directory if
            // selected is null
            IPath serverPath = null;
            if (selectedUserDir != null) {
                serverPath = selectedUserDir.getServersPath();
            } else if (websphereRuntime != null) {
                serverPath = websphereRuntime.getDefaultUserDirPath().append(Constants.SERVERS_FOLDER);
            }
            if (serverPath != null) {
                if (serverPath.append(serverName).toFile().exists()) {
                    wizard.setMessage(Messages.errorServerNameExists, IMessageProvider.ERROR);
                    return;
                }
            }
        }

        if (status == null || status.isOK())
            wizard.setMessage(null, IMessageProvider.NONE);
        else if (status.getSeverity() == IStatus.WARNING)
            wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
        else
            wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
        wizard.update();
    }

    public String getServerName() {
        return createServerName;
    }

    public UserDirectory getUserDir() {
        return selectedUserDir;
    }

    protected void createServer(IProgressMonitor monitor) throws CoreException {
        int count = 100;
        monitor.beginTask(NLS.bind(com.ibm.ws.st.core.internal.Messages.taskServerCreate, createServerName), count);
        if (selectedUserDir == null) {
            // The runtime might not have been created when we first checked,
            // so re-check the user directories and select the default one.
            List<UserDirectory> userDirList = websphereRuntime.getUserDirectories();
            if (!userDirList.isEmpty()) {
                for (UserDirectory ud : userDirList) {
                    if (websphereRuntime.getDefaultUserDirPath().equals(ud.getPath())) {
                        selectedUserDir = ud;
                        break;
                    }
                }
            }

            // No default user directory in the runtime, so we create it
            if (selectedUserDir == null) {
                SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 40);
                count = 60;
                selectedUserDir = websphereRuntime.createDefaultUserDirectory(subMonitor);
                if (selectedUserDir == null) {
                    final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    MessageDialog.openError(shell, Messages.title, NLS.bind(Messages.failedCreateDefaultUserDir, websphereRuntime.getDefaultUserDirPath().toOSString()));
                    monitor.done();
                    return;
                }
            }
        }
        SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, count - 10);
        String tplToCache = selectedTemplate != null ? selectedTemplate : Constants.TEMPLATES_DEFAULT_NAME;
        templateCache.put(websphereRuntime.getRuntime().getId(), tplToCache);
        websphereRuntime.createServer(createServerName, selectedTemplate, selectedUserDir.getPath(), subMonitor);
        if (selectedUserDir.getProject() != null) {
            subMonitor = new SubProgressMonitor(monitor, 10);
            selectedUserDir.getProject().refreshLocal(IResource.DEPTH_INFINITE, subMonitor);
        }
        monitor.done();
    }

    protected boolean isValid() {
        // if composite is disposed, we return false
        return (isDisposed()) ? false : wizard.getMessageType() != IMessageProvider.ERROR;
    }
}
