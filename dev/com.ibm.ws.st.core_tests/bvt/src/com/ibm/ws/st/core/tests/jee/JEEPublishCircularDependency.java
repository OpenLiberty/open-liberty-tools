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
package com.ibm.ws.st.core.tests.jee;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish of enterprise app containing two web apps with circular dependency", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishCircularDependency extends JEETestBase {

    // NOTE: The class files for this test case must be checked in
    // as eclipse will not build them because of the circular dependency
    // error.

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishCircularDependency.getOrderedTests());
        suite.setName(JEEPublishCircularDependency.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishCircularDependency.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishCircularDependency.class, "testPingCircularWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishCircularDependency.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishCircularDependency.class, "doTearDown"));

        return testSuite;
    }

    public String getResourceDir() {
        return "jee/JEEPublishCircularDependency";
    }

    public String getClassName() {
        return JEEPublishCircularDependency.class.getSimpleName();
    }

    public String getServletFeature() {
        return "servlet-3.1";
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        if (!runtimeSupportsFeature(getServletFeature())) {
            print("ATTENTION: Runtime does not support feature " + getServletFeature() + ", skipping test.");
            return;
        }
        createServer(runtime, SERVER_NAME, "resources/jee/JEEPublishCircularDependency");
        createVM();

        importProjects(new Path(getResourceDir()),
                       new String[] { "CircularWebA", "CircularWebB", "CircularEAR" });

        startServer();
        wait("Wait 10 seconds before adding application.", 3000);
        addApp("CircularEAR", false, 30);
    }

    @Test
    // WebA
    public void testPingCircularWebApps() throws Exception {
        if (!runtimeSupportsFeature(getServletFeature()))
            return;
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("CircularWebA/WebAServlet", "Hello from WebA");
        testPingWebPage("CircularWebB/WebBServlet", "Hello from WebB");
    }

    @Test
    // switch loose config mode
    public void switchLooseCfgMode() throws Exception {
        if (!runtimeSupportsFeature(getServletFeature()))
            return;
        boolean isLC = isLooseCfg();
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
        wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, !isLC);
        wc.save(true, null);
        safePublishIncremental(server);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("CircularWebA/WebAServlet", "Hello from WebA");
        testPingWebPage("CircularWebB/WebBServlet", "Hello from WebB");
    }

    @Test
    public void doTearDown() throws Exception {
        if (runtimeSupportsFeature(getServletFeature())) {
            removeApps("CircularEAR", "CircularWebA", "CircularWebB");
            stopServer();
        }
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }
}