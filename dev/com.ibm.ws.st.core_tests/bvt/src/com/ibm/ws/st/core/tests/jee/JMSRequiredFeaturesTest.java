/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Element;

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
 */
@TestCaseDescriptor(description = "Check JMS required feature", isStable = true)
@RunWith(AllTests.class)
public class JMSRequiredFeaturesTest extends JEETestBase {
    private static final String APP_NAME = "JMSSample";
    private static final String APP_WAR = "JMSSample.war";
    private static final String[] EXPECTED_FEATURES = new String[] { "jsp", "localConnector", "wasJmsClient" };
    private static ArrayList<String> initialFeatures = new ArrayList<String>(3);

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JMSRequiredFeaturesTest.getOrderedTests());
        suite.setName(JMSRequiredFeaturesTest.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "testImportandAddApp"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "testPublishApp"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "testFeaturesAdded"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "testRemoveFeatures"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "testSimulatePublish"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "testFeaturesReadded"));
        testSuite.addTest(TestSuite.createTest(JMSRequiredFeaturesTest.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JMSRequiredFeaturesTest");
        init();
        createRuntime();
        createServer();
        createVM(JDK_NAME);
    }

    @Test
    public void testImportandAddApp() throws Exception {
        startServer();
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        print("Initial feature list");
        for (String feature : ws.getConfiguration().getFeatures()) {
            initialFeatures.add(feature);
            print(feature);
        }

        IPath resourceFolder = TestsPlugin.getInstallLocation().append("resources");
        IPath warPath = resourceFolder.append(APP_WAR);

        ModuleHelper.importWAR(warPath.toOSString(), APP_NAME, runtime);
    }

    @Test
    public void testPublishApp() throws Exception {
        addApp(APP_NAME);
    }

    @Test
    public void testFeaturesAdded() throws Exception {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        print("After publish feature list");
        for (String feature : ws.getConfiguration().getFeatures())
            print(feature);
        FeatureUtil.verifyFeatures(wr, EXPECTED_FEATURES, ws.getConfiguration().getFeatures());
    }

    @Test
    public void testRemoveFeatures() throws Exception {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();
        configFile.removeElement(Constants.FEATURE_MANAGER);
        Element featureManager = configFile.addElement(Constants.FEATURE_MANAGER);
        configFile.addElement(featureManager, Constants.FEATURE);
        for (String feature : initialFeatures) {
            print("Adding feature: " + feature);
            configFile.addFeature(feature);
        }
        configFile.save(null);
        ws.refreshConfiguration();
    }

    @Test
    public void testSimulatePublish() throws Exception {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();

        List<IModule[]> moduleList = new ArrayList<IModule[]>(1);
        List<IModuleResourceDelta[]> deltaList = new ArrayList<IModuleResourceDelta[]>(1);
        IModule[] module = new IModule[] { ModuleHelper.getModule(APP_NAME) };
        WebSphereServerBehaviour wsb = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);

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
    }

    @Test
    public void testFeaturesReadded() throws Exception {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        print("Testing features re-added...");
        for (String feature : ws.getConfiguration().getFeatures())
            print(feature);
        FeatureUtil.verifyFeatures(wr, EXPECTED_FEATURES, ws.getConfiguration().getFeatures());
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp(APP_NAME, 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JMSRequiredFeaturesTest\n", 1000);
    }

}