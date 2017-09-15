/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
 *
 */
@TestCaseDescriptor(description = "Test publish for enterprise app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEE6DD_EARTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEE6DD_EARTest.getOrderedTests());
        suite.setName(JEE6DD_EARTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testJSPInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testHTMLInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testUpdateS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testUpdateHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testUpdateJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testAddS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testAddHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testAddJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testremoveS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testRemoveHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "testRemoveJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DD_EARTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/ws_1/dd_updated_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClass().getName());
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/ws_1"), new String[] { "Web30DD_1", "EAR6DD_1" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("EAR6DD_1");
    }

    @Test
    // test simple servlet in a web module
    public void testS1InWeb1() throws Exception {
        testPingWebPage("Web30DD_1/S1", "ddS1");
    }

    @Test
    // test simple jsp in a web module
    public void testJSPInWeb1() throws Exception {
        testPingWebPage("Web30DD_1/a.jsp", "a.jsp");
    }

    @Test
    // test simple html in a web module
    public void testHTMLInWeb1() throws Exception {
        testPingWebPage("Web30DD_1/a.html", "a.html");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateS1InWeb1() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("S1.java"),
                   getProject("Web30DD_1"), "src/web30dd/S1.java", 2000);
        testPingWebPage("Web30DD_1/S1", "new_S1");
    }

    @Test
    // test a update html in a web module
    public void testUpdateHTMLWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("a.html"),
                   getProject("Web30DD_1"), "WebContent/a.html", 2000);
        testPingWebPage("Web30DD_1/a.html", "new_a.html");
    }

    @Test
    // test update jsp in a web module
    public void testUpdateJSPWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("a.jsp"),
                   getProject("Web30DD_1"), "WebContent/a.jsp", 2000);
        testPingWebPage("Web30DD_1/a.jsp", "new_a.jsp");
    }

    @Test
    // test add a servlet to a web module
    public void testAddS2InWeb1() throws Exception {
        print("Test add new");
        updateFile(getUpdatedFileFolder().append("S2.java"),
                   getProject("Web30DD_1"), "src/web30dd/S2.java", 2000);
        testPingWebPage("Web30DD_1/S2", "S2");
    }

    @Test
    // test a update html in a web module
    public void testAddHTMLWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("b.html"),
                   getProject("Web30DD_1"), "WebContent/b.html", 2000);
        testPingWebPage("Web30DD_1/b.html", "b.html");
    }

    @Test
    // test update jsp in a web module
    public void testAddJSPWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("b.jsp"),
                   getProject("Web30DD_1"), "WebContent/b.jsp", 2000);
        testPingWebPage("Web30DD_1/b.jsp", "b.jsp");
    }

    @Test
    // test remove a servlet from a web module
    public void testremoveS2InWeb1() throws Exception {
        print("Test remove");
        deleteFile(getProject("Web30DD_1"), "src/web30dd/S2.java", 2000);
        testWebPageNotFound("Web30DD_1/S2", "S2");
    }

    @Test
    // test remove a html from a web module
    public void testRemoveHTMLWeb1() throws Exception {
        deleteFile(getProject("Web30DD_1"), "WebContent/b.html", 2000);
        testWebPageNotFound("Web30DD_1/b.html", "b.html");
    }

    @Test
    // test remove jsp from a web module
    public void testRemoveJSPWeb1() throws Exception {
        deleteFile(getProject("Web30DD_1"), "WebContent/b.jsp", 2000);
        testWebPageNotFound("Web30DD_1/b.jsp", "b.jsp");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EAR6DD_1", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
