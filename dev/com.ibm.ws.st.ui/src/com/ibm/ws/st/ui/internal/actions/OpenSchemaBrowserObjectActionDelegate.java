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

import java.net.URI;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.ws.st.ui.internal.Activator;

public class OpenSchemaBrowserObjectActionDelegate implements IObjectActionDelegate {
    private URI uri;

    public OpenSchemaBrowserObjectActionDelegate() {
        super();
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // Do nothing
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            action.setEnabled(false);
            return;
        }
        boolean enabled = false;
        IStructuredSelection sel = (IStructuredSelection) selection;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IFile) {
                uri = ((IFile) obj).getLocation().toFile().toURI();
                enabled = true;
            } else {
                action.setEnabled(false);
                return;
            }
        }
        action.setEnabled(enabled);
    }

    @Override
    public void run(IAction action) {
        if (uri == null)
            return;

        Activator.openSchemaBrowser(uri);
    }
}
