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
package com.ibm.ws.st.core.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.internal.RuntimeClasspathContainer;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebSphereRuntimeClasspathProvider;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check WebSphere server", isStable = true)
@RunWith(AllTests.class)
public class WebSphereServerTestCase extends ToolsTestBase {
    private static final String WORK_AREA = "workarea";

    protected static IProject project;

    protected static IServerWorkingCopy serverWC;
    protected static WebSphereServerBehaviour wsServerBehaviour;
    protected static long timestamp;

    protected static final String WEB_MODULE_NAME = "ServerTestMyWeb";
    protected static final String CLOSED_PROJECT = "ServerTestClosedProject";
    public static IModule webModule;

    protected static final String RUNTIME_NAME = WebSphereServerTestCase.class.getCanonicalName() + "_runtime";

    static final String DIR = "/ServerTesting/";
    static final String SERVER = "server";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(WebSphereServerTestCase.getOrderedTests());
        suite.setName(WebSphereServerTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPublish"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCanRun"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testRun"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCanStop"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStop"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testAdaptServer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCleanable"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testAdaptServerBehaviour"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCleanOnStartup"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCanDebug"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testDebug"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCleaned"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCanStop2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStop2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testWebSphereServer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testWebSphereServerBehaviour"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCreateClosedProject"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCreateWebModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testRuntimeTargetClasspathContainer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testRuntimeTargetClasspathContainerSorting"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCreateWebContent"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testGetModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testCanAddModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testHasModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testNotPublishedYet"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStart"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPingWebModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStop3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testAddModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testHasModule2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPublish2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPublishWorked"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStart2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPingWebModule2"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStop4"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testRemoveModule"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testHasModule3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPublish3"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testPublishRemoved"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testExternallyStartedServer"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "testStop5"));
        testSuite.addTest(TestSuite.createTest(WebSphereServerTestCase.class, "doTearDown"));

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

    public void deleteProject() throws Exception {
        if (project != null) {
            project.delete(true, true, null);
        }
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
        createServer(runtime, SERVER, "resources/" + DIR + "/" + SERVER);
        createVM(JDK_NAME);
    }

    @Test
    public void testPublish() throws Exception {
        safePublishFull(server);
    }

    @Test
    public void testCanRun() throws Exception {
        assertTrue(server.canStart(ILaunchManager.RUN_MODE).isOK());
    }

    @Test
    public void testRun() throws Exception {
        startServer();
        assertTrue("Server state should be started but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STARTED);
    }

    @Test
    public void testCanStop() throws Exception {
        assertTrue(server.canStop().isOK());
    }

    @Test
    public void testStop() throws Exception {
        stopServer();
        assertTrue("Server state should be stopped but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STOPPED);
    }

    @Test
    public void testAdaptServer() throws Exception {
        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        assertNotNull(wsServer);
        assertNotNull(wsServer.getServerPath());
    }

    @Test
    public void testCleanable() throws Exception {
        IPath path = wsServer.getServerPath().append(WORK_AREA);
        assertTrue(path.toFile().exists());
        timestamp = path.toFile().lastModified();
    }

    @Test
    public void testAdaptServerBehaviour() throws Exception {
        wsServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        assertNotNull(wsServerBehaviour);
    }

    @Test
    public void testCleanOnStartup() throws Exception {
        wsServerBehaviour.setCleanOnStartup(true);
    }

    @Test
    public void testCanDebug() throws Exception {
        assertTrue(server.canStart(ILaunchManager.DEBUG_MODE).isOK());
    }

    @Test
    public void testDebug() throws Exception {
        debugServer();
        assertTrue("Server state should be started but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STARTED);
        assertTrue("Server mode should be debug but is: " + server.getMode(), server.getMode().equals(ILaunchManager.DEBUG_MODE));
    }

    @Test
    public void testCleaned() throws Exception {
        IPath path = wsServer.getServerPath().append(WORK_AREA);
        assertTrue(path.toFile().exists());
        assertTrue(path.toFile().lastModified() > timestamp);
    }

    @Test
    public void testCanStop2() throws Exception {
        assertTrue(server.canStop().isOK());
    }

    @Test
    public void testStop2() throws Exception {
        stopServer();
        assertTrue("Server state should be stopped but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STOPPED);
    }

    @Test
    public void testWebSphereServer() {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        assertNotNull(ws);
    }

    @Test
    public void testWebSphereServerBehaviour() {
        WebSphereServerBehaviour wsb = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        assertNotNull(wsb);
    }

    @Test
    public void testCreateClosedProject() throws Exception {
        ModuleHelper.createClosedProject(CLOSED_PROJECT);
    }

    @Test
    public void testCreateWebModule() throws Exception {
        ModuleHelper.createWebModule(WEB_MODULE_NAME, server.getRuntime());
        TestUtil.jobWaitBuildandResource();
    }

    @Test
    public void testRuntimeTargetClasspathContainer() throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(WEB_MODULE_NAME);
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] curClasspathEntries = javaProject.getRawClasspath();
        boolean isSpecFound = false;
        boolean isIBMAPIFound = false;
        boolean isThirdPartyFound = false;
        boolean isSPIFound = false;
        boolean isToolsFound = false;
        // Don't do javadoc test for 8.5 servers since it was not available
        boolean isJavaDocFound = getRuntimeVersion().startsWith("8.5");
        for (IClasspathEntry curClasspathEntry : curClasspathEntries) {
            if (curClasspathEntry.getPath().toString().contains(RuntimeClasspathContainer.SERVER_CONTAINER)) {
                IClasspathContainer container = JavaCore.getClasspathContainer(curClasspathEntry.getPath(), javaProject);
                assertNotNull(container);
                IClasspathEntry[] detailedClasspathEntries = container.getClasspathEntries();
                for (IClasspathEntry detailedClasspathEntry : detailedClasspathEntries) {
                    String curPath = detailedClasspathEntry.getPath().toString();
                    // BEGIN: This code should be the same as the OSGi test
                    // The sysout below is for debug purpose to find out all entries.
//                    System.out.println(curPath);
                    if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_DEV + "/")) {
                        if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_IBM_API + "/")) {
                            isIBMAPIFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_API + "/")
                                   && curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_IBM + "/")) {
                            isIBMAPIFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_API + "/")
                                   && curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_SPEC + "/")) {
                            isSpecFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_DEV + "/"
                                                    + WebSphereRuntimeClasspathProvider.FOLDER_SPEC + "/")) {
                            // For WAS 8.5 GA
                            isSpecFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_API + "/")
                                   && curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_THIRD_PARTY + "/")) {
                            isThirdPartyFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_DEV + "/"
                                                    + WebSphereRuntimeClasspathProvider.FOLDER_THIRD_PARTY + "/")) {
                            // For WAS 8.5 GA
                            isThirdPartyFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_SPI + "/")) {
                            isSPIFound = true;
                        } else if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_TOOLS + "/")) {
                            isToolsFound = true;
                        }

                        // Check for existence of URL javadoc entry.
                        if (!isJavaDocFound) {
                            if (curPath.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_API + "/")) {
                                IClasspathAttribute[] extraAttrs = detailedClasspathEntry.getExtraAttributes();
                                if (extraAttrs != null) {
                                    for (IClasspathAttribute curAttr : extraAttrs) {
                                        if ("javadoc_location".equals(curAttr.getName())) {
                                            if (curAttr.getValue() != null) {
                                                String url = curAttr.getValue();
                                                if (url.contains("/" + WebSphereRuntimeClasspathProvider.FOLDER_JAVADOC + "/"))
                                                    isJavaDocFound = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // END: This code should be the same as the OSGi test
                }
            }
        }
        // BEGIN: This code should be the same as the OSGi test
        assertTrue("There are missing or unexpected classpath entries: isSpecFound=" + isSpecFound + ", isIBMAPIFound=" + isIBMAPIFound + ", isThirdPartyFound="
                   + isThirdPartyFound + ", isSPIFound=" + isSPIFound + ", isToolsFound=" + isToolsFound, isSpecFound && isIBMAPIFound && isThirdPartyFound && !isSPIFound
                                                                                                          && !isToolsFound);
        // END: This code should be the same as the OSGi test
        assertTrue("No java doc is found.", isJavaDocFound);
    }

    @Test
    public void testRuntimeTargetClasspathContainerSorting() throws Exception {
        String[] inputData = new String[] {
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.basics_1.1.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.basics_1.1.8a.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.clusterMember_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.clusterMember_1.0.9.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.collectiveController_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.collectiveController_2.0.0.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.config_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.config.1.0_1.0.7.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.connectionpool_1.0.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.connectionpool_1.10.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.connectionpool_1.10.10.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.connectionpool_1.20.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.distributedMap_2.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.distributedMap_10.0.8.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.dynamicRouting_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.endpoint_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.hpel_2.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.jaxrs20_1.0.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.jaxrs20_1.1.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.jaxrs10_1.2.jar", //Made up
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.json_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.kernel.service_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.messaging_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.monitor_1.1.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.oauth_1.1.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.restConnector_1.1.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.scalingMember_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.scriptMetric_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.security_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.security.authorization.saf_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.security.registry.saf_1.0.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.servlet_1.0.1.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.sessionstats_1.0.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.sipServlet.1.1_1.0.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.transaction_1.1.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.webCache_1.1.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.webcontainer.security.app_1.1.8.jar",
                                           "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.zosLocalAdapters_1.0.8.jar",
                                           "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.blueprint_1.0.8.jar",
                                           "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.eclipselink_1.0.8.jar",
                                           "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.wsSecurity_1.0.8.jar",
                                           "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.wssxmlSecurity_1.0.8.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.java.sipServlet.1.1_1.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.annotation.1.1_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.annotation.1.2_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.batch.1.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.batch.2.0_1.0.0.jar", //Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.batch.2.0_1.0.1.jar", //Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.batch.1.0_2.0.0.jar", // Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.cdi.1.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.concurrent.1.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.connector.1.6_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.ejb.3.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.el.2.2_1.0.2.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.el2.2_1.0.2.jar", //Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.interceptor.1.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jaspic.1.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jaxb.2.2_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jaxrs.2.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jaxws.2.2_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jms.1.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jms.2.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jsf.2.0_1.0.2.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jsf.2.2_1.0.13.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jsf.tld.2.0_1.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jsonp.1.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jsp.2.2_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jsp.tld.2.2_1.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.jstl.1.2_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.persistence.2.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.servlet.3.0_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.servlet.3.1_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.transaction.1.1_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.transaction.1.2_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.validation.1.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.websocket.1.0_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.websocket.1.1_1.0.0.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.javaee.wsdl4j.1.2_1.0.0.jar",
                                           "/wlp/dev/api/third-party/com.ibm.ws.jpa.api_1.0.8.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.org.osgi.cmpn.4.2.0_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.org.osgi.core.4.2.0_1.0.1.jar",
                                           "/wlp/dev/api/spec/com.ibm.ws.org.osgi.core.4.2.0_20.0.1.jar", //Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.org.osgi.core.4.2.0_3.0.1.jar", //Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.org.osgi.core.5.0.0_3.0.1.jar", //Made up
                                           "/wlp/dev/api/spec/com.ibm.ws.prereq.wsdl4j.api_1.0.jar"
        };
        String[] expectedResult = new String[] {
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.basics_1.1.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.basics_1.1.8a.jar", //Made up. Jar version does not follow OSGi convention. Full string (com.ibm.websphere.appserver.api.basics_1.1.8a.jar) is compared alphabetically to com.ibm.websphere.appserver.api.basics
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.clusterMember_1.0.9.jar", //Made up. Jar version is greater.
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.collectiveController_2.0.0.jar", //Made up. Jar version is greater
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.config_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.connectionpool_1.20.jar", //Made up
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.distributedMap_10.0.8.jar", //Made up. Jar version is greater, and has two digits
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.dynamicRouting_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.endpoint_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.hpel_2.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.jaxrs10_1.2.jar", //Made up. Has greater jar version and lower spec version, but spec version does not follow patter. So, jar version is used
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.jaxrs20_1.1.jar", //Made up. Has greater jar version and same spec version, but spec version does not follow patter. So, jar version is used
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.json_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.kernel.service_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.messaging_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.monitor_1.1.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.oauth_1.1.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.restConnector_1.1.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.scalingMember_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.scriptMetric_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.security.authorization.saf_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.security.registry.saf_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.security_1.0.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.servlet_1.0.1.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.sessionstats_1.0.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.sipServlet.1.1_1.0.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.transaction_1.1.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.webCache_1.1.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.webcontainer.security.app_1.1.8.jar",
                                                "/wlp/dev/api/ibm/com.ibm.websphere.appserver.api.zosLocalAdapters_1.0.8.jar",
                                                "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.blueprint_1.0.8.jar",
                                                "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.eclipselink_1.0.8.jar",
                                                "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.wsSecurity_1.0.8.jar",
                                                "/wlp/dev/api/third-party/com.ibm.websphere.appserver.thirdparty.wssxmlSecurity_1.0.8.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.java.sipServlet.1.1_1.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.annotation.1.2_1.0.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.batch.2.0_1.0.1.jar", //Made up. Spec version is the same, jar version is greater
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.cdi.1.0_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.concurrent.1.0_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.connector.1.6_1.0.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.ejb.3.1_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.el.2.2_1.0.2.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.el2.2_1.0.2.jar", //Made up. Spec version does not follow pattern (. is missing), so com.ibm.ws.javaee.el2.2_1.0.2.jar is compared alphabetically to com.ibm.ws.javaee.el
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.interceptor.1.1_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jaspic.1.1_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jaxb.2.2_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jaxrs.2.0_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jaxws.2.2_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jms.2.0_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jsf.2.2_1.0.13.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jsf.tld.2.0_1.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jsonp.1.0_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jsp.2.2_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jsp.tld.2.2_1.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.jstl.1.2_1.0.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.persistence.2.1_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.servlet.3.1_1.0.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.transaction.1.2_1.0.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.validation.1.1_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.websocket.1.1_1.0.0.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.javaee.wsdl4j.1.2_1.0.0.jar",
                                                "/wlp/dev/api/third-party/com.ibm.ws.jpa.api_1.0.8.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.org.osgi.cmpn.4.2.0_1.0.1.jar",
                                                "/wlp/dev/api/spec/com.ibm.ws.org.osgi.core.5.0.0_3.0.1.jar", //Made up
                                                "/wlp/dev/api/spec/com.ibm.ws.prereq.wsdl4j.api_1.0.jar"
        };

        List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>(inputData.length);

        for (String s : inputData) {
            classpathEntries.add(JavaCore.newLibraryEntry(new Path(s), null, null));
        }

        //Sort classpath entries
        IClasspathEntry[] entries = classpathEntries.toArray(new IClasspathEntry[classpathEntries.size()]);
        Arrays.sort(entries, new WebSphereRuntimeClasspathProvider.RuntimeClasspathEntriesComparator());
        //Filter out duplicates..  Only latest libs should remain
        entries = WebSphereRuntimeClasspathProvider.filterDuplicateFeatures(entries);
        String[] result = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            result[i] = entries[i].getPath().toString();
        }

        /*
         * Uncomment for debug
         *
         * System.out.println("Result: \n");
         * for (String r : result) {
         * System.out.println("\t" + r + "\n");
         * }
         */

        Assert.assertArrayEquals(expectedResult, result);

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
    public void testStop3() throws Exception {
        stopServer();
        assertTrue("Server state should be stopped but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STOPPED);
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
    public void testPublish2() throws Exception {
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
    public void testStart2() throws Exception {
        startServer();
        assertTrue("Server state should be started but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STARTED);
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
    public void testExternallyStartedServer() throws Exception {
        externallyStartServer("start");
        assertTrue("Server state should be started but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STARTED);
    }

    @Test
    public void testStop5() throws Exception {
        stopServer();
        assertTrue("Server state should be stopped but is: " + server.getServerState(), server.getServerState() == IServer.STATE_STOPPED);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: " + getClass().getName() + "\n");
    }
}