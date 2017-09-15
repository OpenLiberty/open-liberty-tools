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
package com.ibm.ws.st.core.tests;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.APIVisibility;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;
import com.ibm.ws.st.core.internal.config.validation.AbstractConfigurationValidator;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Feature out of sync tests", isStable = true)
@RunWith(AllTests.class)
public class OutOfSyncTest extends ToolsTestBase {

    private static final String RUNTIME_NAME = OutOfSyncTest.class.getCanonicalName() + "_runtime";
    private static final String SERVER_NAME = "outOfSyncServer";
    private static final String WAR_NAME = "KitchenSink";
    private static final String WEB_PROJ_NAME = "SharedWeb1";
    private static final String SHARED_LIB_PROJ_NAME = "SharedUtil1";
    private static final String SHARED_LIB_ID = "SharedId1";
    private static final String MARKER_TYPE = "com.ibm.ws.st.core.configmarker";
    private static final int EXPECTED_MARKERS_COUNT = 2;
    private static final String WEB_APPLICATION = "webApplication";

    private static IModule warModule;

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(OutOfSyncTest.getOrderedTests());
        suite.setName(OutOfSyncTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testCreateServer"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testImportWarApp"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testAddWarModule"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testAppAddedToConfig"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testRemoveAppFromConfig"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testApplyQuickFix1"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testAppRemoved"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testImportSharedLib"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testAddSharedModules"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testSharedLibAddedToConfig1"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testRemoveSharedLibRefFromConfig"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testApplyQuickFix2"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testSharedLibRefUpdated"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testRemoveSharedLibFromConfig"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testApplyQuickFix3"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "testSharedLibAddedToConfig2"));
        testSuite.addTest(TestSuite.createTest(OutOfSyncTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        WLPCommonUtil.cleanUp();
        System.setProperty("wtp.autotest.noninteractive", "true");
        init();
    }

    @Test
    public void testCreateServer() throws Exception {
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, "resources/OutOfSyncTesting/outOfSyncServer");
        wait("Waiting for server creation", 3000);
    }

    @Test
    public void testImportWarApp() throws Exception {
        IPath warPath = resourceFolder.append(WAR_NAME + ".war");
        ModuleHelper.importWAR(warPath.toOSString(), WAR_NAME, runtime);
        warModule = ModuleHelper.getModule(WAR_NAME);
        assertNotNull("Failed to import war app: " + WAR_NAME, warModule);
    }

    @Test
    public void testAddWarModule() throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(new IModule[] { warModule }, null, null);
        IServer curServer = wc.save(true, null);
        wait("Waiting for add modules to server", 3000);
        publish(curServer);
    }

