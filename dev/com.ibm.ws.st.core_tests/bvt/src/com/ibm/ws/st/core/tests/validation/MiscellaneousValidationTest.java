/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.config.ConfigUtils;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check Validation", isStable = true)
@RunWith(AllTests.class)
public class MiscellaneousValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(MiscellaneousValidationTest.getOrderedTests());
        suite.setName(MiscellaneousValidationTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testPlainTextPassword"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testIncludePlainTextPassword"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testCustomEncryptedPassword"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testCustomEncryptedPassword2"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testFactoryIdNoError"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testEmptyRequiredAttr"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testReferenceSingleError"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "testMultipleUsersGroups"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "factoryIdMultipleReference"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "unavailableElement"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "serverXMLinEAR"));
        testSuite.addTest(TestSuite.createTest(MiscellaneousValidationTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void testPlainTextPassword() throws Exception {
        // Test that a warning is given when there is an unencoded password.

        String serverName = "plainTextPassword";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], Messages.warningPlainTextPassword, serverName + "/" + file.getName(), 18);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testCustomEncryptedPassword() throws Exception {
        // Tests when supported custom encryption is used no warning is generated.
        if (!WebSphereUtil.isGreaterOrEqualVersion("2016.0.0.2", getRuntimeVersion()))
            return;
        String serverName = "customEncryptedPassword";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        FileUtil.unZip(resourceFolder.append(RESOURCE_PATH).append(serverName).append("customEncryption.zip").toOSString(),
                       getWebSphereRuntime().getRuntimeLocation().toOSString());
        getWebSphereRuntime().refresh();
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
        FileUtil.deleteDirectory(getWebSphereRuntime().getRuntimeLocation().append("usr/extension").toString(), true);
        FileUtil.deleteDirectory(getWebSphereRuntime().getRuntimeLocation().append("bin/tools/extensions").toString(), true);
    }

    @Test
    public void testCustomEncryptedPassword2() throws Exception {
        // Tests when unsupported custom encryption is used validation warning is generated.

        String serverName = "customEncryptedPassword2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        FileUtil.unZip(resourceFolder.append(RESOURCE_PATH).append(serverName).append("customEncryption.zip").toOSString(),
                       getWebSphereRuntime().getRuntimeLocation().toOSString());
        getWebSphereRuntime().refresh();
        IFile file = getServerFile(serverName, "server.xml");
        ConfigurationFile cFile = getConfigFile(serverName);
        Element keyStore = ConfigUtils.getFirstChildElement(cFile.getDocument().getDocumentElement(), Constants.KEY_STORE);
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0],
                     NLS.bind(Messages.warningCustomEncryptedPasswordNotSupported, ConfigUtils.getEncryptionAlgorithm(keyStore.getAttribute(Constants.PASSWORD_ATTRIBUTE))),
                     serverName + "/" + file.getName(), 28);
        deleteRuntimeServer(serverName);
        FileUtil.deleteDirectory(getWebSphereRuntime().getRuntimeLocation().append("usr/extension").toString(), true);
        FileUtil.deleteDirectory(getWebSphereRuntime().getRuntimeLocation().append("bin/tools/extensions").toString(), true);
    }

    @Test
    public void testIncludePlainTextPassword() throws Exception {
        // Test that no warning is given for an unencoded password in
        // an include.  Validating the include file itself should give
        // a warning.

        String serverName = "includePlainTextPassword";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);

        file = getServerFile(serverName, "a.xml");
        messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], Messages.warningPlainTextPassword, serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testFactoryIdNoError() throws Exception {
        // Test that there are no errors if the same factory element is specified
        // but with a different id.

        String serverName = "factoryIdNoError";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testEmptyRequiredAttr() throws Exception {
        // Test that a warning is given if a required attribute has
        // an empty value.

        String serverName = "emptyRequiredAttr";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.emptyRequiredAttribute, new String[] { "location", "application" }),
                     serverName + "/" + file.getName(), 16);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testReferenceSingleError() throws Exception {
        // Test that a warning is given if a singleton reference is specified
        // and there is a nested element of the reference type.

        String serverName = "referenceSingleError";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.singlePidRefAndNested, new String[] { "dataSource", "jdbcDriverRef", "jdbcDriver" }),
                     serverName + "/" + file.getName(), 19);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testMultipleUsersGroups() throws Exception {
        // Test that there are no errors if the several groups/users are
        // specified with different names

        String serverName = "multipleUsersGroups";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void factoryIdMultipleReference() throws Exception {
        // For an attribute that allows references to different types,
        // test that there is a message if a reference attribute refers to
        // a factory id that does not exist but no message for ids that do exist.
        // Skip for 8.5.0 since this is new in the schema for 8.5.5.
        if (getRuntimeVersion().startsWith("8.5.0"))
            return;
        String serverName = "factoryIdMultipleReference";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        String splitString = "__split_here__";
        String message = NLS.bind(Messages.factoryIdNotFoundMulti, new String[] { splitString, "missingQueue" });
        checkMessage(messages[0], message, splitString, new String[] { "jmsTopic", "jmsQueue" }, serverName + "/" + file.getName(), 35);
        message = NLS.bind(Messages.factoryIdNotFoundMulti, new String[] { splitString, "missingTopic" });
        checkMessage(messages[1], message, splitString, new String[] { "jmsTopic", "jmsQueue" }, serverName + "/" + file.getName(), 47);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void unavailableElement() throws Exception {
        // Test that there is a warning message for elements that are not enabled
        // by a feature.  The warning should be for top level elements only.
        // Skip for 8.5.0 since the feature list did not include the elements.
        if (getRuntimeVersion().startsWith("8.5.0"))
            return;
        String serverName = "unavailableElement";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.unavailableElement, "keyStore"),
                     serverName + "/" + file.getName(), 17);
        checkMessage(messages[1], NLS.bind(Messages.unavailableElement, "basicRegistry"),
                     serverName + "/" + file.getName(), 19);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void serverXMLinEAR() throws Exception {
        IPath projectPath = new Path(RESOURCE_PATH).append("serverXMLinEAR");
        importProjects(projectPath, new String[] { "Web1", "Web1EAR" });
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("Web1EAR");
        IFile file = project.getFile("server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.infoDuplicateItem, new String[] { "feature", "ejbLite-3.2" }),
                     "Web1EAR" + "/" + file.getName(), 31);
        checkMessage(messages[1], NLS.bind(Messages.infoOverrideItem, new String[] { "value", "variable", "default", "override" }),
                     "Web1EAR" + "/" + file.getName(), 34);
        WLPCommonUtil.deleteAllProjects();
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}