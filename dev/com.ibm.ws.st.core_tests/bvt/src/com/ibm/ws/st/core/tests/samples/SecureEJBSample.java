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

import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 *
 */
@TestCaseDescriptor(description = "Secure EJB sample test case", isStable = true)
@RunWith(AllTests.class)
public class SecureEJBSample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(SecureEJBSample.getOrderedTests());
        suite.setName(SecureEJBSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "ejbTest"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "ejbNegativeTest"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "updateEJBTest"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "ejbTest1"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "switchNonLooseConfig"));
        testSuite.addTest(TestSuite.createTest(SecureEJBSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "SecureEJBSample";

        //Set server location. Default format is SampleTesting + test name + server name
        SERVER_LOCATION = RESOURCE_LOCATION + SAMPLE_TEST_NAME + "/" + SERVER_NAME;

        System.out.println("Initializing Test Setup      :" + getClass().getName());

        //Run init from super class to setup resource folders.
        init();

        //create liberty runtime. Runtime name initialized from super class
        createRuntime();

        //Create server
        createServer();

        //Not sure why we need to create vm ?????
        createVM(JDK_NAME);

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "SecureEJBSample", "SecureEJBSample_EJB", "SecureEJBSample_WEB" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("SecureEJBSample");

    }

    @Test
    // Simple SecureEJB test.
    public void ejbTest() throws Exception {
        testPingSecureWebPage("SecureEJBSample", "In SecureEJBServlet, Hello Secure EJB World.", "user1:user1pwd");

    }

    @Test
    // Simple Negative SecureEJB test.
    public void ejbNegativeTest() throws Exception {

        testPingSecureWebPage("SecureEJBSample", " CWWKS9400A: Authorization failed for user", "user2:user2pwd");

    }

    @Test
    // Update SecureEJB Test
    public void updateEJBTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("ejbModule/wasdev/sample/ejb/SampleSecureStatelessBean.java"), getProject("SecureEJBSample_EJB"),
                   "ejbModule/wasdev/sample/ejb/SampleSecureStatelessBean.java", 5000);
        testPingSecureWebPage("SecureEJBSample", "In SecureEJBServlet, Hello Secure EJB World Updated", "user1:user1pwd");

    }

    @Test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    // Simple SecureEJB test.
    public void ejbTest1() throws Exception {
        testPingSecureWebPage("SecureEJBSample", "In SecureEJBServlet, Hello Secure EJB World Updated", "user1:user1pwd");

    }

    @Test
    public void switchNonLooseConfig() throws Exception {
        switchconfig(false);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), false);

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("SecureEJBSample", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
