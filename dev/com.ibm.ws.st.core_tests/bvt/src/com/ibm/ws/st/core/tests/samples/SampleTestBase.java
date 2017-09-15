/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.samples;

import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.tests.ToolsTestBase;

/**
 *
 */
public class SampleTestBase extends ToolsTestBase {

    //sample test name to be initialized in sub class
    protected static String SAMPLE_TEST_NAME = "";

    //Since each sample has different server settings. It is advisable to have different server setting for each samples. This value must
    //be initialized in child class
    protected static final String SERVER_NAME = "server";

    //this holds the location of server.xml used for that junit test
    protected static String SERVER_LOCATION = "";
    //This value is initialized partially. Needs to be updated in doSetup of child class
    protected static final String RESOURCE_LOCATION = "resources/sampleTesting/";

    protected static final String SHARED_RESOURCES = "/shared/resources";

    protected static final String SHARED_CONFIG = "/shared/config";

    protected static final String SHARED_FOLDER = "/shared";

    public String getSampleTestName() {
        return SAMPLE_TEST_NAME;
    }

    public String getServerName() {
        return SERVER_NAME;
    }

    public void createServer() throws Exception {
        createServer(runtime, getServerName(), getServerLocation());

    }

    /**
     * @return
     */
    private String getServerLocation() {
        return SERVER_LOCATION;
    }

    protected IPath getUpdatedFileFolder() {
        return resourceFolder.append("sampleTesting/" + SAMPLE_TEST_NAME + "/ws/updated_files");
    }
}
