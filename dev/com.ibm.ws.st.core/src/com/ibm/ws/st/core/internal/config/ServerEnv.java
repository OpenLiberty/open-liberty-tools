/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import com.ibm.ws.st.core.internal.Constants;

public class ServerEnv extends ExtendedConfigFile {

    private final HashMap<String, String> envVars = new HashMap<String, String>();
    private final HashMap<String, Integer> varLines = new HashMap<String, Integer>();
    private long lastModified = -1;

    public ServerEnv(File file, IFile ifile) throws IOException {
        super(file, ifile);
        lastModified = file.lastModified();
        load();
    }

    public boolean hasChanged() {
        long timestamp = file.lastModified();
        return timestamp != lastModified;
    }

    private void load() throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#"))
                    continue;
                int index = line.indexOf('=');
                if (index <= 0)
                    continue;
                String key = line.substring(0, index);
                String value = line.substring(index + 1, line.length());
                envVars.put(key, value);
                varLines.put(key, new Integer(lineNo));
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    public String getValue(String varName) {
        return envVars.get(varName);
    }

    public void getVariables(ConfigVars vars) {
        // server.env vars do not support variable expansion so no
        // context is needed
        Set<Map.Entry<String, String>> entries = envVars.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            DocumentLocation location = DocumentLocation.createDocumentLocation(file.toURI(), DocumentLocation.Type.SERVER_ENV, varLines.get(key).intValue());
            vars.addResolved(Constants.ENV_VAR_PREFIX + key, entry.getValue(), null, location);
        }
    }

    @Override
    public String toString() {
        return "Server Env [" + file + "]";
    }
}
