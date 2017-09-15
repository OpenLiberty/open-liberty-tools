/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Translated messages.
 */
public class Messages extends NLS {
    public static String errorPublish;
    public static String errorDeleteFile;
    public static String errorCreateFolder;
    public static String errorWebSpecLevel;
    public static String errorEJBSpecLevel;
    public static String errorEARSpecLevel;
    public static String errorNotSupportedModuleInEAR;
    public static String errorEARMissingRequiredModules;
    public static String errorGettingModulesInEAR;
    public static String warningAppIsOnServer;
    public static String errorGenLooseConfigXML;
    public static String errorSharedLibProjectInfoIncomplete;
    public static String warningWebContextRootNotMatch;
    public static String warningWebContextRootNotMatchProjectName;
    public static String warningWebContextRootNotMatchXMI;
    public static String sharedLibraryLabel;
    public static String taskRemoveExteneralApp;

    static {
        NLS.initializeMessages(Activator.PLUGIN_ID + ".internal.Messages", Messages.class);
    }
}
