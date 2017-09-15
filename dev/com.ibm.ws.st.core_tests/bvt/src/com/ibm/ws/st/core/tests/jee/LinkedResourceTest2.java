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

package com.ibm.ws.st.core.tests.jee;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

@RunWith(AllTests.class)
public class LinkedResourceTest2 extends JEETestBase {

    private static final String SERVER_NAME = "LinkedResource2TestServer";
    private static final String STATIC_PROJECT_NAME = "StaticWebProject";
    private static final String DYNAMIC_PROJECT_NAME = "DynamicWebProject";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(LinkedResourceTest2.getOrderedTests());
        suite.setName(LinkedResourceTest2.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(LinkedResourceTest2.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(LinkedResourceTest2.class, "testLinkedWebContent"));
        testSuite.addTest(TestSuite.createTest(LinkedResourceTest2.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: LinkedResourceTest2");
        init();
        createRuntime();
        createVM();
        importProjects(new Path("JEEDDtesting/LinkedResource2"), new String[] { STATIC_PROJECT_NAME });
        importProjects(new Path("JEEDDtesting/LinkedResource2"), new String[] { DYNAMIC_PROJECT_NAME });

        createServer(runtime, null, SERVER_NAME, null);
    }

    @Test
    // Test local html file
    public void testLinkedWebContent() throws Exception {
        startServer();
        addApp(DYNAMIC_PROJECT_NAME, false, 10);
        testPingWebPage(DYNAMIC_PROJECT_NAME + "/static.html", "Hello from static.html");
    }

    @Test
    public void doTearDown() throws Exception {
        stopServer();
        removeApp(DYNAMIC_PROJECT_NAME, 1000);
        removeApp(STATIC_PROJECT_NAME, 1000);
        WLPCommonUtil.cleanUp();
        wait("Ending test: LinkedResourceTest2\n", 5000);
    }

}