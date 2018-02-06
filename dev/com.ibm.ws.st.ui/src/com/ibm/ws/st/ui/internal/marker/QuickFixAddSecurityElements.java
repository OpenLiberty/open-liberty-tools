/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.validation.AbstractConfigurationValidator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.config.AddSecurityElementsDialog;

/**
 * Quick fix for adding missing security elements
 */
public class QuickFixAddSecurityElements extends AbstractMarkerResolution {

    public QuickFixAddSecurityElements() {}

    @Override
    public String getLabel() {
        return Messages.addRequiredServerConfigQuickFix;
    }

    @Override
    public void run(IMarker marker) {
        IResource resource = getResource(marker);
        if (resource == null)
            return;

        final ConfigurationFile configFile = getConfigFile(resource);
        if (configFile == null)
            return;

        WebSphereServerInfo wsInfo = configFile.getWebSphereServer();

        boolean securityEnabled = marker.getAttribute(AbstractConfigurationValidator.APP_SECURITY_ENABLED, false);
        Shell shell = Display.getDefault().getActiveShell();

        AddSecurityElementsDialog dialog = null;
        if (wsInfo != null) { // classic scenario
            dialog = new AddSecurityElementsDialog(shell, wsInfo, securityEnabled);
        } else {
            // Give Liberty Runtime provider extensions a chance
            UserDirectory userDirectory = configFile.getUserDirectory();
            if (userDirectory != null) {
                WebSphereRuntime wsRuntime = userDirectory.getWebSphereRuntime();
                dialog = new AddSecurityElementsDialog(shell, wsRuntime, configFile, securityEnabled);
            }
            if (dialog == null) {
                showErrorMessage();
                return;
            }
        }
        if (dialog.open() == IStatus.OK) {
            try {
                configFile.save(null);
            } catch (IOException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.ERROR, "Add security elements quick fix failed. Error trying to update configuration: " + configFile.getURI(), e);
                showErrorMessage();
            }
        }
    }

    @Override
    protected String getErrorMessage() {
        return Messages.addRequiredServerConfigFailedMsg;
    }
}
