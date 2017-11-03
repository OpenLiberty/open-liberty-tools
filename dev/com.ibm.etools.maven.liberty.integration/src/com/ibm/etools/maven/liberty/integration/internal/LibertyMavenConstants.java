/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.etools.maven.liberty.integration.internal;

public class LibertyMavenConstants {

    public static final String POM_FILE_NAME = "pom.xml";

    public static final String LIBERTY_ASSEMBLY_PROJECT_TYPE = "liberty-assembly";

    public enum ProjectType {
        LIBERTY_ASSEMBLY, STANDARD
    }
    
    // Same id as in org.eclipse.ui.navigator.navigatorContent extension
    public static final String LIBERTY_MAVEN_RUNTIME_CONTENT_ID = "com.ibm.ws.etools.maven.liberty.integration.runtime.content";

    // config xml constants
    public static final String LIBERTY_PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";
    public static final String LIBERTY_PLUGIN_CONFIG_PATH = "/target/" + LIBERTY_PLUGIN_CONFIG_XML;

    // preferences
    public static final String PROMPT_PREFERENCE = "libertyMavenPrompt";
}
