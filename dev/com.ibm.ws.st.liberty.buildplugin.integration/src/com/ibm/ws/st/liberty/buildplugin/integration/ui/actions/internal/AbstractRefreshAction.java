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

package com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages;

public abstract class AbstractRefreshAction extends SelectionProviderAction implements ILibertyBuildPluginImplProvider {
    protected List<Object> objectsToRefresh;
    private final StructuredViewer viewer;

    public AbstractRefreshAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, Messages.refreshAction);
        setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
        setImageDescriptor(null);
        this.viewer = viewer;
        selectionChanged(getStructuredSelection());
    }

    /**
     * Extenders must determine the enablement state of the action given the selections
     * eg. if the selections are homogeneous (same object type) then the action can be enabled, but if mixed,
     * it should be disabled. Cannot allow mixed, otherwise there will be duplicate actions
     * in the context menu.
     *
     * @param selection
     */
    protected abstract void determineEnablementState(IStructuredSelection selection);

    @Override
    public void selectionChanged(IStructuredSelection selection) {
        if (objectsToRefresh == null) {
            objectsToRefresh = new ArrayList<Object>();
        } else {
            objectsToRefresh.clear();
        }
        determineEnablementState(selection);
    }

    @Override
    public void run() {
        if (objectsToRefresh == null || objectsToRefresh.isEmpty()) {
            viewer.refresh(); // no item selected so just refresh the view
            return;
        }

        for (Object object : objectsToRefresh) {
            viewer.refresh(object);
        }
    }
}
