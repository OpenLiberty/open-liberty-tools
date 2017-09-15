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

import org.eclipse.core.resources.IFile;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.tests.util.TestUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeatureValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    @Test
    public void test1_doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void test2_duplicateFeature() throws Exception {
        // Test that there is an informational message if a feature
        // appears twice in the top level file.
        String serverName = "duplicateFeature";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoDuplicateItem, new String[] { "feature", "jsp-2.3" }),
                     serverName + "/" + file.getName(), 23);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test3_duplicateFeatureInclude() throws Exception {
        // Test that there is no message if a feature
        // appears in both the top level and an include.
        String serverName = "duplicateFeatureInclude";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test4_duplicateFeatureMultInclude() throws Exception {
        // Test that there is no message if a feature
        // appears in two includes.
        String serverName = "duplicateFeatureMultInclude";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test5_duplicateIncludeFeature() throws Exception {
        // Test that there is no message at the top level if
        // an include has the same feature twice but there is
        // a message for the include.
        String serverName = "duplicateIncludeFeature";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        file = getServerFile(serverName, "include.xml");
        messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoDuplicateItem, new String[] { "feature", "appSecurity-2.0" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test6_unrecognizedFeature() throws Exception {
        // Test that there is an warning message if a feature
        // is unrecognized.
        String serverName = "unrecognizedFeature";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedFeature, "appInsecurity-2.0"),
                     serverName + "/" + file.getName(), 16);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void test99_doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}