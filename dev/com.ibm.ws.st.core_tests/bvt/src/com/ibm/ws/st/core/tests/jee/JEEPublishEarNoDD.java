/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish of enterprise app with no deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishEarNoDD extends JEEPublishEarBase {

    @Override
    public String getResourceDir() {
        return "jee/JEEPublishEarNoDD";
    }

    @Override
    public String getClassName() {
        return JEEPublishEarNoDD.class.getSimpleName();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishEarNoDD.getOrderedTests());
        suite.setName(JEEPublishEarNoDD.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "addUtilAndWebProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "removeUtilAndWebProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarNoDD.class, "doTearDown"));

        return testSuite;
    }

}