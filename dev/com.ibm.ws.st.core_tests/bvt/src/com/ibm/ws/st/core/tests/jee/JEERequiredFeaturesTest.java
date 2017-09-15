/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.FeatureResolverFeature;
import com.ibm.ws.st.core.internal.RequiredFeatureMap;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.TestsPlugin;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.FeatureUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test checks that the required features are added when an app is added to the server.
 * Must be run after the JEEBuildTest, which imports the required test application.
 */
@TestCaseDescriptor(description = "Check JSP required feature", isStable = true)
@RunWith(AllTests.class)
public class JEERequiredFeaturesTest extends JEETestBase {
    private static final String RUNTIME_NAME = JEERequiredFeaturesTest.class.getCanonicalName() + "_runtime";
    private static final String SERVER_NAME = JEERequiredFeaturesTest.class.getCanonicalName() + "_server";
    private static final String PROJECT_NAME = "KitchenSink";
    private static final String[] EXPECTED_FEATURES = new String[] { "jsp" };

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEERequiredFeaturesTest.getOrderedTests());
        suite.setName(JEERequiredFeaturesTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "testImportApp"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "testModifyModules"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "testFeaturesAdded"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "testRemoveFeatures"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "testSimulatePublish"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "testFeaturesReadded"));
        testSuite.addTest(TestSuite.createTest(JEERequiredFeaturesTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEERequiredFeaturesTest");
        init();
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, "resources/ServerTesting/85NewServer");
        createVM();
        wait("Wait 3 seconds before getting configuration file.", 3000);
        ConfigurationFile configFile = wsServer.getConfiguration();
        if (configFile.hasElement(Constants.FEATURE_MANAGER)) {
            configFile.removeElement(Constants.FEATURE_MANAGER);
            configFile.save(null);
        }
        wait("Wait 5 seconds for configuration file save.", 5000);
    }

    @Test
    public void testImportApp() throws Exception {
        IPath resourceFolder = TestsPlugin.getInstallLocation().append("resources");
        IPath warPath = resourceFolder.append(PROJECT_NAME + ".war");
        ModuleHelper.importWAR(warPath.toOSString(), PROJECT_NAME, runtime);
        wait("Wait 4 seconds for import app operation to finish", 4000);
    }

    @Test
    public void testModifyModules() throws Exception {
        IModule appModule = ModuleHelper.getModule(PROJECT_NAME);
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(new IModule[] { appModule }, null, null);
        wc.save(true, null);
        WLPCommonUtil.print("Wait for modify modules operation to finish");
        WLPCommonUtil.jobWaitBuildandResource();
    }

    @Test
    public void testFeaturesAdded() throws Exception {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        FeatureUtil.verifyFeatures(wr, EXPECTED_FEATURES, ws.getConfiguration().getFeatures());
    }

    @Test
    public void testRemoveFeatures() throws Exception {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();
        configFile.removeElement(Constants.FEATURE_MANAGER);
        configFile.addElement(Constants.FEATURE_MANAGER);
        configFile.save(null);
        ws.refreshConfiguration();
        WLPCommonUtil.print("Wait for configuration refresh");
        WLPCommonUtil.jobWaitBuildandResource();
    }

    @Test
    public void testSimulatePublish() throws Exception {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();

        IModule[] module = new IModule[] { ModuleHelper.getModule(PROJECT_NAME) };
        WebSphereServerBehaviour wsb = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);
        List<IModule[]> moduleList = new ArrayList<IModule[]>(1);
        List<IModuleResourceDelta[]> deltaList = new ArrayList<IModuleResourceDelta[]>(1);
        moduleList.add(module);
        deltaList.add(wsb.getPublishedResourceDelta(module));

        try {
            RequiredFeatureMap rfm = ws.getRequiredFeatures(configFile, moduleList, deltaList, null);
            if (rfm != null) {
                List<String> rfs = FeatureResolverFeature.convertFeatureResolverArrayToStringList(rfm.getFeatures());
                if (rfs != null && !rfs.isEmpty()) {
                    for (String s : rfs)
                        configFile.addFeature(s);
                }
            }
        } catch (CoreException ce) {
            Trace.logError("Error getting required features", ce);
        }

        configFile.save(null);

        if (configFile.getIFile() == null)
            ws.refreshConfiguration();

        WLPCommonUtil.print("Wait for configuration refresh");
        WLPCommonUtil.jobWaitBuildandResource();
    }

    @Test
    public void testFeaturesReadded() throws Exception {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        FeatureUtil.verifyFeatures(wr, EXPECTED_FEATURES, ws.getConfiguration().getFeatures());
    }

    @Test
    public void doTearDown() throws Exception {
        cleanUp();
        wait("Ending test: JEERequiredFeaturesTest\n", 2000);
    }
}