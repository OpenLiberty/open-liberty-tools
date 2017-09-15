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
@TestCaseDescriptor(description = "Test publish for binary modules (utility and web fragment)", isStable = true)
@RunWith(AllTests.class)
public class JEE_BinaryModule_Test extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEE_BinaryModule_Test.getOrderedTests());
        suite.setName(JEE_BinaryModule_Test.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "testS1InNon_Bin_Web"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "testNonBinServletCallBinUtility"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "testBinServletCallBinUtility"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "testNonBinWebCallNonBinFrag"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "testBinWebCallBinFrag"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "testUpdateHTMLWeb1"));
        testSuite.addTest(TestSuite.createTest(JEE_BinaryModule_Test.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/BinaryModule/update_files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEE_BinaryModule_Test");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/BinaryModule"), new String[] { "Web_Not_Bin_1", "EAR_Bin_Module" });
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("EAR_Bin_Module");
    }

    @Test
    // test a servlet in a non-binary web module
    public void testS1InNon_Bin_Web() throws Exception {
        testPingWebPage("Web_Not_Bin_1/S1", "nobin S1");
    }

    @Test
    // test a servlet in a non-binary web module that calls a binary utility
    public void testNonBinServletCallBinUtility() throws Exception {
        testPingWebPage("Web_Not_Bin_1/S2", "nobin S2 C1 ");
    }

    @Test
    // test a servlet in a binary web module that calls a binary utility
    public void testBinServletCallBinUtility() throws Exception {
        testPingWebPage("Web_Bin_1/S1", "bin S1  C1 ");
    }

    @Test
    // test a servlet in a binary fragment from a non binary web
    public void testNonBinWebCallNonBinFrag() throws Exception {
        testPingWebPage("Web_Not_Bin_1/FS_NON_BIN", "FS_NON_BIN");
    }

    @Test
    // test a servlet in a binary fragment from a binary web
    public void testBinWebCallBinFrag() throws Exception {
        testPingWebPage("Web_Bin_1/FS_BIN", "FS_BIN");
    }

    @Test
    // test update the utility jar
    public void testUpdateHTMLWeb1() throws Exception {
        updateFile(getUpdatedFileFolder().append("Utility_Bin.jar"),
                   getProject("EAR_Bin_Module"), "EarContent/lib/Utility_Bin.jar", 2000);
        testPingWebPage("Web_Not_Bin_1/S2", "nobin S2 C1new");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EAR_Bin_Module", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEE_BinaryModule_Test\n", 5000);
    }

}
