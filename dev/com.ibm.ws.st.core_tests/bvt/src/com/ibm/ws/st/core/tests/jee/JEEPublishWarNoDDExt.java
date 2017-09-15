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

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test publish of web app in an external folder with no deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishWarNoDDExt extends JEEPublishWarBase {

    @Override
    public String getResourceDir() {
        return "jee/JEEPublishWarNoDD";
    }

    @Override
    public String getClassName() {
        return JEEPublishWarNoDDExt.class.getSimpleName();
    }

    @Override
    public boolean copyProjects() {
        return false;
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishWarNoDDExt.getOrderedTests());
        suite.setName(JEEPublishWarNoDDExt.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "addUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "removeUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarNoDDExt.class, "doTearDown"));

        return testSuite;
    }

}