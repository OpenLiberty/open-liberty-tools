/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.ws.st.liberty.buildplugin.integration.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

    // Common
    public static String yesLabel;
    public static String noLabel;
    public static String libertyPromptTitle;

    // Detection prompt
    public static String generationPromptToggle;

    // Preferences
    public static String prefComboOptionAsk;

    // Actions
    public static String refreshAction;
    public static String createServerAction;
    public static String createServerActionDescription;
    public static String runtimeLabel;

    // Jobs
    public static String createJob;
    public static String deleteJob;
    public static String refreshJob;
    public static String invalidRuntime;
    public static String noRuntimeDetected;
    public static String createServerActionFailed;
    public static String stopServerActivity;
    public static String activityTimeout;
    public static String scanJob;

    // Errors
    public static String cannotBindApplication;
    public static String configFileNotFound;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}