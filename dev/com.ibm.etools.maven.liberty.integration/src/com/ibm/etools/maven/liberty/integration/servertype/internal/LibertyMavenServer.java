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

package com.ibm.etools.maven.liberty.integration.servertype.internal;

import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;
import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 * Liberty Maven Server Implementation
 */

@SuppressWarnings("restriction")
public class LibertyMavenServer extends AbstractServerExtension {

    // In order to use the existing loose config publishing mechanism, the isLocalSetup needs to be
    // true, otherwise it attempts to do remote publishing
    @Override
    public Boolean isLocalSetup(IServer server) {
        if (server != null) {
            WebSphereServer wsServer = server.getAdapter(WebSphereServer.class);
            return new Boolean(wsServer.isLocalHost());
        }
        return null;
    }
}