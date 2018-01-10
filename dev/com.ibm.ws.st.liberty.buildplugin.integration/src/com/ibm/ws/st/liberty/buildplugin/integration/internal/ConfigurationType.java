/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum ConfigurationType {
    activeBuildProfiles,
    aggregatorParentBasedir,
    aggregatorParentId,
    applicationFilename,
    applications,
    application,
    appsDirectory,
    assemblyArchive,
    assemblyArtifact,
    assemblyInstallDirectory,
    bootstrapProperties,
    bootstrapPropertiesFile,
    configDirectory,
    configFile,
    install,
    installAppPackages,
    installAppsConfigDropins,
    jvmOptions,
    jvmOptionsFile,
    looseApplication,
    installDirectory,
    projectCompileDependency,
    projectType,
    refresh,
    server,
    serverDirectory,
    serverEnv,
    serverOutputDirectory,
    serverName,
    servers, // v2
    stripVersion,
    userDirectory,
    warSourceDirectory;

    public static Set<ConfigurationType> getAllUpdateTriggers() {
        Set<ConfigurationType> allTriggers = new HashSet<ConfigurationType>(getRuntimeUpdateTriggers());
        allTriggers.addAll(getServerUpdateTriggers());
        return allTriggers;
    }

    public static List<ConfigurationType> getRuntimeUpdateTriggers() {
        return Arrays.asList(new ConfigurationType[] { installDirectory, userDirectory, assemblyInstallDirectory, projectCompileDependency, aggregatorParentId,
                                                       aggregatorParentBasedir, warSourceDirectory });
    }

    public static List<ConfigurationType> getServerUpdateTriggers() {
        return Arrays.asList(new ConfigurationType[] { serverDirectory, serverOutputDirectory, appsDirectory, serverName, configFile, looseApplication, applicationFilename,
                                                       stripVersion });
    }
}
