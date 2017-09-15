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

import com.ibm.ws.st.common.ui.internal.Activator;

public class SetServerStringAttributeCommand extends AbstractServerCommand {
    protected String newValue;

    protected String oldValue;

    public SetServerStringAttributeCommand(String label, IServerWorkingCopy curServer, String key, String value) {
        super(label, curServer, key);
        this.newValue = value;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus execute(IProgressMonitor mon, IAdaptable adaptable) throws ExecutionException {
        IStatus status = Status.OK_STATUS;
        try {
            oldValue = curServer.getAttribute(key, "");
            curServer.setAttribute(key, newValue);
        } catch (Exception e) {
            status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e);
        }
        return status;
    }

    /** {@inheritDoc} */
    @Override
    public IStatus redo(IProgressMonitor mon, IAdaptable adaptable) throws ExecutionException {
        return execute(mon, adaptable);
    }

    /** {@inheritDoc} */
    @Override
    public IStatus undo(IProgressMonitor mon, IAdaptable adaptable) throws ExecutionException {
        IStatus status = Status.OK_STATUS;
        try {
            curServer.setAttribute(key, oldValue);
        } catch (Exception e) {
            status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e);
        }
        return status;
    }
}
