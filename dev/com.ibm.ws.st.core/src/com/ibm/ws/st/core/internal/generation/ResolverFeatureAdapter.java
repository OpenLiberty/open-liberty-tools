/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.HeaderElementDefinition;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.st.core.internal.generation.Feature.FeatureType;

public class ResolverFeatureAdapter implements ProvisioningFeatureDefinition {

    private final Feature feature;

    public ResolverFeatureAdapter(Feature feature) {
        this.feature = feature;
    }

    @Override
    public String getApiServices() {
        return null;
    }

    @Override
    public AppForceRestart getAppForceRestart() {
        return null;
    }

    @Override
    public String getFeatureName() {
        if (feature.getFeatureType() == FeatureType.PUBLIC) {
            return feature.getName();
        }
        return feature.getSymbolicName();
    }

    @Override
    public String getSymbolicName() {
        return feature.getSymbolicName();
    }

    @Override
    public Version getVersion() {
        // This is required by the runtime resolver
        return Version.emptyVersion;
    }

    @Override
    public Visibility getVisibility() {
        FeatureType featureType = feature.getFeatureType();
        if (featureType != null) {
            switch (featureType) {
                case PUBLIC:
                    return Visibility.PUBLIC;
                case PROTECTED:
                    return Visibility.PROTECTED;
                case PRIVATE:
                case KERNEL:
                case AUTO:
                    return Visibility.PRIVATE;
            }
        }
        return null;
    }

    @Override
    public boolean isKernel() {
        return FeatureType.KERNEL == feature.getFeatureType();
    }

    @Override
    public String getBundleRepositoryType() {
        String featureInfoName = feature.getFeatureInfoName();
        if (featureInfoName != null) {
            return featureInfoName;
        }
        return "";
    }

    @Override
    public Collection<FeatureResource> getConstituents(SubsystemContentType subsystemContentType) {
        if (subsystemContentType == SubsystemContentType.FEATURE_TYPE) {
            Map<String, List<String>> includes = feature.getIncludes();
            Collection<FeatureResource> constituents = new ArrayList<FeatureResource>();
            for (String symbolicName : includes.keySet()) {
                constituents.add(new FeatureResourceAdapter(symbolicName, includes.get(symbolicName)));
            }
            return constituents;
        }
        return null;
    }

    @Override
    public File getFeatureChecksumFile() {
        return null;
    }

    @Override
    public File getFeatureDefinitionFile() {
        return null;
    }

    @Override
    public String getHeader(String arg0) {
        return null;
    }

    @Override
    public String getHeader(String arg0, Locale arg1) {
        return null;
    }

    @Override
    public Collection<HeaderElementDefinition> getHeaderElements(String arg0) {
        return null;
    }

    @Override
    public int getIbmFeatureVersion() {
        return -1;
    }

    @Override
    public String getIbmShortName() {
        if (feature.getFeatureType() == FeatureType.PUBLIC) {
            String name = feature.getName();
            int index = name.indexOf(':');
            if (index != -1) {
                name = name.substring(index + 1);
            }
            return name;
        }
        return null;
    }

    @Override
    public Collection<String> getIcons() {
        return null;
    }

    @Override
    public Collection<File> getLocalizationFiles() {
        return null;
    }

    @Override
    public String getSupersededBy() {
        // Not applicable for runtime resolver
        return null;
    }

    @Override
    public boolean isAutoFeature() {
        // Required by the runtime resolver
        return feature.getAutoProvisions().size() > 0;
    }

