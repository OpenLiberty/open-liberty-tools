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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jst.server.core.ServerProfilerDelegate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Tests whether the profiling configuration extension point is working correctly
 */

@TestCaseDescriptor(description = "Test profiling configuration extension point", isStable = true)
@RunWith(AllTests.class)
public class TestProfiling extends ToolsTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(TestProfiling.getOrderedTests());
        suite.setName(TestProfiling.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(TestProfiling.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(TestProfiling.class, "testExtensionWorks"));
        testSuite.addTest(TestSuite.createTest(TestProfiling.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() {
        System.setProperty("wtp.autotest.noninteractive", "true");
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testExtensionWorks() throws Exception {
        String arg = getJVMArgs();
        String expectedArgs = "\"" + ProfileExtension.ARG + "\""; // returned args are quoted
        print("Returned args: " + arg);
        assertTrue("Expected args: {" + expectedArgs + "} Actual args: {" + arg + "}", arg.equals(expectedArgs));
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: TestProfiling\n");
    }

    private static String getJVMArgs() throws Exception {
        String vmArgs = "";
        IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
        VMRunnerConfiguration runConfig = new VMRunnerConfiguration("n/a", new String[0]);

        runConfig.setVMArguments(DebugPlugin.parseArguments(vmArgs));

        ServerProfilerDelegate.configureProfiling(null, vmInstall, runConfig, new NullProgressMonitor());
        String[] newVmArgs = runConfig.getVMArguments();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < newVmArgs.length; i++) {
            if (i > 0)
                sb.append(" ");
            String s = newVmArgs[i];
            if (!s.contains("\""))
                sb.append("\"" + s + "\"");
            else
                sb.append(s);
        }
        vmArgs = sb.toString();

        return vmArgs;
    }
}