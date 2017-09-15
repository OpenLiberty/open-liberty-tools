/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
import java.util.Collections;
import java.util.List;

/**
 * Instances of this class are returned by FeatureResolver; each instance refers to a feature to be added
 * to the server configuration, or that is required by an existing application.
 *
 * This class introduced as part of WASRTC 122007.
 */
public class FeatureResolverFeature {

    String name;

    List<String> acceptedAlternatives = new ArrayList<String>();

    public FeatureResolverFeature(String name) {
        this.name = name;

        // Finalize the list
        acceptedAlternatives = Collections.unmodifiableList(acceptedAlternatives);
    }

    public FeatureResolverFeature(String name, String[] acceptedAlternativesStringList) {
        this.name = name;

        if (acceptedAlternativesStringList != null && acceptedAlternativesStringList.length > 0) {
            acceptedAlternatives.addAll(Arrays.asList(acceptedAlternativesStringList));
        }

        // Finalize the list
        acceptedAlternatives = Collections.unmodifiableList(acceptedAlternatives);
    }

    /**
     * The feature string (includes the feature name, and optionally the feature version, separated by a hyphen)
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /** Compares feature name only */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FeatureResolverFeature)) {
            return false;
        }

        FeatureResolverFeature otherFrf = (FeatureResolverFeature) o;

        return otherFrf.getName().equalsIgnoreCase(getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * When determining features to add to the server configuration, some features may _not_ need
     * to be added because the same functionality is already handled by other features that are already present.
     *
     * For example, the wasJmsClient-1.1 feature should not be added if wmqJmsClient-1.1 is already present in the server configuration.
     * Thus the wasJmsClient-1.1 would return an accepted alternative of wmqJmsClient-1.1.
     *
     * See WASRTC 122007 for more information on this scenario.
     * This method will never return null.
     *
     * @return the acceptedAlternatives
     */
    public List<String> getAcceptedAlternatives() {
        return acceptedAlternatives;
    }

    /** Utility method - Convert feature list to feature name list */
    public static String[] convertFeatureResolverArrayToStringArray(FeatureResolverFeature[] list) {
        if (list == null) {
            return null;
        }
        String[] result = new String[list.length];
        for (int x = 0; x < list.length; x++) {
            result[x] = list[x].getName();
        }

        return result;
    }

    public static List<String> convertFeatureResolverArrayToStringList(FeatureResolverFeature[] list) {
        if (list == null) {
            return null;
        }

        List<String> result = new ArrayList<String>(list.length);

        for (FeatureResolverFeature frf : list) {
            result.add(frf.getName());
        }

        return result;
    }
}
