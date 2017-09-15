/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.util.FeatureUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test that correct cdi feature enabled for beans.xml that specifies the 1.1
 * schema location and the version is set to 1.1.
 */
@TestCaseDescriptor(description = "CDI required feature for beans.xml with 1.1 schema location where version is 1.1", isStable = true)
@RunWith(AllTests.class)
public class CDIRequiredFeaturesBeansVersion11 extends CDIRequiredFeaturesBase {
    private static final String TEST_NAME = "CDIRequiredFeaturesBeansVersion11";
    private static final String CDI_FEATURE = "cdi-1.2";
    private static final String[] EXPECTED_FEATURES = new String[] { CDI_FEATURE };

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(CDIRequiredFeaturesBeansVersion11.getOrderedTests());
        suite.setName(CDIRequiredFeaturesBeansVersion11.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeansVersion11.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeansVersion11.class, "testAddApp"));
        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeansVersion11.class, "testFeaturesAdded"));
        testSuite.addTest(TestSuite.createTest(CDIRequiredFeaturesBeansVersion11.class, "doTearDown"));

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