/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent.Type;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.IExternalSchemaLocationProvider;

import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.SchemaUtil;
import com.ibm.ws.st.core.internal.generation.SchemaMetadata;

@SuppressWarnings("restriction")
public class SchemaLocationProvider implements IExternalSchemaLocationProvider, IFacetedProjectListener {
    private static final int CACHE_SIZE = 10;
    private static WebSphereRuntime tempRuntime;

    protected static class CacheEntry {
        /**
         * We keep track of the local file that this schema corresponds to:
         * if the file no longer exists, we will regenerate the cache.
         * This file may be null (for cases where could not resolve to a local file); in this case, the cache will
         * only be regenerated after clearCache(...) is called.
         */
        File configSchemaFile;

        URI uri;
        Map<String, String> map;
    }

    private static final CacheEntry[] cache = new CacheEntry[CACHE_SIZE];
    private static int count = 0;

    public SchemaLocationProvider() {

        ServerListenerUtil.getInstance().addServerListener(new IWebSphereServerListener() {
            @Override
            public void serverChanged(WebSphereServerInfo server) {
                clearCache();
            }

            @Override
            public void runtimeChanged(IRuntime runtime) {
                clearCache();
            }
        });
    }

    @Override
    public Map<?, ?> getExternalSchemaLocation(URI uri) {

        if (uri == null) {
            return null;
        }

        synchronized (cache) {
            for (int i = 0; i < CACHE_SIZE; i++) {
                if (cache[i] != null && uri.equals(cache[i].uri)) {

                    if (cache[i].configSchemaFile == null || cache[i].configSchemaFile.exists()) {

                        return cache[i].map;
                    }

                    // One of the entries needs recaching because it points to a file that no longer exists
                    break;

                }
            }
        }

        long time = System.currentTimeMillis();

        GetExternalSchemaLocationImplReturnValue externalSchemaResult;
        try {
            externalSchemaResult = getExternalSchemaLocationImpl(uri);
        } finally {
            if (Trace.ENABLED)
                Trace.tracePerf("Resolve schema", time);
        }

        // Cache everything since the editor asks for the schema for every single attribute
        // and element in all xml files (there is no way to specify a content type for the
        // org.eclipse.wst.xml.core.externalSchemaLocations extension point).
        if (externalSchemaResult != null) {

            CacheEntry ce = new CacheEntry();
            ce.uri = uri;
            ce.map = externalSchemaResult.map;
            ce.configSchemaFile = externalSchemaResult.schemaFile; // schemaFile may be null

            synchronized (cache) {
                cache[count] = ce;
                // if we reach cache size, reset counter
                if (++count == CACHE_SIZE)
                    count = 0;
            }
            return externalSchemaResult.map;
        }

        // Either external schema result is null, or map is null, so return null
        return null;
    }

    /** Converts a local URL to a File. Returns null if the param is null, if the syntax is improper, if it could not be resolved to a path, etc. */
    private static File convertLocalUrlToFile(URL url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = url.toURI();
            if (uri == null) {
                return null;
            }

            String uriPath = uri.getPath();
            if (uriPath == null) {
                return null;
            }

            return new File(uriPath);

        } catch (URISyntaxException e) {
            return null;
        }
    }

    /*
     * IMPORTANT NOTE - PLEASE READ: This code gets called for all xml files, there is nothing
     * in the extension point that checks the file type so the code must do the checking.
     * There are isServerConfigFile utilities in ConfigUtils (one that takes a URI, one that
     * takes an IFile and one that takes a File) that can help you with this.
     */
    private static GetExternalSchemaLocationImplReturnValue getExternalSchemaLocationImpl(URI uri) {
        final Map<String, String> map = new HashMap<String, String>(2);

        GetExternalSchemaLocationImplReturnValue result = new GetExternalSchemaLocationImplReturnValue();

        URI fileURI = uri;
        if (!URIUtil.isFileURI(fileURI)) {
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IFile[] files = workspaceRoot.findFilesForLocationURI(fileURI);
            if (files.length > 0)
                fileURI = files[0].getLocation().toFile().toURI();
        }

        URL schemaURL = SchemaUtil.getSchemaURLNoFallback(fileURI);
        if (schemaURL != null) {
            map.put(IExternalSchemaLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, schemaURL.toString());
            result.map = map;
            result.schemaFile = convertLocalUrlToFile(schemaURL);
            return result;
        }

        // need to support runtimes that haven't been saved yet. support lookup
        // via one temporary runtime
        if (tempRuntime != null) {
            schemaURL = tempRuntime.getConfigurationSchemaURL(fileURI);
            if (schemaURL != null) {
                map.put(IExternalSchemaLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, schemaURL.toString());
                result.map = map;
                result.schemaFile = convertLocalUrlToFile(schemaURL);
                return result;
            }
        }

        // return a generic global schema for any files in the plugin's .metadata folder - allows
        // merged editor to work
        // TODO - should find alternate mechanism later, once details of the schema editors are
        // better known
        if (fileURI.toASCIIString().contains(Activator.PLUGIN_ID)) {
            try {
                URL url = FileLocator.find(Activator.getInstance().getBundle(), new Path("server.xsd"), null);
                schemaURL = FileLocator.resolve(url);
            } catch (Exception e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "Could not find schema", e);
                // can't find the schema, return null
                return null;
            }
            map.put(IExternalSchemaLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, schemaURL.toString());
            result.map = map;
            result.schemaFile = convertLocalUrlToFile(schemaURL);
            return result;
        }

        // If the file is in the workspace and the content type shows it is a
        // server config file, then use the fallback schema.
        if (ConfigUtils.isServerConfigFile(fileURI)) {
            map.put(IExternalSchemaLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, SchemaMetadata.getFallbackSchema().toString());
            result.map = map;
            result.schemaFile = convertLocalUrlToFile(SchemaMetadata.getFallbackSchema());
            return result;
        }

        return result;
    }

    protected static void setTempRuntime(WebSphereRuntime runtime) {
        tempRuntime = runtime;
    }

    public static void clearCache() {
        synchronized (cache) {
            for (int i = 0; i < CACHE_SIZE; i++) {
                cache[i] = null;
            }
            count = 0;
        }
    }

    /** The return values of the getExternalSchemaLocationImpl method ONLY. Both values may be null when returned. */
    private static class GetExternalSchemaLocationImplReturnValue {
        protected Map<String, String> map = null;
        protected File schemaFile = null;

        protected GetExternalSchemaLocationImplReturnValue() {}

    }

    @Override
    public void handleEvent(IFacetedProjectEvent event) {
        if (event.getType() == Type.TARGETED_RUNTIMES_CHANGED) {
            clearCache();
        }
    }

}