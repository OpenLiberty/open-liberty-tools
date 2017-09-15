/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public class NewQuickServerAction extends SelectionProviderAction {
    private WebSphereServerInfo serverInfo;

    public NewQuickServerAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionNewQuickServer);
        setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_SERVER));
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        Object obj = sel.getFirstElement();
        if (obj instanceof WebSphereServerInfo) {
            serverInfo = (WebSphereServerInfo) obj;

            // Check that there is not already a server for this server definition
            WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
            for (WebSphereServer ws : servers) {
                if (serverInfo.equals(ws.getServerInfo())) {
                    setEnabled(false);
                    return;
                }
            }

            setEnabled(true);
            return;
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        createServer(serverInfo, new NullProgressMonitor());
    }

    public static void createServer(WebSphereServerInfo serverInfo, IProgressMonitor monitor) {
        try {
            IRuntime runtime = serverInfo.getWebSphereRuntime().getRuntime();
            IServerType st = null;
            String runtimeId = runtime.getRuntimeType().getId();
            if (runtimeId.endsWith(Constants.V85_ID_SUFFIX))
                st = ServerCore.findServerType(Constants.SERVERV85_TYPE_ID);
            else
                st = ServerCore.findServerType(Constants.SERVER_TYPE_ID);
            IServerWorkingCopy wc = st.createServer(null, null, runtime, monitor);
            wc.setName(serverInfo.getServerName());
            WebSphereServer ws = (WebSphereServer) wc.loadAdapter(WebSphereServer.class, monitor);
            ws.setServerName(serverInfo.getServerName());
            ws.setUserDir(serverInfo.getUserDirectory());
            wc.save(false, monitor);
        } catch (Exception e) {
            Trace.logError("Error trying to create server: " + serverInfo.getServerName(), e);
        }
    }
}
