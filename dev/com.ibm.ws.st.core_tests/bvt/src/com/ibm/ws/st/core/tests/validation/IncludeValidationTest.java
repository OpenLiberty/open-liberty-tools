/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
public class IncludeValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(IncludeValidationTest.getOrderedTests());
        suite.setName(IncludeValidationTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "testIncludeNotFound"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "testOptionalIncludeNotFound"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "testSubIncludeNotFound"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "testMultInclude"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "testIncludeMultInclude"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "testSubMultInclude"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "includeVariableResoultion1"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "includeVariableResoultion2"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "includeVariableResoultion3"));
        testSuite.addTest(TestSuite.createTest(IncludeValidationTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void testIncludeNotFound() throws Exception {
        // Test that error message given if an include is not found.

        String serverName = "includeNotFound";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");

        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.errorLoadingInclude, "nonexist.xml"), serverName + "/" + file.getName(), 12, IMarker.SEVERITY_ERROR);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void testOptionalIncludeNotFound() throws Exception {
        // Test that error message given if an include is not found.

        String serverName = "optionalIncludeNotFound";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");

        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.errorLoadingInclude, "nonexist.xml"), serverName + "/" + file.getName(), 12, IMarker.SEVERITY_INFO);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void testSubIncludeNotFound() throws Exception {
        // Test that no error is given if the main config includes a file
        // which has an unresolved include.  But the include file itself
        // should give an error.

        String serverName = "subIncludeNotFound";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        file = getServerFile(serverName, "a.xml");
        messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.errorLoadingInclude, "nonexist.xml"), serverName + "/" + file.getName(), 12);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void testMultInclude() throws Exception {
        // Test that an info message is given if the same file is included more
        // than once.

        String serverName = "multInclude";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        IFile include = getServerFile(serverName, "b.xml");
        checkMessage(messages[0], NLS.bind(Messages.infoMultipleInclude, include.getFullPath().toString()), serverName + "/" + file.getName(), 13);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void testIncludeMultInclude() throws Exception {
        // Test that an info message is given if the main config includes 2 files
        // that both include the same file.

        String serverName = "includeMultInclude";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        IFile include = getServerFile(serverName, "c.xml");
        checkMessage(messages[0], NLS.bind(Messages.infoMultipleInclude, include.getFullPath().toString()), serverName + "/" + file.getName(), 13);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void testSubMultInclude() throws Exception {
        // Test that no info message is given if the main config includes a file
        // that itself has multiple includes:
        //    server.xml -> a.xml -> b.xml -> c.xml
        //                  a.xml -> c.xml
        // The include file (a.xml) should give an info message.

        String serverName = "subMultInclude";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        file = getServerFile(serverName, "a.xml");
        messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        IFile include = getServerFile(serverName, "c.xml");
        checkMessage(messages[0], NLS.bind(Messages.infoMultipleInclude, include.getFullPath().toString()), serverName + "/" + file.getName(), 13);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeVariableResoultion1() throws Exception {
        // Test that an include is resolved properly if location
        // contains a variable reference, and the variable is defined
        // in the bootstrap properties

        String serverName = "includeVariableResolution";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        List<String> features = cf.getAllFeatures();
        assertTrue("Number of features is not 2", features.size() == 2);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeVariableResoultion2() throws Exception {
        // Test that an include is not resolved if location contains
        // a variable reference, and the variable is defined in the
        // configuration file

        String serverName = "includeVariableResolution2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.errorLoadingInclude, "${myLocation}"), serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeVariableResoultion3() throws Exception {
        // Test that an include is resolved properly if location
        // contains a variable reference, and the variable is defined
        // in the bootstrap properties (multi-level include)

        String serverName = "includeVariableResolution3";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        List<String> features = cf.getAllFeatures();
        assertTrue("Number of features is not 3", features.size() == 3);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}