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
package com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages;

public abstract class AbstractRefreshAction extends SelectionProviderAction implements ILibertyBuildPluginImplProvider {
    protected Object objectToRefresh;
    private final StructuredViewer viewer;

    public AbstractRefreshAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, Messages.refreshAction);
        setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
        setImageDescriptor(null);
        this.viewer = viewer;
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void run() {
        if (objectToRefresh == null) {
            viewer.refresh(); // no item selected so just refresh the view
            return;
        }

        viewer.refresh(objectToRefresh);
    }
}
