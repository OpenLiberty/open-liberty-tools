/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;

/**
 * The WebSphere runtime explorer view.
 */
public class RuntimeExplorerView extends CommonNavigator {
    public static final String VIEW_ID = "com.ibm.ws.st.ui.runtime.view";

    @Override
    protected CommonViewer createCommonViewerObject(Composite aParent) {
        CommonViewer viewer = super.createCommonViewerObject(aParent);
        viewer.setAutoExpandLevel(2);
        return viewer;
    }

    public void select(WebSphereServer wsServer) {
        // runtime can be null when runtime is deleted but the server is not and
        // the user selects Show in -> Runtime Explorer action.
        if (wsServer == null || wsServer.getWebSphereRuntime() == null)
            return;

        WebSphereServerInfo serverInfo = wsServer.getServerInfo();
        if (serverInfo == null)
            return;

        getCommonViewer().expandToLevel(serverInfo, 1);
        selectReveal(new StructuredSelection(serverInfo));
    }
}
