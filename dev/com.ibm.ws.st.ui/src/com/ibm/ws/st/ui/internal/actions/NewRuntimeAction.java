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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.wizard.WebSphereRuntimeWizardFragment;

@SuppressWarnings("restriction")
public class NewRuntimeAction extends Action {
    private final Shell shell;

    public NewRuntimeAction(Shell shell) {
        super(Messages.actionNewRuntime);
        this.shell = shell;
        setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_RUNTIME));
    }

    @Override
    public void run() {
        try {
            IRuntimeType rt = ServerCore.findRuntimeType(Constants.DEFAULT_RUNTIME_TYPE_ID);
            IRuntimeWorkingCopy wc = rt.createRuntime(null, null);
            TaskModel taskModel = new TaskModel();
            taskModel.putObject(TaskModel.TASK_RUNTIME, wc);
            WizardFragment fragment = new WizardFragment() {
                @Override
                protected void createChildFragments(List<WizardFragment> list) {
                    list.add(new WebSphereRuntimeWizardFragment());
                    list.add(WizardTaskUtil.SaveRuntimeFragment);
                }
            };
            TaskWizard wizard = new TaskWizard(Messages.title, fragment, taskModel);
            WizardDialog dialog = new WizardDialog(shell, wizard);
            dialog.open();
        } catch (CoreException ce) {
            Trace.logError("Error creating runtime", ce);
        }
    }
}
