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
 * Command to change the server password entered by user on the editor's "Server Connection Settings" section
 */
public class SetServerPasswordCommand extends ServerCommand {
    protected String password;
    protected String oldPassword;

    public SetServerPasswordCommand(WebSphereServer server, String password) {
        super(server, Messages.cmdSetServerPassword);
        this.password = password;
    }

    @Override
    public void execute() {
        oldPassword = server.getTempServerConnectionPassword();
        server.setTempServerConnectionPassword(password);
    }

    @Override
    public void undo() {
        server.setTempServerConnectionPassword(oldPassword);
    }
}