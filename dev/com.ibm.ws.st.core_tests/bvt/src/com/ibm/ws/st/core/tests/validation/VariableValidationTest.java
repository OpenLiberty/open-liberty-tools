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

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.config.ConfigVars;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check Validation", isStable = true)
@RunWith(AllTests.class)
public class VariableValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(VariableValidationTest.getOrderedTests());
        suite.setName(VariableValidationTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varValueUndeclaredRef"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varNameContainsRef"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varRefTypeMismatch"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varRefTypeMismatch2"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varDefect96040"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varDefect94757"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varDefect94757_2"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "implicitVarValue"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varRefInvalidValue"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "testServerEnv"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "varList"));
        testSuite.addTest(TestSuite.createTest(VariableValidationTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    @Test
    public void varValueUndeclaredRef() throws Exception {
        // Test that an error occurs if a variable value contains
        // a reference to an undeclared variable

        String serverName = "varValueUndeclaredRef";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unresolvedPropertyValue, new String[] { "value", "variable", "undeclaredVar" }),
                     serverName + "/" + file.getName(), 18);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void varNameContainsRef() throws Exception {
        // Test that a variable name that contains a variable reference
        // causes an error

        String serverName = "varNameContainsRef";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.variableNameContainsRefs, new String[] { "${varName}" }),
                     serverName + "/" + file.getName(), 19);
        checkMessage(messages[1], NLS.bind(Messages.unresolvedPropertyValue, new String[] { "httpPort", "httpEndpoint", "myPort" }),
                     serverName + "/" + file.getName(), 23);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void varRefTypeMismatch() throws Exception {
        // Test that an error occurs if a variable is referenced
        // and its type does not match the expected type

        String serverName = "varRefTypeMismatch";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${invalidVar}", "dropinsEnabled", "applicationMonitor", "boolean" }),
                     serverName + "/" + file.getName(), 25);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void varRefTypeMismatch2() throws Exception {
        // Test that an error occurs if a variable is referenced
        // and its type does not match the expected type (variable
        // defined in include)

        String serverName = "varRefTypeMismatch2";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${invalidVar}", "dropinsEnabled", "applicationMonitor", "boolean" }),
                     serverName + "/" + file.getName(), 25);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void varDefect96040() throws Exception {
        // Test that bootstrap variables that depend on predefined
        // variables are resolved properly.  If both include files are
        // located as expected then there will be an info message about
        // a overriding the value of a variable.

        String serverName = "varDefect96040";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.infoOverrideItem, new String[] { "value", "variable", "1", "2" }),
                     serverName + "/" + file.getName(), 24);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void varDefect94757() throws Exception {
        // Test that ConfigVars.isDurationType() is working correctly.
        assertFalse("Invalid duration type match: d9", ConfigVars.isDurationType("d9"));
        assertFalse("Invalid duration type match: 5d12", ConfigVars.isDurationType("5d12"));
        assertFalse("Invalid duration type match: 1d2h3m4s5mss", ConfigVars.isDurationType("1d2h3m4s5mss"));
        assertFalse("Invalid duration type match: 5ms4s3m2h1f", ConfigVars.isDurationType("5ms4s3m2h1f"));
        assertFalse("Invalid duration type match: 1d2h3mm4s5ms", ConfigVars.isDurationType("1d2h3mm4s5ms"));
        assertTrue("Value should have matched duration type: 12d", ConfigVars.isDurationType("12d"));
        assertTrue("Value should have matched duration type: 15h", ConfigVars.isDurationType("15h"));
        assertTrue("Value should have matched duration type: 22m", ConfigVars.isDurationType("22m"));
        assertTrue("Value should have matched duration type: 42s", ConfigVars.isDurationType("42s"));
        assertTrue("Value should have matched duration type: 500ms", ConfigVars.isDurationType("500ms"));
        assertTrue("Value should have matched duration type: 1d13h15m30s500ms", ConfigVars.isDurationType("1d13h15m30s500ms"));
    }

    @Test
    public void varDefect94757_2() throws Exception {
        // Test that valid durations give no message but an invalid duration
        // does give a message.

        String serverName = "varDefect94757";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.invalidValue, new String[] { "50mx", "agedTimeout", "connectionManager", "duration" }),
                     serverName + "/" + file.getName(), 25);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void implicitVarValue() throws Exception {
        // Test implicit variables for both explicit attributes
        // and default attributes

        String serverName = "implicitVarValue";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${updateTrigger}", "dropinsEnabled", "applicationMonitor", "boolean" }),
                     serverName + "/" + file.getName(), 24);
        checkMessage(messages[1], NLS.bind(Messages.unresolvedPropertyValue, new String[] { "name", "library", "noexist" }),
                     serverName + "/" + file.getName(), 36);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void varRefInvalidValue() throws Exception {
        // Test that an error occurs if a variable is referenced
        // and its value is not valid for the anonymous union type.
        // Anonymous unions are only in runtimes later than 8.5.x.
        if (getRuntimeVersion().startsWith("8.5"))
            return;
        String serverName = "varRefInvalidValue";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.invalidValueNoType, new String[] { "123456789", "httpsPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 21);
        checkMessage(messages[1], NLS.bind(Messages.invalidValueNoType, new String[] { "invalid", "updateTrigger", "applicationMonitor" }),
                     serverName + "/" + file.getName(), 23);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void testServerEnv() throws Exception {
        File etcDir = null;
        try {
            // Copy the etc server.env file over to the <runtime>/etc directory
            IPath serverEnvFiles = resourceFolder.append("validation/serverEnv/files");
            IPath etcServerEnv = serverEnvFiles.append("etc/server.env");
            IPath runtimeEtcPath = (new Path(runtimeLocation)).append("etc");
            etcDir = runtimeEtcPath.toFile();
            assertTrue("Creating runtime etc directory: " + runtimeEtcPath.toOSString(), etcDir.mkdirs());
            FileUtil.copyFile(etcServerEnv.toOSString(), runtimeEtcPath.append("server.env").toOSString());

            // Copy the shared server.env file over to the <user dir>/shared directory
            IPath sharedServerEnv = serverEnvFiles.append("shared/server.env");
            IPath sharedPath = project.getLocation().append("shared");
            FileUtil.copyFile(sharedServerEnv.toOSString(), sharedPath.append("server.env").toOSString());

            // Validate the server.xml file - there should be two messages for invalid type of
            // variable reference
            String serverName = "serverEnv";
            setupRuntimeServer(RESOURCE_PATH, serverName);
            IFile file = getServerFile(serverName, "server.xml");
            ValidatorMessage[] messages = TestUtil.validate(file);
            checkMessageCount(messages, 3);
            checkMessage(messages[0], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${env.PORT}", "httpPort", "httpEndpoint", "int" }),
                         serverName + "/" + file.getName(), 20);
            checkMessage(messages[1], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${env.SECURE_PORT}", "httpsPort", "httpEndpoint", "int" }),
                         serverName + "/" + file.getName(), 21);
            checkMessage(messages[2], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${env.AUTO_EXPAND}", "autoExpand", "applicationManager", "boolean" }),
                         serverName + "/" + file.getName(), 24);

            // Copy the config server.env file over to the server config directory
            IPath configServerEnv = serverEnvFiles.append("config/server.env");
            IContainer serverFolder = file.getParent();
            IPath serverPath = serverFolder.getLocation();
            FileUtil.copyFile(configServerEnv.toOSString(), serverPath.append("server.env").toOSString());

            // Validate the server.xml file - the config server.env overrides two of the variables
            // with a correct value so there should only be one message now
            messages = TestUtil.validate(file);
            checkMessageCount(messages, 1);
            checkMessage(messages[0], NLS.bind(Messages.incorrectVariableReferenceType, new String[] { "${env.SECURE_PORT}", "httpsPort", "httpEndpoint", "int" }),
                         serverName + "/" + file.getName(), 21);
            deleteRuntimeServer(serverName);
        } finally {
            // Clean up the runtime etc dir
            if (etcDir != null && etcDir.exists()) {
                FileUtil.deleteDirectory(etcDir.getAbsolutePath(), true);
            }
        }
    }

    @Test
    public void varList() throws Exception {
        // Test that the ${list(varname)} syntax is handled properly
        String serverName = "varList";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unresolvedPropertyValue, new String[] { "filesetRef", "library", "filesetsBC" }),
                     serverName + "/" + file.getName(), 27);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}