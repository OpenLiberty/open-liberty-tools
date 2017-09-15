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
package com.ibm.ws.st.jee.core.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jst.j2ee.componentcore.util.EARArtifactEdit;
import org.eclipse.jst.j2ee.internal.deployables.J2EEFlexProjDeployable;
import org.eclipse.jst.server.core.IEnterpriseApplication;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.J2EEUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModule2;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.OutOfSyncModuleInfo;
import com.ibm.ws.st.core.internal.ServerExtension;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;

@SuppressWarnings("restriction")
public class JEEServerExtension extends ServerExtension {
    private static final String JST_WEB = "jst.web";
    private static final String JST_WEBFRAGMENT = "jst.webfragment";
    private static final String JST_EJB = "jst.ejb";
    private static final String JST_EAR = "jst.ear";
    private static final String JST_UTILITY = "jst.utility";
    private static final String JST_JCA = "jst.connector";
    private static final String JST_APPCLIENT = "jst.appclient";
    private static final String WAR_EXTENSION = ".war";
    private static final String EJB_EXTENSION = ".jar";
    private static final String EAR_EXTENSION = ".ear";
    private static final String RAR_EXTENSION = ".rar";
    private static final String EAR_APPLICATION = "enterpriseApplication";
    private static final String WEB_APPLICATION = "webApplication";
    private static final String EJB_APPLICATION = "ejbApplication";
    private static final String RAR_APPLICATION = "resourceAdapter";

    private static final String[] staticExtension = new String[] { "bmp", "cab", "css", "doc", "exe", "gif", "htm", "html", "ico", "ini",
                                                                   "jhtml", "jpeg", "jpg", "js", "jsp", "jspf", "jspx", "jpg", "pdf", "png",
                                                                   "properties", "swf", "tif", "tiff", "ttf", "txt", "xhtml", "xls", "zip",
                                                                   "java" };

    private static final String EXT_CLASS = "class";

    protected static String getModuleDeployName(IModule module) {
        String moduleTypeId = module.getModuleType().getId();
        String s = null;
        IModule2 module2 = (IModule2) module.getAdapter(IModule2.class);
        if (module2 != null)
            s = module2.getProperty(IModule2.PROP_DEPLOY_NAME);
        if (s == null || s.isEmpty())
            s = module.getName();
        if (JST_WEB.equals(moduleTypeId)) {
            if (!s.endsWith(WAR_EXTENSION))
                s += WAR_EXTENSION;
        } else if (JST_EAR.equals(moduleTypeId)) {
            if (!s.endsWith(EAR_EXTENSION))
                s += EAR_EXTENSION;
        } else if (JST_EJB.equals(moduleTypeId)) {
            if (!s.endsWith(EJB_EXTENSION))
                s += EJB_EXTENSION;
        } else if (JST_JCA.equals(moduleTypeId)) {
            if (!s.endsWith(RAR_EXTENSION))
                s += RAR_EXTENSION;
        }
        return s;
    }

    @Override
    public IStatus canAddModule(IModule module) {
        String moduleTypeId = module.getModuleType().getId();
        boolean isEAR = JST_EAR.equals(moduleTypeId);
        boolean isWeb = JST_WEB.equals(moduleTypeId);
        boolean isEJB = JST_EJB.equals(moduleTypeId);
        boolean isRAR = JST_JCA.equals(moduleTypeId);

        if (!isEAR && !isWeb && !isEJB && !isRAR)
            return null;

        // check version
        try {
            String version = module.getModuleType().getVersion();
            if (isWeb || isEJB || isRAR)
                // Version checking is covered by the org.eclipse.wst.common.project.facet.core.runtimes
                // extension point entries
                return checkPublishedModule(module);
            else if (isEAR && getWebSphereServer().getWebSphereRuntime().isEARSupported(version)) {
                IEnterpriseApplication ear = (IEnterpriseApplication) module.loadAdapter(IEnterpriseApplication.class, null);
                IModule[] children = null;
                try {
                    children = ear.getModules();
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.logError("Failed to get ear moduless", e);
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorGettingModulesInEAR, null);
                }
                boolean hasAModule = false;
                if (children != null) {
                    for (IModule child : children) {
                        String id = child.getModuleType().getId();

                        // EAR must contain at least one of these JEE Modules
                        if (JST_WEB.equals(id) || JST_EJB.equals(id) || JST_JCA.equals(id) || JST_APPCLIENT.equals(id)) {
                            hasAModule = true;
                        } else if (!JST_WEBFRAGMENT.equals(id) && !JST_UTILITY.equals(id)) {
                            // If it doesn't match any of these types then it's not a supported module
                            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorNotSupportedModuleInEAR, null);
                        }
                    }
                }
                if (!hasAModule)
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorEARMissingRequiredModules, null);

