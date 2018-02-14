/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;

public class DocumentServerAdapter implements IAdapterFactory {
    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IServer.class) {
            if (adaptableObject instanceof Element) {
                Element element = (Element) adaptableObject;
                URI uri = DDETreeContentProvider.getURI(element);
                if (uri == null)
                    return null;
                WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
                for (WebSphereServer server : servers) {
                    if (server.getConfigurationFileFromURI(uri) != null)
                        return server.getServer();
                }
            }
        }
        if (adapterType == WebSphereServerInfo.class) {
            if (adaptableObject instanceof IServer) {
                WebSphereServer ws = ((IServer) adaptableObject).getAdapter(WebSphereServer.class);
                if (ws != null)
                    return ws.getServerInfo();
            }
            if (adaptableObject instanceof Element) {
                Element element = (Element) adaptableObject;
                URI uri = DDETreeContentProvider.getURI(element);
                if (uri == null)
                    return null;
                WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
                for (WebSphereServerInfo server : servers) {
                    if (server.getConfigurationFileFromURI(uri) != null)
                        return server;
                }
            }
        }
        // Need this for getting the server config IFile associated with the server.xml element in the Servers view
        if (adapterType == IFile.class) {
            if (adaptableObject instanceof Element) {
                Element element = (Element) adaptableObject;
                URI uri = DDETreeContentProvider.getURI(element);
                if (uri == null)
                    return null;
                IFile configFile = ConfigUtils.getMappedConfigIFile(uri);
                return configFile;
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class[] { IServer.class, WebSphereServerInfo.class };
    }
}
