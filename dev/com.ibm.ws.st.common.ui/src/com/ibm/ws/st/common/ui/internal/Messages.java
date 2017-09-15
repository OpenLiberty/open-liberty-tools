/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

    public static String L_InputRemoteWASServerInfoWizardTitle;

    public static String L_Browse;
    public static String L_RemoteServerSection;
    public static String L_RemoteServerEnableStart;
    public static String L_RemoteServerPlatform;
    public static String L_RemoteServerPlatform_WIN;
    public static String L_RemoteServerPlatform_LINUX;
    public static String L_RemoteServerPlatform_MAC;
    public static String L_RemoteServerAuthMethod;
    public static String L_RemoteServerAuth_OS;
    public static String L_RemoteServerAuth_SSH;
    public static String L_RemoteServerAuth_SSH_KeyFile;
    public static String L_RemoteServerCredential;
    public static String L_RemoteServerAuth_LogonId;
    public static String L_RemoteServerAuth_LogonPassword;
    public static String L_RemoteServerAuth_SSHId;
    public static String L_RemoteServerAuth_SSHPassphrase;

    // TWAS only
    public static String L_InputRemoteWASServerInfoWizardDescriptionTWAS;
    public static String L_RemoteServerDescription;
    public static String L_RemoteServerPath;
    public static String L_ShouldEnableSecurity;

    // Liberty only
    public static String L_InputRemoteWASServerInfoWizardDescriptionLiberty;
    public static String L_RemoteServerDescriptionLiberty;
    public static String L_RemoteServerDescriptionDocker;
    public static String L_RemoteServerPlatform_Other;
    public static String L_RemoteServerPlatform_OtherTooltip;
    public static String L_RemoteServerLibertyRuntimePath;
    public static String L_RemoteServerLibertyConfigPath;
    public static String L_RemoteServerDebugPortDescription;
    public static String L_RemoteServerDebugPortLabel;
    public static String E_RemoteServerDebugValidation;

    public static String L_SetRemoteServerStartupEnableCommandDescription;
    public static String L_SetRemoteServerStartProfilePathCommandDescription;
    public static String L_SetRemoteServerStartRuntimePathCommandDescription;
    public static String L_SetRemoteServerStartConfigPathCommandDescription;
    public static String L_SetRemoteServerStartPlatformCommandDescription;
    public static String L_SetRemoteServerStartLogonMethodCommandDescription;
    public static String L_SetRemoteServerStartLogonOSIdCommandDescription;
    public static String L_SetRemoteServerStartLogonOSPwdCommandDescription;
    public static String L_SetRemoteServerStartLogonSSHIdCommandDescription;
    public static String L_SetRemoteServerStartLogonSSHPwdCommandDescription;
    public static String L_SetRemoteServerStartLogonSSHKeyFileCommandDescription;
    public static String L_SetRemoteServerStartDebugPortCommandDescription;

    public static String E_RemoteServer_WAS_PATH;
    public static String E_RemoteServer_Liberty_Runtime_PATH;
    public static String E_RemoteServer_Liberty_Config_PATH;
    public static String E_RemoteServer_WAS_PLATFORM;
    public static String E_RemoteServer_LOGON_METHOD;
    public static String E_RemoteServer_OS_LOGON;
    public static String E_RemoteServer_SSH_LOGON;
    public static String E_RemoteServer_OS_LOGON_WIN_NON_IBM_JRE;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}