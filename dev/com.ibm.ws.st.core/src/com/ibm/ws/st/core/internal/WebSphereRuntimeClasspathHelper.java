/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.ws.st.core.internal.config.FeatureList;

/**
 * Wrapper class for WebSphereRuntime which provides utility methods to get information regarding
 * the features in the runtime, relationship between the features, and jars related to those features.
 */
class WebSphereRuntimeClasspathHelper {

    // Map to hold the features that use each jar. The key is the jar name
    // (including path relative to root of server), the target is a set with feature names
    private Map<String, Set<String>> featuresPerJar = new HashMap<String, Set<String>>();

    // Map to hold the features with conflicts. The key is the name of a feature with conflicts,
    // the target is a set of other features in conflict with the key
    // For example, if jpa-2.0 and jpa-2.1 are in conflict with each other, the will be two entries
    // in the map: one with key jpa-2.0 (which will be mapped to a set with one element, jpa-2.1),
    // and other with key jpa-2.1 (which will be mapped to a set with one element, jpa-2.0),
    private Map<String, Set<String>> featuresWithConflicts = new HashMap<String, Set<String>>();

    // Map to hold the jars that should not be added to the classpath of a project
    // with a given feature, because those jars are from another feature in conflict
    // The key is the name of a feature with conflict, and the target is the set of jars that
    // should not be included in the classpath.
    // For example, if jpa-2.0 and jpa-2.1 are in conflict with each other, the will be two entries
    // in the map: one with key jpa-2.0 (which will be mapped to a set of the jars used by jpa-2.1 only),
    // and other with key jpa-2.1 (which will be mapped to a set of the jars used by jpa-2.0 only),
    private Map<String, Set<String>> conflictedJarsPerFeature = new HashMap<String, Set<String>>();

    // Map to hold all the features, sorted by version, from lower to higher. The key is the name of the feature,
    // without version. The target is a SortedSet, with all the versions of the feature (including feature name).
    // For example, for jpa, the key would be jpa, and the target would be [jpa-1.0, jpa-2.0, jpa-2.1]
    private Map<String, SortedSet<String>> featureByNameAndVersions = new HashMap<String, SortedSet<String>>();

    private final WebSphereRuntime runtime;
    private final String runtimeName;

    public WebSphereRuntimeClasspathHelper(WebSphereRuntime runtime) {
        this.runtime = runtime;
        this.runtimeName = runtime.getRuntime() == null ? "Unknown" : runtime.getRuntime().getName();
    }

    private void findFeaturesPerJar() {
        featuresPerJar = new HashMap<String, Set<String>>();
        List<String> features = FeatureList.getFeatures(false, runtime);

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Found " + features.size() + " features in runtime " + runtimeName + ".");

        for (String feature : features) {
            Set<String> jars = FeatureList.getFeatureAPIJars(feature, runtime);

            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Feature: " + feature + ". Jars: " + jars);

            for (String jar : jars) {
                if (!featuresPerJar.containsKey(jar)) {
                    Set<String> featuresToAdd = new HashSet<String>();
                    featuresToAdd.add(feature);
                    featuresPerJar.put(jar, featuresToAdd);
                } else {
                    featuresPerJar.get(jar).add(feature);
                }
            }
        }
    }

