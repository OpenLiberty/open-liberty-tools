/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {
    public static String wizRuntimeTitle;
    public static String wizRuntimeDescription;

    public static String wizServerTitle;
    public static String wizServerDescription;
    public static String wizServerDescriptionLabel;
    public static String wizServerConfiguration;
    public static String wizServerConfigModeGroup;
    public static String wizServerCopyConfigButton;
    public static String wizServerLinkConfigButton;
    public static String wizServerRefreshing;
    public static String warningServerAlreadyDefined;

    public static String wizRemoteServerTitle;
    public static String wizRemoteServerDescription;
    public static String wizRemoteConnectionGroup;
    public static String wizRemoteUserLabel;
    public static String wizRemotePasswordLabel;
    public static String wizRemotePortLabel;
    public static String wizRemoteDirectoryLabel;
    public static String wizRemoteConnect;
    public static String wizRemoteConnectTooltip;
    public static String wizRemoteDownloadingServerConfigFile;
    public static String wizRemoteUserNotSet;
    public static String wizRemotePasswordNotSet;
    public static String wizRemotePortNotSet;
    public static String wizRemoteServerDownloadFailed;
    public static String errorRemoteServerConfigInvalid;
    public static String errorRemoteServerSetup;
    public static String infoOnlyServerConfigSynchronized;

    public static String wizServerTypeTitle;
    public static String wizServerTypeDescription;
    public static String wizServerTypeRadioGroupLabel;
    public static String wizServerTypeLibertyServer;

    public static String wizServerNameTitle;
    public static String wizServerNameDescription;
    public static String wizServerNameFormat;
    public static String errorServerNameExists;
    public static String infoNoUsrDirs;
    public static String failedCreateDefaultUserDir;

    public static String wizLicenseTitle;
    public static String wizLicenseDescription;
    public static String wizLicenseAccept;
    public static String wizLicenseDecline;
    public static String wizLicenseMissing;
    public static String wizLicenseError;

    public static String wizPackageTitle;
    public static String wizPackageDescription;
    public static String wizPackageMessage;
    public static String wizPackageExport;
    public static String wizPackageOverwrite;
    public static String wizPackageFileExists;
    public static String wizPackageInclude;
    public static String wizPackageIncludeAll;
    public static String wizPackageIncludeUsr;
    public static String wizPackageIncludeMinify;
    public static String wizPackageIncludeCommonMessage;
    public static String errorPackageServerRunning;
    public static String errorPackageInvalidFile;
    public static String notifyPublishRequired;

    public static String wizSSLCertificateTitle;
    public static String wizSSLCertificateDescription;
    public static String wizSSLCertificateMessage;
    public static String wizSSLCertificateValidity;
    public static String wizSSLCertificateSubject;
    public static String errorSSLValidity;
    public static String errorSSLSubject;
    public static String serverXMLUpdated;

    public static String wizCreateCollectiveTitle;
    public static String wizCreateCollectiveDescription;
    public static String wizCreateCollectiveMessage;

    public static String errorExistingCollective;
    public static String wizCollectiveRecreate;

    public static String wizJoinCollectiveTitle;
    public static String wizJoinCollectiveDescription;
    public static String wizJoinCollectiveMessage;
    public static String wizJoinCollectiveHost;
    public static String wizJoinCollectivePort;
    public static String wizJoinCollectiveUser;
    public static String wizJoinCollectivePassword;
    public static String wizJoinCollectiveMessage2;
    public static String errorPort;

    public static String wizPluginConfigTitle;
    public static String wizPluginConfigDescription;
    public static String wizPluginConfigMessage;
    public static String wizPluginConfigStoppedMessage;
    public static String wizPluginConfigError;
    public static String wizPluginConfigFailed;

    public static String wizDumpTitle;
    public static String wizDumpDescription;
    public static String wizDumpMessage;
    public static String wizDumpInclude;
    public static String wizDumpServer;
    public static String wizDumpServerIncludeHeap;
    public static String wizDumpServerIncludeSystem;
    public static String wizDumpServerIncludeThread;
    public static String wizDumpJVM;
    public static String wizDumpJVMIncludeHeap;
    public static String wizDumpJVMIncludeSystem;

    public static String title;
    public static String name;
    public static String browse;
    public static String connect;
    public static String create;
    public static String remove;
    public static String url;
    public static String user;
    public static String password;
    public static String passwordEncoding;
    public static String passwordXOR;
    public static String passwordAES;
    public static String passwordHash;
    public static String passwordExplanation;
    public static String passwordKey;
    public static String passwordShow;
    public static String keystorePassword;
    public static String errorPassword;
    public static String userDirectory;
    public static String template;
    public static String overwriteExistingFiles;
    public static String errorBackupFile;
    public static String stopServerToDeleteExistingCollective;
    public static String errorDeleteExistingController;
    public static String notifyOverwriteRemoteConfigFile;

    public static String runtimeInstallGroup;
    public static String runtimeInstallMessage;
    public static String runtimeInstallLink;
    public static String runtimeInstallPath;
    public static String runtimeJREGroup;
    public static String runtimeJRESpecific;
    public static String runtimeJREDefault;
    public static String runtimeJREConfigure;
    public static String runtimeAdvancedLink;
    public static String runtimeAdvancedTitle;
    public static String runtimeAdvancedDescription;
    public static String runtimeUserDirGroup;
    public static String runtimeUserDirNotFound;
    public static String runtimeCacheDescription;
    public static String runtimeCacheRefresh;
    public static String runtimeCreateOptionLabel;
    public static String runtimeExistingDirLabel;
    public static String runtimeNewDirLabel;
    public static String runtimeArchiveInstallLabel;

    public static String serverName;
    public static String server;
    public static String serverType;
    public static String editorGeneralTitle;
    public static String editorGeneralDescription;
    public static String editorGeneralOpenConfiguration;
    public static String editorGeneralLooseConfig;
    public static String editorGeneralLooseConfigMessage;
    public static String editorGeneralStopServerOnShutdown;
    public static String editorGeneralStopServerOnShutdownMessage;

    public static String editorVerifyConnection;
    public static String editorConnectionSuccessful;
    public static String editorVerifyConnectionError;
    public static String editorVerifyConnectTooltip;
    public static String editorServerConnectionSettingsTitle;
    public static String editorServerConnectionSettingsDescription;
    public static String editorConnectionInfoValidationError;
    public static String editorConnectionInfoPortValidationError;

    public static String serverDecoratorErrors;
    public static String serverTooltipName;
    public static String serverTooltipLocation;
    public static String serverTooltipConfigRoot;
    public static String serverTooltipErrors;
    public static String actionOpenConfiguration;
    public static String actionOpenFolder;
    public static String actionOpenMergedConfiguration;
    public static String actionOpenConfigurationSchema;
    public static String errorDialogTitleOpenMergedView;
    public static String errorDialogMessageOpenMergedView;
    public static String actionRefresh;
    public static String actionNew;
    public static String actionNewRuntime;
    public static String actionEditRuntime;
    public static String actionNewServer;
    public static String actionNewQuickServer;
    public static String actionNewWebSphereServer;
    public static String actionShowIn;
    public static String actionDelete;
    public static String menuUtilities;
    public static String menuOpenLogFiles;
    public static String actionOpenLogMessages;
    public static String menuPreviousMessageLogs;
    public static String actionOpenTraceLog;
    public static String menuPreviousTraceLogs;
    public static String downloadLogDialogTitle;
    public static String downloadLogFailure;
    public static String downloadLogDialogMessage;
    public static String downloadLogJob;
    public static String downloadLogError;
    public static String actionPackage;
    public static String actionPluginConfig;
    public static String actionDump;
    public static String actionCreateSSL;
    public static String actionCreateController;
    public static String actionJoinCollective;
    public static String actionProperties;
    public static String actionInstallAddOn;
    public static String actionInstallFeatures;
    public static String actionAddConfigSnippets;
    public static String menuNewExtendedConfig;
    public static String confirmProjectOpen;

    public static String runtimeServers;
    public static String runtimeSharedConfigurations;
    public static String runtimeSharedApplications;
    public static String runtimeSharedResources;
    public static String confirmDelete;
    public static String confirmDeleteServerInUse;
    public static String openEditorOnExternalFile;

    public static String wizInstallTitle;
    public static String wizInstallDescription;
    public static String wizDownloadMessage;
    public static String wizInstallArchiveNotSet;
    public static String wizDownloadConnecting;
    public static String wizDownloadRegister;
    public static String wizInstallFileName;
    public static String wizInstallDestinationLabel;
    public static String wizInstallDestinationNotSet;
    public static String wizInstallArchiveLabel;
    public static String wizInstallArchiveSize;
    public static String wizInstallContentTitle;
    public static String wizInstallContentDescription;
    public static String wizInstallAddonTargetDescription;
    public static String wizConfigRepoLabel;
    public static String wizInstallFolder;
    public static String wizInstallArchive;
    public static String wizInstallSuccess;
    public static String wizInstallFailure;
    public static String wizInstallCancelled;
    public static String wizInstallMissingFeatures;
    public static String jobInstallingRuntime;
    public static String taskAuthenticating;
    public static String taskDownloadLicense;
    public static String taskAcceptingLicense;
    public static String taskPreparingServer;
    public static String taskConnecting;
    public static String taskValidateSecurity;
    public static String taskUpdateSecurity;
    public static String taskDownloading;
    public static String taskUncompressing;
    public static String taskValidatingDrop;
    public static String errorAuthenticationFailed;
    public static String errorSSLSocketFailed;
    public static String errorCannotWriteToInstallFolder;
    public static String errorDownloadingInstallFiles;
    public static String errorInstallingRuntimeEnvironment;
    public static String errorCouldNotConnect;
    public static String errorInvalidFolder;
    public static String errorFolderNotEmpty;
    public static String errorInvalidArchive;
    public static String errorInvalidInputStream;
    public static String errorVerificationFailed;
    public static String publishWithErrors;
    public static String publishWithErrorsDialogTitle;
    public static String publishWithErrorMessage;
    public static String publishWithErrorUserMessage;
    public static String publishWithErrorToggle;
    public static String publishWarningOKButton;
    public static String publishWarningCancelButton;

    public static String wizInstallAddonTitle;
    public static String wizInstallAddonDescription;
    public static String wizInstallConfigSnippetDescription;
    public static String wizInstallAddonInstall;
    public static String wizInstallAddonInstalled;
    public static String wizInstallAddonRemove;
    public static String wizInstallAddonAddArchive;
    public static String wizInstallAddonSummary;
    public static String wizInstallDownloadSummary;
    public static String wizInstallAddonArchive;
    public static String wizInstallAddonArchiveMessage;
    public static String wizDefaultFilterText;
    public static String wizSearchFilterLabel;
    public static String wizSelectedFilterLabel;
    public static String wizConflictFilterLabel;
    public static String wizConflictToolTipMessage;
    public static String errorWizNotAddonJar;
    public static String errorWizAddonJarDup;
    public static String errorInvalidCoreArchive;
    public static String errorWizAddOnNotApplicable;
    public static String licenseAgreementLabel;
    public static String errorRuntimeLocationMissing;
    public static String errorServerMissing;
    public static String errorServerConfigMissing;
    public static String errorInstallProcessFailed;
    public static String wizInstalledToolTipMessage;
    public static String wizInstalledFilterLabel;
    public static String wizRefreshServerFolderJob;
    public static String wizNoApplicableAddonMessage;

    public static String downloaderWASDevName;
    public static String downloaderWASDevDescription;
    public static String unsupportedInstaller;
    public static String unexpectedInstallerError;

    public static String wizPromptMessage;
    public static String wizPromptAlways;
    public static String wizPromptLabel;
    public static String wizPromptIssueColumn;
    public static String wizPromptDescriptionColumn;
    public static String wizPromptActionColumn;
    public static String wizPromptDetailsLabel;

    public static String wizUserDirTitle;
    public static String wizUserDirDescription;
    public static String wizUserDirMessage;
    public static String wizUserDirProject;
    public static String wizUserDirExternal;
    public static String wizUserDirExternalBrowse;
    public static String wizUserDirExternalMessage;
    public static String wizUserDirExternalInvalid;

    public static String wizNewUserProjectTitle;
    public static String wizNewUserProjectDescription;

    public static String configurationLocationTypeServer;
    public static String configurationLocationTypeShared;
    public static String configurationLocationTypeFileSystem;
    public static String configurationLocationTypeURL;

    public static String toolTipEnableFocus;
    public static String toolTipDisableFocus;

    public static String open;
    public static String openConfigFileEditor;
    public static String openBootstrapPropertiesEditor;
    public static String openServerEnvEditor;
    public static String errorAlreadyIncluded;

    public static String taskFixJSP;
    public static String taskColdStart;
    public static String errorFeatureExists;
    public static String errorFeatureUnresolved;

    public static String taskFixJNDI;
    public static String taskFixJDBC;
    public static String errorElementSet;
    public static String errorElementNotSet;

    public static String productInfoFeatures;
    public static String productInfoUnavailable;

    public static String taskAddSharedConfigFile;
    public static String taskAddInclude;

    public static String runtimeClasspathMessage;
    public static String runtimeClasspathExcludeThirdParty;
    public static String runtimeClasspathExcludeStable;
    public static String runtimeClasspathExcludeIBM;
    public static String runtimeClasspathExcludeUnknown;

    public static String propertyPageFeatureRequired;
    public static String propertyPageFeatureRequiredDisabled;
    public static String propertyPageFeatureAlways;
    public static String propertyPageFeaturePrompt;
    public static String propertyPageFeatureNever;
    public static String propertyPageFeatureAppColumn;
    public static String propertyPageFeatureActionColumn;
    public static String propertyPageFeatureAppRequired;
    public static String propertyPageFeatureAppContained;

    public static String filterMessage;
    public static String addButton;
    public static String addButton2;
    public static String newButton;
    public static String editButton;
    public static String setButton;
    public static String removeButton;
    public static String browseButton;
    public static String clearButton;
    public static String upButton;
    public static String downButton;
    public static String contentAssistEmpty;
    public static String doNotShowAgain;

    public static String addButtonAcc;
    public static String newButtonAcc;
    public static String editButtonAcc;
    public static String removeButtonAcc;
    public static String browseButtonAcc;
    public static String browseButtonAcc2;
    public static String browseButtonAcc3;
    public static String browseButtonAcc4;

    public static String relPathButton;
    public static String absPathButton;
    public static String absFilePathButton;
    public static String absFolderPathButton;
    public static String absFolderPathMessage;
    public static String relativePath;
    public static String sharedConfigPath;
    public static String sharedAppsPath;
    public static String selectedPath;
    public static String selectedLocation;
    public static String emptyLocation;
    public static String includeDialogTitle;
    public static String includeDialogLabel;
    public static String includeDialogMessage;
    public static String includeDialogEntryLabel;
    public static String includeConfirmCreate;
    public static String includeCreateFailedTitle;
    public static String includeCreateDirsFailedMsg;
    public static String includeCreateFileFailedMsg;
    public static String appMonitorDialogTitle;
    public static String appMonitorDialogLabel;
    public static String appMonitorDialogMessage;
    public static String appMonitorDialogEntryLabel;
    public static String appLocationDialogTitle;
    public static String appLocationDialogLabel;
    public static String appLocationDialogMessage;
    public static String appLocationDialogEntryLabel;
    public static String logDirDialogTitle;
    public static String logDirDialogLabel;
    public static String logDirDialogMessage;
    public static String logDirDialogEntryLabel;
    public static String filesetDirDialogTitle;
    public static String filesetDirDialogLabel;
    public static String filesetDirDialogMessage;
    public static String filesetDirDialogEntryLabel;
    public static String filesetNoDirSpecified;
    public static String filesetDirNotFound;
    public static String genericFileDialogTitle;
    public static String genericFileDialogLabel;
    public static String genericFileDialogMessage;
    public static String genericFileDialogEntryLabel;
    public static String genericDirDialogTitle;
    public static String genericDirDialogLabel;
    public static String genericDirDialogMessage;
    public static String genericDirDialogEntryLabel;
    public static String genericLocationDialogTitle;
    public static String genericLocationDialogLabel;
    public static String genericLocationDialogMessage;
    public static String genericLocationDialogEntryLabel;
    public static String locationDialogNoRelPathsInfo;
    public static String menuJVMOptionsIn;

    public static String configNone;
    public static String configEmpty;
    public static String errorNoServers;

    public static String configSyncConflicts;
    public static String configSyncOverwriteRemote;
    public static String configSyncCompareMessage;
    public static String configSyncCompare;
    public static String configSyncPublishButton;
    public static String configSyncCancelButton;
    public static String configSyncCompareLeftLabel;
    public static String configSyncCompareRightLabel;

    public static String passwordDialogNoRuntime;
    public static String passwordDialogTitle;
    public static String passwordDialogLabel;
    public static String passwordDialogMessage;

    public static String configEditorHeader;
    public static String configEditorHeaderWithFile;
    public static String configEditorHeaderWithServer;
    public static String sharedConfigEditorHeaderWithFile;

    public static String additionalPropsKeyColumn;
    public static String additionalPropsValueColumn;
    public static String additionalPropsNewTitle;
    public static String additionalPropsNewLabel;
    public static String additionalPropsNewMessage;
    public static String additionalPropsEditTitle;
    public static String additionalPropsEditLabel;
    public static String additionalPropsEditMessage;
    public static String additionalPropsKeyLabel;
    public static String additionalPropsValueLabel;
    public static String additionalPropsKeyError;

    public static String schemaPropsRequiredAttrNoValue;
    public static String schemaPropsWizardTitle;
    public static String schemaPropsSelectPageName;
    public static String schemaPropsSelectPageTitle;
    public static String schemaPropsSelectPageDescription;
    public static String schemaPropsSelectPageTypeLabel;
    public static String schemaPropsEntryPageName;
    public static String schemaPropsEntryPageTitle;
    public static String schemaPropsEntryPageDescription;

    public static String quickFixFailedTitle;

    public static String whitespaceQuickFix;
    public static String whitespaceQuickFixFailed;

    public static String plainTextQuickFix;
    public static String encodingFailedMessage;

    public static String unrecognizedPropertyQuickFix;
    public static String supersededFeatureQuickFix;
    public static String propChangeFailedMessage;

    public static String unrecognizedElementQuickFix;
    public static String elemChangeFailedMessage;

    public static String addFeatureQuickFix;
    public static String addFeatureFailedMessage;

    public static String unrecognizedFeatureQuickFix;
    public static String featureChangeFailedMessage;

    public static String factoryIdNotFoundQuickFix;
    public static String changeFactoryRefFailedMessage;

    public static String duplicateFactoryRefQuickFix;
    public static String removeDupFactoryRefFailedMessage;

    public static String featureConflictQuickFix;
    public static String featureConflictIgnoreInfo;
    public static String featureConflictIgnoreInfoNote;
    public static String featureConflictFailedMessage;
    public static String featureConflictNewFeature;

    public static String addRequiredServerConfigQuickFix;
    public static String addRequiredServerConfigFailedMsg;

    public static String addSecurePortQuickFix;
    public static String addSecurePortFailedMsg;

    public static String updateSecurePortQuickFix;
    public static String updateSecurePortFailedMsg;

    public static String ignoreAllPropFailedMessage;

    public static String ignorePropFailedMessage;

    public static String chainableConfigContext;
    public static String chainableConfigId;
    public static String jdbcDriverCreateTitle;
    public static String jdbcDriverCreateLabel;
    public static String jdbcDriverCreateMessage;
    public static String jdbcDriverLibRef;
    public static String libraryCreateTitle;
    public static String libraryCreateLabel;
    public static String libraryCreateMessage;
    public static String libraryCreateFilesetRef;
    public static String filesetCreateTitle;
    public static String filesetCreateLabel;
    public static String filesetCreateMessage;
    public static String filesetCreateBaseDir;
    public static String filesetCreateIncludes;
    public static String filesetIncludesTitle;
    public static String filesetIncludesLabel;
    public static String filesetIncludesMessage;
    public static String filesetCreateExcludes;
    public static String filesetExcludesTitle;
    public static String filesetExcludesLabel;
    public static String filesetExcludesMessage;
    public static String fileSelectorFilter;

    public static String configSchemaBrowserToolTip;
    public static String configSchemaBrowserName;

    public static String wizNewConfigFileTitle;
    public static String wizNewConfigFilePageTitle;
    public static String wizNewConfigFilePageDesc;

    public static String contentAssistVariableLabel;
    public static String contentAssistTypeLabel;
    public static String contentAssistValueLabel;
    public static String contentAssistIdLabel;
    public static String contentAssistUnresolved;
    public static String contentAssistImplicitLocalLabel;
    public static String contentAssistDescriptionLabel;

    public static String variableDialogTitle;
    public static String nameDialogEnter;
    public static String valueDialogEnter;

    public static String supersedeFeatureDialogTitle;
    public static String supersedeFeatureDialogMessage;
    public static String supersedeFeatureDialogMessage2;
    public static String supersedeFeatureDialogColumn;
    public static String supersedeFeatureDialogRecommended;
    public static String supersedeFeatureDialogOptional;
    public static String supersedeFeatureDialogExisting;
    public static String supersedeFeatureDialogErrorExisting;
    public static String supersedeFeatureDialogErrorRecommended;

    public static String createVariableQuickFix;
    public static String createVariableFailedMessage;

    public static String replaceReferenceQuickFix;
    public static String replaceReferenceFailedMessage;

    public static String featureNameColumn;
    public static String featureDisplayNameColumn;
    public static String featureDisplayName;
    public static String featureEnables;
    public static String featureEnabledBy;
    public static String featureConflictsWith;
    public static String featureDescription;
    public static String featureRichFormatDescription;
    public static String featureRichFormatEmptySelection;
    public static String featureNoDescriptionAvailable;
    public static String featureNone;
    public static String featureShowImplicit;
    public static String featureAllEnabledMsg;
    public static String addFeatureTitle;
    public static String addFeatureLabel;
    public static String addFeatureMessage;

    public static String referenceNestedCount;
    public static String referenceNestedButton;
    public static String referenceTopLevelButton;

    public static String referenceSelectionTitle;
    public static String referenceSelectionMultiLabel;
    public static String referenceSelectionMultiMessage;
    public static String referenceSelectionSingleLabel;
    public static String referenceSelectionSingleMessage;
    public static String referenceSelectionAvailableIds;
    public static String referenceSelectionAvailableVars;

    public static String idDialogTitle;
    public static String idDialogLabel;
    public static String idDialogMessage;
    public static String idLabel;

    public static String variableValue;

    public static String configItemPathLabel;
    public static String ignoreAllAttrAllElemLabel;
    public static String ignoreAllAttrElemLabel;
    public static String ignoreAttrElemLabel;
    public static String ignoreLabelGeneric;

    public static String removeFilterFailedMessage;
    public static String validationFilterLabel;

    public static String removeAppFromServerLabel;
    public static String removeAppFromServerFailedMessage;

    public static String addConfigAppElementLabel;
    public static String addConfigAppElementFailedMessage;
    public static String updateSharedLibRefLabel;

    public static String editorBrowse;

    public static String errorRuntimeLocationMapped;
    public static String errorRuntimeServersRunning;
    public static String errorRTNameSameExistingPrjName;
    public static String errorUnknownDrop;
    public static String errorNotSupportedDrop;
    public static String errorRuntimeRequired;
    public static String errorAddonInvalid;
    public static String errorAddonNoProductId;
    public static String errorAddonProdcutIdMismatch;
    public static String errorAddonInstallTypeMismatch;
    public static String errorAddonVersionMismatch;
    public static String errorAddonEditionMismatch;
    public static String errorAddonAlreadyInstalled;
    public static String errorAddonNotSupported;
    public static String errorAddonIsRepo;
    public static String errorRuntimeNoProductId;
    public static String errorRuntimeNoProductInfo;

    public static String mergedEditorTitle;
    public static String mergedEditorFormText;
    public static String mergedEditorFormTextWithFile;
    public static String mergedEditorFormTextWithServer;
    public static String mergedEditorOverviewText;
    public static String mergedEditorError;

    public static String addElementTitle;
    public static String addElementLabel;
    public static String addElementMessage;
    public static String addElementContextLabel;
    public static String addElementDetailsLabel;
    public static String addElementElementNameLabel;
    public static String addElementEnablingFeaturesLabel;
    public static String addElementHistoryLabel;
    public static String addElementFeatureEnabledElementsLabel;
    public static String addElementAllElementsLabel;
    public static String addElementRecentlyAddedElementsLabel;

    public static String enableElementTitle;
    public static String enableElementMessage;
    public static String enableElementLabel;

    public static String taskRefreshingMetadata;
    public static String taskStoppingServer;
    public static String taskDeletingServers;
    public static String taskTimeoutError;
    public static String errorWebsphereRuntimeAdapter;

    public static String configTreeLabel;
    public static String propertyTreeLabel;

    public static String changeRuntimeLocationTitle;
    public static String changeRuntimeLocationMessage;
    public static String deleteServersLabel;

    public static String loginPasswordDialogTitle;
    public static String loginPasswordDialogMessage;
    public static String configureRepoDialogTitle;
    public static String repoPrefencePageDescriptionLabel1;
    public static String repoPrefencePageDescriptionLabel2;
    public static String repoPrefencePageDescriptionLabel3;

    public static String repositoryInfoDialogEditTitle;
    public static String repositoryInfoDialogEditDescription;
    public static String repositoryInfoDialogRemoteGroupLabel;
    public static String repositoryInfoDialogLocalGroupLabel;
    public static String repositoryInfoDialogLocationLabel;
    public static String repositoryInfoDialogNewTitle;
    public static String repositoryInfoDialogNewDescription;
    public static String repositoryInfoDialogDirectoryRepoButton;
    public static String repositoryInfoDialogZipRepoButton;
    public static String errorRepositoryEmptyName;
    public static String errorRepositoryInvalidURL;
    public static String errorRepositoryInvalidLocation;
    public static String errorRepositoryInvalidArchive;
    public static String errorRepositoryAlreadyExists;
    public static String errorRepositoryUserNotSet;
    public static String errorRepositoryPasswordNotSet;

    public static String emptyElementText;

    public static String featureConflictLabel;
    public static String featureConflictTitle;
    public static String featureConflictDescription;
    public static String featureConflictAddedAppsMessage;
    public static String featureConflictDetails;
    public static String featureConflictDetailsWithDependency;
    public static String featureConflictDetailsWithModules;
    public static String featureConflictDetailsWithDependencyAndModules;
    public static String featureConflictIncludeMessage;
    public static String featureConflictAllResolved;
    public static String featureConflictDependencyChainMessage;

    public static String menuNewConfigDropin;
    public static String newConfigDropinDefaults;
    public static String newConfigDropinOverrides;
    public static String newConfigDropinWizardTitle;
    public static String newConfigDropinWizardDefaultsTitle;
    public static String newConfigDropinWizardOverridesTitle;
    public static String newConfigDropinWizardDefaultsDesc;
    public static String newConfigDropinWizardOverridesDesc;
    public static String newConfigDropinWizardFileNameLabel;
    public static String newConfigDropinWizardXMLExtError;
    public static String newConfigDropinWizardExistsError;

    public static String addRequiredElementsLabel;
    public static String addKeystoreElementTitle;
    public static String addKeystoreElementDescription;
    public static String addSecurityElementsTitle;
    public static String addSecurityElementsDescription;
    public static String addKeystoreElementPasswordLabel;
    public static String addUserRegistryButton;
    public static String addUserRegistryName;
    public static String addUserRegistryPassword;
    public static String keyStorePasswordSetButton;
    public static String basicUserRegistrySetButton;

    public static String X509_DIALOG_TITLE;
    public static String X509_DIALOG_MESSAGE;
    public static String X509_DIALOG_BUTTON_VALID_FOR_SESSION;
    public static String X509_DIALOG_BUTTON_VALID_FOR_WORKSPACE;
    public static String X509_DIALOG_BUTTON_REJECTED;

    public static String X509_CONTROL_ISSUER;
    public static String X509_CONTROL_LABEL_CN;
    public static String X509_CONTROL_LABEL_OU;
    public static String X509_CONTROL_LABEL_O;
    public static String X509_CONTROL_COLUMN_TITLE_CERTIFICATE;
    public static String X509_CONTROL_COLUMN_TITLE_ATTRIBUTE;
    public static String X509_CONTROL_COLUMN_TITLE_VALUE;
    public static String X509_CONTROL_CERTIFICATE_DETAILS_LABEL;
    public static String X509_CONTROL_CERTIFICATE_CAUSE_LABEL;

    public static String enableCustomEncryptionTitle;
    public static String enableCustomEncryptionMessage;
    public static String enableCustomEncryptionLabel;

    public static String enableAutomaticFeatureDetection;
    public static String requiredFeatureDefaultAction;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}