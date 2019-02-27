/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import org.eclipse.core.resources.IFile;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check Validation", isStable = true)
@RunWith(AllTests.class)
public class VariableDefaultValueTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(VariableDefaultValueTest.getOrderedTests());
        suite.setName(VariableDefaultValueTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "variableDefaultValueMsgs"));
        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "variableDefaultValue1"));
        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "variableDefaultValue2"));
        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "variableDefaultValue3"));
        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "variableDefaultValue4"));
        testSuite.addTest(TestSuite.createTest(VariableDefaultValueTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void variableDefaultValueMsgs() throws Exception {
        String serverName = "variableDefaultValueMsgs";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.invalidVariableDecl, new String[] { "port1" }), serverName + "/" + file.getName(), 19);
        checkMessage(messages[1], NLS.bind(Messages.variableDeclNoValue, new String[] { "port2" }), serverName + "/" + file.getName(), 20);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void variableDefaultValue1() throws Exception {
        // Test default value in main config and include
        String serverName = "variableDefaultValue1";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8001 but the value was " + httpPort, httpPort == 8001);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9001 but the value was " + httpsPort, httpsPort == 9001);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void variableDefaultValue2() throws Exception {
        // Test default value in main config overridden by value in main config
        String serverName = "variableDefaultValue2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8002 but the value was " + httpPort, httpPort == 8002);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9002 but the value was " + httpsPort, httpsPort == 9002);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void variableDefaultValue3() throws Exception {
        // Test default value main config overridden by value in include or bootstrap.properties
        String serverName = "variableDefaultValue3";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8002 but the value was " + httpPort, httpPort == 8002);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9002 but the value was " + httpsPort, httpsPort == 9002);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void variableDefaultValue4() throws Exception {
        // Test default value in main config and include with onConflict set to ignore has no effect
        String serverName = "variableDefaultValue4";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8001 but the value was " + httpPort, httpPort == 8001);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9001 but the value was " + httpsPort, httpsPort == 9001);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}