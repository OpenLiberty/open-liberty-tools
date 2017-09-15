/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class ClasspathExtension {
    private static final String EXTENSION_POINT = "classpathExtensions";

    private boolean isEmptyContainer = false;
    private final IProjectFacet[] facets;
    private final String[] natures;

    /**
     * Load classpath extensions.
     */
    public static void createClasspathExtensions(List<ClasspathExtension> includeList, List<ClasspathExtension> emptyContainerlist) {
        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "->- Loading .classpathExtensions extension point ->-");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, EXTENSION_POINT);

        for (IConfigurationElement ce : cf) {
            String isEmptyContainerAttr = ce.getAttribute("isEmptyContainer");
            boolean isEmptyContainer = isEmptyContainerAttr == null ? false : Boolean.valueOf(isEmptyContainerAttr).booleanValue();
            try {
                // load module types
                if (isEmptyContainer) {
                    emptyContainerlist.add(new ClasspathExtension(ce));
                } else {
                    includeList.add(new ClasspathExtension(ce));
                }

                if (Trace.ENABLED)
                    Trace.trace(Trace.EXTENSION_POINT, "  Loaded classpathExtension: " + ce.getAttribute("id"));
            } catch (Throwable t) {
                Trace.logError("Could not load classpathExtension: " + ce.getAttribute("id"), t);
            }
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "-<- Done loading .classpathExtensions extension point -<-");
    }

    public ClasspathExtension(IConfigurationElement element) {
        String isEmptyContainerAttr = element.getAttribute("isEmptyContainer");
        isEmptyContainer = isEmptyContainerAttr == null ? false : Boolean.valueOf(isEmptyContainerAttr).booleanValue();
        IConfigurationElement[] ce = element.getChildren("facet");
        int size = ce.length;
        facets = new IProjectFacet[size];
        for (int i = 0; i < size; i++) {
            String facet = ce[i].getAttribute("type");
            facets[i] = ProjectFacetsManager.getProjectFacet(facet);
        }

        ce = element.getChildren("nature");
        size = ce.length;
        natures = new String[size];
        for (int i = 0; i < size; i++)
            natures[i] = ce[i].getAttribute("type");
    }

    /**
     * Returns <code>true</code> if the project contains one of the supported facet in this extension, and
     * <code>false</code> otherwise.
     * 
     * @param type
     * @return <code>true</code> if the project contains one of the supported facet in this extension, and
     *         <code>false</code> otherwise
     */
    public final boolean containsSupportedFacet(IProject project) {
        IFacetedProject facetedProject = null;
        try {
            facetedProject = ProjectFacetsManager.create(project);
        } catch (CoreException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Failed to create faceted project for: " + project, e);
        }
        if (facetedProject == null)
            return false;

        for (IProjectFacet facet : facets) {
            if (facetedProject.hasProjectFacet(facet))
                return true;
        }
        return false;
    }

    /**
     * Return if this extension indicates the condition listed is for emptying the classpath extension.
     * 
     * @return true if this extension indicates the condition listed is for emptying the classpath extension; otherwise, return false.
     */
    public final boolean isEmptyContainer() {
        return isEmptyContainer;
    }

    /**
     * Returns <code>true</code> if the nature is supported by this extension, and
     * <code>false</code> otherwise.
     * 
     * @param type
     * @return <code>true</code> if the nature is supported by this extension, and
     *         <code>false</code> otherwise
     */
    public final boolean supportsNature(IProject project) {
        for (String s : natures) {
            try {
                if (project.getNature(s) != null)
                    return true;
            } catch (CoreException e) {
                // do nothing
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the facet is supported by this extension, and
     * <code>false</code> otherwise.
     * 
     * @param type
     * @return <code>true</code> if the facet is supported by this extension, and
     *         <code>false</code> otherwise
     */
    public final boolean supportsFacet(IProjectFacet projectFacet) {
        for (IProjectFacet facet : facets) {
            if (facet.getId().equals(projectFacet.getId()))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ClasspathExtension [" + getClass().toString() + "]";
    }
}