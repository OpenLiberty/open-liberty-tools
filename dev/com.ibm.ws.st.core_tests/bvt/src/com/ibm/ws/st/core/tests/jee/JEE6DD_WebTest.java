/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

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
@TestCaseDescriptor(description = "Test publish for web app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEE6DD_WebTest extends JEE6DD_EARTest {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEE6DD_WebTest.getOrderedTests());
        suite.setName(JEE6DD_WebTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testJSPInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testHTMLInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testUpdateS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testUpdateHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testUpdateJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testAddS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testAddHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testAddJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testremoveS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testRemoveHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "testRemoveJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest.class, "doTearDown"));

        return testSuite;
    }

    @Override
    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClass().getName());
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/ws_1"), new String[] { "Web30DD_1" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("Web30DD_1");
    }

    @Override
    @Test
    public void doTearDown() throws Exception {
        removeApp("Web30DD_1", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }
}
