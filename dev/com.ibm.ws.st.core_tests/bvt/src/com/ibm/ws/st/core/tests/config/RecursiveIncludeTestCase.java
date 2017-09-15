/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Document;

import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationIncludeFilter;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Check recursive include", isStable = true)
@RunWith(AllTests.class)
public class RecursiveIncludeTestCase extends ToolsTestBase {
    protected static final String RUNTIME_NAME = RecursiveIncludeTestCase.class.getCanonicalName() + "_runtime";
    protected static final String SERVER_NAME = "recursiveIncludeServer";
    protected static final String SERVER_LOCATION = "resources/config/recursiveInclude/" + SERVER_NAME;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(RecursiveIncludeTestCase.getOrderedTests());
        suite.setName(RecursiveIncludeTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "testGetAllConfigurations"));
        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "testGetAllIncludeFiles"));
        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "testGetAllFeatures"));
        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "testNoDuplicateFeatureMessage"));
        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "testGetIdMap"));
        testSuite.addTest(TestSuite.createTest(RecursiveIncludeTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: RecursiveIncludeTestCase");
        init();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, SERVER_LOCATION);
    }

    @Test
    public void testGetAllConfigurations() throws Exception {
        final ConfigurationFile cf = wsServer.getConfiguration();
        assertNotNull("Main configuration file is null", cf);

        final ArrayList<ConfigurationFile> list = new ArrayList<ConfigurationFile>(4);
        final ConfigurationIncludeFilter includeFilter = new ConfigurationIncludeFilter();
        cf.getAllConfigFiles(list, includeFilter);
        // Should be server.xml, a.xml, b.xml and testOverride.xml (added by test harness)
        assertTrue("Expected number of configuration files is '4', but got'" + list.size() + "'", list.size() == 4);
    }

    @Test
    public void testGetAllIncludeFiles() throws Exception {
        final IPath path = wsServer.getServerPath().append("a.xml");
        final URI uri = path.toFile().toURI();
        final ConfigurationFile cf = wsServer.getConfigurationFileFromURI(uri);

        assertNotNull("Failed to get configuration file for uri:" + uri, cf);

        final ConfigurationFile[] includes = cf.getAllIncludedFiles();
        assertTrue("Expected number of includes is '1', but got '" + includes.length + "'", includes.length == 1);
        if (includes.length > 0) {
            final IPath includePath = wsServer.getServerPath().append("b.xml");
            final URI includeURI = includePath.toFile().toURI();
            final ConfigurationFile includeConfig = includes[0];
            assertTrue("Expecting include file '" + includeURI + "', but got '" + includeConfig.getURI() + "'", includeURI.equals(includeConfig.getURI()));
        }
    }

    @Test
    public void testGetAllFeatures() throws Exception {
        final ConfigurationFile cf = wsServer.getConfiguration();
        assertNotNull("Main configuration file is null", cf);

        final List<String> features = cf.getAllFeatures();
        assertTrue("Expected number of features is '2', but got '" + features.size() + "'", features.size() == 2);
    }

    @Test
    public void testNoDuplicateFeatureMessage() throws Exception {
        final ConfigurationFile cf = wsServer.getConfiguration();
        assertNotNull("Main configuration file is null", cf);

        final IFile file = cf.getIFile();
        assertNotNull("Main configuration resource is null", file);

        final ValidatorMessage[] messages = TestUtil.validate(file);
        assertTrue("No validation errors", messages != null && messages.length > 0);

        final String duplicateFeature = NLS.bind(Messages.infoDuplicateItem, new String[] { "feature", "localConnector-1.0" });
        assertTrue(duplicateFeature, checkNoMessage(messages, duplicateFeature));
    }

    @Test
    public void testGetIdMap() throws Exception {
        final ConfigurationFile cf = wsServer.getConfiguration();
        assertNotNull("Main configuration file is null", cf);

        final Document doc = cf.getDomDocument();
        assertNotNull("IDOMDocument for configuration file is null", doc);

        final String[] ids = DOMUtils.getIds(doc, cf.getURI(), cf.getWebSphereServer(), cf.getUserDirectory(), "jdbcDriver");
        assertTrue("Ids for jdbcDriver are empty, expecting 1", ids.length == 1);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: RecursiveIncludeTestCase\n");
    }

    private boolean checkNoMessage(ValidatorMessage[] messages, String message) {
        if (messages == null || messages.length == 0) {
            return true;
        }

        for (ValidatorMessage msg : messages) {
            String actualText = (String) msg.getAttribute(IMarker.MESSAGE);
            if (messages.equals(actualText)) {
                return false;
            }
        }
        return true;
    }
}