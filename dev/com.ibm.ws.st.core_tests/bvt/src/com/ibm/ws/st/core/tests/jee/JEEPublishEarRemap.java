/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test publish of enterprise app with EJB (defect 115237)", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishEarRemap extends JEETestBase {

    public String getResourceDir() {
        return "jee/JEEPublishEarRemap";
    }

    protected IPath getUpdatedFileFolder() {
        return resourceFolder.append(getResourceDir() + "/files");
    }

    public String getClassName() {
        return JEEPublishEarRemap.class.getSimpleName();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishEarRemap.getOrderedTests());
        suite.setName(JEEPublishEarRemap.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishEarRemap.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarRemap.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarRemap.class, "testUpdateEJB"));
        testSuite.addTest(TestSuite.createTest(JEEPublishEarRemap.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        createServer();
        createVM();

        importProjects(new Path("jee/JEEPublishEarRemap"),
                       new String[] { "EJBApp_EJB", "EJB_APP_WEB", "EJBApp" });

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("EJBApp", false, 30);
    }

    @Test
    // test servlet that calls ejb
    public void testServlet() throws Exception {
        testPingWebPage("EJBApp", "Hello EJB World.");
    }

    @Test
    // test update ejb
    public void testUpdateEJB() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("SampleStatelessBean.java"),
                   getProject("EJBApp_EJB"), "ejbModule/wasdev/sample/ejb/SampleStatelessBean.java", 2000);
        testPingWebPage("EJBApp", "Hello EJB World - Modified.");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EJBApp", 2500);
        stopServer();
        cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }

}