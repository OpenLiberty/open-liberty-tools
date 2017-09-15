/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.ibm.ws.st.core.internal.ApplicationPublisher;
import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.PublishUnit;

@SuppressWarnings("restriction")
public class SharedLibPublisher extends ApplicationPublisher {
    private Properties moduleURICachedMap = null;
    private static String MODULE_URI_CACHE_FILE_NAME = "sharedLibModuleUri.map";

    @Override
    protected IStatus publishModule(int kind, PublishUnit unit, IProgressMonitor monitor) {
        IModule[] module = unit.getModule();
        int deltaKind = unit.getDeltaKind();

        IPath cacheFilePath = getWebSphereServerBehaviour().getTempDirectory().append(MODULE_URI_CACHE_FILE_NAME);
        if (moduleURICachedMap == null) {
            moduleURICachedMap = new Properties();
            FileUtil.loadProperties(moduleURICachedMap, cacheFilePath);
        }
        String cacheKey = FileUtil.genModuleURICacheKey(module);
        List<IStatus> status = new ArrayList<IStatus>();
        // We still need to check if it is a shared lib. If there is another server extension that supports
        // jst.utility as root module, this one will be called too.  We need to expend the extension more to have
        // class to check before calling this extension/method.  
        if (module.length == 1 && SharedLibertyUtils.isSharedLibrary(module[0]) ||
                        deltaKind == ServerBehaviourDelegate.REMOVED && moduleURICachedMap.containsKey(cacheKey)) {
            IProject project = module[0].getProject();
            UtilitySharedLibInfo info = SharedLibertyUtils.getUtilPrjSharedLibInfo(project);
            if (deltaKind == ServerBehaviourDelegate.REMOVED) {
                String filePath = moduleURICachedMap.getProperty(cacheKey);
                IPath path;
                if (filePath != null)
                    path = new Path(filePath);
                else
                    path = (new Path(info.getLibDir())).append(getModuleDeployName(module[0]));
                if (!path.toFile().delete())
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Can't delete shared library jar file for " + project.getName());
                moduleURICachedMap.remove(cacheKey);
            } else if (deltaKind == ServerBehaviourDelegate.ADDED ||
                            deltaKind == ServerBehaviourDelegate.CHANGED ||
                            kind == IServer.PUBLISH_CLEAN) {
                if (info.getLibId().isEmpty() || info.getLibDir().isEmpty()) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorSharedLibProjectInfoIncomplete, project.getName()));
                }
                PublishHelper helper = new PublishHelper(getWebSphereServer().getWorkAreaTempPath().toFile());

                IPath path = new Path(info.getLibDir());
                // make directory if it doesn't exist
                if (!path.toFile().exists()) {
                    if (!path.toFile().mkdirs()) {
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorCreateFolder, path.toOSString()));
                    }
                }

                path = path.append(getModuleDeployName(module[0]));
                IModuleResource[] mr = getWebSphereServerBehaviour().getResources(module);
                IStatus[] stat = helper.publishZip(mr, path, monitor);
                addArrayToList(status, stat);
                moduleURICachedMap.put(cacheKey, path.toOSString());
            }
            FileUtil.saveCachedProperties(moduleURICachedMap, cacheFilePath);
        }
        return combineModulePublishStatus(status, module[module.length - 1].getName());
    }

    private static void addArrayToList(List<IStatus> list, IStatus[] a) {
        if (list == null || a == null || a.length == 0)
            return;

        int size = a.length;
        for (int i = 0; i < size; i++)
            list.add(a[i]);
    }

    /** {@inheritDoc} */
    @Override
    protected String getModuleDeployName(IModule module) {
        if (module == null)
            return null;
        return module.getName() + ".jar";
    }
}
