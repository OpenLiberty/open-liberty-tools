/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config.validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigVars;

/**
 *
 */
public class CustomServerVariablesManager {
    private static final String EXTENSION_ID = "com.ibm.ws.st.core.customServerVariables";
    private static final String HANDLER_ELEMENT = "handler";
    private static final String CLASS_ATTRIBUTE = "class";

    private static final CustomServerVariablesManager customServerVariablesManager = new CustomServerVariablesManager();

    private final List<ICustomServerVariablesHandler> handlers;

    private CustomServerVariablesManager() {
        handlers = new ArrayList<ICustomServerVariablesHandler>();
        IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement configurationElement = extensions[i];
            if (HANDLER_ELEMENT.equals(configurationElement.getName())) {
                try {
                    Object object = configurationElement.createExecutableExtension(CLASS_ATTRIBUTE);
                    handlers.add((ICustomServerVariablesHandler) object);
                } catch (Exception exception) {
                    Trace.logError("Error while creating custom server variables extension.", exception);
                }
            }
        }
    }

    public static CustomServerVariablesManager getInstance() {
        return customServerVariablesManager;
    }

    public void addCustomServerVariables(ConfigVars globalVars, IProject project) {
        Iterator<ICustomServerVariablesHandler> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            ICustomServerVariablesHandler customServerVariablesHandler = iterator.next();
            customServerVariablesHandler.addCustomServerVariables(globalVars, project);
        }
    }

    public void addCustomServerVariables(ConfigVars globalVars, WebSphereServerInfo wsInfo) {
        Iterator<ICustomServerVariablesHandler> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            ICustomServerVariablesHandler customServerVariablesHandler = iterator.next();
            customServerVariablesHandler.addCustomServerVariables(globalVars, wsInfo);
        }
    }

}
