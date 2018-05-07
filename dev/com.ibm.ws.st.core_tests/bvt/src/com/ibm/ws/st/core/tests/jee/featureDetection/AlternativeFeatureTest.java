/*
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 */
package com.ibm.ws.st.core.tests.jee.featureDetection;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.FeatureUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * Test checks that required features are added UNLESS an acceptable alternative
 * is already present in the server config.
 *
 */
@TestCaseDescriptor(description = "Check alternative feature detection", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AlternativeFeatureTest extends ToolsTestBase {
    private static final String SERVER_NAME = "AlternativeFeatureServer";
    private static final String SERVER_RESOURCES_PATH = "resources/FeatureTesting/AlternativeFeatureTest/AlternativeFeatureServer";

    private static final String PROJECT_ROOT = "FeatureTesting/AlternativeFeatureTest";
    private static final String JSF_FACET_PROJECT_NAME = "TestJSFFacet";
    private static final String JSF_FACES_CONFIG_PROJECT_NAME = "TestJSFFaces";

    private static final String FACET_SERVLET_PATH = "TestJSFFacet/JSFFacetServlet";
    private static final String FACET_SERVLET_BODY = "Hello from JSFFacetServlet";

    private static final String FACES_CONFIG_SERVLET_NAME = "TestJSFFaces/JSFFacesServlet";
    private static final String FACES_CONFIG_SERVLET_BODY = "Hello from JSFFacesServlet";

    private static final String JSF_CONTAINER_FEATURE = "jsfContainer";

    @Test
    public void test01_doSetup() throws Exception {
        print("Starting test: AlternativeFeatureTest");
        init();
        createRuntime();
        createServer(runtime, SERVER_NAME, SERVER_RESOURCES_PATH);
        createVM(JDK_NAME);
        importProjects(new Path(PROJECT_ROOT), new String[] { JSF_FACET_PROJECT_NAME, JSF_FACES_CONFIG_PROJECT_NAME });
        startServer();
    }

    @Test
    public void test02_testJSFAlternativeFacet() throws Exception {

        // Test that jsf is added
        addApp(JSF_FACET_PROJECT_NAME);
        checkFeatures(new String[] { "jsf" }, new String[] {});
        testPingWebPage(FACET_SERVLET_PATH, FACET_SERVLET_BODY);
        try {
            removeApp(JSF_FACET_PROJECT_NAME, 2500);
        } catch (Exception e) {
            print("Failed to remove projects, problems might occur", e);
        }

        resetFeatures();

        // Test that jsf is NOT added in this case, since it should
        // see jsfContainer as equivalent.
        String feature = resolveFeature(JSF_CONTAINER_FEATURE);
        if (feature != null) {
            addFeatures(new String[] { feature });

            addApp(JSF_FACET_PROJECT_NAME);
            checkFeatures(new String[] { "jsfContainer" }, new String[] { "jsf" });
            testPingWebPage(FACET_SERVLET_PATH, FACET_SERVLET_BODY);

            try {
                removeApp(JSF_FACET_PROJECT_NAME, 2500);
            } catch (Exception e) {
                print("Failed to remove projects, problems might occur", e);
            }

            resetFeatures();
        }

    }

    @Test
    public void test03_testJSFAlternativeFaces() throws Exception {

        // Test that jsf is added
        addApp(JSF_FACES_CONFIG_PROJECT_NAME);
        checkFeatures(new String[] { "jsf" }, new String[] {});
        testPingWebPage(FACES_CONFIG_SERVLET_NAME, FACES_CONFIG_SERVLET_BODY);

        try {
            removeApp(JSF_FACES_CONFIG_PROJECT_NAME, 2500);
        } catch (Exception e) {
            print("Failed to remove projects, problems might occur", e);
        }

        resetFeatures();

        // Test that jsf is NOT added in this case, since it should
        // see jsfContainer as equivalent.
        String feature = resolveFeature(JSF_CONTAINER_FEATURE);
        if (feature != null) {
            addFeatures(new String[] { feature });

            addApp(JSF_FACES_CONFIG_PROJECT_NAME);
            checkFeatures(new String[] { "jsfContainer" }, new String[] { "jsf" });
            testPingWebPage(FACES_CONFIG_SERVLET_NAME, FACES_CONFIG_SERVLET_BODY);

            try {
                removeApp(JSF_FACES_CONFIG_PROJECT_NAME, 2500);
            } catch (Exception e) {
                print("Failed to remove projects, problems might occur", e);
            }

            resetFeatures();
        }

    }

    public void test99_doTearDown() {
        WLPCommonUtil.cleanUp();
        wait("Ending test: AlternativeFeatureTest\n", 1000);
    }

    private void checkFeatures(String[] featuresPresent, String[] featuresAbsent) {
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        List<String> serverFeatures = wsServer.getConfiguration().getFeatures();
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);

        FeatureUtil.verifyFeatures(wsRuntime, featuresPresent, serverFeatures);

        for (String feature : featuresAbsent) {
            FeatureUtil.verifyFeatureNotEnabled(wsRuntime, feature, serverFeatures);
        }

    }

    private void resetFeatures() {
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        List<String> serverFeatures = wsServer.getConfiguration().getFeatures();

        // Keep localconnector installed
        Iterator<String> featureIterator = serverFeatures.iterator();
        while (featureIterator.hasNext()) {
            String feature = featureIterator.next();
            if (feature.toLowerCase().startsWith("localconnector")) {
                featureIterator.remove();
                break;
            }
        }

        wsServer.getConfiguration().removeFeatures(serverFeatures);

        try {
            wsServer.getConfiguration().save(new NullProgressMonitor());
            wsServer.refreshConfiguration();
        } catch (IOException e) {
            print("Problem trying to save configuration to " + wsServer.getConfiguration().getName());
            print(e.toString());
        }
    }

    private void addFeatures(String[] features) throws Exception {
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        if (features != null) {
            for (String feature : features) {
                wsServer.getConfiguration().addFeature(feature);
            }

            wsServer.getConfiguration().save(new NullProgressMonitor());
            wsServer.refreshConfiguration();
        }
    }
}
