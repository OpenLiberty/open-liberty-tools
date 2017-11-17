/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.liberty.buildplugin.integration.servertype.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereUtil;

/**
 * Liberty Build Plugin Server Implementation
 */
@SuppressWarnings("restriction")
public class AbstractLibertyBuildPluginServer extends AbstractServerExtension {

    // In order to use the existing loose config publishing mechanism, the isLocalSetup needs to be
    // true, otherwise it attempts to do remote publishing
    @Override
    public Boolean isLocalSetup(IServer server) {
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(server);
        if (wsServer != null) {
            return new Boolean(wsServer.isLocalHost());
        }
        return null;
    }

    // Make sure the localConnector feature is enabled and the application
    // update trigger is set to mbean
    @Override
    public void serverConfigChanged(IServer server, IProgressMonitor monitor) {
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(server);
        if (wsServer != null) {
            wsServer.ensureLocalConnectorAndAppMBeanConfig(monitor);
        }
    }
}
