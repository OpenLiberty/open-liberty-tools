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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Messages;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.LibertyBuildPluginProjectNode;

public abstract class AbstractGenerationAction extends SelectionProviderAction implements ILibertyBuildPluginImplProvider {
    private IProject project = null;

    public AbstractGenerationAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, Messages.createServerAction);
        setImageDescriptor(null);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }

        Object obj = sel.getFirstElement();
        if (obj instanceof LibertyBuildPluginProjectNode) {
            project = ((LibertyBuildPluginProjectNode) obj).getProject();
        } else {
            setEnabled(false);
            return;
        }

        setEnabled(true);
    }

    @Override
    public void run() {
        if (project == null)
            return;
        getBuildPluginImpl().triggerAddRuntimeAndServer(project, null);
    }
}
