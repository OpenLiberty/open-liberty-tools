/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {
    public static String actionServerName;
    public static String cmdSetEnableLooseConfig;
    public static String cmdSetStopServerOnShutdown;
    public static String cmdSetServerUserName;
    public static String cmdSetServerPassword;
    public static String cmdSetServerPort;
    public static String unknownMsg;

    public static String errorTimeout;
    public static String taskServerCreate;
    public static String errorServerCreate;
    public static String taskServerStatus;
    public static String errorServerStatus;
    public static String taskPackageServer;
    public static String errorPackagingServer;
    public static String taskPackagePublish;
    public static String taskPackaging;
    public static String taskDumpServer;
    public static String errorDumpServer;
    public static String taskCreateSSLCertificate;
    public static String errorCreateSSLCertificate;
    public static String taskEncodePassword;
    public static String taskListEncryption;
    public static String taskEncryptPassword;
    public static String errorEncodePassword;
    public static String errorEncodePasswordUnsupportedEncoding;
    public static String taskProductInfo;
    public static String errorProductInfo;
    public static String taskInstallFeature;
    public static String taskDeleteDropinsApplication;
    public static String errorDeleteDropinsApplication;

    public static String processLabel;
    public static String processLabelAttr;
    public static String debugTargetLabel;
    public static String runtimeName;
    public static String runtimeFeature;
    public static String runtimeServiceVersion;

    public static String jobRuntimeCache;
    public static String metadataGenerationFailedDetails;
    public static String jobRefreshRuntimeMetadata;
    public static String jobBuildNonJavaProjects;
    public static String jobInitializeConfigSync;
    public static String jobRefreshConfigurationFiles;

    public static String warningFileDelete;

    public static String errorInstallDirMissing;
    public static String errorInstallDirTrailingSlash;
    public static String errorJRE;
    public static String errorJRE60;
    public static String errorNoRuntime;
    public static String errorModuleNotRecognized;
    public static String errorPortInvalid;
    public static String errorPortInUse;
    public static String errorPortsInUse;
    public static String errorServerName;
    public static String errorMovingConfiguration;
    public static String errorSavingBootstrap;
    public static String errorNoServer;
    public static String errorAddingSharedConfig;
    public static String errorRefreshingFolder;
    public static String errorRequiredFeature;
    public static String featureConflict;
    public static String featureConflictWithLoc;
    public static String errorServerConfigurationModifyFailed;
    public static String errorServerFolderNotAccessible;
    public static String errorAppsFolderNotAccessible;
    public static String errorOutputFolderNotAccessible;

    public static String errorLoadingInclude;
    public static String infoMultipleInclude;
    public static String infoOverrideItem;
    public static String infoOverrideItemDropin;
    public static String infoDuplicateItem;
    public static String infoDuplicateItemDropin;
    public static String infoReplaceItem;
    public static String warningPlainTextPassword;
    public static String warningAESEncryptedPasswordNotSupported;
    public static String warningHashEncodedPasswordNotSupported;
    public static String warningCustomEncryptedPasswordNotSupported;
    public static String unrecognizedProperty;
    public static String unrecognizedElement;
    public static String unavailableElement;
    public static String unresolvedPropertyValue;
    public static String incorrectVariableReferenceType;
    public static String invalidVariableExpression;
    public static String expressionMissingLeftOperand;
    public static String expressionMissingRightOperand;
    public static String expressionUndefinedLeftOperand;
    public static String expressionUndefinedRightOperand;
    public static String expressionInvalidLeftOperand;
    public static String expressionInvalidRightOperand;
    public static String unrecognizedFeature;
    public static String supersededFeature;
    public static String invalidValue;
    public static String invalidValueNoType;
    public static String emptyRequiredAttribute;
    public static String factoryIdNotFound;
    public static String factoryIdNotFoundMulti;
    public static String duplicateFactoryId;
    public static String singlePidRefAndNested;
    public static String variableNameContainsRefs;
    public static String missingKeystore;
    public static String missingKeystoreAndUR;
    public static String securePortMismatch;

    public static String invalidWhitespace;
    public static String invalidJVMOption;
    public static String invalidUnicode;
    public static String invalidZipFile;
    public static String serverMustBeStopped;

    public static String errorDeletingServer;

    public static String publishingModule;
    public static String publishModule;
    public static String errorPublishing;
    public static String internalErrorPublishing;
    public static String errorPublishModule;
    public static String jmxConnectionFailure;
    public static String remoteJMXConnectionFailure;
    public static String errorPublishJMX;
    public static String deployAppSuccessfully;
    public static String deployAppFailed;
    public static String errorDeleteFile;
    public static String publishErrorTitle;
    public static String publishConfigSyncError;
    public static String remotePublishConfigSyncFailed;
    public static String publishConfigSyncNoJMXConnector;
    public static String publishConfigSyncCanceled;
    public static String publishConfigSyncSuccess;
    public static String publishWaitingForStatus;
    public static String errorRemotePublishPackage;
    public static String errorRemotePublishTransfer;
    public static String errorRemoteConfigResolution;
    public static String errorPromptRemoteServerSettingsDisabled;
    public static String errorPromptRemoteServerActionsUnavailable;

    public static String publishPromptMessage;
    public static String requiredFeatureIssue;
    public static String requiredFeatureSummary;
    public static String requiredFeatureDetails;

    public static String appMonitorTriggerMBeanIssue;
    public static String appMonitorTriggerMBeanSummary;
    public static String appMonitorTriggerMBeanDetails;

    public static String errorApplicationStart;
    public static String errorApplicationStop;
    public static String errorApplicationRestart;
    public static String warningApplicationNotStarted;

    public static String publishedModuleNotInConfig;
    public static String publishedModuleSharedLibRefMismatchInConfig;

    public static String remoteServerDeletePromptMessage;
    public static String remoteServerDeletePromptIssue;
    public static String remoteServerDeletePromptSummary;
    public static String remoteServerDeletePromptDetails;

    public static String promptActionIgnore;
    public static String promotActionRemoveFromServer;
    public static String promptUpdateServerConfiguration;
    public static String promptActionRestartServers;
    public static String promptActionRestartApplications;
    public static String promptActionDeleteServers;
    public static String promptActionStopServer;
    public static String prompt_yes;
    public static String prompt_no;

    public static String waitForApplicationDialogMsg;

    public static String applicationLabel;
    public static String sharedLibraryLabel;
    public static String outOfSyncIssue;
    public static String outOfSyncAppMissingSummary;
    public static String outOfSyncAppMissingDetails;
    public static String outOfSyncSharedLibRefMismatchSummary;
    public static String outOfSyncSharedLibRefMissingSummary;
    public static String outOfSyncSharedLibRefMissingDetails;
    public static String outOfSyncSharedLibRefNotUsedSummary;
    public static String outOfSyncSharedLibRefNotUsedDetails;
    public static String outOfSyncSharedLibRefAPIVisibilityChangedSummary;
    public static String outOfSyncSharedLibRefAPIVisibilityChangedDetails;

    public static String durationDayAbbreviation;
    public static String durationHourAbbreviation;
    public static String durationMinuteAbbreviation;
    public static String durationSecondAbbreviation;
    public static String durationMillisecondAbbreviation;

    public static String hotCodeReplaceFailedTitle;
    public static String hotCodeReplaceFailedIssue;
    public static String hotCodeReplaceFailedSummary;
    public static String hotCodeReplaceFailedGeneralMsg;
    public static String hotCodeReplaceFailedRestartServerMsg;
    public static String hotCodeReplaceFailedRestartModuleMsg;

    public static String featureInstallFailedMsg;

    public static String productLabel;
    public static String extendedProductLabel;
    public static String sampleLabel;
    public static String featureLabel;
    public static String openSourceLabel;
    public static String configSnippetLabel;
    public static String iFixLabel;
    public static String defaultDescription;
    public static String errorRuntimeLocationMissing;
    public static String errorInstallProcessFailed;

    public static String serverConfigurationDropins;

    public static String mergedConfigBeginInclude;
    public static String mergedConfigEndInclude;
    public static String mergedConfigBeginDropin;
    public static String mergedConfigEndDropin;

    public static String X509_CANNOT_LOAD_TRANSIENT_KEYSTORE;
    public static String X509_CANNOT_LOAD_PERSISTENT_KEYSTORE;
    public static String X509_CANNOT_READ_PERSISTENT_KEYSTORE;
    public static String X509_CANNOT_WRITE_PERSISTENT_KEYSTORE;
    public static String X509_CANNOT_GET_PLUGIN_STATE_LOCATION;
    public static String X509_EXTENSION_HAS_WRONG_CLASS;
    public static String X509_EXTENSION_HAS_NO_CLASS;

    public static String X509_CERT_LABEL_ISSUER_DN;
    public static String X509_CERT_LABEL_ISSUER_ID;
    public static String X509_CERT_LABEL_ISSUER_ALTERNATE_NAMES;
    public static String X509_CERT_LABEL_SUBJECT_DN;
    public static String X509_CERT_LABEL_SUBJECT_ID;
    public static String X509_CERT_LABEL_SUBJECT_ALTERNATE_NAMES;
    public static String X509_CERT_LABEL_VERSION;
    public static String X509_CERT_LABEL_SERIAL;
    public static String X509_CERT_LABEL_NOT_BEFORE;
    public static String X509_CERT_LABEL_NOT_AFTER;
    public static String X509_CERT_LABEL_SIG_ALG_NAME;
    public static String X509_CERT_LABEL_SIG_ALG_OID;
    public static String X509_CERT_LABEL_PUBLIC_KEY_ENCODED;
    public static String X509_CERT_LABEL_PUBLIC_KEY_FORMAT;
    public static String X509_CERT_LABEL_PUBLIC_KEY_ALGORITHM;
    public static String X509_CERT_LABEL_SIGNATURE;
    public static String X509_CERT_LABEL_KEY_USAGE;
    public static String X509_CERT_LABEL_EXT_KEY_USAGE;
    public static String X509_CERT_LABEL_BASIC_CONSTRAINTS;

    public static String X509_BINARY;
    public static String X509_HEX;
    public static String X509_ALTERNATIVE_NAME;
    public static String X509_KEY_USAGE_SEPARATOR;

    public static String L_RemoteExecutingCommands;

    public static String E_RemoteServer_ProfilePathInvalid;
    public static String E_RemoteServer_ServerNameInvalid;
    public static String E_RemoteServer_HostNameInvalid;
    public static String E_RemoteServer_IDInvalid;
    public static String E_RemoteServer_passwordInvalid;
    public static String E_RemoteServer_sshKeyFileInvalid;
    public static String E_RemoteServer_ErrorUtilityAction;
    public static String W_RemoteServer_Problem;
    public static String W_RemoteServer_CommandReturnCode;
    public static String W_RemoteServer_CommandReturnCodeWithError;
    public static String errorPromptCannotRunRemoteUtility;
    public static String errorPromptRemoteUtilityNotSupported;
    public static String errorPromptLocalDockerUtilityNotSupported;

    public static String errorPromptServerNotFound;
    public static String remoteUtilitiesErrorTitle;
    public static String remoteActionsUnavailable;

    public static String remoteDownloadingServerConfigFile;
    public static String remoteServerDownloadFailed;
    public static String failedCreateDefaultUserDir;

    public static String libertyProducerRuntimeNull;
    public static String libertyProducerMissingInfo;
    public static String libertyProducerServerExists;

    public static String errorDockerInfo;

    public static String taskJavaDump;
    public static String taskStartServer;
    public static String taskStopServer;
    public static String taskGenericStartServer;
    public static String taskGenericStopServer;
    public static String jobWaitForServerStart;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}