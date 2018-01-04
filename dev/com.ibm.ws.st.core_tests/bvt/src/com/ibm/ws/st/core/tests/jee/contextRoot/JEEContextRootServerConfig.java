/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.contextRoot;

import java.net.URL;

import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.tests.jee.JEETestBase;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "JEE context root test - verify context root calculation when set in server config.", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("restriction")
public class JEEContextRootServerConfig extends JEETestBase {

    public static final String TEST_NAME = JEEContextRootServerConfig.class.getSimpleName();
    public static final String RESOURCE_PATH = "JEEDDtesting/contextRoot/" + TEST_NAME;
    public static final String SERVER_PATH = RESOURCE_PATH + "/server";
    public static final String[] PROJECTS = new String[] { "serverEnvWeb", "bootstrapWeb", "includeWeb", "configWeb" };

    @Test
    public void test01_doSetup() throws Exception {
        print("Starting test: " + TEST_NAME);
        init();
        createRuntime();
        createServer(runtime, getServerName(), "resources/" + SERVER_PATH);
        createVM();

        importProjects(new Path(RESOURCE_PATH), PROJECTS);
        startServer();
        wait("Wait 5 seconds before adding applications.", 5000);
        for (String project : PROJECTS) {
            addApp(project, false, 1000);
        }

        // Replace the server.xml
        updateFile(resourceFolder.append(RESOURCE_PATH).append("files/server.xml"),
                   getProject(getRuntimeName()), "servers/" + getServerName() + "/server.xml", 2000);
    }

    @Test
    public void test02_serverEnvWebContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("serverEnvWeb");
        URL url = wsServer.getModuleRootURL(module);
        print("returned url " + url);
        String expected = "http://localhost:9080/serverEnvRoot/";
        assertEquals("The context root for serverEnvWeb is incorrect", expected, url.toString());
    }

    @Test
    public void test03_bootstrapWebContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("bootstrapWeb");
        URL url = wsServer.getModuleRootURL(module);
        print("returned url " + url);
        String expected = "http://localhost:9080/bootstrapRoot/";
        assertEquals("The context root for bootstrapWeb is incorrect", expected, url.toString());
    }

    @Test
    public void test04_includeWebContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("includeWeb");
        URL url = wsServer.getModuleRootURL(module);
        print("returned url " + url);
        String expected = "http://localhost:9080/includeRoot/";
        assertEquals("The context root for includeWeb is incorrect", expected, url.toString());
    }

    @Test
    public void test05_configWebContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("configWeb");
        URL url = wsServer.getModuleRootURL(module);
        print("returned url " + url);
        String expected = "http://localhost:9080/configRoot/";
        assertEquals("The context root for configWeb is incorrect", expected, url.toString());
    }

    @Test
    public void test99_doTearDown() throws Exception {
        for (String project : PROJECTS) {
            try {
                removeApp(project, 500);
            } catch (Exception e) {
                print("Failed to remove app: " + project);
            }
        }
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + TEST_NAME + "\n", 5000);
    }
}