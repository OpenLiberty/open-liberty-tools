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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.ServerTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

@RunWith(AllTests.class)
public abstract class JEEPublishEarBase extends JEETestBase {

    public abstract String getResourceDir();

    public abstract String getClassName();

    public boolean copyProjects() {
        return true;
    }

    public void updateWebXML() throws Exception {
        // do nothing
    }

    public void restoreWebXML() throws Exception {
        // do nothing
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        createServer();
        createVM();

        importProjects(new Path(getResourceDir()),
                       new String[] { "Util1", "Util2", "Web1", "Web1_2", "Web1EAR", "Web2", "Web2EAR" }, copyProjects());

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("Web1EAR", false, 30);
    }

    @Test
    // test html in Web1
    public void testHTML() throws Exception {
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/a.html", "web1 a.html");
    }

    @Test
    // test jsp in Web1
    public void testJSP() throws Exception {
        testPingWebPage("Web1/a.jsp", "web1 a.jsp");
    }

    @Test
    // test servlet in Web1
    public void testServlet() throws Exception {
        testPingWebPage("Web1/S1", "web1.S1 U1");
    }

    @Test
    // test servlet in Web1 missing utility
    public void testServletMissingUtil() throws Exception {
        testWebPageNotFound("Web1/S2", "web1.S2 U2");
    }

    // update tests - can be run standalone from here

    @Test
    // test a update html in a web module
    public void testUpdateHTML() throws Exception {
        updateFile(getUpdatedFileFolder().append("a.html"),
                   getProject("Web1"), "WebContent/a.html", 2000);
        testPingWebPage("Web1/a.html", "web1 a.html modified");
    }

    @Test
    // test update jsp in a web module
    public void testUpdateJSP() throws Exception {
        updateFile(getUpdatedFileFolder().append("a.jsp"),
                   getProject("Web1"), "WebContent/a.jsp", 2000);
        testPingWebPage("Web1/a.jsp", "web1 a.jsp modified");
    }

    @Test
    // test update servlet in a web module
    public void testUpdateServlet() throws Exception {
        print("Test update");
        updateFile(getUpdatedFileFolder().append("S1.java"),
                   getProject("Web1"), "src/web1/S1.java", 2000);
        testPingWebPage("Web1/S1", "web1.S1 U1 modified");
    }

    // add/remove tests - can be run standalone from here

    @Test
    // test a add html in a web module
    public void testAddHTML() throws Exception {
        updateFile(getUpdatedFileFolder().append("b.html"),
                   getProject("Web1"), "WebContent/b.html", 2000);
        testPingWebPage("Web1/b.html", "web1 b.html");
    }

    @Test
    // test add jsp in a web module
    public void testAddJSP() throws Exception {
        updateFile(getUpdatedFileFolder().append("b.jsp"),
                   getProject("Web1"), "WebContent/b.jsp", 2000);
        testPingWebPage("Web1/b.jsp", "web1 b.jsp");
    }

    @Test
    // test add a servlet to a web module
    public void testAddServlet() throws Exception {
        print("Test add new");
        updateFile(getUpdatedFileFolder().append("S1b.java"),
                   getProject("Web1"), "src/web1/S1b.java", 2000);
        updateWebXML();
        testPingWebPage("Web1/S1b", "web1.S1b U1");
    }

    @Test
    // test remove a html from a web module
    public void testRemoveHTML() throws Exception {
        deleteFile(getProject("Web1"), "WebContent/b.html", 2000);
        testWebPageNotFound("Web1/b.html", "web1 b.html");
    }

    @Test
    // test remove jsp from a web module
    public void testRemoveJSP() throws Exception {
        deleteFile(getProject("Web1"), "WebContent/b.jsp", 2000);
        testWebPageNotFound("Web1/b.jsp", "web1 b.jsp");
    }

    @Test
    // test remove a servlet from a web module
    public void testRemoveServlet() throws Exception {
        deleteFile(getProject("Web1"), "src/web1/S1b.java", 2000);
        restoreWebXML();
        testWebPageNotFound("Web1/S1b", "web1.S1b U1");
    }

