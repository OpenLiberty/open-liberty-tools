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
package com.ibm.ws.st.core.internal.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereRuntimeProductInfoCacheUtil;
import com.ibm.ws.st.core.internal.config.FeatureList.FeatureMapType;

public class FeatureListExtMetadata extends AbstractFeatureListMetadata {
    public final String extensionName;

    private final HashMap<FeatureMapType, HashMap<String, Feature>> fallBackFeatureMaps = new HashMap<FeatureMapType, HashMap<String, Feature>>();

    private static HashMap<String, FeatureListExtMetadata[]> instancesMap = new HashMap<String, FeatureListExtMetadata[]>();

    private FeatureListExtMetadata(String extName) {
        super("featureList" + extName + ".xml");
        extensionName = extName;
    }

    /**
     * Get all instances of the product extension metadata classes for the given runtime.
     * There is one instance per product extension.
     * 
     * @param runtime the runtime
     */
    public static synchronized FeatureListExtMetadata[] getInstances(WebSphereRuntime runtime) {
        if (runtime == null)
            return new FeatureListExtMetadata[0];
        String runtimeID = runtime.getRuntime().getId();
        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Getting instances for runtime with id <" + runtimeID + "> and location <" + runtime.getRuntime().getLocation().toOSString() + ">");

        }
        // If we have already initialized the instances for this runtime we can return them.            
        if (instancesMap.containsKey(runtimeID)) {
            if (Trace.ENABLED)
                Trace.trace(Trace.DETAILS, "Found instances for runtimeID: " + runtimeID);
            return instancesMap.get(runtimeID);
        }

        List<String> productExtensionNames = WebSphereRuntimeProductInfoCacheUtil.getProductExtensionNames(runtime);
        if (productExtensionNames.size() == 0) {
            // No product extensions were found
            FeatureListExtMetadata[] ext = new FeatureListExtMetadata[0];
            // Add the empty entry to the instancesMap so we don't call external process to 
            // get productInfo each time since product extensions will only be added/removed
            // when the workbench restarts or when the user forces a refresh of the metadata.
            instancesMap.put(runtimeID, ext);
            return ext;
        }

        List<FeatureListExtMetadata> exts = new ArrayList<FeatureListExtMetadata>();
        for (String productExtensionName : productExtensionNames) {
            exts.add(new FeatureListExtMetadata(productExtensionName));
        }
        FeatureListExtMetadata[] instances = exts.toArray(new FeatureListExtMetadata[exts.size()]);
        instancesMap.put(runtimeID, instances);
        return instances;
    }

    @Override
    public synchronized HashMap<FeatureMapType, HashMap<String, Feature>> getFallbackPayload() {
        // no fallback for product extensions
        return fallBackFeatureMaps;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getCommandOptions() {
        return new String[] { "--productExtension=" + extensionName };
    }

    /**
     * Removes the instances from the instance map for a given runtime.
     * Generally this is used when regenerating the product info or when the runtime is removed.
     * 
     * @param runtimeId the runtime identifier
     */
    public static synchronized void clearRuntimeInstances(String runtimeId) {
        instancesMap.remove(runtimeId);
    }

    public String getFeatureListXMLName() {
        return target;
    }
}
