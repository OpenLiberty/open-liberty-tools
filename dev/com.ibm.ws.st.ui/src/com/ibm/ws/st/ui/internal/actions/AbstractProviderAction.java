/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;

public abstract class AbstractProviderAction extends SelectionProviderAction {
    protected Shell shell;

    public AbstractProviderAction(String text, Shell shell, ISelectionProvider selectionProvider) {
        super(selectionProvider, text);
        selectionChanged(getStructuredSelection());
        this.shell = shell;
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }

        Iterator<?> iter = sel.iterator();

        setEnabled(modifySelection(iter));
    }

    public abstract boolean modifySelection(Iterator<?> obj);
    
    public abstract boolean selectionChanged(Iterator<?> obj);
}