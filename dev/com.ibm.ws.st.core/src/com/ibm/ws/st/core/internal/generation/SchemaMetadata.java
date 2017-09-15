/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.generation;

import java.io.File;
import java.net.URL;

import com.ibm.ws.st.core.internal.Trace;

/**
 * SchemaGenerator
 * Class for generating a schema for a runtime and getting the path to a runtime's schema.
 * N.B., this class relies on synchronization in the caller.
 */
public class SchemaMetadata extends Metadata<URL> {
    public static SchemaMetadata getInstance() {
        return instance;
    }

    /** Calling this method causes the metadata to be generated, if it does not already exist. */
    public URL getSchemaPath(IMetadataGenerator metadataGen) {
        URL url = null;
        if (metadataGen != null) {
            try {
                url = getGeneratedInfo(metadataGen);
            } catch (Throwable t) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Error occurred while getting schema for " + metadataGen.getGeneratorId(), t);
                }
            }
        }
        if (url == null) {
            url = getFallbackSchema();
        }
        return url;
    }

    static public URL getFallbackSchema() {
        URL url = MetadataProviderManager.getDefaultSchema();
        return url;
    }

    public boolean metadataExists(IMetadataGenerator metadataGen) {
        File f = getTarget(metadataGen).toFile();
        return f.exists();
    }

    public URL getPayload(IMetadataGenerator metadataGen) {
        URL url = null;
        try {
            File f = getTarget(metadataGen).toFile();
            if (f.exists()) {
                url = f.toURI().toURL();
            } else {
                // avoid multiple logged errors
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Schema file " + getTarget(metadataGen).toOSString() + " does not exist.", null);
                }
            }
        } catch (Throwable t) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Problem occurred while retrieving schema payload for runtime id: " + metadataGen.getGeneratorId(), t);
        }
        if (url == null) {
            url = getFallbackSchema();
        }
        return url;
    }

    @Override
    URL reloadPayload(File file) {
        URL url = null;
        if (file.exists()) {
            try {
                url = file.toURI().toURL();
            } catch (Throwable t) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Error while attempting to reload schema", t);
            }
        }
        if (url == null) {
            url = getFallbackSchema();
        }
        return url;
    }

    private SchemaMetadata() {
        super(SCHEMA_XSD);
    }

    public final static String SCHEMA_XSD = "server.xsd";

    private final static SchemaMetadata instance = new SchemaMetadata();
}
