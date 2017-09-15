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
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.RuntimeExplorer;

public class RefreshAction extends SelectionProviderAction {
    private Object objectToRefresh;
    private final StructuredViewer viewer;

    public RefreshAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, Messages.actionRefresh);
        setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
        setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_REFRESH));
        this.viewer = viewer;
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IRuntime) {
                objectToRefresh = obj;
            } else if (obj instanceof WebSphereRuntime) {
                objectToRefresh = obj;
            } else if (obj instanceof UserDirectory) {
                UserDirectory userDir = (UserDirectory) obj;
                objectToRefresh = userDir.getWebSphereRuntime().getRuntime();
            } else if (obj instanceof RuntimeExplorer.Node) {
                RuntimeExplorer.Node node = (RuntimeExplorer.Node) obj;
                objectToRefresh = node.getWebSphereRuntime();
            } else if (obj instanceof WebSphereServerInfo) {
                WebSphereServerInfo server = (WebSphereServerInfo) obj;
                objectToRefresh = server.getWebSphereRuntime();
            } else if (obj instanceof ConfigurationFolder) {
                ConfigurationFolder folder = (ConfigurationFolder) obj;
                objectToRefresh = folder.getUserDirectory().getWebSphereRuntime();
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(true);
    }

    @Override
    public void run() {
        if (objectToRefresh == null)
            return;

        if (objectToRefresh instanceof IRuntime) {
            IRuntime runtime = (IRuntime) objectToRefresh;
            WebSphereRuntime wasRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            wasRuntime.refresh();
        }
        if (objectToRefresh instanceof WebSphereRuntime) {
            WebSphereRuntime runtime = (WebSphereRuntime) objectToRefresh;
            runtime.refresh();
        }
        viewer.refresh(objectToRefresh);
    }
}
