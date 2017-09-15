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

import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test publish of enterprise app with resource adapter", isStable = true)
@RunWith(AllTests.class)
public class WebFragmentResources extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebFragmentResources.getOrderedTests());
        suite.setName(WebFragmentResources.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebFragmentResources.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebFragmentResources.class, "testPing"));
        testSuite.addTest(TestSuite.createTest(WebFragmentResources.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(WebFragmentResources.class, "doTearDown"));

        return testSuite;
    }

    public String getResourceDir() {
        return "jee/WebFragmentResources";
    }

    public String getClassName() {
        return WebFragmentResources.class.getSimpleName();
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        createServer(runtime, SERVER_NAME, null);
        createVM();

        importProjects(new Path(getResourceDir()),
                       new String[] { "Frag1", "Web1" });

        startServer();
        wait("Wait 10 seconds before adding application.", 3000);
        addApp("Web1", false, 30);
    }

    @Test
    // test html in Web1
    public void testPing() throws Exception {
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/Web1Servlet", "Web1File.txt");
        testPingWebPage("Web1/Web1Servlet", "Frag1File.txt");

        testPingWebPage("Web1/Frag1Servlet", "Web1File.txt");
        testPingWebPage("Web1/Frag1Servlet", "Frag1File.txt");
    }

    @Test
    // switch loose config mode
    public void switchLooseCfgMode() throws Exception {
        boolean isLC = isLooseCfg();
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
        wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, !isLC);
        wc.save(true, null);
        safePublishIncremental(server);
        wait("Wait 3 seconds before ping.", 3000);

        testPingWebPage("Web1/Web1Servlet", "Web1File.txt");
        testPingWebPage("Web1/Web1Servlet", "Frag1File.txt");

        testPingWebPage("Web1/Frag1Servlet", "Web1File.txt");
        testPingWebPage("Web1/Frag1Servlet", "Frag1File.txt");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("Web1", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }
}