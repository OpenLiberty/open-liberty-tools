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
package com.ibm.ws.st.core.internal.config.validation;

import org.eclipse.core.resources.IProject;

import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigVars;

public interface ICustomServerVariablesHandler {

    /**
     * Allows extensions to include custom variables
     * (eg. Maven projects that use the liberty-maven-plugin can specify bootstrap properties within the pom.xml file and
     * these variables should be resolved by the tools when available)
     *
     * @param globalVars
     * @param project the project containing the Liberty Maven project configuration
     */
    public void addCustomServerVariables(ConfigVars globalVars, IProject project);

    void addCustomServerVariables(ConfigVars configVars, WebSphereServerInfo serverInfo);

}
