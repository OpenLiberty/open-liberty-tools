/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.ui.ext.internal;

import java.util.Hashtable;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

    public static final String PLUGIN_ID = "com.ibm.ws.st.common.ui.ext";

    private static Activator instance;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        instance = this;
        Trace.ENABLED = isDebugging();

        // register and initialize the tracing class
        final Hashtable<String, String> props = new Hashtable<String, String>(4);
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, Activator.PLUGIN_ID);
        bundleContext.registerService(DebugOptionsListener.class.getName(), Trace.TS, props);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        instance = null;
        super.stop(bundleContext);
    }

    /**
     * Get the shared instance
     */
    public static Activator getInstance() {
        return instance;
    }

    public static String getPreference(String key, String defaultValue) {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(key, defaultValue);
    }

    public static void setPreference(String key, String value) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
            if (value == null)
                prefs.remove(key);
            else
                prefs.put(key, value);
            prefs.flush();
        } catch (Exception e) {
            Trace.logError("Error setting preference " + key, e);
        }
    }
}
