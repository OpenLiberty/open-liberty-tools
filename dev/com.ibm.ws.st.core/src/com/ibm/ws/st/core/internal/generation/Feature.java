/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.ProcessType;

/**
 * This class represents a <feature> entry from the featureList.xml file.
 */
public class Feature implements Cloneable {
    private final String name;
    private String displayName;
    private String description;
    private String symbolicName;
    private final String featureInfoName;

    private final FeatureType featureType;

    private static final String JAKARTA_EE9_FEATURE_DIRECT_DEPENDENCY = "com.ibm.websphere.appserver.eeCompatible-9.0";
    private static final String JAKARTA_EE9_CONVENIENCE_FEATURE_NAME = "jakartaee-9.0";
    private static final String JAKARTA_EE9_JSP_FEATURE_NAME = "jsp-3.0";

    private final Set<String> enables = new HashSet<String>(8);
    private final Set<String> apiJars = new HashSet<String>(8);
    private final Set<String> spiJars = new HashSet<String>(8);
    private final Set<String> apiPackages = new HashSet<String>(8);
    private final Set<String> spiPackages = new HashSet<String>(8);
    private final Set<String> configElements = new HashSet<String>(10);
    private final Map<String, List<String>> includes = new HashMap<String, List<String>>(8);
    private final Set<String> autoProvisions = new HashSet<String>(2);
    private final Set<String> categoryElements = new HashSet<String>(10);
    private final Set<String> processTypes = new HashSet<String>(8);

    private boolean isSuperseded;
    private boolean isSingleton;
    private final Set<String> supersededBy = new HashSet<String>(6);

