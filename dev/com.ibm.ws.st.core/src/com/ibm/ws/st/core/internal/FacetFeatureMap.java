/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerUtil;

/**
 * Reads in the facetFeatureMapping extensions and provides utilities to access
 * the information.
 */
public class FacetFeatureMap {

    private static final String EXTENSION_POINT = "facetFeatureMapping";

    private final Set<Entry> facetFeatureSet = new HashSet<Entry>();

    private static final FacetFeatureMap instance = new FacetFeatureMap();

    private FacetFeatureMap() {
        loadFeatureMap();
    }

    public static FacetFeatureMap getInstance() {
        return instance;
    }

    /**
     * Load the facetFeatureMapping extensions.
     */
    private void loadFeatureMap() {
        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "->- Loading .facetFeatureMapping extension point ->-");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, EXTENSION_POINT);

        for (IConfigurationElement ce : cf) {
            String featuresAttr = ce.getAttribute("features");
            String[] features = listToArray(featuresAttr, ",");
            IConfigurationElement[] facetElems = ce.getChildren("facet");
            for (IConfigurationElement facetElem : facetElems) {
                Entry entry = new Entry(features, facetElem.getAttribute("id"), facetElem.getAttribute("version"));
                facetFeatureSet.add(entry);
            }
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "-<- Done loading .facetFeatureMapping extension point -<-");
    }

    private String[] listToArray(String list, String separator) {
        List<String> strings = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(list, separator);
        while (st.hasMoreTokens()) {
            String s = st.nextToken().trim();
            if (!s.isEmpty()) {
                strings.add(s);
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Get the entry with the lowest version of the feature(s) that support the given facet
     */
    public Entry getFeatures(IProjectFacetVersion facetVersion) {
        Entry result = null;
        for (Entry entry : facetFeatureSet) {
            if (entry.matches(facetVersion)) {
                if (result == null || isLower(entry.getFeatures(), result.getFeatures()))
                    result = entry;
            }
        }
        return result;
    }

    /**
     * Get the facet feature entries associated with the given project
     */
    public Map<IProjectFacet, Set<IProjectFacetVersion>> getFacets(IProject project, IProgressMonitor monitor) {
        IFacetedProject facetedProject = null;
        try {
            facetedProject = ProjectFacetsManager.create(project);
        } catch (CoreException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Failed to create faceted project for: " + project, e);
        }
        if (facetedProject == null)
            return Collections.emptyMap();

        // Get the targeted runtime for the project
        WebSphereRuntime wsRuntime = null;
        Set<org.eclipse.wst.common.project.facet.core.runtime.IRuntime> facetRuntimes = facetedProject.getTargetedRuntimes();
        for (org.eclipse.wst.common.project.facet.core.runtime.IRuntime facetRuntime : facetRuntimes) {
            IRuntime runtime = FacetUtil.getRuntime(facetRuntime);
            if (runtime != null) {
                wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                if (wsRuntime != null)
                    break;
            }
        }
        if (wsRuntime == null)
            return Collections.emptyMap();

        // Get the required features for the project
        IModule[] module = ServerUtil.getModules(project);
        RequiredFeatureMap requiredFeatureMap = FeatureResolverWrapper.getAllRequiredFeatures(wsRuntime, Collections.singletonList(module), null, null, true, monitor);

        // Resolve any features without versions
        List<FeatureResolverFeature> noVersionFeatures = new ArrayList<FeatureResolverFeature>();
        for (FeatureResolverFeature required : requiredFeatureMap.getFeatures()) {
            if (!required.getName().contains("-")) {
                noVersionFeatures.add(required);
            }
        }
        if (!noVersionFeatures.isEmpty()) {
            FeatureSet installedFeatures = wsRuntime.getInstalledFeatures();
            for (FeatureResolverFeature noVersionFeature : noVersionFeatures) {
                // Check first if feature already exists in required features without a version
                boolean remove = false;
                for (FeatureResolverFeature required : requiredFeatureMap.getFeatures()) {
                    String requiredName = required.getName();
                    if (requiredName.contains("-") && requiredName.toLowerCase().startsWith(noVersionFeature.getName().toLowerCase())) {
                        remove = true;
                        break;
                    }
                }
                if (remove) {
                    // Feature exists with a version so remove the no version one
                    requiredFeatureMap.removeFeature(noVersionFeature);
                } else {
                    // Replace the no version feature with a resolved feature
                    String feature = installedFeatures.resolveToHigherVersion(noVersionFeature.getName());
                    if (feature != null) {
                        requiredFeatureMap.replaceFeature(noVersionFeature, new FeatureResolverFeature(feature));
                    }
                }
            }
        }

        // Remove any contained features
        FeatureResolverFeature[] allContainedFeatures = FeatureResolverWrapper.getAllContainedFeatures(wsRuntime, Collections.singletonList(module), monitor);
        if (allContainedFeatures.length > 0) {
            for (FeatureResolverFeature required : requiredFeatureMap.getFeatures()) {
                for (FeatureResolverFeature contained : allContainedFeatures) {
                    if (required.equals(contained)) {
                        requiredFeatureMap.removeFeature(required);
                    } else if (!contained.getName().contains("-") && required.getName().contains("-")
                               && required.getName().toLowerCase().startsWith(contained.getName().toLowerCase() + "-")) {
                        requiredFeatureMap.removeFeature(required);
                    }
                }
            }
        }

        // Map the features back to facets
        Map<IProjectFacet, Set<IProjectFacetVersion>> facetMap = new HashMap<IProjectFacet, Set<IProjectFacetVersion>>();
        for (FeatureResolverFeature feature : requiredFeatureMap.getFeatures()) {
            List<Entry> entries = getEntriesForFeature(feature.getName());
            for (Entry entry : entries) {
                IProjectFacet facet = ProjectFacetsManager.getProjectFacet(entry.getId());
                try {
                    Set<IProjectFacetVersion> facetVersions = facet.getVersions(entry.getVersion());
                    facetMap.put(facet, facetVersions);
                } catch (CoreException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Failed to get versions for facet: " + facet.getId() + " and version string: " + entry.getVersion() + "(" + e + ").");
                }
            }
        }

        return facetMap;
    }

    private List<Entry> getEntriesForFeature(String feature) {
        List<Entry> entries = new ArrayList<Entry>();
        for (Entry entry : facetFeatureSet) {
            if (entry.containsFeature(feature))
                entries.add(entry);
        }
        return entries;
    }

    // Check if the new set is of lower version then the compare set
    private boolean isLower(String[] newSet, String[] compareSet) {
        // If anything in the compare set is a lower version than in the new set
        // return false (since in this case the new set can't be a lower version).
        // Otherwise return true.
        for (String startStr : compareSet) {
            for (String newStr : newSet) {
                if (FeatureUtil.isLowerVersion(startStr, newStr)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static class Entry {
        private final String[] features;
        private final String id;
        private final String version;

        protected Entry(String[] features, String id, String version) {
            this.features = features;
            this.id = id;
            this.version = version;
        }

        public String[] getFeatures() {
            return features;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public boolean matches(IProjectFacetVersion facetVersion) {
            IProjectFacet facet = facetVersion.getProjectFacet();
            if (!id.equals(facet.getId()))
                return false;
            try {
                Set<IProjectFacetVersion> versions = facetVersion.getProjectFacet().getVersions(version);
                for (IProjectFacetVersion version : versions) {
                    if (facetVersion.getVersionString().equals(version.getVersionString()))
                        return true;
                }
            } catch (CoreException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to get facet versions for facet:" + facet.getId() + ", and version expression: " + version, e);
            }
            return false;
        }

        public boolean containsFeature(String feature) {
            for (String f : features) {
                if (f.equalsIgnoreCase(feature))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (String feature : features) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(feature);
            }
            return "Features: [" + builder.toString() + "], Facet: " + id + " [" + version + "]";
        }
    }

}
