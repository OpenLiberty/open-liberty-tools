/*
* IBM Confidential
*
* OCO Source Materials
*
* (C) Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;

public abstract class AbstractCreateRuntimeServerProjectMenuAction implements IObjectActionDelegate, ILibertyBuildPluginImplProvider {

    private ISelection selection;

    /** {@inheritDoc} */
    @Override
    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            for (Iterator<?> it = structuredSelection.iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = ((IAdaptable) element).getAdapter(IProject.class);
                }
                if (project != null) {
                    getBuildPluginImpl().triggerAddProject(project, null);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart workbenchPart) {
        // intentionally empty
    }

}
