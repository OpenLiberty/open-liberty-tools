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

@TestCaseDescriptor(description = "Test publish of web app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishWarDD extends JEEPublishWarBase {

    @Override
    public String getResourceDir() {
        return "jee/JEEPublishWarDD";
    }

    @Override
    public String getClassName() {
        return JEEPublishWarDD.class.getSimpleName();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishWarDD.getOrderedTests());
        suite.setName(JEEPublishWarDD.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "addUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "removeUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "doTearDown"));

        return testSuite;
    }

    @Override
    public void updateWebXML() throws Exception {
        updateFile(getUpdatedFileFolder().append("webS1b.xml"),
                   getProject("Web1"), "WebContent/WEB-INF/web.xml", 2000);
    }

    @Override
    public void restoreWebXML() throws Exception {
        updateFile(getUpdatedFileFolder().append("web.xml"),
                   getProject("Web1"), "WebContent/WEB-INF/web.xml", 2000);
    }

}