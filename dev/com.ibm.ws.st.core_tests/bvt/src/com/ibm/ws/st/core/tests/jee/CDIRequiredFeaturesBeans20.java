/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.util.FeatureUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 * Test that correct cdi feature enabled for beans.xml that specifies the 1.1
 * schema location.
 */
@TestCaseDescriptor(description = "CDI required feature for beans.xml with 1.1 schema location", isStable = true)
@RunWith(AllTests.class)
public class CDIRequiredFeaturesBeans20 extends CDIRequiredFeaturesBase {
    private static final String TEST_NAME = "CDIRequiredFeaturesBeans20";
    private static final String CDI_FEATURE = "cdi-2.0";
    private static final String[] EXPECTED_FEATURES = new String[] { CDI_FEATURE };

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(CDIRequiredFeaturesBeans20.getOrderedTests());
        suite.setName(CDIRequiredFeaturesBeans20.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeans20.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeans20.class, "testAddApp"));
        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeans20.class, "testFeaturesAdded"));
        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeans20.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + TEST_NAME);
        init();
        createRuntime();
        if (!runtimeSupportsFeature(CDI_FEATURE)) {
            print("ATTENTION: Runtime does not support feature " + CDI_FEATURE + ", skipping test.");
            return;
        }
        createServer(runtime, SERVER12_NAME, SERVER12_LOCATION);
        createVM();
        importProjects(new Path("jee/cdi/" + TEST_NAME),
                       new String[] { "WebProject" });
        forceBuild();
    }

    @Test
    public void testAddApp() throws Exception {
        if (!runtimeSupportsFeature(CDI_FEATURE))
            return;
        addApp();
    }

    @Test
    public void testFeaturesAdded() throws Exception {
        if (!runtimeSupportsFeature(CDI_FEATURE))
            return;
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        FeatureUtil.verifyFeatures(wr, EXPECTED_FEATURES, ws.getConfiguration().getFeatures());
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: " + TEST_NAME + "\n");
    }
}