/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
@TestCaseDescriptor(description = "Test add/remove web module from enterprise app with deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEE6DDAddRemoveWebTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEE6DDAddRemoveWebTest.getOrderedTests());
        suite.setName(JEE6DDAddRemoveWebTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "testS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "testJSPInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "testHTMLInWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "testAddWeb2ToEAR"));
        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "testRemoveWeb2FromEAR"));
        testSuite.addTest(TestSuite.createTest(JEE6DDAddRemoveWebTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/ws_1/dd_updated_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEE6DDAddRemoveWebTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/ws_1"), new String[] { "Web30DD_1", "Web30DD_2", "EAR6DD_1" });
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
    // test add a web module to the published EAR
    public void testAddWeb2ToEAR() throws Exception {
        print("Test Add Web2 to the EAR");
        updateFile(getUpdatedFileFolder().append("add_web2_org.eclipse.wst.common.component"),
                   getProject("EAR6DD_1"), ".settings/org.eclipse.wst.common.component",
                   1);
        updateFile(getUpdatedFileFolder().append("add_web2_application.xml"),
                   getProject("EAR6DD_1"), "EarContent/META-INF/application.xml",
                   2000);

        wait("wait 5 seconds before ping.", 5000);
        testPingWebPage("Web30DD_2/S2", "Web2S2");
        testPingWebPage("Web30DD_2/a.html", "web2_a.html");
        testPingWebPage("Web30DD_2/a.jsp", "web2_a.jsp");
    }

    @Test
    // test remove a web module from the published EAR
    public void testRemoveWeb2FromEAR() throws Exception {
        print("Test Remove Web2 from the EAR");
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component"),
                   getProject("EAR6DD_1"), ".settings/org.eclipse.wst.common.component",
                   1);
        updateFile(getUpdatedFileFolder().append("application.xml"),
                   getProject("EAR6DD_1"), "EarContent/META-INF/application.xml",
                   2000);

        wait("wait 5 seconds before ping.", 5000);
        testWebPageNotFound("Web30DD_2/S2", "Web2S2");
        testWebPageNotFound("Web30DD_2/a.html", "web2_a.html");
        testWebPageNotFound("Web30DD_2/a.jsp", "web2_a.jsp");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EAR6DD_1", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEE6DDAddRemoveWebTest\n", 5000);
    }

}
