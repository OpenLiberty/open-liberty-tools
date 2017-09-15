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

@TestCaseDescriptor(description = "Test publish of web app with no deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishWarNoDD extends JEEPublishWarBase {

    @Override
    public String getResourceDir() {
        return "jee/JEEPublishWarNoDD";
    }

    @Override
    public String getClassName() {
        return JEEPublishWarNoDD.class.getSimpleName();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishWarNoDD.getOrderedTests());
        suite.setName(JEEPublishWarNoDD.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "addUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "removeUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDD.class, "doTearDown"));

        return testSuite;
    }

}