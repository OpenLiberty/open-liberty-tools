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

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check for existence", isStable = true)
@RunWith(AllTests.class)
public class ExistenceTest extends TestCase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ExistenceTest.getOrderedTests());
        suite.setName(ExistenceTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(ExistenceTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ExistenceTest.class, "verifyExistence"));
        testSuite.addTest(TestSuite.createTest(ExistenceTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() {
        WLPCommonUtil.print("Starting test: ExistenceTest");
        System.setProperty("wtp.autotest.noninteractive", "true");
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void verifyExistence() {
        assertNotNull(Activator.getInstance());
    }

    @Test
    public void doTearDown() throws Exception {
        WLPCommonUtil.print("Ending test: ExistenceTest\n");
    }
}