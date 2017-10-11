/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.etools.maven.liberty.integration.internal;

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
    public static final String PLUGIN_ID = "com.ibm.etools.maven.liberty.integration"; //$NON-NLS-1$

    // The shared instance
    private static Activator instance;

    protected Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();

    // base url for icons
    private static URL ICON_BASE_URL;
    private static final String URL_OBJ16 = "obj16/";

    // image keys
    public static final String IMG_MAVEN_RUNTIME = "mavenRuntime";

    public static final String IMG_MAVEN_FOLDER = "mavenFolder";

    /**
     * The constructor
     */
    public Activator() {}

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        Trace.ENABLED = isDebugging();

        // register and initialize the tracing class
        final Hashtable<String, String> props = new Hashtable<String, String>(4);
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, Activator.PLUGIN_ID);
        context.registerService(DebugOptionsListener.class.getName(), Trace.TS, props);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
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
            ICON_BASE_URL = instance.getBundle().getEntry("icons/");

        registerImage(registry, IMG_MAVEN_FOLDER, ICON_BASE_URL + URL_OBJ16 + "/maven_folder.png");
        registerImage(registry, IMG_MAVEN_RUNTIME, ICON_BASE_URL + URL_OBJ16 + "/liberty-runtime-maven-16.png");

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

    public static IPath getLibertyMavenMetadataPath() {
        IPath path = getInstance().getStateLocation().append("LibertyMaven");
        try {
            path.toFile().mkdirs();
        } catch (Exception e) {
            Trace.logError("Failed to create LibertyMaven metadata directory: " + path.toOSString(), e);
        }
        return path;
    }
}
