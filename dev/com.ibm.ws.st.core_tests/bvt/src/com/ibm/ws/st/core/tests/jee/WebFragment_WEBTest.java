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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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
@TestCaseDescriptor(description = "Test publish of web fragment in web app with no deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class WebFragment_WEBTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebFragment_WEBTest.getOrderedTests());
        suite.setName(WebFragment_WEBTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testWebProject"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testJSPinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testServletinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testHTMLinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testUpdateHTMLinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testUpdateJSPinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testUpdateServletinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testAddHTMLinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testAddJSPinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testAddServletinFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "addFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testJSPinFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testServletinFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testHTMLinFrag2"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "removeFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testJSPinRemovedFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testServletinRemovedFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testHTMLinRemovedFrag1"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "testDeleteFrag2Project"));
        testSuite.addTest(TestSuite.createTest(WebFragment_WEBTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/WebFragment/updated_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: WebFragment_WEBTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/WebFragment"), new String[] { "WebFrag_Frag1", "WebFrag_Frag2", "WebFrag_Web" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("WebFrag_Web");
    }

    @Test
    // test jsp in the web project to confirm the web is working
    public void testWebProject() throws Exception {
        testPingWebPage("WebFrag_Web/web.jsp", "web.jsp from WebFrag_Web");
    }

    /*
     * Test fragment 1
     */
    @Test
    // test jsp in web fragment
    public void testJSPinFrag1() throws Exception {
        testPingWebPage("WebFrag_Web/frag1_a.jsp", "frag1_a.jsp from WebFrag_Frag1");
    }

    @Test
    // test servlet in web fragment
    public void testServletinFrag1() throws Exception {
        testPingWebPage("WebFrag_Web/S1", "frag1_S1");
    }

    @Test
    // test html in web fragment
    public void testHTMLinFrag1() throws Exception {
        testPingWebPage("WebFrag_Web/a.html", "a.html from frag1");
    }

    @Test
    // test update html in web fragment
    public void testUpdateHTMLinFrag1() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("frag1_a.html"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/a.html", 2000);
        testPingWebPage("WebFrag_Web/a.html", "new a.html from frag1");
    }

    @Test
    // test update jsp in web fragment
    public void testUpdateJSPinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("new_frag1_a.jsp"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/frag1_a.jsp", 2000);
        testPingWebPage("WebFrag_Web/frag1_a.jsp", "new frag1_a.jsp from WebFrag_Frag1");
    }

    @Test
    // test update servlet in web fragment
    public void testUpdateServletinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("frag1_S1.java"),
                   getProject("WebFrag_Frag1"), "src/frag1/S1.java", 5000);
        testPingWebPage("WebFrag_Web/S1", "new frag1_S1");
    }

    @Test
    // test add new html to web fragment
    public void testAddHTMLinFrag1() throws Exception {
        print("Test add html/jsp/servlet");
        updateFile(getUpdatedFileFolder().append("frag1_b.html"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/b.html", 2000);
        testPingWebPage("WebFrag_Web/b.html", "b.html from frag1");
    }

    @Test
    // test add new jsp to web fragment
    public void testAddJSPinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("frag1_b.jsp"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/frag1_b.jsp", 2000);
        testPingWebPage("WebFrag_Web/frag1_b.jsp", "frag1_b.jsp from WebFrag_Frag1");
    }

    @Test
    // test add new servlet to web fragment
    public void testAddServletinFrag1() throws Exception {
        updateFile(getUpdatedFileFolder().append("frag1_S3.java"),
                   getProject("WebFrag_Frag1"), "src/frag1/S3.java", 2000);
        testPingWebPage("WebFrag_Web/S3", "frag1_S3");
    }

    /*
     * Test add a fragment to published web
     */
    @Test
    // Add frag2 to the web
    public void addFrag2() throws Exception {
        print("Add a new fragment to the web.");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_add_frag2"),
                   getProject("WebFrag_Web"), ".settings/org.eclipse.wst.common.component",
                   2000);
        wait("wait 5 seconds before ping.", 5000);
    }

    @Test
    // test jsp in added web fragment
    public void testJSPinFrag2() throws Exception {
        testPingWebPage("WebFrag_Web/frag2_a.jsp", "frag2_a.jsp from WebFrag_Frag2");
    }

    @Test
    // test servlet in added web fragment
    public void testServletinFrag2() throws Exception {
        testPingWebPage("WebFrag_Web/S2", "frag2_S2");
    }

    @Test
    // test html in added web fragment
    public void testHTMLinFrag2() throws Exception {
        testPingWebPage("WebFrag_Web/frag2_a.html", "a.html from frag2");
    }

    /*
     * Test remove a fragment to published web
     */
    @Test
    // Remove frag1 from the web
    public void removeFrag1() throws Exception {
        print("Remove the fragment 1 from the web.");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_remove_frag1"),
                   getProject("WebFrag_Web"), ".settings/org.eclipse.wst.common.component",
                   2000);
        wait("Wait 5 seconds before ping.", 5000);
    }

    @Test
    // test jsp in removed web fragment
    public void testJSPinRemovedFrag1() throws Exception {
        testWebPageNotFound("WebFrag_Web/frag1_a.jsp", "frag1_a.jsp from WebFrag_Frag1");
    }

    @Test
    // test servlet in removed web fragment
    public void testServletinRemovedFrag1() throws Exception {
        testWebPageNotFound("WebFrag_Web/S1", "frag1_S1");
    }

    @Test
    // test html in removed web fragment
    public void testHTMLinRemovedFrag1() throws Exception {
        testWebPageNotFound("WebFrag_Web/a.html", "a.html from frag1");
    }

    @Test
    // test delete frag2 project
    public void testDeleteFrag2Project() throws Exception {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = workspaceRoot.getProject("WebFrag_Frag2");
        project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
        forceBuild();
        safePublishIncremental(server);

        wait("Wait 5 seconds before ping.", 5000);
        testWebPageNotFound("WebFrag_Web/frag2_a.jsp", "frag2_a.jsp from WebFrag_Frag2");
        testWebPageNotFound("WebFrag_Web/S2", "frag2_S2");
        testWebPageNotFound("WebFrag_Web/frag2_a.html", "a.html from frag2");
        testPingWebPage("WebFrag_Web/web.jsp", "web.jsp from WebFrag_Web");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("WebFrag_Web", 2500); //in case the publish is not done. 
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: WebFragment_WEBTest\n", 5000);
    }
}