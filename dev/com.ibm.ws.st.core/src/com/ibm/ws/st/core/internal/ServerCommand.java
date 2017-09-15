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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * A command that can be executed against a server.
 */
public abstract class ServerCommand extends AbstractOperation {
    protected WebSphereServer server;

    /**
     * ServerCommand constructor comment.
     * 
     * @param server a server
     * @param label a label
     */
    public ServerCommand(WebSphereServer server, String label) {
        super(label);
        this.server = server;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return execute(monitor, info);
    }

    public abstract void execute();

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        execute();
        return null;
    }

    public abstract void undo();

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        undo();
        return null;
    }
}