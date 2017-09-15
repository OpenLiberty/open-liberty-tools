/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.ws.st.core.internal.FeatureUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.generation.Feature;
import com.ibm.ws.st.core.internal.generation.FeatureListCoreMetadata;
import com.ibm.ws.st.core.internal.generation.FeatureListExtMetadata;

public class FeatureList {

    /**
     * Check if a feature is valid.
     *
     * @param feature The feature name
     * @param wsRuntime The runtime, null means use fall back feature list
     */
    public static boolean isValidFeature(String feature, WebSphereRuntime wsRuntime) {
        return getFeature(feature, wsRuntime) != null;
    }

    /**
     * Get the canonical name for a feature if the feature is valid. Returns null if
     * the feature is not valid.
     *
     * @param feature The feature name
     * @param wsRuntime The runtime, null means use fall back feature list
     */
    public static String getCanonicalFeatureName(String featureName, WebSphereRuntime wsRuntime) {
    	Feature feature = getFeature(featureName, wsRuntime);
        if (feature != null)
            return feature.getName();
        return null;
    }

    /**
     * Convert the list of feature names to a list of canonical feature names.
     *
     * @param features The list of features to convert
     * @param wsRuntime The associated runtime
     * @return The canonicalized feature list
     */
    public static List<String> getCanonicalFeatures(List<String> features, WebSphereRuntime wsRuntime) {
        if (wsRuntime == null) {
            return features;
        }
        Map<String, Feature> featureMap = FeatureList.getFeatureMap(wsRuntime);
        final List<String> canonicalFeatures = new ArrayList<String>();
        for (String feature : features) {
            String featureName = feature;
            Feature featureObj = getFeature(featureName, featureMap);
            if (featureObj != null) {
                featureName = featureObj.getName();
            }
            if (!canonicalFeatures.contains(featureName)) {
                canonicalFeatures.add(featureName);
            }
        }
        return canonicalFeatures;
    }

    /**
     * Get the list of features.
     *
     * @param sort Whether to sort the features or not.
     * @param runtimeId The id for the runtime, null means use fall back feature list
     */
    public static List<String> getFeatures(boolean sort, WebSphereRuntime wsRuntime) {
        Set<String> features = getRuntimeFeatureSet(wsRuntime);
        ArrayList<String> featureList = new ArrayList<String>(features.size());
        featureList.addAll(features);
        if (sort) {
            Collections.sort(featureList);
        }
        return featureList;
    }

    public static List<String> getSymbolicNameFeatures(boolean sort, WebSphereRuntime wsRuntime) {
        Collection<Feature> features = getFeatureMap(wsRuntime).values();
        ArrayList<String> featureList = new ArrayList<String>(features.size());
        for (Feature f : features) {
            if (f.getSymbolicName() != null) {
                featureList.add(f.getSymbolicName());
            }
        }

        if (sort) {
            Collections.sort(featureList);
        }

        return featureList;
    }

    /**
     * Get the feature set.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     */
    public static Set<String> getRuntimeFeatureSet(WebSphereRuntime wsRuntime) {
        return getFeatureMap(wsRuntime).keySet();
    }

    /**
     * Get the set of children for the given feature.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     *
     */

    public static Set<String> getFeatureChildren(String feature, WebSphereRuntime wsRuntime) {
        Set<String> children = new HashSet<String>();
        HashMap<String, Feature> map = getFeatureMap(wsRuntime);
        getFeatureChildren(children, feature, map);
        return children;
    }

    private static void getFeatureChildren(Set<String> set, String featureName, HashMap<String, Feature> map) {
        Feature f = getFeature(featureName, map);
        if (f == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unrecognized feature:" + featureName);
            return;
        }
        Set<String> children = f.getEnables();
        if (children != null) {
            for (String child : children) {
                if (!containsIgnoreCase(set, child) && !(featureName.equalsIgnoreCase(child))) {
                    set.add(child);
                    getFeatureChildren(set, child, map);
                }
            }
        }
    }

    /**
     * Get the feature display name.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     */
    public static String getFeatureDisplayName(String feature, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(feature, wsRuntime);
        return f == null ? null : f.getDisplayName();
    }

