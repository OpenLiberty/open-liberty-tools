/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.util.WebResource;

/**
 * Launchable adapter delegate for Web resources.
 */
public class WebSphereLaunchableAdapterDelegate extends LaunchableAdapterDelegate {
    @Override
    public Object getLaunchable(IServer server, IModuleArtifact moduleObject) {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "LaunchableAdapter " + server + "-" + moduleObject);
        if (server.getAdapter(WebSphereServer.class) == null)
            return null;
        if (!(moduleObject instanceof Servlet) &&
            !(moduleObject instanceof WebResource))
            return null;
        return new WebLaunchable(server, moduleObject);
    }

}