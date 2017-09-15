/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.FeatureConflict;
import com.ibm.ws.st.core.internal.RuntimeFeatureResolver.ResolverResult;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/**
 * A prompt for one or more applications that are missing a required feature.
 */
public class RequiredFeaturePrompt extends AbstractPrompt implements IPromptActionHandler {
    private final HashMap<String, List<String>> featureMap = new HashMap<String, List<String>>();
    private final String FEATURE_LOCAL_JMX_NO_VERSION = "localConnector";
    Map<String, RequiredFeatureIssue> issueMap = new HashMap<String, RequiredFeatureIssue>();

    @Override
    public boolean isActive() {
        return !issueMap.isEmpty();
    }

    @Override
    public IPromptActionHandler getActionHandler() {
        return this;
    }

    @Override
    public boolean getApplyAlways() {
        return false;
    }

    @Override
    public void prePromptAction(List<IModule[]> publishedModules, PublishHelper helper, IProgressMonitor monitor) {
        if (publishedModules == null || publishedModules.isEmpty())
            return;

        final WebSphereServer server = helper.getWebSphereServer();
        final ConfigurationFile configFile = server.getConfiguration();
        final Map<String, List<String>> alwaysAdd = new HashMap<String, List<String>>();

        issueMap.clear();
        if (!featureMap.isEmpty())
            featureMap.clear();

        FeatureSet fs = new FeatureSet(server.getWebSphereRuntime(), configFile.getAllFeatures());
        if (fs.resolve(Constants.FEATURE_LOCAL_JMX) == null) {
            configFile.addFeature(Constants.FEATURE_LOCAL_JMX);
        }

        List<IModuleResourceDelta[]> publishedDelta = new ArrayList<IModuleResourceDelta[]>(publishedModules.size());
        for (IModule[] module : publishedModules) {
            publishedDelta.add(server.getWebSphereServerBehaviour().getPublishedResourceDelta(module));
        }

        try {
            RequiredFeatureMap rfm = server.getRequiredFeatures(configFile, publishedModules, publishedDelta, monitor);
            if (rfm != null) {
                FeatureResolverFeature[] requiredFeatures = rfm.getFeatures();
                Map<IProject, ProjectPrefs> prefsMap = new HashMap<IProject, ProjectPrefs>();
                for (FeatureResolverFeature rf : requiredFeatures) {
                    String s = rf.getName();
                    if (s.toLowerCase().startsWith(FEATURE_LOCAL_JMX_NO_VERSION.toLowerCase())) {
                        continue;
                    }

                    List<IModule[]> modules = rfm.getModules(rf);
                    boolean isPrefsMatched = false;
                    boolean isAlwaysPrompt = false;
                    for (IModule[] module : modules) {
                        IProject project = module[module.length - 1].getProject();
                        if (project == null)
                            continue;

                        ProjectPrefs prefs = prefsMap.get(project);
                        if (prefs == null) {
                            prefs = new ProjectPrefs(project);
                            prefsMap.put(project, prefs);
                        }

                        // Order of strength (1 the strongest):
                        // 1. always add
                        // 2. prompt
                        // 3. never add
                        //
                        // The order of strength will dictate the final
                        // behavior
                        int featurePrompt = prefs.getFeaturePrompt(s);
                        switch (featurePrompt) {
                            case ProjectPrefs.ADD_FEATURE_ALWAYS:
                                List<String> appList = alwaysAdd.get(s);
                                if (appList == null) {
                                    appList = new ArrayList<String>();
                                    alwaysAdd.put(s, appList);
                                }
                                appList.add(module[0].getName());
                            case ProjectPrefs.ADD_FEATURE_PROMPT:
                                isAlwaysPrompt = !alwaysAdd.containsKey(s);
                            case ProjectPrefs.ADD_FEATURE_NEVER:
                                isPrefsMatched = true;
                            default:
                                break;
                        }
                    }

                    if (!isAlwaysPrompt) {
                        // if the user has not explicitly set an action in the 
                        // properties page, i.e. ProjectPrefs.ADD_FEATURE_UNKNOWN,
                        // always add the feature
                        if (!isPrefsMatched) {
                            List<String> appList = alwaysAdd.get(s);
                            if (appList == null) {
                                appList = new ArrayList<String>();
                                alwaysAdd.put(s, appList);
                            }
                            for (IModule[] module : modules)
                                appList.add(module[0].getName());
                        }
                        continue;
                    }

                    RequiredFeatureIssue issue = issueMap.get(rf.getName());
                    if (issue == null) {
                        issue = new RequiredFeatureIssue(rf.getName());
                        issueMap.put(rf.getName(), issue);
                    }
                    for (IModule[] module : modules)
                        issue.addApp(module[0].getName());
                }
            }
        } catch (CoreException ce) {
            Trace.logError("Error getting required features", ce);
        }

        if (issueMap.isEmpty()) {
            // If there are no issues but there are conflicts then prompt the user
            // to resolve the conflicts.  If there are issues, let the user decide
            // what features they want to enable first before checking for conflicts.
            WebSphereServer ws = helper.getWebSphereServer();
            List<String> allFeatures = configFile.getAllFeatures();
            List<String> combinedFeatures = new ArrayList<String>(allFeatures);
            for (String feature : alwaysAdd.keySet()) {
                if (!combinedFeatures.contains(feature))
                    combinedFeatures.add(feature);
            }
            ResolverResult result = RuntimeFeatureResolver.resolve(ws.getWebSphereRuntime(), combinedFeatures);
            Set<FeatureConflict> conflicts = result.getFeatureConflicts();
            boolean ignoreConflicts = ws.shouldIgnoreConflicts(conflicts);
            FeatureConflictHandler featureConflictHandler = Activator.getFeatureConflictHandler();
            if (conflicts != null && !conflicts.isEmpty() && featureConflictHandler != null && !ignoreConflicts) {
                if (featureConflictHandler.handleFeatureConflicts(ws.getServerInfo(), alwaysAdd, conflicts, false)) {
                    helper.setConfigChanged(true);
                }
                return;
            }
        }

        for (String s : alwaysAdd.keySet()) {
            if (!configFile.hasFeature(s)) {
                configFile.addFeature(s);
                helper.setConfigChanged(true);
            }
        }
    }

