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

package com.ibm.ws.st.liberty.gradle.ui.actions.internal;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractRefreshAction;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradle;
import com.ibm.ws.st.liberty.gradle.ui.rtexplorer.internal.GradleRuntimeProjectNode;

public class RefreshAction extends AbstractRefreshAction {

    public RefreshAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, viewer);
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyGradle.getInstance();
    }

	@Override
	protected void determineEnablementState(IStructuredSelection selection) {
		for (Object obj : selection.toList()) {
			if (obj instanceof GradleRuntimeProjectNode) {
				objectsToRefresh.add(((GradleRuntimeProjectNode) obj).getProject());
			} else { // If multiple selection includes other objects, then disable this action
				setEnabled(false);
				return;
			}
		}
		setEnabled(!objectsToRefresh.isEmpty());
	}

}
