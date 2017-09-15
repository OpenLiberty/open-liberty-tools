/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
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
public class JEEPublishJCA extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishJCA.getOrderedTests());
        suite.setName(JEEPublishJCA.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testADD"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testFIND"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testStopRAR"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testStartRAR"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "doTearDown"));

        return testSuite;
    }

    public String getResourceDir() {
        return "jee/JEEPublishJCA";
    }

    public String getClassName() {
        return JEEPublishJCA.class.getSimpleName();
    }

    public String getJCAFeature() {
        return "jca-1.7";
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        if (!runtimeSupportsFeature(getJCAFeature())) {
            print("ATTENTION: Runtime does not support feature " + getJCAFeature() + ", skipping test.");
            return;
        }
        createServer(runtime, SERVER_NAME, "resources/jee/JEEPublishJCA");
        createVM();

        importProjects(new Path(getResourceDir()),
                       new String[] { "ExampleRA", "ExampleApp" });

        startServer();
        wait("Wait 10 seconds before adding application.", 3000);
        addApp("ExampleRA", false, 30);
        addApp("ExampleApp", false, 30);
    }

    @Test
    // test html in Web1
    public void testADD() throws Exception {
        if (!runtimeSupportsFeature(getJCAFeature()))
            return;
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("ExampleApp?functionName=ADD&x=2&y=3", "Successfully performed ADD with output: {x=2, y=3}");
    }

    @Test
    // test jsp in Web1
    public void testFIND() throws Exception {
        if (!runtimeSupportsFeature(getJCAFeature()))
            return;
        testPingWebPage("ExampleApp?functionName=FIND&y=3", "Successfully performed FIND with output: {x=2, y=3}");
    }

    @Test
    // test stopping the web module
    public void testStopRAR() throws Exception {
        if (!runtimeSupportsFeature(getJCAFeature()))
            return;
        stopApp("ExampleRA", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testWebPageNotFound("ExampleApp?functionName=FIND&y=3", "Successfully performed FIND with output: {x=2, y=3}");
    }

    @Test
    // test starting the web module
    public void testStartRAR() throws Exception {
        if (!runtimeSupportsFeature(getJCAFeature()))
            return;
        startApp("ExampleRA", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("ExampleApp?functionName=FIND&y=3", "Did not FIND any entries.");
    }

    @Test
    // switch loose config mode
    public void switchLooseCfgMode() throws Exception {
        if (!runtimeSupportsFeature(getJCAFeature()))
            return;
        boolean isLC = isLooseCfg();
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        IServerWorkingCopy wc = wsServer.getServerWorkingCopy();
        wc.setAttribute(WebSphereServer.PROP_LOOSE_CONFIG, !isLC);
        wc.save(true, null);
        safePublishIncremental(server);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("ExampleApp?functionName=ADD&x=2&y=3", "Successfully performed ADD with output: {x=2, y=3}");
        testPingWebPage("ExampleApp?functionName=FIND&y=3", "Successfully performed FIND with output: {x=2, y=3}");
    }

    @Test
    public void doTearDown() throws Exception {
        if (runtimeSupportsFeature(getJCAFeature())) {
            removeApp("ExampleApp", 2500);
            removeApp("ExampleRA", 2500);
            stopServer();
        }
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }
}