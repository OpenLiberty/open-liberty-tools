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
@TestCaseDescriptor(description = "Test publish of web fragment in enterprise app with no deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class WebFragment_EARTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebFragment_EARTest.getOrderedTests());
        suite.setName(WebFragment_EARTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testWebProjectInEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testJSPinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testServletinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testHTMLinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testUpdateHTMLinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testUpdateJSPinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testUpdateServletinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testAddHTMLinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testAddJSPinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testAddServletinFrag1InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "addFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testJSPinFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testServletinFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testHTMLinFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "removeFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testJSPinRemovedFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testServletinRemovedFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testHTMLinRemovedFrag2InEAR"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "testDeleteFrag1Project"));
        testSuite.addTest(TestSuite.createTest(WebFragment_EARTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/WebFragment/updated_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: WebFragment_EARTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/WebFragment"), new String[] { "WebFrag_EAR", "WebFrag_Frag1", "WebFrag_Frag2", "WebFrag_Web" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("WebFrag_EAR");
    }

    @Test
    // test jsp in the web project to confirm the web is working
    public void testWebProjectInEAR() throws Exception {
        testPingWebPage("WebFrag_Web/web.jsp", "web.jsp from WebFrag_Web");
    }

    /*
     * Test fragment 1
     */
    @Test
    // test jsp in web fragment
    public void testJSPinFrag1InEAR() throws Exception {
        testPingWebPage("WebFrag_Web/frag1_a.jsp", "frag1_a.jsp from WebFrag_Frag1");
    }

    @Test
    // test servlet in web fragment
    public void testServletinFrag1InEAR() throws Exception {
        testPingWebPage("WebFrag_Web/S1", "frag1_S1");
    }

    @Test
    // test html in web fragment
    public void testHTMLinFrag1InEAR() throws Exception {
        testPingWebPage("WebFrag_Web/a.html", "a.html from frag1");
    }

    @Test
    // test update html in web fragment
    public void testUpdateHTMLinFrag1InEAR() throws Exception {
        print("Test update.");
        updateFile(getUpdatedFileFolder().append("frag1_a.html"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/a.html", 2000);
        testPingWebPage("WebFrag_Web/a.html", "new a.html from frag1");
    }

    @Test
    // test update jsp in web fragment
    public void testUpdateJSPinFrag1InEAR() throws Exception {
        updateFile(getUpdatedFileFolder().append("new_frag1_a.jsp"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/frag1_a.jsp", 2000);
        testPingWebPage("WebFrag_Web/frag1_a.jsp", "new frag1_a.jsp from WebFrag_Frag1");
    }

    @Test
    // test update servlet in web fragment
    public void testUpdateServletinFrag1InEAR() throws Exception {
        updateFile(getUpdatedFileFolder().append("frag1_S1.java"),
                   getProject("WebFrag_Frag1"), "src/frag1/S1.java", 2000);
        testPingWebPage("WebFrag_Web/S1", "new frag1_S1");
    }

    @Test
    // test add html to web fragment
    public void testAddHTMLinFrag1InEAR() throws Exception {
        updateFile(getUpdatedFileFolder().append("frag1_b.html"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/b.html", 2000);
        testPingWebPage("WebFrag_Web/b.html", "b.html from frag1");
    }

    @Test
    // test add jsp to web fragment
    public void testAddJSPinFrag1InEAR() throws Exception {
        print("Test add new jsp/hmtl/servlet");
        updateFile(getUpdatedFileFolder().append("frag1_b.jsp"),
                   getProject("WebFrag_Frag1"), "src/META-INF/resources/frag1_b.jsp", 2000);
        testPingWebPage("WebFrag_Web/frag1_b.jsp", "frag1_b.jsp from WebFrag_Frag1");
    }

    @Test
    // test add servlet to web fragment
    public void testAddServletinFrag1InEAR() throws Exception {
        updateFile(getUpdatedFileFolder().append("frag1_S3.java"),
                   getProject("WebFrag_Frag1"), "src/frag1/S3.java", 2000);
        testPingWebPage("WebFrag_Web/S3", "frag1_S3");
    }

    /*
     * Test add a fragment to published web
     */
    @Test
    // Add frag2 to the web
    public void addFrag2InEAR() throws Exception {
        print("Add a new fragment to the web.");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_add_frag2"),
                   getProject("WebFrag_Web"), ".settings/org.eclipse.wst.common.component",
                   2000);
        wait("Wait 5 seconds before ping.", 5000);
    }

    @Test
    // test jsp in added web fragment
    public void testJSPinFrag2InEAR() throws Exception {
        testPingWebPage("WebFrag_Web/frag2_a.jsp", "frag2_a.jsp from WebFrag_Frag2");
    }

    @Test
    // test servlet in added web fragment
    public void testServletinFrag2InEAR() throws Exception {
        testPingWebPage("WebFrag_Web/S2", "frag2_S2");
    }

    @Test
    // test html in added web fragment
    public void testHTMLinFrag2InEAR() throws Exception {
        testPingWebPage("WebFrag_Web/frag2_a.html", "a.html from frag2");
    }

    /*
     * Test remove a fragment to published web
     */
    @Test
    // Remove frag2 to the web
    public void removeFrag2InEAR() throws Exception {
        print("Remove the fragment 2 from the web.");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_remove_frag2"),
                   getProject("WebFrag_Web"), ".settings/org.eclipse.wst.common.component",
                   2000);
        wait("Wait 5 seconds before ping.", 5000);
    }

    @Test
    // test jsp in removed web fragment
    public void testJSPinRemovedFrag2InEAR() throws Exception {
        testWebPageNotFound("WebFrag_Web/frag2_a.jsp", "frag2_a.jsp from WebFrag_Frag2");
    }

    @Test
    // test servlet in removed web fragment
    public void testServletinRemovedFrag2InEAR() throws Exception {
        testWebPageNotFound("WebFrag_Web/S2", "frag2_S2");
    }

    @Test
    // test html in removed web fragment
    public void testHTMLinRemovedFrag2InEAR() throws Exception {
        testWebPageNotFound("WebFrag_Web/frag2_a.html", "a.html from frag2");
    }

    @Test
    // test delete frag1 project
    public void testDeleteFrag1Project() throws Exception {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = workspaceRoot.getProject("WebFrag_Frag1");
        project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
        forceBuild();
        safePublishIncremental(server);

        wait("Wait 5 seconds before ping.", 5000);
        testWebPageNotFound("WebFrag_Web/a.html", "new a.html from frag1");
        testWebPageNotFound("WebFrag_Web/frag1_a.jsp", "new frag1_a.jsp from WebFrag_Frag1");
        testWebPageNotFound("WebFrag_Web/S1", "new frag1_S1");
        testPingWebPage("WebFrag_Web/web.jsp", "web.jsp from WebFrag_Web");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("WebFrag_EAR", 2500); //in case the publish is not done. 
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: WebFragment_EARTest\n", 5000);
    }
}