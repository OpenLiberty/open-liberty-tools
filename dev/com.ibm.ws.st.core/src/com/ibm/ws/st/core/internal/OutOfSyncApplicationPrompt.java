/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;

import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;

/**
 * A prompt for one or more applications that are out of sync with serve configuration.
 */
public class OutOfSyncApplicationPrompt extends AbstractPrompt implements IPromptActionHandler {
    private final Map<IPromptIssue, IModule> moduleMap = new HashMap<IPromptIssue, IModule>(2);
    private final HashSet<String> appNames = new HashSet<String>(2);
    private final List<IPromptIssue> issueList = new ArrayList<IPromptIssue>(2);

    private void addAppIssue(OutOfSyncModuleInfo info, IModule module) {
        // Ignore if we have seen the module name before
        if (!appNames.add(module.getName())) {
            return;
        }

        IPromptIssue issue = new OutOfSyncIssue(info, module.getName());
        issueList.add(issue);
        moduleMap.put(issue, module);
    }

    @Override
    public boolean getApplyAlways() {
        return false;
    }

    @Override
    public boolean isActive() {
        return !issueList.isEmpty();
    }

    @Override
    public IPromptActionHandler getActionHandler() {
        return this;
    }

    @Override
    public void prePromptAction(List<IModule[]> publishedModules, PublishHelper helper, IProgressMonitor monitor) {
        final WebSphereServerBehaviour wsBehaviour = helper.getWebSphereServerBehaviour();

        issueList.clear();
        moduleMap.clear();
        appNames.clear();

        for (IModule[] module : publishedModules) {
            if (!module[0].isExternal()) {
                final OutOfSyncModuleInfo info = wsBehaviour.checkModuleConfigOutOfSync(module[0]);
                if (info != null) {
                    addAppIssue(info, module[0]);
                }
            }
        }
    }

    @Override
    public void postPromptAction(IPromptResponse response, PublishHelper helper) {
        final WebSphereServer ws = helper.getWebSphereServer();
        List<IModule> addList = new ArrayList<IModule>(issueList.size());
        List<IModule> removeList = new ArrayList<IModule>(issueList.size());

        for (IPromptIssue issue : issueList) {
            PromptAction action = response.getSelectedAction(issue);
            if (action == PromptAction.UPDATE_SERVER_CONFIG)
                addList.add(moduleMap.get(issue));
            else if (action == PromptAction.REMOVE_FROM_SERVER)
                removeList.add(moduleMap.get(issue));
        }

        IModule[] add = null;
        IModule[] remove = null;

        if (!addList.isEmpty()) {
            add = addList.toArray(new IModule[addList.size()]);
            addList.clear();
        }

        if (!removeList.isEmpty()) {
            remove = removeList.toArray(new IModule[removeList.size()]);
            removeList.clear();
        }

        if (add == null && remove == null)
            return;

        IServerWorkingCopy wc;
        if (ws.getServer().isWorkingCopy()) {
            wc = (IServerWorkingCopy) ws.getServer();
        } else {
            wc = ws.getServer().createWorkingCopy();
        }
        try {
            wc.modifyModules(add, remove, null);
            wc.save(true, null);
        } catch (CoreException e) {
            Trace.logError("Failed to modify application(s) from server: " + ws.getServerName(), e);
        }
    }

    @Override
    public IPromptIssue[] getIssues() {
        return issueList.toArray(new IPromptIssue[issueList.size()]);
    }

    private static class OutOfSyncIssue implements IPromptIssue {

        private final OutOfSyncModuleInfo info;
        private final String details;
        private final String summary;

        OutOfSyncIssue(OutOfSyncModuleInfo info, String moduleName) {
            this.info = info;
            details = buildDetails(info, moduleName);
            summary = buildSummary(info, moduleName);
        }

        @Override
        public String getType() {
            return Messages.outOfSyncIssue;
        }

        @Override
        public String getSummary() {
            return summary;
        }

        @Override
        public String getDetails() {
            return details;
        }

        @Override
        public PromptAction[] getPossibleActions() {
            if (info.getType() == OutOfSyncModuleInfo.Type.SHARED_LIB_REF_MISMATCH)
                return new PromptAction[] { PromptAction.UPDATE_SERVER_CONFIG, PromptAction.IGNORE };

            return new PromptAction[] { PromptAction.UPDATE_SERVER_CONFIG, PromptAction.REMOVE_FROM_SERVER, PromptAction.IGNORE };
        }

