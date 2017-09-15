/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

/**
 * Command to change the server Port entered by user on the editor.
 */
public class SetServerPortCommand extends ServerCommand {
    protected String port;
    protected String oldPort;

    public SetServerPortCommand(WebSphereServer server, String port) {
        super(server, Messages.cmdSetServerPort);
        this.port = port;
    }

    @Override
    public void execute() {
        oldPort = server.getServerSecurePort();
        server.setServerSecurePort(port);
    }

    @Override
    public void undo() {
        server.setServerSecurePort(oldPort);
    }
}