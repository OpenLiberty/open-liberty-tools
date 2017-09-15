/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.ui.internal.ChangeRuntimeLocationDialog;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.plugin.ServerUtil;
import com.ibm.ws.st.ui.internal.wizard.WebSphereRuntimeWizardFragment;

@SuppressWarnings("restriction")
public class EditRuntimeAction extends SelectionProviderAction {
    protected final Shell shell;
    protected IRuntime runtime;

    private final StructuredViewer viewer;

    public EditRuntimeAction(Shell shell, ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, Messages.actionEditRuntime);
        this.shell = shell;
        this.viewer = viewer;
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
            if (obj instanceof IRuntime)
                runtime = (IRuntime) obj;
            else {
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
            IRuntimeWorkingCopy runtimeWorkingCopy = runtime.createWorkingCopy();
            if (showWizard(runtimeWorkingCopy) != Window.CANCEL) {
                WebSphereRuntime wasRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                wasRuntime.refresh();
            }
            viewer.refresh(runtime);
        } catch (Exception ce) {
            Trace.logError("Error editing runtime: " + runtime.getId(), ce);
        }
    }

    protected int showWizard(final IRuntimeWorkingCopy runtimeWorkingCopy) {
        final IPath oldLoc = runtimeWorkingCopy.getLocation();
        TaskModel taskModel = new TaskModel();
        taskModel.putObject(TaskModel.TASK_RUNTIME, runtimeWorkingCopy);

        TaskWizard wizard = new TaskWizard(Messages.wizRuntimeTitle, new WizardFragment() {
            @Override
            protected void createChildFragments(List<WizardFragment> list) {
                list.add(new WebSphereRuntimeWizardFragment());
                list.add(WizardTaskUtil.SaveRuntimeFragment);
            }
        }, taskModel);
        wizard.setForcePreviousAndNextButtons(true);
        WizardDialog dialog = new WizardDialog(shell, wizard) {
            @Override
            protected void finishPressed() {
                if (!oldLoc.equals(runtimeWorkingCopy.getLocation())) {
                    final IServer[] servers = ServerUtil.getServers(runtime);
                    ChangeRuntimeLocationDialog dialog = new ChangeRuntimeLocationDialog(shell, servers != null && servers.length > 0);
                    if (dialog.open() == Window.CANCEL)
                        return;

                    if (dialog.isDeleteServers()) {
                        try {
                            ServerUtil.deleteServers(servers, false);
                        } catch (Exception e) {
                            Trace.logError(e.getLocalizedMessage(), e);
                        }
                    }
                }
                super.finishPressed();
            }

        };
        return dialog.open();
    }
}
