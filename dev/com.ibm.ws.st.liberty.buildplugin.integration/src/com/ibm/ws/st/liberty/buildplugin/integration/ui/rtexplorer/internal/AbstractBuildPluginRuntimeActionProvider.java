/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractGenerationAction;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractRefreshAction;

public abstract class AbstractBuildPluginRuntimeActionProvider extends CommonActionProvider {

    // Each build type implementation will contribute their own implementations of the following two actions.
    // If build type implementations want to contribute their own specific actions, they can simply override fillContextMenu
    protected AbstractGenerationAction generationAction = null;
    protected AbstractRefreshAction refreshAction = null;

    @Override
    public void fillContextMenu(IMenuManager menu) {
        // Get the extension id that contributed this action provider
        String extensionId = getActionSite().getExtensionId();
        ActionContext context = getContext();
        if (context != null) {
            ISelection selection = context.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                Object firstElement = structuredSelection.getFirstElement();
                if (firstElement instanceof LibertyBuildPluginProjectNode) {
                    LibertyBuildPluginProjectNode node = (LibertyBuildPluginProjectNode) firstElement;
                    String runtimeContentId = node.getRuntimeContentId();
                    // If the node id matches the extension id, then we have a match.  Show the menu actions
                    if (runtimeContentId.equals(extensionId)) {
                        if (generationAction.isEnabled())
                            menu.add(generationAction);
                        if (refreshAction.isEnabled())
                            menu.add(refreshAction);
                    }
                }
            }
        }
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        if (refreshAction.isEnabled())
            actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);
        actionBars.updateActionBars();
    }
}
