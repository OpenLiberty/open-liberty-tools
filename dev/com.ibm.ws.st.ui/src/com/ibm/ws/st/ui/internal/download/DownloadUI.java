/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.ui.internal.download;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;

import com.ibm.ws.st.ui.internal.Messages;

@SuppressWarnings("restriction")
public class DownloadUI {

    public static int launchAddonsDialog(Shell shell, TaskModel taskModel) {
        TaskWizard wizard = new TaskWizard(Messages.wizInstallAddonTitle, new ExtendRuntimeWizardContainer(), taskModel);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setPageSize(450, 600);
        return dialog.open();
    }

}
