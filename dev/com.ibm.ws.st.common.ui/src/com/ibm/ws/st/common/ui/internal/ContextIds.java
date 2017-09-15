/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.ui.internal;

/**
 * Ids for context help.
 */
public interface ContextIds {
    public static final String PREFIX_ID = Activator.PLUGIN_ID + ".";

    //remote server startup
    public static final String REMOTE_SERVER_STARTUP_IS_ENABLED = PREFIX_ID + "iege0048";
    public static final String REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_WINDOWS = PREFIX_ID + "iege0047";
    public static final String REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_LINUX = PREFIX_ID + "iege0038";
    // TODO: update context ID for MAC
    public static final String REMOTE_SERVER_STARTUP_REMOTE_PLATFORM_MAC = PREFIX_ID + "iege0038";
    public static final String REMOTE_SERVER_STARTUP_SERVER_PROFILE_PATH = PREFIX_ID + "iege0039";
    // TODO: update context ID for Liberty runtime path
    public static final String REMOTE_SERVER_STARTUP_LIBERTY_RUNTIME_PATH = PREFIX_ID + "iege0039";
    // TODO: update context ID for Liberty config path
    public static final String REMOTE_SERVER_STARTUP_LIBERTY_CONFIG_PATH = PREFIX_ID + "iege0039";
    public static final String REMOTE_SERVER_STARTUP_OS_AUTHENTICATION = PREFIX_ID + "iege0040";
    public static final String REMOTE_SERVER_STARTUP_OS_USER_NAME = PREFIX_ID + "iege0041";
    public static final String REMOTE_SERVER_STARTUP_OS_PASSWORD = PREFIX_ID + "iege0042";
    public static final String REMOTE_SERVER_STARTUP_SSH_AUTHENTICATION = PREFIX_ID + "iege0043";
    public static final String REMOTE_SERVER_STARTUP_SSH_PRIVATE_KEY = PREFIX_ID + "iege0044";
    public static final String REMOTE_SERVER_STARTUP_SSH_USER_NAME = PREFIX_ID + "iege0045";
    public static final String REMOTE_SERVER_STARTUP_SSH_PASSPHRASE = PREFIX_ID + "iege0046";
    public static final String REMOTE_SERVER_STARTUP_DEBUG_PORT = PREFIX_ID + "iege0047";
}