    @Override
    public boolean isCapabilitySatisfied(Collection<ProvisioningFeatureDefinition> featureDefinitionsToCheck) {
        // If it isn't an autofeature, it's satisfied.
        if (!feature.isAutoFeature()) {
            return true;
        }

        boolean isCapabilitySatisfied = true;
        Set<ProvisioningFeatureDefinition> satisfiedFeatureDefs = new HashSet<ProvisioningFeatureDefinition>();

        // Now we need to iterate over each of the filters, until we find we don't have a match.
        for (String autoProvision : feature.getAutoProvisions()) {
            try {
                Filter filter = FrameworkUtil.createFilter(autoProvision);
                Iterator<ProvisioningFeatureDefinition> featureDefIter = featureDefinitionsToCheck.iterator();

                // Now for each filter, iterate over each of the FeatureDefinition headers, checking to see if we have a match.
                boolean featureMatch = false;
                while (featureDefIter.hasNext() && !featureMatch) {
                    ProvisioningFeatureDefinition featureDef = featureDefIter.next();

                    // If we've already satisfied a capability with this FeatureDefinition, we don't need to use it again
                    if (!satisfiedFeatureDefs.contains(featureDef)) {

                        // We have a mismatch between the key the filter is using to look up the feature name and the property containing the name in the
                        // headers. So we need to add a new property for osgi.identity (filter key) that contains the value of the
                        // Subsystem-SymbolicName (manifest header).
                        // We also have to do this for the Subsystem-Type(manifest header) and the type (filter key).
                        Map<String, String> filterProps = new HashMap<String, String>();

                        filterProps.put("osgi.identity", featureDef.getSymbolicName());
                        filterProps.put("type", "osgi.subsystem.feature");

                        if (filter.matches(filterProps)) {
                            satisfiedFeatureDefs.add(featureDef);
                            featureMatch = true;
                        }
                    }
                }
                // Once we've checked all the FeatureDefinitions, apply the result to the isCapabilitySatisfied boolean,
                // so we stop processing as soon as we know we don't have a match.
                isCapabilitySatisfied = featureMatch;
                if (!isCapabilitySatisfied)
                    break;

            } catch (InvalidSyntaxException invalidSyntaxException) {
                invalidSyntaxException.printStackTrace();
            }
        }
        return isCapabilitySatisfied;
    }

    @Override
    public boolean isSingleton() {
        return feature.isSingleton();
    }

    @Override
    public boolean isSuperseded() {
        return feature.isSuperseded();
    }

    @Override
    public boolean isSupportedFeatureVersion() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<ProcessType> getProcessTypes() {
        return EnumSet.of(ProcessType.SERVER);
    }

    @Override
    public String toString() {
        return getFeatureName();
    }

    public class FeatureResourceAdapter implements FeatureResource {

        private final String symbolicName;
        private final List<String> tolerates;

        public FeatureResourceAdapter(String symbolicName, List<String> tolerates) {
            this.symbolicName = symbolicName;
            this.tolerates = tolerates;
        }

        @Override
        public String getSymbolicName() {
            return symbolicName;
        }

        @Override
        public Map<String, String> getDirectives() {
            return new HashMap<String, String>();
        }

        @Override
        public Map<String, String> getAttributes() {
            return new HashMap<String, String>();
        }

        @Override
        public String setExecutablePermission() {
            return null;
        }

        @Override
        public boolean isType(SubsystemContentType subsystemContentType) {
            return subsystemContentType == SubsystemContentType.FEATURE_TYPE;
        }

        @Override
        public VersionRange getVersionRange() {
            return null;
        }

        @Override
        public SubsystemContentType getType() {
            return SubsystemContentType.FEATURE_TYPE;
        }

        @Override
        public List<String> getTolerates() {
            return tolerates;
        }

        @Override
        public int getStartLevel() {
            return 0;
        }

        @Override
        public String getRawType() {
            return SubsystemContentType.FEATURE_TYPE.toString();
        }

        @Override
        public List<String> getOsList() {
            return null;
        }

        @Override
        public String getMatchString() {
            return null;
        }

        @Override
        public String getLocation() {
            return null;
        }

        @Override
        public String getFileEncoding() {
            return null;
        }

        @Override
        public String getExtendedAttributes() {
            return null;
        }

        @Override
        public String getBundleRepositoryType() {
            return null;
        }

        @Override
        public String getRequiredOSGiEE() {
            return null;
        }

    }

}
