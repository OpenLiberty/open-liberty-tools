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
package com.ibm.ws.st.core.internal;

/**
 * Interface used to request that the UI plugin prompts the user for input.
 */
public abstract class PromptHandler {
    // constant value to use for responses that should always apply
    public static final int ALWAYS_APPLY = 100;

    // Style constants.
    public static final int STYLE_CANCEL = 0x00000001; // Show Cancel button
    public static final int STYLE_HELP = 0x00000010; // Show help button
    public static final int STYLE_QUESTION = 0x00000100; // Show question icon
    public static final int STYLE_WARN = 0x00001000; // Show warning icon

    public static abstract class AbstractPrompt {
        /**
         * The issues to be shown in the prompt dialog
         *
         * @return the issues
         */
        public abstract IPromptIssue[] getIssues();

        /**
         * Indicates whether to include a check box to
         * always apply the selected action
         *
         * @return check box flag
         */
        public boolean getApplyAlways() {
            return true;
        }

        /**
         * Indicates whether the prompt is active or not
         *
         * @return active flag
         */
        public boolean isActive() {
            return true;
        }

        /**
         * The handler that performs pre and post prompt actions
         *
         * @return action handler
         */
        public IPromptActionHandler getActionHandler() {
            return null;
        }
    }

    /**
     * Method used to request input from the user. An array of prompts is passed in,
     * and user responses are returned.
     *
     * @param message a message to the user, providing context
     * @param prompts a set of questions the user must reply to
     * @param specify the style bits of the prompt. STYLE_QUESTION and STYLE_WARN are mutually exculsive.
     * @return a list of response actions, one for each issue. ALWAYS_APPLY
     *         is added if the user wants to apply the same action if it
     *         occurs again.
     *         Returns <code>null</code> if the user cancels
     */
    public abstract IPromptResponse getResponse(String message, AbstractPrompt[] prompts, int style);

    /**
     * @param title
     * @param message
     * @param defaultVal
     * @return
     */
    public abstract boolean handleConfirmPrompt(String title, String message, boolean defaultVal);

}