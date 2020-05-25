/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.featureDetection;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FeatureUtil;

/**
 * Base test case containing utilities for feature tests
 */
public class BaseFeatureTest extends ToolsTestBase {

    protected void checkFeatures(String... expectedFeatures) {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        List<String> features = ws.getConfiguration().getFeatures();
        print("After publish feature list");
        for (String feature : features)
            print(feature);
        FeatureUtil.verifyFeatures(wr, expectedFeatures, features);
    }

    protected void addFeature(String feature) {
        FeatureUtil.addFeature(server, feature);
    }

    protected void clearFeatures() {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();
        List<String> featureList = configFile.getFeatures();
        for (int i = 0; i < featureList.size(); i++) {
            if (featureList.get(i).toLowerCase().startsWith("localconnector")) {
                featureList.remove(i);
                break;
            }
        }
        configFile.removeFeatures(featureList);
        try {
            configFile.save(new NullProgressMonitor());
            ws.refreshConfiguration();
        } catch (IOException e) {
            print("Exception trying to save changes to config file " + configFile.getName() + " (" + e + ").");
        }
    }

    protected void cleanupAfterTest(String... projects) {
        try {
            removeApps(projects);
        } catch (Exception e) {
            print("Failed to remove projects", e);
        }
        clearFeatures();
    }

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

}