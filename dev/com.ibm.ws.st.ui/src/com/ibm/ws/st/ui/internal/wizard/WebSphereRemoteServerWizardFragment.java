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
package com.ibm.ws.st.ui.internal.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 *
 */
public class WebSphereRemoteServerWizardFragment extends WebSphereServerWizardCommonFragment {
    protected WebSphereServerParentComposite comp;

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        comp = new WebSphereServerParentComposite(parent, wizard, getActiveServerTypeExtensions(), getTaskModel());
        return comp;
    }

    @Override
    public boolean hasComposite() {
        return !isLocalhost();
    }

    @Override
    public void enter() {
        if (isLocalhost())
            return;
        IServerWorkingCopy wc = (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
        if (comp != null) {
            comp.setServer(wc, null);
            comp.wizard.setMessage(null, IMessageProvider.NONE); //should not validate initial state
        }
        else {
            WebSphereServer server = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);
            server.setDefaults(new NullProgressMonitor());
        }
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        // composite might not be null if the wizard is reused
        if (hasComposite() && comp != null && !comp.isDisposed())
            comp.performFinish(monitor);
    }

    @Override
    public void performCancel(IProgressMonitor monitor) {
        // composite might not be null if the wizard is reused
        if (hasComposite() && comp != null && !comp.isDisposed())
            comp.performCancel();
    }

    @Override
    public boolean isComplete() {
        boolean isComplete = true;
        // composite might not be null if the wizard is reused
        if (hasComposite() && comp != null && !comp.isDisposed()) {
            isComplete = comp.isComplete();
        } else {
            //We need this because the comp is null if it's local scenario
            isComplete = !hasComposite();
        }
        return isComplete && super.isComplete();
    }
}
