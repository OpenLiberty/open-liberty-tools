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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;

import com.ibm.ws.st.ui.internal.Trace;

@SuppressWarnings("restriction")
public class TogglePublishAction implements IViewActionDelegate, IActionDelegate2, ISelectionChangedListener {
    private static final String LAST_AUTO_PUBLISH = "last-auto-publish";

    protected IViewPart view;
    protected IAction action;
    protected IServer server;

    @Override
    public void init(IViewPart view) {
        this.view = view;
        view.getViewSite().getSelectionProvider().addSelectionChangedListener(this);
    }

    @Override
    public void run(IAction action2) {
        if (server == null)
            return;

        try {
            IServerWorkingCopy wc = server.createWorkingCopy();
            ServerWorkingCopy wc2 = (ServerWorkingCopy) wc;
            int current = wc2.getAutoPublishSetting();
            if (current == Server.AUTO_PUBLISH_DISABLE) {
                int last = wc2.getAttribute(LAST_AUTO_PUBLISH, 0);
                if (last > 1)
                    wc2.setAutoPublishSetting(last);
                else
                    wc2.setAutoPublishSetting(Server.AUTO_PUBLISH_RESOURCE);
            } else {
                wc2.setAttribute(LAST_AUTO_PUBLISH, current);
                wc2.setAutoPublishSetting(Server.AUTO_PUBLISH_DISABLE);
            }
            wc2.save(false, null);
        } catch (CoreException ce) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not modify auto-publish setting", ce);
        }
    }

    @Override
    public void selectionChanged(IAction action2, ISelection selection) {
        server = null;
        boolean enabled = true;
        boolean state = false;

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection) selection;
            Iterator<?> iter = sel.iterator();
            if (!iter.hasNext())
                enabled = false;
            else {
                Object obj = iter.next();
                if (iter.hasNext()) {
                    enabled = false;
                } else {
                    if (obj instanceof IServer) {
                        server = (IServer) obj;
                        state = ((Server) server).getAutoPublishSetting() == Server.AUTO_PUBLISH_DISABLE;
                    } else
                        enabled = false;
                }
            }
        } else
            enabled = false;

        action.setChecked(state);
        action.setEnabled(enabled);
    }

    @Override
    public void dispose() {
        view.getViewSite().getSelectionProvider().removeSelectionChangedListener(this);
    }

    @Override
    public void init(IAction action2) {
        this.action = action2;
        action.setChecked(true);
    }

    @Override
    public void runWithEvent(IAction action2, Event event) {
        run(action2);
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        selectionChanged(action, event.getSelection());
    }
}
