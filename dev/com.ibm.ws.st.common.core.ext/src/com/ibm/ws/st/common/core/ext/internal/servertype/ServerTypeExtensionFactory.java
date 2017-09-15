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

package com.ibm.ws.st.common.core.ext.internal.servertype;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.common.core.ext.internal.Trace;

/**
 * Loads the server type extensions
 */
public class ServerTypeExtensionFactory {
    public static final String SERVER_TYPE_LAUNCH_ATTRIBUTE = "serverLaunchConfigurationClass";
    public static final String SERVER_TYPE_ID_ATTRIBUTE = "typeId";
    public static final String SERVER_TYPE_SERVER_ATTR = "serverClass";
    public static final String SERVER_TYPE_BEHAVIOUR_ATTR = "serverBehaviourClass";
    public static final String SERVER_ID = "id";

    public static HashMap<String, IConfigurationElement> serverTypeMap = null;

    public static HashMap<String, AbstractLaunchConfigurationExtension> serverLaunchClassMap = null;

    // Loads the class from an extension
    protected static Object getExtensionClass(String serverType, String attribute) {
        Object returnClass = null;
        IConfigurationElement configClass = serverTypeMap.get(serverType);
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

    /**
     * Get the WebSphereLaunchConfiguration server type extension.
     *
     * @param serverType the server type
     * @return the class if it exists, null otherwise
     */
    public static AbstractLaunchConfigurationExtension getServerLaunchOperation(String serverType) {
        if (serverTypeMap == null)
            init();

        AbstractLaunchConfigurationExtension launchClass = serverLaunchClassMap.get(serverType);
        if (launchClass == null) {
            // Load the launch configuration class found in the extension
            launchClass = (AbstractLaunchConfigurationExtension) getExtensionClass(serverType, SERVER_TYPE_LAUNCH_ATTRIBUTE);

            // For the launch configuration, there only needs to be one instance for a serverType, so cache the value
            // so the same class will always be returned (not a new one)
            serverLaunchClassMap.put(serverType, launchClass);
        }
        return launchClass;
    }

    /**
     * Get the WebSphereServerBehaviour server type extension.
     *
     * @param serverType the server type
     * @return the class if it exists, null otherwise
     */
    public static AbstractServerBehaviourExtension getServerBehaviourExtension(String serverType) {
        if (serverTypeMap == null)
            init();

        AbstractServerBehaviourExtension serverBehaviour = (AbstractServerBehaviourExtension) getExtensionClass(serverType, SERVER_TYPE_BEHAVIOUR_ATTR);

        return serverBehaviour;
    }

    /**
     * Get the WebSphereServer server type extension.
     *
     * @param serverType the server type
     * @return the class if it exists, null otherwise
     */
    public static AbstractServerExtension getServerExtension(String serverType) {
        if (serverTypeMap == null)
            init();

        AbstractServerExtension server = (AbstractServerExtension) getExtensionClass(serverType, SERVER_TYPE_SERVER_ATTR);

        return server;
    }

    /*
     * Gets the server type extensions and stores the values into the serverTypeMap
     */
    protected static void init() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.common.core.ext.serverTypeExtension");

        serverTypeMap = new HashMap<String, IConfigurationElement>();
        serverLaunchClassMap = new HashMap<String, AbstractLaunchConfigurationExtension>();

        String serverTypeId = null;

        for (IConfigurationElement elem : configElements) {
            serverTypeId = elem.getAttribute(SERVER_TYPE_ID_ATTRIBUTE);
            serverTypeMap.put(serverTypeId, elem);
        }
    }
}
