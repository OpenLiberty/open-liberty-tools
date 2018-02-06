/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for superseded feature
 */
public class QuickFixSupersedeFeature extends AbstractMarkerResolution {
    private final String feature;

    public QuickFixSupersedeFeature(String feature) {
        this.feature = feature;
    }

    @Override
    public String getLabel() {
        return NLS.bind(Messages.supersededFeatureQuickFix, new String[] { feature });
    }

    @Override
    public void run(IMarker marker) {
        IResource resource = getResource(marker);
        if (resource == null)
            return;

        final ConfigurationFile configFile = getConfigFile(resource);
        if (configFile == null)
            return;

        String[] replacementFeatures = getReplacementFeatures(configFile);
        if (replacementFeatures == null)
            return;

        try {
            configFile.removeFeature(feature);
            for (String s : replacementFeatures)
                configFile.addFeature(s);
            configFile.save(null);
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix to supersede feature failed. Error trying to update configuration: " + configFile.getURI(), e);
            showErrorMessage();
        }
    }

    private String[] getReplacementFeatures(ConfigurationFile configFile) {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        SupersedeFeatureDialog dialog = null;
        WebSphereRuntime webSphereRuntime = null;
        if (configFile.getWebSphereServer() != null) { // Usual classic case for a server config
            dialog = new SupersedeFeatureDialog(shell, configFile.getWebSphereServer(), feature);
        } else {
            // Let Liberty runtime providers a chance to provide this runtime.  Each config file in the project has an associated user directory and a runtime
            UserDirectory userDirectory = configFile.getUserDirectory();
            if (userDirectory != null) {
                webSphereRuntime = userDirectory.getWebSphereRuntime();
                dialog = new SupersedeFeatureDialog(shell, webSphereRuntime, configFile, feature);
            }
            if (dialog == null) {
                return null;
            }
        }

        if (dialog.open() == Window.CANCEL)
            return null;
        return dialog.getReplacementFeatures();
    }

    @Override
    protected String getErrorMessage() {
        return Messages.featureChangeFailedMessage;
    }
}
