/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.module;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.wst.server.core.IModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Module tests", isStable = true)
@RunWith(AllTests.class)
public class ModuleTestCase extends TestCase {
    protected static final String WEB_MODULE_NAME = "MyWeb";
    protected static final String CLOSED_PROJECT = "ClosedProject";
    public static IModule webModule;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ModuleTestCase.getOrderedTests());
        suite.setName(ModuleTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test00ClosedProject"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test01CreateWebModule"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test02CreateWebContent"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test04GetModule"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test05CountFilesInModule"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test00DeleteWebModule"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "test01DeleteClosedProject"));
        testSuite.addTest(TestSuite.createTest(ModuleTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        WLPCommonUtil.print("Starting test: ModuleTestCase");
    }

    @Test
    public void test00ClosedProject() throws Exception {
        ModuleHelper.createClosedProject(CLOSED_PROJECT);
    }

    @Test
    public void test01CreateWebModule() throws Exception {
        ModuleHelper.createWebModule(WEB_MODULE_NAME);
    }

    @Test
    public void test02CreateWebContent() throws Exception {
        ModuleHelper.createWebContent(WEB_MODULE_NAME, 0);
    }

    @Test
    public void test04GetModule() throws Exception {
        ModuleHelper.buildFull();
        webModule = ModuleHelper.getModule(WEB_MODULE_NAME);
    }

    @Test
    public void test05CountFilesInModule() throws Exception {
        assertEquals(ModuleHelper.countFilesInModule(webModule), 3);
    }

    @Test
    public void test00DeleteWebModule() throws Exception {
        ModuleHelper.deleteModule(ModuleTestCase.WEB_MODULE_NAME);
    }

    @Test
    public void test01DeleteClosedProject() throws Exception {
        ModuleHelper.deleteModule(ModuleTestCase.CLOSED_PROJECT);
    }

    @Test
    public void doTearDown() {
        WLPCommonUtil.jobWaitBuildandResource();
        WLPCommonUtil.print("Ending test: ModuleTestCase\n");
    }
}