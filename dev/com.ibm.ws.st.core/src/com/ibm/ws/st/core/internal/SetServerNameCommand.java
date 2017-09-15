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
package com.ibm.ws.st.core.internal;

/**
 * Command to set the server name.
 */
public class SetServerNameCommand extends ServerCommand {
    protected String name;
    protected String oldName;

    public SetServerNameCommand(WebSphereServer server, String name) {
        super(server, Messages.actionServerName);
        this.name = name;
    }

    public void execute() {
        oldName = server.getServerName();
        server.setServerName(name);
    }

    public void undo() {
        server.setServerName(oldName);
    }
}