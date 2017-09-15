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

//import java.io.File;

//import org.eclipse.core.runtime.IPath;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.ContextIds;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.SWTUtil;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Wizard page to set the server name.
 */
@SuppressWarnings("restriction")
public class WebSphereServerComposite extends AbstractWebSphereServerComposite {
    protected Combo serverName;
    protected Label description;
    protected boolean updating;

    protected Map<String, WebSphereServerInfo> serverInfoMap;
    protected IPath lastRuntimeLocation = null;

    /**
     * WebSphereServerComposite constructor comment.
     *
     * @param parent the parent composite
     * @param wizard the wizard handle
     */
    protected WebSphereServerComposite(Composite parent, IWizardHandle wizard) {
        super(parent, wizard);

        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));

        createControl();
    }

    @Override
    protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.horizontalSpacing = SWTUtil.convertHorizontalDLUsToPixels(this, 4);
        layout.verticalSpacing = SWTUtil.convertVerticalDLUsToPixels(this, 4);
        setLayout(layout);
        setLayoutData(new GridData(GridData.FILL_BOTH));
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ContextIds.RUNTIME_COMPOSITE);

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.server);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        serverName = new Combo(this, SWT.READ_ONLY);
        serverName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        serverName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (updating)
                    return;
                if (server != null) {
                    WebSphereServerInfo serverInfo = serverInfoMap.get(serverName.getText());
                    if (serverInfo != null) {
                        server.setServerName(serverInfo.getServerName());
                        server.setUserDir(serverInfo.getUserDirectory());
                    }
                }

                setTreeInput();
                validate();
            }
        });

        Button createNew = SWTUtil.createButton(this, Messages.create);
        createNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TaskModel taskModel = new TaskModel();
                taskModel.putObject(TaskModel.TASK_RUNTIME, serverWC.getRuntime());
                taskModel.putObject(WebSphereRuntime.PROP_USER_DIRECTORY, userDirectory);
                TaskWizard wizard = new TaskWizard(Messages.wizServerNameTitle, new NewServerNameWizardFragment(), taskModel);
                String newServerName = null;
                String newServerNameWithUsrDir = null;

                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                if (dialog.open() == Window.OK) {
                    newServerName = (String) taskModel.getObject(WebSphereServer.PROP_SERVER_NAME);
                    UserDirectory userDir = (UserDirectory) taskModel.getObject(WebSphereRuntime.PROP_USER_DIRECTORY);
                    server.setServerName(newServerName);
                    server.setUserDir(userDir);
                    newServerNameWithUsrDir = NLS.bind(Messages.wizServerNameFormat, new String[] { newServerName, userDir.getUniqueId() });
                }
                WebSphereRuntime runtime = server.getWebSphereRuntime();
                if (runtime != null) {
                    updating = true;
                    String[] serverNames = getServerNames();
                    serverName.setItems(serverNames);
                    serverName.setEnabled(true);
                    updating = false;
                    // set the serverName to the newly created server
                    // server with same name can exist on different user directories. In that case userDir id is appended to serverName
                    if (newServerName != null && serverInfoMap.get(newServerName) != null) {
                        serverName.setText(newServerName);
                    } else if (newServerNameWithUsrDir != null && serverInfoMap.get(newServerNameWithUsrDir) != null) {
                        serverName.setText(newServerNameWithUsrDir);
                    } else
                        initServerName();

                }

                monitoredSetTreeInput();
                validate();
            }
        });

        label = new Label(this, SWT.NONE);
        label.setText(Messages.wizServerDescriptionLabel);
        GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
        data.verticalIndent = 3;
        label.setLayoutData(data);

        description = new Label(this, SWT.WRAP);
        description.setText("");
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.verticalIndent = 3;
        data.horizontalSpan = 2;
        data.verticalSpan = 2;
        description.setLayoutData(data);

        label = new Label(this, SWT.NONE);
        label.setText("");
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        createConfigControl(this);

        init();
        validate();

        Dialog.applyDialogFont(this);

        serverName.forceFocus();
    }

    protected void setTreeInput() {
        if (serverWC == null)
            return;

        try {
            ConfigurationFile configFile = server.getConfiguration();
            String desc = configFile == null ? "" : configFile.getServerDescription();
            Document document = configFile == null ? null : configFile.getDomDocument();
            setTreeInput(desc, document);
        } catch (Throwable t) {
            Trace.logError("Error loading config tree", t);
        }
    }

    protected void setTreeInput(String desc, Document document) {
        try {
            if (document == null) {
                description.setText(desc);
                treeViewer.setInput(Messages.configNone);
                return;
            }
            // this may cause the generation of the metadata so figure out if we need to clean up
            final WebSphereRuntime wrt = (server == null) ? null : server.getWebSphereRuntime();
            final boolean metadataDirExistsBefore = (wrt == null) ? false : wrt.metadataDirectoryExists();

            description.setText(desc);
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

    protected void monitoredSetTreeInput() {
        if (serverWC == null)
            return;

        try {
            wizard.run(true, true, new IRunnableWithProgress() {
                protected static final int WAIT = 20;

                protected boolean complete = false;

                protected synchronized void finished() {
                    complete = true;
                    notifyAll();
                }

                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.wizServerRefreshing, 10);
                    try {
                        // Execute what we can outside the UI thread so that the
                        // monitor can be updated
                        monitor.worked(1);
                        final ConfigurationFile configFile = server.getConfiguration();
                        if (monitor.isCanceled())
                            throw new CoreException(Status.CANCEL_STATUS);
                        monitor.worked(2);
                        final String desc = configFile == null ? "" : configFile.getServerDescription();
                        if (monitor.isCanceled())
                            throw new CoreException(Status.CANCEL_STATUS);
                        monitor.worked(1);
                        final Document document = configFile == null ? null : configFile.getDomDocument();
                        if (monitor.isCanceled())
                            throw new CoreException(Status.CANCEL_STATUS);
                        monitor.worked(3);

                        // Do the UI part
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                setTreeInput(desc, document);
                                finished();
                            }
                        });

                        // Wait for the UI part to finish
                        int count = 0;
                        while (true) {
                            synchronized (this) {
                                try {
                                    wait(1000);
                                } catch (Exception e) {
                                    // Do nothing
                                }
                                if (complete) {
                                    monitor.worked(3);
                                    break;
                                }
                            }
                            if (monitor.isCanceled())
                                throw new CoreException(Status.CANCEL_STATUS);
                            if (++count > WAIT) {
                                // Shouldn't take this long
                                Trace.logError("Async exec for refreshing the server configuration in the new server wizard timed out for server: "
                                               + server.getServerName(), null);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        if (monitor.isCanceled()) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.INFO,
                                            "Monitor for refreshing the configuration in the new server wizard was cancelled for server: " + server.getServerName());
                        } else {
                            Trace.logError("Error refreshing the configuration in the new server wizard for server: " + server.getServerName(), e);
                        }
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (Exception e) {
            Trace.logError("Error refreshing the configuration in the new server wizard for server: " + server.getServerName(), e);
        }
    }

    @Override
    protected void init() {
        wizard.setTitle(Messages.wizServerTitle);
        wizard.setDescription(Messages.wizServerDescription);

        if (serverName == null || server == null)
            return;

        updating = true;
        server.setDefaults(new NullProgressMonitor());
        updating = false;

        WebSphereRuntime runtime = server.getWebSphereRuntime();
        if (runtime != null) {
            String[] serverNames = getServerNames();
            if (serverNames == null || serverNames.length == 0) {
                serverName.setEnabled(false);
                // remove the current contents of the combo box
                serverName.removeAll();
            } else {
                serverName.setEnabled(true);
                serverName.setItems(serverNames);
                initServerName();
            }
        } else {
            serverName.setItems(new String[] { "error" });
        }

        // may cause generation of metadata
        setTreeInput();

    }

    protected String[] getServerNames() {
        WebSphereRuntime runtime = server.getWebSphereRuntime();
        if (runtime == null) {
            return new String[0];
        }
        runtime.updateServerCache(true);
        List<WebSphereServerInfo> serverInfos = runtime.getWebSphereServerInfos();
        if (serverInfos == null || serverInfos.size() == 0) {
            return new String[0];
        }
        serverInfoMap = new HashMap<String, WebSphereServerInfo>(serverInfos.size());
        for (WebSphereServerInfo serverInfo : serverInfos) {
            String name = serverInfo.getServerName();
            for (WebSphereServerInfo info : serverInfos) {
                if (info.equals(serverInfo)) {
                    continue;
                }
                if (info.getServerName().equals(name)) {
                    name = NLS.bind(Messages.wizServerNameFormat, new String[] { name, serverInfo.getUserDirectory().getUniqueId() });
                    break;
                }
            }
            serverInfoMap.put(name, serverInfo);
        }
        Set<String> serverNames = serverInfoMap.keySet();
        String[] names = serverNames.toArray(new String[serverNames.size()]);
        Arrays.sort(names, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        return names;
    }

    /**
     * getServerName returns server names sorted in alphabetical order. Set serverName to be First server not in use
     */
    protected void initServerName() {
        // Set the server name in the combo to the current server name
        String[] serverNames = getServerNames();
        for (String name : serverNames) {
            if (name != null && !isServerInUse(name)) {
                serverName.setText(name);
                return;
            }

        }
    }

    @Override
    public void validate() {
        IStatus status = validate(server);

        if (status == null || status.isOK())
            wizard.setMessage(null, IMessageProvider.NONE);
        else if (status.getSeverity() == IStatus.WARNING)
            wizard.setMessage(status.getMessage(), IMessageProvider.WARNING);
        else
            wizard.setMessage(status.getMessage(), IMessageProvider.ERROR);
        wizard.update();
    }

    protected static IStatus validate(WebSphereServer server) {
        if (server == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "");

        if (server.getServerName() == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorNoServers);

        IStatus status = server.validate();
        if (status == null || status.isOK()) {
            // check for server name already in use
            String serverName = server.getServerName();
            UserDirectory userDir = server.getUserDirectory();
            if (serverName != null && serverName.trim().length() > 0) {
                WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
                for (WebSphereServer serv : servers) {
                    UserDirectory dir = serv.getUserDirectory();
                    if (serverName.equals(serv.getServerName()) && ((userDir == null && dir == null) || (userDir != null && userDir.equals(dir))) &&
                        server.getServer().getRuntime().equals(serv.getServer().getRuntime())) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.warningServerAlreadyDefined, serverName, serv.getServer().getName()));
                    }
                }
            }
        }
        return status;
    }

    private Boolean isServerInUse(String serverName) {
        WebSphereServerInfo server = serverInfoMap.get(serverName);
        UserDirectory userDir = server.getUserDirectory();
        if (serverName != null && serverName.trim().length() > 0) {
            WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
            for (WebSphereServer serv : servers) {
                UserDirectory dir = serv.getUserDirectory();
                if (server.getServerName().equals(serv.getServerName()) && ((userDir == null && dir == null) || (userDir != null && userDir.equals(dir))) &&
                    server.getWebSphereRuntime().getRuntime().equals(serv.getServer().getRuntime())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        removeGeneratedMetaData(server.getWebSphereRuntime().getRuntime());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isComplete() {
        if (server.getServer() == null)
            return false;

        WebSphereServer wsServer = server;
        if (wsServer == null)
            return false;

        // Reset the runtime server info whenever the user changes the runtime location (WASRTC 124142)
        WebSphereRuntime wr = wsServer.getWebSphereRuntime();
        if (wr != null) {
            if (lastRuntimeLocation == null) {
                // First time we've seen this runtimeValue.
                lastRuntimeLocation = wr.getRuntimeLocation();

            } else if (wr.getRuntimeLocation() != null
                       && !lastRuntimeLocation.equals(wr.getRuntimeLocation())) {
                // The value has changed, so reset the server info; it will be regenerated below
                wr.resetRuntimeServerInfo();
                lastRuntimeLocation = wr.getRuntimeLocation();
            }
        }

        // Set WebSphere server defaults; this call will cause a regeneration of the WebSphereRuntime server information
        // and user directories if needed.
        wsServer.setDefaults(null);

        IStatus status = WebSphereServerComposite.validate(wsServer);
        return (status == null || status.isOK());
    }
}
