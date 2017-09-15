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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.server.core.IRuntime;

/**
 * A project with Liberty as target runtime. Provides methods to get the runtime features used by the project,
 * and other methods useful to build the classpath needed by this project based on runtime features.
 *
 * This class implements IFacetedProjectListener to be able to refresh the cached information in case the
 * facets of the project are modified (added or removed).
 */

public class WebSphereRuntimeTargetedProject implements IFacetedProjectListener {

    private IProject project = null;
    private IFacetedProject facetedProject = null;
    private volatile boolean isRefreshing;
    private final ClasspathEntriesCache classpathEntriesCache;

    Set<String> features = null;

    public WebSphereRuntimeTargetedProject(IProject project) {
        this.project = project;
        try {
            facetedProject = ProjectFacetsManager.create(project);
            facetedProject.addListener(this, IFacetedProjectEvent.Type.POST_INSTALL, IFacetedProjectEvent.Type.POST_UNINSTALL, IFacetedProjectEvent.Type.POST_VERSION_CHANGE);
        } catch (CoreException e) {
            Trace.logError("Error creating faceted project for project " + project.getName(), e);
        }

        classpathEntriesCache = new ClasspathEntriesCache();
    }

    /**
     * Get the runtime associated to this project
     *
     * @return the runtime
     */
    public WebSphereRuntime getRuntime() {

        //TODO: Should we cache runtime?
        WebSphereRuntime wr = null;
        if (facetedProject != null) {
            org.eclipse.wst.common.project.facet.core.runtime.IRuntime rt = facetedProject.getPrimaryRuntime();
            if (rt != null) {
                IRuntime runtime = FacetUtil.getRuntime(rt);
                if (runtime != null)
                    wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            }
        }
        return wr;
    }

    /**
     * Get a map of the features used by this projects, which have conflict with other runtime features, based on the
     * information provided by the runtime
     *
     * @return a Map with conflicted features. The Key of the map is the conflicted feature id, the value is a set with the
     *         ids of the features in conflict.
     */
    public Map<String, Set<String>> getConflictedFeatures() {

        WebSphereRuntime wr = getRuntime();
        WebSphereRuntimeClasspathHelper helper = wr.getClasspathHelper();
        Map<String, Set<String>> featuresWithConflictMap = helper.getFeaturesWithConflicts();
        Set<String> featuresWithConconflict = featuresWithConflictMap.keySet();

        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for (String feature : getFeatures()) {
            if (featuresWithConconflict.contains(feature)) {
                result.put(feature, featuresWithConflictMap.get(feature));
            }
        }

        return result;
    }

    /**
     * Return the features required by this project, based on the facets of the project and the contents of
     * the project
     *
     * Note: It is recommended to call {@link #refresh()} or {@link #refresh(boolean)} before calling this method, because
     * those methods will populate the features required by this project, which are cached.
     *
     * @return a set of features
     */
    public Set<String> getFeatures() {
        if (features == null) {
            if (isRefreshing) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "The project " + project.getName() + " is refreshing but features are " + features);
            }
            return new HashSet<String>();
        }
        return features;
    }

    /**
     * Refreshes the information of this project, including the features required by this project. The refresh
     * is performed in the same thread of the caller. This is equivalent to calling <code>refresh(false)</code>.
     *
     */
    public void refresh() {
        isRefreshing = true;
        try {
            Set<String> result = findFeatures(project, null);
            features = Collections.unmodifiableSet(result);
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Found features for project " + project.getName() + " " + features);
            }
        } finally {
            isRefreshing = false;
        }
    }

    /**
     * Refreshes the information of this project, including the features required by this project. The refresh
     * can be performed in the same thread of the caller, or in a separate job.
     *
     * @param refreshInBackgound <code>true</code> if you want to update in a new job, <code>false</code>
     *            if you want to update in the same thread as the caller.
     */
    public void refresh(boolean refreshInBackgound) {
        if (!refreshInBackgound) {
            refresh();
        } else {
            final IProject tmpProject = project;
            Job job = new Job("Updating liberty runtime features for project: " + tmpProject.getName()) {

                @Override
                protected IStatus run(IProgressMonitor progressMonitor) {
                    refresh();
                    try {
                        tmpProject.refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
                    } catch (Exception e) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.INFO, "Error while trying to refresh: " + e.getMessage());
                        }
                    }
                    return new Status(IStatus.OK, Activator.PLUGIN_ID, "Updated features for project " + tmpProject.getName());
                }

            };
            // Using this scheduling rule causes that the classpath of the project is not correctly refreshed if the features required by a project
            // changes since the latest refresh, having a stale in-memory model (see RTC 150204).
            // The scheduling rule should not be necessary if we are only using the information in the facets to obtain the required features.
            //job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.setSystem(true);
            job.schedule();
        }
    }

    /*
     * Finds the features required by this project, based on the facets of the project. If no features are found, an
     * empty set is returned.
     */

    private Set<String> findFeatures(IProject project, IProgressMonitor progressMonitor) {
        WebSphereRuntime wr = getRuntime();
        if (wr != null) {
            List<String> features = FeatureResolverWrapper.findFeatures(project, wr, true, progressMonitor);
            return new HashSet<String>(features);
        }

        return Collections.emptySet();
    }

    /**
     * If the project requires runtime features that, according to the runtime information, have conflicts with other features,
     * this method returns a set of jars which are used by the conflicted features, but not used by the features required by the project, and
     * therefore, should not be part of the classpath of the project.
     *
     * @return a set of conflicted jars
     */
    public Set<String> getConflictedJars() {
        Set<String> conflictedJars = new HashSet<String>();
        Set<String> jarsUsedByReqFeatures = new HashSet<String>();
        Set<String> featuresRequiredByProject = getFeatures();
        WebSphereRuntime wr = getRuntime();
        if (wr == null)
            return conflictedJars;
        WebSphereRuntimeClasspathHelper cpHelper = wr.getClasspathHelper();

        for (String featureRequired : featuresRequiredByProject) {
            Set<String> jarsPerFeature = cpHelper.getConflictedJarsPerFeature(featureRequired);
            if (jarsPerFeature != null) {
                conflictedJars.addAll(jarsPerFeature);
            }
            jarsPerFeature = cpHelper.getApiJars(featureRequired);
            if (jarsPerFeature != null) {
                jarsUsedByReqFeatures.addAll(jarsPerFeature);
            }
        }

        //If a jar is used by any of the required features, do not consider it as conflicted.
        conflictedJars.removeAll(jarsUsedByReqFeatures);

        return conflictedJars;
    }

    /** {@inheritDoc} */
    @Override
    public void handleEvent(IFacetedProjectEvent event) {
        refresh(true);
    }

    class ClasspathEntriesCache {

        private IClasspathEntry[] entries;
        private long lastAccess;
        private static final long TIMEOUT = 2000; // 2 seconds

        public ClasspathEntriesCache() {
            entries = new IClasspathEntry[0];
            lastAccess = System.currentTimeMillis();
        }

        public IClasspathEntry[] getEntries() {
            long time = System.currentTimeMillis();
            if (time - lastAccess > TIMEOUT) {
                entries = new IClasspathEntry[0];
            }
            lastAccess = time;
            return entries;
        }

        public void setEntries(IClasspathEntry[] entries) {
            this.entries = entries;
            lastAccess = System.currentTimeMillis();
        }

    }

    public ClasspathEntriesCache getClasspathEntriesCache() {
        return classpathEntriesCache;
    }

}
