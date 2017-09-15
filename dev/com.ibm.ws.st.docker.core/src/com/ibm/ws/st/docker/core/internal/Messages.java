/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.core.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

    public static String dockerServerDisplayName;
    public static String L_ExecutingCommands;
    public static String L_DockerCreateServerDumpProcess;
    public static String L_DockerCreateJavaDumpProcess;
    public static String L_DockerCreateSSLCertificateProcess;
    public static String E_DockerServerCannotBeFound;
    public static String E_DockerCouldNotGenerateServerDump;
    public static String E_DockerCouldNotGenerateJavaDump;
    public static String E_DockerCouldNotCopyDump;
    public static String E_DockerSSLUtilityFailed;
    public static String E_RemoteServerActionsDisabled;
    public static String E_RemoteServerActionsUnavailable;

    public static String dockerServerLaunchFailed;
    public static String dockerCreatingNewContainer;
    public static String dockerEnablingLooseConfigSettingJob;
    public static String dockerDisablingLooseConfigSettingJob;

    public static String dockerLooseConfigChangePromptDisableDetails;
    public static String dockerLooseConfigChangePromptEnableDetails;
    public static String dockerLooseConfigChangePromptTitle;
    public static String dockerLooseConfigChangeIOErrorMessage;

    public static String dockerNewContainerPromptSummary;
    public static String dockerNewContainerPromptType;

    public static String dockerNewVolumesPromptDetails;
    public static String dockerNewVolumesPromptTitle;
    public static String dockerNewVolumesPromptError;

    public static String dockerStartingContainerTask;
    public static String dockerCreatingContainerTask;

    public static String dockerUpdateContainerVolumesMsg;
    public static String dockerCreateNewContainerError;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}