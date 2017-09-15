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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish of web app, deploy name comes from IModule2 (defect 115237)", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishWarDeployName extends JEETestBase {

    public String getResourceDir() {
        return "jee/JEEPublishWarDeployName";
    }

    protected IPath getUpdatedFileFolder() {
        return resourceFolder.append(getResourceDir() + "/files");
    }

    public String getClassName() {
        return JEEPublishWarDeployName.class.getSimpleName();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishWarDeployName.getOrderedTests());
        suite.setName(JEEPublishWarDeployName.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishWarDeployName.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDeployName.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDeployName.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDeployName.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        createServer();
        createVM();

        importProjects(new Path("jee/JEEPublishWarDeployName"),
                       new String[] { "WebProject" });

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("WebProject", false, 30);
    }

    @Test
    // test servlet that calls ejb
    public void testServlet() throws Exception {
        testPingWebPage("WebProject/TestServlet", "Hello from TestServlet");
    }

    @Test
    // test update ejb
    public void testUpdateServlet() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("TestServlet.java"),
                   getProject("WebProject"), "src/test/TestServlet.java", 2000);
        testPingWebPage("WebProject/TestServlet", "Hello from TestServlet - Modified");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("WebProject", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }

}