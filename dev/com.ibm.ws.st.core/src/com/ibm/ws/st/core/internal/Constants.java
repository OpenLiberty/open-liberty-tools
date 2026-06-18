/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

public class Constants {
    // path constants
    public static final String USER_FOLDER = "usr";
    public static final String SERVERS_FOLDER = "servers";
    public static final String SHARED_FOLDER = "shared";
    public static final String APPS_FOLDER = "apps";
    public static final String RESOURCES_FOLDER = "resources";
    public static final String CONFIG_FOLDER = "config";
    protected static final String ROOT_FOLDER = "wlp";
    public static final String LIB_FOLDER = "lib";
    public static final String ETC_FOLDER = "etc";
    public static final String EXTENSION_FOLDER = "extension";
    public static final String USR_EXTENSION_FOLDER = "usr/extension";
    public static final String TEMPLATES_FOLDER = "templates/servers";
    public static final String TEMPLATES_DEFAULT_NAME = "defaultServer";
    public static final String REMOTE_USR_FOLDER = "remoteUsr";
    public static final String SECURITY_FOLDER = "security";
    public static final String COLLECTIVE_FOLDER = "collective";
    public static final String CONFIG_DROPINS_FOLDER = "configDropins";
    public static final String CONFIG_DEFAULT_DROPINS_FOLDER = "defaults";
    public static final String CONFIG_OVERRIDE_DROPINS_FOLDER = "overrides";
    public static final String KEYSTORE_XML = "keystore";

    public static final String BATCH_SCRIPT = "server";
    public static final String SERVER_XML = "server.xml";
    public static final String CONSOLE_LOG = "console.log";
    public static final String MESSAGES_LOG = "messages.log";
    public static final String TRACE_LOG = "trace.log";
    public static final String SERVER_ENV = "server.env";

    // include conflict resolution labels
    public static final String CONFLICT_MERGE_LABEL = "MERGE";
    public static final String CONFLICT_REPLACE_LABEL = "REPLACE";
    public static final String CONFLICT_IGNORE_LABEL = "IGNORE";
    public static final String CONFLICT_BREAK_LABEL = "BREAK";

    // environment variables
    public static final String WLP_USER_DIR = "WLP_USER_DIR";
    public static final String WLP_OUTPUT_DIR = "WLP_OUTPUT_DIR";
    public static final String ENV_VAR_PREFIX = "env.";
    public static final String ENV_LOG_DIR = "LOG_DIR";

    // configuration file constants
    public static final String SERVER_ELEMENT = "server";
    public static final String INCLUDE_ELEMENT = "include";
    public static final String LOCATION_ATTRIBUTE = "location";
    public static final String OPTIONAL_ATTRIBUTE = "optional";
    public static final String ONCONFLICT_ATTRIBUTE = "onConflict";
    public static final String FEATURE_MANAGER = "featureManager";
    public static final String FEATURE = "feature";
    public static final String FEATURE_LOCAL_JMX = "localConnector-1.0";
    /** Should get the latest version for restConnector using FeatureSet.resolve(String, String[]) */
    //    public static final String FEATURE_REST_JMX = "restConnector-1.0";

