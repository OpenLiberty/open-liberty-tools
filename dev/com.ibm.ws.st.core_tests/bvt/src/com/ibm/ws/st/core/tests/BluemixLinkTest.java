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
package com.ibm.ws.st.core.tests;

import java.net.URL;

import junit.framework.TestSuite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test bluemix tools link", isStable = true)
@RunWith(AllTests.class)
public class BluemixLinkTest extends ToolsTestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(BluemixLinkTest.getOrderedTests());
        suite.setName(BluemixLinkTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(BluemixLinkTest.class, "testPingBluemixLink"));

        return testSuite;
    }

    @Test
    public void testPingBluemixLink() throws Exception {
        URL url = new URL(Constants.BLUEMIX_URL);
        pingWebPage(url, "IBM Bluemix Docs");
    }

}
