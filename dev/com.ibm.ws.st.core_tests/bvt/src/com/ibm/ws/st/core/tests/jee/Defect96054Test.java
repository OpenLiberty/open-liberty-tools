/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test case for defect 96054. Web application has extra library
 * added through deployment assembly.
 */
@TestCaseDescriptor(description = "Test publish for web application with extra library added through deployment assembly", isStable = true)
@RunWith(AllTests.class)
public class Defect96054Test extends JEETestBase {

    protected static final String SERVER_NAME = "96054Server";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(Defect96054Test.getOrderedTests());
        suite.setName(Defect96054Test.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(Defect96054Test.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(Defect96054Test.class, "testApp"));
        testSuite.addTest(TestSuite.createTest(Defect96054Test.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: Defect96054Test");
        init();
        createRuntime();
        importProjects(new Path("JEEDDtesting/Defects"), new String[] { "96054UserDir" });
        IProject project = getProject("96054UserDir");
        addUserDir(project);
        createServer(runtime, project.getLocation(), SERVER_NAME, null);
        createVM();
        importProjects(new Path("JEEDDtesting/Defects"), new String[] { "96054Servlet" });
    }

    @Test
    public void testApp() throws Exception {
        startServer();
        addApp("96054Servlet", false, 0);
        testPingWebPage("96054Servlet/TestServlet", "Sample Text");
    }

    @Test
    public void doTearDown() throws Exception {
        stopServer();
        removeApp("96054Servlet", 2500);
        WLPCommonUtil.cleanUp();
        wait("Ending test: Defect96054\n", 5000);
    }

}
