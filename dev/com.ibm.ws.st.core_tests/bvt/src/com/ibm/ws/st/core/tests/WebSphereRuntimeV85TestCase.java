/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check WebSphere runtime V85", isStable = true)
@RunWith(AllTests.class)
public class WebSphereRuntimeV85TestCase extends RuntimeTestCaseBase {
    private static final int NUMBER_OF_RUNTIMES = 1;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebSphereRuntimeV85TestCase.getOrderedTests());
        suite.setName(WebSphereRuntimeV85TestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testCreateRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testGetLocation"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testValidateRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testValidateRuntime2"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testAdaptRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testModifyRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "testDeleteRuntime"));
        testSuite.addTest(TestSuite.createTest(WebSphereRuntimeV85TestCase.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected int getNumberOfRuntimes() {
        return NUMBER_OF_RUNTIMES;
    }

    @Override
    protected String getRuntimeTypeId() {
        return WLPCommonUtil.RUNTIME_V85_ID;
    }
}