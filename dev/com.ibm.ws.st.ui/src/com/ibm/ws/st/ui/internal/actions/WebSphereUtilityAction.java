/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;

/**
 * WebSphere Utility Action
 */
public abstract class WebSphereUtilityAction extends AbstractProviderAction {

    protected WebSphereServerInfo server;
    protected WebSphereServer wsServer;
    protected BaseLibertyUtilityUIEnablement baseEnablement = null;

    public WebSphereUtilityAction(String name, Shell shell, ISelectionProvider selProvider) {
        super(name, shell, selProvider);
    }

    @Override
    public boolean modifySelection(Iterator<?> iterator) {
        server = null;
        Object obj = iterator.next();
        if (obj instanceof WebSphereServerInfo)
            server = (WebSphereServerInfo) obj;
        else
            server = Platform.getAdapterManager().getAdapter(obj, WebSphereServerInfo.class);

        if (server == null)
            setEnabled(false);

        wsServer = WebSphereUtil.getWebSphereServer(server);

        return selectionChanged(iterator);
    }

    @Override
    public abstract boolean selectionChanged(Iterator<?> obj);

    public boolean isUtilityRemoteSupported() {
        return true;
    }

    protected boolean notifyUtilityDisabled(String serverType, String utilityType) {
        AbstractUtilityUIEnablementExtension utilityClass = UtilityUIEnablementExtensionFactory.getServerUtilityUIEnablementOperation(wsServer.getServerType());
        if (utilityClass != null) {
            return utilityClass.notifyUtilityDisabled(this.getId(), wsServer, this, shell, serverType, utilityType);
        }
        return false;
    }

    /**
     * only for remote servers
     * checks if the user is already prompted that utilities are disabled or not
     *
     * @return true if the user is already notified why utilities are disabled false otherwise
     */
    protected boolean isDisableUtilityPrompted() {
        if (wsServer != null && !wsServer.isLocalSetup()) {
            if (!isUtilityRemoteSupported()) {
                return wsServer.getDisableUtilityPromptPref(Constants.UTILITY_NOT_SUPPORTED_PROMPT);
            } else if (!wsServer.getIsRemoteServerStartEnabled())
                return wsServer.getDisableUtilityPromptPref(Constants.REMOTE_SETTINGS_DISABLED_PROMPT);
        }
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public WebSphereServer getServer() {
        return wsServer;
    }
}
