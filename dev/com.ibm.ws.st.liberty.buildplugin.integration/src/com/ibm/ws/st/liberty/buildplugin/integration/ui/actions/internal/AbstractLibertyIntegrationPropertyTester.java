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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImplProvider;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.Trace;

public abstract class AbstractLibertyIntegrationPropertyTester extends PropertyTester implements ILibertyBuildPluginImplProvider {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (getSupportedPropertyValue().equals(property)) {
            IProject projectAdapter = null;
            try {
                IAdaptable adaptable = (IAdaptable) receiver;

                projectAdapter = adaptable.getAdapter(IProject.class);
                if (projectAdapter != null) {
                    return getBuildPluginImpl().isSupportedProject(projectAdapter, new NullProgressMonitor())
                           && !getBuildPluginImpl().getMappingHandler().getMappedProjectSet().contains(projectAdapter.getName());
                }
            } catch (Exception e) {
                Trace.logError("Error encountered while checking create server action on project " + projectAdapter, e);
            }
            return false;
        }
        return false;
    }

    protected abstract String getSupportedPropertyValue();

}
