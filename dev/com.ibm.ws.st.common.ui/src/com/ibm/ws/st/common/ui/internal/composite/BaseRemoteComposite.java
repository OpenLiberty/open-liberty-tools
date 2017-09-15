/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.ui.internal.composite;

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.swt.widgets.Composite;

/**
 *
 */
public abstract class BaseRemoteComposite extends AbstractRemoteComposite {

    public BaseRemoteComposite(Composite parent) {
        super(parent);
    }

    protected abstract void execute(IUndoableOperation op);

    protected abstract void showValidationError(int key);

    protected abstract void setUpdating(boolean value);

    protected abstract boolean isUpdating();

}
