/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;

/**
 *
 */
public class BaseLibertyServerExtension extends AbstractServerExtension {
    public Map<String, String> getServiceInfo() {
        return new HashMap<String, String>();
    }

    /** {@inheritDoc} */
    @Override
    public String getServerDisplayName(IServer server) {
        WebSphereServer wsServer = (WebSphereServer) server.getAdapter(WebSphereServer.class);
        if (wsServer == null) {
            // This should never happen
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "The server is not a WebSphereServer as expected: " + server.getName());
            }
            return null;
        }
        return wsServer.getServerName();
    }
}
