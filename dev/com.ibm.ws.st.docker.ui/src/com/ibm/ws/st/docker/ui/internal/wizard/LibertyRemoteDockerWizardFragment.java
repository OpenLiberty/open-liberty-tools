/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.ui.internal.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.util.SocketUtil;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.docker.ui.internal.Activator;
import com.ibm.ws.st.docker.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.wizard.WebSphereServerWizardCommonFragment;

/**
 * New server wizard fragment for liberty running on local docker.
 */
public class LibertyRemoteDockerWizardFragment extends WizardFragment {

    protected LibertyDockerComposite comp;

    public LibertyRemoteDockerWizardFragment() {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasComposite() {
        boolean isDockerType = Activator.SERVER_TYPE.equals(getTaskModel().getObject(WebSphereServerWizardCommonFragment.SERVER_TYPE_DATA));
        return isDockerType && !isLocalhost();
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        comp = new LibertyDockerComposite(parent, wizard, getTaskModel());
        return comp;
    }

    @Override
    public void enter() {
        if (!hasComposite()) {
            return;
        }
        if (comp != null) {
            comp.setup(getTaskModel());
            comp.clearMessage(); //should not validate initial state
            comp.initialValidate();
        } else {
            IServerWorkingCopy wc = (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
            WebSphereServer server = wc.getAdapter(WebSphereServer.class);
            server.setDefaults(new NullProgressMonitor());
        }
    }

    @Override
    public void exit() {
        if (!hasComposite()) {
            return;
        }
        if (comp != null) {
            comp.clearContainers();
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
            // We need this because the comp is null if not localhost
            isComplete = !hasComposite();
        }
        return isComplete && super.isComplete();
    }

    protected boolean isLocalhost() {
        IServerWorkingCopy swc = (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
        String host = swc.getHost();

        if (host != null)
            return SocketUtil.isLocalhost(host);

        Trace.logError("The value for host in the server task model is null", new Exception("Host value is null"));
        return true;
    }
}
