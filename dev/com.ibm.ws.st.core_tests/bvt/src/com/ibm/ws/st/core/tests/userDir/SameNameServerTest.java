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
package com.ibm.ws.st.core.tests.userDir;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.TestsPlugin;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.LibertyTestUtil;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test servers with the same name, one in default user dir, one in project user dir and one in external user dir", isStable = true)
@RunWith(AllTests.class)
public class SameNameServerTest extends ToolsTestBase {

    private static String RESOURCE_PATH = "userDirTesting/sameNameServers";
    private static String PROJECTS_PATH = RESOURCE_PATH + "/projects";
    private static String SERVER_NAME = "defaultServer";
    private static String PROJECT_USER_DIR = "projectUserDir";
    private static String EXTERNAL_USER_DIR = "externalUserDir";
    private static String UPDATE_FOLDER = "fileUpdates";

    private static WebSphereServer defaultServer;
    private static WebSphereServer projectServer;
    private static WebSphereServer externalServer;
    private static IPath externalPath;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(SameNameServerTest.getOrderedTests());
        suite.setName(SameNameServerTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testAddWarToDefault"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testAddWarToProject"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testAddWarToExternal"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testUpdateServlet1"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testUpdateServlet2"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testUpdateServlet3"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testRemoveWeb1"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testRemoveWeb2"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "testRemoveWeb3"));
        testSuite.addTest(TestSuite.createTest(SameNameServerTest.class, "doTearDown"));

        return testSuite;
    }

    public String getClassName() {
        return SameNameServerTest.class.getSimpleName();
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();

        // Create the runtime with the default user dir
        createRuntime(RUNTIME_NAME);

        // Add a project user dir
        importProjects(new Path(RESOURCE_PATH), new String[] { PROJECT_USER_DIR });
        IProject project = getProject(PROJECT_USER_DIR);
        addUserDir(project);

        // Add an external user dir
        externalPath = TestsPlugin.getInstance().getStateLocation().append(EXTERNAL_USER_DIR);
        IPath sourcePath = resourceFolder.append(RESOURCE_PATH).append(EXTERNAL_USER_DIR);
        FileUtil.copyFiles(sourcePath.toOSString(), externalPath.toOSString());
        addUserDir(externalPath);

        // Create server in default user dir
        WebSphereRuntime wsRuntime = getWebSphereRuntime();
        UserDirectory userDir = wsRuntime.getDefaultUserDir();
        LibertyTestUtil.createLibertyServer(wsRuntime.getRuntime(), SERVER_NAME, userDir);
        IServer server = ServerTestUtil.createServer(wsRuntime.getRuntime(), getServerInfo(userDir, "defaultServer"));
        defaultServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);

        // Create server in project user dir
        userDir = wsRuntime.getUserDir(PROJECT_USER_DIR);
        server = ServerTestUtil.createServer(wsRuntime.getRuntime(), getServerInfo(userDir, "projectServer"));
        projectServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);

        // Create server in external user dir
        userDir = wsRuntime.getUserDir(externalPath.toPortableString());
        server = ServerTestUtil.createServer(wsRuntime.getRuntime(), getServerInfo(userDir, "externalServer"));
        externalServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);

        createVM(JDK_NAME);
        importProjects(new Path(PROJECTS_PATH), new String[] { "Web1", "Web2", "Web3" });
        ServerTestUtil.startServer(defaultServer.getServer());
        ServerTestUtil.startServer(projectServer.getServer());
        ServerTestUtil.startServer(externalServer.getServer());
        wait("Wait 10 seconds before adding application.", 3000);
    }

    @Test
    // test starting the web module
    public void testAddWarToDefault() throws Exception {
        ServerTestUtil.addApp(defaultServer.getServer(), "Web1", false, 30);
        wait("Wait 3 seconds before ping.", 3000);
        ServerTestUtil.testPingWebPage(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 0");
    }

    @Test
    // test starting the web module
    public void testAddWarToProject() throws Exception {
        ServerTestUtil.addApp(projectServer.getServer(), "Web2", false, 30);
        wait("Wait 3 seconds before ping.", 3000);
        ServerTestUtil.testPingWebPage(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 0");
        ServerTestUtil.testPingWebPage(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 0");
    }

    @Test
    // test starting the web module
    public void testAddWarToExternal() throws Exception {
        ServerTestUtil.addApp(externalServer.getServer(), "Web3", false, 30);
        wait("Wait 3 seconds before ping.", 3000);
        ServerTestUtil.testPingWebPage(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 0");
        ServerTestUtil.testPingWebPage(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 0");
        ServerTestUtil.testPingWebPage(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 0");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateServlet1() throws Exception {
        ServerTestUtil.updateFile(defaultServer.getServer(), getUpdatedFileFolder().append("Web1Servlet.java"),
                                  getProject("Web1"), "src/web1/Web1Servlet.java", 2000);
        ServerTestUtil.testPingWebPage(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 1");
        ServerTestUtil.testPingWebPage(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 0");
        ServerTestUtil.testPingWebPage(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 0");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateServlet2() throws Exception {
        ServerTestUtil.updateFile(projectServer.getServer(), getUpdatedFileFolder().append("Web2Servlet.java"),
                                  getProject("Web2"), "src/web2/Web2Servlet.java", 2000);
        ServerTestUtil.testPingWebPage(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 1");
        ServerTestUtil.testPingWebPage(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 2");
        ServerTestUtil.testPingWebPage(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 0");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateServlet3() throws Exception {
        ServerTestUtil.updateFile(externalServer.getServer(), getUpdatedFileFolder().append("Web3Servlet.java"),
                                  getProject("Web3"), "src/web3/Web3Servlet.java", 2000);
        ServerTestUtil.testPingWebPage(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 1");
        ServerTestUtil.testPingWebPage(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 2");
        ServerTestUtil.testPingWebPage(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 3");
    }

    @Test
    // test remove a servlet from a web module
    public void testRemoveWeb1() throws Exception {
        ServerTestUtil.removeApp(defaultServer.getServer(), "Web1", 2500);
        ServerTestUtil.testWebPageNotFound(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 1");
        ServerTestUtil.testPingWebPage(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 2");
        ServerTestUtil.testPingWebPage(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 3");
    }

    @Test
    // test remove a servlet from a web module
    public void testRemoveWeb2() throws Exception {
        ServerTestUtil.removeApp(projectServer.getServer(), "Web2", 2500);
        ServerTestUtil.testWebPageNotFound(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 1");
        ServerTestUtil.testWebPageNotFound(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 2");
        ServerTestUtil.testPingWebPage(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 3");
    }

    @Test
    // test remove a servlet from a web module
    public void testRemoveWeb3() throws Exception {
        ServerTestUtil.removeApp(externalServer.getServer(), "Web3", 2500);
        ServerTestUtil.testWebPageNotFound(defaultServer.getServer(), "Web1/Web1Servlet", "Web1Servlet: 1");
        ServerTestUtil.testWebPageNotFound(projectServer.getServer(), "Web2/Web2Servlet", "Web2Servlet: 2");
        ServerTestUtil.testWebPageNotFound(externalServer.getServer(), "Web3/Web3Servlet", "Web3Servlet: 3");
    }

    @Test
    public void doTearDown() throws Exception {
        try {
            if (defaultServer != null)
                ServerTestUtil.stopServer(defaultServer.getServer());
            if (projectServer != null)
                ServerTestUtil.stopServer(projectServer.getServer());
            if (externalServer != null)
                ServerTestUtil.stopServer(externalServer.getServer());
        } catch (Exception e) {
            print("Server cleanup failed for test '" + getClassName() + "'", e);
        }
        WLPCommonUtil.cleanUp();
        if (externalPath != null) {
            try {
                FileUtil.deleteDirectory(externalPath.toOSString(), true);
            } catch (Exception e) {
                print("Failed to clean up external server directory: " + externalPath.toOSString(), e);
            }
        }
        wait("Ending test: " + getClassName() + "\n", 5000);
    }

    protected IPath getUpdatedFileFolder() {
        return resourceFolder.append(RESOURCE_PATH).append(UPDATE_FOLDER);
    }

    protected Map<String, String> getServerInfo(UserDirectory userDir, String serverName) {
        Map<String, String> serverInfo = new HashMap<String, String>();
        serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_SERVER_NAME, SERVER_NAME);
        serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.SERVER_LABEL, serverName);
        serverInfo.put(com.ibm.ws.st.common.core.ext.internal.Constants.LIBERTY_USER_DIR, userDir.getPath().toPortableString());
        return serverInfo;
    }

}
