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
package com.ibm.ws.st.core.tests;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.common.core.ext.internal.Trace;

/**
 *
 */
public class TestBaseExtensionFactory {
    public static final String SERVER_TYPE_TEST_BASE = "testBaseClass";
    public static final String SERVER_TYPE_ID_ATTRIBUTE = "typeId";
    public static final String SERVER_ID = "id";
    public static HashMap<String, IConfigurationElement> serverTypeMap = null;

    /**
     * Get the TestBaseInterface extension.
     *
     * @param serverType the server type
     * @return the class if it exists, null otherwise
     */
    public static TestBaseInterface getTestBaseExtension(String serverType) {
        if (serverTypeMap == null)
            init();
        TestBaseInterface testBase = (TestBaseInterface) getExtensionClass(serverType, SERVER_TYPE_TEST_BASE);
        return testBase;
    }

    /*
     * Gets the server type extensions and stores the values into the serverTypeMap
     */
    protected static void init() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("com.ibm.ws.st.core.tests.TestBase");
        serverTypeMap = new HashMap<String, IConfigurationElement>();
        String serverTypeId = null;

        for (IConfigurationElement elem : configElements) {
            serverTypeId = elem.getAttribute(SERVER_TYPE_ID_ATTRIBUTE);
            serverTypeMap.put(serverTypeId, elem);
        }
    }

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

}
