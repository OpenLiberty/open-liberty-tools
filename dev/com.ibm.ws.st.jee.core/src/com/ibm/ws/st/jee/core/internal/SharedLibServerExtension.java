/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.util.List;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.OutOfSyncModuleInfo;
import com.ibm.ws.st.core.internal.ServerExtension;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

@SuppressWarnings("restriction")
public class SharedLibServerExtension extends ServerExtension {
    private static final String JST_UTILITY = "jst.utility";

    @Override
    public IStatus canAddModule(IModule module) {
        if (SharedLibertyUtils.isValidSharedLibrary(module))
            return Status.OK_STATUS;

        return null;
    }

    @Override
    public IModule[] getChildModules(IModule[] module) {
        return new IModule[0];
    }

    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        if (SharedLibertyUtils.isValidSharedLibrary(module)) // we need this check. utility module that is not shared lib will be passed in.
            return new IModule[] { module };

        return new IModule[0];
    }

    @Override
    public boolean modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) {
        ConfigurationFile config = getWebSphereServer().getConfiguration();

        for (IModule module : add) {
            if (SharedLibertyUtils.isSharedLibrary(module)) {
                UtilitySharedLibInfo settings = SharedLibertyUtils.getUtilPrjSharedLibInfo(module.getProject());
                config.addSharedLibrary(settings.getLibId(), settings.getLibDir(), module.getName() + ".jar", APIVisibility.getAPIVisibilityFromProperties(settings));
            }
        }
        for (IModule module : remove) {
            if (SharedLibertyUtils.isSharedLibrary(module)) {
                UtilitySharedLibInfo settings = SharedLibertyUtils.getUtilPrjSharedLibInfo(module.getProject());
                config.removeSharedLibrary(settings.getLibId(), settings.getLibDir(), module.getName() + ".jar");
                if (module.getProject() != null && module.getProject().getLocation() == null)
                    SharedLibertyUtils.removeSharedLibInfo(module.getProject());
            }
        }
        return true;
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
                if (pModule.length != 1)
                    continue;
                String id = pModule[0].getModuleType().getId();
                if (JST_UTILITY.equals(id) && module.equals(pModule[0]) && SharedLibertyUtils.isSharedLibrary(module)) {
                    inPublishedModules = true;
                    break;
                }
            }
            if (inPublishedModules)
                break;
        }

        if (!inPublishedModules)
            return false;

        // check loose config
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

        if (ILaunchManager.DEBUG_MODE.equals(getWebSphereServer().getServer().getMode()))
            return false;

        return true;
    }

    @Override
    public boolean canRestartModule(IModule[] module) {
        return false;
    }

    @Override
    public OutOfSyncModuleInfo checkModuleConfigOutOfSync(IModule module) {
        if (SharedLibertyUtils.isSharedLibrary(module)) {
            final ConfigurationFile configFile = getConfiguration();
            final String[] ids = configFile.getSharedLibraryIds();
            final String sharedLibId = SharedLibertyUtils.getUtilPrjSharedLibInfo(module.getProject()).getLibId();
            for (String id : ids) {
                if (id.equals(sharedLibId)) {
                    return null;
                }
            }
            return new OutOfSyncModuleInfo(OutOfSyncModuleInfo.Type.SHARED_LIB_ENTRY_MISSING);
        }
        return null;
    }
}
