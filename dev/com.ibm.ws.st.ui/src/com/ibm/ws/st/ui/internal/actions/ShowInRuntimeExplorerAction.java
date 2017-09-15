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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.ui.internal.RuntimeExplorerView;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * "Show in > Runtime Explorer" menu action.
 */
public class ShowInRuntimeExplorerAction implements IObjectActionDelegate {
    protected WebSphereServer wsServer;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // do nothing
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() != 1) {
            action.setEnabled(false);
            return;
        }

        Object obj = sel.getFirstElement();
        if (obj instanceof IServer) {
            IServer server = (IServer) obj;
            wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
            action.setEnabled(true);
        } else
            action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (wsServer == null)
            return;

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(RuntimeExplorerView.VIEW_ID);
                if (part == null) {
                    try {
                        part = page.showView(RuntimeExplorerView.VIEW_ID);
                    } catch (PartInitException e) {
                        Trace.logError("Could not open runtime explorer view", e);
                    }
                }
                if (part != null) {
                    page.activate(part);
                    RuntimeExplorerView view = (RuntimeExplorerView) part.getAdapter(RuntimeExplorerView.class);
                    if (view != null) {
                        view.setFocus();
                        view.select(wsServer);
                    }
                }
            }
        }
    }
}
