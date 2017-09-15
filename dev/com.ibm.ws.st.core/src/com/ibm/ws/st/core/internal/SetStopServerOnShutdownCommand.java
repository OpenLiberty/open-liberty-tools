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
 * Command to change the loose config setting.
 */
public class SetStopServerOnShutdownCommand extends ServerCommand {
    protected boolean isEnabled;
    protected boolean oldIsEnabled;

    public SetStopServerOnShutdownCommand(WebSphereServer server, boolean curIsEnabled) {
        super(server, Messages.cmdSetStopServerOnShutdown);
        this.isEnabled = curIsEnabled;
    }

    @Override
    public void execute() {
        oldIsEnabled = server.isLooseConfigEnabled();
        server.setStopOnShutdown(isEnabled);
    }

    @Override
    public void undo() {
        server.setStopOnShutdown(oldIsEnabled);
    }
}