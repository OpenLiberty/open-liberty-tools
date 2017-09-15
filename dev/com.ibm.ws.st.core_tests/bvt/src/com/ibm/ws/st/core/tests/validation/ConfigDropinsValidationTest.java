/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

@TestCaseDescriptor(description = "Check validation for config dropins", isStable = true)
@RunWith(AllTests.class)
public class ConfigDropinsValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ConfigDropinsValidationProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ConfigDropinsValidationTest.getOrderedTests());
        suite.setName(ConfigDropinsValidationTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(ConfigDropinsValidationTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ConfigDropinsValidationTest.class, "testConfigDropins"));
        testSuite.addTest(TestSuite.createTest(ConfigDropinsValidationTest.class, "testConfigDropins2"));
        testSuite.addTest(TestSuite.createTest(ConfigDropinsValidationTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ConfigDropinsValidationTest");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void testConfigDropins() throws Exception {
        if (!runtimeSupportsFeature("servlet-3.1")) {
            print("Skipping ConfigDropinsValidationTest.testConfigDropins because runtime does not support servlet-3.1 feature.");
            return;
        }
        String serverName = "configDropins";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");

        // Test that port picked up correctly from override dropin
        ConfigurationFile cf = getConfigFile(serverName);
        int httpPort = cf.getHTTPPort();
        assertTrue("The http port should be 8099", httpPort == 8099);

        // Test various validation messages for interaction between dropins and server.xml
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        checkMessageCount(messages, 2);
        IFile file = getServerFile(serverName, "configDropins/defaults/Default2.xml");
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItemDropin, new String[] { "value", "variable", "8002", "8004", file.getFullPath().toString(), "unknown" }),
                     serverName + "/" + serverFile.getName(),
                     11, IMarker.SEVERITY_INFO);
        file = getServerFile(serverName, "configDropins/overrides/override1.xml");
        checkMessage(messages[1], NLS.bind(Messages.infoOverrideItemDropin, new String[] { "httpPort", "httpEndpoint", "8001", "8099", file.getFullPath().toString(), "unknown" }),
                     serverName + "/" + serverFile.getName(),
                     20, IMarker.SEVERITY_INFO);

        deleteRuntimeServer(serverName);
    }

    public void testConfigDropins2() throws Exception {
        String serverName = "configDropins2";
        setupRuntimeServer(RESOURCE_PATH, serverName);

        // Test that port picked up correctly from default dropin
        ConfigurationFile cf = getConfigFile(serverName);
        int httpPort = cf.getHTTPPort();
        assertTrue("The http port should be 8051", httpPort == 8051);

        // Test validation of dropin file
        IFile dropinFile = getServerFile(serverName, "configDropins/defaults/default1.xml");
        ValidatorMessage[] messages = TestUtil.validate(dropinFile);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.infoDuplicateItem, new String[] { "feature", "servlet-3.1" }),
                     serverName + "/configDropins/defaults/" + dropinFile.getName(), 14, IMarker.SEVERITY_INFO);
        checkMessage(messages[1], NLS.bind(Messages.infoOverrideItem, new String[] { "httpPort", "httpEndpoint", "8001", "8051" }),
                     serverName + "/configDropins/defaults/" + dropinFile.getName(), 17, IMarker.SEVERITY_INFO);

        // Test that problems in dropin don't cause messages for server.xml validation
        IFile serverFile = getServerFile(serverName, "server.xml");
        messages = TestUtil.validate(serverFile);
        checkMessageCount(messages, 0);

        deleteRuntimeServer(serverName);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ConfigDropinsValidationTest\n");
    }
}