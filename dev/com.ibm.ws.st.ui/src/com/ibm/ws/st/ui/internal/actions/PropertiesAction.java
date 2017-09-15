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
package com.ibm.ws.st.ui.internal.actions;

import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.RuntimeExplorer;

/**
 * Action to show the property page for a runtime.
 */
public class PropertiesAction extends SelectionProviderAction {
    protected String propertyPageId;
    protected Shell shell;
    protected IRuntime runtime;

    public PropertiesAction(ISelectionProvider selectionProvider, Shell shell) {
        this(shell, null, selectionProvider);
    }

    public PropertiesAction(Shell shell, String propertyPageId, ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionProperties);
        this.shell = shell;
        this.propertyPageId = propertyPageId;
        if (propertyPageId == null)
            setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);

        try {
            selectionChanged((IStructuredSelection) selectionProvider.getSelection());
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IRuntime) {
                runtime = (IRuntime) obj;
            } else if (obj instanceof RuntimeExplorer.Node) {
                RuntimeExplorer.Node node = (RuntimeExplorer.Node) obj;
                runtime = node.getRuntime();
            } else if (obj instanceof UserDirectory) {
                UserDirectory userDir = (UserDirectory) obj;
                runtime = userDir.getWebSphereRuntime().getRuntime();
            } else if (obj instanceof WebSphereServerInfo) {
                WebSphereServerInfo server = (WebSphereServerInfo) obj;
                runtime = server.getWebSphereRuntime().getRuntime();
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(true);
    }

    @Override
    public void run() {
        if (propertyPageId == null)
            propertyPageId = "com.ibm.ws.st.ui.properties.support";
        Dialog dialog = PreferencesUtil.createPropertyDialogOn(shell, runtime, propertyPageId, null, null);
        if (dialog != null)
            dialog.open();
    }
}
