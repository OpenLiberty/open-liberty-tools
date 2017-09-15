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
package com.ibm.ws.st.jee.core.internal;

import java.util.Hashtable;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the bundles's life cycle.
 */
public class Activator extends Plugin {
    // the bundle id
    public static final String PLUGIN_ID = "com.ibm.ws.st.jee.core";

    // the shared instance
    private static Activator instance;

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
        SharedLibResourceListener.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        SharedLibResourceListener.stop();
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
}