        @Override
        public PromptAction getDefaultAction() {
            return PromptAction.UPDATE_SERVER_CONFIG;
        }

        private String buildDetails(OutOfSyncModuleInfo info, String moduleName) {
            OutOfSyncModuleInfo.Type type = info.getType();
            if (type == OutOfSyncModuleInfo.Type.SHARED_LIB_REF_MISMATCH) {
                String addIds = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_IDS_ADD);
                String removeIds = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_IDS_REMOVE);
                String changeIds = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_IDS_CHANGE);
                String apiVisibility = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_API_VISIBILITY);

                if ((removeIds == null || removeIds.isEmpty()) && (addIds == null || addIds.isEmpty()) && (changeIds == null || changeIds.isEmpty())) {
                    return NLS.bind(Messages.outOfSyncSharedLibRefAPIVisibilityChangedDetails, apiVisibility);
                }

                String message = null;
                if (addIds != null && !addIds.isEmpty()) {
                    message = buildRefIdsDetails(Messages.outOfSyncSharedLibRefMissingDetails, moduleName, addIds);
                }
                if (removeIds != null && !removeIds.isEmpty()) {
                    if (message != null) {
                        message += "\n\n";
                    }
                    message += buildRefIdsDetails(Messages.outOfSyncSharedLibRefNotUsedDetails, moduleName, removeIds);
                }
                if (changeIds != null && !changeIds.isEmpty()) {
                    if (message != null) {
                        message += "\n\n";
                    }
                    message += buildRefIdsDetails(Messages.outOfSyncSharedLibRefChangedDetails, moduleName, changeIds);
                }
                return message;
            }

            String label = (type == OutOfSyncModuleInfo.Type.APP_ENTRY_MISSING) ? Messages.applicationLabel : Messages.sharedLibraryLabel;
            return NLS.bind(Messages.outOfSyncAppMissingDetails, new String[] { moduleName, label });
        }

        private String buildRefIdsDetails(String message, String moduleName, String idList) {
            String[] ids = convertToArray(idList);
            StringBuilder sb = new StringBuilder();
            if (ids == null) {
                if (Trace.ENABLED)
                    Trace.logError("lib id references are null", null);
            } else {
                for (String id : ids) {
                    sb.append("\n - ");
                    sb.append(id);
                }
            }
            return NLS.bind(message, new String[] { moduleName, sb.toString() });
        }

        private String buildSummary(OutOfSyncModuleInfo info, String moduleName) {
            OutOfSyncModuleInfo.Type type = info.getType();
            if (type == OutOfSyncModuleInfo.Type.SHARED_LIB_REF_MISMATCH) {
                String addIds = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_IDS_ADD);
                String removeIds = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_IDS_REMOVE);
                String changeIds = info.getPropertyValue(OutOfSyncModuleInfo.Property.LIB_REF_IDS_CHANGE);

                if ((removeIds == null || removeIds.isEmpty()) && (addIds == null || addIds.isEmpty()) && (changeIds == null || changeIds.isEmpty())) {
                    return NLS.bind(Messages.outOfSyncSharedLibRefAPIVisibilityChangedSummary, moduleName);
                }

                if ((removeIds == null || removeIds.isEmpty()) && (changeIds == null || changeIds.isEmpty())) {
                    return NLS.bind(Messages.outOfSyncSharedLibRefMissingSummary, moduleName);
                }

                if ((addIds == null || addIds.isEmpty()) && (changeIds == null || changeIds.isEmpty())) {
                    return NLS.bind(Messages.outOfSyncSharedLibRefNotUsedSummary, moduleName);
                }

                if ((addIds == null || addIds.isEmpty()) && (removeIds == null || removeIds.isEmpty())) {
                    return NLS.bind(Messages.outOfSyncSharedLibRefChangedSummary, moduleName);
                }

                return NLS.bind(Messages.outOfSyncSharedLibRefMismatchSummary, moduleName);
            }
            String label = (type == OutOfSyncModuleInfo.Type.APP_ENTRY_MISSING) ? Messages.applicationLabel : Messages.sharedLibraryLabel;
            return NLS.bind(Messages.outOfSyncAppMissingSummary, new String[] { moduleName, label });
        }

        private String[] convertToArray(String str) {
            if (str == null || str.trim().length() == 0)
                return null;

            List<String> list = new ArrayList<String>();

            StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s != null && s.length() > 0) {
                    list.add(s.trim());
                }
            }

            return list.toArray(new String[list.size()]);
        }
    }
}
