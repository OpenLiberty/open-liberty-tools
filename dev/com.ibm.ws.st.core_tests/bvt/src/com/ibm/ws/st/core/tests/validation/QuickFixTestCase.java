/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.config.validation.ConfigurationQuickFix;
import com.ibm.ws.st.core.internal.config.validation.ConfigurationQuickFix.ResolutionType;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Quick fix tests", isStable = true)
@RunWith(AllTests.class)
public class QuickFixTestCase extends ValidationTestBase {
    protected static final String PROJECT_NAME = "QuickFixTestProject";
    protected static final String MARKER_TYPE = "com.ibm.ws.st.core.configmarker";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(QuickFixTestCase.getOrderedTests());
        suite.setName(QuickFixTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testInvalidFeature"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testPlainTextPassword"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testSupersedeFeature"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedAttributeElement"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedAllAttributeElement"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedAllAttributeAllElement"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedAttributeBestMatch"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedVariabledBestMatch"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedVariabledBestMatch2"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "testUnrecognizedAttributeExtraProperties"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "variableExpressionUndefinedLeftOperand"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "variableExpressionUndefinedRightOperand"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "variableExpressionUndefinedBothOperands"));
        testSuite.addTest(TestSuite.createTest(QuickFixTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: QuickFixTestCase");
        init();
        setupRuntime(PROJECT_NAME, "/quickfix/");
    }

    @Test
    public void testInvalidFeature() throws Exception {
        // Test that an invalid feature is detected and a quick fix provided.

        String serverName = "invalidFeatureQuickFix";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedFeature, "jndi-1.1"), serverName + "/" + file.getName(), 14);

        runQuickFix(messages[0]);

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    @Test
    public void testPlainTextPassword() throws Exception {
        // Test that a plain text password is detected and a quick fix provided.

        String serverName = "quickFixPlainTextPassword";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], Messages.warningPlainTextPassword, serverName + "/" + file.getName(), 18);

