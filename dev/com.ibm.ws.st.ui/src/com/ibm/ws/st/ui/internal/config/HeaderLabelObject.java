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
package com.ibm.ws.st.ui.internal.config;

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.customization.ICustomLabelObject;

/**
 * Editor customization to show the server name in the header
 * when possible.
 */
public class HeaderLabelObject implements ICustomLabelObject {

    /** {@inheritDoc} */
    @Override
    public String getLabel(Element element, IResource resource) {
        if (resource != null) {
            URI uri = resource.getLocation().toFile().toURI();
            WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
            for (WebSphereServerInfo server : servers) {
                ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
                if (configFile != null) {
                    switch (configFile.getLocationType()) {
                        case SERVER:
                            return NLS.bind(Messages.configEditorHeaderWithServer, server.getServerName(), resource.getName());
                        case SHARED:
                            return NLS.bind(Messages.sharedConfigEditorHeaderWithFile, resource.getName());
                        default:
                            return NLS.bind(Messages.configEditorHeaderWithFile, resource.getName());
                    }
                }
            }
            return NLS.bind(Messages.configEditorHeaderWithFile, resource.getName());
        }
        return Messages.configEditorHeader;
    }
}
