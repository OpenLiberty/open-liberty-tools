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
