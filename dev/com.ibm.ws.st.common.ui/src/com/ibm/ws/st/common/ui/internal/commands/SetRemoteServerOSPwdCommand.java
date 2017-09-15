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
package com.ibm.ws.st.common.ui.internal.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;

/**
 * Command to change the server password entered by User on the editor.
 */
public class SetRemoteServerOSPwdCommand extends AbstractServerCommand {
    protected String password;
    protected String oldPassword;
    protected RemoteServerInfo remoteInfo;

    public SetRemoteServerOSPwdCommand(String label, IServerWorkingCopy curServer, String key, String value, RemoteServerInfo remoteInfo) {
        super(label, curServer, key);
        this.password = value;
        this.remoteInfo = remoteInfo;
    }

    @Override
    public IStatus execute(IProgressMonitor mon, IAdaptable adaptable) {
        oldPassword = remoteInfo.getTempRemoteOSPassword();
        remoteInfo.setTempRemoteOSPassword(password);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor mon, IAdaptable adaptable) {
        remoteInfo.setTempRemoteOSPassword(oldPassword);
        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus redo(IProgressMonitor mon, IAdaptable adaptable) throws ExecutionException {
        return execute(mon, adaptable);
    }
}