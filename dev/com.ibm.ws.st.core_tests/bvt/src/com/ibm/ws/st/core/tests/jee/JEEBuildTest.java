/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.TestsPlugin;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test build for imported war file", isStable = true)
@RunWith(AllTests.class)
public class JEEBuildTest extends JEETestBase {
    private static final String RUNTIME_NAME = JEEBuildTest.class.getCanonicalName() + "_runtime";
    private static final String WAR_NAME = "KitchenSink";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEBuildTest.getOrderedTests());
        suite.setName(JEEBuildTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEBuildTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEBuildTest.class, "testImportApp"));
        testSuite.addTest(TestSuite.createTest(JEEBuildTest.class, "testBuild"));
        testSuite.addTest(TestSuite.createTest(JEEBuildTest.class, "testBuildPathResult"));
        testSuite.addTest(TestSuite.createTest(JEEBuildTest.class, "testBuildResult"));
        testSuite.addTest(TestSuite.createTest(JEEBuildTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEEBuildTest");
        init();
        createRuntime(RUNTIME_NAME);
        wait("Wait 3 seconds before importing application.", 3000);
    }

    @Test
    public void testImportApp() throws Exception {
        IPath resourceFolder = TestsPlugin.getInstallLocation().append("resources");
        IPath warPath = resourceFolder.append(WAR_NAME + ".war");
        ModuleHelper.importWAR(warPath.toOSString(), WAR_NAME, runtime);
    }

// The KitchSink is a JEE war.  It doesn't OSGi facet.  We should not test OSGi platform target here.
// We can remove this in the future if there is not objection.
//    @Test
//    public void testRuntimeTarget() throws Exception {
//        IProject project = ModuleHelper.getProject(WAR_NAME);
//        OSGiModuleHelper.setProjectRuntime(project, runtime);
//
//        // we could test the path of the platform target, but it is enough to test
//        // the name of the active platform because we create it and set it active
//        String targetName = TestUtil.getPlatformTargetName();
//        assertTrue(RUNTIME_NAME.equals(targetName));
//    }

    @Test
    public void testBuild() throws Exception {
        ModuleHelper.getProject(WAR_NAME).build(IncrementalProjectBuilder.FULL_BUILD, null);
        WLPCommonUtil.jobWaitBuild();
    }

    @Test
    public void testBuildPathResult() throws Exception {
        IProject project = ModuleHelper.getProject(WAR_NAME);
        IMarker[] markers = project.findMarkers("org.eclipse.jdt.core.buildpath_problem", true, IResource.DEPTH_INFINITE);
        if (markers != null) {
            boolean fail = false;
            for (IMarker m : markers) {
                if (IMarker.SEVERITY_ERROR == m.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO)) {
                    System.err.println("Error in app build path: " + m.getAttribute(IMarker.MESSAGE));
                    System.err.println("   " + m.getResource().getLocation());
                    fail = true;
                }
            }
            if (fail)
                Assert.fail("Error(s) in app build path");
        }
    }

    @Test
    public void testBuildResult() throws Exception {
        IProject project = ModuleHelper.getProject(WAR_NAME);
        IMarker[] markers = project.findMarkers("org.eclipse.jdt.core.problem", true, IResource.DEPTH_INFINITE);
        if (markers != null) {
            boolean fail = false;
            boolean foundIntentionalFail = false;
            for (IMarker m : markers) {
                if (IMarker.SEVERITY_ERROR == m.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO)) {
                    if ("Fail.java".equals(m.getResource().getName())) {
                        foundIntentionalFail = true;
                    } else {
                        System.err.println("Error in app build: " + m.getAttribute(IMarker.MESSAGE));
                        System.err.println("   " + m.getResource().getLocation());
                        fail = true;
                    }
                }
            }
            if (fail)
                Assert.fail("Error(s) in app build");
            if (!foundIntentionalFail)
                Assert.fail("Build not complete. Intentional compile error not found");
        }
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: JEEBuildTest\n");
    }
}