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
package com.ibm.ws.st.jee.core.internal;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ModuleDelegate;

import com.ibm.ws.st.core.internal.FeatureResolver;
import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.RequiredFeatureMap;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

@SuppressWarnings("restriction")
public class JSPFeatureResolver extends FeatureResolver {
    private static final String[] JSP_EXTENSIONS = new String[] { ".jsp", ".jspf", ".jsw", ".jsv", ".jspx" };
    private static final int SEARCH_LIMIT = 500;

    /**
     * Performs a simple (non-foolproof) check to see if there are any JSP files in the
     * given web app. The check will return false if the module is binary (not expanded),
     * if the JSP files have a non-standard extension, or if they are buried too deep
     * within a large web module.
     * 
     * @param module a web module
     * @param monitor a progress monitor
     * @return <code>true</code> if the module contains a JSP file, and <code>false</code>
     *         if it likely doesn't
     */
    private static boolean requiresJSPFeature(IModule module, IProgressMonitor monitor) {
        ModuleDelegate moduleDelegate = (ModuleDelegate) module.loadAdapter(ModuleDelegate.class, monitor);
        if (moduleDelegate == null)
            return false;

        try {
            IModuleResource[] members = moduleDelegate.members();
            Queue<IModuleResource> queue = new ArrayDeque<IModuleResource>(members.length + 20);
            int size = members.length;
            if (size > SEARCH_LIMIT)
                size = SEARCH_LIMIT;
            for (int i = 0; i < size; i++)
                queue.add(members[i]);

            // Perform a breadth-first search (JSP files are more likely to be near the root of a module)
            int count = 0;
            while (count < SEARCH_LIMIT && !queue.isEmpty()) {
                IModuleResource mr = queue.poll();
                if (mr instanceof IModuleFolder) {
                    IModuleFolder folder = (IModuleFolder) mr;
                    members = folder.members();
                    if (members != null) {
                        size = members.length;
                        if (size > SEARCH_LIMIT - count)
                            size = SEARCH_LIMIT - count;
                        for (int i = 0; i < size; i++)
                            queue.add(members[i]);
                    }
                } else if (mr instanceof IModuleFile) {
                    for (String s : JSP_EXTENSIONS)
                        if (mr.getName().endsWith(s))
                            return true;
                }
                count++;
            }
        } catch (CoreException ce) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Problem scanning module members for jsp files", ce);
            return false;
        }
        return false;
    }

    /**
     * Performs a simple (non-foolproof) check to see if there are any JSP files in the
     * given web app. The check will return false if the module is binary (not expanded),
     * if the JSP files have a non-standard extension, or if they are buried too deep
     * within a large web module.
     * 
     * @param module a web module
     * @param monitor a progress monitor
     * @return <code>true</code> if the module contains a JSP file, and <code>false</code>
     *         if it likely doesn't
     */
    private static boolean requiresJSPFeature(IModule module, IModuleResourceDelta[] delta, IProgressMonitor monitor) {
        if (delta == null)
            return false;

        Queue<IModuleResourceDelta> queue = new ArrayDeque<IModuleResourceDelta>(50);
        for (IModuleResourceDelta delta2 : delta)
            queue.add(delta2);

        // Perform a breadth-first search (JSP files are more likely to be near the root of a module)
        int count = 0;
        while (count < SEARCH_LIMIT && !queue.isEmpty()) {
            IModuleResourceDelta mrd = queue.poll();
            IModuleResource mr = mrd.getModuleResource();
            if (mr instanceof IModuleFolder) {
                IModuleResourceDelta[] members = mrd.getAffectedChildren();
                if (members != null) {
                    int size = members.length;
                    if (size > SEARCH_LIMIT - count)
                        size = SEARCH_LIMIT - count;
                    for (int i = 0; i < size; i++)
                        queue.add(members[i]);
                }
            } else if (mr instanceof IModuleFile) {
                if (mrd.getKind() != IModuleResourceDelta.REMOVED) {
                    for (String s : JSP_EXTENSIONS)
                        if (mr.getName().endsWith(s))
                            return true;
                }
            }
            count++;
        }
        return false;
    }

    @Override
    public void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList, FeatureSet existingFeatures,
                                    RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor) {
        for (int i = 0; i < moduleList.size(); i++) {
            IModule[] module = moduleList.get(i);
            IModuleResourceDelta[] delta = deltaList == null ? null : deltaList.get(i);
            if ((delta != null && requiresJSPFeature(module[module.length - 1], delta, monitor)) || requiresJSPFeature(module[module.length - 1], monitor)) {
                FeatureResolver.checkAndAddFeature(requiredFeatures, existingFeatures, wr, JEEConstants.FEATURE_JSP, Collections.singletonList(module), includeAll);
            }
        }
    }
}
