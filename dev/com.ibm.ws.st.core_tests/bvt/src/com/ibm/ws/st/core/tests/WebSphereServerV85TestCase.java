/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check WebSphere server V85", isStable = true)
@RunWith(AllTests.class)
public class WebSphereServerV85TestCase extends WebSphereServerTestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebSphereServerV85TestCase.getOrderedTests());
        suite.setName(WebSphereServerV85TestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPublish"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCanRun"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testRun"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCanStop"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testStop"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testAdaptServer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCleanable"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testAdaptServerBehaviour"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCleanOnStartup"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCanDebug"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testDebug"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCleaned"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCanStop2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testStop2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testWebSphereServer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testWebSphereServerBehaviour"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCreateClosedProject"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCreateWebModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testRuntimeTargetClasspathContainer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testRuntimeTargetClasspathContainerSorting"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCreateWebContent"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testGetModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testCanAddModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testHasModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testNotPublishedYet"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testStart"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPingWebModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testStop3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testAddModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testHasModule2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPublish2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPublishWorked"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testStart2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPingWebModule2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testStop4"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testRemoveModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testHasModule3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPublish3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "testPublishRemoved"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerV85TestCase.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected String getServerTypeId() {
        return WLPCommonUtil.SERVER_V85_ID;
    }
}