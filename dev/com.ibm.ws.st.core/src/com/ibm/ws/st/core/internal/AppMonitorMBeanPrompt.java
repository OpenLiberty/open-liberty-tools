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
package com.ibm.ws.st.core.internal;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;

/**
 *
 */
public class AppMonitorMBeanPrompt extends AbstractPrompt implements IPromptActionHandler {
    private static final String ALWAYS_KEY = "applicationMonitor.updateTrigger.mbean";
    private static final String IGNORE = "ignore";
    IPromptIssue issue = null;

    @Override
    public void prePromptAction(List<IModule[]> publishedModules, PublishHelper helper, IProgressMonitor monitor) {
        issue = null;

        if (publishedModules == null || publishedModules.isEmpty()) {
            return;
        }

        boolean hasInternalModule = false;
        for (IModule[] m : publishedModules) {
            if (m.length > 0 && !m[0].isExternal()) {
                hasInternalModule = true;
                break;
            }
        }

        if (!hasInternalModule)
            return;

        final WebSphereServer server = helper.getWebSphereServer();
        if (server == null) {
            if (Trace.ENABLED)
                Trace.logError("WebSphere server is null.", null);
            return;
        }

        final ConfigurationFile configFile = server.getConfiguration();
        if (configFile == null) {
            if (Trace.ENABLED)
                Trace.logError("Config file is null.", null);
            return;
        }

        if (!configFile.hasElement(Constants.APPLICATION_MONITOR)) {
            setMBean(helper);
            return;
        }

        boolean isAlwaysAdd = false;
        String always = helper.getPromptResponse(ALWAYS_KEY);
        if (always != null) {
            if (always.equals(IGNORE)) {
                return;
            }
            isAlwaysAdd = true;
        }

        String value = configFile.getAppMonitorUpdateTrigger();
        if (value == null || !value.equals("mbean")) {
            if (isAlwaysAdd) {
                setMBean(helper);
                return;
            }
            issue = new AppMonitorUpdateTriggerIssue();
        }
    }

    @Override
    public void postPromptAction(IPromptResponse response, PublishHelper helper) {
        PromptAction action = response.getSelectedAction(issue);
        boolean always = response.getApplyAlways(issue);
        if (always) {
            helper.addPromptResponse(ALWAYS_KEY, (action == PromptAction.UPDATE_SERVER_CONFIG) ? "add" : IGNORE);
            helper.savePromptResponses();
        }

        if (action == PromptAction.UPDATE_SERVER_CONFIG)
            setMBean(helper);
    }

    private void setMBean(PublishHelper helper) {
        final WebSphereServer ws = helper.getWebSphereServer();
        final ConfigurationFile configFile = ws.getConfiguration();
        if (!configFile.hasElement(Constants.APPLICATION_MONITOR)) {
            configFile.addElement(Constants.APPLICATION_MONITOR);
        }
        configFile.setAttribute(Constants.APPLICATION_MONITOR, Constants.APPLICATION_MONITOR_TRIGGER, Constants.APPLICATION_MONITOR_MBEAN);
        helper.setConfigChanged(true);
    }

    @Override
    public boolean isActive() {
        return issue != null;
    }

    @Override
    public IPromptActionHandler getActionHandler() {
        return this;
    }

    @Override
    public IPromptIssue[] getIssues() {
        if (issue == null)
            return null;

        return new IPromptIssue[] { issue };
    }

    private static class AppMonitorUpdateTriggerIssue implements IPromptIssue {

        AppMonitorUpdateTriggerIssue() {}

        @Override
        public String getType() {
            return Messages.appMonitorTriggerMBeanIssue;
        }

        @Override
        public String getSummary() {
            return Messages.appMonitorTriggerMBeanSummary;
        }

        @Override
        public String getDetails() {
            return Messages.appMonitorTriggerMBeanDetails;
        }

        @Override
        public PromptAction[] getPossibleActions() {
            return new PromptAction[] { PromptAction.UPDATE_SERVER_CONFIG, PromptAction.IGNORE };
        }

        @Override
        public PromptAction getDefaultAction() {
            return PromptAction.UPDATE_SERVER_CONFIG;
        }
    }
}
