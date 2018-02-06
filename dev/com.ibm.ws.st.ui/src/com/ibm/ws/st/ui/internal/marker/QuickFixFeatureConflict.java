/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.marker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.FeatureConflictHandler;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.ResolverResult;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for feature conflict
 */
public class QuickFixFeatureConflict extends AbstractMarkerResolution {
    private final String feature;

    public QuickFixFeatureConflict(String feature) {
        this.feature = feature;
    }

    @Override
    public String getLabel() {
        return NLS.bind(Messages.featureConflictQuickFix, new String[] { feature });
    }

    @Override
    public void run(IMarker marker) {
        IResource resource = getResource(marker);
        if (resource == null)
            return;

        final ConfigurationFile configFile = getConfigFile(resource);
        if (configFile == null)
            return;

        WebSphereRuntime webSphereRuntime = null;
        WebSphereServerInfo wsInfo = configFile.getWebSphereServer();
        if (wsInfo != null) {
            webSphereRuntime = wsInfo.getWebSphereRuntime();
        } else {
            // Perhaps the server config file is in a project that has a runtime.
            // Let ghost runtime providers a chance to provide this runtime.
            webSphereRuntime = ConfigUtils.getGhostWebSphereRuntime(resource);
        }
        if (webSphereRuntime == null) {
            return;
        }

        FeatureConflictHandler featureConflictHandler = Activator.getFeatureConflictHandler();
        if (featureConflictHandler == null)
            return;

        ResolverResult result = RuntimeFeatureResolver.resolve(webSphereRuntime, configFile.getAllFeatures());
        final Map<String, List<String>> alwaysAdd = new HashMap<String, List<String>>();
        if (wsInfo != null) {
            if (!featureConflictHandler.handleFeatureConflicts(wsInfo, alwaysAdd, result.getFeatureConflicts(), true))
                return;
        } else {
            if (!featureConflictHandler.handleFeatureConflicts(webSphereRuntime, configFile, alwaysAdd, result.getFeatureConflicts(), true))
                return;
        }

        try {
            configFile.save(null);
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Feature conflicts quick fix failed. Error trying to update configuration: " + configFile.getURI(), e);
            showErrorMessage();
        }
    }

    @Override
    protected String getErrorMessage() {
        return Messages.featureConflictFailedMessage;
    }
}
