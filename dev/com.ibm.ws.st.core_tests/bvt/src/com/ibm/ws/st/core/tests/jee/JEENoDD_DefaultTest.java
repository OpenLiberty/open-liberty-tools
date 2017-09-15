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

@TestCaseDescriptor(description = "JEE No DD publishing tests", isStable = true)
@RunWith(AllTests.class)
public class JEENoDD_DefaultTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEENoDD_DefaultTest.getOrderedTests());
        suite.setName(JEENoDD_DefaultTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "test2"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "test3"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "test4"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "test5"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "test6"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testUpdateS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testUpdateHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testUpdateJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testAddS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testAddHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testAddJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testremoveS2InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testRemoveHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "testRemoveJSPWeb1"));
        testSuite.addTest(TestSuite.createTest(JEENoDD_DefaultTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/ws_1/noDD_updated_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEENoDD_DefaultTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/ws_1"), new String[] { "Utility1", "Web30NoDD1", "Web30NoDD_2", "Web30NoDD_3", "EAR6NoDD_1" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("EAR6NoDD_1");
    }

    @Test
    // test simple servlet in a web module
    public void testS1InWeb1() throws Exception {
        testPingWebPage("Web30NoDD1/S1", "from Web30NoDD1/S1");
    }

    @Test
    // test a html in a web module
    public void testHTMLWeb1() throws Exception {
        testPingWebPage("Web30NoDD1/a.html", "a.html");
    }

    @Test
    // test a jsp in a web module
    public void testJSPWeb1() throws Exception {
        testPingWebPage("Web30NoDD1/a.jsp", "a.jsp");
    }

    @Test
    // test a servlet use a class in utility project.  The class file is in the default output folder.
    public void test2() throws Exception {
        testPingWebPage("Web30NoDD1/CallUtilityInDefaultLocation", "String from default location of utility jarHi from UDefect");
    }

    @Test
    // test a servlet use a class in utility project.  The class file is in a non default output folder.
    public void test3() throws Exception {
        testPingWebPage("Web30NoDD1/CallUtilityInDifferentLocation", "String from different location of utility jar U1 - hi from src folder");
    }

    @Test
    // test simple servlet in a web module that has 2 output folders. 
    public void test4() throws Exception {
        testPingWebPage("Web30NoDD_2/S1", "from Web30NoDD2/S1");
    }

    @Test
    // test a servlet that the output is in a non-default output folder
    public void test5() throws Exception {
        testPingWebPage("Web30NoDD_2/ServletInDiffFolder", "Servlet in differnt folder");
    }

    @Test
    // test a servlet use a call in a jar in the lib directory
    public void test6() throws Exception {
        testPingWebPage("Web30NoDD_3/CallClassInJar", "String from the class in jar Hi from class in jar");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateS1InWeb1() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("S1.java"),
                   getProject("Web30NoDD1"), "src/web30/no_dd1/S1.java", 2000);
        testPingWebPage("Web30NoDD1/S1", "new_S1");
    }

    @Test
    // test a update html in a web module
    public void testUpdateHTMLWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("a.html"),
                   getProject("Web30NoDD1"), "WebContent/a.html", 2000);
        testPingWebPage("Web30NoDD1/a.html", "new_a.html");
    }

    @Test
    // test update jsp in a web module
    public void testUpdateJSPWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("a.jsp"),
                   getProject("Web30NoDD1"), "WebContent/a.jsp", 2000);
        testPingWebPage("Web30NoDD1/a.jsp", "new_a.jsp");
    }

    @Test
    // test add a servlet to a web module
    public void testAddS2InWeb1() throws Exception {
        print("Test add new");
        updateFile(getUpdatedFileFolder().append("S2.java"),
                   getProject("Web30NoDD1"), "src/web30/no_dd1/S2.java", 2000);
        testPingWebPage("Web30NoDD1/S2", "S2");
    }

    @Test
    // test a update html in a web module
    public void testAddHTMLWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("b.html"),
                   getProject("Web30NoDD1"), "WebContent/b.html", 2000);
        testPingWebPage("Web30NoDD1/b.html", "b.html");
    }

    @Test
    // test update jsp in a web module
    public void testAddJSPWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("b.jsp"),
                   getProject("Web30NoDD1"), "WebContent/b.jsp", 2000);
        testPingWebPage("Web30NoDD1/b.jsp", "b.jsp");
    }

    @Test
    // test remove a servlet from a web module
    public void testremoveS2InWeb1() throws Exception {
        print("Test remove");
        deleteFile(getProject("Web30NoDD1"), "src/web30/no_dd1/S2.java", 2000);
        testWebPageNotFound("Web30NoDD1/S2", "S2");
    }

    @Test
    // test remove a html from a web module
    public void testRemoveHTMLWeb1() throws Exception {
        deleteFile(getProject("Web30NoDD1"), "WebContent/b.html", 2000);
        testWebPageNotFound("Web30NoDD1/b.html", "b.html");
    }

    @Test
    // test remove jsp from a web module
    public void testRemoveJSPWeb1() throws Exception {
        deleteFile(getProject("Web30NoDD1"), "WebContent/b.jsp", 2000);
        testWebPageNotFound("Web30NoDD1/b.jsp", "b.jsp");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EAR6NoDD_1", 2500); //in case the publish is not done. 
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEENoDD_DefaultTest\n", 5000);
    }
}