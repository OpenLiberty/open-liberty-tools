/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.featureDetection;

import org.eclipse.core.runtime.Path;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test checks that the required features are added when an app is added to the server.
 */
@TestCaseDescriptor(description = "Check required feature detection for imports", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportFeatureTest extends BaseFeatureTest {
    private static final String TEST_NAME = ImportFeatureTest.class.getSimpleName();
    private static final Path RESOURCE_PATH = new Path("FeatureTesting/" + TEST_NAME);
    private static final String SERVER_NAME = "ImportFeatureTestServer";
    private static final String CDI_PROJECT = "CDIWeb";
    private static final String BEAN_VALIDATION_PROJECT = "BeanValidationWeb";

    private static final String[] ALL_PROJECTS = { CDI_PROJECT, BEAN_VALIDATION_PROJECT };

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
    public void test02_testCDI() throws Exception {
        try {
            addApp(CDI_PROJECT);
            checkFeatures("cdi-2.0");
            testPingWebPage("CDIWeb/CDIWebServlet", "Hello from CDIWebServlet");
        } finally {
            cleanupAfterTest(CDI_PROJECT);
        }
    }

    @Test
    public void test03_testBeanValidation() throws Exception {
        try {
            addApp(BEAN_VALIDATION_PROJECT);
            checkFeatures("beanValidation-2.0");
            testPingWebPage("BeanValidationWeb/BeanValidationWebServlet", "Hello from BeanValidationWebServlet");
        } finally {
            cleanupAfterTest(BEAN_VALIDATION_PROJECT);
        }
    }

    @Test
    public void test99_doTearDown() throws Exception {
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + TEST_NAME + "\n", 1000);
    }

}