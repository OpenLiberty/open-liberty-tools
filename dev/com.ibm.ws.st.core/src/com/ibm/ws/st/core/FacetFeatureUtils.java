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
package com.ibm.ws.st.core;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.ibm.ws.st.core.internal.FacetFeatureMap;

/**
 * Utility class for getting Liberty features and related facets.
 */
public final class FacetFeatureUtils {

    /**
     * Analyzes the given project and determines which Liberty features are
     * required for that project and then maps them back to facets. It
     * includes all facets, even those that might already be set on the project.
     * All versions of the facet that are supported by the Liberty runtime
     * feature are returned. If the project is not targeted to a Liberty
     * runtime then no facets will be returned.
     * 
     * @param project The project to analyze.
     * @param monitor Progress monitor.
     * @return A map of facets to facet versions. May be empty. Will not be null.
     */
    public static Map<IProjectFacet, Set<IProjectFacetVersion>> getFacets(IProject project, IProgressMonitor monitor) {
        return FacetFeatureMap.getInstance().getFacets(project, monitor);
    }
}