    // module control tests - can be run standalone from here

    @Test
    // test stopping the web module
    public void testStopApp() throws Exception {
        stopApp("Web1EAR", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testWebPageNotFound("Web1/a.html", "web1 a.html");
        testWebPageNotFound("Web1/a.jsp", "web1 a.jsp");
        testWebPageNotFound("Web1/S1", "web1.S1 U1");
    }

    @Test
    // test starting the web module
    public void testStartApp() throws Exception {
        startApp("Web1EAR", 30);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/a.html", "web1 a.html");
        testPingWebPage("Web1/a.jsp", "web1 a.jsp");
        testPingWebPage("Web1/S1", "web1.S1 U1");
    }

    // add/remove module tests - can be run standalone from here

    @Test
    // test add new utility and web project
    public void addUtilAndWebProject() throws Exception {
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component.update"),
                   getProject("Web1EAR"), ".settings/org.eclipse.wst.common.component",
                   1);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/S1", "web1.S1 U1");
        testPingWebPage("Web1/S2", "web1.S2 U2");
        testPingWebPage("Web1_2/S1", "web1_2.S1");
    }

    @Test
    // test modify utility project
    public void modifyUtilProject() throws Exception {
        updateFile(getUpdatedFileFolder().append("U2.java"),
                   getProject("Util2"), "src/util2/U2.java",
                   1);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/S2", "web1.S2 U2 modified");
    }

    @Test
    // test remove utility and web project
    public void removeUtilAndWebProject() throws Exception {
        updateFile(getUpdatedFileFolder().append("org.eclipse.wst.common.component"),
                   getProject("Web1EAR"), ".settings/org.eclipse.wst.common.component",
                   1);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/S1", "web1.S1 U1");
        testWebPageNotFound("Web1/S2", "web1.S2 U2");
        testWebPageNotFound("Web1_2/S1", "web1_2.S1");
    }

    // other tests - can be run standalone from here

    @Test
    // test remove web app
    public void removeApp() throws Exception {
        removeApp("Web1EAR", 2500);
        wait("Wait 3 seconds before ping.", 3000);
        testWebPageNotFound("Web1/a.html", "web1 a.html");
        testWebPageNotFound("Web1/a.jsp", "web1 a.jsp");
        testWebPageNotFound("Web1/S1", "web1.S1 U1");
    }

    @Test
    // test add 2 web apps
    public void addMultipleWebApps() throws Exception {
        addApp("Web1EAR", false, 30);
        addApp("Web2EAR", false, 30);
        wait("Wait 3 seconds before ping.", 3000);
        testPingWebPage("Web1/a.html", "web1 a.html");
        testPingWebPage("Web1/a.jsp", "web1 a.jsp");
        testPingWebPage("Web1/S1", "web1.S1 U1");
        testPingWebPage("Web2/a.html", "web2 a.html");
        testPingWebPage("Web2/a.jsp", "web2 a.jsp");
        testPingWebPage("Web2/S1", "web2.S1");
    }

    @Test
    // switch loose config mode
    public void switchLooseCfgMode() throws Exception {
        if (ServerTestUtil.supportsLooseCfg(server)) {
            boolean isLC = isLooseCfg();
            switchconfig(!isLC);
            wait("Wait 3 seconds before ping.", 3000);
            testPingWebPage("Web1/a.html", "web1 a.html");
            testPingWebPage("Web1/a.jsp", "web1 a.jsp");
            testPingWebPage("Web1/S1", "web1.S1 U1");
            testPingWebPage("Web2/a.html", "web2 a.html");
            testPingWebPage("Web2/a.jsp", "web2 a.jsp");
            testPingWebPage("Web2/S1", "web2.S1");
        }
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("Web1EAR", 2500);
        removeApp("Web2EAR", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClassName() + "\n", 5000);
    }

    protected IPath getUpdatedFileFolder() {
        return resourceFolder.append(getResourceDir() + "/files");
    }
}