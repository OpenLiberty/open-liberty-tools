/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.etools.maven.liberty.integration.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {

    // Detection prompt
    public static String generationPromptMsg;

    // Preferences
    public static String prefComboText;

    // Maven
    public static String mavenTargetLabel;

    // Publishing
    public static String errorParentPOMLocationDoesNotExist;

    // Errors

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}