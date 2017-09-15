/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.Wizard;

import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.actions.NewConfigDropinAction;

public class NewConfigDropinWizard extends Wizard {
    protected NewConfigDropinWizardPage page;

    public NewConfigDropinWizard(IPath dropinsPath, NewConfigDropinAction.DropinType type) {
        setWindowTitle(Messages.newConfigDropinWizardTitle);
        setDefaultPageImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_RUNTIME));
        page = new NewConfigDropinWizardPage(dropinsPath, type);
        addPage(page);
        setNeedsProgressMonitor(false);

    }

    public String getFileName() {
        return page.getFileName();
    }

    @Override
    public boolean performFinish() {
        return true;
    }
}
