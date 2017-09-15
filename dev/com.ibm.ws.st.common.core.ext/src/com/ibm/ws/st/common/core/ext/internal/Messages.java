/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

    // Errors
    public static String errorFailedRemoteConnection;
    public static String errorServerSetupFailed;
    public static String errorFailedRemoteConfigUpload;
    public static String errorFailedRemoteStart;
    public static String errorNoServerSetupFound;
    public static String errorNoServerProducersFound;
    public static String errorNoSupportedPlatformFound;
    public static String errorFailedGettingDockerEnv;
    public static String errorFailedDockerCommand;
    public static String errorDockerTempDir;
    public static String errorCopyOut;
    public static String errorDockerOSInfo;

    // Warnings
    public static String warningFileDelete;

    // Flatten image tasks
    public static String flattenExportContainer;
    public static String flattenImportImage;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}