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
package com.ibm.etools.maven.liberty.integration.ui.rtexplorer.internal;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.AbstractBuildPluginRuntimeActionProvider;
import com.ibm.etools.maven.liberty.integration.ui.actions.internal.GenerationAction;
import com.ibm.etools.maven.liberty.integration.ui.actions.internal.RefreshAction;

public class MavenRuntimeActionProvider extends AbstractBuildPluginRuntimeActionProvider {

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);

        ISelectionProvider selProvider = aSite.getStructuredViewer();
        StructuredViewer viewer = aSite.getStructuredViewer();

        refreshAction = new RefreshAction(selProvider, viewer);

        generationAction = new GenerationAction(selProvider, viewer);

    }
}
