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

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Instance command.
 */
public abstract class AbstractServerCommand extends AbstractOperation {
    protected IServerWorkingCopy curServer;
    protected String key;

    /**
     * ServerCommand constructor comment.
     */
    public AbstractServerCommand(String label, IServerWorkingCopy curServer, String key) {
        super(label);
        this.key = key;
        this.curServer = curServer;
    }

    /**
     * Returns true if this command can be undone.
     * 
     * @return boolean
     */
    @Override
    public boolean canUndo() {
        return true;
    }

}
