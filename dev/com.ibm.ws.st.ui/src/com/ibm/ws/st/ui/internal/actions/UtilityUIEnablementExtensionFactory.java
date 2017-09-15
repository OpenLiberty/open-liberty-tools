/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.ui.internal.Trace;

public class UtilityUIEnablementExtensionFactory {

    private static final String TYPE_ID_ATTR = "typeId";
    private static final String TYPE_CLASS = "serverUtilityUIEnablementClass";

    // TODO: change all these variables
    protected static HashMap<String, IConfigurationElement> serverDumpUtilityMap = null;
    protected static HashMap<String, AbstractUtilityUIEnablementExtension> serverDumpUtilityClassMap = null;

    // Loads the class from an extension
    protected static Object getExtensionClass(String serverType, String attribute) {
        Object returnClass = null;
        IConfigurationElement configClass = serverDumpUtilityMap.get(serverType);
        if (configClass != null) {
            try {
                returnClass = configClass.createExecutableExtension(attribute);

            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to load extension class serverType="
                                               + serverType + ", attribute=" + attribute);
                }
            }
        }
        return returnClass;
    }

    public static AbstractUtilityUIEnablementExtension getServerUtilityUIEnablementOperation(String serverType) {
        if (serverDumpUtilityMap == null)
            init();

        AbstractUtilityUIEnablementExtension launchClass = serverDumpUtilityClassMap.get(serverType);
        if (launchClass == null) {
            // Load the launch configuration class found in the extension
            launchClass = (AbstractUtilityUIEnablementExtension) getExtensionClass(serverType, TYPE_CLASS);

            // Defaults to the base liberty server dump utility
            if (launchClass == null) {
                launchClass = new BaseLibertyUtilityUIEnablement();
            }

            // For the launch configuration, there only needs to be one instance for a serverType, so cache the value
            // so the same class will always be returned (not a new one)
            serverDumpUtilityClassMap.put(serverType, launchClass);
        }
        return launchClass;
    }

    /*
     * Gets the server type extensions and stores the values into the serverTypeMap
     */
    protected static void init() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.ui.serverUtilityUIEnablementExtension");

        serverDumpUtilityMap = new HashMap<String, IConfigurationElement>();
        serverDumpUtilityClassMap = new HashMap<String, AbstractUtilityUIEnablementExtension>();

        String serverTypeId = null;

        for (IConfigurationElement elem : configElements) {
            serverTypeId = elem.getAttribute(TYPE_ID_ATTR);
            serverDumpUtilityMap.put(serverTypeId, elem);
        }
    }
}