                return checkPublishedModule(module);
            }
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Module version was not a number", e);
        }
        if (isEAR)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorEARSpecLevel, null);
        if (isEJB)
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorEJBSpecLevel, null);
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorWebSpecLevel, null);
    }

    IStatus checkPublishedModule(IModule module) {
        if (getWebSphereServer().isExternalAppOnServer(module.getName()))
            return new Status(IStatus.WARNING, Activator.PLUGIN_ID, NLS.bind(Messages.warningAppIsOnServer, module.getName()));
        return Status.OK_STATUS;
    }

    @Override
    public IModule[] getChildModules(IModule[] module) {
        IModuleType moduleType = module[module.length - 1].getModuleType();

        if (moduleType != null && JST_WEB.equals(moduleType.getId())) {
            IWebModule webModule = (IWebModule) module[module.length - 1].loadAdapter(IWebModule.class, null);
            if (webModule != null)
                return webModule.getModules();
        } else if (moduleType != null && JST_EAR.equals(moduleType.getId())) {
            IEnterpriseApplication earModule = (IEnterpriseApplication) module[0].loadAdapter(IEnterpriseApplication.class, null);
            if (earModule != null)
                return earModule.getModules();
        }
        return new IModule[0];
    }

    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        String moduleTypeId = module.getModuleType().getId();
        if (JST_EAR.equals(moduleTypeId)) {
            IStatus status = canAddModule(module);
            if (status == null || !status.isOK())
                throw new CoreException(status);
            return new IModule[] { module };
        } else if (JST_WEB.equals(moduleTypeId)) {
            ArrayList<IModule> modules = getWebRootModules(module);
            return modules.toArray(new IModule[modules.size()]);
        } else if (JST_EJB.equals(moduleTypeId)) {
            ArrayList<IModule> modules = getEJBRootModules(module);
            return modules.toArray(new IModule[modules.size()]);
        } else if (JST_WEBFRAGMENT.equals(moduleTypeId)) {
            HashSet<IModule> modules = new HashSet<IModule>();
            IModule[] ms = J2EEUtil.getWebModules(module, null);
            for (IModule m : ms) {
                modules.addAll(getWebRootModules(m));
            }
            return modules.toArray(new IModule[modules.size()]);
        } else if (JST_JCA.equals(moduleTypeId)) {
            ArrayList<IModule> modules = new ArrayList<IModule>();
            IStatus status = canAddModule(module);
            if (status == null || status.getSeverity() == IStatus.ERROR)
                throw new CoreException(status);
            IModule[] ms = J2EEUtil.getEnterpriseApplications(module, null);
            for (IModule m : ms)
                modules.add(m);
            IJ2EEModule jeeModule = (IJ2EEModule) module.loadAdapter(IJ2EEModule.class, null);
            if (jeeModule != null && !jeeModule.isBinary())
                modules.add(module);
            return modules.toArray(new IModule[modules.size()]);
        }

        //should be utility module - need to return possible web and EAR
        HashSet<IModule> modules = new HashSet<IModule>();
        IModule[] ms = J2EEUtil.getWebModules(module, null);
        for (IModule m : ms)
            modules.add(m);

        ms = J2EEUtil.getEnterpriseApplications(module, null);
        for (IModule m : ms)
            modules.add(m);

        return modules.toArray(new IModule[modules.size()]);
    }

    @Override
    public boolean modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) {
        ConfigurationFile config = getWebSphereServer().getConfiguration();
        Application[] appsInConfig = config.getApplications();
        for (IModule module : add) {
            String moduleTypeId = module.getModuleType().getId();
            if (JST_WEB.equals(moduleTypeId)) {
                handleExistingAppEntryInConfigAndDropins(module, appsInConfig);
                IWebModule webModule = (IWebModule) module.loadAdapter(IWebModule.class, monitor);
                String contextRoot = null;
                if (webModule != null) {
                    String cn = webModule.getContextRoot();
                    if (!module.getName().equals(cn))
                        contextRoot = cn;
                }

                Map<String, String> attributes = null;
                if (contextRoot != null) {
                    attributes = new HashMap<String, String>();
                    WebSphereServerInfo serverInfo = getWebSphereServerInfo();
                    if (serverInfo != null && serverInfo.getSchemaHelper().isSupportedApplicationElement(WEB_APPLICATION)) {
                        attributes.put("contextRoot", contextRoot); //TODO need to verify the key when it is available on runtime
                    } else {
                        attributes.put("context-root", contextRoot);
                    }
                }

                if (shouldExcludeAddingModuleToConfig(module)) {
                    continue;
                }

                if (SharedLibertyUtils.isWebRefSharedLibrary(module)) {
                    SharedLibRefInfo settings = SharedLibertyUtils.getSharedLibRefInfo(module.getProject());

                    config.addApplication(module.getName(), WEB_APPLICATION, getModuleDeployName(module), attributes, settings.getLibRefIds(),
                                          APIVisibility.getAPIVisibilityFromProperties(settings));
                } else
                    config.addApplication(module.getName(), WEB_APPLICATION, getModuleDeployName(module), attributes, null, APIVisibility.getDefaults());
            } else if (JST_EJB.equals(moduleTypeId)) {
                handleExistingAppEntryInConfigAndDropins(module, appsInConfig);
                if (SharedLibertyUtils.isEJBRefSharedLibrary(module)) {
                    SharedLibRefInfo settings = SharedLibertyUtils.getSharedLibRefInfo(module.getProject());
                    config.addApplication(module.getName(), EJB_APPLICATION, getModuleDeployName(module), null, settings.getLibRefIds(),
                                          APIVisibility.getAPIVisibilityFromProperties(settings));
                } else
                    config.addApplication(module.getName(), EJB_APPLICATION, getModuleDeployName(module), null, null, APIVisibility.getDefaults());
            } else if (JST_EAR.equals(moduleTypeId)) {
                handleExistingAppEntryInConfigAndDropins(module, appsInConfig);
                if (SharedLibertyUtils.isEARRefSharedLibrary(module)) {
                    SharedLibRefInfo settings = SharedLibertyUtils.getSharedLibRefInfo(module.getProject());
                    config.addApplication(module.getName(), EAR_APPLICATION, getModuleDeployName(module), null, settings.getLibRefIds(),
                                          APIVisibility.getAPIVisibilityFromProperties(settings));
                } else {
                    config.addApplication(module.getName(), EAR_APPLICATION, getModuleDeployName(module), null, null, APIVisibility.getDefaults());
                }
            } else if (JST_JCA.equals(moduleTypeId)) {
                handleExistingAppEntryInConfigAndDropins(module, appsInConfig);
                config.addApplication(module.getName(), RAR_APPLICATION, getModuleDeployName(module), null, null, APIVisibility.getDefaults());
            }
        }
        for (IModule module : remove) {
            config.removeApplication(module.getName());
        }

        return true;
    }

    /**
     * Workaround for NPE in J2EEFlexProjDeploy; we check if the earModule project has an application deployment descriptor
     * before calling it, in order to prevent NPE.
     */
    private static boolean earModuleHasDD(IModule earModule) {
        if (earModule == null || earModule.getProject() == null) {
            return false;
        }

        EARArtifactEdit edit = null;
        try {
            edit = EARArtifactEdit.getEARArtifactEditForRead(earModule.getProject());
            if (edit.getApplication() == null) {
                return false;
            }
        } finally {
            if (edit != null) {
                edit.dispose();
            }
        }
        return true;
    }

    /**
     * Get the context root from an EAR module. This method is required due to Eclipse bugzilla bug 435439, which is caused
     * by an NPE when calling wm.getContextRoot(...) with a web module that doesn't contain an application.xml. This
     * issue only occurs with J2EEFlexProjDeployable.
     *
     * This method introduced as part of fix WASRTC 116410.
     */
    private static String getContextRootFromEARWebModule(IModule earModule, IModule webModule) {
        String contextRoot = null;

        // In the EAR case, pull the context root from the EAR's webmodule API
        IWebModule wm = getWebModule(webModule);

        // In J2EEFlexProjDeployable case, only call getContextRoot(...) when we know our earModule has a DD (otherwise it will NPE)
        if (wm != null && (!(wm instanceof J2EEFlexProjDeployable) || (wm instanceof J2EEFlexProjDeployable && earModuleHasDD(earModule)))) {
            contextRoot = wm.getContextRoot(earModule);
        } else {

            // J2EEFlexProjDeploy throws an NPE when calling getContextRoot on a project without a deployment descriptor
            // This branch handles the J2EEFlexProjDeploy case, where there is no deployment descriptor.

            contextRoot = DeploymentDescriptorHelper.getWebContextRootFromEAR(DeploymentDescriptorHelper.getComponentRoot(earModule.getProject()),
                                                                              getModuleDeployName(webModule));
        }

        return contextRoot;

    }

    @Override
    public URL getModuleRootURL(final IModule module) {
        IModule webModule = module;
        try {
            WebSphereServer serv = getWebSphereServer();
            ConfigurationFile configFile = serv.getConfiguration();

            IServer server = getServer();
            if (server.isWorkingCopy()) {
                IServer orig = ((IServerWorkingCopy) server).getOriginal();
                if (orig != null)
                    server = orig;
            }
            IModule[] modules = server.getModules();
            if (modules.length == 0) {
                return null;
            }

            boolean found = false;
            IModule earModule = null;
            FIND_MODULE: for (IModule m : modules) {
                if (m.equals(webModule)) {
                    found = true;
                    break;
                } else if (JST_WEB.equals(m.getModuleType().getId())) { // look for web fragment
                    if (isChildOfWeb(m, webModule)) {
                        found = true;
                        webModule = m;
                        break FIND_MODULE;
                    }
                } else if (JST_EAR.equals(m.getModuleType().getId())) {
                    IEnterpriseApplication ear = (IEnterpriseApplication) m.loadAdapter(IEnterpriseApplication.class, null);
                    if (ear != null) {
                        IModule[] childModules = ear.getModules();
                        for (IModule m2 : childModules) {
                            if (webModule == m2) {
                                found = true;
                                earModule = m;
                                break FIND_MODULE;
                            } else if (JST_WEB.equals(m2.getModuleType().getId()) && isChildOfWeb(m2, webModule)) { // for web fragment
                                found = true;
                                earModule = m;
                                webModule = m2;
                                break FIND_MODULE;
                            }
                        }
                    }
                }
            }

            if (!found) {
                return null;
            }

            String contextRoot = null;

            if (earModule != null) {

                // This method call is required for WTP builds that do not contain the fix for
                // bugzilla 435439. At present (May 2014), this is both Kepler and Luna WTP builds.
                contextRoot = getContextRootFromEARWebModule(earModule, webModule);

                // When these builds are no longer supported, the following two lines may
                // replace the above line.

//                IWebModule wm = getWebModule(webModule);
//                contextRoot = wm.getContextRoot(earModule);

            } else {
                // In the non EAR case, first check the server config file
                if (configFile != null) {
                    Element ele = configFile.getApplicationElement(WEB_APPLICATION, webModule.getName());
                    if (ele != null) {
                        WebSphereServerInfo serverInfo = getWebSphereServerInfo();
                        if (serverInfo != null && serverInfo.getSchemaHelper().isSupportedApplicationElement(WEB_APPLICATION)) {
                            contextRoot = ele.getAttribute("contextRoot");
                        } else {
                            contextRoot = ele.getAttribute("context-root");
                        }
                        if (contextRoot.isEmpty())
                            contextRoot = null;
                    }
                }
            }

            if (contextRoot == null) {
                // Returns the context root from ibm-web-ext.xml file (XML, not XMI)
                contextRoot = DeploymentDescriptorHelper.getContextRootFromExtXml(DeploymentDescriptorHelper.getComponentRoot(webModule.getProject()));
            }

            if (contextRoot == null) {
                // Returns the context root from ibm-web-ext.xmi file (XMI, not XML)
                contextRoot = DeploymentDescriptorHelper.getContextRootFromExtXmi(DeploymentDescriptorHelper.getComponentRoot(webModule.getProject()));
            }

            if (contextRoot == null) {
                // Returns the context from the web module, in the EAR case we try to get the context root using the ear module, this is
                // directly from the web module
                IWebModule wm = getWebModule(webModule);
                if (wm != null) {
                    contextRoot = wm.getContextRoot();
                }
            }

            if (contextRoot == null) {
                contextRoot = webModule.getName();
            }

            String url = getServerBaseWebURL();

            if (contextRoot.startsWith("/"))
                url += contextRoot;
            else
                url += "/" + contextRoot;

            if (!url.endsWith("/"))
                url += "/";

            return new URL(url);
        } catch (Exception e) {
            Trace.logError("Could not get root URL for module: " + module.getName(), e);
            return null;
        }
    }

    @Override
    public boolean isPublishRequired(IModule[] modules, IResourceDelta delta) {
        if (modules.length == 0)
            return false;

        WebSphereServerBehaviour behaviour = (WebSphereServerBehaviour) getServer().loadAdapter(WebSphereServerBehaviour.class, null);
        if (behaviour == null)
            return true;

        boolean inPublishedModules = false;
        for (IModule module : modules) {
            List<IModule[]> modulesOnServer = behaviour.getPublishedModules();
            for (IModule[] pModule : modulesOnServer) {
                String id = pModule[0].getModuleType().getId();
                if ((JST_EAR.equals(id) || JST_WEB.equals(id) || JST_EJB.equals(id) || JST_UTILITY.equals(id) || JST_JCA.equals(id))
                    && module.equals(pModule[pModule.length - 1])) {
                    inPublishedModules = true;
                    break;
                }
            }
            if (inPublishedModules)
                break;
        }

        if (!inPublishedModules)
            return false;

        //check loose config
        boolean looseConfig = getWebSphereServer().getServer().getAttribute(WebSphereServer.PROP_LOOSE_CONFIG, false);
        if (!looseConfig)
            return true;

        // checks if this delta resource contains ".../.settings/..."
        // .settings and .apt_generated do not need a publish, regardless, but changes in component is needed
        if ((delta.getFullPath().segmentCount() >= 2)) {
            IPath path = delta.getFullPath();
            if (path.segment(1).equalsIgnoreCase(".settings")) {
                if (delta.getFullPath().segmentCount() >= 3) {
                    if (path.segment(2).equalsIgnoreCase("org.eclipse.wst.common.component") == false) {
                        return false;
                    }
                    return true;
                }
                return false;
            } else if (path.segment(1).equalsIgnoreCase(".apt_generated")) {
                return false;
            }
        }

        String extension = delta.getResource().getFileExtension();
        if (extension == null)
            return true;

        if (matchExtension(extension))
            return false;

        // changes to class files in debug mode are handled by hot method replace. Therefore we do not trigger a publish .
        // It is a known limitation that changes to method annotations are not handled by hot method replace.
        if (EXT_CLASS.equalsIgnoreCase(extension) && ILaunchManager.DEBUG_MODE.equals(getWebSphereServer().getServer().getMode()))
            return false;

        return true;
    }

    @Override
    public OutOfSyncModuleInfo checkModuleConfigOutOfSync(IModule module) {
        OutOfSyncModuleInfo info = super.checkModuleConfigOutOfSync(module);
        if (info != null)
            return info;

        final List<String> addRefIds = new ArrayList<String>(2);
        final List<String> removeRefIds = new ArrayList<String>(2);
        boolean apiVisibilityMismatch = false;
        String apiVisibility = null;
        if (isSharedLibUsed(module)) {
            final ConfigurationFile configFile = getConfiguration();
            Application[] apps = configFile.getApplications();
            Application matchedApp = null;
            for (Application app : apps) {
                if (app.getName().equals(module.getName())) {
                    matchedApp = app;
                    break;
                }
            }

            // Should not happen, because it would have been caught by
            // the call to super.checkModuleConfigOutOfSync.
            if (matchedApp == null) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not find a matching application " + module.getName(), null);
                return null;
            }

            SharedLibRefInfo settings = SharedLibertyUtils.getSharedLibRefInfo(module.getProject());
            List<String> moduleLibRefIds = settings.getLibRefIds();
            String[] appLibRefIds = matchedApp.getSharedLibRefs();
            List<String> configSharedLibIds = idArrayToList(configFile.getSharedLibraryIds());

            EnumSet<APIVisibility> matchedAppAPIVisibility = matchedApp.getAPIVisibility();
            EnumSet<APIVisibility> settingsAPIVisibility = APIVisibility.getAPIVisibilityFromProperties(settings);

            apiVisibilityMismatch = !matchedAppAPIVisibility.equals(settingsAPIVisibility);

            if (apiVisibilityMismatch) {
                apiVisibility = APIVisibility.generateAttributeValue(settingsAPIVisibility);
            }

            if (appLibRefIds == null || appLibRefIds.length == 0) {
                // nothing to do
                if (moduleLibRefIds.isEmpty())
                    return null;

                addRefIds.addAll(moduleLibRefIds);
            } else {
                addRefIds.addAll(moduleLibRefIds);
                for (String id : appLibRefIds) {
                    if (moduleLibRefIds.contains(id))
                        addRefIds.remove(id);
                    else if (!configSharedLibIds.contains(id))
                        removeRefIds.add(id);
                }
            }
        }

        if (addRefIds.isEmpty() && removeRefIds.isEmpty() && !apiVisibilityMismatch)
            return null;

        return new OutOfSyncModuleLibRefInfo(idListToString(addRefIds), idListToString(removeRefIds), apiVisibility);
    }

    private List<String> idArrayToList(String[] ids) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<String> idList = new ArrayList<String>(ids.length);
        for (String id : ids) {
            idList.add(id);
        }

        return idList;
    }

    private boolean isSharedLibUsed(IModule module) {
        String moduleTypeId = module.getModuleType().getId();

        if (JST_WEB.equals(moduleTypeId))
            return SharedLibertyUtils.isWebRefSharedLibrary(module);

        if (JST_EAR.equals(moduleTypeId))
            return SharedLibertyUtils.isEARRefSharedLibrary(module);

        return false;
    }

    private String idListToString(List<String> idList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String id : idList) {
            if (id.trim().isEmpty())
                continue;
            if (first) {
                sb.append(id);
                first = false;
            } else
                sb.append(',').append(id);
        }

        return sb.toString();
    }

    private boolean matchExtension(String inS) {
        for (String s : staticExtension) {
            if (s.equalsIgnoreCase(inS))
                return true;
        }
        return false;
    }

    private ArrayList<IModule> getWebRootModules(IModule webModule) throws CoreException {
        ArrayList<IModule> roots = new ArrayList<IModule>();
        IStatus status = canAddModule(webModule);
        if (status == null || status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);
        IModule[] ms = J2EEUtil.getEnterpriseApplications(webModule, null);
        for (IModule m : ms)
            roots.add(m);
        IJ2EEModule jeeModule = (IJ2EEModule) webModule.loadAdapter(IJ2EEModule.class, null);
        if (jeeModule != null && !jeeModule.isBinary())
            roots.add(webModule);
        return roots;
    }

    private ArrayList<IModule> getEJBRootModules(IModule ejbModule) throws CoreException {
        IStatus status = canAddModule(ejbModule);
        if (status == null || status.getSeverity() == IStatus.ERROR)
            throw new CoreException(status);

        ArrayList<IModule> roots = new ArrayList<IModule>();
        IModule[] ms = J2EEUtil.getEnterpriseApplications(ejbModule, null);
        for (IModule m : ms)
            roots.add(m);

        // check schema if standalone EJBs are supported
        WebSphereServerInfo serverInfo = getWebSphereServerInfo(); // WebSphereServerInfo can be null during server creation
        if (serverInfo != null && serverInfo.getSchemaHelper().isSupportedApplicationElement(EJB_APPLICATION)) {
            IJ2EEModule jeeModule = (IJ2EEModule) ejbModule.loadAdapter(IJ2EEModule.class, null);
            if (jeeModule != null && !jeeModule.isBinary())
                roots.add(ejbModule);
        }
        return roots;
    }

    private boolean isChildOfWeb(IModule webModule, IModule child) {
        IWebModule web = (IWebModule) webModule.loadAdapter(IWebModule.class, null);
        if (web != null) {
            IModule[] children = web.getModules();
            for (IModule m : children) {
                if (m.equals(child))
                    return true;
            }
        }
        return false;
    }

    private static IWebModule getWebModule(IModule module) {
        return isWebModule(module) ? (IWebModule) module.loadAdapter(IWebModule.class, null) : null;
    }

    private static boolean isWebModule(IModule module) {
        if (module == null) {
            return false;
        }
        return JST_WEB.equals(module.getModuleType().getId());
    }

    static class OutOfSyncModuleLibRefInfo extends OutOfSyncModuleInfo {
        private final String addIds;
        private final String removeIds;
        private final String apiVisibility;

        OutOfSyncModuleLibRefInfo(String addIds, String removeIds, String apiVisibility) {
            super(Type.SHARED_LIB_REF_MISMATCH);
            this.addIds = addIds;
            this.removeIds = removeIds;
            this.apiVisibility = apiVisibility;
        }

        @Override
        public String getPropertyValue(Property key) {
            if (key == Property.LIB_REF_IDS_ADD)
                return addIds;

            if (key == Property.LIB_REF_IDS_REMOVE)
                return removeIds;

            if (key == Property.LIB_REF_API_VISIBILITY)
                return apiVisibility;

            return super.getPropertyValue(key);
        }
    }
}