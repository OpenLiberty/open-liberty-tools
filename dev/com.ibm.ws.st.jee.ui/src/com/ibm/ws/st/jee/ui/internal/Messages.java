/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {
    public static String browse;
    public static String remove;
    public static String add;
    public static String sharedLibDescription;
    public static String sharedLibId;
    public static String sharedLibDirectory;
    public static String sharedLibBrowseMessage;
    public static String sharedLibTitle;
    public static String sharedLibReferences;
    public static String sharedLibExisting;
    public static String sharedLibWorkspace;
    public static String sharedLibProject;
    public static String sharedLibServer;
    public static String sharedLibNone;
    public static String sharedLibConfirmRemoveTitle;
    public static String sharedLibConfirmRemoveMessage;
    public static String sharedLibServerUpdateFailedTitle;
    public static String sharedLibServerUpdateFailedMessage;
    public static String sharedLibAPIVisibilityLabel;
    public static String sharedLibAPIVisibilityAPI;
    public static String sharedLibAPIVisibilityIBMAPI;
    public static String sharedLibAPIVisibilitySpec;
    public static String sharedLibAPIVisibilityThirdParty;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}
