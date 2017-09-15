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

@TestCaseDescriptor(description = "Servlet sample test case", isStable = true)
@RunWith(AllTests.class)
public class ServletSample extends SampleTestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ServletSample.getOrderedTests());
        suite.setName(ServletSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "servletTest"));
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "updateServletTest"));
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "servletTestLooseConfig"));
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "switchNonLooseConfig"));
        testSuite.addTest(TestSuite.createTest(ServletSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "servlet";

        //Set server location. Default format is SampleTesting + test name + server name
        SERVER_LOCATION = RESOURCE_LOCATION + "/" + SERVER_NAME;

        System.out.println("Initializing Test Setup      :" + getClass().getName());

        //Run init from super class to setup resource folders.
        init();

        //create liberty runtime. Runtime name initialized from super class
        createRuntime();

        //Create server
        createServer();

        createVM(JDK_NAME);

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "ServletApp", "ServletAppEAR" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("ServletApp");

    }

    @Test
    // Simple Servlet test.
    public void servletTest() throws Exception {
        testPingWebPage("ServletApp", "Simple Servlet ran successfully");

    }

    @Test
    // Update Servlet test. File is replaced instead of updating string content
    public void updateServletTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("SimpleServlet.java"), getProject("ServletApp"), "src/wasdev/sample/servlet/SimpleServlet.java", 2000);
        testPingWebPage("ServletApp", "Simple Servlet Updated successfully");

    }

    @Test
    // switch config test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    // Simple Servlet Loose config test.
    public void servletTestLooseConfig() throws Exception {
        testPingWebPage("ServletApp", "Simple Servlet Updated successfully");

    }

    @Test
    // switch non loose config test
    public void switchNonLooseConfig() throws Exception {
        switchconfig(false);
        assertEquals(wsServer.isLooseConfigEnabled(), false);

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("ServletApp", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
