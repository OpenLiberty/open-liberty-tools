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
package com.ibm.ws.st.ui.internal;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import com.ibm.ws.st.ui.internal.actions.OpenConfigFileAction;
import com.ibm.ws.st.ui.internal.actions.OpenMergedConfigAction;
import com.ibm.ws.st.ui.internal.actions.OpenSchemaBrowserAction;

public class ConfigurationResourceActionProvider extends CommonActionProvider {
    protected OpenConfigFileAction openAction;
    protected OpenMergedConfigAction openMergedAction;
    protected OpenSchemaBrowserAction openSchemaBrowserAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        ISelectionProvider selectionProvider = aSite.getStructuredViewer();
        openAction = new OpenConfigFileAction(selectionProvider);
        openMergedAction = new OpenMergedConfigAction(selectionProvider);
        openSchemaBrowserAction = new OpenSchemaBrowserAction(selectionProvider);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        menu.add(openAction);
        menu.add(openMergedAction);
        menu.add(openSchemaBrowserAction);
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        actionBars.setGlobalActionHandler("org.eclipse.ui.navigator.Open", openAction);
        actionBars.updateActionBars();
    }
}
