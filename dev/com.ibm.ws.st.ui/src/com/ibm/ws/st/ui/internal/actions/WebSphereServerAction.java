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
package com.ibm.ws.st.ui.internal.actions;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

/**
 * WebSphere server action.
 */
public abstract class WebSphereServerAction extends WebSphereUtilityAction {

    public WebSphereServerAction(String name, Shell shell, ISelectionProvider selProvider) {
        super(name, shell, selProvider);
    }

    /**
     * Return a path relative to the runtime that needs to exist before this action can be shown. Return
     * <code>null</code> if no path needs to be validated.
     * 
     * @return the path that must exist to enable this action, or <code>null</code>
     */
    protected String getActionValidationPath() {
        return null;
    }

    /**
     * Return the feature that must exist before this action can be shown. Return
     * <code>null</code> if no feature needs to be validated.
     * 
     * @return the feature that must exist to enable this action, or <code>null</code>
     */
    protected String getActionValidationFeature() {
        return null;
    }

    @Override
    public boolean selectionChanged(Iterator<?> iterator) {
        //for remote server, utilities are disabled if they are not supported or remote start settings are not enabled
        // here check the status of disable utilities notification, if user is already prompted then disable the utility else do nothing
        if (isDisableUtilityPrompted())
            return false;

        String actionValidationPath = getActionValidationPath();
        if (actionValidationPath != null) {
            // only enable the action if the validation path exists
            try {
                File fp = server.getWebSphereRuntime().getRuntime().getLocation().append(actionValidationPath).toFile();
                if (!fp.exists())
                    return false;
            } catch (Exception e) {
                // do nothing
                return false;
            }
        }

        String feature = getActionValidationFeature();
        if (feature != null) {
            if (!server.getWebSphereRuntime().getInstalledFeatures().supports(feature))
                return false;
        }
        return true;
    }

    @Override
    public boolean isUtilityRemoteSupported() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.common.core.ext.remoteExecutionDelegate");
        return elements != null && elements.length > 0;
    }
}