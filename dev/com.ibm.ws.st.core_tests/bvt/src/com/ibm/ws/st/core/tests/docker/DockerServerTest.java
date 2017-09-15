/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.docker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.wst.server.core.IServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.service.setup.LibertySetup;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 *
 */
@TestCaseDescriptor(description = "Test liberty server on docker container - general testing", isStable = true)
@RunWith(AllTests.class)
public class DockerServerTest extends ToolsTestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(DockerServerTest.getOrderedTests());
        suite.setName(DockerServerTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(DockerServerTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(DockerServerTest.class, "testCreateDockerServer"));
        testSuite.addTest(TestSuite.createTest(DockerServerTest.class, "testVerifyRestConnector"));
        testSuite.addTest(TestSuite.createTest(DockerServerTest.class, "testStopDockerServer"));
        testSuite.addTest(TestSuite.createTest(DockerServerTest.class, "testStartDockerServer"));
        testSuite.addTest(TestSuite.createTest(DockerServerTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Staring test: DockerServerTest");
        init();
        createRuntime(RUNTIME_NAME);
        createVM(JDK_NAME);
    }

    @Test
    public void testCreateDockerServer() throws Exception {
        createServer(runtime, null, null);
    }

    @Test
    public void testVerifyRestConnector() throws Exception {
        // get the configured restConnector features
        List<String> features = wsServer.getConfiguration().getAllFeatures();
        Set<String> restConnectorFeatures = new HashSet<String>(2);
        for (String f : features) {
            if (f.toLowerCase().startsWith("restconnector-")) {
                restConnectorFeatures.add(f);
            }
        }

        // get the featurelist from the runtime in the docker container
        String containerName = DockerTestUtil.getDockerContainerName();
        BaseDockerContainer container = DockerTestUtil.getExistingContainer(DockerTestUtil.getDockerMachine(), containerName);

        // assert that only one restConnector feature is configured
        assertTrue("Only one restConnector feature should be configured", restConnectorFeatures.size() == 1);

        // assert that the latest version is configured
        String expectedFeature = LibertySetup.resolveFeature(container, wsServer.getServiceInfo(), "restConnector");
        assertNotNull("The restConnector feature is not installed in the runtime", expectedFeature);
        String actualFeature = restConnectorFeatures.iterator().next();
        assertTrue("The latest restConnector feature should be configured. Expected: " + expectedFeature + " Actual: " + actualFeature, actualFeature.equals(expectedFeature));
    }

    @Test
    public void testStopDockerServer() throws Exception {
        stopServer();
        assertTrue(getStateMsg(IServer.STATE_STOPPED, server.getServerState()), server.getServerState() == IServer.STATE_STOPPED);
    }

    @Test
    public void testStartDockerServer() throws Exception {
        startServer();
        assertTrue(getStateMsg(IServer.STATE_STARTED, server.getServerState()), server.getServerState() == IServer.STATE_STARTED);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: DockerServerTest\n");
    }

}