/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.contextRoot;

import junit.framework.Assert;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.util.WebResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.WebLaunchable;
import com.ibm.ws.st.core.tests.jee.JEETestBase;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "JEE context root test - default web app", isStable = true)
@RunWith(AllTests.class)
public class JEEContextRoot_WAR_Without_XML extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEContextRoot_WAR_Without_XML.getOrderedTests());
        suite.setName(JEEContextRoot_WAR_Without_XML.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEContextRoot_WAR_Without_XML.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEContextRoot_WAR_Without_XML.class, "testContextRoot"));
        testSuite.addTest(TestSuite.createTest(JEEContextRoot_WAR_Without_XML.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEEContextRoot_WAR_Without_XML");
        init();
        createRuntime();
        createServer();
        createVM();

        ModuleHelper.importWAR(resourceFolder.append("JEEDDtesting/contextRoot/ConRootServlet.war").toOSString(), "ConRootServlet", runtime);
        deleteFile(getProject("ConRootServlet"), "WebContent/WEB-INF/ibm-web-ext.xml", 500);
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("ConRootServlet");
    }

    @Test
    public void testContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("ConRootServlet");
        IModuleArtifact servlet = new WebResource(module, new Path("/*"));
        WebLaunchable launch = new WebLaunchable(server, servlet);
        String url = launch.getURL().toString();
        String expected = "http://localhost:9080/ConRootServlet/*";
        print("returned url " + url);
        Assert.assertEquals("The context root is incorrect", expected, url);
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("ConRootServlet", 2500); //in case the publish is not done. 
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEEContextRoot_WAR_Without_XML\n", 5000);
    }
}