    public static final String INSTANCE_ID = "id";
    public static final String APPLICATION = "application";
    public static final String WEB_APPLICATION = "webApplication";
    public static final String APP_AUTOSTART = "autoStart";
    public static final String APP_NAME = "name";
    public static final String APP_LOCATION = "location";
    public static final String APP_TYPE = "type";
    public static final String APP_CONTEXT_ROOT = "context-root";
    public static final String APP_CONTEXT_ROOT_NEW = "contextRoot";
    public static final String LOGGING = "logging";
    public static final String LOG_DIR = "logDirectory";
    public static final String TRACE_FILE = "traceFileName";
    public static final String MESSAGES_FILE = "messageFileName";
    public static final String HTTP_ENDPOINT = "httpEndpoint";
    public static final String DEFAULT_HTTP_ENDPOINT = "defaultHttpEndpoint";
    public static final String HTTP_PORT = "httpPort";
    public static final String HTTPS_PORT = "httpsPort";
    public static final String HTTP_WHITEBOARD = "httpWhiteboard";
    public static final String HTTP_WHITEBOARD_CONTEXTPATH = "contextPath";
    public static final String DATASOURCE_VENDOR_PROPERTIES = "properties";
    public static final String APPLICATION_MONITOR = "applicationMonitor";
    public static final String APPLICATION_MONITOR_TRIGGER = "updateTrigger";
    public static final String APPLICATION_MONITOR_MBEAN = "mbean";
    public static final String JDBC_DRIVER = "jdbcDriver";
    public static final String FACTORY_ID = "id";
    public static final String SHARED_LIBRARY = "library";
    public static final String SHARED_LIBRARY_REF = "libraryRef";
    public static final String FILESET = "fileset";
    public static final String FILESET_REF = "filesetRef";
    public static final String FILESET_DIR = "dir";
    public static final String FILESET_INCLUDES = "includes";
    public static final String FILESET_EXCLUDES = "excludes";
    public static final String VARIABLE_ELEMENT = "variable";
    public static final String VARIABLE_NAME = "name";
    public static final String VARIABLE_VALUE = "value";
    public static final String VARIABLE_DEFAULT_VALUE = "defaultValue";
    public static final String VARIABLE_USER = "user";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String FACTORY_TYPE_ID = "factoryIdType";
    public static final String DATA_SOURCE = "dataSource";
    public static final String BASIC_USER_REGISTY = "basicRegistry";
    public static final String ADMIN_ROLE = "administrator-role";
    public static final String QUICK_START_SECURITY = "quickStartSecurity";
    public static final String USER_NAME = "userName";
    public static final String USER_PASSWORD = "userPassword";
    public static final String KEY_STORE = "keyStore";
    public static final String DEFAULT_KEY_STORE = "defaultKeyStore";
    public static final String PASSWORD_ATTRIBUTE = "password";
    public static final String SSL_ELEMENT = "ssl";
    public static final String KEYSTORE_REF_ATTR = "keyStoreRef";
    public static final String DEFAULT_SSL_CONFIG_ID = "defaultSSLConfig";
    public static final String SSL_DEFAULT_ELEMENT = "sslDefault";
    public static final String SSL_REF_ATTR = "sslRef";
    public static final String REMOTE_FILE_ACCESS = "remoteFileAccess";
    public static final String WRITE_DIR = "writeDir";

    // configuration file types
    public static final String BOOLEAN_TYPE = "booleanType";
    public static final String SHORT_TYPE = "shortType";
    public static final String INT_TYPE = "intType";
    public static final String LONG_TYPE = "longType";
    public static final String DURATION_TYPE = "duration";
    public static final String MINUTE_DURATION_TYPE = "minuteDuration";
    public static final String SECOND_DURATION_TYPE = "secondDuration";
    public static final String LOCATION_TYPE = "location";
    public static final String PID_TYPE = "pidType";
    public static final String PID_LIST_TYPE = "pidListType";
    public static final String PASSWORD_TYPE = "password";
    public static final String PASSWORD_HASH_TYPE = "passwordHash";

    // generic schema types
    public static final String XSD_BOOLEAN_TYPE = "boolean";
    public static final String XSD_SHORT_TYPE = "short";
    public static final String XSD_INT_TYPE = "int";
    public static final String XSD_LONG_TYPE = "long";
    public static final String XSD_STRING_TYPE = "string";
    public static final String XSD_TOKEN_TYPE = "token";

    // shared library
    public static final String LIBRARY = "library";
    public static final String LIB_FILESET = "fileset";
    public static final String LIB_DIR = "dir";
    public static final String LIB_INCLUDES = "includes";
    public static final String LIB_CLASSLOADER = "classloader";
    public static final String LIB_COMMON_LIBREF = "commonLibraryRef";
    public static final String LIB_PRIVATE_LIBREF = "privateLibraryRef";
    public static final String FILE = "file";
    public static final String FOLDER = "folder";
    public static final String API_VISIBILITY_ATTRIBUTE_NAME = "apiTypeVisibility";
    public static final String API_VISIBILITY_ATTRIBUTE_VALUE_API = "api";
    public static final String API_VISIBILITY_ATTRIBUTE_VALUE_IBM_API = "ibm-api";
    public static final String API_VISIBILITY_ATTRIBUTE_VALUE_SPEC = "spec";
    public static final String API_VISIBILITY_ATTRIBUTE_VALUE_STABLE = "stable";
    public static final String API_VISIBILITY_ATTRIBUTE_VALUE_THIRD_PARTY = "third-party";

    public static final String SHARED_LIBRARY_SETTING_API_VISIBILITY_API_KEY = "com.ibm.ws.st.core.api.visibility.api";
    public static final String SHARED_LIBRARY_SETTING_API_VISIBILITY_IBM_API_KEY = "com.ibm.ws.st.core.api.visibility.ibmapi";
    public static final String SHARED_LIBRARY_SETTING_API_VISIBILITY_SPEC_KEY = "com.ibm.ws.st.core.api.visibility.spec";
    public static final String SHARED_LIBRARY_SETTING_API_VISIBILITY_STABLE_KEY = "com.ibm.ws.st.core.api.visibility.stable";
    public static final String SHARED_LIBRARY_SETTING_API_VISIBILITY_THIRD_PARTY_KEY = "com.ibm.ws.st.core.api.visibility.thirdparty";

