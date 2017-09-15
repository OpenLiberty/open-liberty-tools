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
package com.ibm.ws.st.core.internal.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;

public class Bootstrap extends ExtendedConfigFile {
    private static final String KEY_INCLUDE = "bootstrap.include"; // relative path
    private static final String KEY_MESSAGES_FILE = "com.ibm.ws.logging.message.file.name";
    private static final String KEY_LOG_DIR = "com.ibm.ws.logging.log.directory";
    private static final String KEY_TRACE_FILE = "com.ibm.ws.logging.trace.file.name";

    public static final Set<String> NON_VAR_KEYS = new HashSet<String>(5);

    static {
        NON_VAR_KEYS.add(KEY_INCLUDE);
    }

    protected HashMap<String, String> properties = new HashMap<String, String>();
    protected long lastModified = -1;

    public Bootstrap(File file, IFile ifile) throws IOException {
        super(file, ifile);
        lastModified = file.lastModified();
        load();
    }

    public boolean hasChanged() {
        long timestamp = file.lastModified();
        return timestamp != lastModified;
    }

    private void load() throws IOException {
        mergeProperties(properties, file.toURI().toURL(), null);
    }

    /**
     * Merge properties from resource specified by urlStr (which is resolved
     * against the given
     * baseURL, in the case of relative paths) into the target map.
     * 
     * @param target
     *            Target map to populate with new properties
     * @param baseURL
     *            Base location used for resolving relative paths
     * @param urlStr
     *            URL string describing the properties resource to load
     */
    private void mergeProperties(Map<String, String> target, URL baseURL, String urlStr) throws IOException {
        String includes = null;
        URL url;
        if (baseURL != null && urlStr == null)
            url = baseURL;
        else
            url = new URL(baseURL, urlStr);

        // Read properties from file then trim trailing white spaces
        Properties props = getProperties(url.openStream());
        Properties p = new Properties();

        for (Enumeration<?> propsKeys = props.propertyNames(); propsKeys.hasMoreElements();) {
            String propKey = (String) propsKeys.nextElement();
            String propValue = props.getProperty(propKey);
            propValue = propValue.trim();
            p.put(propKey, propValue);
        }

        includes = (String) p.remove(KEY_INCLUDE);

        // First value to be set wins. Add values in the current file before
        // looking at included files.
        addMissingProperties(p, target);

        if (includes != null)
            processIncludes(target, url, includes);
    }

    /**
     * Add properties from source to target, if the target map does not
     * already contain a property with that value.
     * 
     * @param source
     * @param target
     */
    private void addMissingProperties(Properties source, Map<String, String> target) {
        if (source == null || source.isEmpty() || target == null)
            return;

        // only add "new" properties (first value wins)
        for (String key : source.stringPropertyNames()) {
            if (!target.containsKey(key)) {
                String value = source.getProperty(key);
                target.put(key, value);

                // also set the new bootstrap property as a system property
                System.setProperty(key, value);
            }
        }
    }

    /**
     * Process included/referenced bootstrap properties. Properties resources
     * should be specified using a) relative path from the containing
     * bootstrap.properties,
     * b) absolute path, or c) full URI/URLs.
     * 
     * @param mergeProps
     * @param includeProps
     */
    private void processIncludes(Map<String, String> mergeProps, URL rootURL, String includeProps) throws IOException {
        if (includeProps == null)
            return;

        String props[] = includeProps.trim().split("\\s*,\\s*");
        for (String pname : props) {
            mergeProperties(mergeProps, rootURL, pname);
        }
    }

    public Set<Map.Entry<String, String>> getVariables(ConfigVars vars) {
        vars.startContext();
        DocumentLocation location = DocumentLocation.createDocumentLocation(file.toURI(), DocumentLocation.Type.BOOTSTRAP);
        Set<Map.Entry<String, String>> entries = properties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            if (!NON_VAR_KEYS.contains(key)) {
                vars.add(key, entry.getValue(), location);
            }
        }
        vars.endContext();
        return entries;
    }

    public String getMessagesFile() {
        return properties.get(KEY_MESSAGES_FILE);
    }

    public String getTraceFile() {
        return properties.get(KEY_TRACE_FILE);
    }

    public String getLogDir() {
        return properties.get(KEY_LOG_DIR);
    }

    /**
     * Read properties from input stream. Will close the input stream before
     * returning.
     * 
     * @param is
     *            InputStream to read properties from
     * @return Properties object; will be empty if InputStream is null or empty.
     * @throws LaunchException
     */
    public static Properties getProperties(final InputStream is) throws IOException {
        Properties p = new Properties();

        try {
            if (is != null && is.available() > 0)
                p.load(is);
        } catch (Throwable e) {
            throw new IOException("Unable to load properties from InputStream", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        return p;
    }

    @Override
    public String toString() {
        return "Bootstrap [" + file + "]";
    }
}
