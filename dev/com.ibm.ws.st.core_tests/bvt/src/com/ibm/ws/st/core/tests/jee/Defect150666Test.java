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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish works when a utility project depends on an external jar", isStable = true)
@RunWith(AllTests.class)
public class Defect150666Test extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(Defect150666Test.getOrderedTests());
        suite.setName(Defect150666Test.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(Defect150666Test.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(Defect150666Test.class, "testServlet1"));
        testSuite.addTest(TestSuite.createTest(Defect150666Test.class, "testServlet2"));
        testSuite.addTest(TestSuite.createTest(Defect150666Test.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: Defect150666Test");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("jee/Defect150666"), new String[] {
                                                                   "ResourceProject",
                                                                   "UtilProject",
                                                                   "WebProject",
                                                                   "WebProjectEAR" });
        startServer();
    }

    @Test
    public void testServlet1() throws Exception {
        wait("wait 3 seconds before adding application.", 3000);
        addApp("WebProject");
        wait("Wait 5 seconds before ping", 5000);
        testPingWebPage("WebProject/TestServlet", "Hello from TestServlet Util.getInfo() Util2.getInfo");
        removeApp("WebProject", 2500);
    }

    @Test
    public void testServlet2() throws Exception {
        wait("wait 3 seconds before adding application.", 3000);
        addApp("WebProjectEAR", false, 30);
        wait("Wait 5 seconds before ping", 5000);
        testPingWebPage("WebProject/TestServlet", "Hello from TestServlet Util.getInfo() Util2.getInfo");
        removeApp("WebProjectEAR", 2500);
    }

    @Test
    public void doTearDown() throws Exception {
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: Defect150666Test\n", 5000);
    }
}