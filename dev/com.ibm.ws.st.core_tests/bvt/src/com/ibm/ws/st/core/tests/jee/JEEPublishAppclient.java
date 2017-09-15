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

import java.util.ArrayList;

import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.tests.module.ModuleHelper;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test tolerate ear with App client module", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishAppclient extends JEETestBase {

    // private static final String RESOURCE_DIR = "jee/TolerateAppClient";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishAppclient.getOrderedTests());
        suite.setName(JEEPublishAppclient.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishAppclient.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishAppclient.class, "importEar"));
        testSuite.addTest(TestSuite.createTest(JEEPublishAppclient.class, "ejbTest"));
        testSuite.addTest(TestSuite.createTest(JEEPublishAppclient.class, "ImportEAR2"));
        testSuite.addTest(TestSuite.createTest(JEEPublishAppclient.class, "doTearDown"));

        return testSuite;
    }

    @Override
    protected boolean isLooseCfg() {
        return true;
    }

    @Test
    public void doSetup() throws Exception {
        print("Starting test: TolerateAppclient.ear");
        init();
        createRuntime();
        createServer();
        createVM();

        //customer provided ear contains compilation error. Need to set publishWithError true
        IServerWorkingCopy wc = server.createWorkingCopy();
        wc.setAttribute("publishWithError", true);

        // feature conflict between servlet-3.0 & ejbLite-3.2 need to set features manually to avoid feature conflict
        wc.save(false, null);
        ConfigurationFile cf = wsServer.getConfiguration();
        cf.removeFeature("jsp-2.2");
        cf.addFeature("servlet-3.1");
        cf.save(null);
        startServer();
    }

    @Test
    // Simple EJB test.
    public void importEar() throws Exception {
        ModuleHelper.importEAR(resourceFolder.append("jee/TolerateAppClient.ear").toString(), "TolerateAppClient", new ArrayList<String>(), runtime);
        wait("wait 3 seconds before adding application.", 3000);
        addApp("TolerateAppClient", false, 30);

    }

    @Test
    // Simple EJB test.
    public void ejbTest() throws Exception {
        testPingWebPage("EJBApp", "Hello EJB World.");

    }

    @Test
    // Import test . EAR provided by chuck. Not sure if i can used customer ear for testing purpose.
    public void ImportEAR2() throws Exception {
        ModuleHelper.importEAR(resourceFolder.append("jee/TolerateAppClient2.ear").toString(), "TolerateAppClient2", new ArrayList<String>(), runtime);
    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("TolerateAppClient", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: TolerateAppclient\n", 5000);
    }
}