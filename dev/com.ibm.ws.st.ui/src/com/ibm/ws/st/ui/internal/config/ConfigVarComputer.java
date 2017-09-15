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
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.ui.IEditorInput;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.config.ConfigVars;

/**
 * Used for custom controls where the implicit local variables could be
 * changing. Implicit local variables are any attributes defined on the
 * current element or any that have a default value. The configuration
 * allows these to be used as variables. For example:
 * 
 * <application id="myApp" name="${id}" location="myApp.war" type="war"/>
 * 
 * Also delays gathering of global variables until requested.
 */
public class ConfigVarComputer {

    ConfigVars globalVars = null;
    IEditorInput editorInput;
    Element elem;
    String attrName;
    boolean globalOnly;

    /**
     * Create a config variable computer which will determine the available variables
     * given the editor input, the current element and the current attribute that
     * the user is editing.
     * 
     * @param editorInput - The editor input. Used to determine the URI of the document
     *            and thus which server or user directory it is associated with for getting the
     *            predefined variables.
     * @param elem - The element for which to get the variables. Used to get the DOM
     *            to search for global variables and to get the implicit local variables
     *            (from the elements attributes). May be null. If null then the variables
     *            will only include the set of predefined variables.
     * @param attrName - The current attribute to exclude from the variables (since it
     *            can't refer to itself). May be null.
     * @param globalOnly - Only include global variables.
     */
    public ConfigVarComputer(IEditorInput editorInput, Element elem, String attrName, boolean globalOnly) {
        this.editorInput = editorInput;
        this.elem = elem;
        this.attrName = attrName;
        this.globalOnly = globalOnly;
    }

    public ConfigVars getConfigVars() {
        if (globalOnly)
            return getGlobalVars();
        return ConfigUIUtils.getConfigVars(editorInput, elem, attrName, getGlobalVars());
    }

    public ConfigVars getGlobalVars() {
        if (globalVars == null) {
            globalVars = ConfigUIUtils.getConfigVars(editorInput, elem == null ? null : elem.getOwnerDocument());
        }
        return globalVars;
    }
}
