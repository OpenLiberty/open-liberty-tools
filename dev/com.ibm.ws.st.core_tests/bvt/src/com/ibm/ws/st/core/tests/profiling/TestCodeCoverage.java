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
package com.ibm.ws.st.core.tests.profiling;

import junit.framework.TestSuite;

import org.eclipse.debug.core.ILaunchManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.launch.LaunchUtilities;
import com.ibm.ws.st.core.internal.launch.ServerStartInfo;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Tests whether the profiling configuration extension point is working correctly
 */

@TestCaseDescriptor(description = "Test profiling configuration extension point", isStable = true)
@RunWith(AllTests.class)
public class TestCodeCoverage extends ToolsTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(TestCodeCoverage.getOrderedTests());
        suite.setName(TestCodeCoverage.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(TestCodeCoverage.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(TestCodeCoverage.class, "testExtensionWorks"));
        testSuite.addTest(TestSuite.createTest(TestCodeCoverage.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        System.setProperty("wtp.autotest.noninteractive", "true");
        init();
        createRuntime("CodeCoverageRuntime");
        createServer(runtime, "CodeCoverageTestServer", null);
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testExtensionWorks() throws Exception {
        String arg = getJVMArgs();
        String expectedArgs = "\"" + CodeCoverageExtension.ARG + "\""; // returned args are quoted
        print("Returned args: " + arg);
        assertTrue("Expected args: {" + expectedArgs + "} Actual args: {" + arg + "}", arg.equals(expectedArgs));
    }

    @Test
    public void testServerMode() throws Exception {
        // Force the CodeCoverageExtension to return true for isProfiling 
        CodeCoverageExtension.isProfiling = Boolean.TRUE;
        startServer();
        assertEquals("Server is expected to be in profiling mode but instead was in " + server.getMode(), ILaunchManager.PROFILE_MODE, server.getMode());
        stopServer();
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: TestCodeCoverage\n");
    }

    private static String getJVMArgs() throws Exception {
        // get vm args provided by server pre-start extensions 
        ServerStartInfo startInfo = new ServerStartInfo(server, server.getMode());
        String vmArgs = "";
        return LaunchUtilities.processVMArguments(vmArgs, startInfo);
    }
}