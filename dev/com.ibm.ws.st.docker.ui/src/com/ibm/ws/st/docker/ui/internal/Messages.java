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
package com.ibm.ws.st.docker.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {
    public static String wizDockerServerGatheringContainers;
    public static String wizDockerServerTitle;
    public static String wizDockerServerDescription;
    public static String wizDockerServerConnectionInfo;
    public static String wizDockerServerContainerInfo;
    public static String wizDockerServerLibertySecurityInfo;
    public static String wizDockerMachineName;
    public static String wizDockerContainerName;
    public static String wizDockerContainerNameFormat;
    public static String wizDockerUserLabel;
    public static String wizDockerPasswordLabel;
    public static String wizDockerPasswordShowButton;
    public static String wizDockerSecurePort;
    public static String wizDockerLooseConfigButton;
    public static String wizDockerDirectoryLabel;
    public static String wizDockerConnect;
    public static String wizDockerConnectTooltip;
    public static String wizDockerNoContainersFound;
    public static String wizDockerContainerNameNotSet;
    public static String wizDockerRefresh;
    public static String wizDockerRefreshTooltip;
    public static String wizDockerUserNotSet;
    public static String wizDockerPasswordNotSet;
    public static String wizDockerPortNotSet;
    public static String wizDockerMachineConnectionError;
    public static String infoOnlyServerConfigSynchronized;

    public static String wizDockerNoRegOrUser;
    public static String wizDockerSecuDiaTitle;
    public static String wizDockerUserMismatch;
    public static String wizDockerPWMismatch;
    public static String wizDockerNotAdmin;
    public static String wizDockerMissingServerXML;

    public static String wizDockerSupportLooseConfigTitle;
    public static String wizDockerContainerNotEnabledForLooseConfig;

    public static String wizDockerConnectExceptionDialogTitle;
    public static String wizDockerConnectExceptionDialogDetails;
    public static String wizDockerIOExceptionDialogTitle;
    public static String wizDockerIOExceptionDialogDetails;
    public static String wizDockerExceptionDialogTitle;
    public static String wizDockerExceptionDialogDetails;
    public static String wizDockerExistingProjectOverlaps;

    public static String wizDockerMappedPortErrorMsg;

    public static String wizDockerNoUserCreateButton;
    public static String wizDockerNoUserProceedButton;

    public static String dockerServerDeleteTitle;
    public static String dockerServerDeleteRemoveArtifacts;
    public static String dockerServerDeleteErrorTitle;
    public static String dockerServerDeleteRemoveContainerFailed;
    public static String dockerServerDeleteRemoveImageFailed;

    public static String dockerInfoUnavailable;
    public static String dockerInfoTask;
    public static String dockerInfoMachineLabel;
    public static String dockerInfoName;
    public static String dockerInfoMachineIP;
    public static String dockerInfoContainerLabel;
    public static String dockerInfoOrigContainerLabel;
    public static String dockerInfoImageName;
    public static String dockerInfoWorkingContainerLabel;

    public static String dockerServerModeSwitchTitle;
    public static String dockerServerModeSwitchMessage;
    public static String dockerServerModeSwitchContainerFormat;
    public static String dockerServerModeSwitchImageFormat;

    public static String dockerFlattenImageTitle;
    public static String dockerFlattenImageMessage;
    public static String dockerFlattenImageError;

    public static String dockerRemoteLogonWizardDescription;

    public static String dockerUserDirName;

    public static String wizDockerViewInUseContainersLink;
    public static String wizDockerInUseContainersCannotCreateNewServer;

    public static String containersInUseDialogTitle;
    public static String containersInUseDialogBoldMessage;
    public static String containersInUseDialogTableColumnContainersHeader;
    public static String containersInUseDialogTableColumnServersHeader;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}