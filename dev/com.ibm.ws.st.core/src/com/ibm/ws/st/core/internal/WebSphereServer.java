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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.servertype.AbstractServerExtension;
import com.ibm.ws.st.common.core.ext.internal.servertype.ServerTypeExtensionFactory;
import com.ibm.ws.st.common.core.internal.CommonServerUtil;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.ResolverResult;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour.ApplicationStateTracker;
import com.ibm.ws.st.core.internal.config.Bootstrap;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.jmx.JMXConnectionInfo;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;

public class WebSphereServer extends ServerDelegate implements IURLProvider, IAdaptable {
    public static final String PROP_SERVER_NAME = "serverName";
    public static final String PROP_LOOSE_CONFIG = "looseConfig";
    public static final String PROP_STOP_ON_SHUTDOWN = "stopOnShutdown";
    public static final String PUBLISH_WITH_ERROR = "publishWithError";
    public static final String PROP_USER_NAME = "userName";
    public static final String PROP_SECURE_PORT = "securePort";
    public static final String PROP_SERVER_TYPE = "serverType";
    public static final String LIBERTY_SERVER_TYPE = "libertyServer";
    public static final String PROP_USERDIR_ID = "userDirId";

    // remote server secure storage keys
    public static final String SECURE_SERVER_CONNECTION_PASSWORD_KEY = ".serverConnectionPassword";

    private String tempServerConnectionPassword = "";

    private String cachedHost = "localhost";
    private boolean cachedIsLocalHost = true;

    private JMXConnectionInfo jmxConnectionInfo = null;

    protected ServerExtensionWrapper[] serverExtensions;

    protected transient static Vector<PropertyChangeListener> propertyListeners;

    protected AbstractServerExtension serverExtension = null;

    public WebSphereServer() {
        // do nothing
    }

    /**
     * Add a property change listener
     *
     * @param listener
     *            java.beans.PropertyChangeListener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyListeners == null)
            propertyListeners = new Vector<PropertyChangeListener>();
        propertyListeners.add(listener);
    }

    @SuppressWarnings("rawtypes")
    public void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue) {
        if (propertyListeners == null)
            return;

        PropertyChangeEvent event = new PropertyChangeEvent(RemoteServerInfo.class, propertyName, oldValue, newValue);
        try {
            Vector clone = (Vector) propertyListeners.clone();
            int size = clone.size();
            for (int i = 0; i < size; i++)
                try {
                    PropertyChangeListener listener = (PropertyChangeListener) clone.elementAt(i);
                    listener.propertyChange(event);
                } catch (Exception e) {
                    // Do nothing
                }
        } catch (Exception e) {
            // Do nothing
        }
    }

    @Override
    protected void initialize() {
        getServerExtensions();
    }

    @Override
    public void setDefaults(IProgressMonitor monitor) {
        setAttribute("auto-publish-setting", 2);
        setAttribute("auto-publish-time", 1);
        setAttribute(PROP_LOOSE_CONFIG, true);
        setAttribute(PROP_SERVER_TYPE, LIBERTY_SERVER_TYPE);
    }

    protected synchronized ServerExtensionWrapper[] getServerExtensions() {
        if (serverExtensions != null)
            return serverExtensions;

        if (getServer().isWorkingCopy()) {
            IServer server = ((IServerWorkingCopy) getServer()).getOriginal();
            if (server != null) {
                WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                if (ws != this)
                    return ws.getServerExtensions();
            }
        }
        serverExtensions = ServerExtensionWrapper.createServerExtensions();
        for (ServerExtensionWrapper se : serverExtensions)
            se.initServer(this);

        return serverExtensions;
    }

    public String getServerName() {
        return getAttribute(PROP_SERVER_NAME, (String) null);
    }

    public String getServerDisplayName() {
        return getServerTypeExtension().getServerDisplayName(getServer());
    }

    public void setServerName(String name) {
        setAttribute(PROP_SERVER_NAME, name);
        refreshConfiguration();
    }

    public boolean isLooseConfigEnabled() {
        return getAttribute(PROP_LOOSE_CONFIG, false);
    }

    public void setLooseConfigEnabled(boolean looseConfigEnabled) {
        setAttribute(PROP_LOOSE_CONFIG, looseConfigEnabled);
    }

    public boolean isStopOnShutdown() {
        return getAttribute(PROP_STOP_ON_SHUTDOWN, true);
    }

    public void setStopOnShutdown(boolean stopOnShutdown) {
        setAttribute(PROP_STOP_ON_SHUTDOWN, stopOnShutdown);
    }

    public void SetPublishWithError(boolean publishWithError) {
        setAttribute(PUBLISH_WITH_ERROR, publishWithError);

    }

    public void setStopTimeout(int timeout) {
        setAttribute("stop-timeout", timeout);
    }

    public boolean isPublishWithError() {
        return getAttribute(PUBLISH_WITH_ERROR, false);
    }

    public void setServerUserName(String userName) {
        String oldValue = getServerUserName();
        if (oldValue.equals(userName))
            return;

        setServerProperty(PROP_USER_NAME, userName);
    }

    public String getServerUserName() {
        return getServerPropertyString(PROP_USER_NAME);
    }

    public void setServerPassword(String curPassword) {
        String oldPasswd = getServerPassword();
        if (oldPasswd.equals(curPassword))
            return;

        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences node = preferences.node(CommonServerUtil.getSecurePreferenceNodeName(getServer()));
        try {
            node.put(SECURE_SERVER_CONNECTION_PASSWORD_KEY, curPassword, true);
            preferences.flush();
        } catch (Exception e) {
            Trace.logError("Failed to store server password", e);
        }
    }

    public String getServerPassword() {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        String nodeName = CommonServerUtil.getSecurePreferenceNodeName(getServer());
        ISecurePreferences node = preferences.node(nodeName);
        String password = "";
        try {
            password = node.get(SECURE_SERVER_CONNECTION_PASSWORD_KEY, "");
            if (password.equals("")) {
                password = node.get(".password", "");
            }
        } catch (StorageException e) {
            Trace.logError("Failed to retrieve server password", e);
        }
        return password;
    }

    public String getTempServerConnectionPassword() {
        return tempServerConnectionPassword;
    }

    public void setTempServerConnectionPassword(String tempPassword) {
        String oldPass = getTempServerConnectionPassword();
        if (oldPass != null && oldPass.equals(tempPassword))
            return;
        this.tempServerConnectionPassword = tempPassword;
        firePropertyChangeEvent(SECURE_SERVER_CONNECTION_PASSWORD_KEY, oldPass, tempPassword);
    }

    public void setServerSecurePort(String securePort) {
        String oldValue = getServerSecurePort();
        if (oldValue.equals(securePort))
            return;

        setServerProperty(PROP_SECURE_PORT, securePort);
    }

    public String getServerSecurePort() {
        return getServerPropertyString(PROP_SECURE_PORT);
    }

    /**
     * This should match the type name specified in the server type
     * extension point or libertyServer for the basic liberty server.
     */
    public void setServerType(String type) {
        String oldValue = getServerType();
        if (type.equals(oldValue))
            return;

        setServerProperty(PROP_SERVER_TYPE, type);

        // Clear out the cached server extension
        serverExtension = null;
    }

