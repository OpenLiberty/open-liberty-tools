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
package com.ibm.ws.st.core.tests;

import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check WebSphere runtime", isStable = true)
@RunWith(AllTests.class)
public class WebSphereRuntimeTestCase extends RuntimeTestCaseBase {
    private static final int NUMBER_OF_RUNTIMES = 1;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebSphereRuntimeTestCase.getOrderedTests());
        suite.setName(WebSphereRuntimeTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testCreateRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testGetLocation"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testValidateRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testValidateRuntime2"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testAdaptRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testModifyRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "testDeleteRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected int getNumberOfRuntimes() {
        return NUMBER_OF_RUNTIMES;
    }
}