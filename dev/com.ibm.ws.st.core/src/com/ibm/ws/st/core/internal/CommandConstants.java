/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

/**
 * This class defines the strings used to run server commands
 */
public class CommandConstants {

    public static final String UTILITY_TYPE = "utilityType";
    public static final String START_SERVER = "start";
    public static final String STOP_SERVER = "stop";
    public static final String DEBUG_SERVER = "debug";
    public static final String PACKAGE_SERVER = "package";
    public static final String DUMP_SERVER = "dump";
    public static final String JAVA_DUMP = "javadump";
    public static final String SERVER = "server";

    public static final String HOST = "--host=";
    public static final String PORT = "--port=";
    public static final String USER = "--user=";
    public static final String PASSWORD = "--password=";

    public static final String GENERAL_ARCHIVE = "--archive=";
    public static final String GENERAL_INCLUDE = "--include=";
    public static final String CREATE_CONFIG_FILE = "--createConfigFile=";

    //create SSL certificate
    public static final String CREATE_SSL_CERTIFICATE = "createSSLCertificate";
    public static final String CREATE_SSL_CERTIFICATE_SECURITY_UTIL = "securityUtility";
    public static final String CREATE_SSL_CERTIFICATE_SERVER = "--server=";
    public static final String CREATE_SSL_CERTIFICATE_PASSWORD_ENCODING = "--passwordEncoding=";
    public static final String CREATE_SSL_CERTIFICATE_PASSWORD_KEY = "--passwordKey=";
    public static final String CREATE_SSL_CERTIFICATE_VALIDITY = "--validity=";
    public static final String CREATE_SSL_CERTIFICATE_SUBJECT = "--subject=";

    public static final String deleteExisting = "deleteExisting";
}