    // prefix of runtime and server ids
    public static final String ID_PREFIX = "com.ibm.ws.st";
    public static final String RUNTIME_ID_PREFIX = ID_PREFIX + ".runtime";
    public static final String SERVER_ID_PREFIX = ID_PREFIX + ".server";

    // suffix of runtime and server ids
    public static final String WLP_ID_SUFFIX = ".wlp";
    public static final String V85_ID_SUFFIX = ".v85.was";

    // runtime and server ids
    public static final String RUNTIME_TYPE_ID = RUNTIME_ID_PREFIX + WLP_ID_SUFFIX;
    public static final String SERVER_TYPE_ID = SERVER_ID_PREFIX + WLP_ID_SUFFIX;
    public static final String RUNTIMEV85_TYPE_ID = RUNTIME_ID_PREFIX + V85_ID_SUFFIX;
    public static final String SERVERV85_TYPE_ID = SERVER_ID_PREFIX + V85_ID_SUFFIX;

    // default runtime id
    public static final String DEFAULT_RUNTIME_TYPE_ID = RUNTIME_TYPE_ID;

    // for local JMX connection
    public static final String VM_SERVER_PATH = "was.server.dir";

    // minimal config content
    public static final String INITIAL_CONFIG_CONTENT = "<server>\n    \n</server>";

    public static final String RUNTIME_COMPONENT_SERVICE_VERSION_ID = "com.ibm.ws.st.runtime.serviceVersion";

    public static final String JOB_FAMILY = Activator.PLUGIN_ID + ".job.family";

    // configuration variables
    public static final String SERVER_CONFIG_VAR = "${server.config.dir}";
    public static final String SERVER_OUTPUT_VAR = "${server.output.dir}";
    public static final String WLP_USER_DIR_VAR = "${wlp.user.dir}";
    public static final String WLP_INSTALL_VAR = "${wlp.install.dir}";
    public static final String LOGGING_DIR_VAR = "${com.ibm.ws.logging.log.directory}";
    public static final String MESSAGES_FILENAME_VAR = "${com.ibm.ws.logging.message.file.name}";

    //file names
    public static final String GENERATED_SSL_INCLUDE = "GeneratedSSLInclude.xml";
    public static final String REMOTE_CONFIG_SYNC_FILENAME = "remoteConfigSyncInfo.properties";
    public static final String CONFIG_SYNC_FILENAME = "configSyncInfo.properties";
    public static final String SSL_KEY_FILE = "key.jks";
    public static final String IGNORED_FEATURES = "ignoredFeatures.xml";

    //file extensions
    public static final String ZIP_EXT = ".zip";
    public static final String JAR_EXT = ".jar";

    //Utility enable/disable preferences
    public static final String REMOTE_SETTINGS_DISABLED_PROMPT = "remoteSettingsDisabledPrompt";
    public static final String UTILITY_NOT_SUPPORTED_PROMPT = "utilityNotSupportedPrompt";

    //Delete server action preferences
    public static final String DELETE_PROJECT_FILES_ALWAYS_ACTION = "deleteProjectFilesAlwaysAction";
    public static final String DELETE_PROJECT_FILES_APPLY_ALWAYS = "deleteProjectFilesApplyAlways";

    // repo
    public static final String INSTALL_UTILITY_CMD = "installUtility";
    public static final String INSTALL_UTILITY_JAR = "ws-installUtility.jar";
    public static final String FEATURE_MANAGER_CMD = "featureManager";

    // A special case needs to be done in generic publishers, which requires the type to
    // be known to plugins that should not reference the LibertyMaven plugins directly
    public static final String SERVER_TYPE_LIBERTY_MAVEN = "LibertyMaven";

    public static final String SERVER_TYPE_LIBERTY_GRADLE = "LibertyGradle";

    // Runtime properties
    public static final String RUNTIME_PROP_PRODUCT_ID = "com.ibm.websphere.productId";
    public static final String RUNTIME_PROP_PRODUCT_VERSION = "com.ibm.websphere.productVersion";
    public static final String RUNTIME_PROP_PRODUCT_EDITION = "com.ibm.websphere.productEdition";
    public static final String RUNTIME_PROP_PRODUCT_INSTALL_TYPE = "com.ibm.websphere.productInstallType";
    public static final String RUNTIME_PROP_PRODUCT_LICENSE_TYPE = "com.ibm.websphere.productLicenseType";
}