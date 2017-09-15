/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A fixed list of features.
 */
public class FeatureSet implements Iterable<String> {
    private final String[] features;
    private final WebSphereRuntime wsRuntime;

    public FeatureSet(WebSphereRuntime wr, String[] s) {
        wsRuntime = wr;
        features = s;
    }

    public FeatureSet(WebSphereRuntime wr, List<String> s) {
        wsRuntime = wr;
        features = s.toArray(new String[s.size()]);
    }

    /**
     * Returns <code>true</code> if the given feature is contained in this set or
     * supported (a subset) of a feature within it.
     * If the given feature includes a version, it attempts to find an exact match.
     * If the given feature does not contain a version, it will match against any
     * version of the feature.
     * 
     * @param featureToFind with or without version, for example jsp or jsp-2.2
     * @return
     */
    public boolean supports(String featureToFind) {
        if (featureToFind == null)
            return false;

        // if the feature to match doesn't have a version then find the matching features from the supported features first
        if (!featureToFind.contains(FeatureUtil.FEATURE_SEPARATOR)) {
            String t = featureToFind.toLowerCase() + FeatureUtil.FEATURE_SEPARATOR;
            // find match
            for (String f : wsRuntime.getInstalledFeatures()) {
                if (f.toLowerCase().startsWith(t) && isFeatureSupported(f))
                    return true;
            }
        } else
            return isFeatureSupported(featureToFind);

        return false;
    }

    /**
     * Returns true if the given feature is contained in this set or
     * supported (a subset) of a feature within it.
     * 
     * @param featureToFind with version, for example jsp-2.2
     * @return
     */
    public boolean isFeatureSupported(String featureToFind) {
        // if the version of feature is not known, use com.ibm.ws.st.core.internal.FeatureSet.supports(String) instead
        if (!featureToFind.contains(FeatureUtil.FEATURE_SEPARATOR))
            return false;

        for (String feature : features) {
            if (feature.equalsIgnoreCase(featureToFind) || FeatureUtil.isLowerVersion(featureToFind, feature) || wsRuntime.isContainedBy(featureToFind, feature))
                return true;
        }
        return false;
    }

    /**
     * Attempt to resolve the given feature against this set.
     * If the feature includes a version, it attempts to find an exact match.
     * If the feature does not contain a version, it will try to match against the highest
     * version of the feature in this set.
     * If no match can be found, <code>null</code> is returned.
     * 
     * @param feature a feature (e.g."servlet")
     * @return a resolved feature if possible (e.g. "servlet-3.0"), otherwise <code>null</code>
     */
    public String resolve(String feature) {
        return resolve(feature, features);
    }

    /**
     * Attempt to resolve the given feature against the provided set of features.
     * If the feature includes a version, it attempts to find an exact match.
     * If the feature does not contain a version, it will try to match against the highest
     * version of the feature in the provided set of features.
     * If no match can be found, <code>null</code> is returned.
     * 
     * @param feature a feature (e.g."servlet")
     * @param features a set of features to resolve against
     * @return a resolved feature if possible (e.g. "servlet-3.0"), otherwise <code>null</code>
     */
    public static String resolve(String feature, String[] features) {
        if (feature == null)
            return null;

        if (!feature.contains(FeatureUtil.FEATURE_SEPARATOR)) {
            String t = feature.toLowerCase() + FeatureUtil.FEATURE_SEPARATOR;
            String matchedFeature = null;
            // find match
            for (String f : features) {
                if (f.toLowerCase().startsWith(t)) {
                    try {
                        if (matchedFeature == null
                            || FeatureUtil.compareFeatureVersions(matchedFeature, f) < 0)
                            matchedFeature = f;
                    } catch (Throwable e) {
                        if (Trace.ENABLED)
                            Trace.logError("Failed to compare feature version between " + matchedFeature + " and " + f, e);
                    }
                }
            }
            return matchedFeature;
        }
        for (String f : features)
            if (f.equalsIgnoreCase(feature))
                return f;

        return null;
    }

    /**
     * If the feature is not present in the given list, then this method returns a higher version feature closest
     * to the feature in the given list.
     * 
     * @param feature
     * @return closest higher version feature
     */

    public String resolveToHigherVersion(String feature) {
        String resolvedFeature = resolve(feature, features);

        if (resolvedFeature == null)
            resolvedFeature = FeatureUtil.findHigherVersion(Arrays.asList(this.features), feature);

        return resolvedFeature;
    }

    /**
     * Attempt to resolve the given feature against this set.
     * If the feature includes a version, it attempts to find an exact match.
     * If the feature does not contain a version, it will return a list of all
     * possible version matches in this set.
     * 
     * If no match can be found, <code>null</code> is returned.
     * 
     * @param feature a feature (e.g."servlet")
     * @return a resolved feature list if possible (e.g. "servlet-3.0, servlet-3.1"), otherwise <code>null</code>
     */
    public String[] resolveAll(String feature) {
        return resolveAll(feature, features);
    }

    /**
     * Attempt to resolve the given feature against the provided set.
     * If the feature includes a version, it attempts to find an exact match.
     * If the feature does not contain a version, it will return a list of all
     * possible version matches in this set.
     * 
     * If no match can be found, <code>null</code> is returned.
     * 
     * @param feature a feature (e.g."servlet")
     * @param features a set of features to resolve against
     * @return a resolved feature list if possible (e.g. "servlet-3.0, servlet-3.1"), otherwise <code>null</code>
     */
    public static String[] resolveAll(String feature, String[] features) {
        if (feature == null)
            return null;

        List<String> matchList = new ArrayList<String>();
        if (!feature.contains(FeatureUtil.FEATURE_SEPARATOR)) {
            String t = feature.toLowerCase() + FeatureUtil.FEATURE_SEPARATOR;
            // find match
            for (String f : features) {
                if (f.toLowerCase().startsWith(t)) {
                    matchList.add(f);
                }
            }
            if (matchList.isEmpty())
                return null;

            String[] match = new String[matchList.size()];
            return matchList.toArray(match);
        }

        for (String f : features)
            if (f.equalsIgnoreCase(feature))
                return new String[] { f };

        return null;
    }

    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(features).iterator();
    }

    /**
     * Sort the feature list alphabetically.
     */
    public void sort() {
        Arrays.sort(features);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FeatureSet[");
        int size = features.length;
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(",");
            String s = features[i];
            sb.append(s);
        }
        sb.append("]");
        return sb.toString();
    }
}