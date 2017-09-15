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

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check server order is case insensitive", isStable = true)
@RunWith(AllTests.class)
public class OrderedServersTestCase extends ToolsTestBase {

    private static final String[] EXPECTED_SERVER_NAMES = { "AnotherServer", "defaultServer", "TestServer" };
    private static final String[] SERVER_NAMES = { "defaultServer", "TestServer", "AnotherServer" };

    protected static WebSphereRuntime wsRuntime;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(OrderedServersTestCase.getOrderedTests());
        suite.setName(OrderedServersTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(OrderedServersTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(OrderedServersTestCase.class, "testOrderedServers"));
        testSuite.addTest(TestSuite.createTest(OrderedServersTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: OrderedServersTestCase");
        init();
        createRuntime(RUNTIME_NAME);
        for (String server : SERVER_NAMES) {
            createServer(runtime, server, null);
        }

        createVM(JDK_NAME);
    }

    @Test
    public void testOrderedServers() {
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        assertNotNull(wsRuntime);

        String[] serverNames = wsRuntime.getServerNames();
        assertTrue("The list of server names has a different size", EXPECTED_SERVER_NAMES.length == serverNames.length);

        boolean isMatched = true;
        for (int i = 0; i < EXPECTED_SERVER_NAMES.length && isMatched; ++i) {
            if (!serverNames[i].equals(EXPECTED_SERVER_NAMES[i]))
                isMatched = false;
        }

        assertTrue("The order of server names did not match the one in the expected list", isMatched);
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        print("Ending test: OrderedServersTestCase\n");
    }
}