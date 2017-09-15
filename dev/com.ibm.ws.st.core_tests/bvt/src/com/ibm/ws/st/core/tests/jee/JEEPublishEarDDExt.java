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

@TestCaseDescriptor(description = "Test publish of enterprise app in an external folder with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishEarDDExt extends JEEPublishEarBase {

    @Override
    public String getResourceDir() {
        return "jee/JEEPublishEarDD";
    }

    @Override
    public String getClassName() {
        return JEEPublishEarDDExt.class.getSimpleName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean copyProjects() {
        return false;
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishEarDDExt.getOrderedTests());
        suite.setName(JEEPublishEarDDExt.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "addUtilAndWebProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "removeUtilAndWebProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarDDExt.class, "doTearDown"));

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