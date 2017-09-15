/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.core.internal;

import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
    // the bundle id
    public static final String PLUGIN_ID = "com.ibm.ws.st.docker.core";

    // the shared instance
    private static Activator instance;

    private static AbstractServerCleanupHandler serverCleanupHandler;
    private static AbstractModeSwitchHandler modeSwitchHandler;
    private static AbstractFlattenImageHandler flattenImageHandler;

    public Activator() {
        // do nothing
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        Trace.ENABLED = isDebugging();

        // register and initialize the tracing class
        final Hashtable<String, String> props = new Hashtable<String, String>(4);
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);
        context.registerService(DebugOptionsListener.class.getName(), Trace.TS, props);
        initHandlers();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getInstance() {
        return instance;
    }

    protected static void initHandlers() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.docker.core.libertyDockerHandlers");
        if (configElements.length > 0) {
            // Should only be one so just pick the first one
            IConfigurationElement elem = configElements[0];
            try {
                serverCleanupHandler = (AbstractServerCleanupHandler) elem.createExecutableExtension("serverCleanupHandlerClass");
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to create the server cleanup handler.", e);
                }
            }
            try {
                modeSwitchHandler = (AbstractModeSwitchHandler) elem.createExecutableExtension("modeSwitchHandlerClass");
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to create the mode switch handler.", e);
                }
            }
            try {
                flattenImageHandler = (AbstractFlattenImageHandler) elem.createExecutableExtension("flattenImageHandlerClass");
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to create the flatten image handler.", e);
                }
            }

        }
    }

    /**
     * Get the server cleanup handler
     */
    public static AbstractServerCleanupHandler getServerCleanupHandler() {
        return serverCleanupHandler;
    }

    /**
     * Get the mode switch handler
     */
    public static AbstractModeSwitchHandler getModeSwitchHandler() {
        return modeSwitchHandler;
    }

    /**
     * Get the flatten image handler
     */
    public static AbstractFlattenImageHandler getFlattenImageHandler() {
        return flattenImageHandler;
    }
}
