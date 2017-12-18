/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentType;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentVersion;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.internal.facets.RuntimeFacetComponentProviderDelegate;

/**
 * This extension maps installed features to facet runtime components. To avoid a new extension point,
 * the mapping is done through a naming pattern - all runtime components (providers of the
 * org.eclipse.wst.common.project.facet.core.runtimes <runtime-component-type id="">) should be named
 * "com.ibm.ws.st.runtime.<featureName>", where <featureName> corresponds to the feature that it enables.
 *
 * It is important for all programming model features to define a runtime component. This enables the
 * facet runtime to correctly block users from targeting projects to server that do not have the
 * necessary feature support. This will be even more important when Liberty supports minify
 * (i.e. custom-built servers).
 */
@SuppressWarnings("restriction")
public class WebSphereRuntimeComponentProvider extends RuntimeFacetComponentProviderDelegate {
    @Override
    public List<IRuntimeComponent> getRuntimeComponents(IRuntime runtime) {
        if (runtime == null)
            return new ArrayList<IRuntimeComponent>();

        try {
            List<IRuntimeComponent> list = new ArrayList<IRuntimeComponent>();

            WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            FeatureSet fs = wr.getInstalledFeatures();
            fs.sort();
            for (String s : fs) {
                try {
                    int ind = s.indexOf("-");
                    if (ind > 0 && ind < s.length() - 1) {
                        // feature names are not case sensitive
                        String t = s.substring(0, ind).toLowerCase();
                        if (RuntimeManager.isRuntimeComponentTypeDefined("com.ibm.ws.st.runtime." + t)) {
                            IRuntimeComponentType rct = RuntimeManager.getRuntimeComponentType("com.ibm.ws.st.runtime." + t);
                            if (rct != null) {
                                t = s.substring(ind + 1);
                                if (rct.hasVersion(t)) {
                                    Map<String, String> properties = new HashMap<String, String>(2);
                                    properties.put("type", NLS.bind(Messages.runtimeFeature, s));
                                    list.add(RuntimeManager.createRuntimeComponent(rct.getVersion(t), properties));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Problem creating runtime component for " + s, e);
                }
            }

            // Check what version of EAR applications are supported by the runtime
            if (RuntimeManager.isRuntimeComponentTypeDefined("com.ibm.ws.st.runtime.enterpriseApplication")) {
                IRuntimeComponentType rct = RuntimeManager.getRuntimeComponentType("com.ibm.ws.st.runtime.enterpriseApplication");
                if (rct != null) {
                    if (wr.isEARSupported("7.0") && rct.hasVersion("7.0")) {
                        list.add(RuntimeManager.createRuntimeComponent(rct.getVersion("7.0"), new HashMap<String, String>(0)));
                    }
                    if (wr.isEARSupported("8.0") && rct.hasVersion("8.0")) {
                        list.add(RuntimeManager.createRuntimeComponent(rct.getVersion("8.0"), new HashMap<String, String>(0)));
                    }
                }
            }

            // Add the runtime service version component.

            String curVersion = wr.getRuntimeVersion();
            IRuntimeComponentType rct = RuntimeManager.getRuntimeComponentType(Constants.RUNTIME_COMPONENT_SERVICE_VERSION_ID);
            if (rct != null && curVersion != null) {
                IRuntimeComponentVersion rcv = null;
                if (WebSphereUtil.isGreaterOrEqualVersion("9.0.0", curVersion)) {
                    rcv = rct.getVersion("9.0.0");
                } else if (WebSphereUtil.isGreaterOrEqualVersion("8.5.5", curVersion)) {
                    rcv = rct.getVersion("8.5.5");
                } else {
                    rcv = rct.getVersion("8.5.0");
                }
                if (rcv != null) {
                    Map<String, String> properties = new HashMap<String, String>(2);
                    properties.put("type", NLS.bind(Messages.runtimeServiceVersion, rcv.getVersionString()));
                    list.add(RuntimeManager.createRuntimeComponent(rcv, properties));
                }
            }

            return list;
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Problem creating runtime features", e);
            return null;
        }
    }
}