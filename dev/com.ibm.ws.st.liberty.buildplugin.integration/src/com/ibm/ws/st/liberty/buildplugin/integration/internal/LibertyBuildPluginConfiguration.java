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

package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibertyBuildPluginConfiguration {

    private final HashMap<ConfigurationType, String> configMap = new HashMap<ConfigurationType, String>();
    private List<String> jvmOptions = null;
    private Map<String, String> bootstrapProperties = null;
    private List<String> activeBuildProfiles = null;
    private final Set<String> projectCompileDependencies = new HashSet<String>();
    private final long lastModified;
    private String version = null;

    public LibertyBuildPluginConfiguration(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getConfigValue(ConfigurationType type) {
        return configMap.get(type);
    }

    public String setConfigValue(ConfigurationType type, String value) {
        Trace.trace(Trace.INFO, "Setting: " + type.name() + " = " + value);
        return configMap.put(type, value);
    }

    /**
     * @return the bootstrapProperties
     */
    public Map<String, String> getBootstrapProperties() {
        if (bootstrapProperties == null)
            return Collections.emptyMap();
        return bootstrapProperties;
    }

    /**
     * @param bootstrapProperties the bootstrapProperties to set
     */
    public void setBootstrapProperties(Map<String, String> bootstrapProperties) {
        this.bootstrapProperties = bootstrapProperties;
    }

    /**
     * @return the jvmOptions
     */
    public List<String> getJvmOptions() {
        return jvmOptions;
    }

    /**
     * @param jvmOptions the jvmOptions to set
     */
    public void setJvmOptions(List<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    /**
     * @return the lastModified
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * @return the activeBuildProfiles list
     */
    public List<String> getActiveBuildProfiles() {
        if (activeBuildProfiles == null)
            return Collections.emptyList();
        return activeBuildProfiles;
    }

    /**
     * @param activeBuildProfiles the activeBuildProfile to add
     */
    public void setActiveBuildProfiles(List<String> profiles) {
        activeBuildProfiles = profiles;
    }

    public void addProjectCompileDependency(String dependency) {
        projectCompileDependencies.add(dependency);
    }

    public Set<String> getProjectCompileDependencies() {
        return projectCompileDependencies;
    }

    public Set<ConfigurationType> getDelta(LibertyBuildPluginConfiguration cachedConfig) {
        // if there's no cached config or it's empty then everything in the current config map should be used in the update
        if (cachedConfig == null || cachedConfig.configMap.isEmpty()) {
            return configMap.keySet();
        }
        // if the configMap is not the same size or does not contain the same contents we need to compute the deltas
        if (!mapsEqual(configMap, cachedConfig.configMap)) {
            // TODO return set with only the types of the values that have changed
            return new HashSet<ConfigurationType>(Arrays.asList(ConfigurationType.values()));
        }
        return Collections.emptySet();
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean mapsEqual(Map<ConfigurationType, String> configMapA, Map<ConfigurationType, String> configMapB) {
        // If the map sizes are different they aren't equal
        if (configMapA.size() != configMapB.size())
            return false;
        return configMapA.equals(configMapB);
    }

}
