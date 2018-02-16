/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class ProjectPrefs {
    protected static final String QUALIFIER = "com.ibm.ws.st";

    private static final String PROP_CLASSPATH_EXCLUDE_THIRD_PARTY = QUALIFIER + ".classpath.exclude.third-party";
    private static final String PROP_CLASSPATH_EXCLUDE_STABLE = QUALIFIER + ".classpath.exclude.stable";
    private static final String PROP_CLASSPATH_EXCLUDE_IBM_API = QUALIFIER + ".classpath.exclude.ibm-api";
    private static final String PROP_CLASSPATH_EXCLUDE_UNRECOGNIZED = QUALIFIER + ".classpath.exclude.unrecognized";
    private static final String PROP_FEATURE = QUALIFIER + ".feature.";

    private static final String PROP_OLD_EXCLUDE_THIRD_PARTY = "exclude-third-party";
    private static final String PROP_OLD_EXCLUDE_STABLE = "exclude-stable";
    private static final String PROP_OLD_EXCLUDE_IBM_API = "exclude-ibm-api";
    private static final String PROP_OLD_EXCLUDE_UNRECOGNIZED = "exclude-unrecognized";

    private static final String PROP_RUNTIME_PROJECT_VALIDATION_SERVERS = QUALIFIER + ".runtime.validation.configured.servers";

    private static final String[] ADD_FEATURE_STRINGS = new String[] { "prompt", "always", "never" };

    public static final byte ADD_FEATURE_PROMPT = 0;
    public static final byte ADD_FEATURE_ALWAYS = 1;
    public static final byte ADD_FEATURE_NEVER = 2;

    protected IEclipsePreferences prefs;

    public ProjectPrefs(IProject project) {
        prefs = new ProjectScope(project).getNode(QUALIFIER);
    }

    public int getFeaturePrompt(String feature) {
        String value = prefs.get(PROP_FEATURE + feature, null);
        if (value == null)
            return Activator.getDefaultClassScanning();

        for (int i = 0; i < ADD_FEATURE_STRINGS.length; i++) {
            if (ADD_FEATURE_STRINGS[i].equals(value))
                return i;
        }
        return Activator.getDefaultClassScanning();
    }

    public void setFeaturePrompt(String feature, byte prompt) {
        if (prompt < 0 || prompt >= ADD_FEATURE_STRINGS.length)
            return;

        prefs.put(PROP_FEATURE + feature, ADD_FEATURE_STRINGS[prompt]);
    }

    public boolean reset() {
        try {
            prefs.clear();
            return true;
        } catch (Exception e) {
            Trace.logError("Could not reset preferences: " + prefs, e);
            return false;
        }
    }

    public boolean save() {
        try {
            prefs.flush();
            return true;
        } catch (Exception e) {
            Trace.logError("Could not save preferences: " + prefs, e);
            return false;
        }
    }

    public boolean isExcludeThirdPartyAPI() {
        return prefs.getBoolean(PROP_CLASSPATH_EXCLUDE_THIRD_PARTY, prefs.getBoolean(PROP_OLD_EXCLUDE_THIRD_PARTY, false));
    }

    public void setExcludeThirdPartyAPI(boolean b) {
        if (isExcludeThirdPartyAPI() == b)
            return;
        prefs.putBoolean(PROP_CLASSPATH_EXCLUDE_THIRD_PARTY, b);
        if (prefs.get(PROP_OLD_EXCLUDE_IBM_API, null) != null)
            prefs.putBoolean(PROP_OLD_EXCLUDE_IBM_API, b);
    }

    public boolean isExcludeStableAPI() {
        return prefs.getBoolean(PROP_CLASSPATH_EXCLUDE_STABLE, prefs.getBoolean(PROP_OLD_EXCLUDE_STABLE, false));
    }

    public void setExcludeStableAPI(boolean b) {
        if (isExcludeStableAPI() == b)
            return;
        prefs.putBoolean(PROP_CLASSPATH_EXCLUDE_STABLE, b);
        if (prefs.get(PROP_OLD_EXCLUDE_STABLE, null) != null)
            prefs.putBoolean(PROP_OLD_EXCLUDE_STABLE, b);
    }

    public boolean isExcludeIBMAPI() {
        return prefs.getBoolean(PROP_CLASSPATH_EXCLUDE_IBM_API, prefs.getBoolean(PROP_OLD_EXCLUDE_IBM_API, false));
    }

    public void setExcludeIBMAPI(boolean b) {
        if (isExcludeIBMAPI() == b)
            return;
        prefs.putBoolean(PROP_CLASSPATH_EXCLUDE_IBM_API, b);
        if (prefs.get(PROP_OLD_EXCLUDE_IBM_API, null) != null)
            prefs.putBoolean(PROP_OLD_EXCLUDE_IBM_API, b);
    }

    public boolean isExcludeUnrecognized() {
        return prefs.getBoolean(PROP_CLASSPATH_EXCLUDE_UNRECOGNIZED, prefs.getBoolean(PROP_OLD_EXCLUDE_UNRECOGNIZED, false));
    }

    public void setExcludeUnrecognized(boolean b) {
        if (isExcludeUnrecognized() == b)
            return;
        prefs.putBoolean(PROP_CLASSPATH_EXCLUDE_UNRECOGNIZED, b);
        if (prefs.get(PROP_OLD_EXCLUDE_UNRECOGNIZED, null) != null)
            prefs.putBoolean(PROP_OLD_EXCLUDE_UNRECOGNIZED, b);
    }

    @Override
    public String toString() {
        return prefs.toString();
    }

    public String getRuntimeProjectValidationConfiguredServers() {
        return prefs.get(PROP_RUNTIME_PROJECT_VALIDATION_SERVERS, null);
    }

    public void setRuntimeProjectValidationConfiguredServers(String servers) {
        if (servers == null || servers.isEmpty())
            return;
        prefs.put(PROP_RUNTIME_PROJECT_VALIDATION_SERVERS, servers);
    }

}