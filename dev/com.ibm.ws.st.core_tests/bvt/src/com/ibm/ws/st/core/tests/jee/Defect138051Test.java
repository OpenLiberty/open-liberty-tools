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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish works where class files are not on the default path", isStable = true)
@RunWith(AllTests.class)
public class Defect138051Test extends JEETestBase {

    private static final String RESOURCE_DIR = "jee/Defect138051";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(Defect138051Test.getOrderedTests());
        suite.setName(Defect138051Test.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(Defect138051Test.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(Defect138051Test.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(Defect138051Test.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(Defect138051Test.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(Defect138051Test.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: Defect138051Test");
        init();
        createRuntime();
        createServer();
        createVM();

        importProjects(new Path("jee/Defect138051"), new String[] { "Defect138051Project" });

        startServer();
        wait("wait 3 seconds before adding application.", 3000);
        addApp("Defect138051Project", false, 30);
    }

    @Test
    // test servlet
    public void testServlet() throws Exception {
        testPingWebPage("Defect138051Project/TestServlet", "Hello from TestServlet");
    }

    @Test
    // test update servlet
    public void testUpdateServlet() throws Exception {
        IPath path = resourceFolder.append(RESOURCE_DIR).append("files");
        updateFile(path.append("TestServlet.java"), getProject("Defect138051Project"),
                   "src/test/TestServlet.java", 2000);
        testPingWebPage("Defect138051Project/TestServlet", "Hello from TestServlet - modified");
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
        wait("wait 3 seconds before ping.", 3000);
        testPingWebPage("Defect138051Project/TestServlet", "Hello from TestServlet - modified");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("Defect138051Project", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: Defect138051Test\n", 5000);
    }
}