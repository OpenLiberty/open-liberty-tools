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
package com.ibm.ws.st.core.internal.generation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.WebSphereUtil;

/**
 * Remove out of sync meta data
 */
public class MetaDataRemover {

    public static void removeOutOfSyncMetaData() {
        try {
            long time = System.currentTimeMillis();
            MetaDataRemover.removeOutOfSyncMetaDataImpl();
            if (Trace.ENABLED)
                Trace.tracePerf("Metadata remove job", time);
        } catch (Exception e) {
            Trace.logError("Error in WebSphere Runtime MetaData Remover", e);
        }
    }

    protected static void removeOutOfSyncMetaDataImpl() {
        final File metaDataFolder = Activator.getInstance().getStateLocation().toFile();
        if (!metaDataFolder.exists()) {
            return;
        }

        final WebSphereRuntime[] runtimes = WebSphereUtil.getWebSphereRuntimes();
        final ArrayList<String> availableRuntimeIds = new ArrayList<String>(runtimes.length);
        if (runtimes.length > 0) {
            for (WebSphereRuntime runtime : runtimes) {
                // Remove any out of sync server metadata
                WebSphereServerInfo.removeOutOfSyncMetadata(runtime);
                availableRuntimeIds.add(runtime.getRuntime().getId());
            }
        }

        final File[] dirs = metaDataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !availableRuntimeIds.contains(name);
            }
        });

        if (dirs != null && dirs.length > 0) {
            for (File d : dirs) {
                if (isMetaDataDir(d)) {
                    Metadata.removeMetadata(d.getName(), false);
                    removeMetaDataDir(d);
                }
            }
        }
    }

    public static void removeCancelledMetaData(final Collection<String> runtimeIds) {
        final IPath metaDataPath = Activator.getInstance().getStateLocation();
        final File metaDataFolder = metaDataPath.toFile();

        if (!metaDataFolder.exists()) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not find plugin folder for meta data: " + metaDataPath.toOSString());
            return;
        }

        final File[] dirs = metaDataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return runtimeIds.contains(name);
            }
        });

        if (dirs != null && dirs.length > 0) {
            for (File d : dirs) {
                if (d.isDirectory()) {
                    // Remove any server metadata first
                    WebSphereServerInfo.removeCancelledMetaData(new Path(d.getAbsolutePath()), d.getName());
                    Metadata.removeMetadata(d.getName(), true);
                    removeMetaDataDir(d);
                }
            }
        }
    }

    private static boolean isMetaDataDir(File m) {
        if (!m.isDirectory()) {
            return false;
        }

        final File[] metaDataFiles = m.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(SchemaMetadata.SCHEMA_XSD) || name.startsWith(FeatureListCoreMetadata.FEATURELIST_XML);
            }
        });

        return metaDataFiles != null && metaDataFiles.length > 0;
    }

    private static void removeMetaDataDir(File dir) {
        final File[] metaDataFiles = dir.listFiles();
        if (metaDataFiles != null && metaDataFiles.length > 0) {
            for (File m : metaDataFiles) {
                if (m.isDirectory()) {
                    removeMetaDataDir(m);
                } else {
                    removeMetaDataFile(m);
                }
            }
        }
        removeMetaDataFile(dir);
    }

    private static void removeMetaDataFile(File f) {
        try {
            if (f.exists() && !f.delete()) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Unable to delete " + f.toString());
                }
            }
        } catch (Exception e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Failed to delete " + f.toString(), e);
            }
        }
    }
}
