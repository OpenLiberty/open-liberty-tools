/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.osgi.framework.BundleContext;

import com.ibm.ws.st.core.internal.FeatureConflictHandler;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.LaunchUtil;
import com.ibm.ws.st.core.internal.MessageHandler;
import com.ibm.ws.st.core.internal.MissingKeystoreHandler;
import com.ibm.ws.st.core.internal.PromptHandler;
import com.ibm.ws.st.core.internal.PublishWithErrorHandler;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DocumentLocation;
import com.ibm.ws.st.core.internal.config.URILocation;
import com.ibm.ws.st.ui.internal.config.AddSecurityElementsDialog;
import com.ibm.ws.st.ui.internal.config.FeatureConflictDialog;
import com.ibm.ws.st.ui.internal.config.FileConfigEditorInput;
import com.ibm.ws.st.ui.internal.config.FileStoreConfigEditorInput;
import com.ibm.ws.st.ui.internal.merge.MergedConfigResourceListener;
import com.ibm.ws.st.ui.internal.merge.MergedEditorFileStorage;
import com.ibm.xwt.dde.editor.DDEMultiPageEditorPart;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "com.ibm.ws.st.ui";

    public static final String CONFIGURATION_EDITOR = "com.ibm.ws.st.ui.configuration.editor";
    private static final String CONFIGURATION_DOC_URL = "http://www14.software.ibm.com/webapp/wsbroker/redirect?version=cord&product=was-nd-dist&topic=cwlp_config";
    private static final String SCHEMA_BROWSER_ID = "wasSchema";
    private static final String DELIM = ",";
    private static final String TEXT_EDITOR = "org.eclipse.ui.DefaultTextEditor";
    private static final String EXPLORER_VIEW_ID = "org.eclipse.ui.navigator.ProjectExplorer";

    // the shared instance
    private static Activator instance;

    protected Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();

    // base url for icons
    private static URL ICON_BASE_URL;

    private static final String URL_OBJ = "obj16/";
    private static final String URL_WIZBAN = "wizban/";

    public static final String IMG_CONFIG_FILE = "configFile";
    public static final String IMG_CONFIG_ELEMENT = "configElement";
    public static final String IMG_SERVER_CONFIG = "serverConfig";
    public static final String IMG_RUNTIME = "runtime";
    public static final String IMG_SERVER = "server";
    public static final String IMG_SERVER_FOLDER = "serverFolder";
    public static final String IMG_CONFIG_FOLDER = "configFolder";
    public static final String IMG_APP_FOLDER = "appFolder";
    public static final String IMG_WIZ_RUNTIME = "wizRuntime";
    public static final String IMG_WIZ_SERVER = "wizServer";
    public static final String IMG_REFRESH = "refresh";
    public static final String IMG_RELATIVE_PATH = "relativePath";
    public static final String IMG_ABSOLUTE_PATH = "absolutePath";
    public static final String IMG_USER_PROJECT = "userProject";
    public static final String IMG_USER_FOLDER = "userFolder";
    public static final String IMG_MENU_DOWN = "menuDown";
    public static final String IMG_BOOTSTRAP_PROPS = "bootstrap.properties";
    public static final String IMG_JVM_OPTIONS = "jvm.options";
    public static final String IMG_SERVER_ENV = "server.env";
    public static final String IMG_VARIABLE_REF = "variableRef";
    public static final String IMG_FEATURE_ELEMENT = "featureElement";
    public static final String IMG_FEATURE_SUPERSEDED = "featureSuperseded";
    public static final String IMG_FACTORY_REF = "factoryRef";
    public static final String IMG_ERROR_OVERLAY = "errorOverlay";
    public static final String IMG_IGNORE_FILTER = "ignoreFilter";
    public static final String IMG_WIZ_ADD_ON = "addOnWizard";
    public static final String IMG_ADD_ON = "addOn";
    public static final String IMG_ADD_ON_SAMPLE = "addOnSample";
    public static final String IMG_ADD_ON_FEATURE = "addOnFeature";
    public static final String IMG_ADD_ON_CONFIG_SNIPPET = "addOnConfigSnippet";
    public static final String IMG_ADD_ON_OPEN_SOURCE = "addOnOpenSource";
    public static final String IMG_ENUMERATOR = "enumerator";
    public static final String IMG_ENUMERATOR_DEFAULT = "enumeratorDefault";
    public static final String IMG_INFORMATION = "information";
    public static final String PUBLISH_WITH_ERROR = "publishWithError";

    public static final String IMG_LICENSE = "license";

    // Icons for the config editor tree view
    public static final String IMG_CONFIG_ELEM = "configElem";
    public static final String IMG_SECURITY_ELEM = "securityElem";
    public static final String IMG_INCLUDE_ELEM = "includeElem";
    public static final String IMG_FEATURES_ELEM = "featuresElem";
    public static final String IMG_APP_MONITOR_ELEM = "appMonitorElem";
    public static final String IMG_APP_MANAGER_ELEM = "appManagerElem";
    public static final String IMG_DATASOURCE_ELEM = "datasourceElem";
    public static final String IMG_LOGGING_ELEM = "loggingElem";
    public static final String IMG_VARIABLE_ELEM = "variableElem";
    public static final String IMG_SSL_ELEM = "sslElem";
    public static final String IMG_HTTP_ELEM = "httpElem";

    public Activator() {
        // do nothing
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        Trace.ENABLED = isDebugging();

        // register and initialize the tracing class
        final Hashtable<String, String> props = new Hashtable<String, String>(4);
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, Activator.PLUGIN_ID);
        context.registerService(DebugOptionsListener.class.getName(), Trace.TS, props);

        ErrorPageListener epl = ErrorPageListener.getInstance();
        LaunchUtil.setErrorPage(epl.getPort() + "/" + epl.hashCode());

        addRequiredFeatureListener();
        MergedConfigResourceListener.start();

        PlatformUI.getWorkbench().getProgressService().registerIconForFamily(getImageDescriptor(IMG_RUNTIME), com.ibm.ws.st.core.internal.Constants.JOB_FAMILY);
    }

    private static void addRequiredFeatureListener() {
        com.ibm.ws.st.core.internal.Activator.setPromptHandler(new PromptHandler() {
            @Override
            public IPromptResponse getResponse(final String message, final PromptHandler.AbstractPrompt[] prompts, final int style) {
                final PromptDialog[] dialog = new PromptDialog[1];
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        Shell shell = Display.getDefault().getActiveShell();
                        dialog[0] = new PromptDialog(shell, message, prompts, style);
                        dialog[0].open();
                    }
                });
                if (dialog[0].getReturnCode() != 0)
                    return null;
                return dialog[0].getResponses();
            }

            @Override
            public boolean handleConfirmPrompt(final String title, final String message, final boolean defaultVal) {
                final boolean[] response = { defaultVal };
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        Shell shell = Display.getDefault().getActiveShell();
                        response[0] = MessageDialog.openConfirm(shell, title, message);
                    }
                });
                return response[0];
            }
        });

        com.ibm.ws.st.core.internal.Activator.setFeatureConflictHandler(new FeatureConflictHandler() {
            /** {@inheritDoc} */
            @Override
            public boolean handleFeatureConflicts(final WebSphereServerInfo wsServerInfo, final Map<String, List<String>> requiredFeatures, final Set<FeatureConflict> conflicts,
                                                  final boolean quickFixMode) {
                return invokeDialog(wsServerInfo, null, null, requiredFeatures, conflicts, quickFixMode);
            }

            @Override
            public boolean handleFeatureConflicts(WebSphereRuntime wsRuntime, ConfigurationFile file, Map<String, List<String>> requiredFeatures, Set<FeatureConflict> conflicts,
                                                  boolean quickFixMode) {
                return invokeDialog(null, wsRuntime, file, requiredFeatures, conflicts, quickFixMode);
            }

            // Two situations: (See above two methods)
            //     1. wsServerInfo must be non-null and wsRuntime and configFile are null
            // OR  2. wsServerInfo is null and wsRuntime and configFile are non-null

            private boolean invokeDialog(final WebSphereServerInfo wsServerInfo, final WebSphereRuntime wsRuntime, final ConfigurationFile configFile,
                                         final Map<String, List<String>> requiredFeatures, final Set<FeatureConflict> conflicts,
                                         final boolean quickFixMode) {
                final FeatureConflictDialog[] dialog = new FeatureConflictDialog[1];
                final int[] response = { -1 };
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        Shell shell = Display.getDefault().getActiveShell();
                        if (wsRuntime != null && configFile != null) {
                            dialog[0] = new FeatureConflictDialog(shell, wsRuntime, configFile, requiredFeatures, conflicts);
                        } else {
                            dialog[0] = new FeatureConflictDialog(shell, wsServerInfo, requiredFeatures, conflicts);
                        }
                        dialog[0].setShowIgnoreButton(!quickFixMode);
                        response[0] = dialog[0].open();
                    }
                });
                if (wsServerInfo != null) {
                    WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(wsServerInfo);
                    if (wsServer != null) {
                        if (response[0] == IDialogConstants.IGNORE_ID) {
                            wsServer.saveIgnoredFeatureConflicts(dialog[0].getConflicts());
                        } else if (!quickFixMode || (quickFixMode && dialog[0].getRemainingConflictsSize() == 0)) {
                            wsServer.saveIgnoredFeatureConflicts(null);
                        }
                    }
                }
                if (dialog[0].getReturnCode() != 0)
                    return false;
                return dialog[0].isChanged();
            }
        });

        com.ibm.ws.st.core.internal.Activator.setMissingKeystoreHandler(new MissingKeystoreHandler() {
            /** {@inheritDoc} */
            @Override
            public boolean handleMissingKeystore(final WebSphereServerInfo wsServerInfo, final boolean appSecurityEnabled) {
                final AddSecurityElementsDialog[] dialog = new AddSecurityElementsDialog[1];
                final int[] response = { -1 };
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        Shell shell = Display.getDefault().getActiveShell();
                        dialog[0] = new AddSecurityElementsDialog(shell, wsServerInfo, appSecurityEnabled);
                        response[0] = dialog[0].open();
                    }
                });

                return dialog[0].getReturnCode() == 0;
            }
        });

        com.ibm.ws.st.core.internal.Activator.setPublishWithErrorHandler(new PublishWithErrorHandler() {

            @Override
            public boolean handlePublishWithError(final ArrayList<String> projectName, final WebSphereServer server) {
                final PublishWithErrorDialog[] dialog = new PublishWithErrorDialog[1];

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        Shell shell = Display.getDefault().getActiveShell();
                        dialog[0] = new PublishWithErrorDialog(shell, projectName);
                        dialog[0].open();
                        //set return code since custom button are used
                        setReturnCode(dialog[0].getReturnCode());
                        // don't set toggle if user selects cancel
                        if (getReturnCode() == 0) {
                            IServerWorkingCopy wc = server.getServer().createWorkingCopy();
                            wc.setAttribute(PUBLISH_WITH_ERROR, dialog[0].getToggleState());
                            try {
                                wc.save(false, null);
                            } catch (CoreException e) {
                                Trace.logError("Unable to save changes to Server", e);
                            }
                        }
                    }
                });
                return getReturnCode() == 0;
            }
        });

        com.ibm.ws.st.core.internal.Activator.setMessageHandler(new MessageHandler() {

            /** {@inheritDoc} */
            @Override
            public boolean handleMessage(final MessageType type, final String title, final String message) {
                final Boolean[] answer = new Boolean[1];
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = false;
                        Shell shell = Display.getDefault().getActiveShell();
                        switch (type) {
                            case INFORMATION:
                                result = MessageDialog.open(MessageDialog.INFORMATION, shell, title, message, SWT.NONE);
                                break;
                            case WARNING:
                                result = MessageDialog.open(MessageDialog.WARNING, shell, title, message, SWT.NONE);
                                break;
                            case ERROR:
                                result = MessageDialog.open(MessageDialog.ERROR, shell, title, message, SWT.NONE);
                                break;
                        }
                        answer[0] = result ? Boolean.TRUE : Boolean.FALSE;
                    }
                });
                return answer[0].booleanValue();
            }
        });

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        MergedConfigResourceListener.stop();
        com.ibm.ws.st.core.internal.Activator.setPromptHandler(null);
        instance = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getInstance() {
        return instance;
    }

    /**
     * Register an image with the registry.
     *
     * @param key java.lang.String
     * @param partialURL java.lang.String
     */
    private void registerImage(ImageRegistry registry, String key, String partialURL) {
        try {
            ImageDescriptor id = ImageDescriptor.createFromURL(new URL(ICON_BASE_URL, partialURL));
            registry.put(key, id);
            imageDescriptors.put(key, id);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Error registering image", e);
        }
    }

    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry registry = new ImageRegistry();
        if (ICON_BASE_URL == null)
            ICON_BASE_URL = instance.getBundle().getEntry("icons/");

        registerImage(registry, IMG_CONFIG_FILE, URL_OBJ + "configurationFile.gif");
        registerImage(registry, IMG_CONFIG_ELEMENT, URL_OBJ + "configElement.gif");
        registerImage(registry, IMG_SERVER_CONFIG, URL_OBJ + "serverConfiguration.gif");
        registerImage(registry, IMG_RUNTIME, URL_OBJ + "liberty-was-lp-16.png");
        registerImage(registry, IMG_SERVER, URL_OBJ + "liberty-was-lp-16.png");
        registerImage(registry, IMG_SERVER_FOLDER, URL_OBJ + "serversFolder.gif");
        registerImage(registry, IMG_CONFIG_FOLDER, URL_OBJ + "sharedConfigFolder.gif");
        registerImage(registry, IMG_APP_FOLDER, URL_OBJ + "sharedAppsFolder.gif");
        registerImage(registry, IMG_WIZ_RUNTIME, URL_WIZBAN + "liberty-banner.png");
        registerImage(registry, IMG_WIZ_SERVER, URL_WIZBAN + "liberty-server-banner.png");
        registerImage(registry, IMG_REFRESH, URL_OBJ + "refresh.gif");
        registerImage(registry, IMG_RELATIVE_PATH, URL_OBJ + "relativePath.gif");
        registerImage(registry, IMG_ABSOLUTE_PATH, URL_OBJ + "absolutePath.gif");
        registerImage(registry, IMG_USER_PROJECT, URL_OBJ + "userProject.gif");
        // use the same image as the server folder for now
        registerImage(registry, IMG_USER_FOLDER, URL_OBJ + "serversFolder.gif");
        registerImage(registry, IMG_MENU_DOWN, URL_OBJ + "menuDown.gif");
        registerImage(registry, IMG_BOOTSTRAP_PROPS, URL_OBJ + "bootstrapPropertiesFile.gif");
        registerImage(registry, IMG_JVM_OPTIONS, URL_OBJ + "bootstrapPropertiesFile.gif");
        registerImage(registry, IMG_SERVER_ENV, URL_OBJ + "bootstrapPropertiesFile.gif");
        registerImage(registry, IMG_VARIABLE_REF, URL_OBJ + "variableRef.gif");
        registerImage(registry, IMG_FEATURE_ELEMENT, URL_OBJ + "featureElement.gif");
        registerImage(registry, IMG_FACTORY_REF, URL_OBJ + "factoryRef.gif");
        registerImage(registry, IMG_ERROR_OVERLAY, URL_OBJ + "error.gif");
        registerImage(registry, IMG_IGNORE_FILTER, URL_OBJ + "bootstrapProperties.gif");
        registerImage(registry, IMG_WIZ_ADD_ON, URL_WIZBAN + "liberty-addon-banner.png");
        registerImage(registry, IMG_ADD_ON, URL_OBJ + "addon-was-lpn-48.png");
        registerImage(registry, IMG_ADD_ON_SAMPLE, URL_OBJ + "addOnSample.png");
        registerImage(registry, IMG_ADD_ON_FEATURE, URL_OBJ + "addOnFeature.png");
        registerImage(registry, IMG_ADD_ON_CONFIG_SNIPPET, URL_OBJ + "addOnConfigSnippet.png");
        registerImage(registry, IMG_ADD_ON_OPEN_SOURCE, URL_OBJ + "addOnOpenSource.png");
        registerImage(registry, IMG_ENUMERATOR, URL_OBJ + "enum.gif");
        registerImage(registry, IMG_ENUMERATOR_DEFAULT, URL_OBJ + "enumDefault.gif");
        registerImage(registry, IMG_INFORMATION, URL_OBJ + "information.gif");

        registerImage(registry, IMG_LICENSE, URL_OBJ + "liberty-was-lp-16.png");

        registerImage(registry, IMG_CONFIG_ELEM, URL_OBJ + "configElement.gif");
        registerImage(registry, IMG_SECURITY_ELEM, URL_OBJ + "securityElement.gif");
        registerImage(registry, IMG_INCLUDE_ELEM, URL_OBJ + "includeElement.gif");
        registerImage(registry, IMG_FEATURES_ELEM, URL_OBJ + "featuresElement.gif");
        registerImage(registry, IMG_APP_MONITOR_ELEM, URL_OBJ + "appMonitorElement.gif");
        registerImage(registry, IMG_APP_MANAGER_ELEM, URL_OBJ + "applicationElement.gif");
        registerImage(registry, IMG_DATASOURCE_ELEM, URL_OBJ + "datasourceElement.gif");
        registerImage(registry, IMG_LOGGING_ELEM, URL_OBJ + "trace.gif");
        registerImage(registry, IMG_VARIABLE_ELEM, URL_OBJ + "variable.gif");
        registerImage(registry, IMG_SSL_ELEM, URL_OBJ + "sslElement.gif");
        registerImage(registry, IMG_HTTP_ELEM, URL_OBJ + "httpElement.gif");

        ImageDescriptor warn = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_WARNING);
        DecorationOverlayIcon decoratedImage = new DecorationOverlayIcon(registry.get(IMG_FEATURE_ELEMENT), warn, IDecoration.BOTTOM_LEFT);
        registry.put(IMG_FEATURE_SUPERSEDED, decoratedImage);

        return registry;
    }

    /**
     * Return the image with the given key from the image registry.
     *
     * @param key
     * @return Image
     */
    public static Image getImage(String key) {
        return getInstance().getImageRegistry().get(key);
    }

    /**
     * Return the image with the given key from the image registry.
     *
     * @param key
     * @return ImageDescriptor
     */
    public static ImageDescriptor getImageDescriptor(String key) {
        try {
            getInstance().getImageRegistry();
            return getInstance().imageDescriptors.get(key);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Missing image", e);
            return ImageDescriptor.getMissingImageDescriptor();
        }
    }

    public static String getBundleVersion() {
        return getInstance().getBundle().getVersion().toString();
    }

    public static String getPreference(String key, String defaultValue) {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(key, defaultValue);
    }

    public static boolean getPreferenceBoolean(String key, boolean defaultValue) {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID).getBoolean(key, defaultValue);
    }

    public static void setPreferenceBoolean(String key, boolean value) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
            prefs.putBoolean(key, value);
            prefs.flush();
        } catch (Exception e) {
            Trace.logError("Error setting preference: " + key, e);
        }
    }

    public static void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
        prefs.addPreferenceChangeListener(listener);
    }

    public static void removePreferenceChangeListener(IPreferenceChangeListener listener) {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
        prefs.removePreferenceChangeListener(listener);
    }

    public static List<String> getPreferenceList(String key) {
        String value = Activator.getPreference(key, null);
        if (value == null)
            return new ArrayList<String>();

        List<String> list = new ArrayList<String>(5);

        StringTokenizer st = new StringTokenizer(value, DELIM);
        while (st.hasMoreTokens())
            list.add(st.nextToken());

        return list;
    }

    public static void setPreference(String key, String value) {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
            if (value == null)
                prefs.remove(key);
            else
                prefs.put(key, value);
            prefs.flush();
        } catch (Exception e) {
            Trace.logError("Error setting preference: " + key, e);
        }
    }

    public static void addToPreferenceList(String key, String value) {

        if (key == null || key.length() == 0 || value == null || value.length() == 0) {
            return;
        }

        String previousLocations = Activator.getPreference(key, null);
        if (previousLocations == null) {
            Activator.setPreference(key, value);
            return;
        }

        List<String> list = new ArrayList<String>(4);
        list.add(value);

        StringTokenizer st = new StringTokenizer(previousLocations, DELIM);

        int count = 0;
        while (st.hasMoreTokens() && count < 4) {
            String s = st.nextToken();
            if (!list.contains(s))
                list.add(s);

            count++;
        }

        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0)
                sb.append(DELIM);
            sb.append(s);
        }
        Activator.setPreference(key, sb.toString());
    }

    /**
     * Open the configuration editor at a particular node (expressed by the xpath) or the root node if
     * xpath is null. If file is not null and is accessible then an internal editor is launched.
     * Otherwise, the uri is used to launch an external editor.
     *
     * The external editor will be the form-based editor if it can be found, otherwise we'll
     * use the regular xml editor.
     *
     * @param file
     * @param uri
     * @param xpath
     */
    public static IEditorPart openConfigurationEditor(IFile file, final URI uri, String xpath) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart editorPart = null;

        try {
            IEditorInput editorInput = null;
            if (file != null && file.isAccessible())
                editorInput = new FileConfigEditorInput(file, file.getLocation().toFile().toURI(), xpath);
            else if (file != null && !externalEditorConfirmation())
                return null;
            else
                editorInput = new FileStoreConfigEditorInput(EFS.getStore(uri), xpath);

            // check for existing editor before opening a new one
            IEditorReference[] editors = page.getEditorReferences();
            if (editors != null) {
                for (IEditorReference ref : editors) {
                    if (ref.getEditorInput().equals(editorInput)) {
                        editorPart = ref.getEditor(true);
                        if (xpath != null && editorPart instanceof DDEMultiPageEditorPart)
                            ((DDEMultiPageEditorPart) editorPart).setSelection(xpath, null);
                        page.activate(editorPart);
                        return editorPart;
                    }
                }
            }

            editorPart = page.openEditor(editorInput, CONFIGURATION_EDITOR, true);

            if (file == null || !file.isAccessible()) {
                final File f = new File(uri);
                final long[] lastModified = new long[] { f.lastModified() };

                editorPart.addPropertyListener(new IPropertyListener() {
                    @Override
                    public void propertyChanged(Object arg0, int arg1) {
                        long newLastModified = f.lastModified();
                        if (newLastModified != lastModified[0]) {
                            lastModified[0] = newLastModified;
                            WebSphereServerInfo[] servers = WebSphereUtil.getWebSphereServerInfos();
                            for (WebSphereServerInfo server : servers) {
                                ConfigurationFile configFile = server.getConfigurationFileFromURI(uri);
                                if (configFile != null)
                                    server.updateCache();
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            Trace.logError("Error opening editor for " + ((file != null && file.isAccessible()) ? file.getLocationURI() : uri), e);
        }
        return editorPart;
    }

    /**
     * Open an editor for the given file. If file is not null and is accessible then an
     * internal editor is launched. Otherwise, the uri is used to launch an external editor.
     *
     * @param file
     * @param uri
     */
    public static IEditorPart openEditor(IFile file, final URI uri) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart editorPart = null;

        try {
            IEditorInput editorInput = null;
            if (file != null && file.isAccessible())
                editorInput = new FileEditorInput(file);
            else if (file != null && !externalEditorConfirmation())
                return null;
            else
                editorInput = new FileStoreEditorInput(EFS.getStore(uri));

            // check for existing editor before opening a new one
            IEditorReference[] editors = page.getEditorReferences();
            if (editors != null) {
                for (IEditorReference ref : editors) {
                    if (ref.getEditorInput().equals(editorInput)) {
                        editorPart = ref.getEditor(true);
                        page.activate(editorPart);
                        return editorPart;
                    }
                }
            }

            editorPart = page.openEditor(editorInput, TEXT_EDITOR, true);
        } catch (Exception e) {
            Trace.logError("Error opening editor for " + ((file != null && file.isAccessible()) ? file.getLocationURI() : uri), e);
        }
        return editorPart;
    }

    /**
     * Open the configuration editor at the root node.
     *
     * @param file
     * @param uri
     */
    public static IEditorPart openConfigurationEditor(IFile file, final URI uri) {
        return openConfigurationEditor(file, uri, null);
    }

    /**
     * Open the correct editor for the given DocumentLocation.
     */
    public static IEditorPart openEditor(DocumentLocation location) {
        if (location.isConfigFile()) {
            return Activator.openConfigurationEditor(location.getFile(), location.getURI(), location.getXPath());
        }
        return Activator.openEditor(location.getFile(), location.getURI());
    }

    /**
     * Go to the location in the given editor. If this is a config file,
     * this will open the source view instead of the design view. Intended
     * for when a hyperlink is activated from the source view or when the
     * file is not a config file.
     */
    public static void goToLocation(IEditorPart editorPart, DocumentLocation location) {
        if (editorPart == null || location == null)
            return;
        IFile file = location.getFile();
        if (file != null && (location.getLine() != -1 || location.getStartOffset() != -1)) {
            IMarker marker = null;
            try {
                marker = file.createMarker(IMarker.MARKER);
                if (location.getLine() != -1)
                    marker.setAttribute(IMarker.LINE_NUMBER, location.getLine());
                if (location.getStartOffset() != -1) {
                    marker.setAttribute(IMarker.CHAR_START, location.getStartOffset());
                    if (location.getEndOffset() != -1)
                        marker.setAttribute(IMarker.CHAR_END, location.getEndOffset());
                }
                IDE.gotoMarker(editorPart, marker);
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Failed to create marker for: " + file.getFullPath().toString(), e);
                }
            } finally {
                if (marker != null) {
                    try {
                        marker.delete();
                    } catch (CoreException e) {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Failed to delete marker.", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * The behaviour of this method depends on what the URILocation refers to:
     * <ul>
     * <li>Configuration File - opens the configuration editor. The location of a variable
     * declaration or a factory definition are examples.</li>
     * <li>Resource - shows the resource in the package explorer view. An automatic
     * shared library directory is an example.</li>
     * <li>File (on the file system) - opens a system browser for the file or directory.
     * An automatic shared library directory that is not in the workspace is an example.</li>
     * </ul>
     *
     * @param location The location to open.
     */
    public static void open(URILocation location) {
        if (location instanceof DocumentLocation) {
            openEditor((DocumentLocation) location);
        } else {
            IResource resource = location.getResource();
            if (resource != null) {
                showResource(resource);
            } else {
                showExternalFile(location.getURI());
            }
        }
    }

    /**
     * Show the resource in the project explorer view.
     *
     * @param resource The resource to show.
     */
    public static void showResource(IResource resource) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(EXPLORER_VIEW_ID);
                if (part == null) {
                    try {
                        part = page.showView(EXPLORER_VIEW_ID);
                    } catch (PartInitException e) {
                        Trace.logError("Could not open the project explorer view", e);
                    }
                }
                if (part != null) {
                    page.activate(part);
                    CommonNavigator view = part.getAdapter(CommonNavigator.class);
                    if (view != null) {
                        view.setFocus();
                        view.selectReveal(new StructuredSelection(resource));
                    }
                }
            }
        }
    }

    /**
     * Show the file in a file system browser.
     *
     * @param uri The file uri.
     */
    public static void showExternalFile(URI uri) {
        File file = new File(uri);
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            Trace.logError("Error opening file " + file.toString(), e);
        }
    }

    /**
     * Open the merged configuration editor against the given config root.
     *
     * @param uri
     */
    public static void openMergedConfiguration(URI uri) {
        ConfigurationFile config = getConfigFile(uri);
        if (config == null) {
            MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.errorDialogTitleOpenMergedView,
                                    Messages.errorDialogMessageOpenMergedView);
            Trace.logError("Error opening merged editor - no configuration file for: " + uri, null);
            return;
        }
        IFile fileResource = config.getIFile();
        if (fileResource == null) {
            Trace.logError("Error opening merged editor - no IFile for: " + uri, null);
        }
        try {
            IPath filePath = ConfigUtils.getMergedConfigLocation(fileResource);
            if (filePath == null) {
                Trace.logError("Error opening merged editor - could not determine location for: " + uri, null);
                return;
            }
            File file = filePath.toFile();
            if (file.exists()) {
                if (!file.delete()) {
                    Trace.logError("Failed to delete existing merged configuration file: " + file.getPath(), null);
                    return;
                }
            } else {
                IPath dirPath = filePath.removeLastSegments(1);
                File dir = dirPath.toFile();
                if (!dir.exists() && !dir.mkdirs()) {
                    Trace.logError("Failed to create directory " + file.getPath() + " for writing merged configuration.", null);
                    return;
                }
            }
            config.flatten(file);
            file.setReadOnly();
            String name = NLS.bind(Messages.mergedEditorTitle, config.getName());
            MergedEditorFileStorage store = new MergedEditorFileStorage(file, name);
            StorageEditorInput storageInput = new StorageEditorInput(store, file.toURI());
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            page.openEditor(storageInput, CONFIGURATION_EDITOR, true);
        } catch (Throwable t) {
            Trace.logError("Error opening merged editor for: " + uri, t);
        }
    }

    private static ConfigurationFile getConfigFile(URI uri) {
        ConfigurationFile configFile = ConfigUtils.getConfigFile(uri);
        if (configFile != null) {
            return configFile;
        }
        // let Custom Liberty Runtime Providers provide their 'mapped' Config File
        configFile = ConfigUtils.getMappedConfigFile(uri);
        if (configFile != null) {
            return configFile;
        }

        UserDirectory userDir = ConfigUtils.getUserDirectory(uri);
        if (userDir != null) {
            try {
                return new ConfigurationFile(uri, userDir);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to create configuration file for: " + uri);
            }
        }
        return null;
    }

    /**
     * Open the configuration schema browser editor against the given config schema.
     *
     * @param uri
     */
    public static void openSchemaBrowser(URI uri) {
        try {
            URL url = new URL(CONFIGURATION_DOC_URL);
            IWorkbenchBrowserSupport bs = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser b = bs.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR,
                                             SCHEMA_BROWSER_ID,
                                             Messages.configSchemaBrowserName,
                                             Messages.configSchemaBrowserToolTip);
            b.openURL(url);
        } catch (Exception e) {
            Trace.logError("Error opening schema browser", e);
        }
    }

    private static boolean externalEditorConfirmation() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        return MessageDialog.openQuestion(shell, Messages.title, Messages.openEditorOnExternalFile);
    }
}
