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

import org.eclipse.wst.server.core.IModule;

/**
 *
 */
public class ExcludeSyncModuleInfo {

    HashMap<String, String> properties = null;
    final IModule module;

    public static final String APP_FILE_NAME = "appFileName";
    public static final String APPS_DIR = "appsDir";
    public static final String INSTALL_APPS_CONFIG_DROPINS = "installAppsConfigDropins";
    public static final String FULL_APP_PATH = "fullAppPath";

    public ExcludeSyncModuleInfo(IModule module) {
        properties = new HashMap<String, String>();
        this.module = module;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public IModule getModule() {
        return module;
    }

}