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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;

/**
 * "Clean" workarea menu action.
 */
public class CleanAction implements IObjectActionDelegate {
    protected IServer server;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // no-op
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }
        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof IServer) {
                server = (IServer) obj;
                WebSphereServerBehaviour wsServer = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
                if (wsServer.canCleanOnStart()) {
                    action.setEnabled(true);
                    action.setChecked(wsServer.isCleanOnStartup());
                    return;
                }
            }
        }
        action.setChecked(false);
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (server == null)
            return;

        WebSphereServerBehaviour wsServer = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        wsServer.setCleanOnStartup(action.isChecked());
    }
}
