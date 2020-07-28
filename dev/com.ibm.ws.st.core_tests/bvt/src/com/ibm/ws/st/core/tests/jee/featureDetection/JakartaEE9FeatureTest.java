/*
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.core.tests.jee.featureDetection;

import org.eclipse.core.runtime.Path;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test if Jakarta EE9 features are added automatically when an app is added to the server.
 *
 * Expected results: given we decide to disable all Jakarta EE9 features,
 * so Jakarta EE9 features shouldn't be added automatically.
 */
@TestCaseDescriptor(description = "Check required feature detection for Jakarta EE9 features", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JakartaEE9FeatureTest extends BaseFeatureTest {
    private static final String TEST_NAME = JakartaEE9FeatureTest.class.getSimpleName();
    private static final Path RESOURCE_PATH = new Path("FeatureTesting/" + TEST_NAME);
    private static final String SERVER_NAME = "JakartaEE9FeatureTestServer";
    private static final String JSP_PROJECT = "JSP";

    private static final String[] ALL_PROJECTS = { JSP_PROJECT };

    @Test
    public void test01_doSetup() throws Exception {
        print("Starting test: " + TEST_NAME);
        init();
        createRuntime();
        createServer(runtime, SERVER_NAME, "resources/" + RESOURCE_PATH + "/" + SERVER_NAME);
        createVM(JDK_NAME);
        importProjects(RESOURCE_PATH, ALL_PROJECTS);
        startServer();
    }

    @Test
    public void test02_testJSP() throws Exception {
        try {
            addApp(JSP_PROJECT);
            checkFeatures("jsp-2.3");
            testPingWebPage("JSP/test.jsp", "Hello from test.jsp");
        } finally {
            cleanupAfterTest(JSP_PROJECT);
        }
    }

    @Test
    public void test99_doTearDown() throws Exception {
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + TEST_NAME + "\n", 1000);
    }
}
