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
 * Command to change the user name entered by user on the editor.
 */
public class SetServerUserNameCommand extends ServerCommand {
    protected String userName;
    protected String oldUserName;

    public SetServerUserNameCommand(WebSphereServer server, String userName) {
        super(server, Messages.cmdSetServerUserName);
        this.userName = userName;
    }

    @Override
    public void execute() {
        oldUserName = server.getServerUserName();
        server.setServerUserName(userName);
    }

    @Override
    public void undo() {
        server.setServerUserName(oldUserName);
    }
}