    /**
     * Get the feature description.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     */
    public static String getFeatureDescription(String feature, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(feature, wsRuntime);
        return f == null ? null : f.getDescription();
    }

    /**
     * Return whether the feature is superseded.
     */
    public static boolean isFeatureSuperseded(String feature, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(feature, wsRuntime);
        return f == null ? false : f.isSuperseded();
    }

    /**
     * Get the features that supersede this feature.
     */
    public static Set<String> getFeatureSupersededBy(String feature, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(feature, wsRuntime);
        return f == null ? null : f.getSupersededBy();
    }

    /**
     * Get the set of parents for the given feature.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     *
     */

    public static Set<String> getFeatureParents(String featureName, WebSphereRuntime wsRuntime) {
        Set<String> set = new HashSet<String>();
        HashMap<String, Feature> map = getFeatureMap(wsRuntime);
        getFeatureParents(set, featureName, map);
        return set;
    }

    private static void getFeatureParents(Set<String> set, String featureName, HashMap<String, Feature> map) {
        // contains features now...
        Set<Entry<String, Feature>> entries = map.entrySet();

        for (Entry<String, Feature> entry : entries) {
            String key = entry.getKey();
            Feature f = entry.getValue();
            if (key == null || f == null)
                continue;

            if (containsIgnoreCase(f.getEnables(), featureName)) {
                if (!containsIgnoreCase(set, key) && !featureName.equalsIgnoreCase(key)) {
                    set.add(key);
                    getFeatureParents(set, key, map);
                }
            }
        }
    }

    /**
     * Get the set of API Jar info for the given feature.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     */
    public static Set<String> getFeatureAPIJars(String feature, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(feature, wsRuntime);
        if (f == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Unrecognized feature:" + feature);
            return new HashSet<String>(); // this method is not used except in junit test. We may change to return null if appropriate.
        }
        return f.getApiJars();
    }

    /**
     * Get the feature config elements.
     *
     * @param runtimeId The id for the runtime, null means use fall back feature list
     */
    public static Set<String> getFeatureConfigElements(String feature, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(feature, wsRuntime);
        return f == null ? null : f.getConfigElements();
    }

    /**
     * Returns true if feature1 is enabled by feature2 (either feature2 matches feature1 or
     * one of its children does).
     */
    public static boolean isEnabledBy(String feature1, String feature2, WebSphereRuntime wsRuntime) {
        if (feature1.equalsIgnoreCase(feature2) || FeatureUtil.isSupportedBy(feature1, feature2))
            return true;
        return isContainedBy(feature1, feature2, wsRuntime);
    }

    /**
     * Returns true if the all of the features in the featuresToCheck list are enabled by
     * the features in allFeatures list.
     */
    public static boolean featuresEnabled(List<String> featuresToCheck, List<String> allFeatures, WebSphereRuntime wsRuntime) {
        for (String feature : featuresToCheck) {
            if (!featureEnabled(feature, allFeatures, wsRuntime))
                return false;
        }
        return true;
    }

    /**
     * Returns true if featuretoCheck is enabled by a feature in the allFeatures list.
     */
    public static boolean featureEnabled(String featureToCheck, List<String> allFeatures, WebSphereRuntime wsRuntime) {
        for (String feature : allFeatures) {
            if (isEnabledBy(featureToCheck, feature, wsRuntime))
                return true;
        }
        return false;
    }

    /**
     * Returns true if feature1 is a subset of feature2.
     */
    public static boolean isContainedBy(String feature1, String feature2, WebSphereRuntime wsRuntime) {
        Set<String> children = FeatureList.getFeatureChildren(feature2, wsRuntime);
        return isSupportedBy(children, feature1);
    }

