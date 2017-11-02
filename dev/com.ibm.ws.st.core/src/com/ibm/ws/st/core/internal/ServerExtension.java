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

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;

/**
 * Extension for downstream plugins to add support for new module types.
 * Subclasses should be stateless and not hold onto server or configuration objects.
 */
public abstract class ServerExtension {
    private ServerExtensionWrapper wrapper;

    /**
     * Init method, called once to pass in the context.
     *
     * @param wrapper
     */
    protected final void init(ServerExtensionWrapper wrapper) {
        this.wrapper = wrapper;
    }

    protected final IServer getServer() {
        return wrapper.getWebSphereServer().getServer();
    }

    protected final WebSphereServer getWebSphereServer() {
        return wrapper.getWebSphereServer();
    }

    protected final WebSphereServerInfo getWebSphereServerInfo() {
        return wrapper.getWebSphereServerInfo();
    }

    protected final ConfigurationFile getConfiguration() {
        return wrapper.getWebSphereServer().getConfiguration();
    }

    /**
     * Returns the partial/base web URL for this server.
     */
    protected String getServerBaseWebURL() {
        return getWebSphereServer().getServerWebURL();
    }

    /**
     * @see ServerDelegate#getChildModules(IModule[])
     */
    public abstract IModule[] getChildModules(IModule[] module);

    /**
     * @see ServerDelegate#getRootModules(IModule)
     */
    public abstract IModule[] getRootModules(IModule module) throws CoreException;

    /**
     * Returns <code>IStatus.OK</code> if the module can be added, <code>IStatus.ERROR</code>
     * if the module is recognized and unsupported, or <code>null</code> if it cannot be
     * recognized.
     *
     * @see ServerDelegate#canModifyModules(IModule[], IModule[])
     */
    public IStatus canAddModule(IModule module) {
        return null;
    }

    /**
     * Returns <code>true</code> if the configuration was modified, or <code>false</code> otherwise.
     *
     * @see ServerDelegate#modifyModules(IModule[], IModule[], IProgressMonitor)
     */
    public boolean modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) {
        return false;
    }

    /**
     * @see IURLProvider#getModuleRootURL(IModule)
     */
    public URL getModuleRootURL(IModule module) {
        return null;
    }

    /**
     * Returns <code>true</code> if the server extension requires a publish on the delta, or <code>false</code> otherwise.
     *
     */
    public boolean isPublishRequired(IModule[] modules, IResourceDelta delta) {
        return false;
    }

    @Override
    public String toString() {
        return "ServerExtension [" + getClass().toString() + "]";
    }

    /**
     *
     * @see ServerBehaviourDelegate#canRestartModule(IModule[])
     */
    public boolean canRestartModule(IModule[] module) {
        return true;
    }

    public OutOfSyncModuleInfo checkModuleConfigOutOfSync(IModule module) {
        final ConfigurationFile configFile = getConfiguration();
        final Application[] apps = configFile.getApplications();
        for (Application app : apps) {
            if (app.getName().equals(module.getName())) {
                return null;
            }
        }
        return new OutOfSyncModuleInfo(OutOfSyncModuleInfo.Type.APP_ENTRY_MISSING);
    }

    protected void handleExistingAppEntryInConfigAndDropins(IModule module, Application[] appsInConfig) {
        if (module != null && module.getName() != null) {
            String appName = module.getName();

            WebSphereServerBehaviour serverBehaviour = getWebSphereServer().getWebSphereServerBehaviour();
            if (serverBehaviour == null) // This should not happen
                return;

            HashSet<String> ignoredConfigFileModuleNames = new HashSet<String>();
            // If the exclusion is not done here, on server startup it will complain that the module
            // did not start and display an error
            HashMap<IModule, ExcludeSyncModuleInfo> sync = serverBehaviour.getExcludeSyncModules();
            Set<IModule> keys = sync.keySet();
            for (IModule key : keys) {
                if (key != null) {
                    String name = key.getName();
                    if (name != null)
                        ignoredConfigFileModuleNames.add(name);
                }
            }

            // including app entries without app files on the server
            for (Application app : appsInConfig) {
                if (appName.equals(app.getName())) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Application, " + appName + " is in the configuration before add it. ");

                    if (ignoredConfigFileModuleNames.contains(appName)) {
                        continue;
                    }

                    List<String> apps = serverBehaviour.getOverriddenAppsInServerXML();
                    if (!apps.contains(appName))
                        apps.add(appName);
                    return;
                }
            }

            // cover external module in dropins
            for (IModule m : getServer().getModules()) {
                if (m.isExternal() && appName.equals(m.getName())) {
                    if (ignoredConfigFileModuleNames.contains(appName))
                        continue;
                    List<String> apps = serverBehaviour.getOverriddenDropinsApps();
                    if (!apps.contains(appName))
                        apps.add(appName);
                    return;

                }
            }
        }
    }

    protected boolean shouldExcludeAddingModuleToConfig(IModule module) {
        // For this Liberty maven or Gradle change, it is extremely high risk to modify this in general without
        // testing the implications. For now, we have to special case it
        if (Constants.SERVER_TYPE_LIBERTY_MAVEN.equals(getWebSphereServer().getServerType()) ||
            Constants.SERVER_TYPE_LIBERTY_GRADLE.equals(getWebSphereServer().getServerType())) {
            ConfigurationFile config = getWebSphereServer().getConfiguration();
            if (config != null) {
                Application[] app = config.getApplications();
                for (int i = 0; i < app.length; i++) {
                    if (app[i] != null && module.getName().equals(app[i].getName())) {
                        return true;
                    }
                }

                // If dropins, do not add the application to the configuration file
                // The Liberty Maven or Gradle case is dealt with specifically to avoid regression. If the dropins case
                // needs to be handled in the base Liberty case, more work needs to be done to support this
                WebSphereServerBehaviour behaviour = (WebSphereServerBehaviour) getServer().loadAdapter(WebSphereServerBehaviour.class, null);
                if (behaviour != null) {
                    HashMap<IModule, ExcludeSyncModuleInfo> excluded = behaviour.getExcludeSyncModules();
                    ExcludeSyncModuleInfo info = excluded.get(module);
                    if (info != null) {
                        HashMap<String, String> map = info.getProperties();
                        if (map != null) {
                            if ("dropins".equals(map.get(ExcludeSyncModuleInfo.APPS_DIR))) {
                                return true;
                            }
                        }
                    }
                }
            }

        }

        return false;
    }
}