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

package com.ibm.ws.st.docker.ui.internal;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.ui.internal.ActionConstants;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.actions.AbstractUtilityUIEnablementExtension;
import com.ibm.ws.st.ui.internal.actions.WebSphereUtilityAction;

/**
 * Handle the UI enablement for Liberty Local Docker servers
 */
public class LibertyLocalDockerUtilityUIEnablement extends AbstractUtilityUIEnablementExtension {

    @Override
    public boolean notifyUtilityDisabled(String utilityId, WebSphereServer wsServer, WebSphereUtilityAction action, Shell shell,
                                         String serverType, String utilityType) {

        if (wsServer != null) {
            if (utilityId.equals(ActionConstants.PACKAGE_ACTION_ID) || utilityId.equals(ActionConstants.PLUGIN_CONFIG_ACTION_ID)
                || utilityId.equals(ActionConstants.CONFIG_SNIPPET_ACTION_ID)) {
                String key = null;
                key = Constants.UTILITY_NOT_SUPPORTED_PROMPT;
                if (!wsServer.getDisableUtilityPromptPref(key)) {
                    MessageDialogWithToggle errorMessage = MessageDialogWithToggle.openError(shell, com.ibm.ws.st.core.internal.Messages.remoteUtilitiesErrorTitle,
                                                                                             com.ibm.ws.st.core.internal.Messages.errorPromptLocalDockerUtilityNotSupported,
                                                                                             Messages.doNotShowAgain, false, null, null);
                    if (errorMessage.getToggleState()) {
                        wsServer.setDisableUtilityPromptPref(key, errorMessage.getToggleState());
                        action.setEnabled(false);
                    }
                    return true;
                }

                //          If the code reached here, this mean that the user is already notified but the utility is not disabled because the server selection might not have changed.
                //          In such scenario display the error message again.
                MessageDialogWithToggle error = MessageDialogWithToggle.openError(shell, com.ibm.ws.st.core.internal.Messages.remoteUtilitiesErrorTitle,
                                                                                  com.ibm.ws.st.core.internal.Messages.errorPromptLocalDockerUtilityNotSupported,
                                                                                  Messages.doNotShowAgain, false, null, null);
                if (error.getToggleState())
                    action.setEnabled(false);
                return true;
            }
        }

        return false;
    }
}
