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

@TestCaseDescriptor(description = "Test publish of enterprise app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishEarDD extends JEEPublishEarBase {

    @Override
    public String getResourceDir() {
        return "jee/JEEPublishEarDD";
    }

    @Override
    public String getClassName() {
        return JEEPublishEarDD.class.getSimpleName();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishEarDD.getOrderedTests());
        suite.setName(JEEPublishEarDD.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "addUtilAndWebProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "removeUtilAndWebProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDD.class, "doTearDown"));

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