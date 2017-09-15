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

/**
 *
 */
@TestCaseDescriptor(description = "Test publish of enterprise app with resource adapter", isStable = true)
@RunWith(AllTests.class)
public class JEEPublishJCA_traceFileName_stdout extends JEEPublishJCA {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JEEPublishJCA_traceFileName_stdout.getOrderedTests());
        suite.setName(JEEPublishJCA_traceFileName_stdout.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();

        testSuite.addTest(TestSuite.createTest(JEEPublishJCA_traceFileName_stdout.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testADD"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testFIND"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testStopRAR"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "testStartRAR"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA.class, "switchLooseCfgMode"));
        testSuite.addTest(TestSuite.createTest(JEEPublishJCA_traceFileName_stdout.class, "doTearDown"));

        return testSuite;
    }

    @Override
    @Test
    public void doSetup() throws Exception {
        print("Starting test: " + getClassName());
        init();
        createRuntime();
        if (!runtimeSupportsFeature(getJCAFeature())) {
            print("ATTENTION: Runtime does not support feature " + getJCAFeature() + ", skipping test.");
            return;
        }
        createServer(runtime, SERVER_NAME, "resources/jee/JEEPublishJCA");
        createVM();

        //Add <logging traceFileName="stdout"/> to server.xml
        ConfigurationFile serverConfig = wsServer.getConfiguration();
        Element logging = serverConfig.addElement(Constants.LOGGING);
        logging.setAttribute(Constants.TRACE_FILE, "stdout");
        serverConfig.save(null);
        wsServer.refreshConfiguration();

        importProjects(new Path(getResourceDir()),
                       new String[] { "ExampleRA", "ExampleApp" });

        startServer();
        wait("Wait 10 seconds before adding application.", 3000);
        addApp("ExampleRA", false, 30);
        addApp("ExampleApp", false, 30);
    }
}
