/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;

import com.ibm.ws.st.common.core.ext.internal.producer.AbstractServerProducer;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandlerProvider;

public class Activator extends Plugin {

    public static final String PLUGIN_ID = "com.ibm.ws.st.common.core.ext";

    public static final String WTP_SERVER_PRODUCER_EXTENSION_POINT = "WTPServerProducer";
    public static final String SERVER_SETUP_EXTENSION_POINT = "RemoteServerSetup";
    public static final String PLATFORM_HANDLER_PROVIDER_EXTENSION_POINT = "platformHandlerProvider";

    public static final String ATTR_RUNTIME_TYPE = "runtimeType";
    public static final String ATTR_SERVICE_TYPE = "serviceType";
    public static final String ATTR_ORDER = "order";

    private static Activator instance;

    private static Map<String, IPlatformHandlerProvider> platformProviders = new HashMap<String, IPlatformHandlerProvider>();

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
        initPlatformProviders();
    }

    private void initPlatformProviders() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, PLATFORM_HANDLER_PROVIDER_EXTENSION_POINT);

        for (IConfigurationElement ce : cf) {
            String type = ce.getAttribute("type");
            try {
                IPlatformHandlerProvider provider = (IPlatformHandlerProvider) ce.createExecutableExtension("class");
                platformProviders.put(type, provider);
            } catch (CoreException e) {
                Trace.logError("Error while creating executable extension for platform provider type: " + type, e);
            }
        }
    }

    public static IPlatformHandlerProvider getPlatformProvider(String type) {
        return platformProviders.get(type);
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

    public static AbstractServerSetup getServerSetup(String serviceType) {
        AbstractServerSetup serverSetup = null;
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, SERVER_SETUP_EXTENSION_POINT);

        IConfigurationElement bestMatch = null;

        for (IConfigurationElement ce : cf) {
            if (ce.getAttribute(ATTR_SERVICE_TYPE).equals(serviceType)) {
                if (bestMatch != null) {
                    int currentOrder = Integer.parseInt(bestMatch.getAttribute(ATTR_ORDER));
                    int tempOrder = Integer.parseInt(ce.getAttribute(ATTR_ORDER));
                    if (tempOrder < currentOrder)
                        bestMatch = ce;
                } else {
                    bestMatch = ce;
                }
            }
        }

        if (bestMatch != null) {
            try {
                serverSetup = (AbstractServerSetup) bestMatch.createExecutableExtension("class");
            } catch (CoreException e) {
                Trace.logError("Error while creating executable extension for server setup", e);
            }
        }

        return serverSetup;
    }

    public static AbstractServerProducer getServerProducer(String runtimeType) {
        AbstractServerProducer serverProducer = null;
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, WTP_SERVER_PRODUCER_EXTENSION_POINT);

        IConfigurationElement bestMatch = null;

        for (IConfigurationElement ce : cf) {
            if (ce.getAttribute(ATTR_RUNTIME_TYPE).equals(runtimeType)) {
                if (bestMatch != null) {
                    int currentOrder = Integer.parseInt(bestMatch.getAttribute(ATTR_ORDER));
                    int tempOrder = Integer.parseInt(ce.getAttribute(ATTR_ORDER));
                    if (tempOrder < currentOrder)
                        bestMatch = ce;
                } else {
                    bestMatch = ce;
                }
            }
        }

        if (bestMatch != null) {
            try {
                serverProducer = (AbstractServerProducer) bestMatch.createExecutableExtension("class");
            } catch (CoreException e) {
                Trace.logError("Error while creating executable extension for server producer", e);
            }
        }

        return serverProducer;
    }

}
