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
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Check validation for ssl defined but no keystore", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SSLNoKeystoreValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "SSLNoKeystoreProject";
    protected static final String RESOURCE_PATH = "/validation/";

    @Test
    public void test1_doSetup() throws Exception {
        print("Starting test: SSLNoKeystoreValidationTest");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void test2_testSSLNoKeystore1() throws Exception {
        // Test validation message for ssl with no keystore
        if (!runtimeSupportsFeature("servlet-3.1")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore1 because runtime does not support servlet-3.1 feature.");
            return;
        }

        String serverName = "sslNoKeystore1";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        String expectedText = Messages.missingKeystore;
        ValidatorMessage message = findMessage(messages, expectedText);
        assertTrue("The following validation message should be generated: " + expectedText, message != null);
        checkMessage(message, expectedText,
                     serverName + "/" + serverFile.getName(),
                     12, IMarker.SEVERITY_WARNING);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test3_testSSLNoKeystore2() throws Exception {
        // Test validation message for ssl + appSecurity with no keystore
        if (!runtimeSupportsFeature("servlet-3.1")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore2 because runtime does not support servlet-3.1 feature.");
            return;
        }

        String serverName = "sslNoKeystore2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        String expectedText = Messages.missingKeystoreAndUR;
        ValidatorMessage message = findMessage(messages, expectedText);
        assertTrue("The following validation message should be generated: " + expectedText, message != null);
        checkMessage(message, expectedText,
                     serverName + "/" + serverFile.getName(),
                     12, IMarker.SEVERITY_WARNING);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test4_testSSLNoKeystore3() throws Exception {
        // Test validation message for ssl + appSecurity + ejbRemote with no keystore
        if (!runtimeSupportsFeature("ejbRemote-3.2")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore3 because runtime does not support ejbRemote-3.2 feature.");
            return;
        }

        String serverName = "sslNoKeystore3";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        String expectedText = Messages.missingKeystoreAndUR;
        ValidatorMessage message = findMessage(messages, expectedText);
        assertTrue("The following validation message should be generated: " + expectedText, message != null);
        checkMessage(message, expectedText,
                     serverName + "/" + serverFile.getName(),
                     12, IMarker.SEVERITY_ERROR);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test5_testSSLNoKeystore4() throws Exception {
        // Test validation message for javaee-7.0 with no keystore
        if (!runtimeSupportsFeature("javaee-7.0")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore4 because runtime does not support javaee-7.0 feature.");
            return;
        }

        String serverName = "sslNoKeystore4";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], Messages.missingKeystoreAndUR,
                     serverName + "/" + serverFile.getName(),
                     12, IMarker.SEVERITY_ERROR);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test6_testSSLNoKeystore5() throws Exception {
        // Test no validation message for javaee-7.0 with default ssl pointing to different keystore
        if (!runtimeSupportsFeature("javaee-7.0")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore5 because runtime does not support javaee-7.0 feature.");
            return;
        }

        String serverName = "sslNoKeystore5";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test7_testSSLNoKeystore6() throws Exception {
        // Test no validation message for javaee-7.0 with default ssl overridden
        if (!runtimeSupportsFeature("javaee-7.0")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore6 because runtime does not support javaee-7.0 feature.");
            return;
        }

        String serverName = "sslNoKeystore6";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test8_testSSLNoKeystore7() throws Exception {
        // Test no validation message for javaee-7.0 with default ssl and
        // keystore has no id
        if (!runtimeSupportsFeature("javaee-7.0")) {
            print("Skipping SSLNoKeystoreValidationTest.testSSLNoKeystore7 because runtime does not support javaee-7.0 feature.");
            return;
        }

        String serverName = "sslNoKeystore7";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile serverFile = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(serverFile);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test99_doTearDown() {
        cleanUp();
        print("Ending test: SSLNoKeystoreValidationTest\n");
    }
}