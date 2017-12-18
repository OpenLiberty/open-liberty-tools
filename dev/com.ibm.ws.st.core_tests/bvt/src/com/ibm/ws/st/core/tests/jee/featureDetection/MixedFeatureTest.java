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
package com.ibm.ws.st.core.tests.jee.featureDetection;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FeatureUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test checks that the required features are added when an app is added to the server.
 */
@TestCaseDescriptor(description = "Check required feature detection", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MixedFeatureTest extends ToolsTestBase {
    private static final Path RESOURCE_PATH = new Path("FeatureTesting/MixedFeatureTest");
    private static final String WEB31_PROJECT = "Web31";
    private static final String WEB40_PROJECT = "Web40";
    private static final String JSP_PROJECT = "JSP";
    private static final String JSF22_PROJECT = "JSF22";
    private static final String JPA21_PROJECT = "JPA21";
    private static final String JSONB10_PROJECT = "JSONB10";
    private static final String JSONP11_PROJECT = "JSONP11";
    private static final String WEB7_PROJECT = "Web7";
    private static final String WEB7EAR_PROJECT = "Web7EAR";
    private static final String WEB8_PROJECT = "Web8";
    private static final String WEB8EAR_PROJECT = "Web8EAR";
    private static final String JAXRS20_PROJECT = "JAXRS20";
    private static final String JAXRS21_PROJECT = "JAXRS21";

    private static final String[] ALL_PROJECTS = { WEB31_PROJECT, WEB40_PROJECT,
                                                   JSP_PROJECT, JSF22_PROJECT,
                                                   JPA21_PROJECT,
                                                   JSONB10_PROJECT, JSONP11_PROJECT,
                                                   WEB7_PROJECT, WEB7EAR_PROJECT,
                                                   WEB8_PROJECT, WEB8EAR_PROJECT,
                                                   JAXRS20_PROJECT, JAXRS21_PROJECT };

    @Test
    public void test01_doSetup() throws Exception {
        print("Starting test: MixedFeatureTest");
        init();
        createRuntime();
        createServer(runtime, "MixedFeatureTestServer", "resources/FeatureTesting/MixedFeatureTest/MixedFeatureTestServer");
        createVM(JDK_NAME);
        importProjects(RESOURCE_PATH, ALL_PROJECTS);
        startServer();
    }

    @Test
    public void test02_testWeb31() throws Exception {
        try {
            addApp(WEB31_PROJECT);
            checkFeatures("servlet-3.1");
            testPingWebPage("Web31/Web31Servlet", "Hello from Web31Servlet");
        } finally {
            cleanupAfterTest(WEB31_PROJECT);
        }
    }

    @Test
    public void test03_testWeb40() throws Exception {
        try {
            addApp(WEB40_PROJECT);
            checkFeatures("servlet-4.0");
            testPingWebPage("Web40/Web40Servlet", "Hello from Web40Servlet");
        } finally {
            cleanupAfterTest(WEB40_PROJECT);
        }
    }

    @Test
    public void test04_testWebAll() throws Exception {
        try {
            addApps(WEB31_PROJECT, WEB40_PROJECT);
            checkFeatures("servlet-4.0");
            testPingWebPage("Web31/Web31Servlet", "Hello from Web31Servlet");
            testPingWebPage("Web40/Web40Servlet", "Hello from Web40Servlet");
        } finally {
            cleanupAfterTest(WEB31_PROJECT, WEB40_PROJECT);
        }
    }

    @Test
    public void test05_testJSP() throws Exception {
        try {
            addApp(JSP_PROJECT);
            checkFeatures("jsp-2.3");
            testPingWebPage("JSP/test.jsp", "Hello from test.jsp");
        } finally {
            cleanupAfterTest(JSP_PROJECT);
        }
    }

    @Test
    public void test06_testJSF22() throws Exception {
        try {
            addApp(JSF22_PROJECT);
            checkFeatures("jsf-2.2");
            testPingWebPage("JSF22/JSF22Servlet", "Hello from JSF22Servlet");
        } finally {
            cleanupAfterTest(JSF22_PROJECT);
        }
    }

    @Test
    public void test07_testJPA21() throws Exception {
        try {
            addApp(JPA21_PROJECT);
            checkFeatures("jpa-2.1");
            testPingWebPage("JPA21/JPA21Servlet", "Hello from JPA21Servlet");
        } finally {
            cleanupAfterTest(JPA21_PROJECT);
        }
    }

    @Test
    public void test08_testJsonB10() throws Exception {
        try {
            addApps(JSONB10_PROJECT);
            checkFeatures("jsonb-1.0");
            testPingWebPage("JSONB10/JSONB10Servlet", "Hello from JSONB10Servlet");
        } finally {
            cleanupAfterTest(JSONB10_PROJECT);
        }
    }

    @Test
    public void test09_testJsonP11() throws Exception {
        try {
            addApps(JSONP11_PROJECT);
            checkFeatures("jsonp-1.1");
            testPingWebPage("JSONP11/JSONP11Servlet", "Hello from JSONP11Servlet");
        } finally {
            cleanupAfterTest(JSONP11_PROJECT);
        }
    }

    @Test
    public void test10_testJsonAll() throws Exception {
        try {
            addApps(JSONB10_PROJECT, JSONP11_PROJECT);
            checkFeatures("jsonb-1.0");
            testPingWebPage("JSONB10/JSONB10Servlet", "Hello from JSONB10Servlet");
            testPingWebPage("JSONP11/JSONP11Servlet", "Hello from JSONP11Servlet");
        } finally {
            cleanupAfterTest(JSONB10_PROJECT, JSONP11_PROJECT);
        }
    }

    @Test
    public void test11_testEar7() throws Exception {
        try {
            addApp(WEB7EAR_PROJECT);
            checkFeatures("servlet-3.1");
            testPingWebPage("Web7/Web7Servlet", "Hello from Web7Servlet");
        } finally {
            cleanupAfterTest(WEB7EAR_PROJECT);
        }
    }

    @Test
    public void test12_testEar8() throws Exception {
        try {
            addApp(WEB8EAR_PROJECT);
            checkFeatures("servlet-4.0");
            testPingWebPage("Web8/Web8Servlet", "Hello from Web8Servlet");
        } finally {
            cleanupAfterTest(WEB8EAR_PROJECT);
        }
    }

    @Test
    public void test13_testEarAll() throws Exception {
        try {
            addApps(WEB7EAR_PROJECT, WEB8EAR_PROJECT);
            checkFeatures("servlet-4.0");
            testPingWebPage("Web7/Web7Servlet", "Hello from Web7Servlet");
            testPingWebPage("Web8/Web8Servlet", "Hello from Web8Servlet");
        } finally {
            cleanupAfterTest(WEB7EAR_PROJECT, WEB8EAR_PROJECT);
        }
    }

    @Test
    public void test14_testWeb40_31() throws Exception {
        try {
            // Test that a newer feature will not be replaced with an older one
            addFeature("servlet-4.0");
            addApp(WEB31_PROJECT);
            checkFeatures("servlet-4.0");
            testPingWebPage("Web31/Web31Servlet", "Hello from Web31Servlet");
        } finally {
            cleanupAfterTest(WEB31_PROJECT);
        }
    }

    @Test
    public void test15_testJaxrs20() throws Exception {
        try {
            addApp(JAXRS20_PROJECT);
            checkFeatures("jaxrs-2.0");
            testPingWebPage("JAXRS20/JAXRS20Servlet", "Hello from JAXRS20Servlet");
        } finally {
            cleanupAfterTest(JAXRS20_PROJECT);
        }
    }

    @Test
    public void test16_testJaxrs21() throws Exception {
        try {
            addApp(JAXRS21_PROJECT);
            checkFeatures("jaxrs-2.1");
            testPingWebPage("JAXRS21/JAXRS21Servlet", "Hello from JAXRS21Servlet");
        } finally {
            cleanupAfterTest(JAXRS21_PROJECT);
        }
    }

    @Test
    public void test17_testJaxrsAll() throws Exception {
        try {
            addApps(JAXRS20_PROJECT, JAXRS21_PROJECT);
            checkFeatures("jaxrs-2.1");
            testPingWebPage("JAXRS20/JAXRS20Servlet", "Hello from JAXRS20Servlet");
            testPingWebPage("JAXRS21/JAXRS21Servlet", "Hello from JAXRS21Servlet");
        } finally {
            cleanupAfterTest(JAXRS20_PROJECT, JAXRS21_PROJECT);
        }
    }

    private void checkFeatures(String... expectedFeatures) {
        WebSphereRuntime wr = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        List<String> features = ws.getConfiguration().getFeatures();
        print("After publish feature list");
        for (String feature : features)
            print(feature);
        FeatureUtil.verifyFeatures(wr, expectedFeatures, features);
    }

    private void addFeature(String feature) {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();
        configFile.addFeature(feature);
        try {
            configFile.save(new NullProgressMonitor());
            ws.refreshConfiguration();
        } catch (IOException e) {
            print("Exception trying to save changes to config file " + configFile.getName() + " (" + e + ").");
        }
    }

    private void clearFeatures() {
        WebSphereServer ws = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        ConfigurationFile configFile = ws.getConfiguration();
        List<String> featureList = configFile.getFeatures();
        for (int i = 0; i < featureList.size(); i++) {
            if (featureList.get(i).toLowerCase().startsWith("localconnector")) {
                featureList.remove(i);
                break;
            }
        }
        configFile.removeFeatures(featureList);
        try {
            configFile.save(new NullProgressMonitor());
            ws.refreshConfiguration();
        } catch (IOException e) {
            print("Exception trying to save changes to config file " + configFile.getName() + " (" + e + ").");
        }
    }

    private void cleanupAfterTest(String... projects) {
        try {
            removeApps(projects);
        } catch (Exception e) {
            print("Failed to remove projects", e);
        }
        clearFeatures();
    }

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

    @Test
    public void test99_doTearDown() throws Exception {
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: MixedFeatureTest\n", 1000);
    }

}