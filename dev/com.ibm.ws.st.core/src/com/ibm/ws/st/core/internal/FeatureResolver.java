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
package com.ibm.ws.st.core.internal;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

import com.ibm.ws.st.core.internal.config.FeatureList;

/**
 * Extension for downstream bundles to declare that certain features are required to
 * support running a given module.
 * Subclasses must be stateless and thread-safe.
 */
public abstract class FeatureResolver {
    /**
     * Returns a list of features that must be available on the server to support
     * the given application, or <code>null</code> to indicate no additional
     * features are necessary.
     * <p>If delta is non-<code>null</code>, it indicates that this is a delta
     * change, not a full module. If the resolver scans files it must only look
     * within the delta to avoid performance issues.</p>
     * <p>Extensions may return fully-qualified features (e.g. 'servlet-3.0') or
     * simple feature names (e.g. 'servlet'). The later case will automatically
     * resolve to a version of the feature that is supported by the runtime.</p>
     * <p>For simplicity, extensions may return features that already exist on
     * the server. For performance, they may skip looking for features that are
     * already configured.</p>
     * 
     * @param wr a WebSphere runtime
     * @param module an individual module
     * @param delta the module's resource delta, or <code>null</code> if the entire
     *            module has been added or removed
     * @param requiredFeatures The required features found so far (by other resolvers).
     *            The resolver should take these into account and not add a feature
     *            for something that is already covered.
     * @param existingFeatures the features that are currently configured on the server,
     *            or <code>null</code>
     * @param monitor a progress monitor
     */
    public void getRequiredFeatures(WebSphereRuntime wr, IModule[] module, IModuleResourceDelta[] delta, FeatureSet existingFeatures,
                                    RequiredFeatureMap requiredFeatures, IProgressMonitor monitor) {
        List<IModule[]> moduleList = Collections.singletonList(module);
        List<IModuleResourceDelta[]> deltaList = Collections.singletonList(delta);
        getRequiredFeatures(wr, moduleList, deltaList, existingFeatures, requiredFeatures, false, monitor);
    }

    /**
     * Same as above but takes a list of modules and deltas. The includeAll parameter means include all features in the list even if
     * enabled by another feature.
     */
    public abstract void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList,
                                             FeatureSet existingFeatures, RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor);

    /**
     * Returns a list of features that are provided from within the given module through
     * binary files (eg. libraries included within a module), etc. If a library is provided within
     * a module then it's likely the user wants to use that library instead of enabling a feature.
     * Returns <code>null</code> to indicate no contained features were found.
     * <p>Extensions may return fully-qualified features (e.g. 'servlet-3.0') or
     * simple feature names (e.g. 'servlet'). The latter case will automatically
     * resolve to a version of the feature that is supported by the runtime.</p>
     * 
     * @param wr a WebSphere runtime
     * @param module an individual module
     * @param monitor a progress monitor
     * @return an array of contained features, or <code>null</code>
     */
    public FeatureResolverFeature[] getContainedFeatures(WebSphereRuntime wr, IModule[] module, IProgressMonitor monitor) {
        List<IModule[]> moduleList = Collections.singletonList(module);
        return getContainedFeatures(wr, moduleList, monitor);
    }

    /**
     * Same as above but takes a list of modules.
     */
    public FeatureResolverFeature[] getContainedFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, IProgressMonitor monitor) {
        return null;
    }

    public static void checkAndAddFeature(RequiredFeatureMap requiredFeatureMap, FeatureSet existingFeatures, WebSphereRuntime wr, String feature,
                                          List<IModule[]> moduleList, boolean includeAll) {
        checkAndAddFeature(requiredFeatureMap, existingFeatures, wr, new FeatureResolverFeature(feature), moduleList, includeAll);
    }

    public static void checkAndAddFeature(RequiredFeatureMap requiredFeatureMap, FeatureSet existingFeatures, WebSphereRuntime wr, FeatureResolverFeature feature,
                                          List<IModule[]> moduleList, boolean includeAll) {

        String featureName = feature.getName();
        if (existingFeatures != null && existingFeatures.supports(featureName))
            return;

        if (requiredFeatureMap.contains(feature)) {
            requiredFeatureMap.addModules(feature, moduleList);
            return;
        }

        if (!includeAll) {
            FeatureResolverFeature[] requiredFeatures = requiredFeatureMap.getFeatures();
            for (int i = 0; i < requiredFeatures.length; i++) {
                // Check if the feature is already enabled by a feature in the list
                if (FeatureList.isEnabledBy(featureName, requiredFeatures[i].getName(), wr)) {
                    requiredFeatureMap.addModules(feature, moduleList);
                    return;
                }
                // Check if the feature enables any existing features in the list and replace it
                // Need to loop through the whole list since there may be more than one
                if (FeatureList.isEnabledBy(requiredFeatures[i].getName(), featureName, wr)) {
                    if (!requiredFeatureMap.contains(feature)) {
                        requiredFeatureMap.replaceFeature(requiredFeatures[i], feature);
                    } else {
                        requiredFeatureMap.removeFeature(requiredFeatures[i]);
                    }
                    requiredFeatureMap.addModules(feature, moduleList);
                }
            }
        }

        if (!requiredFeatureMap.contains(feature))
            requiredFeatureMap.addFeature(feature, moduleList);
    }

    @Override
    public String toString() {
        return "FeatureResolver [" + getClass().toString() + "]";
    }
}