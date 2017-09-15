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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import com.ibm.ws.st.ui.internal.Messages;

public class NewUserDirProjectWizardPage extends WizardNewProjectCreationPage {
    public NewUserDirProjectWizardPage() {
        super("new-user-dir-project");
        setTitle(Messages.wizNewUserProjectTitle);
        setDescription(Messages.wizNewUserProjectDescription);
    }

    @Override
    public void createControl(final Composite parent) {
        super.createControl(parent);

        Composite composite = (Composite) getControl();
        createWorkingSetGroup(composite, null, new String[] { "org.eclipse.ui.resourceWorkingSetPage" });

        Dialog.applyDialogFont(composite);
    }
}
