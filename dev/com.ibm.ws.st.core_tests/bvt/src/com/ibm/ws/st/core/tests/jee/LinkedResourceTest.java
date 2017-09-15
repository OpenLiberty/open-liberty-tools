/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

@RunWith(AllTests.class)
public class LinkedResourceTest extends JEETestBase {

    private static final String SERVER_NAME = "LinkedResourceTestServer";
    private static final String PROJECT_NAME = "LinkedResourceProject";
    private static final String USER_DIR_NAME = "LinkedResourceUserDir";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(LinkedResourceTest.getOrderedTests());
        suite.setName(LinkedResourceTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(LinkedResourceTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(LinkedResourceTest.class, "testLocalHTML"));
        testSuite.addTest(TestSuite.createTest(LinkedResourceTest.class, "testLinkedHTML"));
        testSuite.addTest(TestSuite.createTest(LinkedResourceTest.class, "testLinkedHTMLFolder"));
        testSuite.addTest(TestSuite.createTest(LinkedResourceTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: LinkedResourceTest");
        init();
        createRuntime();
        createVM();
        importProjects(new Path("JEEDDtesting/LinkedResource"), new String[] { USER_DIR_NAME, PROJECT_NAME });

        // Set up the links
        IProject project = getProject(PROJECT_NAME);
        IFolder webContentFolder = project.getFolder("WebContent");
        IPath path = resourceFolder.append("JEEDDtesting/LinkedResource");
        IFile file = webContentFolder.getFile("linked.html");
        file.createLink(path.append("linked.html"), IResource.REPLACE, null);
        IFolder folder = webContentFolder.getFolder("htmlFolder");
        folder.createLink(path.append("htmlFolder"), IResource.REPLACE, null);

        project = getProject(USER_DIR_NAME);
        addUserDir(project);
        createServer(runtime, project.getLocation(), SERVER_NAME, null);
    }

    @Test
    // Test local html file
    public void testLocalHTML() throws Exception {
        startServer();
        addApp(PROJECT_NAME, false, 10);
        testPingWebPage(PROJECT_NAME + "/local.html", "Hello from local.html");
    }

    @Test
    // Test linked html file
    public void testLinkedHTML() throws Exception {
        testPingWebPage(PROJECT_NAME + "/linked.html", "Hello from linked.html");
    }

    @Test
    // Test linked html file
    public void testLinkedHTMLFolder() throws Exception {
        testPingWebPage(PROJECT_NAME + "/htmlFolder/linked2.html", "Hello from linked2.html");
    }

    @Test
    public void doTearDown() throws Exception {
        stopServer();
        removeApp(PROJECT_NAME, 1000);
        WLPCommonUtil.cleanUp();
        wait("Ending test: LinkedResourceTest\n", 5000);
    }

}