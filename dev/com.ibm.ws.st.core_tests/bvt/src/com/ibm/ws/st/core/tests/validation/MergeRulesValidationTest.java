/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
public class MergeRulesValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(MergeRulesValidationTest.getOrderedTests());
        suite.setName(MergeRulesValidationTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testRedeclareApp"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testIncludeRedeclareApp"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testOverrideItem"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testIncludeOverrideItem"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testOverrideNestedElement"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testOverrideNestedElement_2"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testRedeclareVariable"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "testIncludeRedeclareVariable"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "mergeRules1"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "mergeRules2"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "mergeRules3"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "mergeRules4"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "mergeRules5"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictMerge"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictIgnore"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictIgnore2"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictIgnore3"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictIgnore4"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictReplace"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "includeConflictNested"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "onConflictIgnoreReplace"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "onConflictMergeReplace"));
        testSuite.addTest(TestSuite.createTest(MergeRulesValidationTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void testRedeclareApp() throws Exception {
        // Test that a warning is given if an application is redeclared.

        String serverName = "redeclareApp";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "name", "application", "noisyservlet", "noisyservlet2" }), serverName + "/"
                                                                                                                                                + file.getName(),
                     17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testIncludeRedeclareApp() throws Exception {
        // Test that a warning is given if an application is redeclared
        // where one of the declarations is in an include.

        String serverName = "includeRedeclareApp";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "name", "application", "noisyservlet", "myservlet" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testOverrideItem() throws Exception {
        // Test that an info message is given if an item is overridden.

        String serverName = "overrideItem";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "traceFileName", "logging", "foo.out", "bar.out" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testIncludeOverrideItem() throws Exception {
        // Test that an info message is given if an item is overridden
        // where the one of the specifications is in an include.

        String serverName = "includeOverrideItem";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "traceFileName", "logging", "foo.out", "bar.out" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testOverrideNestedElement() throws Exception {
        // Test that an info message is given if a password is overridden.

        String serverName = "overrideNestedElement";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "password", "user", "{xor}Lz4sLA==", "{xor}Lz4sLCgwLTs=" }),
                     serverName + "/" + file.getName(), 20);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testOverrideNestedElement_2() throws Exception {
        // Test that an info message is given if a password and name is overridden.

        String serverName = "overrideNestedElement_2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "password", "user", "{xor}Lz4sLA==", "{xor}Lz4sLCgwLTs=" }),
                     serverName + "/" + file.getName(), 20);
        checkMessage(messages[1], NLS.bind(Messages.infoOverrideItem, new String[] { "name", "user", "user", "user1" }),
                     serverName + "/" + file.getName(), 20);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testRedeclareVariable() throws Exception {
        // Test that an informational message is given if a variable
        // is redeclared with a different value.

        String serverName = "redeclareVariable";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "value", "variable", "1", "2" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testIncludeRedeclareVariable() throws Exception {
        // Test that an information message is given if a variable is redeclared
        // with a different value where one of the declarations is in an include.

        String serverName = "includeRedeclareVariable";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "value", "variable", "def", "abc" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    public void mergeRules1() throws Exception {
        // Singleton elements are always merged
        String serverName = "mergeRules1";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoDuplicateItem, new String[] { "feature", "jsp-2.3" }),
                     serverName + "/" + file.getName(), 21);
        deleteRuntimeServer(serverName);
    }

    public void mergeRules2() throws Exception {
        // Factory elements at the top level with the same id are merged
        String serverName = "mergeRules2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "jndiName", "dataSource", "jdbc/myDriver", "jdbc/myDriver1" }),
                     serverName + "/" + file.getName(), 28);
        deleteRuntimeServer(serverName);
    }

    public void mergeRules3() throws Exception {
        // Factory elements at the top level with no id are distinct
        String serverName = "mergeRules3";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    public void mergeRules4() throws Exception {
        // Nested factory elements are merged if parent ids and nested ids match
        String serverName = "mergeRules4";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "databaseName", "properties.derby.embedded", "DB", "DB1" }),
                     serverName + "/" + file.getName(), 35);
        deleteRuntimeServer(serverName);
    }

    public void mergeRules5() throws Exception {
        // No override messages for singleton since schema validator will already
        // complain about extra child
        String serverName = "mergeRules5";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        //check message count verifies message count ,verify message type to make sure it is schema validation error.
        assertTrue("Message type should be xml validation Marker", messages[0].getType().equalsIgnoreCase(XML_MARKER));
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictMerge() throws Exception {
        String serverName = "includeConflictMerge";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "databaseName", "properties.derby.embedded", "DB", "DB2" }),
                     serverName + "/" + file.getName(), 30);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictIgnore() throws Exception {
        String serverName = "includeConflictIgnore";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 9080 but the value was " + httpPort, httpPort == 9080);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictIgnore2() throws Exception {
        String serverName = "includeConflictIgnore2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8001 but the value was " + httpPort, httpPort == 8001);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictIgnore3() throws Exception {
        String serverName = "includeConflictIgnore3";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8001 but the value was " + httpPort, httpPort == 8001);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictIgnore4() throws Exception {
        String serverName = "includeConflictIgnore4";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8002 but the value was " + httpPort, httpPort == 8002);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictReplace() throws Exception {
        String serverName = "includeConflictReplace";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        IFile includeFile = getServerFile(serverName, "a.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0],
                     NLS.bind(Messages.infoReplaceItem, new String[] { "variable[port1]", includeFile.getFullPath().toPortableString(), "variable[port1]",
                                                                       file.getFullPath().toPortableString() }),
                     serverName + "/" + file.getName(), 27);
        checkMessage(messages[1], NLS.bind(Messages.infoOverrideItem, new String[] { "value", "variable", "9445", "9444" }),
                     serverName + "/" + file.getName(), 28);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 9081 but the value was " + httpPort, httpPort == 9081);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9444 but the value was " + httpsPort, httpsPort == 9444);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void includeConflictNested() throws Exception {
        String serverName = "includeConflictNested";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        IFile includeFile1 = getServerFile(serverName, "a.xml");
        IFile includeFile2 = getServerFile(serverName, "b.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0],
                     NLS.bind(Messages.infoReplaceItem, new String[] { "variable[port2]", includeFile2.getFullPath().toPortableString(), "variable[port2]",
                                                                       includeFile1.getFullPath().toPortableString() }),
                     serverName + "/" + file.getName(), 27);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 9081 but the value was " + httpPort, httpPort == 9081);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9444 but the value was " + httpsPort, httpsPort == 9444);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void onConflictIgnoreReplace() throws Exception {
        // Test that include2 replaces elements in include 1 but
        // they are ignored in the main server.xml
        String serverName = "onConflictIgnoreReplace";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        IFile include1File = getServerFile(serverName, "include1.xml");
        IFile include2File = getServerFile(serverName, "include2.xml");
        checkMessage(messages[0],
                     NLS.bind(Messages.infoReplaceItem, new String[] { "featureManager", include2File.getFullPath().toPortableString(), "featureManager",
                                                                       include1File.getFullPath().toPortableString() }),
                     serverName + "/" + file.getName(), 21);
        checkMessage(messages[1],
                     NLS.bind(Messages.infoReplaceItem, new String[] { "httpEndpoint[defaultHttpEndpoint]", include2File.getFullPath().toPortableString(),
                                                                       "httpEndpoint[defaultHttpEndpoint]", include1File.getFullPath().toPortableString() }),
                     serverName + "/" + file.getName(), 21);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8001 but the value was " + httpPort, httpPort == 8001);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 8002 but the value was " + httpsPort, httpsPort == 8002);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void onConflictMergeReplace() throws Exception {
        // Test that include2 replaces elements in include 1 and the
        // result is merged in the main server.xml
        String serverName = "onConflictMergeReplace";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 3);
        IFile include1File = getServerFile(serverName, "include1.xml");
        IFile include2File = getServerFile(serverName, "include2.xml");
        checkMessage(messages[0],
                     NLS.bind(Messages.infoReplaceItem, new String[] { "featureManager", include2File.getFullPath().toPortableString(), "featureManager",
                                                                       include1File.getFullPath().toPortableString() }),
                     serverName + "/" + file.getName(), 21);
        checkMessage(messages[1],
                     NLS.bind(Messages.infoReplaceItem, new String[] { "httpEndpoint[defaultHttpEndpoint]", include2File.getFullPath().toPortableString(),
                                                                       "httpEndpoint[defaultHttpEndpoint]", include1File.getFullPath().toPortableString() }),
                     serverName + "/" + file.getName(), 21);
        checkMessage(messages[2], NLS.bind(Messages.infoOverrideItem, new String[] { "httpsPort", "httpEndpoint", "8002", "9443" }), serverName + "/"
                                                                                                                                     + file.getName(),
                     21);

        ConfigurationFile cf = getConfigFile(serverName);
        assertNotNull("Configuration file is null.", cf);
        int httpPort = cf.getHTTPPort();
        assertTrue("Expecting the value of httpPort to be 8001 but the value was " + httpPort, httpPort == 8001);
        int httpsPort = cf.getHTTPSPort();
        assertTrue("Expecting the value of httpsPort to be 9443 but the value was " + httpsPort, httpsPort == 9443);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}