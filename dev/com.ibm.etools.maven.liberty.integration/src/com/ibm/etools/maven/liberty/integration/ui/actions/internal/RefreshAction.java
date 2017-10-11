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

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.actions.internal.AbstractRefreshAction;
import com.ibm.ws.st.liberty.buildplugin.integration.ui.rtexplorer.internal.LibertyBuildPluginProjectNode;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;

public class RefreshAction extends AbstractRefreshAction {

    public RefreshAction(ISelectionProvider selectionProvider, StructuredViewer viewer) {
        super(selectionProvider, viewer);
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof LibertyBuildPluginProjectNode) {
                objectToRefresh = obj;
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(true);
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

}
