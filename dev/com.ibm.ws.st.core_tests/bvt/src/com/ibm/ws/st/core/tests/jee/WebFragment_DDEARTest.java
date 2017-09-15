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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/*
 * Test web fragment support.  Story 52156
 */
@TestCaseDescriptor(description = "Test publish of web fragment in enterprise app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class WebFragment_DDEARTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebFragment_DDEARTest.getOrderedTests());
        suite.setName(WebFragment_DDEARTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testWebProject"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testJSPinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testServletinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testHTMLinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testUpdateHTMLinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testUpdateJSPinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testUpdateServletinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testAddHTMLinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testAddJSPinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testAddServletinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "addFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testJSPinFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testServletinFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testHTMLinFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "removeFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testJSPinRemovedFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testServletinRemovedFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "testHTMLinRemovedFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_DDEARTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/WebFragment/dd_updated_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: WebFragment_DDEARTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/WebFragment"), new String[] { "WebFrag_DDEAR", "WebFrag_DDFrag1", "WebFrag_DDFrag2", "WebFrag_DDWeb" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("WebFrag_DDEAR");
    }

    @Test
    // test jsp in the web project to confirm the web is working
    public void testWebProject() throws Exception {
        testPingWebPage("WebFrag_DDWeb/webDD.jsp", "webDD.jsp");
    }

    /*
     * Test fragment 1
     */
    @Test
    // test jsp in web fragment
    public void testJSPinFrag1() throws Exception {
        testPingWebPage("WebFrag_DDWeb/ddfrag1_a.jsp", "ddfrag1_a.jsp");
    }

    @Test
    // test servlet in web fragment
    public void testServletinFrag1() throws Exception {
        testPingWebPage("WebFrag_DDWeb/S1", "ddfrag1.S1");
    }

    @Test
    // test html in web fragment
    public void testHTMLinFrag1() throws Exception {
        testPingWebPage("WebFrag_DDWeb/ddfrag1_a.html", "ddfrag1_a.html");
    }

    @Test
    // test update html in web fragment
    public void testUpdateHTMLinFrag1() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("new_ddfrag1_a.html"),
                   getProject("WebFrag_DDFrag1"), "src/META-INF/resources/ddfrag1_a.html", 2000);
        testPingWebPage("WebFrag_DDWeb/ddfrag1_a.html", "ddfrag1_new_a.html");
    }

    @Test
    // test update jsp in web fragment
    public void testUpdateJSPinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("new_ddfrag1_a.jsp"),
                   getProject("WebFrag_DDFrag1"), "src/META-INF/resources/ddfrag1_a.jsp", 2000);
        testPingWebPage("WebFrag_DDWeb/ddfrag1_a.jsp", "ddfrag1_new_a.jsp");
    }

    @Test
    // test update servlet in web fragment
    public void testUpdateServletinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("new_S1.java"),
                   getProject("WebFrag_DDFrag1"), "src/ddfrag1/S1.java", 5000);
        testPingWebPage("WebFrag_DDWeb/S1", "frag1.new.S1");
    }

    @Test
    // test add new html to web fragment
    public void testAddHTMLinFrag1() throws Exception {
        print("Test add html/jsp/servlet");
        updateFile(getUpdatedFileFolder().append("ddfrag1_b.html"),
                   getProject("WebFrag_DDFrag1"), "src/META-INF/resources/ddfrag1_b.html", 2000);
        testPingWebPage("WebFrag_DDWeb/ddfrag1_b.html", "ddfrag1_b.html");
    }

    @Test
    // test add new jsp to web fragment
    public void testAddJSPinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("ddfrag1_b.jsp"),
                   getProject("WebFrag_DDFrag1"), "src/META-INF/resources/ddfrag1_b.jsp", 2000);
        testPingWebPage("WebFrag_DDWeb/ddfrag1_b.jsp", "ddfrag1_b.jsp");
    }

    @Test
    // test add new servlet to web fragment
    public void testAddServletinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("S3.java"),
                   getProject("WebFrag_DDFrag1"), "src/ddfrag1/S3.java", 2000);
        testPingWebPage("WebFrag_DDWeb/S3", "ddfrag1.S3");
    }

    /*
     * Test add a fragment to published web
     */
    @Test
    // Add frag2 to the web
    public void addFrag2() throws Exception {
        print("Add a new fragment to the web.");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_add_frag2"),
                   getProject("WebFrag_DDWeb"), ".settings/org.eclipse.wst.common.component",
                   2000);
        wait("Wait 5 seconds before ping.", 5000);
    }

    @Test
    // test jsp in added web fragment
    public void testJSPinFrag2() throws Exception {
        testPingWebPage("WebFrag_DDWeb/ddfrag2_a.jsp", "ddfrag2_a.jsp");
    }

    @Test
    // test servlet in added web fragment
    public void testServletinFrag2() throws Exception {
        testPingWebPage("WebFrag_DDWeb/S2", "frag2.S2");
    }

    @Test
    // test html in added web fragment
    public void testHTMLinFrag2() throws Exception {
        testPingWebPage("WebFrag_DDWeb/ddfrag2_a.html", "ddfrag2_a.html");
    }

    /*
     * Test remove a fragment to published web
     */
    @Test
    // Remove frag1 to the web
    public void removeFrag2() throws Exception {
        print("Remove the fragment 1 from the web.");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_remove_frag2"),
                   getProject("WebFrag_DDWeb"), ".settings/org.eclipse.wst.common.component",
                   2000);
        wait("Wait 5 seconds before ping.", 5000);
    }

    @Test
    // test jsp in removed web fragment
    public void testJSPinRemovedFrag2() throws Exception {
        testWebPageNotFound("WebFrag_DDWeb/ddfrag2_a.jsp", "ddfrag2_a.jsp");
    }

    @Test
    // test servlet in removed web fragment
    public void testServletinRemovedFrag2() throws Exception {
        testWebPageNotFound("WebFrag_DDWeb/S2", "frag2.S2");
    }

    @Test
    // test html in removed web fragment
    public void testHTMLinRemovedFrag2() throws Exception {
        testWebPageNotFound("WebFrag_DDWeb/ddfrag2_a.html", "ddfrag2_a.html");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("WebFrag_DDEAR", 2500); //in case the publish is not done. 
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: WebFragment_DDEARTest\n", 5000);
    }
}