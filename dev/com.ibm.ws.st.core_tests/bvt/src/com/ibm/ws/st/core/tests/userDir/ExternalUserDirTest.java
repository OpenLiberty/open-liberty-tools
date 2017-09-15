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
package com.ibm.ws.st.core.tests.userDir;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.TestsPlugin;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish of enterprise app with resource adapter", isStable = true)
@RunWith(AllTests.class)
public class ExternalUserDirTest extends ToolsTestBase {

    private static String SERVER_NAME = "externalServer";

    private static IPath externalPath;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ExternalUserDirTest.getOrderedTests());
        suite.setName(ExternalUserDirTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(ExternalUserDirTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ExternalUserDirTest.class, "testAddWar"));
        testSuite.addTest(TestSuite.createTest(ExternalUserDirTest.class, "testStopWar"));
        testSuite.addTest(TestSuite.createTest(ExternalUserDirTest.class, "testStartWar"));
        testSuite.addTest(TestSuite.createTest(ExternalUserDirTest.class, "testRemoveWar"));
        testSuite.addTest(TestSuite.createTest(ExternalUserDirTest.class, "doTearDown"));

        return testSuite;
    }

    public String getClassName() {
        return ExternalUserDirTest.class.getSimpleName();
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        externalPath = TestsPlugin.getInstance().getStateLocation().append("externalUserDir");
        IPath sourcePath = resourceFolder.append("userDirTesting/externalUserDir").append("userDir");
        FileUtil.copyFiles(sourcePath.toOSString(), externalPath.toOSString());
        createRuntime(RUNTIME_NAME, externalPath);
        createServer(runtime, externalPath, SERVER_NAME, null);
        createVM(JDK_NAME);
        importProjects(new Path("userDirTesting/externalUserDir"), new String[] { "Web1" });
        startServer();
        wait("Wait 10 seconds before adding application.", 3000);
    }

    @Test
    // test starting the web module
    public void testAddWar() throws Exception {
        addApp("Web1", false, 30);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/Web1Servlet", "Web1Servlet: 0");
    }

    @Test
    // test stopping the web module
    public void testStopWar() throws Exception {
        stopApp("Web1", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testWebPageNotFound("Web1/Web1Servlet", "Web1Servlet: 0");
    }

    @Test
    // test starting the web module
    public void testStartWar() throws Exception {

        startApp("Web1", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/Web1Servlet", "Web1Servlet: 0");
    }

    @Test
    // test stopping the web module
    public void testRemoveWar() throws Exception {
        removeApp("Web1", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testWebPageNotFound("Web1/Web1Servlet", "Web1Servlet: 0");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("Web1", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        if (externalPath != null) {
            try {
                FileUtil.deleteDirectory(externalPath.toOSString(), true);
            } catch (Exception e) {
                print("Failed to clean up external server directory: " + externalPath.toOSString(), e);
            }
        }
        wait("Ending test: " + getClassName() + "\n", 5000);
    }
}
