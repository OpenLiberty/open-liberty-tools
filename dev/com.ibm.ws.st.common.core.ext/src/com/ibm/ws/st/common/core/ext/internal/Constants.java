/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.common.core.ext.internal;

public class Constants {

    /**
     * OS & environment constants
     */
    public static final String HOSTNAME = "hostname";
    public static final String SERVER_LABEL = "serverLabel";
    public static final String OS_NAME = "osName";
    public static final String DEBUG_PORT = "debugPort";

    /**
     * Remote user name and password (used for OS login and SSH login - and potentially others)
     */
    public static final String OS_USER = "osUser";
    public static final String OS_PASSWORD = "osPassword";

    /**
     * Docker constants
     */
    public static final String DOCKER_MACHINE_TYPE = "dockerMachineType";
    public static final String DOCKER_MACHINE = "dockerMachine";
    public static final String DOCKER_CONTAINER = "dockerContainer";
    public static final String DOCKER_IMAGE = "dockerImage";

    public static final String LOGON_METHOD = "logonMethod";
    public static final String LOGON_METHOD_OS = "logonMethodOS";
    public static final String LOGON_METHOD_SSH = "logonMethodSSH";
    public static final String SSH_KEY = "sshKey";

    public static final String LOOSE_CFG = "looseCfg";

    /**
     * Liberty constants
     */
    public static final String LIBERTY_SERVER_NAME = "libertyServerName";
    public static final String LIBERTY_USER_DIR = "libertyUserDir";
    public static final String LIBERTY_CONFIG_SOURCE = "libertyConfigSource";
    public static final String LIBERTY_USER = "libertyUser";
    public static final String LIBERTY_PASSWORD = "libertyPassword";
    public static final String LIBERTY_HTTPS_PORT = "libertyHttpsPort";
    public static final String LIBERTY_HTTP_PORT = "libertyHttpPort";
    public static final String LIBERTY_SERVER_CONFIG_PATH = "libertyServerConfigPath";
    public static final String LIBERTY_RUNTIME_INSTALL_PATH = "LibertyRuntimeInstallPath";

    public static final String DROPINS_DIR = "configDropins";
    public static final String DEFAULTS_DIR = "defaults";
    public static final String OVERRIDES_DIR = "overrides";

    public static final String[] LIBERTY_VALIDATION_KEYS = { HOSTNAME, SERVER_LABEL, OS_NAME, OS_USER, OS_PASSWORD, LOGON_METHOD, SSH_KEY, LIBERTY_SERVER_NAME, LIBERTY_USER,
                                                             LIBERTY_PASSWORD,
                                                             LIBERTY_HTTPS_PORT, LIBERTY_SERVER_CONFIG_PATH, LIBERTY_RUNTIME_INSTALL_PATH };

    public static final String LIBERTY_REMOTE_ADMINISTRATION_NAME = "remoteAdministration.xml";
    public static final String LIBERTY_OLD_REMOTE_ADMINISTRATION_NAME = "st_remoteAdministration_st.xml";
    public static final String LIBERTY_DEFAULTS_PATH = "/" + DROPINS_DIR + "/" + DEFAULTS_DIR + "/";
    public static final String LIBERTY_BASIC_REGISTRY_NAME = "basicRegistry.xml";
    public static final String LIBERTY_OLD_BASIC_REGISTRY_NAME = "st_basicRegistry_st.xml";
}
