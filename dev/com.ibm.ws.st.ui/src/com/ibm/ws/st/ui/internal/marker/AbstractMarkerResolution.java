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
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * An abstract quick fix with some helper methods.
 */
public abstract class AbstractMarkerResolution implements IMarkerResolution {
    protected IFile getResource(IMarker marker) {
        IResource resource = marker.getResource();
        if (resource != null && (resource instanceof IFile))
            return (IFile) resource;

        if (Trace.ENABLED)
            Trace.trace(Trace.ERROR, "Quick fix failed. Marker's resource is null or not a file", null);
        showErrorMessage();
        return null;
    }

    protected ConfigurationFile getConfigFile(IResource resource) {
        ConfigurationFile configFile = ConfigUtils.getConfigFile(resource);
        if (configFile == null) {
            final URI uri = resource.getLocation().toFile().toURI();
            UserDirectory userDir = ConfigUtils.getUserDirectory(uri);
            if (userDir != null) {
                try {
                    configFile = new ConfigurationFile(uri, userDir);
                } catch (IOException e) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.WARNING, "Could not create configuration file: " + uri);
                    }
                }
            }
        }
        if (configFile == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.ERROR, "Quick fix failed. Could not locate configuration file: " + resource.getLocationURI(), null);
            showErrorMessage();
        }
        return configFile;
    }

    protected abstract String getErrorMessage();

    protected void showErrorMessage() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MessageDialog.openError(shell, Messages.title, getErrorMessage());
    }
}