    public static HashMap<String, Feature> getFeatureMap(WebSphereRuntime wsRuntime) {
        HashMap<String, Feature> allInclusiveFeatureMap = new HashMap<String, Feature>();

        allInclusiveFeatureMap.putAll(FeatureListCoreMetadata.getInstance().getFeatureListMaps(wsRuntime).get(FeatureMapType.PUBLIC_FEATURES_KEYED_BY_NAME));
        for (FeatureListExtMetadata ext : FeatureListExtMetadata.getInstances(wsRuntime)) {
            HashMap<String, Feature> extMap = ext.getFeatureListMaps(wsRuntime).get(FeatureMapType.PUBLIC_FEATURES_KEYED_BY_NAME);
            if (extMap != null)
                allInclusiveFeatureMap.putAll(extMap);
        }

        return allInclusiveFeatureMap;
    }

    public static HashMap<String, Feature> getAllFeaturesKeyedBySymbolicName(WebSphereRuntime wsRuntime) {
        HashMap<String, Feature> allInclusiveFeatureMap = new HashMap<String, Feature>();

        allInclusiveFeatureMap.putAll(FeatureListCoreMetadata.getInstance().getFeatureListMaps(wsRuntime).get(FeatureMapType.ALL_FEATURES_KEYED_BY_SYMBOLIC_NAME));
        for (FeatureListExtMetadata ext : FeatureListExtMetadata.getInstances(wsRuntime)) {
            HashMap<String, Feature> extMap = ext.getFeatureListMaps(wsRuntime).get(FeatureMapType.ALL_FEATURES_KEYED_BY_SYMBOLIC_NAME);
            if (extMap != null)
                allInclusiveFeatureMap.putAll(extMap);
        }

        return allInclusiveFeatureMap;
    }

    private static Feature getFeature(String featureName, WebSphereRuntime wsRuntime) {
        return getFeature(featureName, getFeatureMap(wsRuntime));
    }

    private static Feature getFeature(String featureName, Map<String, Feature> featureMap) {
        if (featureMap != null) {
            Set<Map.Entry<String, Feature>> entrySet = featureMap.entrySet();
            for (Map.Entry<String, Feature> entry : entrySet) {
                String key = entry.getKey();
                if (key.equalsIgnoreCase(featureName))
                    return entry.getValue();
            }
        }
        return null;
    }

    public static boolean containsIgnoreCase(Set<String> set, String val) {
        for (String entry : set) {
            if (val.equalsIgnoreCase(entry))
                return true;
        }
        return false;
    }

    private static boolean isSupportedBy(Set<String> set, String val) {
        for (String entry : set) {
            if (val.equalsIgnoreCase(entry) || FeatureUtil.isSupportedBy(val, entry))
                return true;
        }
        return false;
    }

    public enum FeatureMapType {

        PUBLIC_FEATURES_KEYED_BY_NAME,
        ALL_FEATURES_KEYED_BY_SYMBOLIC_NAME;

    }

    /**
     * Returns the name of the public feature for the given symbolic name
     *
     * If the feature doesn't exist, it returns null
     * If the feature is not public, it returns the symbolic name
     *
     * @param symbolicName
     * @param wsRuntime
     * @return public feature name
     */
    public static String getPublicFeatureName(String symbolicName, WebSphereRuntime wsRuntime) {
        Feature feature = getAllFeaturesKeyedBySymbolicName(wsRuntime).get(symbolicName);
        if (feature != null) {
            String name = feature.getName();
            if (name != null) {
                return name;
            }
        } else {
            return null;
        }
        return symbolicName;
    }

    /**
     * Returns the symbolic name of a public feature
     *
     * If the feature doesn't exist, it returns null
     *
     * @param publicFeatureName
     * @param wsRuntime
     * @return symbolic name of the public feature
     */
    public static String getFeatureSymbolicName(String publicFeatureName, WebSphereRuntime wsRuntime) {
        HashMap<String, Feature> featureMap = getFeatureMap(wsRuntime);
        Feature feature = featureMap.get(publicFeatureName);
        if (feature != null) {
            return feature.getSymbolicName();
        }
        return null;
    }

    /**
     * Returns the category of a feature.
     *
     * If the feature doesn't exist, it returns null
     *
     * @param feature name
     * @param wsRuntime
     * @return category
     */
    public static Set<String> getFeatureCategory(String featureName, WebSphereRuntime wsRuntime) {
        Feature f = getFeature(featureName, wsRuntime);
        return f == null ? null : f.getCategoryElements();
    }
}
