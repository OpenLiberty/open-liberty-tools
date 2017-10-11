/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractGenerationAction;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractRefreshAction;

public abstract class AbstractBuildPluginRuntimeActionProvider extends CommonActionProvider {

    protected AbstractGenerationAction generationAction = null;
    protected AbstractRefreshAction refreshAction = null;

    @Override
    public void fillContextMenu(IMenuManager menu) {
        if (generationAction.isEnabled())
            menu.add(generationAction);
        if (refreshAction.isEnabled())
            menu.add(refreshAction);

    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        if (refreshAction.isEnabled())
            actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);
        actionBars.updateActionBars();
    }
}
