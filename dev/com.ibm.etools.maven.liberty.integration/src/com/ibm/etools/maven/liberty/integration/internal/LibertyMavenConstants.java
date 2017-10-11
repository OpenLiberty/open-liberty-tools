/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.etools.maven.liberty.integration.internal;

public class LibertyMavenConstants {

    public static final String POM_FILE_NAME = "pom.xml";

    public static final String PROFILE_ID = "profileId";
    public static final String JVM_PARAM = "param";

    public static final String LIBERTY_ASSEMBLY_PROJECT_TYPE = "liberty-assembly";

    public enum ProjectType {
        LIBERTY_ASSEMBLY, STANDARD
    }

    // config xml constants
    public static final String LIBERTY_PLUGIN_CONFIG_XML = "liberty-plugin-config.xml";
    public static final String LIBERTY_PLUGIN_CONFIG_PATH = "/target/" + LIBERTY_PLUGIN_CONFIG_XML;

    // preferences
    public static final String PROMPT_PREFERENCE = "libertyMavenPrompt";
}