    @Override
    public void postPromptAction(IPromptResponse response, PublishHelper helper) {
        Collection<RequiredFeatureIssue> issues = issueMap.values();
        Map<String, List<String>> featureList = new HashMap<String, List<String>>();
        for (RequiredFeatureIssue issue : issues) {
            PromptAction action = response.getSelectedAction(issue);
            if (action == PromptAction.UPDATE_SERVER_CONFIG)
                featureList.put(issue.getFeatureName(), issue.getApps());
        }

        if (!featureList.isEmpty()) {
            // Check for conflicts in the final set of enabled features.
            final WebSphereServer ws = helper.getWebSphereServer();
            final ConfigurationFile configFile = ws.getConfiguration();
            final List<String> configFeatures = configFile.getAllFeatures();
            List<String> combinedFeatures = new ArrayList<String>(configFeatures);
            for (String feature : featureList.keySet()) {
                if (!combinedFeatures.contains(feature))
                    combinedFeatures.add(feature);
            }
            ResolverResult result = RuntimeFeatureResolver.resolve(ws.getWebSphereRuntime(), combinedFeatures);
            Set<FeatureConflict> conflicts = result.getFeatureConflicts();
            boolean ignoreConflicts = ws.shouldIgnoreConflicts(conflicts);
            FeatureConflictHandler featureConflictHandler = Activator.getFeatureConflictHandler();
            if (conflicts != null && !conflicts.isEmpty() && featureConflictHandler != null && !ignoreConflicts) {
                if (featureConflictHandler.handleFeatureConflicts(ws.getServerInfo(), featureList, conflicts, false)) {
                    helper.setConfigChanged(true);
                }
            } else {
                for (String s : featureList.keySet()) {
                    if (!configFile.hasFeature(s)) {
                        configFile.addFeature(s);
                        helper.setConfigChanged(true);
                    }
                }
            }
        }
    }

    @Override
    public IPromptIssue[] getIssues() {
        Collection<RequiredFeatureIssue> values = issueMap.values();
        IPromptIssue[] issues = new IPromptIssue[values.size()];
        return values.toArray(issues);
    }

    private static class RequiredFeatureIssue implements IPromptIssue {
        private final String featureName;
        private final List<String> appList = new ArrayList<String>(2);

        RequiredFeatureIssue(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public String getType() {
            return Messages.requiredFeatureIssue;
        }

        @Override
        public String getSummary() {
            return NLS.bind(Messages.requiredFeatureSummary, featureName);
        }

        @Override
        public String getDetails() {
            StringBuilder sb = new StringBuilder();
            for (String app : appList) {
                sb.append("\n - ");
                sb.append(app);
            }

            return NLS.bind(Messages.requiredFeatureDetails, new String[] { featureName, sb.toString() });
        }

        @Override
        public PromptAction[] getPossibleActions() {
            return new PromptAction[] { PromptAction.UPDATE_SERVER_CONFIG, PromptAction.IGNORE };
        }

        @Override
        public PromptAction getDefaultAction() {
            return PromptAction.UPDATE_SERVER_CONFIG;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RequiredFeatureIssue))
                return false;

            if (other == this)
                return true;

            RequiredFeatureIssue otherIssue = (RequiredFeatureIssue) other;
            return featureName.equals(otherIssue.featureName);
        }

        @Override
        public int hashCode() {
            return featureName.hashCode();
        }

        @Override
        public String toString() {
            return "Required feature " + featureName;
        }

        void addApp(String appName) {
            if (!appList.contains(appName))
                appList.add(appName);
        }

        List<String> getApps() {
            return new ArrayList<String>(appList);
        }

        String getFeatureName() {
            return featureName;
        }
    }
}
