/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import java.io.IOException;

import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check WebSphere server", isStable = true)
@RunWith(AllTests.class)
public class WebSphereServerPublishWIncludeTest extends ToolsTestBase {
    protected static IProject project;

    protected static IServerWorkingCopy serverWC;
    protected static WebSphereServerBehaviour wsServerBehaviour;
    protected static long timestamp;

    protected static final String WEB_MODULE_NAME = "ServerTestMyWeb";
    protected static final String CLOSED_PROJECT = "ServerTestClosedProject";
    public static IModule webModule;

    protected static final String RUNTIME_NAME = WebSphereServerPublishWIncludeTest.class.getCanonicalName() + "_runtime";

    static final String SERVER = "server";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebSphereServerPublishWIncludeTest.getOrderedTests());
        suite.setName(WebSphereServerPublishWIncludeTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testAdaptServer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testAdaptServerBehaviour"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testCreateWebModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testCreateWebContent"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testGetModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testCanAddModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testHasModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testNotPublishedYet"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testStart"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testPingWebModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testAddModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testHasModule2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testPublishServerWithInclude"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testPublishWorked"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testPingWebModule2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testStop4"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testRemoveModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testHasModule3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testPublish3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "testPublishRemoved"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerPublishWIncludeTest.class, "doTearDown"));

        return testSuite;
    }

    protected IServerAttributes getServerAttributes() throws Exception {
        return server;
    }

    protected IServerWorkingCopy getServerWorkingCopy() throws Exception {
        if (serverWC == null)
            serverWC = server.createWorkingCopy();

        return serverWC;
    }

    public void clearWorkingCopy() {
        serverWC = null;
    }

    protected static String getRuntimeName() {
        return RUNTIME_NAME;
    }

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

    @Test
    public void doSetup() throws Exception {
        // We have another child test case for testing 85 server
        print("Starting test: " + getClass().getName());
        init();
        createRuntime();

        // Copy over shared config files
        IPath resourcePath = resourceFolder.append("config/sharedConfig");
        IPath runtimePath = (new Path(runtimeLocation)).append("/usr/shared/config");
        FileUtil.copyFiles(resourcePath.toPortableString(), runtimePath.toPortableString());

        createServer(runtime, SERVER, "resources/config/includePublishTestServer");
        createVM(JDK_NAME);
    }

    @Test
    public void testAdaptServer() throws Exception {
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        assertNotNull(wsServer);
        assertNotNull(wsServer.getServerPath());
    }

    @Test
    public void testAdaptServerBehaviour() throws Exception {
        wsServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        assertNotNull(wsServerBehaviour);
    }

    @Test
    public void testCreateWebModule() throws Exception {
        ModuleHelper.createWebModule(WEB_MODULE_NAME, server.getRuntime());
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testCreateWebContent() throws Exception {
        ModuleHelper.createWebContent(WEB_MODULE_NAME, 0);
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testGetModule() throws Exception {
        ModuleHelper.buildFull();
        webModule = ModuleHelper.getModule(WEB_MODULE_NAME);
    }

    @Test
    public void testCanAddModule() throws Exception {
        IStatus status = server.canModifyModules(new IModule[] { webModule }, null, null);
        assertTrue(status.isOK());
    }

    @Test
    public void testHasModule() throws Exception {
        IModule[] modules = server.getModules();
        int size = modules.length;
        boolean found = false;
        for (int i = 0; i < size; i++) {
            if (webModule.equals(modules[i]))
                found = true;
        }
        if (found)
            assertTrue(false);
    }

    @Test
    public void testNotPublishedYet() {
        IPath path = server.getRuntime().getLocation();
        path = path.append("usr/servers").append(wsServer.getServerName());
        path = path.append("apps").append(webModule.getName() + ".war");
        assertTrue(!path.toFile().exists());
    }

    @Test
    public void testStart() throws Exception {
        startServer();
        assertTrue("Server state should be started but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STARTED);
    }

    @Test
    public void testPingWebModule() throws Exception {
        wait("Wait 3 seconds before ping", 3000);
        testWebPageNotFound(webModule.getName() + "/test0.html", "Hello!");
    }

    @Test
    public void testAddModule() throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(new IModule[] { webModule }, null, null);
        wc.save(true, null);
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testHasModule2() throws Exception {
        IModule[] modules = server.getModules();
        int size = modules.length;
        boolean found = false;
        for (int i = 0; i < size; i++) {
            if (webModule.equals(modules[i]))
                found = true;
        }
        if (!found)
            assertTrue(false);
    }

    @Test
    public void testPublishServerWithInclude() throws Exception {
        safePublishFull(server);
    }

    @Test
    public void testPublishWorked() {
        IPath path = server.getRuntime().getLocation();
        path = path.append("usr/servers").append(wsServer.getServerName());
        path = path.append("apps").append(webModule.getName() + ".war.xml");
        assertTrue(path.toFile().exists());
    }

    @Test
    public void testPingWebModule2() throws Exception {
        wait("Wait 3 seconds before ping", 3000);
        testPingWebPage(webModule.getName() + "/test0.html", "Hello!");
    }

    @Test
    public void testStop4() throws Exception {
        stopServer();
        assertTrue("Server state should be stopped but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STOPPED);
    }

    @Test
    public void testRemoveModule() throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(null, new IModule[] { webModule }, null);
        wc.save(true, null);
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testHasModule3() throws Exception {
        IModule[] modules = server.getModules();
        int size = modules.length;
        boolean found = false;
        for (int i = 0; i < size; i++) {
            if (webModule.equals(modules[i]))
                found = true;
        }
        if (found)
            assertTrue(false);
    }

    @Test
    public void testPublish3() throws Exception {
        safePublishFull(server);
    }

    @Test
    public void testPublishRemoved() {
        IPath path = server.getRuntime().getLocation();
        path = path.append("usr/servers").append(wsServer.getServerName());
        path = path.append("apps").append(webModule.getName() + ".war.xml");
        assertTrue(!path.toFile().exists());
    }

    @Test
    public void doTearDown() {
        cleanUp();

        // Delete shared config files
        IPath runtimePath = (new Path(runtimeLocation)).append("/usr/shared/config");
        try {
            FileUtil.deleteDirectoryContents(runtimePath.toPortableString());
        } catch (IOException e) {
            print("Failed to clean up the shared config dir " + runtimePath.toOSString() + " (" + e + ")");
        }

        print("Ending test: " + getClass().getName() + "\n");
    }
}