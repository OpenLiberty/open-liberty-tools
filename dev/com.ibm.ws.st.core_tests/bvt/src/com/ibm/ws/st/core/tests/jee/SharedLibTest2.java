/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibRef;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibraryRefType;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.jee.core.internal.JEEServerExtConstants;
import com.ibm.ws.st.jee.core.internal.SharedLibRefInfo;
import com.ibm.ws.st.jee.core.internal.SharedLibertyUtils;

// Test that common and private library references can be added and
// changed on a war project.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SharedLibTest2 extends SharedLibBase {

    private static IModule util1Module;
    private static IModule util2Module;
    private static IModule util3Module;
    private static IModule warModule;
    private static List<LibRef> libRefs;

    @Test
    public void test01_doSetup() throws Exception {
        WLPCommonUtil.cleanUp();
        System.setProperty("wtp.autotest.noninteractive", "true");
        init();

        createVM(JDK_NAME);
        createRuntime(RUNTIME_NAME);
        createServer(runtime, SERVER_NAME, "resources/JEEDDtesting/SharedLibTest2/server");
        wait("Waiting for server creation", 3000);

        importProjects(new Path("JEEDDtesting/SharedLibTest2"), new String[] { UTIL1_NAME, UTIL2_NAME, UTIL3_NAME, WAR_NAME });

        util1Module = ModuleHelper.getModule(UTIL1_NAME);
        util2Module = ModuleHelper.getModule(UTIL2_NAME);
        util3Module = ModuleHelper.getModule(UTIL3_NAME);
        warModule = ModuleHelper.getModule(WAR_NAME);

        setupUtilLibrary(util1Module.getProject(), UTIL1_ID);
        setupUtilLibrary(util2Module.getProject(), UTIL2_ID);
        setupUtilLibrary(util3Module.getProject(), UTIL3_ID);
    }

    @Test
    public void test02_addWarModule() throws Exception {
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.modifyModules(new IModule[] { util1Module, util2Module, util3Module, warModule }, null, null);
        IServer curServer = wc.save(true, null);
        wait("Waiting for add modules to server", 3000);
        safePublishIncremental(curServer);
        assertNotNull("Application should be added to the configuration", getApp(WAR_NAME));
    }

    @Test
    public void test03_addLibRefs() throws Exception {
        SharedLibRefInfo refInfo = SharedLibertyUtils.getSharedLibRefInfo(warModule.getProject());
        libRefs = new ArrayList<LibRef>();
        libRefs.add(new LibRef(UTIL1_ID, LibraryRefType.COMMON));
        libRefs.add(new LibRef(UTIL2_ID, LibraryRefType.PRIVATE));
        refInfo.setLibRefs(libRefs);
        SharedLibertyUtils.saveSettings(refInfo, warModule.getProject().getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH));
        warModule.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    @Test
    public void test04_runQuickFix() throws Exception {
        runQuickFix(WAR_NAME);
    }

    @Test
    public void test05_checkSharedLibRefUpdated() throws Exception {
        checkLibRefs(getApp(WAR_NAME), libRefs);
    }

    @Test
    public void test06_updateLibRefs() throws Exception {
        SharedLibRefInfo refInfo = SharedLibertyUtils.getSharedLibRefInfo(warModule.getProject());
        libRefs = new ArrayList<LibRef>();
        libRefs.add(new LibRef(UTIL1_ID, LibraryRefType.COMMON));
        libRefs.add(new LibRef(UTIL2_ID, LibraryRefType.COMMON));
        libRefs.add(new LibRef(UTIL3_ID, LibraryRefType.PRIVATE));
        refInfo.setLibRefs(libRefs);
        SharedLibertyUtils.saveSettings(refInfo, warModule.getProject().getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH));
        warModule.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    @Test
    public void test07_runQuickFix() throws Exception {
        runQuickFix(WAR_NAME);
    }

    @Test
    public void test08_checkSharedLibRefUpdated() throws Exception {
        checkLibRefs(getApp(WAR_NAME), libRefs);
    }

    @Test
    public void test09_updateLibRefs() throws Exception {
        SharedLibRefInfo refInfo = SharedLibertyUtils.getSharedLibRefInfo(warModule.getProject());
        libRefs = new ArrayList<LibRef>();
        libRefs.add(new LibRef(UTIL1_ID, LibraryRefType.COMMON));
        libRefs.add(new LibRef(UTIL2_ID, LibraryRefType.COMMON));
        libRefs.add(new LibRef(UTIL3_ID, LibraryRefType.COMMON));
        refInfo.setLibRefs(libRefs);
        SharedLibertyUtils.saveSettings(refInfo, warModule.getProject().getLocation().append(JEEServerExtConstants.SHARED_LIBRARY_REF_SETTING_FILE_PATH));
        warModule.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    @Test
    public void test10_runQuickFix() throws Exception {
        runQuickFix(WAR_NAME);
    }

    @Test
    public void test11_checkSharedLibRefUpdated() throws Exception {
        checkLibRefs(getApp(WAR_NAME), libRefs);
    }

    @Test
    public void test12_checkPrivateLibRefsGone() throws Exception {
        ConfigurationFile config = wsServer.getConfiguration();
        Element appElem = config.getApplicationElement(Constants.WEB_APPLICATION, WAR_NAME);
        for (Element child = DOMUtils.getFirstChildElement(appElem, Constants.LIB_CLASSLOADER); child != null; child = DOMUtils.getNextElement(child, Constants.LIB_CLASSLOADER)) {
            assertFalse("The privateLibraryRef attribute should have been removed from app: " + WAR_NAME, child.hasAttribute(Constants.LIB_PRIVATE_LIBREF));
        }
    }

    @Test
    public void test13_testApp() throws Exception {
        startServer();
        wait("Wait 5 seconds before pinging the application.", 5000);
        testPingWebPage("Web1/Web1Servlet", "Infos: Util1 class, Util2 class, Util3 class");
    }

    @Test
    public void test99_doTearDown() {
        cleanUp();
    }
}
