/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;

import com.ibm.ws.st.core.internal.jmx.JMXConnection;

public class ServerExtensionWrapper {
    private static final String EXTENSION_POINT = "serverExtensions";
    private static final String MODULE = "module";
    private static final String TYPE = "type";
    private static final String APPLICATION_TYPE = "applicationType";
    private static final String APPLICATION_ELEMENT = "applicationElement";

    private static String[] allApplicationTypes;
    private static String[] allGenericModuleTypes;
    private static String[] allApplicationElements;

    private static final String GENERIC_APP_TYPE = "generic";

    private final IConfigurationElement configElement;
    private final String[] moduleTypes;
    private final String[] applicationTypes;
    private final String[] genericModuleTypes;
    private final String[] applicationElements;
    private static final HashMap<String, String> applicationElementToTypeMap = new HashMap<String, String>(6);
    private ServerExtension delegate;
    private ApplicationPublisher publishDelegate;
    private WebSphereServer server;
    private WebSphereServerBehaviour servBehaviour;
    private JMXConnection jmxConnection;

    /**
     * Load the server extensions.
     */
    public static ServerExtensionWrapper[] createServerExtensions() {
        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "->- Loading .serverExtensions extension point ->-");

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(Activator.PLUGIN_ID, EXTENSION_POINT);
        List<ServerExtensionWrapper> list = new ArrayList<ServerExtensionWrapper>(cf.length);

        List<String> appTypeList = new ArrayList<String>();
        List<String> genericTypeList = new ArrayList<String>();
        List<String> applicationElementList = new ArrayList<String>();
        applicationElementList.add("application"); // Liberty 8.5 uses it with type attribute.
        for (IConfigurationElement ce : cf) {
            try {
                ServerExtensionWrapper sew = new ServerExtensionWrapper(ce);
                list.add(sew);
                for (String s : sew.applicationTypes) {
                    if (s != null)
                        appTypeList.add(s);
                }
                for (String s : sew.applicationElements) {
                    if (s != null)
                        applicationElementList.add(s);
                }
                for (String s : sew.genericModuleTypes) {
                    genericTypeList.add(s);
                }

                if (Trace.ENABLED)
                    Trace.trace(Trace.EXTENSION_POINT, "  Loaded serverExtension: " + ce.getAttribute("id"));
            } catch (Throwable t) {
                Trace.logError("Could not load serverExtension: " + ce.getAttribute("id"), t);
            }
        }

        if (allApplicationTypes == null)
            allApplicationTypes = appTypeList.toArray(new String[appTypeList.size()]);

        if (allApplicationElements == null)
            allApplicationElements = applicationElementList.toArray(new String[applicationElementList.size()]);

        if (allGenericModuleTypes == null)
            allGenericModuleTypes = genericTypeList.toArray(new String[genericTypeList.size()]);

        if (Trace.ENABLED)
            Trace.trace(Trace.EXTENSION_POINT, "-<- Done loading .serverExtensions extension point -<-");

