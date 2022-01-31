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
package com.ibm.ws.st.core.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.generation.Feature;
import com.ibm.ws.st.core.internal.generation.ResolverFeatureAdapter;

public class RuntimeFeatureResolver {

    public static ResolverResult resolve(WebSphereRuntime wsRuntime, List<String> features) {

        // Data structures to be used for resolving
        final HashMap<String, Feature> featureMap = FeatureList.getFeatureMap(wsRuntime);
        final HashMap<String, Feature> allFeaturesKeyedBySymbolicName = FeatureList.getAllFeaturesKeyedBySymbolicName(wsRuntime);

        // Obtain resolver
        FeatureResolver featureResolver = Activator.getFeatureResolver();

        // Create feature repository
        Repository repository = new Repository() {

            @Override
            public ProvisioningFeatureDefinition getFeature(String featureNameOrSymbolicName) { // either feature name or symbolic
                Feature feature = featureMap.get(featureNameOrSymbolicName);
                if (feature == null) {
                    feature = allFeaturesKeyedBySymbolicName.get(featureNameOrSymbolicName);
                }
                if (feature != null) {
                    return new ResolverFeatureAdapter(feature);
                }
                return null;
            }

            @Override
            public List<String> getConfiguredTolerates(String arg0) {
                return Collections.emptyList();
            }

            @Override
            public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
                List<ProvisioningFeatureDefinition> autoFeatures = new ArrayList<ProvisioningFeatureDefinition>();
                for (Feature feature : allFeaturesKeyedBySymbolicName.values()) {
                    ResolverFeatureAdapter resolverFeatureAdapter = new ResolverFeatureAdapter(feature);
                    if (resolverFeatureAdapter.isAutoFeature()) {
                        autoFeatures.add(resolverFeatureAdapter);
                    }
                }
                return autoFeatures;
            }
        };

        // Resolve (the feature resolver only handles canonical names)
        List<String> canonicalFeatures = FeatureList.getCanonicalFeatures(features, wsRuntime);
        Result result = featureResolver.resolveFeatures(repository, canonicalFeatures, Collections.<String> emptySet(), true);

        // Process conflicts
        Set<FeatureConflict> featureConflicts = new HashSet<FeatureConflict>();
        for (Entry<String, Collection<Chain>> conflict : result.getConflicts().entrySet()) {
            String conflictA = null;
            String conflictB = null;
            List<String> dependencyChainA = new ArrayList<String>();
            List<String> dependencyChainB = new ArrayList<String>();

            for (Chain chain : conflict.getValue()) {
                List<String> candidates = chain.getCandidates();
                if (conflictA == null) {
                    conflictA = candidates.get(0);
                    if (chain.getChain().isEmpty()) {
                        dependencyChainA.add(conflictA);
                    } else {
                        dependencyChainA.addAll(chain.getChain());
                        dependencyChainA.add(conflictA);
                    }
                } else if (!conflictA.equals(candidates.get(0))) {
                    conflictB = candidates.get(0);
                    if (chain.getChain().isEmpty()) {
                        dependencyChainB.add(conflictB);
                    } else {
                        dependencyChainB.addAll(chain.getChain());
                        dependencyChainB.add(conflictB);
                    }
                    break;
                }
            }
            featureConflicts.add(new FeatureConflict(dependencyChainA, dependencyChainB));
        }
        return new ResolverResult(featureConflicts, result.getMissing(), result.getNonPublicRoots(), result.getResolvedFeatures());
    }

    public static class ResolverResult {

        private final Set<FeatureConflict> featureConflicts;
        private final Set<String> missingFeatures;
        private final Set<String> nonPublicFeatures;
        private final Set<String> resolvedFeatures;

        public ResolverResult(Set<FeatureConflict> featureConflicts, Set<String> missingFeatures, Set<String> nonPublicFeatures, Set<String> resolvedFeatures) {
            this.featureConflicts = featureConflicts;
            this.missingFeatures = missingFeatures;
            this.nonPublicFeatures = nonPublicFeatures;
            this.resolvedFeatures = resolvedFeatures;
        }

        public Set<FeatureConflict> getFeatureConflicts() {
            return featureConflicts;
        }

        public Set<String> getMissingFeatures() {
            return missingFeatures;
        }

        public Set<String> getNonPublicFeatures() {
            return nonPublicFeatures;
        }

        public Set<String> getResolvedFeatures() {
            return resolvedFeatures;
        }

    }

    public static String getDependencyChainString(List<String> dependencyChain, WebSphereRuntime wsRuntime) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = dependencyChain.iterator();
        if (iterator.hasNext()) {
            stringBuilder.append('\'' + FeatureList.getPublicFeatureName(iterator.next(), wsRuntime) + '\'');
        }
        while (iterator.hasNext()) {
            stringBuilder.append(" --> '").append(FeatureList.getPublicFeatureName(iterator.next(), wsRuntime) + '\'');
        }
        if (dependencyChain.size() > 1) {
            stringBuilder.insert(0, "[");
            stringBuilder.append("]");
        }
        return stringBuilder.toString();
    }

    public static class FeatureConflict implements Serializable {

        private static final long serialVersionUID = 9091468059230107746L;
        private final List<String> dependencyChainA;
        private final List<String> dependencyChainB;

        public FeatureConflict(List<String> dependencyChainA, List<String> dependencyChainB) {
            this.dependencyChainA = dependencyChainA;
            this.dependencyChainB = dependencyChainB;
        }

        public String getConfiguredFeatureA() {
            return dependencyChainA.get(0);
        }

        public String getConfiguredFeatureB() {
            return dependencyChainB.get(0);
        }

        public String getConflictingFeatureA() {
            return dependencyChainA.get(dependencyChainA.size() - 1);
        }

        public String getConflictingFeatureB() {
            return dependencyChainB.get(dependencyChainB.size() - 1);
        }

        public List<String> getDependencyChainA() {
            return dependencyChainA;
        }

        public List<String> getDependencyChainB() {
            return dependencyChainB;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((dependencyChainA == null) ? 0 : dependencyChainA.hashCode());
            result = prime * result + ((dependencyChainB == null) ? 0 : dependencyChainB.hashCode());
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FeatureConflict other = (FeatureConflict) obj;
            if (dependencyChainA == null || other.dependencyChainA == null) {
                return dependencyChainA == other.dependencyChainA;
            } else if (!(dependencyChainA.containsAll(other.dependencyChainA) || dependencyChainA.containsAll(other.dependencyChainB)))
                return false;

            if (dependencyChainB == null || other.dependencyChainB == null) {
                return dependencyChainB == other.dependencyChainB;
            } else if (!(dependencyChainB.containsAll(other.dependencyChainB) || dependencyChainB.containsAll(other.dependencyChainA)))
                return false;
            return true;
        }

    }

}