    public String getServerType() {
        return getAttribute(PROP_SERVER_TYPE, LIBERTY_SERVER_TYPE);
    }

    public boolean isPublishToRuntime() {
        return true;
    }

    @Override
    public IStatus canModifyModules(IModule[] add, IModule[] remove) {
        // confirm the runtime isn't missing
        if (getServer().getRuntime() == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorNoRuntime, null);

        if (getServerName() != null && getServerInfo() == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorNoServer, getServerName()), null);

        // confirm the facets are supported by WebSphere
        if (add != null) {
            for (IModule module : add) {
                if (module.getProject() != null) {
                    IStatus status = FacetUtil.verifyFacets(module.getProject(), getServer());
                    if (status != null && !status.isOK())
                        return status;
                }
            }
        }

        // return ok if at least one extension can accept the change
        if (add != null && add.length > 0) {
            int unsupported = add.length;
            for (IModule module : add) {
                boolean recognized = false;
                for (ServerExtensionWrapper se : getServerExtensions()) {
                    if (se.supports(module.getModuleType())) {
                        // TODO performance: no need to call getRootModules() if no
                        // modules of the root type (se.getRootModuleTypes()) exist
                        IStatus status = se.canAddModule(module);
                        if (status != null && !status.isOK())
                            return status;
                        if (status != null)
                            recognized = true;
                    }
                }
                if (!recognized)
                    break;
                unsupported--;
            }
            if (unsupported == 0)
                return Status.OK_STATUS;

            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorModuleNotRecognized, null);
        }

        return Status.OK_STATUS;
    }

    @Override
    public IModule[] getChildModules(IModule[] module) {
        if (module == null)
            return null;

        // aggregate child modules from all extensions
        List<IModule> children = new ArrayList<IModule>();
        for (ServerExtensionWrapper se : getServerExtensions()) {
            if (se.supports(module[module.length - 1].getModuleType())) {
                IModule[] c = se.getChildModules(module);
                if (c != null && c.length > 0) {
                    for (IModule m : c)
                        children.add(m);
                }
            }
        }

        return children.toArray(new IModule[children.size()]);
    }

    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        if (module == null)
            return null;

        // aggregate root modules from all extensions
        List<IModule> root = new ArrayList<IModule>();
        for (ServerExtensionWrapper se : getServerExtensions()) {
            if (se.supports(module.getModuleType())) {
                // TODO performance: no need to call getRootModules() if no
                // modules of the root type (se.getRootModuleTypes()) exist
                IModule[] c = se.getRootModules(module);
                if (c != null && c.length > 0) {
                    for (IModule m : c)
                        root.add(m);
                }
            }
        }

        if (root.size() > 1) {
            // favour root modules that are already installed on the server
            IModule[] existingModules = getServer().getModules();
            IModule m = null;
            for (IModule mr : root) {
                for (IModule me : existingModules) {
                    if (mr.equals(me)) {
                        m = mr;
                        break;
                    }
                }
                if (m != null)
                    break;
            }
            if (m != null && !m.equals(root.get(0))) {
                // move found module to position 0
                root.remove(m);
                root.add(0, m);
            }
        }

        return root.toArray(new IModule[root.size()]);
    }

    private void gatherChildModules(List<IModule[]> moduleList, IModule[] parent) {
        int size = parent.length;
        IModule[] children = getChildModules(parent);
        for (IModule m : children) {
            IModule[] mod = new IModule[size + 1];
            System.arraycopy(parent, 0, mod, 0, size);
            mod[size] = m;
            moduleList.add(mod);
            gatherChildModules(moduleList, mod);
        }
    }

    @Override
    public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
        IStatus status = canModifyModules(add, remove);
        if (status == null || status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);

        AbstractServerExtension serverExt = getServerTypeExtension();
        if (serverExt != null) {
            status = serverExt.preModifyModules(getServer(), add, remove, monitor);
            if (!status.isOK()) {
                throw new CoreException(status);
            }
        }

        // make sure we start with the latest config file
        if (getConfiguration().getIFile() == null)
            refreshConfiguration();

        boolean configModified = false;
        if (add != null && add.length > 0) {
            ConfigurationFile configFile = getConfiguration();
            final List<String> allFeatures = configFile.getAllFeatures();

            if (!isFeatureConfigured(Constants.FEATURE_LOCAL_JMX)) {
                configFile.addFeature(Constants.FEATURE_LOCAL_JMX);
                allFeatures.add(Constants.FEATURE_LOCAL_JMX);
                configModified = true;
            }

            // build up full module list
            List<IModule[]> moduleList = new ArrayList<IModule[]>();
            for (IModule module : add) {
                IModule[] mod = new IModule[] { module };
                moduleList.add(mod);
                gatherChildModules(moduleList, mod);
            }

            final Map<String, List<String>> alwaysAdd = new HashMap<String, List<String>>();
            List<String> featuresToAdd = new ArrayList<String>();
            RequiredFeatureMap rfm = getRequiredFeatures(configFile, moduleList, null, monitor);
            if (rfm != null) {
                Map<IProject, ProjectPrefs> prefsMap = new HashMap<IProject, ProjectPrefs>();
                FeatureResolverFeature[] rfs = rfm.getFeatures();
                for (FeatureResolverFeature rf : rfs) {
                    List<IModule[]> modules = rfm.getModules(rf);
                    for (IModule[] module : modules) {
                        IProject project = module[module.length - 1].getProject();
                        if (project == null)
                            continue;

                        ProjectPrefs prefs = prefsMap.get(project);
                        if (prefs == null) {
                            prefs = new ProjectPrefs(project);
                            prefsMap.put(project, prefs);
                        }

                        String featureName = rf.getName();
                        if (!featuresToAdd.contains(featureName)
                            && prefs.getFeaturePrompt(featureName) == ProjectPrefs.ADD_FEATURE_ALWAYS) {
                            featuresToAdd.add(featureName);
                            List<String> appList = alwaysAdd.get(featureName);
                            if (appList == null) {
                                appList = new ArrayList<String>();
                                alwaysAdd.put(featureName, appList);
                            }
                            appList.add(module[0].getName());
                            break;
                        }
                    }
                }
            }

            List<String> combinedFeatures = new ArrayList<String>(allFeatures);
            combinedFeatures.addAll(featuresToAdd);

            ResolverResult result = RuntimeFeatureResolver.resolve(getWebSphereRuntime(), combinedFeatures);
            Set<FeatureConflict> conflicts = result.getFeatureConflicts();
            boolean ignoreConflicts = shouldIgnoreConflicts(conflicts);
            FeatureConflictHandler featureConflictHandler = Activator.getFeatureConflictHandler();
            if (conflicts != null && !conflicts.isEmpty() && featureConflictHandler != null && !ignoreConflicts) {
                if (featureConflictHandler.handleFeatureConflicts(getServerInfo(), alwaysAdd, conflicts, false)) {
                    configModified = true;
                }
            } else if (!featuresToAdd.isEmpty()) {
                for (String s : featuresToAdd) {
                    configFile.addFeature(s);
                }
                configModified = true;
            }

            if (!configFile.hasElement(Constants.APPLICATION_MONITOR)) {
                configFile.addElement(Constants.APPLICATION_MONITOR);
                configModified = true;
            }

            configFile.setAttribute(Constants.APPLICATION_MONITOR, Constants.APPLICATION_MONITOR_TRIGGER, Constants.APPLICATION_MONITOR_MBEAN);
        }

        // check for removal of external module
        if (remove != null && remove.length > 0) {
            ConfigurationFile configFile = getConfiguration();

            if (!isFeatureConfigured(Constants.FEATURE_LOCAL_JMX)) {
                configFile.addFeature(Constants.FEATURE_LOCAL_JMX);
                configModified = true;
            }

            for (IModule module : remove) {
                if (module.isExternal()) {
                    // TODO - should we remove the app folder on disk?
                    getConfiguration().removeApplication(module.getName());
                    configModified = true;
                }
            }
        }

        // use server extensions to add or remove module
        for (ServerExtensionWrapper se : getServerExtensions()) {
            List<IModule> addList = new ArrayList<IModule>();
            if (add != null) {
                for (IModule module : add) {
                    if (se.supports(module.getModuleType())) {
                        addList.add(module);
                        //clear states before we modify the config file so we don't loose any state change
                        getWebSphereServerBehaviour().appStateTracker.andOpAppState(module.getName(), ApplicationStateTracker.NEED_RESTART_APP); // clear all states except 14W
                    }
                }
            }

            List<IModule> removeList = new ArrayList<IModule>();
            if (remove != null) {
                for (IModule module : remove) {
                    if (se.supports(module.getModuleType())) {
                        removeList.add(module);
                        //clear states before we modify the config file so we don't loose any state change
                        getWebSphereServerBehaviour().appStateTracker.andOpAppState(module.getName(), 0); // clear all states
                    }
                }
            }

            if (addList.size() > 0 || removeList.size() > 0) {
                if (se.modifyModules(addList.toArray(new IModule[addList.size()]), removeList.toArray(new IModule[removeList.size()]), monitor))
                    configModified = true;
            }
        }

        if (configModified) {
            try {
                getConfiguration().save(monitor);
            } catch (IOException e) {
                Trace.logError("Error saving configuration for server: " + getServerName(), e);
                status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorServerConfigurationModifyFailed, getConfiguration().getPath().toOSString(), e));
                throw new CoreException(status);
            }
            if (getConfiguration().getIFile() == null)
                refreshConfiguration();
        }
    }

    /** Add localConnector feature and save config file */
    public void addLocalConnectorFeature(IProgressMonitor monitor) {
        boolean configModified = false;
        ConfigurationFile configFile = getConfiguration();

        if (!isFeatureConfigured(Constants.FEATURE_LOCAL_JMX)) {
            configFile.addFeature(Constants.FEATURE_LOCAL_JMX);
            configModified = true;
        }

        if (configModified) {
            try {
                configFile.save(monitor);
            } catch (IOException e) {
                Trace.logError("Error saving configuration for server: " + getServerName(), e);
            }
        }
    }

    /** Returns new features that are required and that are not currently present in configFile feature set (or otherwise not supported) */
    public RequiredFeatureMap getRequiredFeatures(ConfigurationFile configFile, List<IModule[]> moduleList, List<IModuleResourceDelta[]> deltaList,
                                                  IProgressMonitor monitor) throws CoreException {
        if (!Activator.isAutomaticFeatureDetectionEnabled()) {
            return null;
        }

        if (moduleList == null || moduleList.isEmpty())
            return null;

        if (deltaList != null && moduleList.size() != deltaList.size()) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "The delta list should be either null or have the same size as the module list", null);
            return null;
        }

        // check for required features
        WebSphereRuntime wr = getWebSphereRuntime();
        FeatureSet existingFeatures = new FeatureSet(wr, configFile.getAllFeatures());

        List<IModule[]> allModules = new ArrayList<IModule[]>(moduleList.size());
        List<IModuleResourceDelta[]> allDeltas = deltaList == null ? null : new ArrayList<IModuleResourceDelta[]>(deltaList.size());
        for (int i = 0; i < moduleList.size(); i++) {
            IModule[] module = moduleList.get(i);
            if (!module[0].isExternal()) {
                allModules.add(module);
                if (deltaList != null && allDeltas != null) {
                    allDeltas.add(deltaList.get(i));
                }
            }
        }

        RequiredFeatureMap requiredFeatureMap = FeatureResolverWrapper.getAllRequiredFeatures(wr, allModules, allDeltas, existingFeatures, false, monitor);

        if (requiredFeatureMap.isEmpty())
            return null;

        // remove features with no version information, if the list also
        // contains the same features with version information,
        // e.g. project facet define servlet-3.0 and we also get servlet,
        //      in that case we remove servlet and use servlet-3.0
        //
        // remove features with no version information, if the feature list in
        // the configuration file contains the same feature with version
        // information
        FeatureResolverFeature[] requiredFeatures = requiredFeatureMap.getFeatures();
        for (FeatureResolverFeature rf : requiredFeatures) {
            if (!rf.getName().contains(FeatureUtil.FEATURE_SEPARATOR)) {
                String feature = rf.getName().toLowerCase() + FeatureUtil.FEATURE_SEPARATOR;
                boolean removed = false;
                for (FeatureResolverFeature rf2 : requiredFeatures) {
                    if (rf2.getName().toLowerCase().startsWith(feature)) {
                        // remove from required list
                        List<IModule[]> modules = requiredFeatureMap.getModules(rf);
                        if (modules != null) {
                            for (IModule[] module : modules) {
                                requiredFeatureMap.addModule(rf2, module);
                            }
                        }
                        requiredFeatureMap.removeFeature(rf);
                        removed = true;
                        break;
                    }
                }

                // If the configuration file has a version of the feature
                // remove it from the list
                if (!removed) {
                    String f = existingFeatures.resolve(rf.getName());
                    if (f != null)
                        requiredFeatureMap.removeFeature(rf);
                }
            }
        }

        // remove contained features
        FeatureResolverFeature[] allContainedFeatures = FeatureResolverWrapper.getAllContainedFeatures(wr, allModules, monitor);

        requiredFeatures = requiredFeatureMap.getFeatures();
        checkRequired: for (FeatureResolverFeature required : requiredFeatures) {
            if (allContainedFeatures.length > 0) {
                for (FeatureResolverFeature contained : allContainedFeatures) {
                    if (required.equals(contained)) {
                        // do not add
                        requiredFeatureMap.removeFeature(required);
                        continue checkRequired;
                    } else if (!contained.getName().contains("-") && required.getName().contains("-") && required.getName().startsWith(contained + "-")) {
                        // do not add
                        requiredFeatureMap.removeFeature(required);
                        continue checkRequired;
                    }
                }
            }

            // find the feature in the installed features, if the required feature is not present,
            // then find the closest higher version in the feature list
            String feature = wr.getInstalledFeatures().resolveToHigherVersion(required.getName());

            if (feature == null) {
                // error should show application name, but also give the leaf module name as a hint if it exists
                List<IModule[]> modules = requiredFeatureMap.getModules(required);
                IModule[] module = modules.get(0);
                String app = module[0].getName();
                if (module.length > 1)
                    app += " (" + module[module.length - 1].getName() + ")";

                String[] params = new String[] { app, required.getName(), getServer().getRuntime().getName() };
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorRequiredFeature, params)));
            }

            // If the feature is already supported, remove it from the list
            if (existingFeatures.supports(feature)) {
                requiredFeatureMap.removeFeature(required);
            } else if (!feature.toLowerCase().equals(required.getName().toLowerCase())) {
                // If the feature has accepted alternatives, transfer them to the new feature object
                FeatureResolverFeature newFeature;
                if (required.getAcceptedAlternatives().size() > 0) {
                    newFeature = new FeatureResolverFeature(feature, required.getAcceptedAlternatives().toArray(new String[required.getAcceptedAlternatives().size()]));
                } else {
                    newFeature = new FeatureResolverFeature(feature);
                }
                requiredFeatureMap.replaceFeature(required, newFeature);
            }
        }

        if (requiredFeatureMap.isEmpty())
            return null;

        // remove features that are contained by existing features in the config
        requiredFeatures = requiredFeatureMap.getFeatures();
        for (String f : existingFeatures) {
            for (FeatureResolverFeature fAdd : requiredFeatures) {
                if (wr.isContainedBy(fAdd.getName(), f)) {
                    requiredFeatureMap.removeFeature(fAdd);
                    break;
                }
            }
        }

        if (requiredFeatureMap.isEmpty())
            return null;

        // remove features that have accepted alternatives in existing feature set
        // added as part of WASRTC 122007
        requiredFeatures = requiredFeatureMap.getFeatures();
        for (String f : existingFeatures) {
            for (FeatureResolverFeature fAdd : requiredFeatures) {
                if (fAdd.getAcceptedAlternatives().size() > 0) {
                    boolean acceptedAlternativeFound = false;
                    for (String acceptedAlternative : fAdd.getAcceptedAlternatives()) {
                        String resolvedAlternative = wr.getInstalledFeatures().resolve(acceptedAlternative);
                        if (f.equalsIgnoreCase(resolvedAlternative)) {
                            acceptedAlternativeFound = true;
                            break;
                        }
                    }
                    if (acceptedAlternativeFound) {
                        requiredFeatureMap.removeFeature(fAdd);
                    }
                }
            }
        }

        if (requiredFeatureMap.isEmpty())
            return null;

        // remove features that are contained by other features to add
        requiredFeatures = requiredFeatureMap.getFeatures();
        for (int i = 0; i < requiredFeatures.length - 1; i++) {
            for (int j = i + 1; j < requiredFeatures.length; j++) {
                FeatureResolverFeature fi = requiredFeatures[i];
                FeatureResolverFeature fj = requiredFeatures[j];
                if (FeatureUtil.isLowerVersion(fi.getName(), fj.getName()) || wr.isContainedBy(fi.getName(), fj.getName())) {
                    requiredFeatureMap.removeFeature(fi);
                    break;
                } else if (FeatureUtil.isLowerVersion(fj.getName(), fi.getName()) || wr.isContainedBy(fj.getName(), fi.getName())) {
                    requiredFeatureMap.removeFeature(fj);
                    break;
                }
            }
        }

        return (requiredFeatureMap.isEmpty() ? null : requiredFeatureMap);
    }

    @Override
    public URL getModuleRootURL(IModule module) {
        if (module == null) {
            // based on the doc in IURLProvider, we need to return
            // the root of the server when the module is null.
            String s = null;
            try {
                s = getServerWebURL();
                URL url = new URL(s);
                return url;
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Failed to get server root url. " + s, e);
                return null;
            }
        }

        // use first extension capable of detecting the root
        for (ServerExtensionWrapper se : getServerExtensions()) {
            if (se.supports(module.getModuleType())) {
                URL url = se.getModuleRootURL(module);
                if (url != null)
                    return url;
            }
        }
        return null;
    }

    public IStatus validate() {
        // validate the local server name
        String name = getServer().getName();
        if (name == null || name.isEmpty())
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "", null);

        if (getServerInfo() == null)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorNoServer, getServerName()));

        // validate the WebSphere server name
        return validateServerName(getServerName());
    }

    public static IStatus validateServerName(String serverName) {
        if (serverName == null)
            return Status.OK_STATUS;

        int size = serverName.length();
        for (int i = 0; i < size; i++) {
            char c = serverName.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.')) // TODO: should limit to only ASCII
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorServerName, c + ""), null);
        }
        return Status.OK_STATUS;
    }

    public WebSphereRuntime getWebSphereRuntime() {
        if (getServer().getRuntime() == null)
            return null;

        return (WebSphereRuntime) getServer().getRuntime().loadAdapter(WebSphereRuntime.class, null);
    }

    public WebSphereServerBehaviour getWebSphereServerBehaviour() {
        if (getServer() == null)
            return null;

        return (WebSphereServerBehaviour) getServer().loadAdapter(WebSphereServerBehaviour.class, null);
    }

    public WebSphereServerInfo getServerInfo() {
        WebSphereRuntime wr = getWebSphereRuntime();
        if (wr == null)
            return null;

        return wr.getServerInfo(getServerName(), getUserDirectory());
    }

    public void refreshConfiguration() {
        if (getServer().isWorkingCopy()) {
            IServerWorkingCopy wc = (IServerWorkingCopy) getServer();
            IServer server = wc.getOriginal();
            if (server != null) {
                WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                if (ws != this) {
                    ws.refreshConfiguration();
                    return;
                }
            }
        }
        WebSphereServerInfo serverInfo = getServerInfo();
        if (serverInfo == null || serverInfo.getConfigRoot() == null)
            return;
        if (Trace.ENABLED)
            // We need to call getServer() because we want the original one.
            Trace.trace(Trace.INFO, "Configuration refreshed " + this + " " + getServer().isWorkingCopy());
        if (!serverInfo.updateCache())
            return;

        WebSphereServerBehaviour wsb = getWebSphereServerBehaviour();
        if (wsb != null)
            wsb.syncExternalModules();
    }

    public IPath getServerPath() {
        WebSphereServerInfo rc = getServerInfo();
        if (rc == null)
            return null;
        return rc.getServerPath();
    }

    public IPath getOutputPath() {
        WebSphereServerInfo rc = getServerInfo();
        if (rc == null)
            return null;
        return rc.getServerOutputPath();
    }

    /**
     * Returns the folder in the workspace that's used to store configuration files,
     * or <code>null</code> if the configuration is in not within the workspace.
     */
    public IFolder getFolder() {
        WebSphereServerInfo rc = getServerInfo();
        if (rc == null)
            return null;
        return rc.getServerFolder();
    }

    /**
     * Returns the fully qualified file system path to the configuration root.
     */
    public IPath getConfigurationRoot() {
        WebSphereServerInfo serverInfo = getServerInfo();
        if (serverInfo == null)
            return null;

        ConfigurationFile configFile = serverInfo.getConfigRoot();
        if (configFile == null)
            return null;

        return configFile.getPath();
    }

    public Bootstrap getBootstrap() {
        WebSphereServerInfo serverInfo = getServerInfo();
        if (serverInfo == null)
            return null;
        return serverInfo.getBootstrap();
    }

    private void setupUserDir() {
        // Make sure the user dir is initialized to support servers from old workspaces
        if (getServer().isWorkingCopy()) {
            IServer server = ((IServerWorkingCopy) getServer()).getOriginal();
            if (server != null) {
                WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
                if (ws != this) {
                    return;
                }
            }
            // Shouldn't save the working copy if it was created somewhere else
            setupUserDir((IServerWorkingCopy) getServer(), false);
        } else {
            IServerWorkingCopy serverWC = getServer().createWorkingCopy();
            setupUserDir(serverWC, true);
        }
    }

    private void setupUserDir(IServerWorkingCopy server, boolean save) {
        // Look for the first server info that matches the server name (there should only
        // be one for old workspaces)
        List<WebSphereServerInfo> serverInfos = getWebSphereRuntime().getWebSphereServerInfos();
        for (WebSphereServerInfo serverInfo : serverInfos) {
            if (serverInfo.getServerName().equals(getServerName())) {
                server.setAttribute(PROP_USERDIR_ID, serverInfo.getUserDirectory().getUniqueId());
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Setting the user directory for server: " + getServerName() + " to: " + serverInfo.getUserDirectory().getUniqueId());
                }
                if (save) {
                    try {
                        server.save(true, null);
                    } catch (CoreException e) {
                        Trace.logError("Failed to set up the user directory for server: " + getServerName(), e);
                    }
                }
                break;
            }
        }
    }

    public void setUserDir(String userDirId) {
        setAttribute(PROP_USERDIR_ID, userDirId);
    }

    public void setUserDir(UserDirectory userDir) {
        setAttribute(PROP_USERDIR_ID, userDir == null ? null : userDir.getUniqueId());
    }

    public UserDirectory getUserDirectory() {
        String id = getUserDirId();
        WebSphereRuntime wr = getWebSphereRuntime();
        if (id != null && wr != null) {
            return wr.getUserDir(id);
        }
        return null;
    }

    public String getUserDirId() {
        String id = getAttribute(PROP_USERDIR_ID, (String) null);
        if (id == null) {
            setupUserDir();
            id = getAttribute(PROP_USERDIR_ID, (String) null);
        }
        return id;
    }

    public ConfigurationFile getConfigurationFileFromURI(URI uri) {
        WebSphereServerInfo serverInfo = getServerInfo();
        if (serverInfo == null)
            return null;
        return serverInfo.getConfigurationFileFromURI(uri);
    }

    public URI[] getConfigurationURIs() {
        WebSphereServerInfo serverInfo = getServerInfo();
        if (serverInfo == null)
            return new URI[0];
        return serverInfo.getConfigurationURIs();
    }

    public ConfigurationFile getConfiguration() {
        WebSphereServerInfo serverInfo = getServerInfo();
        if (serverInfo == null)
            return null;

        return serverInfo.getConfigRoot();
    }

    @Override
    public ServerPort[] getServerPorts() {
        List<ServerPort> ports = new ArrayList<ServerPort>();

        try {
            // TODO see work item
            //URI uri = getConfigurationSchemaURI();
            //ConfigurationSchema schema = getConfigurationSchema();
            //List<ServerPort> schemaPorts = schema.getPorts();

            ConfigurationFile configFile = getConfiguration();
            if (configFile != null) {
                configFile.getPorts(ports); // will check include files for ports as well
            }
        } catch (Exception e) {
            Trace.logError("Error getting server ports for server: " + getServerName(), e);
        }
        return ports.toArray(new ServerPort[ports.size()]);
    }

    public IPath getWorkAreaTempPath() {
        return getOutputPath().append("workarea").append("com.ibm.ws.server.adapter");
    }

    public IPath getWorkAreaPath() {
        return getOutputPath().append("workarea");
    }

    public String getDefaultRemoteLogDirectory() {
        JMXConnection jmx = null;
        try {
            jmx = createJMXConnection();
            CompositeData metadata = (CompositeData) jmx.getMetadata(Constants.LOGGING_DIR_VAR, "a");
            if (metadata == null)
                return null;
            String logFileDirectory = (String) metadata.get("fileName");
            return logFileDirectory;
        } catch (Exception e) {
            Trace.logError("getRemoteLogDirectory :Connection to Remote Server failed", e);
        } finally {
            if (jmx != null)
                jmx.disconnect();
        }
        return null;
    }

    public IPath getMessagesFile() throws ConnectException, UnsupportedServiceException, IOException {
        //relativePath is null for localhost
        IPath relativePath = RemoteUtils.getRemoteLogDirectory(this);
        if (relativePath != null)
            return relativePath.append(getServerInfo().getMessageFileName());
        return getServerInfo().getMessagesFile();
    }

    /**
     * handles only remote case. Local case is handled in serverInfo object
     *
     * @param fileName
     * @return
     * @throws UnsupportedServiceException
     * @throws ConnectException
     * @throws IOException
     */

    public IPath getTraceLogFile() throws ConnectException, UnsupportedServiceException, IOException {
        IPath relativePath = RemoteUtils.getRemoteLogDirectory(this);
        if (relativePath != null)
            return relativePath.append(getServerInfo().getTraceFileName());
        return getServerInfo().getTraceLogFile();
    }

    @Override
    public String toString() {
        return "WebSphereServer [" + getServerName() + "]";
    }

    public boolean isExternalAppOnServer(String moduleName) {
        return getExternalApp(moduleName) != null;
    }

    public IModule getExternalApp(String moduleName) {
        if (moduleName == null)
            return null;
        IModule[] modules = getServer().getModules();
        for (IModule m : modules) {
            if (m.isExternal() && moduleName.equals(m.getName()))
                return m;
        }
        return null;
    }

    /**
     * Returns the partial/base web URL for this server.
     *
     * @return
     */
    public String getServerWebURL() {
        ConfigurationFile configFile = getConfiguration();
        int httpPort = 9080;
        //special case for remote server. Query MBean server to retrieve the port value
        if (!isLocalSetup()) {
            JMXConnection connection = null;
            try {
                connection = createJMXConnection();
                httpPort = connection.getRemoteUnsecureHTTPPort();
            } catch (Exception e) {
                Trace.logError("Failed to connect to remote server via JMX.", e);
            } finally {
                if (connection != null)
                    connection.disconnect();
            }
        } else if (configFile != null)
            httpPort = configFile.getHTTPPort();
        return getServerTypeExtension().getBaseURL(getServer(), httpPort);
    }

    public boolean isLocalHost() {
        String host = getServer().getHost();
        if (cachedHost.equals(host))
            return cachedIsLocalHost;
        cachedHost = host;
        cachedIsLocalHost = SocketUtil.isLocalhost(host);
        return cachedIsLocalHost;
    }

    public boolean isLocalSetup() {
        AbstractServerExtension serverExt = getServerTypeExtension();
        if (serverExt != null) {
            Boolean isLocalhost = serverExt.isLocalSetup(getServer());
            if (isLocalhost != null) {
                return isLocalhost.booleanValue();
            }
        }

        if (LIBERTY_SERVER_TYPE.equals(getServerType())) {
            return isLocalHost();
        }
        return false;
    }

    public JMXConnection createJMXConnection() throws Exception {
        JMXConnection connection = null;
        if (!isLocalSetup()) {
            String userName = getServerUserName();
            String password = getServerPassword();
            String host = getConnectionHost();
            String portNum = getConnectionPort();
            connection = new JMXConnection(host, portNum, userName, password);
            connection.connect();
            //update the remoteUserDir when new connection is made
            if (getServerInfo() != null)
                getServerInfo().getUserDirectory().setRemoteUserPath(new Path(getWebSphereServerBehaviour().resolveConfigVar("${wlp.user.dir}", connection)));

        } else {
            connection = getServerInfo().createLocalJMXConnection();
        }
        return connection;
    }

    public String getConnectionPort() {
        String port = null;
        String serverPort = getServerSecurePort();
        AbstractServerExtension serverExt = getServerTypeExtension();
        if (serverExt != null) {
            port = serverExt.getConnectionPort(getServer(), serverPort);
        }
        if (port == null) {
            port = serverPort;
        }
        return port;
    }

    public String getConnectionHost() {
        String host = null;
        String serverHost = getServer().getHost();
        String serverPort = getServerSecurePort();
        AbstractServerExtension serverExt = getServerTypeExtension();
        if (serverExt != null) {
            host = serverExt.getConnectionHost(getServer(), serverHost, serverPort);
        }
        if (host == null) {
            host = serverHost;
        }
        return host;
    }

    // Remote Server start up
    public boolean getIsRemoteServerStartEnabled() {
        return getServerPropertyBoolean(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED);
    }

    public void setIsRemoteServerStartEnabled(boolean enabled) {
        boolean oldEnabled = getIsRemoteServerStartEnabled();
        if (oldEnabled == enabled)
            return;
        setServerProperty(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, enabled);
    }

    public String getRemoteServerStartRuntimePath() {
        return getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH);
    }

    public void setRemoteServerStartRuntimePath(String newValue) {
        String oldValue = getRemoteServerStartRuntimePath();
        if (oldValue.equals(newValue)) {
            return;
        }
        setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH, newValue);
    }

    public String getRemoteServerStartConfigPath() {
        return getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH);
    }

    public void setRemoteServerStartConfigPath(String newValue) {
        String oldValue = getRemoteServerStartConfigPath();
        if (oldValue.equals(newValue)) {
            return;
        }
        setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH, newValue);
    }

    public int getRemoteServerStartPlatform() {
        return getAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS);
    }

    public void setRemoteServerStartPlatform(int newValue) {
        int oldValue = getRemoteServerStartPlatform();
        if (oldValue == newValue)
            return;
        setServerProperty(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, newValue);
    }

    public int getRemoteServerStartLogonMethod() {
        // return getServerPropertyInt(PROPERTY_REMOTE_START_LOGONMETHOD);
        return getAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS);
    }

    public void setRemoteServerStartLogonMethod(int newValue) {
        int oldValue = getRemoteServerStartLogonMethod();
        if (oldValue == newValue)
            return;
        setServerProperty(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, newValue);
    }

    // Remote Server start up - OS ID
    public String getRemoteServerStartOSId() {
        return getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID);
    }

    public void setRemoteServerStartOSId(String newValue) {
        String oldValue = getRemoteServerStartOSId();
        if (oldValue.equals(newValue)) {
            return;
        }
        setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID, newValue);
    }

    // Remote Server start up - OS password
    public String getRemoteServerStartOSPassword() {
        RemoteServerInfo remoteInfo = new RemoteServerInfo(RemoteServerInfo.RemoteServerType.Liberty);
        return remoteInfo.getRemoteServerOSPwd(CommonServerUtil.getSecurePreferenceNodeName(getServer()));
    }

    public void setRemoteServerStartOSPassword(String newValue) {
        RemoteServerInfo remoteInfo = new RemoteServerInfo(RemoteServerInfo.RemoteServerType.Liberty);
        remoteInfo.setRemoteServerOSPwd(newValue, CommonServerUtil.getSecurePreferenceNodeName(getServer()));
    }

    // Remote Server start up - SSH ID
    public String getRemoteServerStartSSHId() {
        return getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID);
    }

    public void setRemoteServerStartSSHId(String newValue) {
        String oldValue = getRemoteServerStartSSHId();
        if (oldValue.equals(newValue)) {
            return;
        }
        setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID, newValue);
    }

    // Remote Server start up - SSH passphrase
    public String getRemoteServerStartSSHPassphrase() {
        try {
            String value = getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE);
            if (value.isEmpty())
                return "";
            return PasswordUtil.decode(value);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Failed to get remote SSH passphrase", e);
        }
        return "";
    }

    public void setRemoteServerStartSSHPassphrase(String newValue) {
        String oldValue = getRemoteServerStartSSHPassphrase();
        if (oldValue.equals(newValue)) {
            return;
        }
        try {
            String valueToSet = (newValue.isEmpty()) ? "" : PasswordUtil.encode(newValue);
            setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE, valueToSet);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Failed to set remote SSH passphrase", e);
        }
    }

    // Remote Server start up - OS ID
    public String getRemoteServerStartSSHKeyFile() {
        return getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE);
    }

    public void setRemoteServerStartSSHKeyFile(String newValue) {
        String oldValue = getRemoteServerStartSSHKeyFile();
        if (oldValue.equals(newValue)) {
            return;
        }
        setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, newValue);
    }

    // Cloud enabled/disabled
    public boolean getIsCloudEnabled() {
        return getServerPropertyBoolean(RemoteServerInfo.PROPERTY_IS_CLOUD_ENABLED);
    }

    /**
     * Returns the remote start debug port setting. Default is 7777.
     *
     * @return
     */
    public String getRemoteServerStartDebugPort() {
        try {
            String port = getServerPropertyString(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT);
            if (port == null || port.isEmpty())
                port = "7777";
            return port;
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Failed to get remote SSH passphrase", e);
        }
        return "7777";
    }

    /**
     * Returns the remote start debug port setting. Default is 7777.
     *
     * @return
     */
    public void setRemoteServerStartDebugPort(String newValue) {
        String oldValue = getRemoteServerStartDebugPort();
        if (oldValue == newValue) {
            return;
        }
        setAttribute(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT, newValue);
    }

    protected void setServerProperty(String propName, boolean propValue) {
        if (propName == null) {
            return;
        }
        setAttribute(propName, propValue);
    }

    protected void setServerProperty(String propName, int propValue) {
        if (propName == null) {
            return;
        }
        setAttribute(propName, propValue);
    }

    protected void setServerProperty(String propName, String propValue) {
        if (propName == null) {
            return;
        }
        setAttribute(propName, propValue == null ? "" : propValue);
    }

    protected void setServerProperty(String propName, List<String> propValue) {
        if (propName == null) {
            return;
        }
        setAttribute(propName, propValue == null ? new ArrayList<String>(0) : propValue);
    }

    protected boolean getServerPropertyBoolean(String propName) {
        return getAttribute(propName, false);
    }

    protected boolean getServerPropertyBoolean(String propName,
                                               boolean defaultValue) {
        return getAttribute(propName, defaultValue);
    }

    protected int getServerPropertyInt(String propName) {
        return getAttribute(propName, -1);
    }

    protected String getServerPropertyString(String propName) {
        return getAttribute(propName, "");
    }

    @SuppressWarnings("unchecked")
    protected List<Object> getServerPropertyList(String propName) {
        return getAttribute(propName, new ArrayList<String>());
    }

    protected void setJMXConnectionInfo(JMXConnectionInfo jmxInfo) {
        if (jmxInfo == null)
            return;
        jmxConnectionInfo = jmxInfo;
    }

    protected JMXConnectionInfo getJMXConnectionInfo() {
        return jmxConnectionInfo;
    }

    public void setRemoteServerProperties(RemoteServerInfo remoteInfo) {
        setIsRemoteServerStartEnabled(remoteInfo.getBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, getIsRemoteServerStartEnabled()));
        setRemoteServerStartPlatform(remoteInfo.getIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_WINDOWS));
        setRemoteServerStartRuntimePath(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH));
        setRemoteServerStartConfigPath(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH));
        setRemoteServerStartLogonMethod(remoteInfo.getIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, RemoteServerInfo.REMOTE_SERVER_STARTUP_LOGON_OS));
        setRemoteServerStartOSId(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID));
        setRemoteServerStartOSPassword(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD));
        setRemoteServerStartSSHKeyFile(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE));
        setRemoteServerStartSSHId(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID));
        setRemoteServerStartSSHPassphrase(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE));
        setRemoteServerStartDebugPort(remoteInfo.getStringValue(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT));
    }

    public RemoteServerInfo getRemoteServerInfo() {
        RemoteServerInfo remoteInfo = new RemoteServerInfo(RemoteServerInfo.RemoteServerType.Liberty);
        remoteInfo.putBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, getIsRemoteServerStartEnabled());
        remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, getRemoteServerStartPlatform());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH, getRemoteServerStartRuntimePath());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH, getRemoteServerStartConfigPath());
        remoteInfo.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_LOGONMETHOD, getRemoteServerStartLogonMethod());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_OS_ID, getRemoteServerStartOSId());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_OS_PWD, getRemoteServerStartOSPassword());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_KEY_FILE, getRemoteServerStartSSHKeyFile());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_ID, getRemoteServerStartSSHId());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_SSH_PASSPHRASE, getRemoteServerStartSSHPassphrase());
        remoteInfo.put(RemoteServerInfo.PROPERTY_REMOTE_START_DEBUG_PORT, getRemoteServerStartDebugPort());
        return remoteInfo;
    }

    public void setDisableUtilityPromptPref(String key, boolean value) {
        if (key != null) {
            String newKey = getServer().getId() + "_" + key;
            Activator.setPreference(newKey, value);
        }
    }

    public boolean getDisableUtilityPromptPref(String key) {
        if (key != null) {
            String newKey = getServer().getId() + "_" + key;
            return Activator.getPreference(newKey, false);
        }
        return false;
    }

    /**
     * Get the known feature conflicts
     */
    public Set<FeatureConflict> readIgnoredFeatureConflicts() {
        Set<FeatureConflict> ignoredFeatureConflicts = Collections.emptySet();
        IPath path = null;
        try {
            path = getWebSphereServerBehaviour().getTempDirectory().append(Constants.IGNORED_FEATURES);
            File ignoredFeaturesFile = path.toFile();
            if (ignoredFeaturesFile.exists()) {
                ignoredFeatureConflicts = IgnoredFeatureConflictMetadataHandler.read(ignoredFeaturesFile);
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Cannot retrieve ignored features from " + path, e);
        }

        return ignoredFeatureConflicts;
    }

    public void saveIgnoredFeatureConflicts(Set<FeatureConflict> knownConflicts) {
        IPath path = null;
        try {
            path = getWebSphereServerBehaviour().getTempDirectory().append(Constants.IGNORED_FEATURES);
            File ignoredFeaturesFile = path.toFile();
            if (knownConflicts != null && !knownConflicts.isEmpty()) {
                if (ignoredFeaturesFile.exists())
                    FileUtil.deleteFile(ignoredFeaturesFile);
                IgnoredFeatureConflictMetadataHandler.generateMetadataFile(path, knownConflicts);
            } else {
                FileUtil.deleteFile(ignoredFeaturesFile);
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Cannot write ignored features to " + path, e);
        }
    }

    public boolean shouldIgnoreConflicts(Set<FeatureConflict> conflicts) {
        if (conflicts.isEmpty())
            return true;
        Set<FeatureConflict> ignoredConflicts = readIgnoredFeatureConflicts();
        if (ignoredConflicts.size() != conflicts.size())
            return false;

        for (FeatureConflict ignoreConflict : ignoredConflicts) {
            boolean found = false;
            for (FeatureConflict conflict : conflicts) {
                if (ignoreConflict.equals(conflict))
                    found = true;
            }
            if (!found)
                return false;
        }
        return true;
    }

    /**
     * Saves the serviceInfo attributes into the server
     *
     * @param serviceInfo
     */
    public void setServiceInfo(Map<String, String> serviceInfo) {
        if (serviceInfo != null) {
            Iterator<Entry<String, String>> entries = serviceInfo.entrySet().iterator();
            ISecurePreferences node = getSecurePreferencesNode();
            while (entries.hasNext()) {
                Entry<String, String> entry = entries.next();
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    if (value == null) {
                        node.remove(key);
                    } else {
                        node.put(key, value, true);
                    }
                } catch (Exception e) {
                    Trace.logError("Failed to save the value for the " + key + " key in the secure storage.", e);
                }
            }
            try {
                // Make sure the preferences are saved right away in case eclipse gets
                // killed (preferences are only saved automatically on normal exit).
                node.flush();
            } catch (IOException e) {
                Trace.logError("Failed to save secure preferences for the " + getServerName() + " server.  If eclipse is restarted the server may not restore properly.", e);
            }
        }
    }

    public Map<String, String> getServiceInfo() {
        Map<String, String> serviceInfo = new HashMap<String, String>();
        AbstractServerExtension ext = getServerTypeExtension();
        if (ext != null) {
            String[] keys = ext.getServiceInfoKeys();
            ISecurePreferences node = getSecurePreferencesNode();
            for (String key : keys) {
                try {
                    String value = node.get(key, (String) null);
                    serviceInfo.put(key, value);
                } catch (Exception e) {
                    Trace.logError("Failed to retrieve the value for the " + key + " key from the secure storage.", e);
                }
            }
        }
        // Update remote setting in case they were changed through the server editor
        RemoteUtils.copyRemoteInfoToServiceInfo(getRemoteServerInfo(), serviceInfo);
        return serviceInfo;
    }

    private ISecurePreferences getSecurePreferencesNode() {
        String nodeName = CommonServerUtil.getSecurePreferenceNodeName(getServer());
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        return preferences.node(nodeName);
    }

    /**
     * Do any necessary cleanup before the server is deleted
     */
    public void cleanup() {
        getServerTypeExtension().cleanup(getServer());
    }

    public boolean requiresRemoteStartSettings() {
        return getServerTypeExtension().requiresRemoteStartSettings(getServer());
    }

    private AbstractServerExtension getServerTypeExtension() {
        if (serverExtension == null) {
            serverExtension = ServerTypeExtensionFactory.getServerExtension(getServerType());
            if (serverExtension == null) {
                serverExtension = new BaseLibertyServerExtension();
            }
        }

        return serverExtension;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(Class adapter) {
        AbstractServerExtension serverExt = getServerTypeExtension();
        if (adapter.isInstance(serverExt)) {
            return serverExt;
        }
        return null;
    }

    public boolean isFeatureConfigured(String featureName) {
        ConfigurationFile cf = getConfiguration();
        if (cf == null)
            return false;
        List<String> allFeatures = cf.getAllFeatures();
        return FeatureSet.resolve(featureName, allFeatures.toArray(new String[allFeatures.size()])) != null;
    }

    public void ensureLocalConnectorAndAppMBeanConfig(IProgressMonitor monitor) {
        boolean configModified = false;
        final ConfigurationFile configFile = getConfiguration();

        if (!isFeatureConfigured(Constants.FEATURE_LOCAL_JMX)) {
            configFile.addFeature(Constants.FEATURE_LOCAL_JMX);
            configModified = true;
        }

        if (!configFile.hasElement(Constants.APPLICATION_MONITOR)) {
            configFile.addElement(Constants.APPLICATION_MONITOR);
            configFile.setAttribute(Constants.APPLICATION_MONITOR, Constants.APPLICATION_MONITOR_TRIGGER, Constants.APPLICATION_MONITOR_MBEAN);
            configModified = true;
        } else {
            // Also another unlikely event where the trigger isn't mbean
            String updateType = configFile.getResolvedAttributeValue(Constants.APPLICATION_MONITOR, Constants.APPLICATION_MONITOR_TRIGGER);
            if (updateType == null || !updateType.equals(Constants.APPLICATION_MONITOR_MBEAN)) {
                configFile.setAttribute(Constants.APPLICATION_MONITOR, Constants.APPLICATION_MONITOR_TRIGGER, Constants.APPLICATION_MONITOR_MBEAN);
                configModified = true;
            }
        }
        if (configModified) {
            try {
                configFile.save(monitor);
            } catch (IOException e) {
                Trace.logError("Error saving configuration for server: " + getServerName(), e);
            }
        }
    }

}