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
package com.ibm.ws.st.jee.core.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IEnterpriseApplication;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.DeletedModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.ibm.ws.st.core.internal.ApplicationPublisher;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.PublishUnit;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.looseconfig.DeploymentAssemblyUtil;

@SuppressWarnings("restriction")
public class JEEPublisher extends ApplicationPublisher {
    private static final String JST_WEB = "jst.web";
    private static final String JST_EAR = "jst.ear";
    private static final String JST_UTILITY = "jst.utility";
    private static final String JST_WEBFRAGMENT = "jst.webfragment";
    private static final String JST_EJB = "jst.ejb";
    private static final String JST_JCA = "jst.connector";
    private static final String JST_APPCLIENT = "jst.appclient";

    private Properties moduleURICachedMap = null;
    private static String MODULE_URI_CACHE_FILE_NAME = "jeeModuleUri.map";

    private void loadModuleURICache() {
        if (moduleURICachedMap == null)
            moduleURICachedMap = new Properties();
        IPath filePath = getWebSphereServerBehaviour().getTempDirectory().append(MODULE_URI_CACHE_FILE_NAME);
        FileUtil.loadProperties(moduleURICachedMap, filePath);
    }

    private IPath getDeployPath(IModule[] module, boolean shared) {
        WebSphereServerBehaviour webSphereServerBehaviour = getWebSphereServerBehaviour();
        IPath path = webSphereServerBehaviour.getRootPublishFolder(shared);

        // If loose config, allow extension to override the deploy path.  The resource could be on a mounted volume.
        if (isLooseConfig()) {
            IPath overridePath = getAppsPathOverride();
            if (overridePath != null) {
                path = overridePath;
            }
        }

        if (module.length == 0) // this should not happen, just in case
            return path;
        IJ2EEModule jeeModule = (IJ2EEModule) module[module.length - 1].loadAdapter(IJ2EEModule.class, null);
        String moduleTypeId = module[module.length - 1].getModuleType().getId();
        if (module.length >= 2 && ((jeeModule != null && jeeModule.isBinary()) || JST_UTILITY.equals(moduleTypeId) || JST_WEBFRAGMENT.equals(moduleTypeId))) {
            String uri;
            uri = getModuleURI(module);

            IModule[] parentModule = new IModule[module.length - 1];
            for (int i = 0; i < parentModule.length; i++) {
                parentModule[i] = module[i];
            }
            IPath parentPath = getDeployPath(parentModule, false);

            if (uri == null) {
                // this is a deleted module, we need to get the uri from the cache
                if (moduleURICachedMap == null)
                    loadModuleURICache();
                uri = moduleURICachedMap.getProperty(FileUtil.genModuleURICacheKey(module));
            }
            if (uri == null) {
                // this is an error condition. So, use the module name instead.
                Trace.logError("Can't get the module uri of " + module[module.length - 1].getName(), null);
                uri = module[module.length - 1].getName();
            }
            path = parentPath.append(uri);
        } else if (module.length >= 2) {
            IModule[] parentModule = new IModule[module.length - 1];
            for (int i = 0; i < parentModule.length; i++) {
                parentModule[i] = module[i];
            }
            IPath parentPath = getDeployPath(parentModule, false);
            IProject parentProject = parentModule[parentModule.length - 1].getProject();
            String targetInArchive = DeploymentAssemblyUtil.getDeployPath(parentProject, module[module.length - 1].getName());
            if (targetInArchive != null)
                path = parentPath.append(targetInArchive);
            else
                path = parentPath.append(getModuleDeployName(module[module.length - 1]));
        } else {
            for (IModule m : module) {
                path = path.append(getModuleDeployName(m));
            }
        }

        return path;
    }

    /**
     * @param module
     * @param parent
     * @return
     */
    private String getModuleURI(IModule[] module) {
        if (module == null || module.length < 2)
            return null;
        if (JST_EAR.equals(module[0].getModuleType().getId())) {
            IEnterpriseApplication ear = (IEnterpriseApplication) module[module.length - 2].loadAdapter(IEnterpriseApplication.class, null);
            if (ear != null) // it is null if the child is a deleted one
                return ear.getURI(module[module.length - 1]);
        } else { // if (JST_WEB.equals(parent.getModuleType().getId())) {
            IWebModule web = (IWebModule) module[module.length - 2].loadAdapter(IWebModule.class, null);
            if (web != null)
                return web.getURI(module[module.length - 1]);
        }
        return null;
    }

