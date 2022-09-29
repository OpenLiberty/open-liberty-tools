/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishControllerDelegate;

public class WASPublishControllerDelegate extends PublishControllerDelegate {

    /** {@inheritDoc} */
    @Override
    public boolean isPublishRequired(IServer server, IResourceDelta delta) {
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        if (wsServer == null)
            return true;
        // 0 = unknown whether this value is used but if it is then treat it as disabled 
        // 1 = autopublishing disabled
        // 2 = autopublishing enabled
        int autoPublishSettings = server.getAttribute("auto-publish-setting", 1);
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "WASPublishControllerDelegate.isPublishRequired autoPublishSettings=" + autoPublishSettings);
        }
        if (autoPublishSettings < 2) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "WASPublishControllerDelegate returning false as autopublish is disabled" + autoPublishSettings);
            }
            return false;
        }
        WebSphereServerBehaviour behaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        if (behaviour == null) // We should be able to load it
            return true;

        List<IModule[]> fullModules = behaviour.getPublishedModules();
        if (fullModules.isEmpty())
            return false;

        IProject project = delta.getResource().getProject();
        if (project == null)
            return false;

        ArrayList<IModule> modules = new ArrayList<IModule>();
        for (IModule[] fm : fullModules) {
            if (project.equals(fm[fm.length - 1].getProject()))
                modules.add(fm[fm.length - 1]);
        }

        if (modules.isEmpty())
            return false;

        IModule[] mods = modules.toArray(new IModule[modules.size()]);

        ServerExtensionWrapper[] serverExtensions = wsServer.getServerExtensions();
        for (ServerExtensionWrapper se : serverExtensions) {
            for (IModule m : modules) {
                if (se.supports(m.getModuleType())) {
                    se.initServer(wsServer);
                    boolean publishRequired = se.isPublishRequired(mods, delta);
                    if (publishRequired)
                        return true;
                    break; // The extension will check all the modules. We can break here.
                }
            }
        }
        return false;
    }
}