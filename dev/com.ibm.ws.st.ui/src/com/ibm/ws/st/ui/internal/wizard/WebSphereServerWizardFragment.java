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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;

public class WebSphereServerWizardFragment extends WebSphereServerWizardCommonFragment {
    protected WebSphereServerParentComposite comp;

    /** The last path value seen for runtime location field. */
    protected IPath lastRuntimeLocation = null;

    protected IRuntime lastRuntime = null;

    protected boolean hadServers = false;

    public WebSphereServerWizardFragment() {
        //
    }

    @Override
    public boolean hasComposite() {
        return isLocalhost();
    }

    @Override
    public boolean isComplete() {
        if (!hasComposite() || comp == null || comp.isDisposed())
            return false;

        return comp.isComplete();
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        comp = new WebSphereServerParentComposite(parent, wizard, getActiveServerTypeExtensions(), getTaskModel());
        return comp;
    }

    @Override
    public void enter() {
        // update the child composite if the runtime has changed
        IRuntime rt = (IRuntime) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
        if (lastRuntime != null && !lastRuntime.equals(rt)) {
            boolean hasServers = runtimeHasServers();
            if (hasServers != hadServers) {
                comp.updateChildren(hasServers);
            } else {
                for (Control ctrl : comp.getChildren()) {
                    if (ctrl instanceof IServerWizardComposite)
                        ((IServerWizardComposite) ctrl).reInitialize();
                }
            }

        }

        lastRuntime = rt;
        hadServers = runtimeHasServers();

        if (!runtimeHasServers() || !isLocalhost()) {
            return;
        }

        IServerWorkingCopy wc = (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);

        if (comp != null) {
            UserDirectory userDir = (UserDirectory) getTaskModel().getObject(WebSphereRuntime.PROP_USER_DIRECTORY);
            comp.setServer(wc, userDir);
        } else {
            WebSphereServer server = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, null);
            server.setDefaults(new NullProgressMonitor());
        }
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        if (hasComposite() && comp != null)
            comp.performFinish(monitor);
    }

    @Override
    public void performCancel(IProgressMonitor monitor) {
        if (hasComposite() && comp != null) {
            comp.performCancel();
        }

    }
}
