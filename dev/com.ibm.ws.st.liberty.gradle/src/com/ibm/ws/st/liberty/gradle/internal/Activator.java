/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.gradle.internal;

import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.ws.st.liberty.gradle"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
    protected Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();

    // base url for icons
    private static URL ICON_BASE_URL;
    private static final String URL_OBJ16 = "obj16/";

    // image keys
    public static final String IMG_GRADLE_RUNTIME = "gradleRuntime";

    public static final String IMG_GRADLE_FOLDER = "gradleFolder";

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
        Trace.ENABLED = isDebugging();

        // register and initialize the tracing class
        final Hashtable<String, String> props = new Hashtable<String, String>(4);
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, Activator.PLUGIN_ID);
        context.registerService(DebugOptionsListener.class.getName(), Trace.TS, props);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static Activator getInstance() {
		return plugin;
	}
	
    /**
     * Register an image with the registry.
     *
     * @param key java.lang.String
     * @param partialURL java.lang.String
     */
    private void registerImage(ImageRegistry registry, String key, String partialURL) {
        try {
            ImageDescriptor id = ImageDescriptor.createFromURL(new URL(ICON_BASE_URL, partialURL));
            registry.put(key, id);
            imageDescriptors.put(key, id);
        } catch (Exception e) {
            Trace.trace(Trace.WARNING, "Error registering image", e);
        }
    }

    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry registry = new ImageRegistry();
        if (ICON_BASE_URL == null)
            ICON_BASE_URL = plugin.getBundle().getEntry("icons/");

        // TODO - change these when we get new gradle images/icons
        registerImage(registry, IMG_GRADLE_FOLDER, ICON_BASE_URL + URL_OBJ16 + "/gradle-folder.png");
        registerImage(registry, IMG_GRADLE_RUNTIME, ICON_BASE_URL + URL_OBJ16 + "/liberty-runtime-gradle.png");

        return registry;
    }

    /**
     * Return the image with the given key from the image registry.
     *
     * @param key
     * @return Image
     */
    public static Image getImage(String key) {
        return getInstance().getImageRegistry().get(key);
    }

    /**
     * Return the image with the given key from the image registry.
     *
     * @param key
     * @return ImageDescriptor
     */
    public static ImageDescriptor getImageDescriptor(String key) {
        try {
            getInstance().getImageRegistry();
            return getInstance().imageDescriptors.get(key);
        } catch (Exception e) {
            Trace.trace(Trace.WARNING, "Missing image", e);
            return ImageDescriptor.getMissingImageDescriptor();
        }
    }

    public static IPath getLibertyGradleMetadataPath() {
        IPath path = getInstance().getStateLocation().append("LibertyGradle");
        try {
            path.toFile().mkdirs();
        } catch (Exception e) {
            Trace.logError("Failed to create LibertyGradle metadata directory: " + path.toOSString(), e);
        }
        return path;
    }

}
