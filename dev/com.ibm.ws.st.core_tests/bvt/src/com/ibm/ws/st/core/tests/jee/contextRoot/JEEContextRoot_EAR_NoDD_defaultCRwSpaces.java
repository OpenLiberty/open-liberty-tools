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
package com.ibm.ws.st.core.tests.jee.contextRoot;

import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.util.WebResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebLaunchable;
import com.ibm.ws.st.core.tests.jee.JEETestBase;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "JEE context root test - Web (with spaces in name) in EAR. Context root not defined in EAR so fall back to web module context root.", isStable = true)
@RunWith(AllTests.class)
public class JEEContextRoot_EAR_NoDD_defaultCRwSpaces extends JEETestBase {

    private static final String TEST_NAME = "JEEContextRoot_EAR_NoDD_defaultCRwSpaces";
    private static final String APP = "Hello WorldEAR";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEContextRoot_EAR_NoDD_defaultCRwSpaces.getOrderedTests());
        suite.setName(JEEContextRoot_EAR_NoDD_defaultCRwSpaces.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEContextRoot_EAR_NoDD_defaultCRwSpaces.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEContextRoot_EAR_NoDD_defaultCRwSpaces.class, "testContextRoot"));
        testSuite.addTest(TestSuite.createTest(JEEContextRoot_EAR_NoDD_defaultCRwSpaces.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEEContextRoot_EAR_NoDD_defaultCRwSpaces");
        init();
        createRuntime();
        createServer();
        createVM(JDK_NAME);

        importProjects(new Path("JEEDDtesting/contextRoot/" + TEST_NAME + "/ws"), new String[] { "Hello World", "Hello WorldEAR" });

        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp(APP);
    }

    @Test
    public void testContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("Hello World");
        IModuleArtifact servlet = new WebResource(module, new Path("/*"));
        WebLaunchable launch = new WebLaunchable(server, servlet);
        String url = launch.getURL().toString();
        String expected = "http://localhost:9080/Hello_World/*";
        print("returned url " + url);
        assertEquals("The context root is incorrect. expected: " + expected + " actual: " + url, expected, url);
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp(APP, 2500); //in case the publish is not done.
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEEContextRoot_EAR_NoDD_defaultCRwSpaces\n", 5000);
    }
}