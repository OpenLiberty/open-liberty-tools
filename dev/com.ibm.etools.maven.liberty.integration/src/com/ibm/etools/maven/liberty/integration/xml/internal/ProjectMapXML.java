/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2017 All Rights Reserved
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.etools.maven.liberty.integration.xml.internal;

import com.ibm.ws.st.liberty.buildplugin.integration.xml.internal.AbstractProjectMapXML;
import com.ibm.etools.maven.liberty.integration.internal.Activator;

public class ProjectMapXML extends AbstractProjectMapXML {

    private static ProjectMapXML instance;

    private ProjectMapXML() {
        super(Activator.getLibertyMavenMetadataPath().append(PROJECT_MAP_FILE_NAME).toFile());
    }

    public static ProjectMapXML instance() {
        if (instance != null)
            return instance;
        instance = new ProjectMapXML();
        return instance;
    }

}
