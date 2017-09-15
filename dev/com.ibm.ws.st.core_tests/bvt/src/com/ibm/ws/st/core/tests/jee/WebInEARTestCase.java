/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test web module in enterprise application", isStable = true)
@RunWith(AllTests.class)
public class WebInEARTestCase extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebInEARTestCase.getOrderedTests());
        suite.setName(WebInEARTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebInEARTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebInEARTestCase.class, "testPingWebModule"));
        testSuite.addTest(TestSuite.createTest(WebInEARTestCase.class, "testRemoveEAR"));
        testSuite.addTest(TestSuite.createTest(WebInEARTestCase.class, "testRemoveCompleted"));
        testSuite.addTest(TestSuite.createTest(WebInEARTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: WebInEARTestCase");
        init();
        createRuntime();
        createServer();
        createVM();
        importEAR();
        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("EAR5");
    }

    protected static void importEAR() throws Exception {
        IPath earPath = resourceFolder.append("libertyJUnitEAR5.ear");
        ArrayList<String> list = new ArrayList<String>();
        list.add("WEB-INF/lib/U.jar");

        ModuleHelper.importEAR(earPath.toOSString(), "EAR5", list, runtime);
        forceBuild();
        waitForBuildToFinish();
    }

    @Test
    public void testPingWebModule() throws Exception {
        wait("Wait 5 seconds before ping", 5000);
        testPingWebPage("Web25/S", "U-hi ABC");
    }

    @Test
    public void testRemoveEAR() throws Exception {
        removeApp("EAR5", 2500);
    }

    @Test
    public void testRemoveCompleted() {
        IPath path = server.getRuntime().getLocation();
        path = path.append("usr/servers").append(wsServer.getServerName());
        path = path.append("apps").append("EAR5.ear");
        assertTrue(!path.toFile().exists());
    }

    @Test
    public void doTearDown() throws Exception {
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: WebInEARTestCase\n", 5000);
    }
}
