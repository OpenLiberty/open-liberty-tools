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
package com.ibm.ws.st.ui.internal.actions;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.RuntimeExplorer;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.wizard.WebSphereServerWizardFragment;

@SuppressWarnings("restriction")
public class NewServerAction extends SelectionProviderAction {
    private final Shell shell;
    private IRuntime runtime;
    private UserDirectory userDirectory;

    public NewServerAction(ISelectionProvider selectionProvider, Shell shell) {
        super(selectionProvider, Messages.actionNewServer);
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
                userDirectory = null; // use the default one
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
            IServerType st = null;
            String runtimeId = runtime.getRuntimeType().getId();
            if (runtimeId.endsWith(Constants.V85_ID_SUFFIX))
                st = ServerCore.findServerType(Constants.SERVERV85_TYPE_ID);
            else
                st = ServerCore.findServerType(Constants.SERVER_TYPE_ID);
            IServerWorkingCopy wc = st.createServer(null, null, runtime, null);
            TaskModel taskModel = new TaskModel();
            taskModel.putObject(TaskModel.TASK_SERVER, wc);
            taskModel.putObject(WebSphereRuntime.PROP_USER_DIRECTORY, userDirectory);
            taskModel.putObject(TaskModel.TASK_RUNTIME, runtime);
            taskModel.putObject("preventResize", Boolean.valueOf(true));
            WizardFragment fragment = new WizardFragment() {
                @Override
                protected void createChildFragments(List<WizardFragment> list) {
                    list.add(new WebSphereServerWizardFragment());
                    list.add(WizardTaskUtil.SaveServerFragment);
                }
            };
            TaskWizard wizard = new TaskWizard(Messages.title, fragment, taskModel);
            WizardDialog dialog = new WizardDialog(shell, wizard);
            dialog.open();
        } catch (CoreException ce) {
            Trace.logError("Error creating server", ce);
        }
    }
}
