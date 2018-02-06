/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFolder;
import com.ibm.ws.st.core.internal.config.ExtendedConfigFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.RuntimeExplorer;

public class ShowInExplorerAction extends SelectionProviderAction {
    private static final String EXPLORER_VIEW_ID = "org.eclipse.ui.navigator.ProjectExplorer";
    private IResource resource;

    public ShowInExplorerAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, "Explorer");

        IViewRegistry reg = PlatformUI.getWorkbench().getViewRegistry();
        IViewDescriptor desc = reg.find(EXPLORER_VIEW_ID);
        setText(desc.getLabel());
        setImageDescriptor(desc.getImageDescriptor());
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        resource = null;
        Object obj = sel.getFirstElement();
        if (obj instanceof IServer) {
            IServer srv = (IServer) obj;
            WebSphereServer server = (WebSphereServer) srv.loadAdapter(WebSphereServer.class, null);
            if (server != null && server.getServerPath() != null)
                resource = server.getServerInfo().getServerFolder();
        } else if (obj instanceof ConfigurationFolder) {
            ConfigurationFolder folder = (ConfigurationFolder) obj;
            resource = folder.getFolder();
        } else if (obj instanceof ConfigurationFile) {
            ConfigurationFile configFile = (ConfigurationFile) obj;
            resource = configFile.getIFile();
        } else if (obj instanceof ExtendedConfigFile) {
            ExtendedConfigFile configFile = (ExtendedConfigFile) obj;
            resource = configFile.getIFile();
        } else if (obj instanceof Element) {
            Element element = (Element) obj;
            // only enable on the root ("the file") element
            if (element.getParentNode() == element.getOwnerDocument()) {
                WebSphereServerInfo wsi = Platform.getAdapterManager().getAdapter(obj, WebSphereServerInfo.class);
                if (wsi != null) { // Classic Liberty
                    resource = wsi.getServerFolder();
                } else {
                	    // Runtime Providers
                    IFile configFile = Platform.getAdapterManager().getAdapter(obj, IFile.class);
                    if (configFile != null) {
                        resource = configFile;
                    }
                }
            }
        } else if (obj instanceof UserDirectory) {
            UserDirectory userDir = (UserDirectory) obj;
            resource = userDir.getProject();
        } else if (obj instanceof RuntimeExplorer.Node) {
            RuntimeExplorer.Node node = (RuntimeExplorer.Node) obj;
            UserDirectory userDir = node.getUserDirectory();
            if (RuntimeExplorer.NodeType.SHARED_APPLICATIONS == node.getType())
                resource = userDir.getSharedAppsFolder();
            else if (RuntimeExplorer.NodeType.SHARED_CONFIGURATIONS == node.getType())
                resource = userDir.getSharedConfigFolder();
            else if (RuntimeExplorer.NodeType.SERVERS == node.getType())
                resource = userDir.getServersFolder();
        } else if (obj instanceof WebSphereServerInfo) {
            WebSphereServerInfo server = (WebSphereServerInfo) obj;
            resource = server.getServerFolder();
        }
        setEnabled(resource != null);
    }

    @Override
    public void run() {
        if (resource == null) {
            return;
        }

        Activator.showResource(resource);
    }
}
