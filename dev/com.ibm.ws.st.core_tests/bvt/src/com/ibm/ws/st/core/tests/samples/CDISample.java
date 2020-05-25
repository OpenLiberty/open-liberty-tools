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
@TestCaseDescriptor(description = "CDI sample test case", isStable = true)
@RunWith(AllTests.class)
public class CDISample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(CDISample.getOrderedTests());
        suite.setName(CDISample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(CDISample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(CDISample.class, "cdiTest"));
        testSuite.addTest(TestSuite.createTest(CDISample.class, "updateCDITest"));
        testSuite.addTest(TestSuite.createTest(CDISample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(CDISample.class, "cdiTestLooseConfig"));
        testSuite.addTest(TestSuite.createTest(CDISample.class, "switchNonLooseConfig"));
        testSuite.addTest(TestSuite.createTest(CDISample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "CDISample";

        //Set server location. Default format is SampleTesting + test name + server name
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

        // Add jsp feature (no facet so add explicitly to get the right version)
        FeatureUtil.addFeature(server, "jsp-2.3");

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "cdiApp", "cdiAppEAR" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("cdiApp");

    }

    @Test
    // Simple injected Bean test.
    public void cdiTest() throws Exception {
        testPingWebPage("cdiApp", "Congratulations! You successfully used CDI to inject a bean at the request scope");

    }

    @Test
    // Update CDI Test
    public void updateCDITest() throws Exception {
        updateFile(getUpdatedFileFolder().append("InjectedBean.java"), getProject("cdiApp"),
                   "src/wasdev/sample/cdi/InjectedBean.java", 2000);
        testPingWebPage("cdiApp", "Congratulations! You successfully used CDI to inject a bean at the request scope- Updated");

    }

    @Test
    // Update CDI Test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    // Simple CDI Loose config test.
    public void cdiTestLooseConfig() throws Exception {
        testPingWebPage("cdiApp", "Congratulations! You successfully used CDI to inject a bean at the request scope- Updated");

    }

    @Test
    // Update CDI Test
    public void switchNonLooseConfig() throws Exception {
        switchconfig(false);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), false);

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("cdiApp", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
