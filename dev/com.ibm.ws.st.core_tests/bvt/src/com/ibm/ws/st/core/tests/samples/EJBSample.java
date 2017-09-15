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

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 *
 */
@TestCaseDescriptor(description = "EJB sample test case", isStable = true)
@RunWith(AllTests.class)
public class EJBSample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(EJBSample.getOrderedTests());
        suite.setName(EJBSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "ejbTest"));
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "updateEJBTest"));
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "ejbTestLooseConfig"));
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "switchNonLooseConfig"));
        testSuite.addTest(TestSuite.createTest(EJBSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "EJBSample";

        SERVER_LOCATION = RESOURCE_LOCATION + SERVER_NAME;

        System.out.println("Initializing Test Setup      :" + getClass().getName());

        //Run init from super class to setup resource folders.
        init();

        //create liberty runtime. Runtime name initialized from super class
        createRuntime();

        //Create server
        createServer();

        //Not sure why we need to create vm ?????
        createVM(JDK_NAME);

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "EJBApp", "EJBApp_EJB", "EJBApp_WEB" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("EJBApp");

    }

    @Test
    // Simple EJB test.
    public void ejbTest() throws Exception {
        testPingWebPage("EJBApp", "Hello EJB World.");

    }

    @Test
    // Update EJB Test
    public void updateEJBTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("ejbModule/wasdev/sample/ejb/SampleStatelessBean.java"), getProject("EJBApp_EJB"),
                   "ejbModule/wasdev/sample/ejb/SampleStatelessBean.java", 2000);
        testPingWebPage("EJBApp", "Hello EJB World Updated.");

    }

    @Test
    // Update EJB Test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);
    }

    @Test
    // Simple EJB Loose config test.
    public void ejbTestLooseConfig() throws Exception {
        testPingWebPage("EJBApp", "Hello EJB World Updated.");

    }

    @Test
    // Update EJB Test
    public void switchNonLooseConfig() throws Exception {
        switchconfig(false);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), false);
        testPingWebPage("EJBApp", "Hello EJB World Updated.");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EJBApp", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
