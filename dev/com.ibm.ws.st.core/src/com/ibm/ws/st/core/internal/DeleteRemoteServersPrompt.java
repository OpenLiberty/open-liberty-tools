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

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;

public class DeleteRemoteServersPrompt extends AbstractPrompt {
    IPromptIssue issue = null;

    DeleteRemoteServersPrompt(IServer server, IProject project) {
        issue = new DeleteServerIssue(server, project);
    }

    @Override
    public boolean isActive() {
        return issue != null;
    }

    @Override
    public IPromptIssue[] getIssues() {
        return new IPromptIssue[] { issue };
    }

    private static class DeleteServerIssue implements IPromptIssue {

        IServer server;
        IProject project;

        DeleteServerIssue(IServer server, IProject project) {
            this.server = server;
            this.project = project;
        }

        @Override
        public String getType() {
            return Messages.remoteServerDeletePromptIssue;
        }

        @Override
        public String getSummary() {
            return NLS.bind(Messages.remoteServerDeletePromptSummary, server.getId());
        }

        @Override
        public String getDetails() {
            return NLS.bind(Messages.remoteServerDeletePromptDetails, project.getName());
        }

        @Override
        public PromptAction[] getPossibleActions() {
            return new PromptAction[] { PromptAction.DELETE_SERVER_FILES, PromptAction.IGNORE };
        }

        @Override
        public PromptAction getDefaultAction() {
            return PromptAction.DELETE_SERVER_FILES;
        }
    }
}
