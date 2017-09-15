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

@TestCaseDescriptor(description = "JEE context root test - Web in EAR. Context root in application.xml.", isStable = true)
@RunWith(AllTests.class)
public class JEEContextRoot_EAR_CRinAppXML extends JEETestBase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEContextRoot_EAR_CRinAppXML.getOrderedTests());
        suite.setName(JEEContextRoot_EAR_CRinAppXML.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEContextRoot_EAR_CRinAppXML.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEContextRoot_EAR_CRinAppXML.class, "testContextRoot"));
        testSuite.addTest(TestSuite.createTest(JEEContextRoot_EAR_CRinAppXML.class, "doTearDown"));

        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: JEEContextRoot_EAR_CRinAppXML");
        init();
        createRuntime();
        createServer();
        createVM();

        ModuleHelper.importEAR(resourceFolder.append("JEEDDtesting/contextRoot/ContextRootEAR.ear").toOSString(), "ContextRootEAR", null, runtime);
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("ContextRootEAR");
    }

    @Test
    public void testContextRoot() throws Exception {
        IModule module = ModuleHelper.getModule("ConRootServletInEar");
        IModuleArtifact servlet = new WebResource(module, new Path("/*"));
        WebLaunchable launch = new WebLaunchable(server, servlet);
        String url = launch.getURL().toString();
        String expected = "http://localhost:9080/AppXMLContextRootEAR/*";
        print("returned url " + url);
        Assert.assertEquals("The context root is incorrect", expected, url);
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("ContextRootEAR", 2500); //in case the publish is not done. 
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: JEEContextRoot_EAR_CRinAppXML\n", 5000);
    }
}