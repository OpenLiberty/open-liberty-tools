/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.samples;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.FeatureUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 *
 */
@TestCaseDescriptor(description = "Non-persistent timer sample test case", isStable = true)
@RunWith(AllTests.class)
public class NonPersistentTimerSample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(NonPersistentTimerSample.getOrderedTests());
        suite.setName(NonPersistentTimerSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "servletTest"));
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "updateServletTest"));
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "servletTest1"));
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "switchNonLooseConfig"));
        testSuite.addTest(TestSuite.createTest(NonPersistentTimerSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "NonPersistentSample";

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

        // Add ejbLite feature (no facet so add explicitly to get the right version)
        FeatureUtil.addFeature(server, "ejbLite-3.2");

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "NonPersistentTimerSampleWeb" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("NonPersistentTimerSampleWeb");

    }

    @Test
    // Simple servlet test.
    public void servletTest() throws Exception {
        testPingWebPage("NonPersistentTimerSampleWeb", "EJB Asynchronous Methods and Timers");

    }

    @Test
    // Update servlet Test
    public void updateServletTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("TimerServlet.java"), getProject("NonPersistentTimerSampleWeb"),
                   "src/wasdev/sample/nonpersistenttimer/TimerServlet.java", 2000);
        testPingWebPage("NonPersistentTimerSampleWeb", "Updated EJB Asynchronous Methods and Timers");

    }

    @Test
    // switch to loose config
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    // Simple servlet test.
    public void servletTest1() throws Exception {
        testPingWebPage("NonPersistentTimerSampleWeb", "Updated EJB Asynchronous Methods and Timers");

    }

    @Test
    // switch back to non-loose config
    public void switchNonLooseConfig() throws Exception {
        switchconfig(false);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), false);

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("NonPersistentTimerSampleWeb", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
