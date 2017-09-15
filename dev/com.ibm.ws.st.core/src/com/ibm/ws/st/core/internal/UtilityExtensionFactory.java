/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.common.core.ext.internal.Trace;

/**
 * Reads in utility extensions and stores in a utility type ->
 * extension map.
 */
public class UtilityExtensionFactory {
    protected static final String UTILITY_EXT = "com.ibm.ws.st.core.utilityExtension";
    protected static final String UTILITY_ELEM = "utility";
    protected static final String TYPE_ATTR = "type";
    protected static final String CLASS_ATTR = "class";

    private static HashMap<String, IConfigurationElement> utilityExtMap = null;

    public static UtilityExtension getExtensionClass(String type) throws CoreException {
        if (utilityExtMap == null) {
            init();
        }
        UtilityExtension returnClass = null;
        IConfigurationElement configClass = utilityExtMap.get(type);
        if (configClass != null) {
            try {
                returnClass = (UtilityExtension) configClass.createExecutableExtension(CLASS_ATTR);
            } catch (CoreException e) {
                Trace.logError("Failed to load utility extension class for type: " + type, e);
                throw e;
            }
        }
        return returnClass;
    }

    /*
     * Read in the extensions and initialize the map
     */
    private static void init() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor(UTILITY_EXT);

        utilityExtMap = new HashMap<String, IConfigurationElement>();

        for (IConfigurationElement elem : configElements) {
            String utilityType = elem.getAttribute(TYPE_ATTR);
            utilityExtMap.put(utilityType, elem);
        }
    }
}
