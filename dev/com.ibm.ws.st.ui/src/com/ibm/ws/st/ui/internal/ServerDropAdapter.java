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
package com.ibm.ws.st.ui.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.actions.NewQuickServerAction;

public class ServerDropAdapter extends CommonDropAdapterAssistant {
    private List<WebSphereServerInfo> serverList;

    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferData) {
        if (target instanceof IWorkspaceRoot) {
            if (LocalSelectionTransfer.getTransfer().isSupportedType(transferData)) {
                ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
                if (!s.isEmpty() && s instanceof IStructuredSelection) {
                    IStructuredSelection sel = (IStructuredSelection) s;
                    Iterator<?> elements = sel.iterator();
                    serverList = new ArrayList<WebSphereServerInfo>(sel.size());
                    while (elements.hasNext()) {
                        Object obj = elements.next();
                        if (obj instanceof WebSphereServerInfo) {
                            WebSphereServerInfo server = (WebSphereServerInfo) obj;
                            String serverName = server.getServerName();
                            IRuntime runtime = server.getWebSphereRuntime().getRuntime();

                            WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
                            for (WebSphereServer ws : servers) {
                                if (runtime.equals(ws.getServer().getRuntime()) && serverName.equals(ws.getServerName())) {
                                    return Status.CANCEL_STATUS;
                                }
                            }
                            serverList.add(server);
                        }
                    }
                    // Return OK only if all elements were valid
                    if (serverList.size() == sel.size())
                        return Status.OK_STATUS;
                }
            }
        }

        return Status.CANCEL_STATUS;
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent, Object target) {
        for (WebSphereServerInfo server : serverList)
            NewQuickServerAction.createServer(server, null);
        return Status.OK_STATUS;
    }
}
