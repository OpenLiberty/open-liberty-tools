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

import java.net.URI;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.DDETreeContentProvider;
import com.ibm.ws.st.ui.internal.Messages;

public class RefreshConfigFileAction extends SelectionProviderAction {
    private WebSphereServerInfo refreshServer;

    public RefreshConfigFileAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionRefresh);
        setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
        setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_REFRESH));
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        refreshServer = null;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IServer) {
                IServer srv = (IServer) obj;
                WebSphereServer server = (WebSphereServer) srv.loadAdapter(WebSphereServer.class, null);
                if (server != null)
                    refreshServer = server.getServerInfo();
            } else if (obj instanceof ConfigurationFile) {
                ConfigurationFile configFile = (ConfigurationFile) obj;
                URI uri = configFile.getURI();
                WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
                for (WebSphereServerInfo server : servers) {
                    ConfigurationFile file = server.getConfigurationFileFromURI(uri);
                    if (file != null)
                        refreshServer = server;
                }
            } else if (obj instanceof Element) {
                Element element = (Element) obj;
                URI uri = DDETreeContentProvider.getURI(element);
                WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
                for (WebSphereServerInfo server : servers) {
                    ConfigurationFile file = server.getConfigurationFileFromURI(uri);
                    if (file != null)
                        refreshServer = server;
                }
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(refreshServer != null);
    }

    @Override
    public void run() {
        if (refreshServer == null)
            return;

        refreshServer.updateCache();
    }
}
