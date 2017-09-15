/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Element;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

@TestCaseDescriptor(description = "Test publish of web app with deployment descriptor, when traceFileName is set to stdout on the server.xml ", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishWarDD_traceFileName_stdout extends JEEPublishWarDD {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishWarDD_traceFileName_stdout.getOrderedTests());
        suite.setName(JEEPublishWarDD_traceFileName_stdout.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD_traceFileName_stdout.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testServletMissingUtil"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testUpdateHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testUpdateJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testUpdateServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testAddHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testAddJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testAddServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testRemoveHTML"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testRemoveJSP"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testRemoveServlet"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testStopApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "testStartApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "addUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "modifyUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "removeUtilProject"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "removeApp"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "addMultipleWebApps"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishWarDD.class, "doTearDown"));

        return testSuite;
    }

    @Override
    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClass().getName());

        init();
        createRuntime();
        createServer();
        createVM();

        //Add <logging traceFileName="stdout"/> to server.xml
        ConfigurationFile serverConfig = wsServer.getConfiguration();
        Element logging = serverConfig.addElement(Constants.LOGGING);
        logging.setAttribute(Constants.TRACE_FILE, "stdout");
        serverConfig.save(null);
        wsServer.refreshConfiguration();

        importProjects(new Path(getResourceDir()),
                       new String[] { "Util1", "Util2", "Web1", "Web2" });

        startServer();
        wait("Wait 3 seconds before adding application.", 3000);
        addApp("Web1", false, 30);

    }
}
