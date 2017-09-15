/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.config.FeatureList.FeatureMapType;

public class FeatureListCoreMetadata extends AbstractFeatureListMetadata {
    public static final String FEATURELIST_XML = "featureList.xml";

    private HashMap<FeatureMapType, HashMap<String, Feature>> fallBackFeatureMaps = null;

    private final static FeatureListCoreMetadata instance = new FeatureListCoreMetadata();

    private FeatureListCoreMetadata() {
        super(FEATURELIST_XML);
    }

    static public FeatureListCoreMetadata getInstance() {
        return instance;
    }

    @Override
    public synchronized HashMap<FeatureMapType, HashMap<String, Feature>> getFallbackPayload() {
        if (fallBackFeatureMaps == null) {
            URL url = MetadataProviderManager.getDefaultFeatureList();
            if (url != null) {
                BufferedInputStream is = null;
                try {
                    is = new BufferedInputStream(url.openStream());
                    fallBackFeatureMaps = FeatureInfoHandler.parseFeatureListXML(is);
                } catch (Throwable t) {
                    Trace.logError("Error loading fallback featurelist file: " + url, t);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            if (Trace.ENABLED)
                                Trace.trace(Trace.WARNING, "Unable to close fallback featurelist file", e);
                        }
                    }
                }
            }
        }
        return fallBackFeatureMaps;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getCommandOptions() {
        // no options required for core feature list
        return null;
    }
}
