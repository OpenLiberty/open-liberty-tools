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

package com.ibm.ws.st.core.internal;

import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Reads in utility launch extensions and stores in a server type id ->
 * launch configuration type map.
 */
public class UtilityLaunchFactory {
    protected static final String SERVER_TYPE_ID_ATTR = "serverTypeId";
    protected static final String LAUNCH_CONFIG_TYPE_ID_ATTR = "launchConfigTypeId";

    protected static HashMap<String, String> utilityLaunchMap = null;

    public static String getLaunchConfigurationType(String serverType) {
        if (utilityLaunchMap == null)
            init();

        String launchConfigType = utilityLaunchMap.get(serverType);
        if (launchConfigType == null || launchConfigType.isEmpty()) {
            launchConfigType = "com.ibm.ws.st.core.utilityLaunchConfiguration";
        }
        return launchConfigType;
    }

    /*
     * Read in the extensions and initialize the map
     */
    protected static void init() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.core.utilityLaunchExtension");

        utilityLaunchMap = new HashMap<String, String>();

        for (IConfigurationElement elem : configElements) {
            String serverTypeId = elem.getAttribute(SERVER_TYPE_ID_ATTR);
            String launchConfigType = elem.getAttribute(LAUNCH_CONFIG_TYPE_ID_ATTR);
            utilityLaunchMap.put(serverTypeId, launchConfigType);
        }
    }
}
