/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix to add a feature.
 */
public class QuickFixAddFeature extends AbstractMarkerResolution {
    private final String feature;

    public QuickFixAddFeature(String feature) {
        this.feature = feature;
    }

    @Override
    public String getLabel() {
        return NLS.bind(Messages.addFeatureQuickFix, feature);
    }

    @Override
    public void run(IMarker marker) {
        IResource resource = getResource(marker);
        if (resource == null)
            return;

        ConfigurationFile configFile = getConfigFile(resource);
        if (configFile == null)
            return;

        try {
            configFile.addFeature(feature);
            configFile.save(null);
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix to add feature failed trying to save configuration: " + configFile.getURI(), e);
            showErrorMessage();
        }
    }

    @Override
    protected String getErrorMessage() {
        return Messages.addFeatureFailedMessage;
    }
}
