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
package com.ibm.ws.st.ui.internal.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.Wizard;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

public class UserDirWizard extends Wizard {
    protected UserDirWizardPage page;

    public UserDirWizard() {
        setWindowTitle(Messages.wizUserDirTitle);
        setDefaultPageImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_RUNTIME));
        page = new UserDirWizardPage();
        addPage(page);
        setNeedsProgressMonitor(false);

    }

    public IPath getPath() {
        return page.getPath();
    }

    public IProject getProject() {
        return page.getProject();
    }

    @Override
    public boolean performFinish() {
        return true;
    }
}
