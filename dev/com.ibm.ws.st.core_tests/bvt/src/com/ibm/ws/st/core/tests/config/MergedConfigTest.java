/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.config;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 * Test include files:
 * - Test each type of include (relative, shared, absolute)
 * - Test multiple levels of includes
 * - Test that the merged view shows the main configuration file merged with
 * its included files (test multiple levels of include)
 */
@TestCaseDescriptor(description = "Test merged config comments and ordering for includes and dropins", isStable = true)
@RunWith(AllTests.class)
public class MergedConfigTest extends ToolsTestBase {

    protected static final String SERVER_NAME = "mergedConfigServer";
    protected static final String SERVER_LOCATION = "resources/config/" + SERVER_NAME;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(MergedConfigTest.getOrderedTests());
        suite.setName(MergedConfigTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(MergedConfigTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(MergedConfigTest.class, "testMergedConfiguration"));
        testSuite.addTest(TestSuite.createTest(MergedConfigTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: MergedConfigTest");
        init();
        createRuntime();
        createServer(runtime, SERVER_NAME, SERVER_LOCATION);
    }

    @Test
    public void testMergedConfiguration() throws Exception {
        final IPath path = com.ibm.ws.st.core.internal.Activator.getInstance().getStateLocation().append("flat-config.xml");
        ConfigurationFile config = wsServer.getConfiguration();

        try {
            config.flatten(path.toFile());
        } catch (Throwable t) {
            // flatten
        }

        config = new ConfigurationFile(path.toFile().toURI(), config.getUserDirectory());
        final Element serverElem = config.getDocument().getDocumentElement();

        // Check default dropin
        Node node = skipTextNodes(serverElem.getFirstChild());
        assertTrue("Check for default dropin begin comment.", node.getNodeType() == Node.COMMENT_NODE);
        IFile file = getServerFile("configDropins/defaults/default.xml");
        String expectedMsg = NLS.bind(Messages.mergedConfigBeginDropin, file.getFullPath().toString());
        String actualMsg = node.getTextContent();
        assertTrue("Check text for default dropin begin comment.", expectedMsg.equals(actualMsg));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for variable element node.", node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(Constants.VARIABLE_ELEMENT));
        String varName = ((Element) node).getAttribute(Constants.VARIABLE_NAME);
        assertTrue("Check for default variable.", "defaultVar".equals(varName));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for default dropin end comment.", node.getNodeType() == Node.COMMENT_NODE);
        expectedMsg = NLS.bind(Messages.mergedConfigEndDropin, file.getFullPath().toString());
        actualMsg = node.getTextContent();
        assertTrue("Check text for default dropin end comment.", expectedMsg.equals(actualMsg));

        // Check server
        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for variable element node.", node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(Constants.VARIABLE_ELEMENT));
        varName = ((Element) node).getAttribute(Constants.VARIABLE_NAME);
        assertTrue("Check for server variable.", "serverVar".equals(varName));

        // Check include with default attributes
        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for include begin comment.", node.getNodeType() == Node.COMMENT_NODE);
        file = getServerFile("include.xml");
        expectedMsg = NLS.bind(Messages.mergedConfigBeginInclude, new String[] { file.getFullPath().toString(), "location='include.xml', optional='false', onConflict='MERGE'" });
        actualMsg = node.getTextContent();
        assertTrue("Check text for include end comment.", expectedMsg.equals(actualMsg));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for variable element node.", node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(Constants.VARIABLE_ELEMENT));
        varName = ((Element) node).getAttribute(Constants.VARIABLE_NAME);
        assertTrue("Check for include variable.", "includeVar".equals(varName));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for include end comment.", node.getNodeType() == Node.COMMENT_NODE);
        expectedMsg = NLS.bind(Messages.mergedConfigEndInclude, file.getFullPath().toString());
        actualMsg = node.getTextContent();
        assertTrue("Check text for include end comment.", expectedMsg.equals(actualMsg));

        // Check include with set attributes
        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for include2 begin comment.", node.getNodeType() == Node.COMMENT_NODE);
        file = getServerFile("include2.xml");
        expectedMsg = NLS.bind(Messages.mergedConfigBeginInclude, new String[] { file.getFullPath().toString(), "location='include2.xml', optional='true', onConflict='REPLACE'" });
        actualMsg = node.getTextContent();
        assertTrue("Check text for include2 end comment.", expectedMsg.equals(actualMsg));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for variable element node.", node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(Constants.VARIABLE_ELEMENT));
        varName = ((Element) node).getAttribute(Constants.VARIABLE_NAME);
        assertTrue("Check for include2 variable.", "include2Var".equals(varName));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for include2 end comment.", node.getNodeType() == Node.COMMENT_NODE);
        expectedMsg = NLS.bind(Messages.mergedConfigEndInclude, file.getFullPath().toString());
        actualMsg = node.getTextContent();
        assertTrue("Check text for include2 end comment.", expectedMsg.equals(actualMsg));

        // Check for featureManager from server.xml
        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for the featureManager element.", node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(Constants.FEATURE_MANAGER));

        // Check override dropin
        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for override dropin begin comment.", node.getNodeType() == Node.COMMENT_NODE);
        file = getServerFile("configDropins/overrides/override.xml");
        expectedMsg = NLS.bind(Messages.mergedConfigBeginDropin, file.getFullPath().toString());
        actualMsg = node.getTextContent();
        assertTrue("Check text for override dropin begin comment.", expectedMsg.equals(actualMsg));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for variable element node.", node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(Constants.VARIABLE_ELEMENT));
        varName = ((Element) node).getAttribute(Constants.VARIABLE_NAME);
        assertTrue("Check for override variable.", "overrideVar".equals(varName));

        node = skipTextNodes(node.getNextSibling());
        assertTrue("Check for override dropin end comment.", node.getNodeType() == Node.COMMENT_NODE);
        expectedMsg = NLS.bind(Messages.mergedConfigEndDropin, file.getFullPath().toString());
        actualMsg = node.getTextContent();
        assertTrue("Check text for override dropin end comment.", expectedMsg.equals(actualMsg));

    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        wait("Exiting test: MergedConfigTest", 1000);
    }

    // Lookup the IFile given a server name and a file name.
    private IFile getServerFile(String relPath) {
        IFolder folder = wsServer.getFolder();
        return folder.getFile(new Path(relPath));
    }

    private Node skipTextNodes(Node startNode) {
        Node node = startNode;
        while (node.getNodeType() == Node.TEXT_NODE) {
            node = node.getNextSibling();
        }
        return node;
    }

}
