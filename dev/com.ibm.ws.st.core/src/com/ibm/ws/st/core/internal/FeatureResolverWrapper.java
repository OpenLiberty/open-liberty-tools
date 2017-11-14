/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ModuleDelegate;

import com.ibm.ws.st.core.internal.config.FeatureList;

public class FeatureResolverWrapper {
    private static final String EXTENSION_POINT = "requiredFeatures";

    private static FeatureResolverWrapper[] featureExtensions;
    private static Map<String, String[]> alternativeFeatureMap = new HashMap<String, String[]>();

    static {
        alternativeFeatureMap.put("jsf", new String[] { "jsfContainer" });
    }

    private final IConfigurationElement configElement;
    private final String[] moduleTypes;
    private final String[] facetIds;
    private final String[] facetVersions;
    private final IPath[] paths;
    private final FeatureResolverFeature[] features;
    private FeatureResolver delegate;
    private int priority = 50;

    protected static synchronized FeatureResolverWrapper[] getFeatureExtensions() {
        if (featureExtensions != null)
            return featureExtensions;

        featureExtensions = FeatureResolverWrapper.loadRequiredFeatureExtensions();
        return featureExtensions;
    }

    /**
     * Load the required feature extensions.
     */
    private static FeatureResolverWrapper[] loadRequiredFeatureExtensions() {
        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "->- Loading .requiredFeatures extension point ->-");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, EXTENSION_POINT);
        List<FeatureResolverWrapper> list = new ArrayList<FeatureResolverWrapper>(cf.length);

        for (IConfigurationElement ce : cf) {
            try {
                list.add(new FeatureResolverWrapper(ce));

                if (Trace.ENABLED)
                    Trace.trace(Trace.EXTENSION_POINT, "  Loaded requiredFeature: " + ce.getAttribute("id"));
            } catch (Throwable t) {
                Trace.logError("Could not load requiredFeature: " + ce.getAttribute("id"), t);
            }
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "-<- Done loading .requiredFeatures extension point -<-");

        FeatureResolverWrapper[] extArray = list.toArray(new FeatureResolverWrapper[list.size()]);

        Arrays.sort(extArray, new Comparator<FeatureResolverWrapper>() {
            @Override
            public int compare(FeatureResolverWrapper o1, FeatureResolverWrapper o2) {
                if (o1.getPriority() > o2.getPriority())
                    return 1;
                if (o1.getPriority() < o2.getPriority())
                    return -1;
                return 0;
            }
        });
        return extArray;
    }

    public FeatureResolverWrapper(IConfigurationElement element) {
        this.configElement = element;

        features = convertToArray(element.getAttribute("features"));

        String priorityStr = element.getAttribute("priority");
        if (priorityStr != null && !priorityStr.isEmpty()) {
            try {
                priority = Integer.parseInt(priorityStr);
            } catch (NumberFormatException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Required feature extension priority is not a valid int: " + priorityStr);
            }
        }

        IConfigurationElement[] ce = element.getChildren("module");
        int size = ce.length;
        moduleTypes = new String[size];
        for (int i = 0; i < size; i++)
            moduleTypes[i] = ce[i].getAttribute("type");

        ce = element.getChildren("facet");
        size = ce.length;
        facetIds = new String[size];
        facetVersions = new String[size];
        for (int i = 0; i < size; i++) {
            facetIds[i] = ce[i].getAttribute("id");
            facetVersions[i] = ce[i].getAttribute("version");
        }

        ce = element.getChildren("content");
        size = ce.length;
        paths = new IPath[size];
        for (int i = 0; i < size; i++)
            paths[i] = new Path(ce[i].getAttribute("path"));
    }

    private static FeatureResolverFeature[] convertToArray(String str) {
        if (str == null || str.trim().length() == 0)
            return null;

        List<FeatureResolverFeature> list = new ArrayList<FeatureResolverFeature>();

        StringTokenizer st = new StringTokenizer(str, ",");
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s != null && s.length() > 0) {
                list.add(new FeatureResolverFeature(s.trim()));
            }
        }

        FeatureResolverFeature[] s = new FeatureResolverFeature[list.size()];
        s = list.toArray(s);
        return s;
    }

    /**
     * Performs a check to see if the given path exists in the module.
     * 
     * @param module a module
     * @param path a path
     * @param monitor a progress monitor
     * @return <code>true</code> if the module contains the file, and <code>false</code> otherwise
     */
    private static boolean moduleContains(IModule module, IPath path) {
        ModuleDelegate moduleDelegate = (ModuleDelegate) module.loadAdapter(ModuleDelegate.class, null);
        if (moduleDelegate == null)
            return false;

        try {
            IModuleResource[] members = moduleDelegate.members();
            return contains(members, path);
        } catch (CoreException ce) {
            Trace.trace(Trace.INFO, "Problem scanning module members for jsp files", ce);
            return false;
        }
    }

    private static boolean contains(IModuleResource[] members, IPath path) {
        for (IModuleResource res : members) {
            IPath resPath = res.getModuleRelativePath().append(res.getName());
            if (path.equals(resPath))
                return true;
            if (res instanceof IModuleFolder && resPath.isPrefixOf(path)) {
                IModuleFolder folder = (IModuleFolder) res;
                if (contains(folder.members(), path))
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the module type is supported by this extension, and
     * <code>false</code> otherwise.
     * 
     * @param type
     * @return <code>true</code> if the module type is supported by this extension, and
     *         <code>false</code> otherwise
     */
    protected boolean supports(IModule[] module) {
        IModule m = module[module.length - 1];
        String moduleType = m.getModuleType().getId();
        if (moduleTypes != null && moduleTypes.length > 0) {
            boolean found = false;
            for (String s : moduleTypes) {
                if (s.equals(moduleType)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }

        if (facetIds != null && facetIds.length > 0) {
            boolean found = false;
            IProject project = m.getProject();
            if (project != null) {
                int size = facetIds.length;
                for (int i = 0; i < size; i++) {
                    try {
                        if (facetVersions[i] == null) {
                            if (FacetedProjectFramework.hasProjectFacet(project, facetIds[i])) {
                                found = true;
                                break;
                            }
                        } else {
                            if (FacetedProjectFramework.hasProjectFacet(project, facetIds[i], facetVersions[i])) {
                                found = true;
                                break;
                            }
                        }
                    } catch (CoreException ce) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Problem checking facet for required features: " + facetIds[i], ce);
                    }
                }
            }
            if (!found)
                return false;
        }

        if (paths != null && paths.length > 0) {
            for (IPath path : paths) {
                if (!moduleContains(m, path))
                    return false;
            }
        }
        return true;
    }

    protected void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList, FeatureSet existingFeatures,
                                       RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor) {
        if (features != null) {
            for (FeatureResolverFeature feature : features) {
                FeatureResolver.checkAndAddFeature(requiredFeatures, existingFeatures, wr, feature, moduleList, includeAll);
            }
            return;
        }
        try {
            getDelegate().getRequiredFeatures(wr, moduleList, deltaList, existingFeatures, requiredFeatures, includeAll, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling feature resolver for required features", t);
        }
    }

    protected FeatureResolverFeature[] getContainedFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, IProgressMonitor monitor) {
        String clas = configElement.getAttribute("class");
        if (clas == null || clas.length() == 0)
            return null;

        try {
            return getDelegate().getContainedFeatures(wr, moduleList, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling feature resolver for contained features", t);
            return null;
        }
    }

    protected int getPriority() {
        return priority;
    }

    private FeatureResolver getDelegate() {
        if (delegate == null) {
            try {
                delegate = (FeatureResolver) configElement.createExecutableExtension("class");
            } catch (Throwable t) {
                Trace.logError("Could not create delegate", t);
                delegate = new FeatureResolver() {
                    @Override
                    public void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList, FeatureSet existingFeatures,
                                                    RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor) {
                        return; // no features required
                    }
                };
            }
        }
        return delegate;
    }

    public static RequiredFeatureMap getAllRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList,
                                                            FeatureSet existingFeatures, boolean includeAll, IProgressMonitor monitor) {
        RequiredFeatureMap featureMap = new RequiredFeatureMap();

        FeatureResolverWrapper[] featureExtensions = getFeatureExtensions();
        for (FeatureResolverWrapper fe : featureExtensions) {
            List<IModule[]> supportedModules = new ArrayList<IModule[]>(moduleList.size());
            List<IModuleResourceDelta[]> supportedDeltas = deltaList == null ? null : new ArrayList<IModuleResourceDelta[]>(deltaList.size());
            for (int i = 0; i < moduleList.size(); i++) {
                if (fe.supports(moduleList.get(i))) {
                    supportedModules.add(moduleList.get(i));
                    if (deltaList != null && supportedDeltas != null)
                        supportedDeltas.add(deltaList.get(i));
                }
            }
            if (!supportedModules.isEmpty()) {
                fe.getRequiredFeatures(wr, supportedModules, supportedDeltas, existingFeatures, featureMap, includeAll, monitor);
            }
        }

        addAcceptableAlternativesToFeatures(featureMap);

        return featureMap;
    }

    public static RequiredFeatureMap getAllRequiredFeaturesBasedOnFacets(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList,
                                                                         FeatureSet existingFeatures, boolean includeAll, IProgressMonitor monitor) {
        RequiredFeatureMap featureMap = new RequiredFeatureMap();

        FacetFeatureResolver facetFeatureResolver = new FacetFeatureResolver();
        facetFeatureResolver.getRequiredFeatures(wr, moduleList, deltaList, existingFeatures, featureMap, includeAll, monitor);

        addAcceptableAlternativesToFeatures(featureMap);

        return featureMap;
    }

    public static FeatureResolverFeature[] getAllContainedFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, IProgressMonitor monitor) {
        List<FeatureResolverFeature> list = new ArrayList<FeatureResolverFeature>();

        FeatureResolverWrapper[] featureExtensions = getFeatureExtensions();
        for (FeatureResolverWrapper fe : featureExtensions) {
            List<IModule[]> supportedModules = new ArrayList<IModule[]>(moduleList.size());
            for (int i = 0; i < moduleList.size(); i++) {
                if (fe.supports(moduleList.get(i))) {
                    supportedModules.add(moduleList.get(i));
                }
            }
            if (!supportedModules.isEmpty()) {
                FeatureResolverFeature[] features = fe.getContainedFeatures(wr, supportedModules, monitor);
                if (features != null && features.length > 0) {
                    for (FeatureResolverFeature frf : features) {
                        if (!list.contains(frf)) {
                            list.add(frf);
                        }
                    }
                }
            }
        }
        return list.toArray(new FeatureResolverFeature[list.size()]);
    }

    public static FeatureResolverFeature[] getAllContainedFeaturesBasedOnFacets(WebSphereRuntime wr, List<IModule[]> moduleList, IProgressMonitor monitor) {
        List<FeatureResolverFeature> list = new ArrayList<FeatureResolverFeature>();

        FacetFeatureResolver facetFeatureResolver = new FacetFeatureResolver();
        FeatureResolverFeature[] features = facetFeatureResolver.getContainedFeatures(wr, moduleList, monitor);
        if (features != null && features.length > 0) {
            for (FeatureResolverFeature frf : features) {
                if (!list.contains(frf)) {
                    list.add(frf);
                }
            }
        }

        return list.toArray(new FeatureResolverFeature[list.size()]);
    }

    /**
     * Return a list of features required by a given IProject and a given WebSphereRuntime
     * 
     * @param project The IProject to analyze
     * @param wr The WebSphereRuntime against which will validate the features required by project
     * @param facetBasedOnly If true, will only use the facets of the projects to search for features.
     *            If false, the facets and the contents of the project will used to search for features.
     * @param monitor
     * @return a list of features (can be empty, but not null)
     */
    public static List<String> findFeatures(IProject project, WebSphereRuntime wr, boolean facetBasedOnly, IProgressMonitor monitor) {
        return findFeatures(project, wr, facetBasedOnly, false, monitor);
    }

    /**
     * Return a list of features required by a given IProject and a given WebSphereRuntime
     * 
     * @param project The IProject to analyze
     * @param wr The WebSphereRuntime against which will validate the features required by project
     * @param facetBasedOnly If true, will only use the facets of the projects to search for features.
     *            If false, the facets and the contents of the project will used to search for features.
     * @param includeAll If true, include all features even if enabled by other features.
     * @param monitor
     * @return a list of features (can be empty, but not null)
     */
    public static List<String> findFeatures(IProject project, WebSphereRuntime wr, boolean facetBasedOnly, boolean includeAll, IProgressMonitor monitor) {

        IModule[] modules = ServerUtil.getModules(project);
        if (modules == null || modules.length == 0)
            return Collections.emptyList();

        List<String> requiredFeatures = new ArrayList<String>();
        List<String> containedFeatures = new ArrayList<String>();

        List<IModule[]> moduleList = new ArrayList<IModule[]>(modules.length);
        for (IModule m : modules) {
            IModule[] module = new IModule[] { m };
            moduleList.add(module);
        }

        RequiredFeatureMap featureMap = null;
        if (facetBasedOnly) {
            featureMap = getAllRequiredFeaturesBasedOnFacets(wr, moduleList, null, null, includeAll, monitor);
        }
        else {
            featureMap = getAllRequiredFeatures(wr, moduleList, null, null, includeAll, monitor);
        }

        if (featureMap != null && !featureMap.isEmpty()) {
            String[] req = FeatureResolverFeature.convertFeatureResolverArrayToStringArray(featureMap.getFeatures());
            List<String> requiredFeaturesWithoutVersion = new ArrayList<String>();
            for (String s : req) {
                if (!s.contains("-")) {
                    requiredFeaturesWithoutVersion.add(s);
                }
                else {
                    resolveAndAdd(s, requiredFeatures, wr);
                }
            }
            // Check if the list of required features contains one that matches the features
            // without version. If not, add them. 
            for (String noVersion : requiredFeaturesWithoutVersion) {
                boolean found = false;
                for (String version : requiredFeatures) {
                    if (version.startsWith(noVersion)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    resolveAndAdd(noVersion, requiredFeatures, wr);
                }
            }
        }
        String[] cont = null;
        if (facetBasedOnly) {
            cont = FeatureResolverFeature.convertFeatureResolverArrayToStringArray(getAllContainedFeaturesBasedOnFacets(wr, moduleList, monitor));
        }
        else {
            cont = FeatureResolverFeature.convertFeatureResolverArrayToStringArray(getAllContainedFeatures(wr, moduleList, monitor));
        }
        if (cont != null) {
            List<String> containedFeaturesWithoutVersion = new ArrayList<String>();
            for (String s : cont) {
                if (!s.contains("-")) {
                    containedFeaturesWithoutVersion.add(s);
                }
                else {
                    resolveAndAdd(s, containedFeatures, wr);
                }
            }
            // Check if the list of contained features contains one that matches the features
            // without version. If not, add them.
            for (String noVersion : containedFeaturesWithoutVersion) {
                boolean found = false;
                for (String version : containedFeatures) {
                    if (version.startsWith(noVersion)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    resolveAndAdd(noVersion, containedFeatures, wr);
                }
            }
        }

        if (requiredFeatures.isEmpty() && containedFeatures.isEmpty())
            return Collections.emptyList();

        List<String> requiredFeatures2 = new ArrayList<String>();
        checkRequired: for (String required : requiredFeatures) {
            for (String contained : containedFeatures) {
                if (required.equals(contained)) {
                    // do not add
                    continue checkRequired;
                } else if (!contained.contains("-") && required.contains("-") && required.startsWith(contained + "-")) {
                    // do not add
                    continue checkRequired;
                }
            }

            String feature = required;
            if (!feature.contains("-")) {
                // try to find any matching feature for now. May want to
                // look for newest or oldest in the future
                String f = feature.toLowerCase() + "-";
                for (String fv : wr.getInstalledFeatures()) {
                    if (fv.toLowerCase().startsWith(f)) {
                        feature = fv;
                        break;
                    }
                }
            }

            if (!requiredFeatures2.contains(feature))
                requiredFeatures2.add(feature);
        }
        return requiredFeatures2;

    }

    /**
     * Resolves a feature based on the installed features in the WebSphereRuntime, If the feature is resolved, and
     * the list of features does not contains it, the resolved feature will be added.
     * 
     * @param featureToResolve the feature to be resolved
     * @param features list of features to which the resolved feature must be added
     * @param wr the runtime against which the feature will be resolved
     * @return the resolved feature, if it was added to the list, or null if the feature could not be resolved or
     *         was not added to the list.
     */
    private static void resolveAndAdd(String featureToResolve, List<String> features, WebSphereRuntime wr) {
        // if required feature is not there in the installed features, then find the next higher version of it 
        String resolvedFeature = wr.getInstalledFeatures().resolveToHigherVersion(featureToResolve);

        if (resolvedFeature == null)
            return;

        if (!resolvedFeature.contains("-"))
            resolvedFeature += "-*";

        int i = 0;
        while (i < features.size()) {
            // Check if the feature is already enabled by a feature in the list
            if (FeatureList.isEnabledBy(resolvedFeature, features.get(i), wr)) {
                return;
            }
            // Check if the feature enables any existing features in the list and replace it
            // Need to loop through the whole list since there may be more than one
            if (FeatureList.isEnabledBy(features.get(i), resolvedFeature, wr)) {
                if (!features.contains(resolvedFeature)) {
                    features.set(i, resolvedFeature);
                    i++;
                } else {
                    features.remove(i);
                }
            } else {
                i++;
            }
        }

        if (!features.contains(resolvedFeature)) {
            features.add(resolvedFeature);
        }
    }

    /**
     * Add acceptable alternatives to features from the alternativeFeatureMap
     *
     * @param featureMap The features being added which may require having alternative features inserted
     */
    private static void addAcceptableAlternativesToFeatures(RequiredFeatureMap featureMap) {

        for (int i = 0; i < featureMap.getFeatures().length; i++) {
            FeatureResolverFeature feature = featureMap.getFeatures()[i];
            for (String key : alternativeFeatureMap.keySet()) {
                if (feature.name.equals(key) || feature.name.startsWith(key + "-")) {
                    List<String> currentAlternatives = feature.getAcceptedAlternatives();
                    String[] alternativesToAdd = alternativeFeatureMap.get(key);
                    String[] alternatives = new String[currentAlternatives.size() + alternativesToAdd.length];
                    for (int j = 0; j < alternatives.length; j++) {
                        if (j < currentAlternatives.size()) {
                            alternatives[j] = currentAlternatives.get(j);
                        } else {
                            alternatives[j] = alternativesToAdd[j - currentAlternatives.size()];
                        }

                    }
                    featureMap.replaceFeature(feature, new FeatureResolverFeature(feature.getName(), alternatives));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "FeatureResolverWrapper [" + configElement.getAttribute("id") + "]";
    }
}