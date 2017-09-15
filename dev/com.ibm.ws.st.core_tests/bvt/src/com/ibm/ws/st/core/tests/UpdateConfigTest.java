/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.util.ImportUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test that config file gets updated if import server.xml with older timestamp.", isStable = true)
@RunWith(AllTests.class)
public class UpdateConfigTest extends ToolsTestBase {

    private static final String DIR = "ServerTesting/updateConfig";
    private static final String SERVER_NAME = "baseServer";
    private static final String MODIFIED = "modified";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(UpdateConfigTest.getOrderedTests());
        suite.setName(UpdateConfigTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(UpdateConfigTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(UpdateConfigTest.class, "testInitialPort"));
        testSuite.addTest(TestSuite.createTest(UpdateConfigTest.class, "updateConfigFile"));
        testSuite.addTest(TestSuite.createTest(UpdateConfigTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClass().getName());
        init();
        createRuntime();
        createServer(runtime, SERVER_NAME, "resources/" + DIR + "/" + SERVER_NAME);
        createVM(JDK_NAME);
    }

    @Test
    public void testInitialPort() {
        ConfigurationFile configFile = wsServer.getConfiguration();
        int port = configFile.getHTTPPort();
        assertTrue("Port should be 9080 to start.", port == 9080);
    }

    @Test
    public void updateConfigFile() throws Exception {
        ConfigurationFile configFile = wsServer.getConfiguration();
        URI uri = configFile.getURI();
        File file = new File(uri);
        assertTrue("File for config should exist.", file.exists());

        long lastModified = file.lastModified();
        IPath modifiedPath = TestsPlugin.getInstallLocation().append("resources/" + DIR + "/" + MODIFIED + "/server.xml");
        File modifiedFile = modifiedPath.toFile();
        assertTrue("Modified server.xml should exist.", modifiedFile.exists());

        // Set the timestamp to earlier than the original server.xml
        modifiedFile.setLastModified(lastModified - 32000000);

        // Import the modified server.xml
        IFile destFile = configFile.getIFile();
        IPath destPath = destFile.getFullPath().removeLastSegments(1);
        ImportUtil.importFile(destPath, modifiedFile);

        configFile = wsServer.getConfiguration();
        int port = configFile.getHTTPPort();
        assertTrue("Port should be 9080 to start.", port == 8001);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: " + getClass().getName() + "\n");
    }

}
