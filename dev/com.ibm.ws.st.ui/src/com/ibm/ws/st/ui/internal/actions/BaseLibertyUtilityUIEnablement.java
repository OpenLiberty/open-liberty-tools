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

package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.ui.internal.ActionConstants;
import com.ibm.ws.st.ui.internal.Messages;

/**
 *
 */
public class BaseLibertyUtilityUIEnablement extends AbstractUtilityUIEnablementExtension {

    // Originally taken from com.ibm.ws.st.ui.internal.actions.WebSphereUtilityAction.notifyUtilityDisabled()

    @Override
    /**
     * only applicable to remote servers
     * notify the user that utility is disabled. That happens if the utility is not supported or the Remote Execution settings are not enabled
     *
     * @return true when the user is notified that utility is disabled, false otherwise
     */
    public boolean notifyUtilityDisabled(String utilityId, WebSphereServer wsServer, WebSphereUtilityAction action, Shell shell, String serverType, String utilityType) {
        if (wsServer != null && !wsServer.isLocalSetup()) {
            String key = null;
            //"Web plugin config" and "add config snippets" is not supported for remote server, check if error is displayed to the user, if not display it
            if (action.getId().equals(ActionConstants.PLUGIN_CONFIG_ACTION_ID) || action.getId().equals(ActionConstants.CONFIG_SNIPPET_ACTION_ID)) {
                key = Constants.UTILITY_NOT_SUPPORTED_PROMPT;
                if (!wsServer.getDisableUtilityPromptPref(key)) {
                    MessageDialogWithToggle errorMessage = MessageDialogWithToggle.openError(shell, com.ibm.ws.st.core.internal.Messages.remoteUtilitiesErrorTitle,
                                                                                             com.ibm.ws.st.core.internal.Messages.errorPromptRemoteUtilityNotSupported,
                                                                                             Messages.doNotShowAgain, false, null, null);
                    if (errorMessage.getToggleState()) {
                        wsServer.setDisableUtilityPromptPref(key, errorMessage.getToggleState());
                        action.setEnabled(false);
                    }
                    return true;
                }
//              If the code reached here, this mean that the user is already notified but the utility is not disabled because the server selection might not have changed.
//              In such scenario display the error message again.
                MessageDialogWithToggle error = MessageDialogWithToggle.openError(shell, com.ibm.ws.st.core.internal.Messages.remoteUtilitiesErrorTitle,
                                                                                  com.ibm.ws.st.core.internal.Messages.errorPromptRemoteUtilityNotSupported,
                                                                                  Messages.doNotShowAgain, false, null, null);
                if (error.getToggleState())
                    action.setEnabled(false);
                return true;
            }
            //if utilities are supported and remote settings are not enabled and user is not prompted
            else if (action.isUtilityRemoteSupported() && !wsServer.getIsRemoteServerStartEnabled() && !wsServer.getServerType().equals("LibertyDocker")) {
                key = Constants.REMOTE_SETTINGS_DISABLED_PROMPT;
                if (!wsServer.getDisableUtilityPromptPref(key)) {
                    MessageDialogWithToggle errorMessage = MessageDialogWithToggle.openError(shell, com.ibm.ws.st.core.internal.Messages.remoteUtilitiesErrorTitle,
                                                                                             com.ibm.ws.st.core.internal.Messages.errorPromptCannotRunRemoteUtility,
                                                                                             Messages.doNotShowAgain, false, null, null);
                    if (errorMessage.getToggleState()) {
                        wsServer.setDisableUtilityPromptPref(key, errorMessage.getToggleState());
                        action.setEnabled(false);
                    }
                    return true;
                }
                MessageDialogWithToggle error = MessageDialogWithToggle.openError(shell, com.ibm.ws.st.core.internal.Messages.remoteUtilitiesErrorTitle,
                                                                                  com.ibm.ws.st.core.internal.Messages.errorPromptCannotRunRemoteUtility,
                                                                                  Messages.doNotShowAgain, false, null, null);
                if (error.getToggleState())
                    action.setEnabled(false);
                return true;
            }
        }
        return false;
    }
}
