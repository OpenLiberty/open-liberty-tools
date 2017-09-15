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
package com.ibm.ws.st.core.tests.jee;

import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 *
 */
@TestCaseDescriptor(description = "Test publish for web app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEE6DD_WebTest_V85 extends JEE6DD_WebTest {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEE6DD_WebTest_V85.getOrderedTests());
        suite.setName(JEE6DD_WebTest_V85.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testJSPInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testHTMLInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testUpdateS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testUpdateHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testUpdateJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testAddS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testAddHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testAddJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testremoveS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testRemoveHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "testRemoveJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_WebTest_V85.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected String getServerTypeId() {
        return WLPCommonUtil.SERVER_V85_ID;
    }
}
