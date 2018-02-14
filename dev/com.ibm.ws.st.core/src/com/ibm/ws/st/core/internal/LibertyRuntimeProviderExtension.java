/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 *
 */
public class LibertyRuntimeProviderExtension {

    private static final String EXTENSION_POINT = "libertyRuntimeProvider"; //$NON-NLS-1$

    private static List<LibertyRuntimeProvider> libertyRuntimeProviders = null;

    /**
     *
     */
    public LibertyRuntimeProviderExtension() {
        // Empty
    }

    public static List<LibertyRuntimeProvider> getLibertyRuntimeProviders() {
        if (libertyRuntimeProviders != null) {
            return libertyRuntimeProviders;
        }

        libertyRuntimeProviders = loadLibertyRuntimeProviderExtensions();
        return libertyRuntimeProviders;
    }

    private static List<LibertyRuntimeProvider> loadLibertyRuntimeProviderExtensions() {
        IConfigurationElement[] libertyRuntimeProviderExtensions = null;
        List<LibertyRuntimeProvider> handlers = new ArrayList<LibertyRuntimeProvider>();
        libertyRuntimeProviderExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor(Activator.PLUGIN_ID, EXTENSION_POINT);
        for (IConfigurationElement configurationElement : libertyRuntimeProviderExtensions) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "configurationElement class=[" + configurationElement.getAttribute("class") + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            Object o;
            try {
                o = configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
                if (o instanceof LibertyRuntimeProvider) {
                    handlers.add((LibertyRuntimeProvider) o);
                }
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Cannot get the Liberty Runtime Provider class ", e); //$NON-NLS-1$
                }
            }
        }
        return handlers;
    }
}
