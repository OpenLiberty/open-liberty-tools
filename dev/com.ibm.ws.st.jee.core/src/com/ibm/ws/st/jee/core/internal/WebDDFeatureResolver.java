/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.core.ResourceRef;
import org.eclipse.jst.javaee.web.WebApp;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

import com.ibm.ws.st.core.internal.FeatureResolver;
import com.ibm.ws.st.core.internal.FeatureResolverFeature;
import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.RequiredFeatureMap;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

@SuppressWarnings("restriction")
public class WebDDFeatureResolver extends FeatureResolver {
    private static final String RES_TYPE_DATASOURCE = "javax.sql.DataSource";
    private static final String RES_TYPE_JMS = "javax.jms.";

    @Override
    public void getRequiredFeatures(WebSphereRuntime wr, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList, FeatureSet existingFeatures,
                                    RequiredFeatureMap requiredFeatures, boolean includeAll, IProgressMonitor monitor) {
        for (IModule[] module : moduleList) {
            IModule m = module[module.length - 1];
            IProject project = m.getProject();
            if (project == null)
                continue;

            IModelProvider newProvider = ModelProviderManager.getModelProvider(project);
            Object webAppObj = newProvider.getModelObject();
            if (webAppObj == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "No web app impl available");
                continue;
            }

            List<FeatureResolverFeature> features = new ArrayList<FeatureResolverFeature>();
            if (webAppObj instanceof WebApp) {
                WebApp webApp = (WebApp) webAppObj;
                if (!webApp.getDataSource().isEmpty())
                    features.add(JEEConstants.FEATURE_JDBC);
                else {
                    List<ResourceRef> refs = webApp.getResourceRefs();
                    for (ResourceRef ref : refs) {
                        if (!features.contains(JEEConstants.FEATURE_JDBC) && RES_TYPE_DATASOURCE.equals(ref.getResType())) {
                            features.add(JEEConstants.FEATURE_JDBC);
                        }
                        if (!features.contains(JEEConstants.FEATURE_JMS) && ref.getResType() != null && ref.getResType().startsWith(RES_TYPE_JMS)) {
                            features.add(JEEConstants.FEATURE_JMS);
                        }
                    }
                }
            } else if (webAppObj instanceof org.eclipse.jst.j2ee.webapplication.WebApp) {
                org.eclipse.jst.j2ee.webapplication.WebApp webApp = (org.eclipse.jst.j2ee.webapplication.WebApp) webAppObj;

                EList<?> refs = webApp.getResourceRefs();
                if (refs != null) {
                    for (Object obj : refs) {
                        org.eclipse.jst.j2ee.common.ResourceRef ref = (org.eclipse.jst.j2ee.common.ResourceRef) obj;
                        if (!features.contains(JEEConstants.FEATURE_JDBC) && RES_TYPE_DATASOURCE.equals(ref.getType())) {
                            features.add(JEEConstants.FEATURE_JDBC);
                        }
                        if (!features.contains(JEEConstants.FEATURE_JMS) && ref.getType() != null && ref.getType().startsWith(RES_TYPE_JMS)) {
                            features.add(JEEConstants.FEATURE_JMS);
                        }
                    }
                }
            } else {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not cast to any known web app impl");
            }

            for (FeatureResolverFeature feature : features) {
                FeatureResolver.checkAndAddFeature(requiredFeatures, existingFeatures, wr, feature, Collections.singletonList(module), includeAll);
            }
        }
    }
}
