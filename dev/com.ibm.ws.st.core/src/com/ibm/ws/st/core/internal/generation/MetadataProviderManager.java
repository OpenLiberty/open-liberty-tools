/*
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.core.internal.generation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.MetadataProvider;

/**
 * Manage metadata provider extensions
 */
public class MetadataProviderManager {
    private static final String EXTENSION_ID = "com.ibm.ws.st.core.metadataProvider";
    private static final String PROVIDER_ELEMENT = "provider";
    private static final String CLASS_ATTRIBUTE = "class";

    private static final MetadataProviderManager metadataProviderManager = new MetadataProviderManager();

    private final List<MetadataProvider> providers;

    public static URL getDefaultFeatureList() {
        for (MetadataProvider provider : getInstance().providers) {
            URL featureListURL = provider.getDefaultFeatureList();
            if (featureListURL != null) {
                return featureListURL;
            }
        }
        return Metadata.getFallback(FeatureListCoreMetadata.FEATURELIST_XML);
    }

    public static URL getDefaultSchema() {
        for (MetadataProvider provider : getInstance().providers) {
            URL schemaURL = provider.getDefaultSchema();
            if (schemaURL != null) {
                return schemaURL;
            }
        }
        return Metadata.getFallback(SchemaMetadata.SCHEMA_XSD);
    }

    private static MetadataProviderManager getInstance() {
        return metadataProviderManager;
    }

    private MetadataProviderManager() {
        providers = new ArrayList<MetadataProvider>();
        IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement configurationElement = extensions[i];
            if (PROVIDER_ELEMENT.equals(configurationElement.getName())) {
                try {
                    Object object = configurationElement.createExecutableExtension(CLASS_ATTRIBUTE);
                    providers.add((MetadataProvider) object);
                } catch (Exception exception) {
                    Trace.logError("Error while creating metadata provider extension.", exception);
                }
            }
        }
    }

}
