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

package com.ibm.ws.st.liberty.gradle.internal;

public class LibertyGradleConstants {

    public static final String POM_FILE_NAME = "pom.xml";

    public enum ProjectType {
        STANDARD
    }

    // Same id as in org.eclipse.ui.navigator.navigatorContent extension
    public static final String LIBERTY_GRADLE_RUNTIME_CONTENT_ID = "com.ibm.ws.st.liberty.gradle.runtime.content";

    public static final String BUILDSHIP_GRADLE_PROJECT_NATURE = "org.eclipse.buildship.core.gradleprojectnature";
    
    // config xml constants
    public static final String LIBERTY_PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";
    public static final String LIBERTY_PLUGIN_CONFIG_PATH = "/build/" + LIBERTY_PLUGIN_CONFIG_XML;

    // preferences
    public static final String PROMPT_PREFERENCE = "libertyGradlePrompt";
    
    // tasks
    public static final String BUILD_TASK = "build";
    public static final String ASSEMBLE_TASK = "assemble";
    public static final String INSTALL_APPS_TASK = "installApps";
    
    // arguments
    public static final String[] SKIP_TESTS_ARGS = new String[] {"--exclude-task", "test"};
    
}
