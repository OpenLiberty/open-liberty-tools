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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

import com.ibm.ws.st.ui.internal.Messages;

public class WebSphereActionProvider extends CommonActionProvider {
    protected OpenConfigFileAction openAction;
    protected OpenMergedConfigAction openMergedAction;
    protected OpenSchemaBrowserAction openSchemaBrowserAction;
    protected ShowInExplorerAction showInExplorerAction;
    protected ShowInFilesystemAction showInFilesystemAction;
    protected RefreshConfigFileAction refreshConfigAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        ISelectionProvider selectionProvider = aSite.getStructuredViewer();
        openAction = new OpenConfigFileAction(selectionProvider);
        openMergedAction = new OpenMergedConfigAction(selectionProvider);
        openSchemaBrowserAction = new OpenSchemaBrowserAction(selectionProvider);
        showInExplorerAction = new ShowInExplorerAction(selectionProvider);
        showInFilesystemAction = new ShowInFilesystemAction(selectionProvider);
        refreshConfigAction = new RefreshConfigFileAction(selectionProvider);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        menu.add(openAction);
        if (openMergedAction.isEnabled())
            menu.add(openMergedAction);
        if (openSchemaBrowserAction.isEnabled())
            menu.add(openSchemaBrowserAction);

        if (showInExplorerAction.isEnabled()) {
            String text = Messages.actionShowIn;
            final IWorkbench workbench = PlatformUI.getWorkbench();
            final IBindingService bindingService = (IBindingService) workbench
                                .getAdapter(IBindingService.class);
            final TriggerSequence[] activeBindings = bindingService
                                .getActiveBindingsFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
            if (activeBindings.length > 0)
                text += "\t" + activeBindings[0].format();

            MenuManager showInMenu = new MenuManager(text, IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
            showInMenu.add(showInExplorerAction);
            showInMenu.add(showInFilesystemAction);
            menu.appendToGroup(ICommonMenuConstants.GROUP_SHOW, showInMenu);
        }

        if (refreshConfigAction.isEnabled())
            menu.add(refreshConfigAction);
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        actionBars.setGlobalActionHandler("org.eclipse.ui.navigator.Open", openAction);
        actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshConfigAction);
        actionBars.updateActionBars();
    }
}
