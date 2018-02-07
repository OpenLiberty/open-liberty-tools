/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.List;

public class FeatureUtil {

    static final String FEATURE_SEPARATOR = "-";

    /**
     * Check to see if a feature1 is an older version of feature2
     *
     * @param feature1
     * @param feature2
     * @return true if condition satisfied, otherwise false
     */
    public static boolean isLowerVersion(String feature1, String feature2) {
        int index = feature1.indexOf(FEATURE_SEPARATOR);

        if (index == -1 || feature2.indexOf(FEATURE_SEPARATOR) != index)
            return false;

        if (feature2.toLowerCase().startsWith(feature1.substring(0, index + 1).toLowerCase()))
            return compareFeatureVersions(feature1, feature2) < 0;

        return false;
    }

    /**
     * Check to see if a feature1 is directly enabled by feature2. Does not
     * check children.
     *
     * @param feature1
     * @param feature2
     * @return true if condition satisfied, otherwise false
     */
    public static boolean isSupportedBy(String feature1, String feature2) {

        // If no version on feature2 then return false
        int index = feature2.indexOf(FEATURE_SEPARATOR);
        if (index == -1) {
            return false;
        }

        // If the names don't match, return false
        String feature2Name = feature2.substring(0, index).toLowerCase();
        if (!feature1.toLowerCase().startsWith(feature2Name)) {
            return false;
        }

        // If no version on feature1 then return true
        if (feature1.indexOf(FEATURE_SEPARATOR) == -1) {
            return true;
        }

        // Otherwise, compare the versions
        return compareFeatureVersions(feature1, feature2) <= 0;
    }

    /**
     * compare the versions of the featureA and featureB
     *
     * @param featureA
     * @param featureB
     * @return 1 if featureA's version is greater than featureB, -1 otherwise
     */
    static int compareFeatureVersions(String featureA, String featureB) {
        String versionA = featureA.substring(featureA.indexOf(FEATURE_SEPARATOR) + 1);
        String versionB = featureB.substring(featureB.indexOf(FEATURE_SEPARATOR) + 1);

        String[] splitA = versionA.split("\\.");
        String[] splitB = versionB.split("\\.");
        int comparisonLimit = splitA.length < splitB.length ? splitA.length : splitB.length;
        for (int i = 0; i < comparisonLimit; i++) {
            int valA = Integer.valueOf(splitA[i]).intValue();
            int valB = Integer.valueOf(splitB[i]).intValue();
            if (valA > valB)
                return 1;
            if (valA < valB)
                return -1;
        }
        if (versionA.length() > versionB.length())
            return 1;
        if (versionA.length() < versionB.length())
            return -1;
        return 0;
    }

    /**
     * returns the first matching lower version of featureToCheck from given featureList.
     *
     * @param featureList
     * @param featureToCheck
     * @return first matching lower version feature
     */
    public static String findLowerVersion(List<String> featureList, String featureToCheck) {
        int index = featureToCheck.indexOf(FEATURE_SEPARATOR);
        if (index == -1)
            return null;

        String f = featureToCheck.substring(0, index + 1).toLowerCase();
        for (String feature : featureList) {
            if (feature.toLowerCase().startsWith(f)) {
                if (FeatureUtil.isLowerVersion(feature, featureToCheck))
                    return feature;
            }
        }

        return null;
    }

    /**
     * Returns the feature with closest matching higher version than featureToCheck in the given featureList. for example,
     * if the featureToChech has version 1.2 and featureList has versions 1.3, 1.5, 2.0 then this method will return the feature
     * with version 1.3.
     *
     * @param featureList
     * @param featureToCheck
     * @return closest higher version feature
     */
    @SuppressWarnings("boxing")
    public static String findHigherVersion(List<String> featureList, String featureToCheck) {
        int index = featureToCheck.indexOf(FEATURE_SEPARATOR);

        if (index == -1)
            return null;

        float previousVersion = Float.MAX_VALUE;
        String higherVersionFeature = null;
        String f = featureToCheck.substring(0, index + 1).toLowerCase();

        //loops through all the features and find the feature with higher version that is closest to the version of featureToCheck.
        for (String feature : featureList) {
            if (feature.toLowerCase().startsWith(f) && FeatureUtil.isLowerVersion(featureToCheck, feature)) {
                float currentVersion = Float.valueOf(feature.substring(feature.indexOf(FEATURE_SEPARATOR) + 1));
                // previousVersion was initiated with a very large floating number. so fist time, currentVersion will always be smaller than previousVersion
                if (currentVersion < previousVersion) {
                    previousVersion = currentVersion;
                    higherVersionFeature = feature;
                }
            }
        }

        return higherVersionFeature;
    }

    static String getName(String feature) {
        int index = feature.indexOf(FEATURE_SEPARATOR);
        if (index == -1) {
            return feature;
        }
        // Return the string up to, but not including, the FEATURE_SEPARATOR
        return feature.substring(0, index);
    }

    static String getVersion(String feature) {
        int index = feature.indexOf(FEATURE_SEPARATOR);
        if (index == -1) {
            return feature;
        }
        // Add one so we do not return the FEATURE_SEPARATOR
        return feature.substring(index + 1);
    }
}
