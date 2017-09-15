/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 *
 */

@TestCaseDescriptor(description = "Check feature list", isStable = true)
@RunWith(AllTests.class)
public class FeatureListTest extends ToolsTestBase {
    protected static final String RUNTIME_NAME = FeatureListTest.class.getCanonicalName() + "_runtime";

    static final String DIR = "/FeatureTesting/";
    static final String SERVER = "feature";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(FeatureListTest.getOrderedTests());
        suite.setName(FeatureListTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testIsFeature1"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testIsFeature2"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatures1"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatures2"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetRuntimeFeatureSet1"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetRuntimeFeatureSet2"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatureChildren1"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatureChildren2"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatureParents1"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatureParents2"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatureAPIJars1"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "testGetFeatureAPIJars2"));
        testSuite.addTest(TestSuite.createTest(FeatureListTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: FeatureListTest");
        init();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER, "resources/" + DIR + "/" + SERVER);
        setServerStartTimeout(120);
        setServerStopTimeout(60);
        createVM(JDK_NAME);
    }

    private void testIsFeature(WebSphereRuntime wsRuntime) throws Exception {
        // first
        assertTrue(FeatureList.isValidFeature("appSecurity-2.0", wsRuntime));
        // middle
        assertTrue(FeatureList.isValidFeature("jpa-2.1", wsRuntime));
        // last
        assertTrue(FeatureList.isValidFeature("webProfile-7.0", wsRuntime));
    }

    private void testGetFeatures(WebSphereRuntime wsRuntime) {
        List<String> featureList1 = FeatureList.getFeatures(false, wsRuntime);
        assertTrue(featureList1.size() != 0);
        List<String> featureList2 = FeatureList.getFeatures(true, wsRuntime);
        Collections.sort(featureList1);
        assertTrue(featureList1.equals(featureList2));
    }

    private void testGetRuntimeFeatureSet(WebSphereRuntime wsRuntime) {
        List<String> featureList1 = FeatureList.getFeatures(true, wsRuntime);
        assertTrue(featureList1.size() != 0);
        Set<String> featureSet = FeatureList.getRuntimeFeatureSet(wsRuntime);
        ArrayList<String> featureList2 = new ArrayList<String>(featureSet);
        Collections.sort(featureList2);
        assertTrue(featureList1.equals(featureList2));
    }

    private void testGetFeatureChildren(String feature, WebSphereRuntime wsRuntime, String[] expected) {
        Set<String> children = FeatureList.getFeatureChildren(feature, wsRuntime);
        for (String s : expected) {
            assertTrue(FeatureList.containsIgnoreCase(children, s));
        }
    }

    /**
     * Get the set of parents for the given feature.
     */
    private void testGetFeatureParents(String featureName, WebSphereRuntime wsRuntime, String[] expected) {
        Set<String> parents = FeatureList.getFeatureParents(featureName, wsRuntime);
        for (String s : expected) {
            assertTrue(FeatureList.containsIgnoreCase(parents, s));
        }
    }

    @Test
    public void testIsFeature1() throws Exception {
        // Fallback feature list
        testIsFeature(null);
    }

    @Test
    public void testIsFeature2() throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        testIsFeature(runtime);
    }

    @Test
    public void testGetFeatures1() throws Exception {
        testGetFeatures(null);
    }

    @Test
    public void testGetFeatures2() throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        testGetFeatures(runtime);
    }

    @Test
    public void testGetRuntimeFeatureSet1() throws Exception {
        testGetRuntimeFeatureSet(null);
    }

    @Test
    public void testGetRuntimeFeatureSet2() throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        testGetRuntimeFeatureSet(runtime);
    }

    @Test
    public void testGetFeatureChildren1() throws Exception {
        testGetFeatureChildren("jpa-2.1", null, new String[] { "jdbc-4.1", "jndi-1.0", "jpaContainer-2.1" });
    }

    @Test
    public void testGetFeatureChildren2() throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        testGetFeatureChildren("jpa-2.1", runtime, new String[] { "jdbc-4.1", "jndi-1.0", "jpaContainer-2.1" });
    }

    @Test
    public void testGetFeatureParents1() throws Exception {
        testGetFeatureParents("servlet-3.1", null, new String[] { "jsp-2.3" });
    }

    @Test
    public void testGetFeatureParents2() throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        testGetFeatureParents("servlet-3.1", runtime, new String[] { "jsp-2.3" });
    }

    @Test
    public void testGetFeatureAPIJars1() throws Exception {
        testGetFeatureAPIJars("jaxrs-2.0", null);
    }

    @Test
    public void testGetFeatureAPIJars2() throws Exception {
        WebSphereRuntime runtime = wsServer.getWebSphereRuntime();
        assertNotNull(runtime);
        testGetFeatureAPIJars("jaxrs-2.0", runtime);
    }

    /**
     * Get the set of API Jar Info for the given feature.
     */
    private void testGetFeatureAPIJars(String featureName, WebSphereRuntime wsRuntime) {
        Set<String> parents = FeatureList.getFeatureAPIJars(featureName, wsRuntime);
        assertFalse("Set of API jars for feature '" + featureName + "' should not be empty.", parents.isEmpty());
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: FeatureListTest\n");
    }
}
