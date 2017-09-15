/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;

/**
 *
 */
public class WebSphereRuntimeProductInfoCacheUtil {
    public static final String PRODUCT_EXTENSION_TITLE = "Product Extension:";
    public static final String FEATURE_INFO = "featureInfo";

    // Important: This is used while parsing product extensions from featureInfo. If this is changed
    // the parsing logic may need to be changed as well.
    public static final String PRODUCT_INFO_LINE_SEPARATOR = " >$< ";

    private static final String VERSION = "version";
    private static final String CACHE_FILE_NAME = "productInfoKey.properties";

    protected static String getProductVersionInfo(WebSphereRuntime runtime) {
        return executeProductInfo(runtime, VERSION);
    }

    protected static String getProductFeatureInfo(WebSphereRuntime runtime) {
        return executeProductInfo(runtime, FEATURE_INFO);
    }

    protected static Properties getCombinedProductKey(final WebSphereRuntime runtime) {
        final Properties prop = new Properties();

        Thread versionThread = new Thread("Product version") {
            @Override
            public void run() {
                String s = getProductVersionInfo(runtime);
                if (s != null)
                    prop.put(VERSION, s);
            }
        };

        Thread featureInfoThread = new Thread("Product feature info") {
            @Override
            public void run() {
                String s = getProductFeatureInfo(runtime);
                if (s != null)
                    prop.put(FEATURE_INFO, s);
            }
        };
        versionThread.start();
        featureInfoThread.start();
        try {
            versionThread.join();
        } catch (InterruptedException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Product version thread was interrupted", e);
        }
        try {
            featureInfoThread.join();
        } catch (InterruptedException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Product feature info thread was interrupted", e);
        }
        return prop;
    }

    /**
     * Saves the runtime production information to the file system.
     *
     * @param runtime the runtime
     * @param productInfo the production information. If it is null, it will be generated
     */
    public static void saveProductInfoCache(WebSphereRuntime runtime, final Properties productInfo) {
        if (runtime == null)
            return;

        Properties curProp;
        if (productInfo == null)
            curProp = getCombinedProductKey(runtime);
        else
            curProp = productInfo;

        IPath path = buildMetadataDirectoryPath(runtime).append(CACHE_FILE_NAME);
        FileUtil.saveCachedProperties(curProp, path);
    }

    /**
     * Deletes the cached product information of the runtime
     *
     * @param runtime the runtime
     * @return false if the cache cannot be deleted. true otherwise
     */
    public static boolean deleteProductInfoCache(WebSphereRuntime runtime) {
        if (runtime == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "The runtime is null.");
            return true;
        }

        IPath path = buildMetadataDirectoryPath(runtime).append(CACHE_FILE_NAME);
        File file = path.toFile();
        if (file.exists() && !file.delete()) {
            if (Trace.ENABLED)
                Trace.logError("Failed to delete cache file: " + path.toOSString(), null);
            return false;
        }
        return true;
    }

    protected static Properties loadCachedProductKey(WebSphereRuntime runtime) {
        Properties prop = new Properties();
        if (runtime != null) {
            IPath path = buildMetadataDirectoryPath(runtime);
            FileUtil.loadProperties(prop, path.append(CACHE_FILE_NAME));
        } else {
            if (Trace.ENABLED)
                Trace.logError("The runtime is null.", null);
        }
        return prop;
    }

    /**
     * Returns the product information if it is different from what is in the cache.
     *
     * @param runtime the runtime
     * @return the current product information if it is different from what is in the cache; null otherwise
     */
    public static Properties getChangedProductInfo(WebSphereRuntime runtime) {
        /*
         * Querying the runtime product information is expensive. It takes about 1.5 seconds to complete.
         * A call to saveProductInfoCache(WebSphereRuntime, Properties) is often needed after a call
         * to this method. The saveProductInfoCache(WebSphereRuntime, Properties) will query the information again
         * if it is not passed in.
         */
        if (runtime == null)
            return null;

        long startTime = 0; // set to zero for compilation warning.
        if (Trace.ENABLED)
            startTime = System.currentTimeMillis();
        Properties cached = loadCachedProductKey(runtime);
        Properties current = getCombinedProductKey(runtime);

        if (Trace.ENABLED)
            Trace.tracePerf("Time to determine if productKey cache need to be updated", startTime);
        if (current.equals(cached))
            return null;
        return current;
    }

    private static String executeProductInfo(WebSphereRuntime runtime, String cmd) {
        if (runtime.getRuntime() == null || runtime.getRuntime().getLocation() == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Runtime location is null. " + runtime.getRuntime().getName());
            return null;
        }

        try {
            String productInfo = runtime.getProductInfo(cmd, null);
            if (productInfo == null) // not supported on 8.5.0.0
                return null;
            return productInfo.replace("\n", PRODUCT_INFO_LINE_SEPARATOR);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Exception occured when executing command " + cmd, e);
        }
        return null;
    }

    private static IPath buildMetadataDirectoryPath(WebSphereRuntime runtime) {
        return Activator.getInstance().getStateLocation().append(runtime.getRuntime().getId());
    }

    public static List<String> getProductExtensionNames(WebSphereRuntime runtime) {
        List<String> productExtensionNames = new ArrayList<String>(3);
        Properties productInfo = getCombinedProductKey(runtime);

        String featureInfo = (String) productInfo.get(FEATURE_INFO);

        // If featureInfo is null that means something went wrong, so return the empty list.
        // For V8.5GA runtimes this is expected since the productInfo tool is not available.
        if (featureInfo == null) {
            // runtime version can be null if the runtime's installation location no longer exists
            String runtimeVersion = runtime.getRuntimeVersion();
            if (runtimeVersion == null || !runtimeVersion.startsWith("8.5.0")) {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                Trace.logError("featureInfo is null, this could be because the product info generation failed.", t);
            }
            return productExtensionNames;
        }

        // Parse the string to find the product extension names.
        int index = featureInfo.indexOf(PRODUCT_EXTENSION_TITLE);
        if (index < 0)
            return productExtensionNames;

        featureInfo = featureInfo.substring(index);
        String[] featureInfos = featureInfo.split(PRODUCT_INFO_LINE_SEPARATOR.replace("$", "\\$"));

        // If "Product Extension" appears more than once in the string
        // then something went wrong.
        if (featureInfos[0].lastIndexOf(PRODUCT_EXTENSION_TITLE) != 0) {
            return productExtensionNames;
        }

        for (String f : featureInfos) {
            // Parse the product extensions and add them to the list.
            if (f.contains(PRODUCT_EXTENSION_TITLE)) {
                int subIndex = f.indexOf(':') + 1;
                if (subIndex < f.length())
                    productExtensionNames.add(f.substring(subIndex).trim());
            }
        }
        return productExtensionNames;
    }
}