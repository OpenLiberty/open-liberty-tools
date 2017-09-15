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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test add/remove web module from enterprise app with no deployment descriptor", isStable = true)
@RunWith(AllTests.class)
public class JEEAddRemoveWebTest extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEAddRemoveWebTest.getOrderedTests());
        suite.setName(JEEAddRemoveWebTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "testS1InWeb1"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "testAddWeb2"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "testRemoveWeb2"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "testAddWeb3andUtil"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "testDeleteWeb1Project"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "testRemoveWeb3andUtil"));
        testSuite.addTest(TestSuite.createTest(JEEAddRemoveWebTest.class, "doTearDown"));

        return testSuite;
    }

    private IPath getUpdatedFileFolder() {
        return resourceFolder.append("JEEDDtesting/JEEAddRemoveWebUtil/Updated_Files");
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEEAddRemoveWebTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/JEEAddRemoveWebUtil"), new String[] { "EAR_Modify_ModuleEAR",
                                                                                   "EAR_Modify_Web1",
                                                                                   "EAR_Modify_Web2",
                                                                                   "EAR_Modify_Web3",
                                                                                   "EAR_Modify_Util"
        });

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("EAR_Modify_ModuleEAR", false, 30);
    }

    @Test
    // test simple servlet in a web module
    public void testS1InWeb1() throws Exception {
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("EAR_Modify_Web1/a.html", "web1.a.html");
        testPingWebPage("EAR_Modify_Web1/S1", "web1.S1");
    }

    @Test
    // add web2 to the EAR and test
    public void testAddWeb2() throws Exception {
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_1_addWeb2"),
                   getProject("EAR_Modify_ModuleEAR"), ".settings/org.eclipse.wst.common.component",
                   2000);

        wait("Wait 5 seconds before ping.", 5000);
        testPingWebPage("EAR_Modify_Web2/S1", "web2.S1");
        testPingWebPage("EAR_Modify_Web2/a.html", "web2.a.html");
        testPingWebPage("EAR_Modify_Web2/a.jsp", "web2.a.jsp");
    }

    @Test
    // remove web2 to the EAR and test
    public void testRemoveWeb2() throws Exception {
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_0_hasWeb1"),
                   getProject("EAR_Modify_ModuleEAR"), ".settings/org.eclipse.wst.common.component",
                   2000);

        wait("Wait 5 seconds before ping.", 5000);
        testWebPageNotFound("EAR_Modify_Web2/S1", "web2.S1");
        testWebPageNotFound("EAR_Modify_Web2/a.html", "web2.a.html");
        testWebPageNotFound("EAR_Modify_Web2/a.jsp", "web2.a.jsp");
    }

    @Test
    // add web3 and utility module to the EAR and test
    public void testAddWeb3andUtil() throws Exception {
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_3_addUtil"),
                   getProject("EAR_Modify_ModuleEAR"), ".settings/org.eclipse.wst.common.component",
                   2000);

        wait("Wait 5 seconds before ping.", 5000);
        testPingWebPage("EAR_Modify_Web3/S1", "web3.S1");
        testPingWebPage("EAR_Modify_Web3/SCall", "hi from U");
    }

    @Test
    // test deleting a web project that is part of the ear
    public void testDeleteWeb1Project() throws Exception {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = workspaceRoot.getProject("EAR_Modify_Web1");
        project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
        forceBuild();
        safePublishIncremental(server);

        wait("Wait 5 seconds before ping.", 5000);
        testWebPageNotFound("EAR_Modify_Web1/a.html", "web1.a.html");
        testWebPageNotFound("EAR_Modify_Web1/S1", "web1.S1");
        testPingWebPage("EAR_Modify_Web3/S1", "web3.S1");
        testPingWebPage("EAR_Modify_Web3/SCall", "hi from U");
    }

    @Test
    // remove web3 and utility module to the EAR and test
    public void testRemoveWeb3andUtil() throws Exception {
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component_0_hasWeb1"),
                   getProject("EAR_Modify_ModuleEAR"), ".settings/org.eclipse.wst.common.component",
                   2000);

        wait("Wait 5 seconds before ping.", 5000);
        testWebPageNotFound("EAR_Modify_Web3/S1", "web3.S1");
        testWebPageNotFound("EAR_Modify_Web3/SCall", "hi from U");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("EAR_Modify_ModuleEAR", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEEAddRemoveWebTest\n", 5000);
    }
}