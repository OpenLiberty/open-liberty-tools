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
package com.ibm.ws.st.ui.internal.actions;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.RuntimeExplorer;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.wizard.NewServerNameWizardFragment;

@SuppressWarnings("restriction")
public class NewWebSphereServerAction extends SelectionProviderAction {
    private final Shell shell;
    private IRuntime runtime;
    private UserDirectory userDirectory;

    public NewWebSphereServerAction(ISelectionProvider selectionProvider, Shell shell) {
        super(selectionProvider, Messages.actionNewWebSphereServer);
        this.shell = shell;
        setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_SERVER));
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IRuntime) {
                runtime = (IRuntime) obj;
                userDirectory = null; // we use the default one
            } else if (obj instanceof UserDirectory) {
                userDirectory = (UserDirectory) obj;
                runtime = userDirectory.getWebSphereRuntime().getRuntime();
            } else if (obj instanceof RuntimeExplorer.Node) {
                RuntimeExplorer.Node node = (RuntimeExplorer.Node) obj;
                runtime = node.getRuntime();
                userDirectory = node.getUserDirectory();
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(true);
    }

    @Override
    public void run() {
        if (runtime == null) {
            return;
        }

        try {
            TaskModel taskModel = new TaskModel();
            taskModel.putObject(TaskModel.TASK_RUNTIME, runtime);
            taskModel.putObject(WebSphereRuntime.PROP_USER_DIRECTORY, userDirectory);
            TaskWizard wizard = new TaskWizard(Messages.wizServerNameTitle, new NewServerNameWizardFragment(), taskModel);

            WizardDialog dialog = new WizardDialog(shell, wizard);
            if (dialog.open() == Window.CANCEL)
                return;

            String newServerName = (String) taskModel.getObject(WebSphereServer.PROP_SERVER_NAME);
            WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            if (userDirectory == null) {
                userDirectory = wsRuntime.getDefaultUserDir();
                if (userDirectory == null) {
                    // Use the first user directory in the list
                    List<UserDirectory> userDirs = wsRuntime.getUserDirectories();
                    if (userDirs.size() > 0) {
                        userDirectory = userDirs.get(0);
                    }
                }
            }
            WebSphereServerInfo server = wsRuntime.getServerInfo(newServerName, userDirectory);
            if (server != null)
                getSelectionProvider().setSelection(new StructuredSelection(server));
        } catch (Exception e) {
            Trace.logError("Error creating WebSphere server", e);
        }
    }
}
