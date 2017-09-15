/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.ui.internal.Trace;

public class ShowInServersAction extends SelectionProviderAction {
    private static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";
    private IServer server;

    public ShowInServersAction(ISelectionProvider sp) {
        super(sp, "Servers");

        IViewRegistry reg = PlatformUI.getWorkbench().getViewRegistry();
        IViewDescriptor desc = reg.find(SERVERS_VIEW_ID);
        setText(desc.getLabel());
        setImageDescriptor(desc.getImageDescriptor());
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.isEmpty()) {
            setEnabled(false);
            return;
        }
        boolean enabled = false;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof WebSphereServerInfo) {
                WebSphereServerInfo serverInfo = (WebSphereServerInfo) obj;
                String serverName = serverInfo.getServerName();

                server = null;
                WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
                for (WebSphereServer ws : servers) {
                    if (serverName.equals(ws.getServerName())) {
                        server = ws.getServer();
                    }
                }
                enabled = (server != null);
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(enabled);
    }

    @Override
    public void run() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(SERVERS_VIEW_ID);
                if (part == null) {
                    try {
                        part = page.showView(SERVERS_VIEW_ID);
                    } catch (PartInitException e) {
                        Trace.logError("Could not open servers view", e);
                    }
                }
                if (part != null) {
                    page.activate(part);
                    CommonNavigator view = (CommonNavigator) part.getAdapter(CommonNavigator.class);
                    if (view != null) {
                        view.setFocus();
                        view.selectReveal(new StructuredSelection(server));
                    }
                }
            }
        }
    }
}
