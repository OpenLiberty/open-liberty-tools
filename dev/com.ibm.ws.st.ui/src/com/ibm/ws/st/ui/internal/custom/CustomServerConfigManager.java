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
package com.ibm.ws.st.ui.internal.custom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IServer;

public class CustomServerConfigManager {

    private static final String EXTENSION_ID = "com.ibm.ws.st.ui.customServerConfig";
    private static final String HANDLER_ELEMENT = "handler";
    private static final String CLASS_ATTRIBUTE = "class";

    private static final CustomServerConfigManager customServerConfigurationManager = new CustomServerConfigManager();

    private final List<ICustomServerConfig> handlers;

    private CustomServerConfigManager() {
        handlers = new ArrayList<ICustomServerConfig>();
        IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement configurationElement = extensions[i];
            if (HANDLER_ELEMENT.equals(configurationElement.getName())) {
                try {
                    Object object = configurationElement.createExecutableExtension(CLASS_ATTRIBUTE);
                    if (object instanceof ICustomServerConfig) {
                        handlers.add((ICustomServerConfig) object);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public static CustomServerConfigManager getInstance() {
        return customServerConfigurationManager;
    }

    public List<Object> getCustomServerElements(IServer server) {
        List<Object> customServerElements = new ArrayList<Object>();
        Iterator<ICustomServerConfig> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            ICustomServerConfig customServerConfig = iterator.next();
            List<Object> currentCustomServerElements = customServerConfig.getCustomServerElements(server);
            if (currentCustomServerElements != null) {
                customServerElements.addAll(currentCustomServerElements);
            }
        }
        return customServerElements;
    }

}
