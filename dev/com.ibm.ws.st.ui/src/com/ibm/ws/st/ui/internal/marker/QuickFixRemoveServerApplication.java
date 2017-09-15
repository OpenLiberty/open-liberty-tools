/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.marker;

import java.net.URI;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for out of sync published applications
 * 
 * Remove application from server
 */
public class QuickFixRemoveServerApplication extends AbstractMarkerResolution {
    private final String appName;

    public QuickFixRemoveServerApplication(String appName) {
        this.appName = appName;
    }

    @Override
    public String getLabel() {
        return NLS.bind(Messages.removeAppFromServerLabel, appName);
    }

    @Override
    public void run(IMarker marker) {
        final IResource resource = getResource(marker);
        if (resource == null)
            return;

        // Get the application module
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(appName);
        final IModule module = (project != null) ? ServerUtil.getModule(project) : null;
        if (module == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix for remove server app failed. Could not locate application module: " + appName, null);
            showErrorMessage();
            return;
        }

        final URI uri = resource.getLocation().toFile().toURI();
        final WebSphereServer[] servers = WebSphereUtil.getWebSphereServers();
        for (WebSphereServer server : servers) {
            ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
            if (configFile != null) {
                IServerWorkingCopy wc;
                if (server.getServer().isWorkingCopy()) {
                    wc = (IServerWorkingCopy) server.getServer();
                } else {
                    wc = server.getServer().createWorkingCopy();
                }
                try {
                    // Remove the application from the server

                    // modifyModules will update the configuration file
                    // which would cause a config sync on WebsphereServerBehaviour
                    // (the sync will update the publish state of the server)
                    wc.modifyModules(null, new IModule[] { module }, null);
                    wc.save(true, null);
                } catch (CoreException ce) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.ERROR,
                                    "Quick fix for remove server app failed. Error trying to remove application '" + appName + "' from server: " + server.getServerName(), ce);
                    }
                    showErrorMessage();
                }
                return;
            }
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.ERROR, "Quick fix for remove server app failed. Could not locate configuration file: " + uri, null);
        showErrorMessage();
    }

    @Override
    protected String getErrorMessage() {
        return Messages.removeAppFromServerFailedMessage;
    }
}
