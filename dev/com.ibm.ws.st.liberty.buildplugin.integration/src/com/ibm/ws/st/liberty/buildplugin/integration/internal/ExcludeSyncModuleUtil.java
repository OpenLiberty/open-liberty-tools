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

package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.util.HashMap;

import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.core.internal.ExcludeSyncModuleInfo;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;

@SuppressWarnings("restriction")
public class ExcludeSyncModuleUtil {

    public static void updateExcludeSyncModuleMapping(IModule[] modules, LibertyBuildPluginConfiguration config, WebSphereServerBehaviour behaviour) {
        if (modules != null && behaviour != null && config != null) {
            HashMap<IModule, ExcludeSyncModuleInfo> map = behaviour.getExcludeSyncModules();
            String appsDir = config.getConfigValue(ConfigurationType.appsDirectory);
            String appFileName = config.getConfigValue(ConfigurationType.applicationFilename);
            String installAppsConfigDropins = config.getConfigValue(ConfigurationType.installAppsConfigDropins);

            for (IModule module : modules) {
                ExcludeSyncModuleInfo info = new ExcludeSyncModuleInfo(module);
                HashMap<String, String> props = info.getProperties();
                props.put(ExcludeSyncModuleInfo.APP_FILE_NAME, appFileName);
                props.put(ExcludeSyncModuleInfo.APPS_DIR, appsDir);
                props.put(ExcludeSyncModuleInfo.INSTALL_APPS_CONFIG_DROPINS, installAppsConfigDropins);

                String serverOutputDir = config.getConfigValue(ConfigurationType.serverOutputDirectory);
                if (serverOutputDir != null && appsDir != null && appFileName != null) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(serverOutputDir);
                    buffer.append("/");
                    buffer.append(appsDir);
                    buffer.append("/");
                    buffer.append(appFileName);
                    props.put(ExcludeSyncModuleInfo.FULL_APP_PATH, buffer.toString());
                }

                map.put(module, info);
            }
        }
    }
}
