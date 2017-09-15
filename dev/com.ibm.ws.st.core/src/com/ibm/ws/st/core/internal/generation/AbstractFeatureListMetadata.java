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
package com.ibm.ws.st.core.internal.generation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.config.FeatureList.FeatureMapType;

/**
 *
 */
public abstract class AbstractFeatureListMetadata extends Metadata<HashMap<FeatureMapType, HashMap<String, Feature>>> {

    protected AbstractFeatureListMetadata(String target) {
        super(target);
    }

    public abstract HashMap<FeatureMapType, HashMap<String, Feature>> getFallbackPayload();

    public abstract String[] getCommandOptions();

    public HashMap<FeatureMapType, HashMap<String, Feature>> getFeatureListMaps(IMetadataGenerator metadataGen) {
        HashMap<FeatureMapType, HashMap<String, Feature>> map = null;
        if (metadataGen != null) {
            try {
                map = getGeneratedInfo(metadataGen);
            } catch (Throwable t) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Error occurred while getting feature list for " + metadataGen.getGeneratorId(), t);
                }

            }
        }
        if (map == null) {
            map = getFallbackPayload();
        }
        return map;
    }

    public HashMap<FeatureMapType, HashMap<String, Feature>> getPayload(IMetadataGenerator metadataGen) {
        HashMap<FeatureMapType, HashMap<String, Feature>> map = null;
        InputStream is = null;
        try {
            File f = getTarget(metadataGen).toFile();
            if (f.exists()) {
                is = new FileInputStream(f);
                map = FeatureInfoHandler.parseFeatureListXML(is);
            } else {
                // avoid multiple logged errors

                // bug in mac causes it to always fail to generate feature list in GA version
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Feature list file " + getTarget(metadataGen).toOSString() + " does not exist.", null);
                }
            }
        } catch (Throwable e) {
            Trace.logError("Error occurred while parsing or retrieving feature list payload for generator: " + metadataGen.getGeneratorId(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error closing featurelist file", e);
                }
            }
        }
        if (map == null) {
            map = getFallbackPayload();
        }
        return map;
    }

    @Override
    HashMap<FeatureMapType, HashMap<String, Feature>> reloadPayload(File file) {
        HashMap<FeatureMapType, HashMap<String, Feature>> map = null;
        if (file.exists()) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                map = FeatureInfoHandler.parseFeatureListXML(is);
            } catch (Throwable t) {
                Trace.logError("Error parsing featurelist file: " + file.getAbsolutePath(), t);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Error closing featurelist file: " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        if (map == null) {
            map = getFallbackPayload();
        }
        return map;

    }

}