    @Override
    protected IStatus publishModule(int kind, PublishUnit unit, IProgressMonitor monitor) {

        IModule[] module = unit.getModule();
        int size = module.length;
        List<IStatus> status = new ArrayList<IStatus>();

        if (ServerBehaviourDelegate.ADDED == unit.getDeltaKind()) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Module added: " + unit.getModuleName());
            cacheModuleURI(module);
        }
        if (!isLooseConfig()) {
            PublishHelper helper = new PublishHelper(getWebSphereServer().getWorkAreaTempPath().toFile());

            String moduleTypeId = module[size - 1].getModuleType().getId();
            IJ2EEModule jeeModule = (IJ2EEModule) module[size - 1].loadAdapter(IJ2EEModule.class, monitor);
            if (JST_EAR.equals(moduleTypeId)) {
                publishDir(kind, unit.getDeltaKind(), module, getDeployPath(module, false), helper, status, monitor);

            } else if (JST_WEB.equals(moduleTypeId) || JST_EJB.equals(moduleTypeId) || JST_WEBFRAGMENT.equals(moduleTypeId) || JST_JCA.equals(moduleTypeId)
                       || JST_APPCLIENT.equals(moduleTypeId)) {
                if (jeeModule != null) {
                    if (jeeModule.isBinary() || JST_WEBFRAGMENT.equals(moduleTypeId)) {
                        publishJar(kind, unit.getDeltaKind(), module, jeeModule.isBinary(), helper, status, monitor);
                    } else {
                        try {
                            publishDir(kind, unit.getDeltaKind(), module, getDeployPath(module, false), helper, status, monitor);
                        } catch (NullPointerException e) {
                            status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorPublishingModule, module[size - 1])));
                            e.printStackTrace();
                        }
                    }
                } else if (module[size - 1] instanceof DeletedModule) {
                    boolean skipDelete = false;
                    PublishUnit parent = unit.getParent();
                    if (parent != null) {
                        for (PublishUnit sibling : parent.getChildren()) {
                            if (sibling.getModule().length == size &&
                                sibling.getModule()[size - 1] != module[size - 1] &&
                                getModuleDeployName(sibling.getModule()[size - 1]).equals(getModuleDeployName(module[size - 1])) &&
                                sibling.getDeltaKind() != ServerBehaviourDelegate.REMOVED) {
                                skipDelete = true;
                                break;
                            }
                        }
                    }
                    if (!skipDelete)
                        deleteModuleResourcesFromServer(module, helper, status, monitor);
                }
            } else if (module.length > 1 && JST_UTILITY.equals(moduleTypeId)) { // We can't publish utility module alone
                if (jeeModule != null && jeeModule.isBinary())
                    publishJar(kind, unit.getDeltaKind(), module, true, helper, status, monitor);
                else
                    publishJar(kind, unit.getDeltaKind(), module, false, helper, status, monitor);
            }
        }

        int appChangeKind = getApplicationChangeKind(unit);
        if (appChangeKind != ServerBehaviourDelegate.ADDED &&
            appChangeKind != ServerBehaviourDelegate.REMOVED &&
            unit.getDeltaKind() != ServerBehaviourDelegate.NO_CHANGE) {
            computeModuleDelta(module, unit.getDeltaKind());
        }

        // Don't clean up the cache until the delta has been calculated
        if (ServerBehaviourDelegate.REMOVED == unit.getDeltaKind()) {
            removeCachedModuleURI(module);
        }

        return combineModulePublishStatus(status, module[size - 1].getName());
    }

    private void deleteModuleResourcesFromServer(IModule[] module, PublishHelper helper, List<IStatus> status, IProgressMonitor monitor) {
        IPath path = getDeployPath(module, false);

        File file = path.toFile();
        if (file.exists()) {
            if (file.isFile()) {
                if (!file.delete()) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorDeleteFile, path.toOSString())));
                    return;
                }
            } else {
                IStatus[] stat = PublishHelper.deleteDirectory(file, monitor);
                addArrayStatusToList(status, stat);
            }
        }
    }

    private void cacheModuleURI(IModule[] module) {
        if (module == null || module.length == 1) // EAR doesn't have URI.  don't cache
            return;
        if (moduleURICachedMap == null)
            loadModuleURICache();
        String key = FileUtil.genModuleURICacheKey(module);
        String uri = getModuleURI(module);
        if (uri != null)
            moduleURICachedMap.put(key, uri);
        else
            Trace.logError("Can't get the uri for module:" + key, null);
    }

    private void removeCachedModuleURI(IModule[] module) {
        if (module == null || module.length == 1) // EAR doesn't have URI.  don't cache
            return;
        if (moduleURICachedMap == null)
            loadModuleURICache();
        String key = FileUtil.genModuleURICacheKey(module);
        moduleURICachedMap.remove(key);
    }

    private void computeModuleDelta(IModule[] module, int deltaKind) {
        IPath deployPath = null;
        boolean isLooseConfig = isLooseConfig();

        if (deltaKind == ServerBehaviourDelegate.REMOVED && module.length != 1 && isLooseConfig)
            // the module entry is removed from the loose config xml. We don't need to anything to the sets
            return;
        if (!isLooseConfig)
            deployPath = getDeployPath(module, false);

        if (!isLooseConfig && deployPath != null) {
            // Add the base path of the module itself
            if (deltaKind == IModuleResourceDelta.ADDED)
                getAddedResourceList().add(deployPath.toOSString());
            else if (deltaKind == IModuleResourceDelta.REMOVED)
                getRemovedResourceList().add(deployPath.toOSString());
        }

        IModuleResourceDelta[] rDeltas = getWebSphereServerBehaviour().getPublishedResourceDelta(module);
        if (rDeltas != null && rDeltas.length != 0) {
            String moduleTypeId = module[module.length - 1].getModuleType().getId();
            if (!isLooseConfig && (JST_UTILITY.equals(moduleTypeId) || JST_WEBFRAGMENT.equals(moduleTypeId))) { // in non-loose case, utility and web fragment modules are published as jar
                getChangedResourceList().add(getDeployPath(module, false).toOSString());
            } else {
                if (!isLooseConfig) {
                    IJ2EEModule jeeModule = (IJ2EEModule) module[module.length - 1].loadAdapter(IJ2EEModule.class, null);
                    if (jeeModule != null && jeeModule.isBinary() && deployPath != null && deployPath.segmentCount() > 1)
                        deployPath = deployPath.removeLastSegments(1); // binary module is deployed as an archive
                }
                computeDeltaResources(deployPath, rDeltas);
            }
        }

    }

    private void publishJar(int kind, int deltaKind, IModule[] module, boolean isBinary, PublishHelper helper, List<IStatus> status, IProgressMonitor monitor) {
        IPath path = getDeployPath(module, false);

        // remove if requested or if previously published and are now serving without publishing
        WebSphereServer serv = getWebSphereServer();
        if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED
            || !serv.isPublishToRuntime()) {
            File file = path.toFile();
            if (file.exists() && file.isFile()) {
                if (!file.delete()) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorDeleteFile, path.toOSString())));
                    return;
                }
            }

            if (deltaKind == ServerBehaviourDelegate.REMOVED || !serv.isPublishToRuntime())
                return;
        }
        WebSphereServerBehaviour servBehaviour = getWebSphereServerBehaviour();
        if (kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
            // avoid changes if no changes to module since last publish
            IModuleResourceDelta[] delta = servBehaviour.getPublishedResourceDelta(module);
            if (delta == null || delta.length == 0)
                return;
        }

        // create directory if it doesn't exist
        if (path.segmentCount() > 1) { // should be
            IPath folder = path.removeLastSegments(1);
            File dir = folder.toFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorCreateFolder, folder.toOSString())));
                    return;
                }
            }
        }

        if (monitor.isCanceled()) {
            status.add(Status.CANCEL_STATUS);
            return;
        }

        // if the location is a directory, remove it first.  This may happen when switch between binary and non-binary module in RAD.
        File dir = path.toFile();
        if (dir.exists() && dir.isDirectory())
            PublishHelper.deleteDirectory(dir, null);

        IModuleResource[] mr = servBehaviour.getResources(module);
        IStatus[] stat;
        if (isBinary) // it is a jar already
            stat = helper.publishToPath(mr, path, monitor);
        else
            stat = helper.publishZip(mr, path, monitor);
        addArrayStatusToList(status, stat);
    }

    @Override
    public void prePublishApplication(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        IModule[] module = unit.getModule();
        if (module.length == 1 && unit.getDeltaKind() == ServerBehaviourDelegate.ADDED) {
            String appName = module[0].getName();
            if (getWebSphereServerBehaviour().getOverriddenAppsInServerXML().contains(appName) || getWebSphereServerBehaviour().getOverriddenDropinsApps().contains(appName)) {
                removeExternalAppFiles(module[0], NLS.bind(Messages.taskRemoveExteneralApp, appName), monitor);
            }
        }

        clearAddedResourceList();
        clearRemovedResourceList();
        clearChangedResourceList();
    }

    @Override
    public void postPublishApplication(int kind, PublishUnit app, MultiStatus status, IProgressMonitor monitor) {
        super.postPublishApplication(kind, app, status, monitor);
        if (moduleURICachedMap == null) // nothing need to update
            return;

        IPath filePath = getWebSphereServerBehaviour().getTempDirectory().append(MODULE_URI_CACHE_FILE_NAME);
        FileUtil.saveCachedProperties(moduleURICachedMap, filePath);
    }

    @Override
    protected String getModuleDeployName(IModule module) {
        return JEEServerExtension.getModuleDeployName(module);
    }

    @Override
    public boolean requireConsoleOutputBeforePublishComplete(int kind, PublishUnit unit, MultiStatus status, IProgressMonitor monitor) {
        int kind2 = unit.getDeltaKind();
        if (kind2 == ServerBehaviourDelegate.ADDED
            || (kind2 == ServerBehaviourDelegate.REMOVED)) {
            return true;
        }
        if (getAddedResourceList().isEmpty() && getRemovedResourceList().isEmpty() && getChangedResourceList().isEmpty())
            return false;

        if (checkFileExtension(getAddedResourceList())
            || checkFileExtension(getChangedResourceList())
            || checkFileExtension(getRemovedResourceList())) {
            return true;
        }

        return false;
    }
}
