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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
/**
 * A helper class used by com.ibm.ws.st.ui.ConsoleLineTracker
 */
public class ConsoleLineTrackerHelper {

    public static final IStatus EXIST_STATUS = new Status(IStatus.INFO, Activator.PLUGIN_ID, "");
    public static final IStatus UNRESOLVED_STATUS = new Status(IStatus.INFO, Activator.PLUGIN_ID, "");

    private ConsoleLineTrackerHelper() {
        // private constructor
    }

    public static IStatus addFeatureSupport(WebSphereServer server, String feature) throws Exception {
        WebSphereRuntime runtime = server.getWebSphereRuntime();
        ConfigurationFile configFile = server.getConfiguration();

        // make sure external config files are in sync
        if (configFile.getIFile() == null)
            server.refreshConfiguration();

        FeatureSet existingFeatures = new FeatureSet(runtime, configFile.getAllFeatures());
        String resolvedFeature = existingFeatures.resolve(feature);

        if (resolvedFeature != null)
            return EXIST_STATUS;

        // get all possible version variations and check to see if
        // any is contained by existing features in the server
        // configuration
        FeatureSet installedFeatures = runtime.getInstalledFeatures();
        String[] resolvedAll = installedFeatures.resolveAll(feature);
        if (resolvedAll != null) {
            for (String r : resolvedAll) {
                for (String f : existingFeatures) {
                    if (runtime.isContainedBy(r, f)) {
                        return EXIST_STATUS;
                    }
                }
            }
        }

        // get latest feature version from runtime
        resolvedFeature = installedFeatures.resolve(feature);

        if (resolvedFeature == null) {
            return UNRESOLVED_STATUS;
        }

        configFile.addFeature(resolvedFeature);
        configFile.save(null);

        if (configFile.getIFile() == null)
            server.refreshConfiguration();

        return Status.OK_STATUS;
    }

    public static boolean isFeatureExist(WebSphereServer server, String feature) {
        WebSphereRuntime runtime = server.getWebSphereRuntime();
        ConfigurationFile configFile = server.getConfiguration();
        FeatureSet existingFeatures = new FeatureSet(runtime, configFile.getAllFeatures());
        return existingFeatures.resolve(feature) != null;
    }
}