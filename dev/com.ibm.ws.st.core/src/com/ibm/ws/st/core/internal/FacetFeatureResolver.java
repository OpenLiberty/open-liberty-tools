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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

/**
 * Uses the FacetFeatureMap to determine the required features for a set of modules
 * based on the enabled facets for those modules.
 */
public class FacetFeatureResolver extends FeatureResolver {

    @Override
    public void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList, FeatureSet existingFeatures,
                                    RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor) {
        if (moduleList == null || moduleList.isEmpty())
            return;

        // Get the facets for all of the modules so they can be processed together
        Map<IProjectFacetVersion, List<IModule[]>> facetMap = collectFacets(moduleList);
        if (facetMap.isEmpty())
            return;

        IProjectFacetVersion[] projectFacets = facetMap.keySet().toArray(new IProjectFacetVersion[facetMap.size()]);
        Arrays.sort(projectFacets, new Comparator<IProjectFacetVersion>() {
            @Override
            public int compare(IProjectFacetVersion facet1, IProjectFacetVersion facet2) {
                int result = facet1.getProjectFacet().getId().compareTo(facet2.getProjectFacet().getId());
                if (result == 0) {
                    float version1 = Float.parseFloat(facet1.getVersionString());
                    float version2 = Float.parseFloat(facet2.getVersionString());
                    result = Float.compare(version1, version2);
                }
                return result;
            }
        });

        int i = 0;
        while (i < projectFacets.length) {
            // Collect up the facets with the same id
            List<IProjectFacetVersion> facetList = new ArrayList<IProjectFacetVersion>();
            String id = projectFacets[i].getProjectFacet().getId();
            facetList.add(projectFacets[i]);
            while (++i < projectFacets.length && id.equals(projectFacets[i].getProjectFacet().getId())) {
                facetList.add(projectFacets[i]);
            }

            // Start with the highest facet version and find the lowest version of the
            // feature(s) that supports it.  Remove any facet versions from the list
            // that are supported by that feature and then repeat until the list is
            // empty.
            while (!facetList.isEmpty()) {
                IProjectFacetVersion facet = facetList.remove(facetList.size() - 1);
                FacetFeatureMap.Entry facetFeatureEntry = FacetFeatureMap.getInstance().getFeatures(facet);
                if (facetFeatureEntry == null)
                    continue;

                Set<IModule[]> modulesToAdd = new HashSet<IModule[]>();
                modulesToAdd.addAll(facetMap.get(facet));

                if (facetList.size() > 0) {
                    // Remove any other facets from the list that are covered by the feature(s)
                    // and collect up all of the modules.
                    try {
                        Set<IProjectFacetVersion> versions = facet.getProjectFacet().getVersions(facetFeatureEntry.getVersion());
                        List<IProjectFacetVersion> removeList = new ArrayList<IProjectFacetVersion>();
                        for (IProjectFacetVersion version : versions) {
                            for (IProjectFacetVersion listVersion : facetList) {
                                if (listVersion.equals(version)) {
                                    removeList.add(listVersion);
                                    modulesToAdd.addAll(facetMap.get(listVersion));
                                }
                            }
                        }
                        facetList.removeAll(removeList);
                    } catch (CoreException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Failed to get facets versions for: " + facet, e);
                    }
                }

                for (String feature : facetFeatureEntry.getFeatures()) {
                    FeatureResolver.checkAndAddFeature(requiredFeatures, existingFeatures, wr, feature, new ArrayList<IModule[]>(modulesToAdd), includeAll);
                }
            }
        }
    }

    private Map<IProjectFacetVersion, List<IModule[]>> collectFacets(List<IModule[]> moduleList) {
        Map<IProjectFacetVersion, List<IModule[]>> facetMap = new HashMap<IProjectFacetVersion, List<IModule[]>>();

        for (IModule[] module : moduleList) {
            IModule m = module[module.length - 1];
            IProject project = m.getProject();
            if (project == null)
                continue;

            try {
                IFacetedProject fp = ProjectFacetsManager.create(project);
                if (fp != null) {
                    Set<IProjectFacetVersion> facetSet = fp.getProjectFacets();
                    for (IProjectFacetVersion facet : facetSet) {
                        List<IModule[]> modules = facetMap.get(facet);
                        if (modules == null) {
                            modules = new ArrayList<IModule[]>();
                            facetMap.put(facet, modules);
                        }
                        modules.add(module);
                    }
                }
            } catch (CoreException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to get facets for: " + project.getName(), e);
            }
        }

        return facetMap;
    }
}
