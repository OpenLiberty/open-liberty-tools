/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite.IContainer;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite.IMessageHandler;

/**
 *
 */
public abstract class AbstractWizardFragment extends WizardFragment {

    public static final String ARCHIVE_SOURCE = "archive_source";

    protected AbstractDownloadComposite comp;
    protected int severity;

    @Override
    public void enter() {
        if (hasComposite() && comp != null) {
            comp.enter();
        }
    }

    @Override
    public void exit() {
        if (hasComposite() && comp != null)
            comp.exit();
    }

    @Override
    public boolean hasComposite() {
        return getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP) != null;
    }

    @Override
    public boolean isComplete() {
        return comp == null || severity != IMessageProvider.ERROR;
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        if (comp != null)
            comp.performCancel(monitor);
    }

    protected IMessageHandler getMessageHandler(final IWizardHandle wizard) {
        return new IMessageHandler() {
            @Override
            public void setMessage(String message, int severity) {
                AbstractWizardFragment.this.severity = severity;
                wizard.setMessage(message, severity);
                wizard.update();
            }
        };
    }

    protected IContainer getContainer(final IWizardHandle wizard) {
        return new IContainer() {
            @Override
            public void setTitle(String title) {
                wizard.setTitle(title);
            }

            @Override
            public void setDescription(String desc) {
                wizard.setDescription(desc);
            }

            @Override
            public void setImageDescriptor(ImageDescriptor image) {
                wizard.setImageDescriptor(image);
            }

            @Override
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InterruptedException, InvocationTargetException {
                wizard.run(fork, cancelable, runnable);
            }
        };
    }
}
