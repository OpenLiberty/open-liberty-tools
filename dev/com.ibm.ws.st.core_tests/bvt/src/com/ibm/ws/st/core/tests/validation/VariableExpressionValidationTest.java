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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check Validation", isStable = true)
@RunWith(AllTests.class)
public class VariableExpressionValidationTest extends ValidationTestBase {
    protected static final String PROJECT_NAME = "ValidationTestProject";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(VariableExpressionValidationTest.getOrderedTests());
        suite.setName(VariableExpressionValidationTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidConstants"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidVariables"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidConstantAndVariable"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidVariableAndConstant"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidLocalAddition"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidLocalSubstraction"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidLocalMultiplication"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionValidLocalDivision"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionMissingLeftOperand"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionMissingRightOperand"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionMissingBothOperands"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionUndefinedLeftOperand"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionUndefinedRightOperand"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionUndefinedBothOperands"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionInvalidLeftOperand"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionInvalidRightOperand"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionInvalidBothOperands"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "variableExpressionMultipleOperators"));
        testSuite.addTest(TestSuite.createTest(VariableExpressionValidationTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: ValidationTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
    }

    // Variable expression - valid expression using constants: ${9080+1}
    public void variableExpressionValidConstants() throws Exception {
        String serverName = "variableExpressionValidConstants";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid expression using variables: ${myPort+someNumber}
    @Test
    public void variableExpressionValidVariables() throws Exception {
        String serverName = "variableExpressionValidVariables";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid expression using a variable and a constant: ${1+myport}
    @Test
    public void variableExpressionValidConstantAndVariable() throws Exception {
        String serverName = "variableExpressionValidConstantAndVariable";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid expression using a constant and a variable: ${myPort+1}
    @Test
    public void variableExpressionValidVariableAndConstant() throws Exception {
        String serverName = "variableExpressionValidVariableAndConstant";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid local variable (addition): ${httpPort+1}
    @Test
    public void variableExpressionValidLocalAddition() throws Exception {
        String serverName = "variableExpressionValidLocalAddition";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid local variable (subtraction): ${httpPort-1}
    @Test
    public void variableExpressionValidLocalSubstraction() throws Exception {
        String serverName = "variableExpressionValidLocalSubtraction";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid local variable (multiplication): ${httpPort*1}
    @Test
    public void variableExpressionValidLocalMultiplication() throws Exception {
        String serverName = "variableExpressionValidLocalMultiplication";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - valid local variable (division): ${httpPort/1}
    @Test
    public void variableExpressionValidLocalDivision() throws Exception {
        String serverName = "variableExpressionValidLocalDivision";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression missing left operand: ${+1}
    @Test
    public void variableExpressionMissingLeftOperand() throws Exception {
        String serverName = "variableExpressionMissingLeftOperand";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionMissingLeftOperand, new String[] { "${+1}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression missing right operand: ${1+}
    @Test
    public void variableExpressionMissingRightOperand() throws Exception {
        String serverName = "variableExpressionMissingRightOperand";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionMissingRightOperand, new String[] { "${1+}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression missing both operands: ${+}
    @Test
    public void variableExpressionMissingBothOperands() throws Exception {
        String serverName = "variableExpressionMissingBothOperands";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.expressionMissingLeftOperand, new String[] { "${+}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        checkMessage(messages[1], NLS.bind(Messages.expressionMissingRightOperand, new String[] { "${+}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression undefined left operand: ${x+1}
    @Test
    public void variableExpressionUndefinedLeftOperand() throws Exception {
        String serverName = "variableExpressionUndefinedLeftOperand";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionUndefinedLeftOperand, new String[] { "${x+1}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression undefined right operand: ${1+x}
    @Test
    public void variableExpressionUndefinedRightOperand() throws Exception {
        String serverName = "variableExpressionUndefinedRightOperand";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionUndefinedRightOperand, new String[] { "${1+x}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression undefined both operands: ${x+y}
    @Test
    public void variableExpressionUndefinedBothOperands() throws Exception {
        String serverName = "variableExpressionUndefinedBothOperands";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.expressionUndefinedLeftOperand, new String[] { "${x+y}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        checkMessage(messages[1], NLS.bind(Messages.expressionUndefinedRightOperand, new String[] { "${x+y}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression invalid left operand: ${x+1}
    @Test
    public void variableExpressionInvalidLeftOperand() throws Exception {
        String serverName = "variableExpressionInvalidLeftOperand";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionInvalidLeftOperand, new String[] { "${myPort+1}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression invalid right operand: ${1+x}
    @Test
    public void variableExpressionInvalidRightOperand() throws Exception {
        String serverName = "variableExpressionInvalidRightOperand";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionInvalidRightOperand, new String[] { "${1+myPort}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression invalid both operands: ${x+y}
    @Test
    public void variableExpressionInvalidBothOperands() throws Exception {
        String serverName = "variableExpressionInvalidBothOperands";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.expressionInvalidLeftOperand, new String[] { "${myPort+someNumber}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        checkMessage(messages[1], NLS.bind(Messages.expressionInvalidRightOperand, new String[] { "${myPort+someNumber}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    // Variable expression - invalid expression multiple operators: ${1+1+1}
    @Test
    public void variableExpressionMultipleOperators() throws Exception {
        String serverName = "variableExpressionMultipleOperators";
        setupRuntimeServer(RESOURCE_PATH, serverName);
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.invalidVariableExpression, new String[] { "${1+1+1}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        deleteRuntimeServer(serverName);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: ValidationTestCase\n");
    }
}