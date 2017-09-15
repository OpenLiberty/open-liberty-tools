/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test publish JEE 7 Web application on server
 */
@TestCaseDescriptor(description = "Test publish for JEE7 web app", isStable = true)
@RunWith(AllTests.class)
public class JEE7PublishTest extends JEETestBase {

    public String runtimeName = JEE7PublishTest.class.getCanonicalName();

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEE7PublishTest.getOrderedTests());
        suite.setName(JEE7PublishTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEE7PublishTest.class, "doSetUp"));
        testSuite.addTest(TestSuite.createTest(JEE7PublishTest.class, "testPingJSP"));
        testSuite.addTest(TestSuite.createTest(JEE7PublishTest.class, "testPingServlet"));
        testSuite.addTest(TestSuite.createTest(JEE7PublishTest.class, "testPingHTML"));
        testSuite.addTest(TestSuite.createTest(JEE7PublishTest.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEE7PublishTest.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetUp() throws Exception {
        print("Starting test: " + getClass().getName());
        init();
        createRuntime(runtimeName);
        createServer();
        createVM();
        importProjects(new Path("jee/JEE7"), new String[] { "Web7", "Web7EAR" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("Web7EAR");
    }

    @Test
    // test simple servlet in a web module
    public void testPingServlet() throws Exception {
        testPingWebPage("Web7/S1", "Servlet");
    }

    @Test
    // test simple jsp in a web module
    public void testPingJSP() throws Exception {
        testPingWebPage("Web7/a.jsp", "jsp");
    }

    @Test
    // test simple html in a web module
    public void testPingHTML() throws Exception {
        testPingWebPage("Web7/a.html", "html");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateServlet() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("a.html"),
                   getProject("Web7"), "WebContent/a.html", 2000);
        testPingWebPage("Web7/a.html", "new_html");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("Web7EAR", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("jee/JEE7/updatedfiles");
    }
}
