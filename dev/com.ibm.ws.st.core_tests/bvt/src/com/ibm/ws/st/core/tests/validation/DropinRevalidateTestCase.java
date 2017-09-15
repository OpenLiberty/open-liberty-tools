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
package com.ibm.ws.st.core.tests.validation;

import java.net.URI;

import junit.framework.TestSuite;

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

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.util.FileUtil;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test that the dependencies for a server configuration are set up
 * properly. If they are, revalidation should happen automatically
 * if a dropin file changes.
 */
@TestCaseDescriptor(description = "Dropin revalidate test", isStable = false)
@RunWith(AllTests.class)
public class DropinRevalidateTestCase extends ValidationTestBase {

    protected static final String PROJECT_NAME = "DropinRevalidateTestProject";
    protected static final String SERVER_NAME = "dropinRevalidate";
    protected static final String RESOURCE_PATH = "/validation/";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(DropinRevalidateTestCase.getOrderedTests());
        suite.setName(DropinRevalidateTestCase.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(DropinRevalidateTestCase.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(DropinRevalidateTestCase.class, "testInitialUndefinedVarError"));
        testSuite.addTest(TestSuite.createTest(DropinRevalidateTestCase.class, "testAddVarDefToDropin"));
        testSuite.addTest(TestSuite.createTest(DropinRevalidateTestCase.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: DropinRevalidateTestCase");
        init();
        setupRuntime(PROJECT_NAME, null);
        setupRuntimeServer(RESOURCE_PATH, SERVER_NAME);
    }

    @Test
    public void testInitialUndefinedVarError() throws Exception {
        // Initially there should be a marker for the undefined variable
        IMarker[] markers = null;
        try {
            markers = project.findMarkers("com.ibm.ws.st.core.configmarker", true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            // ignore
        }

        checkMarkers(markers, 1);
    }

    @Test
    public void testAddVarDefToDropin() throws Exception {
        // Add the variable definition to the dropin and check
        // the error goes away.
        final IFile file = getDefaultDropinsFile(SERVER_NAME, "default.xml");
        assertNotNull(file);

        final URI uri = file.getLocationURI();
        final ConfigurationFile cf = getConfigFile(SERVER_NAME, uri);
        assertNotNull(cf);

        cf.addVariable("myPort", "8001");
        cf.save(null);
        TestUtil.wait("Waiting for configuration save", 1500);

        boolean jobDone = TestUtil.jobWait(Constants.JOB_FAMILY);
        assertTrue(jobDone);
        jobDone = TestUtil.jobWait(ResourcesPlugin.FAMILY_MANUAL_BUILD);
        assertTrue(jobDone);

        // Get the DOM for the runtime copy and check that the changes are there.
        final IPath path = cf.getPath();
        final Document doc = FileUtil.getDOM(path.toOSString());
        final Element elem = DOMUtils.getFirstChildElement(doc.getDocumentElement(), Constants.VARIABLE_ELEMENT);
        assertTrue("Variable element should exist.", elem != null);
        if (elem != null)
            assertTrue("Value of name attribute should be 'myPort' but got: " + elem.getAttribute("name"), "myPort".equals(elem.getAttribute("name")));

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
    public void doTearDown() {
        cleanUp();
        print("Ending test: DropinRevalidateTestCase");
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
