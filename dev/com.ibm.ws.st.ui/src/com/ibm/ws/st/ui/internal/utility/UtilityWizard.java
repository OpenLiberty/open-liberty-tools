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
package com.ibm.ws.st.ui.internal.utility;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Trace;

public class UtilityWizard extends Wizard {
    protected final UtilityWizardPage page;

    public UtilityWizard(UtilityWizardPage page) {
        this.page = page;
        setWindowTitle(page.getTitle());
        addPage(page);
        setNeedsProgressMonitor(true);
        setDefaultPageImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_SERVER));
    }

    public static int open(Shell shell, UtilityWizardPage page) {
        UtilityWizard wizard = new UtilityWizard(page);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setPageSize(400, 400);
        return dialog.open();
    }

    @Override
    public boolean performFinish() {
        if (!page.preFinish())
            return false;

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        page.finish(monitor);
                    } catch (final Throwable t) {
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                MessageDialog.openError(getShell(), page.getTitle(), t.getLocalizedMessage());
                            }
                        });
                        throw new InvocationTargetException(t);
                    }
                }
            });
        } catch (Exception e) {
            Trace.logError("Error finishing wizard with the title: " + page.getTitle(), e);
            return false;
        }

        return true;
    }
}