    private void findFeatureVersions() {
        featureByNameAndVersions = new HashMap<String, SortedSet<String>>();
        List<String> features = FeatureList.getFeatures(false, runtime);

        for (String feature : features) {
            String name = FeatureUtil.getName(feature);
            if (!featureByNameAndVersions.containsKey(name)) {
                SortedSet<String> versions = new TreeSet<String>(new Comparator<String>() {

                    @Override
                    public int compare(String featureA, String featureB) {
                        //Sort from lower to higher version. Assume the features have same name
                        int indexA = featureA.indexOf("-");
                        int indexB = featureB.indexOf("-");

                        if (indexA == -1 && indexB == -1)
                            return 0;

                        if (indexA == -1)
                            return -1;

                        if (indexB == -1)
                            return 1;

                        String versionA = featureA.substring(indexA + 1);
                        String versionB = featureB.substring(indexB + 1);

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

                });
                versions.add(feature);
                featureByNameAndVersions.put(name, versions);
            } else {
                featureByNameAndVersions.get(name).add(feature);
            }
        }
    }

    private Set<String> getJarsInFeatureAndConflicts(String feature) {

        Set<String> jars = FeatureList.getFeatureAPIJars(feature, runtime);

        // Check if the jars are used by other feature that is not the current feature nor
        // the features in conflict with current feature. If that is the case, we want to keep the jar
        Set<String> jarsInConflictingFeaturesOnly = new HashSet<String>();
        for (String jar : jars) {
            Set<String> featuresUsingJar = featuresPerJar.get(jar);

            // TODO: The code below is not working. Investigate why. In the mean time, we add the jars to
            // the final list if the jar is used in at least one feature
            //boolean foundInNonConflictingFeature = false;
            //if (featuresUsingJar != null && featuresUsingJar.size() > 0) {
            //for (String featureUsingJar : featuresUsingJar) {
            //if (!featureUsingJar.equals(feature) && !featuresWithConflicts.get(feature).contains(featureUsingJar)) {
            //foundInNonConflictingFeature = true;
            //break;
            //}
            //}
            //if (!foundInNonConflictingFeature) {
            //jarsInConflictingFeaturesOnly.add(jar);
            //}
            //}

            if (featuresUsingJar != null && featuresUsingJar.size() > 0) {
                jarsInConflictingFeaturesOnly.add(jar);
            }
        }
        return jarsInConflictingFeaturesOnly;
    }

    private void findConflictedJarsPerFeature() {
        conflictedJarsPerFeature = new HashMap<String, Set<String>>();
        for (String featureWithConflict : featuresWithConflicts.keySet()) {
            // First, get the jars used by the feature in conflict

            Set<String> jars1 = getJarsInFeatureAndConflicts(featureWithConflict);
            Set<String> jarsTotal = new HashSet<String>();

            for (String feature : featuresWithConflicts.get(featureWithConflict)) {
                Set<String> tmp = getJarsInFeatureAndConflicts(feature);
                tmp.removeAll(jars1);
                jarsTotal.addAll(tmp);
            }
            conflictedJarsPerFeature.put(featureWithConflict, jarsTotal);

        }

    }

    private void findFeaturesWithConflicts() {
        //TODO: Need a smart way to get this info.

        featuresWithConflicts = new HashMap<String, Set<String>>();
        Set<String> features;

        // JPA features
        features = new HashSet<String>();
        features.add("jpa-2.0");
        features.add("jpa-2.1");
        featuresWithConflicts.put("jpa-2.2", features);
        features = new HashSet<String>();
        features.add("jpa-2.0");
        features.add("jpa-2.2");
        featuresWithConflicts.put("jpa-2.1", features);
        features = new HashSet<String>();
        features.add("jpa-2.1");
        features.add("jpa-2.2");
        featuresWithConflicts.put("jpa-2.0", features);

        // EJB features
        features = new HashSet<String>();
        features.add("ejbLite-3.2");
        features.add("mdb-3.2");
        featuresWithConflicts.put("ejbLite-3.1", features);
        features = new HashSet<String>();
        features.add("ejbLite-3.1");
        features.add("mdb-3.1");
        featuresWithConflicts.put("ejbLite-3.2", features);

        //Servlets
        features = new HashSet<String>();
        features.add("servlet-3.0");
        features.add("servlet-3.1");
        featuresWithConflicts.put("servlet-4.0", features);
        features = new HashSet<String>();
        features.add("servlet-3.0");
        features.add("servlet-4.0");
        featuresWithConflicts.put("servlet-3.1", features);
        features = new HashSet<String>();
        features.add("servlet-3.1");
        features.add("servlet-4.0");
        featuresWithConflicts.put("servlet-3.0", features);

        //jax-rs
        features = new HashSet<String>();
        features.add("jaxrs-1.1");
        features.add("jaxrs-2.0");
        features.add("jaxrsClient-2.0");
        featuresWithConflicts.put("jaxrs-2.1", features);
        features = new HashSet<String>();
        features.add("jaxrs-1.1");
        features.add("jaxrs-2.1");
        features.add("jaxrsClient-2.1");
        featuresWithConflicts.put("jaxrs-2.0", features);
        features = new HashSet<String>();
        features.add("jaxrs-2.0");
        features.add("jaxrsClient-2.0");
        features.add("jaxrs-2.1");
        features.add("jaxrsClient-2.1");
        featuresWithConflicts.put("jaxrs-1.1", features);

        //Bean Validation
        features = new HashSet<String>();
        features.add("beanValidation-1.0");
        features.add("beanValidation-1.1");
        featuresWithConflicts.put("beanValidation-2.0", features);
        features = new HashSet<String>();
        features.add("beanValidation-1.0");
        features.add("beanValidation-2.0");
        featuresWithConflicts.put("beanValidation-1.1", features);
        features = new HashSet<String>();
        features.add("beanValidation-1.1");
        features.add("beanValidation-2.0");
        featuresWithConflicts.put("beanValidation-1.0", features);

        //CDI
        features = new HashSet<String>();
        features.add("cdi-1.0");
        features.add("cdi-1.2");
        featuresWithConflicts.put("cdi-2.0", features);
        features = new HashSet<String>();
        features.add("cdi-1.0");
        features.add("cdi-2.0");
        featuresWithConflicts.put("cdi-1.2", features);
        features = new HashSet<String>();
        features.add("cdi-1.2");
        features.add("cdi-2.0");
        featuresWithConflicts.put("cdi-1.0", features);

        //JSF
        features = new HashSet<String>();
        features.add("jsf-2.0");
        features.add("jsf-2.2");
        featuresWithConflicts.put("jsf-2.3", features);
        features = new HashSet<String>();
        features.add("jsf-2.0");
        features.add("jsf-2.3");
        featuresWithConflicts.put("jsf-2.2", features);
        features = new HashSet<String>();
        features.add("jsf-2.2");
        features.add("jsf-2.3");
        featuresWithConflicts.put("jsf-2.0", features);

    }

    /**
     * Refreshes the cached information
     */
    public synchronized void refresh() {

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Entering WebSphereRuntimeClasspathHelper:refresh() for runtime " + runtimeName);

        long start = System.currentTimeMillis();

        findFeatureVersions();
        findFeaturesPerJar();
        findFeaturesWithConflicts();
        findConflictedJarsPerFeature();

        if (Trace.ENABLED) {
            Trace.tracePerf("Leaving WebSphereRuntimeClasspathHelper:refresh() for runtime " + runtimeName, start);
            Trace.trace(Trace.INFO, "Features per Jar: " + featuresPerJar);
            Trace.trace(Trace.INFO, "Features with conflicts: " + featuresWithConflicts);
            Trace.trace(Trace.INFO, "Conflicted jars per features: " + conflictedJarsPerFeature);
        }
    }

    /**
     * Returns a map of features, and the features in conflict with the feature.
     * For example, if jpa-2.0 and jpa-2.1 are in conflict with each other, there will be two entries
     * in the map: one with key jpa-2.0 (which will be mapped to a set with one element, jpa-2.1),
     * and other with key jpa-2.1 (which will be mapped to a set with one element, jpa-2.0),
     *
     * @return the map of conflicted features. Could be empty if the runtime does not define any
     *         features in conflict.
     */
    public Map<String, Set<String>> getFeaturesWithConflicts() {
        return featuresWithConflicts;
    }

    /**
     * Given a feature, if the feature is in conflict with other features, this method
     * will return the jars which are used by the conflicted features, but not in the feature
     * passed in as parameter.
     * For example, if jpa-2.0 and jpa-2.1 are in conflict with each other, and jpa-2.1 is passed in
     * as parameter, this method will return a set with the jars used by jpa-2.0 only.
     *
     * @param featureName a feature name, for example, jpa-2.0. Must not be null.
     * @return a set of jars in conflict with the featureName. If featureName is not in conflict with
     *         any other feature, the returned set will be empty.
     */
    public Set<String> getConflictedJarsPerFeature(String featureName) {
        if (featureName == null)
            throw new IllegalArgumentException();
        return conflictedJarsPerFeature.get(featureName);
    }

    /**
     * Returns the highest version for a given feature name.
     *
     * @param featureName the name of a feature, without the version, For example, jpa.
     * @return the highest version of the feature, including the feature name, for example jpa-2-1,
     *         or null if the feature is not found.
     *
     */
    public String getHighestVersion(String featureName) {
        SortedSet<String> versions = featureByNameAndVersions.get(featureName);
        if (versions != null) {
            return versions.last();
        }
        return null;
    }

    /**
     * Returns the API jars for a feature.
     *
     * @param featureName the name of the feature. Must not be null.
     * @return a set of API jars.
     */
    public Set<String> getApiJars(String featureName) {
        if (featureName == null)
            throw new IllegalArgumentException();
        return FeatureList.getFeatureAPIJars(featureName, runtime);

    }
}
