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

@TestCaseDescriptor(description = "Test publish for binary modules (web, web fragment, util, ejb)", isStable = true)
@RunWith(AllTests.class)
public class JEEBinaryModule extends JEETestBase {

    private static final String EJB_FEATURE = "ejb-3.2";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEBinaryModule.getOrderedTests());
        suite.setName(JEEBinaryModule.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEBinaryModule.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEBinaryModule.class, "testWeb"));
        testSuite.addTest(TestSuite.createTest(JEEBinaryModule.class, "testWebFragment"));
        testSuite.addTest(TestSuite.createTest(JEEBinaryModule.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEBinaryModule.class, "doTearDown"));

        return testSuite;
    }

    public String getResourceDir() {
        return "jee/JEEBinaryModule";
    }

    public String getClassName() {
        return JEEBinaryModule.class.getSimpleName();
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        if (!runtimeSupportsFeature(EJB_FEATURE)) {
            print("ATTENTION: Runtime does not support feature " + EJB_FEATURE + ", skipping test.");
            return;
        }
        createServer(runtime, SERVER_NAME, "resources/jee/JEEBinaryModule");
        createVM();

        importProjects(new Path(getResourceDir()),
                       new String[] { "Web1EAR" });

        startServer();
        wait("Wait before adding application.", 3000);
        addApp("Web1EAR", false, 30);
    }

    @Test
    // test html in Web1
    public void testWeb() throws Exception {
        if (!runtimeSupportsFeature(EJB_FEATURE))
            return;
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/Web1Servlet", "Hello from Web1Servlet (Util1 class, EJB1 stateless bean, Util2 class, EJB2 stateless bean)");
    }

    @Test
    // test jsp in Web1
    public void testWebFragment() throws Exception {
        if (!runtimeSupportsFeature(EJB_FEATURE))
            return;
        testPingWebPage("Web1/Frag1Servlet", "Hello from Frag1Servlet (Util1 class, EJB1 stateless bean)");
    }

    @Test
    // switch loose config mode
    public void switchLooseCfgMode() throws Exception {
        if (!runtimeSupportsFeature(EJB_FEATURE))
            return;
        boolean isLC = isLooseCfg();
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
        wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, !isLC);
        wc.save(true, null);
        safePublishIncremental(server);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/Web1Servlet", "Hello from Web1Servlet (Util1 class, EJB1 stateless bean, Util2 class, EJB2 stateless bean)");
        testPingWebPage("Web1/Frag1Servlet", "Hello from Frag1Servlet (Util1 class, EJB1 stateless bean)");
    }

    @Test
    public void doTearDown() throws Exception {
        if (runtimeSupportsFeature(EJB_FEATURE)) {
            removeApp("Web1EAR", 2500);
            stopServer();
        }
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }
}