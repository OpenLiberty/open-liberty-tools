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

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;

import junit.framework.TestSuite;

/**
 * Test that the server configuration linked to the workspace functions
 * properly.
 */
@RunWith(AllTests.class)
public class ConfigTestCase extends ToolsTestBase {
    protected static final String RUNTIME_NAME = "ConfigTestCase_runtime";
    protected static final String SERVER_NAME = "configTestServer";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(ConfigTestCase.getOrderedTests());
        suite.setName(ConfigTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "testCheckFiles"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "testValidate"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "testModifyConfig"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "testValidatePostModify"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "testDeleteInclude"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "testValidateAfterDelete"));
        testSuite.addTest(TestSuite.createTest(ConfigTestCase.class, "doTearDown"));

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
        // Check that the server config files.
        checkFile("bootstrap.properties");
        checkFile("server.xml");
        checkFile("includes/a.xml");
    }

    @Test
    public void testValidate() throws CoreException {
        // Run the validator and make sure all the includes are found.  There
        // should be no errors.
        IFile file = getServerFile("server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        assertTrue(messages.length == 0);
    }

    @Test
    public void testModifyConfig() throws Exception {
        // Modify the main server.xml file and make sure the changes are mirrored
        // to the runtime.
        IFile file = getServerFile("server.xml");
        assertNotNull(file);
        String filePath = file.getLocation().toOSString();
        Document doc = FileUtil.getDOM(filePath);
        assertNotNull(doc);
        Element elem = doc.getDocumentElement();
        Element child = doc.createElement(Constants.INCLUDE_ELEMENT);
        IPath includePath = resourceFolder.append("config/absoluteConfig/configTestAbsoluteInclude.xml");
        child.setAttribute(Constants.LOCATION_ATTRIBUTE, includePath.toOSString());
        elem.appendChild(doc.createTextNode("\n\t"));
        elem.appendChild(child);
        elem.appendChild(doc.createTextNode("\n"));
        FileUtil.saveDOM(doc, filePath);
        boolean refreshed = TestUtil.refreshResource(file);
        assertTrue(refreshed);

        boolean jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);

        // Get the DOM for the runtime file and check that the changes are there.
        ConfigurationFile config = wsServer.getConfiguration();
        IPath runtimePath = config.getPath();
        doc = FileUtil.getDOM(runtimePath.toOSString());
        elem = doc.getDocumentElement();
        boolean found = false;
        for (Element current = DOMUtils.getFirstChildElement(elem, Constants.INCLUDE_ELEMENT); current != null; current = DOMUtils.getNextElement(current,
                                                                                                                                                  Constants.INCLUDE_ELEMENT)) {
            String location = DOMUtils.getAttributeValue(current, Constants.LOCATION_ATTRIBUTE);
            if (location != null && location.equals(includePath.toOSString())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testValidatePostModify() throws CoreException {
        // Run the validator and make sure all the includes are found.  There
        // should be no errors.
        IFile file = getServerFile("server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        assertTrue(messages.length == 0);
    }

    @Test
    public void testDeleteInclude() throws Exception {
        // Delete an include file and make sure it is deleted from the
        // runtime.
        IFile include = getServerFile("includes/a.xml");
        assertNotNull(include);
        boolean deleted = TestUtil.deleteResource(include);
        assertTrue(deleted);

        boolean jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);

        IPath runtimePath = (new Path(runtimeLocation)).append("/usr/servers/" + SERVER_NAME + "/includes/a.xml");
        File runtimeInclude = new File(runtimePath.toOSString());
        assertFalse(runtimeInclude.exists());
    }

    @Test
    public void testValidateAfterDelete() throws CoreException {
        // Run the validator and make sure there is one include not found
        // error for a.xml.
        IFile file = getServerFile("server.xml");
        ValidatorMessage[] messages = TestUtil.validate(file);
        assertTrue(messages.length == 1);
        String message = (String) messages[0].getAttribute("message");
        assertNotNull(message);
        assertTrue(message.equals(NLS.bind(Messages.errorLoadingInclude, "includes/a.xml")));
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        wait("Exiting test: CongigTestCase", 1000);
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
        IPath resourcePath = resourceFolder.append("/config/configTestServer");
        IPath runtimePath = (new Path(runtimeLocation)).append("/usr/servers/" + SERVER_NAME);
        FileUtil.copyFiles(resourcePath.toPortableString(), runtimePath.toPortableString());

        resourcePath = resourceFolder.append("config/sharedConfig");
        runtimePath = (new Path(runtimeLocation)).append("/usr/shared/config");
        FileUtil.copyFiles(resourcePath.toPortableString(), runtimePath.toPortableString());

//        wsServer.getWebSphereRuntime().updateServerCache(true);
    }

}