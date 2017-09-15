/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import org.eclipse.wst.server.core.IServer;

/**
 * Server startup data including the server object and the launch mode.
 */
public class ServerStartInfo {

    private final IServer server;
    private final String mode;

    public ServerStartInfo(IServer server, String mode) {
        this.server = server;
        this.mode = mode;
    }

    public IServer getServer() {
        return server;
    }

    public String getMode() {
        return mode;
    }

}
