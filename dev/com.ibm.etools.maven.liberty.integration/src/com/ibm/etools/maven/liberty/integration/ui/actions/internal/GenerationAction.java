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
package com.ibm.etools.maven.liberty.integration.ui.actions.internal;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;

import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractGenerationAction;

public class GenerationAction extends AbstractGenerationAction {

    /**
     * @param selectionProvider
     * @param viewer
     */
    public GenerationAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, viewer);
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

}