        runQuickFix(messages[0]);

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    @Test
    public void testSupersedeFeature() throws Exception {
        // Test that a superseded feature is detected and a quick fix is provided
        String serverName = "supersedeFeature";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.supersededFeature, "appSecurity-1.0"), serverName + "/" + file.getName(), 13);

        checkQuickFix(messages[0]);
    }

    /**
     * Test that unrecognized attributes are detected and a quick fix to
     * ignore a specific attribute on named elements is provided.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedAttributeElement() throws Exception {
        String serverName = "unrecAttrQuickFix1";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedProperty, new String[] { "newAttr", "featureManager" }), serverName + "/" + file.getName(), 12);
        checkMessage(messages[1], NLS.bind(Messages.unrecognizedProperty, new String[] { "newAttr2", "featureManager" }), serverName + "/" + file.getName(), 12);
        runQuickFix(messages[0], ResolutionType.IGNORE_ATTR_ELEM, new String[] { "newAttr", "featureManager" });

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedProperty, new String[] { "newAttr2", "featureManager" }), serverName + "/" + file.getName(), 12);
    }

    /**
     * Test that all unrecognized attributes are detected and a quick fix to
     * ignore all unrecognized attributes on named elements is provided.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedAllAttributeElement() throws Exception {
        String serverName = "unrecAttrQuickFix2";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedProperty, new String[] { "newAttr", "featureManager" }), serverName + "/" + file.getName(), 12);
        checkMessage(messages[1], NLS.bind(Messages.unrecognizedProperty, new String[] { "newAttr2", "featureManager" }), serverName + "/" + file.getName(), 16);
        runQuickFix(messages[0], ResolutionType.IGNORE_ALL_ATTR_ELEM, new String[] { "featureManager" });

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    /**
     * Test that all unrecognized attributes are detected and a quick fix to
     * ignore all unrecognized attributes on named elements is provided.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedAllAttributeAllElement() throws Exception {
        String serverName = "unrecAttrQuickFix3";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedProperty, new String[] { "newAttr", "featureManager" }), serverName + "/" + file.getName(), 12);
        checkMessage(messages[1], NLS.bind(Messages.unrecognizedProperty, new String[] { "myAttr", "dataSource" }), serverName + "/" + file.getName(), 17);
        runQuickFix(messages[0], ResolutionType.IGNORE_ALL_ATTR_ALL_ELEM, null);

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    /**
     * Test that the unrecognized attribute is detected and a quick fix to
     * find best match is provided.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedAttributeBestMatch() throws Exception {
        String serverName = "unrecAttrQuickFix4";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedProperty, new String[] { "httport", "httpEndpoint" }), serverName + "/" + file.getName(), 16);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_ATTR, new String[] { "httpPort" });

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    /**
     * Test that the unrecognized variable reference is detected and a quick
     * fix to find best match is provided.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedVariabledBestMatch() throws Exception {
        String serverName = "unrecVarQuickFix1";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unresolvedPropertyValue, new String[] { "httpPort", "httpEndpoint", "myProt" }),
                     serverName + "/" + file.getName(), 16);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_VARIABLE, new String[] { "myProt", "myPort" });

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    /**
     * Test that the unrecognized variable reference is detected and a quick
     * fix to find best match is provided (covers include)
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedVariabledBestMatch2() throws Exception {
        String serverName = "unrecVarQuickFix2";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unresolvedPropertyValue, new String[] { "httpsPort", "httpEndpoint", "myPrt" }),
                     serverName + "/" + file.getName(), 16);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_VARIABLE, new String[] { "myPrt", "mysPort" });

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    /**
     * Test that if extra properties are defined on the element, no message
     * is given for an attribute that does not have a best match. For an
     * attribute that does have a best match, an informational message with
     * a quick fix should be given.
     *
     * @throws Exception
     */
    @Test
    public void testUnrecognizedAttributeExtraProperties() throws Exception {
        String serverName = "unrecAttrExtraProperties";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.unrecognizedProperty, new String[] { "loggerTimeout", "properties.derby.embedded" }), serverName + "/" + file.getName(), 23);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_ATTR, new String[] { "loginTimeout" });

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    private void checkQuickFix(ValidatorMessage message) throws Exception {
        IResource resource = message.getResource();
        assertNotNull("ValidatorMessage resource is null.", resource);
        IMarker marker = resource.createMarker(MARKER_TYPE);
        copyAttributes(marker, message.getAttributes());

        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
        assertTrue("Did not get any marker resolutions", resolutions.length > 0);
    }

    private void runQuickFix(ValidatorMessage message) throws Exception {
        IResource resource = message.getResource();
        assertNotNull("ValidatorMessage resource is null.", resource);
        IMarker marker = resource.createMarker(MARKER_TYPE);
        copyAttributes(marker, message.getAttributes());

        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
        assertTrue("Did not get any marker resolutions.", resolutions.length > 0);
        resolutions[0].run(marker);
        TestUtil.jobWaitBuildandResource("Waiting for marker resolution run");
    }

    private void runQuickFix(ValidatorMessage message, ResolutionType resolutionType, String[] hints) throws Exception {
        IResource resource = message.getResource();
        assertNotNull("ValidatorMessage resource is null.", resource);
        IMarker marker = resource.createMarker(MARKER_TYPE);
        copyAttributes(marker, message.getAttributes());

        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
        assertTrue("Did not get any marker resolutions.", resolutions.length > 0);
        boolean foundResolution = false;
        for (IMarkerResolution resolution : resolutions) {
            if (resolution instanceof ConfigurationQuickFix) {
                if (resolutionType == ((ConfigurationQuickFix) resolution).getResolutionType()) {
                    if (checkHints(resolution, hints)) {
                        foundResolution = true;
                        resolution.run(marker);
                        TestUtil.jobWaitBuildandResource("Waiting for marker resolution run");
                        break;
                    }
                }
            }
        }

        assertTrue("Did not find a matching resolution", foundResolution);
    }

    @SuppressWarnings("rawtypes")
    private void copyAttributes(IMarker marker, Map attrs) throws Exception {
        Iterator entries = attrs.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            marker.setAttribute(key, value);
        }
    }

    private boolean checkHints(IMarkerResolution resolution, String[] hints) {
        if (hints == null) {
            return true;
        }

        final String label = resolution.getLabel();
        for (String hint : hints) {
            if (!label.contains(hint)) {
                return false;
            }
        }

        return true;
    }

    // Variable expression - undefined left operand: ${myport+1}
    @Test
    public void variableExpressionUndefinedLeftOperand() throws Exception {
        String serverName = "variableExpressionUndefinedLeftOperand";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionUndefinedLeftOperand, new String[] { "${myport+1}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_VARIABLE, null);

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    // Variable expression - undefined right operand: ${1+myport}
    @Test
    public void variableExpressionUndefinedRightOperand() throws Exception {
        String serverName = "variableExpressionUndefinedRightOperand";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 1);
        checkMessage(messages[0], NLS.bind(Messages.expressionUndefinedRightOperand, new String[] { "${1+myport}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_VARIABLE, null);

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    // Variable expression - undefined both operands: ${myport+somenumber}
    @Test
    public void variableExpressionUndefinedBothOperands() throws Exception {
        String serverName = "variableExpressionUndefinedBothOperands";
        IFile file = getServerFile(serverName, "server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        checkMessageCount(messages, 2);

        checkMessage(messages[0], NLS.bind(Messages.expressionUndefinedLeftOperand, new String[] { "${myport+somenumber}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        runQuickFix(messages[0], ResolutionType.BEST_MATCH_VARIABLE, null);

        checkMessage(messages[1], NLS.bind(Messages.expressionUndefinedRightOperand, new String[] { "${myport+somenumber}", "httpPort", "httpEndpoint" }),
                     serverName + "/" + file.getName(), 17);
        runQuickFix(messages[1], ResolutionType.BEST_MATCH_VARIABLE, null);

        messages = TestUtil.validate(file);
        checkMessageCount(messages, 0);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: QuickFixTestCase\n");
    }
}