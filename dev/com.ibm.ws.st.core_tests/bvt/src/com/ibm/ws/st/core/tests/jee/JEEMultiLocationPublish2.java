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
package com.ibm.ws.st.core.tests.jee;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.tests.util.ServerTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test publish of applications in various locations - switch between run and debug modes", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JEEMultiLocationPublish2 extends JEETestBase {

    private static final String resourceLoc = "jee/JEEMultiLocationPublish";

    public String getClassName() {
        return JEEMultiLocationPublish2.class.getSimpleName();
    }

    @Test
    public void test1_doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        createServer();
        createVM();

        IPath srcPath = resourceFolder.append(resourceLoc);
        IPath tmpPath = new Path(System.getProperty("java.io.tmpdir"));
        tmpPath = tmpPath.append("JEEMultiLocationPublish");

        // Copy test project directories to a temporary location and import projects from that location without
        // copying so the project location is not in the workspace
        copyAndImport("locationAProjects", srcPath, new String[] { "UtilA", "WebA" }, tmpPath);
        copyAndImport("locationBProjects", srcPath, new String[] { "UtilB", "WebB" }, tmpPath);

        // Import a workspace project
        importProjects((new Path(resourceLoc)).append("workspaceProjects"), new String[] { "WorkspaceEAR" });

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
    }

    private void copyAndImport(String subDir, IPath srcPath, String[] projectNames, IPath tmpPath) throws Exception {
        IPath srcDir = srcPath.append(subDir);
        IPath tmpDir = tmpPath.append(subDir);
        ServerTestUtil.copyProjects(srcDir, projectNames, tmpDir);
        ServerTestUtil.importProjects(tmpDir, projectNames, false);
    }

    @Test
    public void test2_testWebApps() throws Exception {
        addApps("WebA", "WorkspaceEAR");
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("WebA/WebAServlet", "WebA");
        testPingWebPage("WebB/WebBServlet", "WebBServlet UtilA UtilB");
    }

    @Test
    public void test3_switchToDebugMode() throws Exception {
        stopServer();
        debugServer();
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("WebA/WebAServlet", "WebA");
        testPingWebPage("WebB/WebBServlet", "WebBServlet UtilA UtilB");
    }

    @Test
    public void test4_testRemoveApp() throws Exception {
        removeApp("WorkspaceEAR", 2500);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("WebA/WebAServlet", "WebA");
        testWebPageNotFound("WebB/WebBServlet", "WebBServlet UtilA UtilB");
    }

    @Test
    public void test5_switchToRunMode() throws Exception {
        stopServer();
        startServer();
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("WebA/WebAServlet", "WebA");
        testWebPageNotFound("WebB/WebBServlet", "WebBServlet UtilA UtilB");
    }

    public void test6_addApp() throws Exception {
        addApp("WorkspaceEAR");
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("WebA/WebAServlet", "WebA");
        testPingWebPage("WebB/WebBServlet", "WebBServlet UtilA UtilB");
    }

    @Test
    public void test99_doTearDown() throws Exception {
        removeApps("WebA", "WorkspaceEAR");
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }

}