        return list.toArray(new ServerExtensionWrapper[list.size()]);
    }

    public static boolean isValidApplicationType(String appType) {
        if (appType == null)
            return false;
        for (String s : allApplicationTypes) {
            if (s.equals(appType))
                return true;
        }
        return false;
    }

    public ServerExtensionWrapper(IConfigurationElement element) {
        this.configElement = element;

        IConfigurationElement[] ce = element.getChildren(MODULE);
        int size = ce.length;
        moduleTypes = new String[size];
        applicationTypes = new String[size];
        applicationElements = new String[size];
        ArrayList<String> genericModuleList = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            moduleTypes[i] = ce[i].getAttribute(TYPE);
            applicationTypes[i] = ce[i].getAttribute(APPLICATION_TYPE);
            if (applicationTypes[i] != null && applicationTypes[i].equals(GENERIC_APP_TYPE))
                genericModuleList.add(moduleTypes[i]);
            applicationElements[i] = ce[i].getAttribute(APPLICATION_ELEMENT);
            if (applicationElements[i] != null)
                applicationElementToTypeMap.put(applicationElements[i], applicationTypes[i]);
        }
        genericModuleTypes = new String[genericModuleList.size()];
        genericModuleList.toArray(genericModuleTypes);
    }

    public final boolean supportsApplicationType(IModuleType type) {
        if (type == null)
            return false;
        String typeId = type.getId();
        for (int i = 0; i < applicationTypes.length; i++) {
            if (applicationTypes[i] != null && !applicationTypes[i].isEmpty()) {
                if (typeId.equals(moduleTypes[i]))
                    return true;
            }
        }
        return false;
    }

    public final static boolean isGenericApplicationType(IModuleType type) {
        if (type == null)
            return false;
        String id = type.getId();
        for (String s : allGenericModuleTypes) {
            if (s.equals(id))
                return true;
        }
        return false;
    }

    protected String[] getRootModuleTypes() {
        // count elements first to avoid use of a list
        int size = moduleTypes.length;
        int count = 0;
        for (int i = 0; i < size; i++)
            if (applicationTypes[i] != null)
                count++;

        String[] roots = new String[count];
        count = 0;
        for (int i = 0; i < size; i++)
            if (applicationTypes[i] != null)
                roots[count++] = moduleTypes[i];

        return roots;
    }

    /**
     * Returns <code>true</code> if the module type is supported by this extension, and
     * <code>false</code> otherwise.
     *
     * @param type
     * @return <code>true</code> if the module type is supported by this extension, and
     *         <code>false</code> otherwise
     */
    public final boolean supports(IModuleType type) {
        for (String s : moduleTypes) {
            if (s.equals(type.getId()))
                return true;
        }
        return false;
    }

    /**
     * Init method, called to pass in the current context.
     *
     * @param server
     */
    protected void initServer(WebSphereServer server) {
        this.server = server;
    }

    protected WebSphereServerInfo getWebSphereServerInfo() {
        return server.getServerInfo();
    }

    protected WebSphereServer getWebSphereServer() {
        return server;
    }

    /**
     * Init method, called to pass in the current context.
     *
     * @param servBehaviour
     */
    protected void initServerBehaviour(WebSphereServerBehaviour servBehaviour) {
        this.servBehaviour = servBehaviour;
    }

    protected WebSphereServerBehaviour getWebSphereServerBehaviour() {
        return servBehaviour;
    }

    private ServerExtension getDelegate() {
        if (delegate == null) {
            try {
                delegate = (ServerExtension) configElement.createExecutableExtension("class");
                delegate.init(this);
            } catch (Throwable t) {
                Trace.logError("Could not create delegate", t);
                delegate = new ServerExtension() {
                    @Override
                    public IModule[] getRootModules(IModule module) throws CoreException {
                        return null;
                    }

                    @Override
                    public IModule[] getChildModules(IModule[] module) {
                        return null;
                    }
                };
            }
        }
        return delegate;
    }

    public ApplicationPublisher getPublishDelegate() {
        if (publishDelegate == null) {
            try {
                publishDelegate = (ApplicationPublisher) configElement.createExecutableExtension("publishClass");
                publishDelegate.init(this);
            } catch (Throwable t) {
                Trace.logError("Could not create delegate", t);
                publishDelegate = new ApplicationPublisher() {
                    @Override
                    public IStatus publishModule(int kind, PublishUnit module, IProgressMonitor monitor) {
                        return null;
                    }

                    @Override
                    protected String getModuleDeployName(IModule module) {
                        return null;
                    }
                };
            }
        }
        return publishDelegate;
    }

    public boolean isPublishRequired(IModule[] modules, IResourceDelta delta) {
        try {
            return getDelegate().isPublishRequired(modules, delta);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return true;
        }
    }

    public IModule[] getChildModules(IModule[] module) {
        try {
            return getDelegate().getChildModules(module);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return null;
        }
    }

    public IModule[] getRootModules(IModule module) throws CoreException {
        try {
            return getDelegate().getRootModules(module);
        } catch (CoreException ce) {
            throw ce;
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return null;
        }
    }

    public IStatus canAddModule(IModule module) {
        try {
            return getDelegate().canAddModule(module);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, null, t);
        }
    }

    public boolean modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) {
        try {
            return getDelegate().modifyModules(add, remove, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return false;
        }
    }

    public URL getModuleRootURL(IModule module) {
        try {
            return getDelegate().getModuleRootURL(module);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return null;
        }
    }

    public IStatus publishModule(int kind, PublishUnit module, IProgressMonitor monitor) {
        try {
            return getPublishDelegate().publishModule(kind, module, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.internalErrorPublishing, t);
        }
    }

    public void prePublishApplication(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        try {
            getPublishDelegate().prePublishApplication(kind, unit, status, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.internalErrorPublishing, t));
        }
    }

    public void postPublishApplication(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        try {
            getPublishDelegate().postPublishApplication(kind, unit, status, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.internalErrorPublishing, t));
        }
    }

    public void publishModuleAndChildren(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        try {
            getPublishDelegate().publishModuleAndChildren(kind, unit, status, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.internalErrorPublishing, t));
        }
    }

    public boolean needToActOnLooseConfigModeChange(PublishUnit pu) {
        try {
            return getPublishDelegate().needToActOnLooseConfigModeChange(pu);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return false;
        }
    }

    /**
     * Sets the isLooseConfig flag for the publish delegate.
     * This differs from the isLooseConfigSupported value.
     * In some cases a server extension may not support loose config
     * and the isLooseConfig value may be false regardless of setting
     * this flag.
     */
    public void setIsLooseConfig(boolean isLooseConfig) {
        try {
            getPublishDelegate().setIsLooseConfig(isLooseConfig);
        } catch (Throwable t) {
            Trace.logError("Error while trying to set loose config setting.", t);
        }
    }

    public void handleLooseConfigModeChange(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        try {
            getPublishDelegate().handleLooseConfigModeChange(kind, unit, status, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.internalErrorPublishing, t));
        }
    }

    public boolean requireConsoleOutputBeforePublishComplete(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        try {
            return getPublishDelegate().requireConsoleOutputBeforePublishComplete(kind, unit, status, monitor);
        } catch (Throwable t) {
            Trace.logError("Error calling server extension", t);
            return false;
        }
    }

    public boolean canRestartModule(IModule[] module) {
        if (module == null || module.length == 0)
            return false;
        try {
            return getDelegate().canRestartModule(module);
        } catch (Throwable t) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Exception in canRestartModule() for " + module[0].getName(), t);
            }
        }
        return false;
    }

    public OutOfSyncModuleInfo checkModuleConfigOutOfSync(IModule module) {
        try {
            return getDelegate().checkModuleConfigOutOfSync(module);
        } catch (Throwable t) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Exception in checkModuleConfigOutOfSync() for " + module.getName(), t);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ServerExtensionWrapper [" + getClass().toString() + "]";
    }

    /**
     * @return all application elements
     */
    public static String[] getAllApplicationElements() {
        if (allApplicationElements == null)
            createServerExtensions();

        String[] r = new String[allApplicationElements.length];
        System.arraycopy(allApplicationElements, 0, r, 0, allApplicationElements.length);
        return r;
    }

    public static String getAppTypeFromAppElement(String label) {
        return applicationElementToTypeMap.get(label);
    }

    /**
     * @return the jmxConnection
     */
    public JMXConnection getJmxConnection() {
        return jmxConnection;
    }

    /**
     * @param jmxConnection the jmxConnection to set
     */
    protected void setJmxConnection(JMXConnection jmxConnection) {
        this.jmxConnection = jmxConnection;
    }
}