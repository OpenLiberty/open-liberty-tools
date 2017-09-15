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
package com.ibm.ws.st.core.internal;

import java.net.URL;

import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.IURLProvider2;
import org.eclipse.wst.server.core.util.HttpLaunchable;
import org.eclipse.wst.server.core.util.WebResource;

/**
 *
 */
public class WebLaunchable extends HttpLaunchable {

    public WebLaunchable(final IServer server, final IModuleArtifact moduleObject) {
        super(new IURLProvider2() {
            @Override
            public URL getModuleRootURL(IModule module) {
                IURLProvider urlProvider = (IURLProvider) server.loadAdapter(IURLProvider.class, null);
                return urlProvider.getModuleRootURL(module);
            }

            @Override
            public URL getLaunchableURL() {
                try {
                    URL url = getModuleRootURL(moduleObject.getModule());

                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Root URL: " + url);

                    if (url == null)
                        return null;

                    if (moduleObject instanceof Servlet) {
                        Servlet servlet = (Servlet) moduleObject;
                        if (servlet.getAlias() != null) {
                            String path = servlet.getAlias();
                            if (path.startsWith("/"))
                                path = path.substring(1);
                            url = new URL(url, path);
                        } else
                            url = new URL(url, "servlet/" + servlet.getServletClassName());
                    } else if (moduleObject instanceof WebResource) {
                        WebResource resource = (WebResource) moduleObject;
                        String path = resource.getPath().toString();
                        if (Trace.ENABLED)
                            Trace.trace(Trace.INFO, "path: " + path);
                        if (path != null && path.startsWith("/") && path.length() > 0)
                            path = path.substring(1);
                        if (path != null && path.length() > 0)
                            url = new URL(url, path);
                    }
                    return url;
                } catch (Exception e) {
                    Trace.logError("Error getting URL for " + moduleObject, e);
                    return null;
                }
            }
        });
    }
}