    public Feature(String name, String featureInfoName, FeatureType featureType) {
        this.name = name;
        this.featureInfoName = featureInfoName;
        this.featureType = featureType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the featureType
     */
    public FeatureType getFeatureType() {
        return featureType;
    }

    /**
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the process type
     */
    public ProcessType getProcessType() {
        if (processTypes.size() == 1 && processTypes.contains("CLIENT"))
            return ProcessType.CLIENT;

        return ProcessType.SERVER;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the symbolic name
     */
    public String getSymbolicName() {
        return symbolicName;
    }

    /**
     * @return the category
     */
    public Set<String> getCategoryElements() {
        return categoryElements;
    }

    /**
     * @return the enables set
     */
    public Set<String> getEnables() {
        return enables;
    }

    /**
     * @return the apiJars set
     */
    public Set<String> getApiJars() {
        return apiJars;
    }

    /**
     * @return the spiJars set
     */
    public Set<String> getSpiJars() {
        return spiJars;
    }

    /**
     * @return the apiPackages set
     */
    public Set<String> getApiPackages() {
        return apiPackages;
    }

    /**
     * @return the spiPackages set
     */
    public Set<String> getSpiPackages() {
        return spiPackages;
    }

    /**
     * @return the includes
     */
    public Map<String, List<String>> getIncludes() {
        return includes;
    }

    /**
     * @return is superseded
     */
    public boolean isSuperseded() {
        return isSuperseded;
    }

    /**
     * @return the isSingleton
     */
    public boolean isSingleton() {
        return isSingleton;
    }

    /**
     * @return the superseded by set
     */
    public Set<String> getSupersededBy() {
        return supersededBy;
    }

    /**
     * @return the config elements
     */
    public Set<String> getConfigElements() {
        return configElements;
    }

    /**
     * @return the autoProvision
     */
    public Set<String> getAutoProvisions() {
        return autoProvisions;
    }

    /**
     * @return the featureInfoName
     */
    public String getFeatureInfoName() {
        return featureInfoName;
    }

    /**
     * @return if the feature is Jakarta EE9 feature
     */
    public boolean isJakartaEE9Feature() {
        if (includes.containsKey(JAKARTA_EE9_FEATURE_DIRECT_DEPENDENCY)
            || name.equals(JAKARTA_EE9_CONVENIENCE_FEATURE_NAME)
            || name.equals(JAKARTA_EE9_JSP_FEATURE_NAME)) {
            return true;
        }

        return false;
    }

    /**
     * A feature can be a public feature and still have auto provisions
     * making it an auto feature as well so a separate method is needed
     * since checking the type is not sufficient.
     *
     * @return If this feature is an auto feature
     */
    public boolean isAutoFeature() {
        return !autoProvisions.isEmpty();
    }

    /**
     * @param displayName a new display name
     */
    protected void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @param description a new description
     */
    protected void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param symbolicName a new symbolic name
     */
    protected void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    /**
     * @param processType a new process type
     */
    protected void addProcessType(String processType) {
        this.processTypes.add(processType);
    }

    /**
     * @param category a new category
     */
    protected void addCategoryElements(String category) {
        this.categoryElements.add(category);
    }

    /**
     * @param enables the enables to add
     */
    protected void addEnables(String enables) {
        this.enables.add(enables);
    }

    /**
     * @param enables the apiJar to add
     */
    protected void addAPIJar(String apiJar) {
        this.apiJars.add(apiJar);
    }

    /**
     * @param enables the apiPackage to add
     */
    protected void addAPIPackage(String apiPackage) {
        this.apiPackages.add(apiPackage);
    }

    /**
     * @param enables the spiJar to add
     */
    protected void addSPIJar(String spiJar) {
        this.spiJars.add(spiJar);
    }

    /**
     * @param enables the spiPackage to add
     */
    protected void addSPIPackage(String spiPackage) {
        this.spiPackages.add(spiPackage);
    }

    /**
     * @param isSuperseded a new isSuperseded
     */
    protected void setSuperseded(boolean isSuperseded) {
        this.isSuperseded = isSuperseded;
    }

    /**
     * @param isSingleton the isSingleton to set
     */
    protected void setSingleton(boolean isSingleton) {
        this.isSingleton = isSingleton;
    }

    /**
     * @param supersededBy the supersedBy to add
     */
    protected void addSupersededBy(String supersededBy) {
        this.supersededBy.add(supersededBy);
    }

    protected void addIncludes(String symbolicName, List<String> versions) {
        this.includes.put(symbolicName, versions);
    }

    protected void addAutoProvision(String autoProvision) {
        this.autoProvisions.add(autoProvision);
    }

    @Override
    public Feature clone() {
        Feature clonedFeature = new Feature(getName(), featureInfoName, featureType);
        clonedFeature.setDisplayName(getDisplayName());
        clonedFeature.setDescription(getDescription());
        clonedFeature.setSymbolicName(getSymbolicName());
        clonedFeature.getEnables().addAll(getEnables());
        clonedFeature.getApiJars().addAll(getApiJars());
        clonedFeature.getApiPackages().addAll(getApiPackages());
        clonedFeature.getSpiJars().addAll(getSpiJars());
        clonedFeature.getSpiPackages().addAll(getSpiPackages());
        clonedFeature.getConfigElements().addAll(getConfigElements());
        clonedFeature.setSuperseded(isSuperseded());
        clonedFeature.setSingleton(isSingleton());
        clonedFeature.getSupersededBy().addAll(getSupersededBy());
        clonedFeature.getIncludes().putAll(getIncludes());
        clonedFeature.getAutoProvisions().addAll(getAutoProvisions());
        clonedFeature.getCategoryElements().addAll(getCategoryElements());

        return clonedFeature;
    }

    /**
     * @param configElement the config element to add
     */
    protected void addConfigElement(String configElement) {
        this.configElements.add(configElement);
    }

    @Override
    public String toString() {
        return "Feature[" + (name != null ? name : symbolicName) + "]";
    }

    public static enum FeatureType {
        PUBLIC,
        PROTECTED,
        PRIVATE,
        AUTO,
        KERNEL;
    }
}
