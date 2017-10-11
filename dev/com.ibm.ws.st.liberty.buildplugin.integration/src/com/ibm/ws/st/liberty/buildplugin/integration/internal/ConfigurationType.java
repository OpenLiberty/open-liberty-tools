/*
* IBM Confidential
*
* OCO Source Materials
*
* (C) Copyright IBM Corp. 2017 All Rights Reserved.
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum ConfigurationType {
    activeBuildProfiles,
    applicationFilename,
    installDirectory,
    serverDirectory,
    userDirectory,
    serverOutputDirectory,
    serverName,
    configDirectory,
    configFile,
    appsDirectory,
    looseApplication,
    bootstrapPropertiesFile,
    bootstrapProperties,
    serverEnv,
    jvmOptions,
    jvmOptionsFile,
    stripVersion,
    installAppPackages,
    assemblyArtifact,
    assemblyArchive,
    assemblyInstallDirectory,
    refresh,
    installAppsConfigDropins,
    install,
    projectType,
    aggregatorParentId,
    aggregatorParentBasedir,
    projectCompileDependency,
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
