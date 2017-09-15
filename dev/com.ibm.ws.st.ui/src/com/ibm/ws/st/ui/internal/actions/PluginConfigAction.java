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
package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.ui.internal.ActionConstants;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.utility.PluginConfigWizardPage;
import com.ibm.ws.st.ui.internal.utility.UtilityWizard;

/**
 * Generate plugin config action.
 */
public class PluginConfigAction extends WebSphereServerAction {

    public static final String ID = ActionConstants.PLUGIN_CONFIG_ACTION_ID;

    public PluginConfigAction(Shell shell, ISelectionProvider selProvider) {
        super(Messages.actionPluginConfig, shell, selProvider);
    }

    @Override
    public boolean isUtilityRemoteSupported() {
        return false;
    }

    @Override
    public void run() {
        if (notifyUtilityDisabled(wsServer.getServerType(), ID))
            return;
        UtilityWizard.open(shell, new PluginConfigWizardPage(server));
    }

    @Override
    public String getId() {
        return ID;
    }
}