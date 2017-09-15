/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;

public class SharedLibertyUtils {
    static Map<String, SharedLibInfo> sharedLibInfoMap = new ConcurrentHashMap<String, SharedLibInfo>();

    public static UtilitySharedLibInfo getUtilPrjSharedLibInfo(IProject project) {
        UtilitySharedLibInfo settings = new UtilitySharedLibInfo();
        if (project != null)
            getSettings(settings, project, JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH);
        return settings;
    }

    public static SharedLibRefInfo getSharedLibRefInfo(IProject project) {
        SharedLibRefInfo settings = new SharedLibRefInfo();
        getSettings(settings, project, JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH);
        return settings;
    }

    private static void getSettings(Properties settings, IProject project, String filePath) {
        IPath projLocation = project.getLocation();
        // The project coud be in the process of deletion, so check to
        // see if we have saved the settings
        if (projLocation == null) {
            if (JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH.equals(filePath)) {
                SharedLibInfo info = getSharedLibInfo(project);
                if (info != null) {
                    settings.setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, info.getLibId());
                    settings.setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_DIR_KEY, info.getLibDir());
                }
            }
            return;
        }

        getSettingsFromFile(settings, project, filePath);
    }

    private static void getSettingsFromFile(Properties settings, IProject project, String filePath) {
        File file = project.getLocation().append(filePath).toFile();
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                settings.load(in);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "getSettings()", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "getSettings()", e);
                    }
                }
            }
        }
    }

    /**
     * Answer if the module is a shared library if the module project is in the workspace
     * 
     * @param module
     * @return
     */
    public static boolean isSharedLibrary(IModule module) {
        return hasProjectLibSettings(module, JEEServerExtConstants.JST_UTILITY, JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH);
    }

    public static boolean isValidSharedLibrary(IModule module) {
        if (module == null)
            return false;

        String id = module.getModuleType().getId();
        if (!JEEServerExtConstants.JST_UTILITY.equals(id))
            return false;

        UtilitySharedLibInfo sharedLibInfo = getUtilPrjSharedLibInfo(module.getProject());
        String libId = sharedLibInfo.getLibId();
        return libId != null && !libId.isEmpty();
    }

    public static boolean isEJBRefSharedLibrary(IModule module) {
        return hasProjectLibSettings(module, JEEServerExtConstants.JST_EJB, JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH);
    }

    public static boolean isWebRefSharedLibrary(IModule module) {
        return hasProjectLibSettings(module, JEEServerExtConstants.JST_WEB, JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH);
    }

    public static boolean isEARRefSharedLibrary(IModule module) {
        return hasProjectLibSettings(module, JEEServerExtConstants.JST_EAR, JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH);
    }

    protected static boolean hasProjectLibSettings(IModule module, String moduleTypeId, String filePath) {
        if (module == null || moduleTypeId == null || filePath == null)
            return false;

        String id = module.getModuleType().getId();
        if (!moduleTypeId.equals(id))
            return false;

        IProject project = module.getProject();
        if (project == null) // null if this a deleted project in the workspace
            return false;

        if (project.getLocation() == null) {
            if (JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH.equals(filePath))
                return getSharedLibInfo(project) != null;

            return false;
        }

        IPath settingFile = project.getLocation().append(filePath);
        File file = settingFile.toFile();
        return file.exists();
    }

    public static boolean hasSharedLibSettingsFile(IProject project, String filePath) {
        if (project == null || project.getLocation() == null) // null if this a deleted project in the workspace
            return false;

        IPath settingFile = project.getLocation().append(filePath);
        File file = settingFile.toFile();
        return file.exists();
    }

    public static void saveSettings(Properties settings, IPath path) {
        if (settings == null || path == null)
            return;

        File file = path.toFile();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            settings.store(out, null);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "saveSettings()", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "saveSettings()", e);
                }
            }
        }
    }

    static void addSharedLibInfo(IProject project) {
        if (project == null || project.getLocation() == null)
            return;

        Properties props = new Properties();
        getSettingsFromFile(props, project, JEEServerExtConstants.SHARED_LIBRARY_SETTING_FILE_PATH);
        String id = props.getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, "");
        String dir = props.getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_DIR_KEY, "");
        sharedLibInfoMap.put(project.getName(), new SharedLibInfo(id, dir));
    }

    static void removeSharedLibInfo(IProject project) {
        if (project != null && isLastPublished(project))
            sharedLibInfoMap.remove(project.getName());
    }

    private static SharedLibInfo getSharedLibInfo(IProject project) {
        return sharedLibInfoMap.get(project.getName());
    }

    public static boolean isPublished(IProject project) {
        return getPublishCount(project) > 0;
    }

    private static boolean isLastPublished(IProject project) {
        return getPublishCount(project) < 2;
    }

    private static int getPublishCount(IProject project) {
        final IModule module = ServerUtil.getModule(project);
        return (module == null) ? 0 : ServerUtil.getServersByModule(module, null).length;
    }

    private static class SharedLibInfo {
        private final String id;
        private final String dir;

        SharedLibInfo(String id, String dir) {
            this.id = id;
            this.dir = dir;
        }

        String getLibId() {
            return id;
        }

        String getLibDir() {
            return dir;
        }
    }
}
