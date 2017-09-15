/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import java.util.List;

import org.eclipse.core.runtime.Status;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.ConsoleLineTrackerHelper;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Simulate update config actions from console line tracker", isStable = false)
@RunWith(AllTests.class)
public class ConsoleTrackerTestCase extends ToolsTestBase {
    private static final String FEATURE_JSP = "jsp";
    private static final String FEATURE_JNDI = "jndi";
    private static final String FEATURE_JDBC = "jdbc";

    protected static final String SERVER_NAME = "consoleServer";
    protected static final String SERVER_LOCATION = "resources/ConsoleTrackerTesting/" + SERVER_NAME;

    protected static WebSphereRuntime wsRuntime;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ConsoleTrackerTestCase.getOrderedTests());
        suite.setName(ConsoleTrackerTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJSPMissing"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJSPExist1"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJSPExist2"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJNDIMissing"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJNDIExist1"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJDBCMissing"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJDBCExist"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "testAddJNDIExist2"));
        testSuite.addTest(TestSuite.createTest(ConsoleTrackerTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ConsoleTrackerTestCase");
        init();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, SERVER_LOCATION);
        createVM(JDK_NAME);
        wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);

        wait("Wait 3 seconds before running tests", 3000);
    }

    @Test
    public void testAddJSPMissing() throws Exception {
        assertTrue("Failed to add jsp feature", Status.OK_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JSP));
        wait("Wait 3 seconds for server config update", 3000);
    }

    @Test
    public void testAddJSPExist1() throws Exception {
        assertTrue("Expecting jsp feature to be found in server configuration",
                   ConsoleLineTrackerHelper.EXIST_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JSP));
    }

    @Test
    public void testAddJSPExist2() throws Exception {
        ConfigurationFile configFile = wsServer.getConfiguration();
        // replace jsp feature with jsf feature
        List<String> features = configFile.getFeatures();
        for (String feature : features) {
            if (feature.startsWith("jsp-")) {
                configFile.replaceFeature(feature, "jsf-2.2");
                break;
            }
        }
        configFile.save(null);
        wait("Wait for jsf-2.0 feature replace", 2000);
        assertTrue("Expecting jsp feature to be found in server configuration",
                   ConsoleLineTrackerHelper.EXIST_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JSP));
    }

    @Test
    public void testAddJNDIMissing() throws Exception {
        assertTrue("Failed to add jndi feature", Status.OK_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JNDI));
        wait("Wait 3 seconds for server config update", 3000);
    }

    @Test
    public void testAddJNDIExist1() throws Exception {
        assertTrue("Expecting jndi feature to be found in server configuration",
                   ConsoleLineTrackerHelper.EXIST_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JNDI));
    }

    public void testAddJDBCMissing() throws Exception {
        assertTrue("Failed to add jdbc feature", Status.OK_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JDBC));
        wait("Wait 3 seconds for server config update", 3000);
    }

    @Test
    public void testAddJDBCExist() throws Exception {
        assertTrue("Expecting jdbc feature to be found in server configuration", ConsoleLineTrackerHelper.isFeatureExist(wsServer, FEATURE_JDBC));
    }

    @Test
    public void testAddJNDIExist2() throws Exception {
        assertTrue("Expecting jndi feature to be found in server configuration",
                   ConsoleLineTrackerHelper.EXIST_STATUS == ConsoleLineTrackerHelper.addFeatureSupport(wsServer, FEATURE_JNDI));
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: ConsoleTrackerTestCase\n");
    }
}