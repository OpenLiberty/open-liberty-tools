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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Trace;

/**
 * SchemaGenerator
 * Class for generating a schema for a runtime and getting the path to a runtime's schema.
 * N.B., this class relies on synchronization in the caller.
 */
public abstract class Metadata<PayloadType> implements GeneratorJob.Helper {
    // wait for maximum allowed for both the schema and feature list to be generated
    // plus a couple of seconds for scheduling; should never hit this timeout.
    static final int TIME_OUT_PERIOD = GeneratorJob.NUMBER_OF_ATTEMPTS * 3 + 2;
    public static final int CORE_METADATA = 0x1;
    public static final int EXT_METADATA = 0x2;
    public static final int SCHEMA_METADATA = 0x4;
    public static final int ALL_METADATA = 0xF;

    static public void generateMetadata(IMetadataGenerator metadataGen, IJobChangeListener[] listeners, int metadataTypes) {
        String genId = metadataGen.getGeneratorId();

        List<GeneratorJob.Helper> helpers = new ArrayList<GeneratorJob.Helper>();

        // schema generation
        if ((metadataTypes & SCHEMA_METADATA) == SCHEMA_METADATA) {
            SchemaMetadata schemaMetadata = SchemaMetadata.getInstance();
            schemaMetadata.createData(genId, metadataGen);
            helpers.add(schemaMetadata);
        }

        // core feature list generation
        if ((metadataTypes & CORE_METADATA) == CORE_METADATA) {
            FeatureListCoreMetadata featureListCoreMetadata = FeatureListCoreMetadata.getInstance();
            featureListCoreMetadata.createData(genId, metadataGen);
            helpers.add(featureListCoreMetadata);
        }

        // product extensions feature list generation
        if ((metadataTypes & EXT_METADATA) == EXT_METADATA) {
            for (FeatureListExtMetadata ext : FeatureListExtMetadata.getInstances(metadataGen.getWebSphereRuntime())) {
                ext.createData(genId, metadataGen);
                helpers.add(ext);
            }
        }

        GeneratorJob.generate(metadataGen, helpers.toArray(new GeneratorJob.Helper[helpers.size()]), listeners);
    }

    static public void removeMetadata(String generatorId, boolean destroy) {
        FeatureListCoreMetadata featureListMetadata = FeatureListCoreMetadata.getInstance();
        SchemaMetadata schemaMetadata = SchemaMetadata.getInstance();
        featureListMetadata.remove(generatorId, destroy);
        schemaMetadata.remove(generatorId, destroy);
        // The product extension feature lists behave slightly different. Since there is an instance for each product
        // extension, one instance per product extension per runtime, we can just remove the instances themselves
        // instead of resetting the data object.
        FeatureListExtMetadata.clearRuntimeInstances(generatorId);
    }

    /**
     * If the data points to a file that no longer exists return false, else return true; this occurs
     * if the metadata was deleted outside the workbench, after it was generated.
     */
    private static boolean doesDataFileStillExist(Metadata<?>.Data d) {
        if (d.payload instanceof URL) {
            URL u = (URL) d.payload;
            try {
                File f = new File(u.toURI().getPath());
                if (!f.exists()) {
                    return false;
                }
            } catch (URISyntaxException e) {
                // If we're unable to conert to a URI, then don't mess with it it
            }

        }

        return true;
    }

    protected PayloadType getGeneratedInfo(IMetadataGenerator metadataGen) {
        String genId = metadataGen.getGeneratorId();
        int i = 0;

        Data sd = infos.get(genId);
        if (sd == null || !doesDataFileStillExist(sd)) {
            // We can get here if this is the first access of the runtime from the tools
            // ie, it was created in a previous invocation of the tools
            IPath info = getTarget(metadataGen);
            File file = info.toFile();
            if (file.exists()) {
                createData(genId, metadataGen);
                setPayload(genId, reloadPayload(file));
            } else if (metadataGen.isReadyToGenerateMetadata()) {
                metadataGen.generateMetadata(null, true);
            }
            sd = infos.get(genId);
        }
        if (sd != null) {
            for (i = 0; i < TIME_OUT_PERIOD && sd.isGenerating(); ++i) {
                synchronized (this) {
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                    sd = infos.get(genId);
                }
            }
        }
        if (sd != null && sd.payload != null) {
            // not INVALID nor isGenerating
            return sd.payload;
        }
        if (Trace.ENABLED) {
            String problem;
            if (i >= TIME_OUT_PERIOD) {
                problem = "Timed out waiting for metadata for generator: ";
            } else if (sd != null) {
                problem = "Attempting to access metadata for deleted generator: ";
            } else {
                problem = "Problem generating metadata for generator: ";
            }
            Throwable t = new Throwable();
            t.fillInStackTrace();
            Trace.trace(Trace.WARNING, problem + genId, t);
        }
        return null;
    }

    abstract PayloadType reloadPayload(File file);

    public void createData(String generatorId, IMetadataGenerator metadataGen) {
        infos.put(generatorId, new Data((PayloadType) null, metadataGen));
    }

    protected static IPath getBasePath(IMetadataGenerator metadataGen) {
        return metadataGen.getBasePath(basePath);
    }

    public synchronized void remove(String generatorId, boolean destroy) {
        if (destroy) {
            // completely remove knowledge of the generator, this generatorId might
            // be used again
            infos.remove(generatorId);
        } else {
            // this generatorId was removed so signal to any waiting threads that
            // it was removed (so they don't timeout)
            setPayload(generatorId, null);
        }
    }

    public synchronized void generateIfMissingOrRemoved(IMetadataGenerator metadataGen, IJobChangeListener listener) {
        String id = metadataGen.getGeneratorId();
        boolean needToGenerate = false;
        Data d = infos.get(id);
        if (d == null) {
            needToGenerate = true;
        } else {
            if (d.isRemoved()) {
                // clean up old metadata so that it is not used if generation fails
                metadataGen.removeMetadata(null, false, true);
                needToGenerate = true;
            }
        }
        if (needToGenerate) {
            metadataGen.generateMetadata(listener, true);
        }
    }

    public final synchronized void generationComplete(String generatorId, PayloadType payload) {
        setPayload(generatorId, payload);
        notifyAll();
    }

    protected Metadata(String target) {
        super();
        infos = new ConcurrentHashMap<String, Data>();
        this.target = target;
    }

    @Override
    public IPath getTarget(IMetadataGenerator metadataGen) {
        return getBasePath(metadataGen).append(target);
    }

    void setPayload(String generatorId, PayloadType p) {
        Data data = infos.get(generatorId);
        if (data == null)
            return;

        data.payload = p;
        if (p == null) {
            // this is now invalid
            data.metadataGen = null;
        }
    }

    static protected URL getFallback(String file) {
        URL url = null;
        try {
            url = FileLocator.find(Activator.getInstance().getBundle(), new Path(file), null);
            url = FileLocator.resolve(url);
        } catch (Throwable t) {
            Trace.logError("Could not find fallback file " + file, t);
            url = null;
        }
        return url;
    }

    final ConcurrentHashMap<String, Data> infos;
    static final IPath basePath = Activator.getInstance().getStateLocation();
    final String target;

    final class Data {
        // { null, null } is removed
        // { null, metadataGen } means it is still generating
        public PayloadType payload;
        public IMetadataGenerator metadataGen;

        public Data(PayloadType payload, IMetadataGenerator metadataGen) {
            this.payload = payload;
            this.metadataGen = metadataGen;
        }

        public boolean isGenerating() {
            return payload == null && metadataGen != null;
        }

        public boolean isRemoved() {
            return metadataGen == null;
        }
    }

}
