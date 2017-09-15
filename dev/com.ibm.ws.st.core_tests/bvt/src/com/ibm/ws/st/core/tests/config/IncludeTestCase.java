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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;

import junit.framework.TestSuite;

/**
 * Test include files:
 * - Test each type of include (relative, shared, absolute)
 * - Test multiple levels of includes
 * - Test that the merged view shows the main configuration file merged with
 * its included files (test multiple levels of include)
 */
@RunWith(AllTests.class)
public class IncludeTestCase extends ToolsTestBase {

    protected static final String RUNTIME_NAME = "IncludeTestCase_runtime";
    protected static final String SERVER_NAME = "includeTestServer";

    private static final String[] CONFIG_NAMES = {
                                                   "server.xml",
                                                   "a.xml",
                                                   "includeTestRelativeInclude.xml",
                                                   "includeTestSharedInclude.xml",
                                                   "includeTestAbsoluteInclude.xml"
    };

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(IncludeTestCase.getOrderedTests());
        suite.setName(IncludeTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(IncludeTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(IncludeTestCase.class, "testCheckFiles"));
        testSuite.addTest(TestSuite.createTest(IncludeTestCase.class, "testAddAbsoluteInclude"));
        testSuite.addTest(TestSuite.createTest(IncludeTestCase.class, "testConfigurationURIs"));
        testSuite.addTest(TestSuite.createTest(IncludeTestCase.class, "testMergedConfiguration"));
        testSuite.addTest(TestSuite.createTest(IncludeTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        init();
        setupRuntimeServers();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, null);
    }

    @Test
    public void testCheckFiles() throws Exception {
        // Check that the server config files were copied.
        checkFile("bootstrap.properties");
        checkFile("server.xml");
        checkFile("a.xml");
    }

    @Test
    public void testAddAbsoluteInclude() throws Exception {
        // Modify the main server.xml file and make sure the changes are copied
        // to the runtime.
        final IFile file = getServerFile("a.xml");
        assertNotNull(file);

        final String filePath = file.getLocation().toOSString();
        Document doc = FileUtil.getDOM(filePath);
        assertNotNull(doc);

        final IPath includePath = resourceFolder.append("config/absoluteConfig/includeTestAbsoluteInclude.xml");
        final String absolutionLocation = includePath.toOSString();
        Element elem = doc.getDocumentElement();
        Element includeElem = doc.createElement(Constants.INCLUDE_ELEMENT);

        includeElem.setAttribute(Constants.LOCATION_ATTRIBUTE, absolutionLocation);
        elem.appendChild(doc.createTextNode("\n    "));
        elem.appendChild(includeElem);
        elem.appendChild(doc.createTextNode("\n"));
        FileUtil.saveDOM(doc, filePath);

        boolean refreshed = TestUtil.refreshResource(file);
        assertTrue(refreshed);

        boolean jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);

        // Get the DOM for the runtime copy and check that the changes are there.
        boolean found = false;
        final IPath runtimePath = (new Path(runtimeLocation)).append("/usr/servers/" + wsServer.getServerName() + "/a.xml");
        doc = FileUtil.getDOM(runtimePath.toOSString());
        elem = doc.getDocumentElement();

        final NodeList includeList = elem.getElementsByTagName(Constants.INCLUDE_ELEMENT);
        for (int i = 0; i < includeList.getLength(); i++) {
            Element node = (Element) includeList.item(i);
            String location = DOMUtils.getAttributeValue(node, Constants.LOCATION_ATTRIBUTE);
            if (absolutionLocation.equals(location)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testConfigurationURIs() {
        URI[] uris = wsServer.getConfigurationURIs();

        int count = 0;
        for (URI uri : uris) {
            final IPath path = new Path(uri.getPath());
            final String name = path.lastSegment();
            for (int i = 0; i < CONFIG_NAMES.length; ++i) {
                if (CONFIG_NAMES[i].equals(name)) {
                    count++;
                    break;
                }
            }
        }
        assertTrue(count == CONFIG_NAMES.length);
    }

    @Test
    public void testMergedConfiguration() throws Exception {
        final IFile file = getServerFile("server.xml");
        assertNotNull(file);

        final IPath path = com.ibm.ws.st.core.internal.Activator.getInstance().getStateLocation().append("flat-config.xml");
        final URI uri = file.getLocationURI();
        ConfigurationFile config = wsServer.getConfigurationFileFromURI(uri);
        assertNotNull(config);

        try {
            config.flatten(path.toFile());
        } catch (Throwable t) {
            // flatten
        }

        config = new ConfigurationFile(path.toFile().toURI(), config.getUserDirectory());
        final Element server = config.getDocument().getDocumentElement();
        assertNotNull(server);

        // check features, we should have 2 features in the merged configuration
        final List<String> features = config.getFeatures();
        assertTrue(features.size() == 2);

        final Element appMonitor = DOMUtils.getFirstChildElement(server, Constants.APPLICATION_MONITOR);
        assertNotNull(appMonitor);

        final Element registry = DOMUtils.getFirstChildElement(server, "basicRegistry");
        assertNotNull(registry);

        final Element dataSource = DOMUtils.getFirstChildElement(server, "dataSource");
        assertNotNull(dataSource);

        final Element httpEndpoint = DOMUtils.getFirstChildElement(server, Constants.HTTP_ENDPOINT);
        assertNotNull(httpEndpoint);
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        wait("Exiting test: IncludeTestCase", 1000);
    }

    private void checkFile(String fileName) throws Exception {
        IFile ifile = getServerFile(fileName);
        assertNotNull("No IFile found for: " + fileName, ifile);
        assertTrue("IFile does not exist for: " + ifile.getLocation(), ifile.exists());
        File file = ifile.getLocation().toFile();
        assertTrue("File does not exist for: " + file.getPath(), file.exists());
        File dir = new File(runtimeLocation);
        String dirPath = dir.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        assertTrue("File is not located in runtime. File path: " + filePath + ", runtime path: " + dirPath, filePath.startsWith(dirPath));
    }

    // Lookup the IFile given a server name and a file name.
    private IFile getServerFile(String relPath) {
        IFolder folder = wsServer.getFolder();
        return folder.getFile(new Path(relPath));
    }

    // Set up all of the runtime servers needed for this set of tests, copying
    // the folders and files from the test project to the runtime usr/servers directory.
    private void setupRuntimeServers() throws IOException {
        IPath resourcePath = resourceFolder.append("/config/includeTestServer");
        IPath runtimePath = (new Path(runtimeLocation)).append("/usr/servers/" + SERVER_NAME);
        FileUtil.copyFiles(resourcePath.toPortableString(), runtimePath.toPortableString());

        resourcePath = resourceFolder.append("config/sharedConfig");
        runtimePath = (new Path(runtimeLocation)).append("/usr/shared/config");
        FileUtil.copyFiles(resourcePath.toPortableString(), runtimePath.toPortableString());
    }

}