    @Test
    public void testAppAddedToConfig() {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        assertNotNull("Main configuration file for server is null", rootConfig);

        Application[] apps = rootConfig.getApplications();
        assertNotNull("Failed to publish application '" + WAR_NAME + "' to server", apps);
        boolean found = false;
        for (Application app : apps) {
            if (WAR_NAME.equals(app.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("Could not find application: " + WAR_NAME, found);
    }

    @Test
    public void testRemoveAppFromConfig() throws Exception {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        assertNotNull("Main configuration file for server is null", rootConfig);

        final boolean result = rootConfig.removeApplication(WAR_NAME);
        assertTrue("Could not remove application '" + WAR_NAME + "' from configuration", result);

        rootConfig.save(null);
        if (rootConfig.getIFile() != null) {
            wsServer.refreshConfiguration();
        }

        wait("Waiting for server configuration refresh", 3000);
    }

    @Test
    public void testApplyQuickFix1() throws Exception {
        final IProject project = wsServer.getUserDirectory().getProject();
        assertNotNull("Could not get project", project);

        project.build(IncrementalProjectBuilder.FULL_BUILD, null);
        WLPCommonUtil.jobWaitBuild();

        IMarker[] markers = project.findMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        IMarker quickFixMarker = null;
        if (markers != null) {
            for (IMarker m : markers) {
                int fixType = m.getAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR,
                                             AbstractConfigurationValidator.QuickFixType.NONE.ordinal());

                if (fixType == AbstractConfigurationValidator.QuickFixType.OUT_OF_SYNC_APP.ordinal()) {
                    final String appName = m.getAttribute(AbstractConfigurationValidator.APPLICATION_NAME, "");
                    if (appName.equals(WAR_NAME)) {
                        quickFixMarker = m;
                        break;
                    }
                }
            }
        }

        assertNotNull("Could not find out of sync marker", quickFixMarker);
        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(quickFixMarker);
        assertTrue("Did not get any marker resolutions.", resolutions.length == EXPECTED_MARKERS_COUNT);
        resolutions[1].run(quickFixMarker);
        wait("Waiting for quick fix", 3000);
    }

    @Test
    public void testAppRemoved() {
        final IModule[] modules = server.getModules();
        boolean found = false;
        for (IModule module : modules) {
            if (warModule.equals(module))
                found = true;
        }
        assertTrue("Quick fix failed - could not remove application: " + WAR_NAME, !found);
    }

    @Test
    public void testImportSharedLib() throws Exception {
        final IPath sharedLibPath = new Path("JEEDDtesting/SharedLib");
        final IPath settingsFolder = resourceFolder.append("JEEDDtesting/SharedLib/settings");
        importProjects(sharedLibPath, new String[] { SHARED_LIB_PROJ_NAME, WEB_PROJ_NAME });

        final IProject sharedUtilProj = getProject(SHARED_LIB_PROJ_NAME);
        assertNotNull("Project for '" + SHARED_LIB_PROJ_NAME + "' is null", sharedUtilProj);
        updateFile(settingsFolder.append("com.ibm.ws.st.shared.library.u1"),
                   sharedUtilProj, ".settings/com.ibm.ws.st.shared.library", 1);

        final IProject sharedWebProj = getProject(WEB_PROJ_NAME);
        assertNotNull("Project for '" + WEB_PROJ_NAME + "' is null", sharedWebProj);
        updateFile(settingsFolder.append("com.ibm.ws.st.shared.library.ref.w1"),
                   sharedWebProj, ".settings/com.ibm.ws.st.shared.library.ref", 1);
        wait("Wait 3 seconds before adding application.", 3000);
    }

    @Test
    public void testAddSharedModules() throws Exception {
        final IModule sharedWebModule = ModuleHelper.getModule(WEB_PROJ_NAME);
        final IModule sharedLibModule = ModuleHelper.getModule(SHARED_LIB_PROJ_NAME);

        assertNotNull("Failed to import shared lib: " + SHARED_LIB_PROJ_NAME, sharedLibModule);
        assertNotNull("Failed to import shared web : " + WEB_PROJ_NAME, sharedWebModule);

        IServerWorkingCopy wc = server.createWorkingCopy();
        IModule[] modules = new IModule[] { sharedLibModule, sharedWebModule };
        wc.modifyModules(modules, null, null);
        IServer curServer = wc.save(true, null);
        wait("Waiting for add modules to server", 3000);
        publish(curServer);
    }

    @Test
    public void testSharedLibAddedToConfig1() throws Exception {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        final String[] sharedLibIds = rootConfig.getSharedLibraryIds();
        boolean found = false;
        if (sharedLibIds != null && sharedLibIds.length > 0) {
            for (String id : sharedLibIds) {
                if (SHARED_LIB_ID.equals(id)) {
                    found = true;
                }
            }
        }
        assertTrue("Could not find shared lib id '" + SHARED_LIB_ID + "' to configuration", found);
    }

    @Test
    public void testRemoveSharedLibRefFromConfig() throws Exception {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        assertNotNull("Main configuration file for server is null", rootConfig);

        Application[] apps = rootConfig.getApplications();
        for (Application app : apps) {
            if (app.getName().equals(WEB_PROJ_NAME)) {
                rootConfig.addApplication(app.getName(), WEB_APPLICATION, app.getLocation(), null, null, APIVisibility.getDefaults());
                break;
            }
        }

        rootConfig.save(null);
        if (rootConfig.getIFile() != null) {
            wsServer.refreshConfiguration();
        }

        wait("Waiting for server configuration refresh", 3000);

        apps = rootConfig.getApplications();
        String[] sharedLibRefIds = new String[0];
        for (Application app : apps) {
            if (app.getName().equals(WEB_PROJ_NAME)) {
                sharedLibRefIds = app.getSharedLibRefs();
                break;
            }
        }
        assertNull("Could not remove shared library reference for application '" + WEB_PROJ_NAME + "' from configuration", sharedLibRefIds);
    }

    @Test
    public void testApplyQuickFix2() throws Exception {
        final IProject project = wsServer.getUserDirectory().getProject();
        assertNotNull("Could not get project", project);

        project.build(IncrementalProjectBuilder.FULL_BUILD, null);
        WLPCommonUtil.jobWaitBuild();

        IMarker[] markers = project.findMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        IMarker quickFixMarker = null;
        if (markers != null) {
            for (IMarker m : markers) {
                int fixType = m.getAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR,
                                             AbstractConfigurationValidator.QuickFixType.NONE.ordinal());

                if (fixType == AbstractConfigurationValidator.QuickFixType.OUT_OF_SYNC_SHARED_LIB_REF_MISMATCH.ordinal()) {
                    final String appName = m.getAttribute(AbstractConfigurationValidator.APPLICATION_NAME, "");
                    if (appName.equals(WEB_PROJ_NAME)) {
                        quickFixMarker = m;
                        break;
                    }
                }
            }
        }

        assertNotNull("Could not find out of sync marker", quickFixMarker);
        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(quickFixMarker);
        assertTrue("Did not get any marker resolutions.", resolutions.length == 1);
        resolutions[0].run(quickFixMarker);
        wait("Waiting for quick fix", 3000);
    }

    @Test
    public void testSharedLibRefUpdated() {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        assertNotNull("Main configuration file for server is null", rootConfig);

        Application[] apps = rootConfig.getApplications();
        Application matchedApp = null;
        for (Application app : apps) {
            if (app.getName().equals(WEB_PROJ_NAME)) {
                matchedApp = app;
                break;
            }
        }

        assertTrue("Quick fix failed - could not updated shared lib references", matchedApp != null && matchedApp.getSharedLibRefs() != null);
    }

    @Test
    public void testRemoveSharedLibFromConfig() throws Exception {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        assertNotNull("Main configuration file for server is null", rootConfig);

        rootConfig.removeSharedLibrary(SHARED_LIB_ID, "TEMP", "SharedUtil1.jar");
        rootConfig.save(null);
        if (rootConfig.getIFile() != null) {
            wsServer.refreshConfiguration();
        }
        wait("Waiting for server configuration refresh", 3000);

        final String[] sharedLibIds = rootConfig.getSharedLibraryIds();
        boolean removed = true;
        if (sharedLibIds != null && sharedLibIds.length > 0) {
            for (String id : sharedLibIds) {
                if (SHARED_LIB_ID.equals(id)) {
                    removed = false;
                }
            }
        }
        assertTrue("Could not remove shared lib id '" + SHARED_LIB_ID + "' from configuration", removed);
    }

    @Test
    public void testApplyQuickFix3() throws Exception {
        final IProject project = wsServer.getUserDirectory().getProject();
        assertNotNull("Could not get project", project);

        project.build(IncrementalProjectBuilder.FULL_BUILD, null);
        WLPCommonUtil.jobWaitBuild();

        IMarker[] markers = project.findMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        IMarker quickFixMarker = null;
        if (markers != null) {
            for (IMarker m : markers) {
                int fixType = m.getAttribute(AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR,
                                             AbstractConfigurationValidator.QuickFixType.NONE.ordinal());

                if (fixType == AbstractConfigurationValidator.QuickFixType.OUT_OF_SYNC_APP.ordinal()) {
                    final String appName = m.getAttribute(AbstractConfigurationValidator.APPLICATION_NAME, "");
                    if (SHARED_LIB_PROJ_NAME.equals(appName)) {
                        quickFixMarker = m;
                        break;
                    }
                }
            }
        }
        assertNotNull("Could not find out of sync marker", quickFixMarker);
        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(quickFixMarker);
        assertTrue("Did not get any marker resolutions.", resolutions.length == EXPECTED_MARKERS_COUNT);
        resolutions[0].run(quickFixMarker);
        wait("Waiting for quick fix", 3000);
    }

    private void publish(IServer server) {
        final IStatus[] status = new IStatus[1];
        server.publish(IServer.PUBLISH_INCREMENTAL, null, null, new IServer.IOperationListener() {
            @Override
            public void done(IStatus result) {
                status[0] = result;
            }
        });

        int timeRun = 0;
        while (status[0] == null && timeRun < 5) {
            try {
                print("waiting for server publish " + timeRun);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            timeRun++;
        }

        assertNotNull("The server publish operation timed out", status[0]);
    }

    @Test
    public void testSharedLibAddedToConfig2() throws Exception {
        final ConfigurationFile rootConfig = wsServer.getConfiguration();
        final String[] sharedLibIds = rootConfig.getSharedLibraryIds();
        boolean found = false;
        if (sharedLibIds != null && sharedLibIds.length > 0) {
            for (String id : sharedLibIds) {
                if (SHARED_LIB_ID.equals(id)) {
                    found = true;
                }
            }
        }
        assertTrue("Quick fix failed - could not add shared lib id '" + SHARED_LIB_ID + "' to configuration", found);
    }

    @Test
    public void doTearDown() {
        cleanUp();
    }
}
