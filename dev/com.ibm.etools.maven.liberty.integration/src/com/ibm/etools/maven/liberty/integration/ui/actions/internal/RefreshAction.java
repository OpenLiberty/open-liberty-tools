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

package com.ibm.etools.maven.liberty.integration.ui.actions.internal;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.etools.maven.liberty.integration.ui.rtexplorer.internal.MavenRuntimeProjectNode;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractRefreshAction;

public class RefreshAction extends AbstractRefreshAction {

    public RefreshAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, viewer);
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    protected void determineEnablementState(IStructuredSelection selection) {
        for (Object obj : selection.toList()) {
            if (obj instanceof MavenRuntimeProjectNode) {
                objectsToRefresh.add(((MavenRuntimeProjectNode) obj).getProject());
            } else { // If multiple selection includes other objects, then disable this action
                setEnabled(false);
                return;
            }
        }
        setEnabled(!objectsToRefresh.isEmpty());
    }

}
