/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.ManifestElement;

import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;

public class FeatureHandler {

    private enum FeatureVisibility {
        PUBLIC, PRIVATE, PROTECTED, AUTO
    }

    private final Map<String, int[]> featureMap = new HashMap<String, int[]>();
    private FeatureProcessor processor = null;

    public boolean containsFeatures(Collection<String> collection) {
        if (collection == null || collection.isEmpty())
            return false;

        for (String feature : collection) {
            if (!featureMap.containsKey(feature))
                return false;
        }
        return true;
    }

    public void addFeatures(Collection<String> collection) {
        if (collection == null || collection.isEmpty())
            return;

        for (String feature : collection) {
            int[] count = featureMap.get(feature);
            if (count == null) {
                count = new int[1];
                featureMap.put(feature, count);
            }
            ++count[0];
        }
    }

    public void removeFeatures(Collection<String> collection) {
        if (collection == null || collection.isEmpty())
            return;

        for (String feature : collection) {
            int[] count = featureMap.get(feature);
            if (count != null) {
                if (--count[0] == 0) {
                    featureMap.remove(feature);
                }
            }
        }
    }

    public boolean isMultipleSelection(String feature) {
        int[] count = featureMap.get(feature);
        if (count == null)
            return false;

        return count[0] > 1;
    }

    public void reset() {
        featureMap.clear();
    }
    
    public boolean isInstalled(IRuntimeInfo runtime, Collection<String> features) {
        if (features == null || features.isEmpty())
            return false;

        List<String> installedFeatures = runtime.getInstalledFeatures();
        if (installedFeatures == null || installedFeatures.isEmpty())
            return false;

        if (processor == null) {
            processor = new FeatureProcessor(runtime.getLocation());
        }

        for (String f : features) {
            if (!installedFeatures.contains(f)) {
                FeatureVisibility visibility = processor.getFeatureVisibility(f);
                if (visibility == null || visibility == FeatureVisibility.PUBLIC)
                    return false;
            }
        }

        return true;
    }

    /**
     * A helper class for inquiring about the visibility of
     * an installed feature (symbolic name)
     */
    private static class FeatureProcessor {

        private final HashMap<String, FeatureVisibility> symbolicFeatureMap = new HashMap<String, FeatureVisibility>();
        private final IPath runtimeLocation;

        protected FeatureProcessor(IPath runtimeLocation) {
            this.runtimeLocation = runtimeLocation;
            processFeatures();
        }

        protected FeatureVisibility getFeatureVisibility(String symbolicName) {
            return symbolicFeatureMap.get(symbolicName);
        }

        private void processFeatures() {
            if (runtimeLocation == null)
                return;

            IPath path = runtimeLocation.append("lib").append("features");

            // find all manifest files within this folder
            File[] files = path.toFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (file.isDirectory())
                        return false;

                    return file.getName().endsWith(".mf");
                }
            });

            for (File f : files) {
                InputStream in = null;
                try {
                    Map<String, String> props = new HashMap<String, String>();
                    in = new FileInputStream(f);
                    FileUtil.readSubsystem(in, props);

                    // Parse the symbolic name value
                    String content = props.get("Subsystem-SymbolicName");

                    // Per the java docs, the header name we are passing is
                    // only specified to provide error messages when the
                    // header value is invalid
                    ManifestElement[] elements = ManifestElement.parseHeader("Subsystem-SymbolicName", content);
                    if (elements != null) {
                        String visibility = elements[0].getAttribute("visibility");
                        FeatureVisibility v = processVisibility(visibility);
                        symbolicFeatureMap.put(elements[0].getValue(), v);
                    }
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Could not process manifest file" + f.getName(), e);
                } finally {
                    try {
                        if (in != null)
                            in.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        private FeatureVisibility processVisibility(String value) {
            if (value == null)
                return FeatureVisibility.AUTO;

            if (value.equals("public"))
                return FeatureVisibility.PUBLIC;

            if (value.equals("private"))
                return FeatureVisibility.PRIVATE;

            if (value.equals("protected"))
                return FeatureVisibility.PROTECTED;

            return FeatureVisibility.AUTO;
        }
    }
}
