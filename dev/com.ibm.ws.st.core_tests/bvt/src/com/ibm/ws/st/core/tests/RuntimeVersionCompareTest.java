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
package com.ibm.ws.st.core.tests;

import junit.framework.TestSuite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test comparing runtime versions", isStable = true)
@RunWith(AllTests.class)
public class RuntimeVersionCompareTest extends ToolsTestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(RuntimeVersionCompareTest.getOrderedTests());
        suite.setName(RuntimeVersionCompareTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(RuntimeVersionCompareTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(RuntimeVersionCompareTest.class, "testRuntimeVersionCompare"));
        testSuite.addTest(TestSuite.createTest(RuntimeVersionCompareTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: RuntimeVersionCompareTest");
        init();
    }

    @Test
    public void testRuntimeVersionCompare() {
        assertTrue("9.0 >= 9.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0", "9.0"));
        assertTrue("9.1 >= 9.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0", "9.1"));
        assertFalse("9.0 >= 9.1 should be false", WebSphereUtil.isGreaterOrEqualVersion("9.1", "9.0"));
        assertTrue("9.0.1 >= 9.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0", "9.0.1"));
        assertFalse("9.0 >= 9.0.1 should be false", WebSphereUtil.isGreaterOrEqualVersion("9.0.1", "9.0"));
        assertTrue("9.0 >= 9.0.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0.0", "9.0"));
        assertTrue("9.0.0 >= 9.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0", "9.0.0"));
        assertTrue("9.0.1 >= 9.0.1.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0.1.0", "9.0.1"));
        assertTrue("9.0.1.0 >= 9.0.1 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0.1", "9.0.1.0"));
        assertFalse("9.0 >= 9.0.0.1 should be false", WebSphereUtil.isGreaterOrEqualVersion("9.0.0.1", "9.0"));
        assertTrue("9.0.0.1 >= 9.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("9.0", "9.0.0.1"));
        assertTrue("9.0 >= 8.0 should be true", WebSphereUtil.isGreaterOrEqualVersion("8.0", "9.0"));
        assertTrue("9 >= 8 should be true", WebSphereUtil.isGreaterOrEqualVersion("8", "9"));
        assertTrue("9.0 >= 8.1 should be true", WebSphereUtil.isGreaterOrEqualVersion("8.1", "9.0"));
        assertFalse("8.0 >= 9.0 should be false", WebSphereUtil.isGreaterOrEqualVersion("9.0", "8.0"));
        assertFalse("8 >= 9 should be false", WebSphereUtil.isGreaterOrEqualVersion("9", "8"));
        assertFalse("8.1 >= 9.0 should be false", WebSphereUtil.isGreaterOrEqualVersion("9.0", "8.1"));
        assertTrue("8.1.2 >= 8.0.3 should be true", WebSphereUtil.isGreaterOrEqualVersion("8.0.3", "8.1.2"));
        assertFalse("8.0.3 >= 8.1.2 should be false", WebSphereUtil.isGreaterOrEqualVersion("8.1.2", "8.0.3"));
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: RuntimeVersionCompareTest\n");
    }
}