/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.core.tests.jee;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.validation.ValidatorMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibRef;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibraryRefType;
import com.ibm.ws.st.core.tests.util.TestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test shared libraries", isStable = true)
@RunWith(AllTests.class)
public class SharedLibTest extends JEETestBase {

    private static final String LIBDIR = "tmp";

//    private IPath getUpdatedFileFolder() {
//        return resourceFolder.append("JEEDDtesting/SharedLib/Updated_Files");
//    }

    private static IPath getSettingsFileFolder() {
        return resourceFolder.append("JEEDDtesting/SharedLib/settings");
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(SharedLibTest.getOrderedTests());
        suite.setName(SharedLibTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "testSimpleUse"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "testAddNewJarToExistingLib"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "testAddNewLib"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "testAddNewEAR"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "testAddSharedLibRef"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "testRemoveAJar"));
        testSuite.addTest(TestSuite.createTest(SharedLibTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: SharedLibTest");
        init();
        createRuntime();
        createServer();
        createVM();
        importProjects(new Path("JEEDDtesting/SharedLib"), new String[] {
                                                                          "SharedUtil1",
                                                                          "SharedUtil2",
                                                                          "SharedUtil3",
                                                                          "SharedWeb1",
                                                                          "SharedWeb2",
                                                                          "SharedWeb3",
                                                                          "SharedEAR1",
        });

        // copy the shared lib setting files
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath libPath = workspaceRoot.getLocation().append(LIBDIR);
        assertTrue("Failed to create directory: " + libPath.toOSString(), libPath.toFile().mkdirs());
        String libDir = libPath.toOSString();
        libDir = libDir.replace("\\", "\\\\");
        updateSharedLib("SharedUtil1", "com.ibm.ws.st.shared.library.u1", libDir);
        updateSharedLib("SharedUtil2", "com.ibm.ws.st.shared.library.u2", libDir);
        updateSharedLib("SharedUtil3", "com.ibm.ws.st.shared.library.u3", libDir);
        updateFile(getSettingsFileFolder().append("com.ibm.ws.st.shared.library.ref.w1"),
                   getProject("SharedWeb1"), ".settings/com.ibm.ws.st.shared.library.ref", 1);
        updateFile(getSettingsFileFolder().append("com.ibm.ws.st.shared.library.ref.w2"),
                   getProject("SharedWeb2"), ".settings/com.ibm.ws.st.shared.library.ref", 1);

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("SharedUtil1", false, 2);
        addApp("SharedWeb1");
    }

    private void updateSharedLib(String projectName, String source, String libDir) throws Exception {
        IProject project = getProject(projectName);
        updateFile(getSettingsFileFolder().append(source),
                   project, ".settings/com.ibm.ws.st.shared.library", 1);
        TestUtil.modifyFile(project, ".settings/com.ibm.ws.st.shared.library", "TEMP", libDir);
    }

    @Test
    // test load the shared class in a web module
    public void testSimpleUse() throws Exception {
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("SharedWeb1/S1", "This is U1.");
    }

// Shared lib update is not supported in v85
//    @Test
//    // update the shared class
//    public void testUpdateClass() throws Exception {
//        updateFile(getUpdatedFileFolder().append("U1.java"),
//                   getProject("SharedUtil1"), "src/shared/util1/U1.java",
//                   1500);
//
//        testPingWebPage("SharedWeb1/S1", "This is new U1.");
//    }

    @Test
    // test add a new jar to existing lib
    public void testAddNewJarToExistingLib() throws Exception {
        addApp("SharedUtil2", false, 2);
        wait("Wait 5 seconds before ping.", 5000);
        testPingWebPage("SharedWeb1/S2", "This is U2.");
    }

    @Test
    // test add a new library
    public void testAddNewLib() throws Exception {
        addApp("SharedUtil3", false, 2);
        addApp("SharedWeb2", false, 30);
        wait("Wait 5 seconds before ping.", 5000);
        testPingWebPage("SharedWeb2/S3", "This is U3.");
    }

    @Test
    // test add a new library
    public void testAddNewEAR() throws Exception {
        addApp("SharedEAR1", false, 30);
        wait("Wait 5 seconds before ping.", 5000);
        testPingWebPage("SharedWeb3/S4", "This is U3.");
    }

    @Test
    // test add a reference to a published shared library and make sure that
    // the validator does not complain about the new reference
    public void testAddSharedLibRef() throws Exception {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        assertNotNull("Main configuration file for server is null", rootConfig);

        boolean isUpdated = false;
        Application[] apps = rootConfig.getApplications();
        for (Application app : apps) {
            if (app.getName().equals("SharedEAR1")) {
                List<LibRef> sharedLibRefs = app.getSharedLibRefs();
                List<LibRef> refsList = new ArrayList<LibRef>();
                if (sharedLibRefs != null) {
                    refsList.addAll(sharedLibRefs);
                }
                refsList.add(new LibRef("SharedId1", LibraryRefType.COMMON));
                rootConfig.addApplication(app.getName(), "enterpriseApplication", app.getLocation(), null, refsList, APIVisibility.getDefaults());
                isUpdated = true;
                break;
            }
        }

        assertTrue("Did not update SharedEAR1", isUpdated);

        rootConfig.save(null);
        if (rootConfig.getIFile() != null) {
            wsServer.refreshConfiguration();
        }

        wait("Waiting for server configuration refresh", 3000);

        final IFile file = rootConfig.getIFile();
        assertNotNull("Main configuration resource is null", file);

        final ValidatorMessage[] messages = TestUtil.validate(file);
        assertTrue("Got validation errors", messages == null || messages.length == 0);
    }

    //TODO add a case to test add a new lib to a published WAR.  This is not supported by tools yet.

    @Test
    // remove utility module to the EAR and test
    public void testRemoveAJar() throws Exception {
        removeApp("SharedUtil1", 2000);

        wait("Wait 3 seconds before ping.", 3000);
        testWebPageNotFound("SharedWeb1/S1", "This is U1.");
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("SharedEAR1", 300);
        removeApp("SharedWeb3", 100);
        removeApp("SharedWeb1", 100);
        removeApp("SharedWeb2", 100);
        removeApp("SharedUtil2", 100);
        removeApp("SharedUtil3", 1200);
        stopServer();
        WLPCommonUtil.cleanUp();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath libPath = workspaceRoot.getLocation().append(LIBDIR);
        if (!libPath.toFile().delete()) {
            WLPCommonUtil.print("Could not delete shared lib directory: " + libPath.toOSString());
        }
        wait("Ending test: SharedLibTest\n", 5000);
    }
}