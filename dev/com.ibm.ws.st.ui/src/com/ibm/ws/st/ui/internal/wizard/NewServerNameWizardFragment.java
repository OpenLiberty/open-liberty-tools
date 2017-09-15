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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.ui.internal.download.AbstractWizardFragment;

public class NewServerNameWizardFragment extends WizardFragment {
    protected NewServerNameComposite comp;

    public NewServerNameWizardFragment() {
        // do nothing
    }

    @Override
    public boolean hasComposite() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return (comp == null || comp.wizard.getMessageType() != IMessageProvider.ERROR);
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle wizard) {
        comp = new NewServerNameComposite(parent, wizard, false);
        return comp;
    }

    @Override
    public void enter() {
        if (comp != null)
            comp.setRuntime((IRuntime) getTaskModel().getObject(TaskModel.TASK_RUNTIME), (UserDirectory) getTaskModel().getObject(WebSphereRuntime.PROP_USER_DIRECTORY),
                            (String) getTaskModel().getObject(AbstractWizardFragment.ARCHIVE_SOURCE));
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        if (comp != null) {
            String serverName = comp.getServerName();
            UserDirectory userDir = comp.getUserDir();
            getTaskModel().putObject(WebSphereServer.PROP_SERVER_NAME, serverName);
            getTaskModel().putObject(WebSphereRuntime.PROP_USER_DIRECTORY, userDir);
            comp.createServer(monitor);
        }
    }
}
