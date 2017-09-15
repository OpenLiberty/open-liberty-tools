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

import org.eclipse.wst.server.core.IModule;

/**
 * Map of required features to modules that require them.
 */
public final class RequiredFeatureMap {

    private final Map<FeatureResolverFeature, Set<IModule[]>> moduleMap = new HashMap<FeatureResolverFeature, Set<IModule[]>>();

    public FeatureResolverFeature[] getFeatures() {
        return moduleMap.keySet().toArray(new FeatureResolverFeature[moduleMap.size()]);
    }

    public List<IModule[]> getModules(FeatureResolverFeature feature) {
        Set<IModule[]> moduleSet = moduleMap.get(feature);
        return moduleSet == null ? null : new ArrayList<IModule[]>(moduleSet);
    }

    public void removeFeature(FeatureResolverFeature feature) {
        moduleMap.remove(feature);
    }

    public void addFeature(FeatureResolverFeature feature, IModule[] module) {
        addFeature(feature, module == null ? null : Collections.singletonList(module));
    }

    public void addFeature(FeatureResolverFeature feature, List<IModule[]> modules) {
        Set<IModule[]> moduleSet = moduleMap.get(feature);
        if (moduleSet == null) {
            moduleSet = new HashSet<IModule[]>();
            moduleMap.put(feature, moduleSet);
        }
        if (modules != null)
            moduleSet.addAll(modules);
    }

    public void addModule(FeatureResolverFeature feature, IModule[] module) {
        addModules(feature, module == null ? null : Collections.singletonList(module));
    }

    public void addModules(FeatureResolverFeature feature, List<IModule[]> moduleList) {
        if (!moduleMap.containsKey(feature) || moduleList == null)
            return;

        Set<IModule[]> moduleSet = moduleMap.get(feature);
        moduleSet.addAll(moduleList);
    }

    public void replaceFeature(FeatureResolverFeature remove, FeatureResolverFeature add) {
        Set<IModule[]> moduleSet = moduleMap.get(remove);
        moduleMap.remove(remove);
        moduleMap.put(add, moduleSet);
    }

    public boolean contains(FeatureResolverFeature feature) {
        return moduleMap.containsKey(feature);
    }

    public boolean isEmpty() {
        return moduleMap.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean firstEntry = true;
        for (Map.Entry<FeatureResolverFeature, Set<IModule[]>> entry : moduleMap.entrySet()) {
            if (firstEntry) {
                firstEntry = false;
            } else {
                builder.append(", ");
            }
            builder.append(entry.getKey());
            builder.append("[");
            boolean firstModule = true;
            for (IModule[] module : entry.getValue()) {
                if (module.length == 0)
                    continue;
                if (firstModule) {
                    firstModule = false;
                } else {
                    builder.append(", ");
                }
                builder.append(module[module.length - 1]);
            }
            builder.append("]");
        }
        builder.append("}");
        return builder.toString();
    }
}