/*
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 *
 */
public class GhostRuntimeProviderExtension {

    private static final String EXTENSION_POINT = "ghostRuntimeProvider";

    private static List<GhostRuntimeProvider> ghostRuntimeProviders = null;

    /**
     *
     */
    public GhostRuntimeProviderExtension() {
        // Empty
    }

    public static List<GhostRuntimeProvider> getGhostRuntimeProviders() {
        if (ghostRuntimeProviders != null) {
            return ghostRuntimeProviders;
        }

        ghostRuntimeProviders = loadGhostRuntimeProviderExtensions();
        return ghostRuntimeProviders;
    }

    private static List<GhostRuntimeProvider> loadGhostRuntimeProviderExtensions() {
        IConfigurationElement[] ghostRuntimeProviderExtensions = null;
        List<GhostRuntimeProvider> handlers = new ArrayList<GhostRuntimeProvider>();
        ghostRuntimeProviderExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor(Activator.PLUGIN_ID, EXTENSION_POINT);
        for (IConfigurationElement configurationElement : ghostRuntimeProviderExtensions) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "configurationElement class=[" + configurationElement.getAttribute("class") + "]");
            }
            Object o;
            try {
                o = configurationElement.createExecutableExtension("class");
                if (o instanceof GhostRuntimeProvider) {
                    handlers.add((GhostRuntimeProvider) o);
                }
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Cannot get the ghost runtime provider class ", e);
                }
            }
        }
        return handlers;
    }
}
