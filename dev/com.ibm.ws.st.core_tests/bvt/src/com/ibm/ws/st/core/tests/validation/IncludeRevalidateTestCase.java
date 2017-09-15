/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import java.net.URI;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 * Test that the dependencies for a server configuration are set up
 * properly. If they are, revalidation should happen automatically
 * if an include file changes.
 */
@TestCaseDescriptor(description = "Include revalidate test", isStable = true)
@RunWith(AllTests.class)
public class IncludeRevalidateTestCase extends ValidationTestBase {

    protected static final String PROJECT_NAME = "IncludeRevalidateTestProject";
    protected static final String SERVER_NAME = "includeRevalidate";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(IncludeRevalidateTestCase.getOrderedTests());
        suite.setName(IncludeRevalidateTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(IncludeRevalidateTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(IncludeRevalidateTestCase.class, "testAddDuplicateApplication"));
        testSuite.addTest(TestSuite.createTest(IncludeRevalidateTestCase.class, "testReplaceDuplicateApplication"));
        testSuite.addTest(TestSuite.createTest(IncludeRevalidateTestCase.class, "testAddDuplicateApplication2"));
        testSuite.addTest(TestSuite.createTest(IncludeRevalidateTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: IncludeRevalidateTestCase");
        init();
        setupRuntime(PROJECT_NAME, "/validation/revalidate");
    }

    @Test
    public void testAddDuplicateApplication() throws Exception {
        final ConfigurationFile cf = getConfigFile(SERVER_NAME);

        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put("id", "app1");
        cf.addApplication("app1", "application", "${server.config.dir}/apps/app1", attrs, null, APIVisibility.getDefaults());
        cf.save(null);
        TestUtil.wait("Waiting for configuration save", 1500);

        boolean jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);

        // Get the DOM for the runtime copy and check that the changes are there.
        final IPath path = cf.getPath();
        final Document doc = FileUtil.getDOM(path.toOSString());
        final Element elem = DOMUtils.getFirstChildElement(doc.getDocumentElement(), Constants.APPLICATION);
        assertTrue("Application element should exist.", elem != null);
        if (elem != null)
            assertTrue("Value of id attribute should be 'app1' but got: " + elem.getAttribute("id"), "app1".equals(elem.getAttribute("id")));

        TestUtil.wait("Waiting for validation", 1500);

        IMarker[] markers = null;
        try {
            markers = project.findMarkers("com.ibm.ws.st.core.configmarker", true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            // ignore
        }

        checkMarkers(markers, 1);
    }

    @Test
    public void testReplaceDuplicateApplication() throws Exception {
        final IFile file = getServerFile(SERVER_NAME, "a.xml");
        assertNotNull(file);

        final URI uri = file.getLocationURI();
        final ConfigurationFile cf = getConfigFile(SERVER_NAME, uri);
        assertNotNull(cf);

        cf.removeElement("application");
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put("id", "app2");
        cf.addApplication("app2", "application", "${server.config.dir}/apps/app2", attrs, null, APIVisibility.getDefaults());
        cf.save(null);
        TestUtil.wait("Waiting for configuration save", 1500);

        boolean jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);

        // Get the DOM for the runtime copy and check that the changes are there.
        final IPath path = cf.getPath();
        final Document doc = FileUtil.getDOM(path.toOSString());
        final Element elem = DOMUtils.getFirstChildElement(doc.getDocumentElement(), Constants.APPLICATION);
        assertTrue("Application element should exist.", elem != null);
        if (elem != null)
            assertTrue("Value of id attribute should be 'app2' but got: " + elem.getAttribute("id"), "app2".equals(elem.getAttribute("id")));

        TestUtil.wait("Waiting for validation", 1500);

        IMarker[] markers = null;
        try {
            markers = project.findMarkers("com.ibm.ws.st.core.configmarker", true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            // ignore
        }

        checkMarkers(markers, 0);
    }

    @Test
    public void testAddDuplicateApplication2() throws Exception {
        final IFile file = getServerFile(SERVER_NAME, "b.xml");
        assertNotNull(file);

        final URI uri = file.getLocationURI();
        final ConfigurationFile cf = getConfigFile(SERVER_NAME, uri);
        assertNotNull(cf);

        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put("id", "app1");
        cf.addApplication("app1", "application", "c:/tmp/app1", attrs, null, APIVisibility.getDefaults());
        cf.save(null);
        TestUtil.wait("Waiting for configuration save", 1500);

        boolean jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);

        // Get the DOM for the runtime copy and check that the changes are there.
        final IPath path = cf.getPath();
        final Document doc = FileUtil.getDOM(path.toOSString());
        final Element elem = DOMUtils.getFirstChildElement(doc.getDocumentElement(), Constants.APPLICATION);
        assertTrue("Application element should exist.", elem != null);
        if (elem != null)
            assertTrue("Value of id attribute should be 'app1' but got: " + elem.getAttribute("id"), "app1".equals(elem.getAttribute("id")));

        TestUtil.wait("Waiting for validation", 1500);

        IMarker[] markers = null;
        try {
            markers = project.findMarkers("com.ibm.ws.st.core.configmarker", true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            // ignore
        }

        checkMarkers(markers, 1);
    }

    @Test
    public void doTearDown() {
        cleanUp();
        print("Ending test: IncludeRevalidateTestCase");
    }

    private void checkMarkers(IMarker[] markers, int expectedLen) throws Exception {
        if (markers == null) {
            if (expectedLen > 0)
                Assert.fail("Validation should not result in null set of markers.");
            return;
        }
        if (markers.length != expectedLen) {
            for (IMarker marker : markers)
                print("Marker message: " + marker.getAttribute(IMarker.MESSAGE));
            Assert.fail("Validation should result in " + expectedLen + " marker(s) but got: " + markers.length);
        }
    }
}
