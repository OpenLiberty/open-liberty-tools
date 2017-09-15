/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.core.internal;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

import com.ibm.ws.st.core.internal.IPromptActionHandler;
import com.ibm.ws.st.core.internal.IPromptIssue;
import com.ibm.ws.st.core.internal.IPromptResponse;
import com.ibm.ws.st.core.internal.PromptAction;
import com.ibm.ws.st.core.internal.PromptHandler.AbstractPrompt;
import com.ibm.ws.st.core.internal.PublishHelper;

/**
 * Generic issue for a new container prompt. The details message is passed in to the constructor.
 */
public class NewContainerPrompt extends AbstractPrompt implements IPromptActionHandler {
    private IPromptIssue issue = null;
    private String detailsMessage = null;

    /**
     *
     */
    public NewContainerPrompt(String detailsMessage) {
        this.detailsMessage = detailsMessage;
    }

    /** {@inheritDoc} */
    @Override
    public void prePromptAction(List<IModule[]> publishedModules, PublishHelper helper, IProgressMonitor monitor) {

        issue = new NewContainerIssue(detailsMessage);

    }

    /** {@inheritDoc} */
    @Override
    public void postPromptAction(IPromptResponse response, PublishHelper helper) {
        // Empty

    }

    /** {@inheritDoc} */
    @Override
    public IPromptIssue[] getIssues() {
        return new IPromptIssue[] { issue };
    }

    @Override
    public boolean isActive() {
        return issue != null;
    }

    @Override
    public IPromptActionHandler getActionHandler() {
        return this;
    }

    private static class NewContainerIssue implements IPromptIssue {
        private final String detailsMessage;

        public NewContainerIssue(String detailsMessage) {
            this.detailsMessage = detailsMessage;
        }

        /** {@inheritDoc} */
        @Override
        public String getType() {
            return Messages.dockerNewContainerPromptType;
        }

        /** {@inheritDoc} */
        @Override
        public String getSummary() {
            return Messages.dockerNewContainerPromptSummary;
        }

        /** {@inheritDoc} */
        @Override
        public String getDetails() {
            return detailsMessage;
        }

        /** {@inheritDoc} */
        @Override
        public PromptAction[] getPossibleActions() {
            return new PromptAction[] { PromptAction.YES, PromptAction.NO };
        }

        /** {@inheritDoc} */
        @Override
        public PromptAction getDefaultAction() {
            return PromptAction.YES;
        }
    }
}
