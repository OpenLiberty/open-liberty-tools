/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class AllJEETests {
    public static final String JDK_VERSION_7 = "1.7.0";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        for (Class<? extends TestCase> testclass : addTestCases()) {
            suite.addTest(new JUnit4TestAdapter(testclass));
        }
        return suite;
    }

    /**
     * @return
     */
    private static ArrayList<Class<? extends TestCase>> addTestCases() {
        ArrayList<Class<? extends TestCase>> testsuites = new ArrayList<Class<? extends TestCase>>();
        testsuites.add(JEEBuildTest.class);
        testsuites.add(JEERequiredFeaturesTest.class);
        testsuites.add(JEENoDD_DefaultTest.class);
        testsuites.add(WebInEARTestCase.class);
        testsuites.add(JEEAddRemoveWebTest.class);
        testsuites.add(JEE6DDAddRemoveWebTest.class);
        testsuites.add(JEE6DD_EARTest.class);
        testsuites.add(JEE6DD_WebTest.class);
        testsuites.add(JEE_BinaryModule_Test.class);
        // SharedLibTest is failing because the runtime does not find the shared library when
        // it is initially deployed
//        testsuites.add(SharedLibTest.class);
        testsuites.add(WebFragment_WEBTest.class);
        testsuites.add(WebFragment_EARTest.class);
        testsuites.add(WebFragment_DDWEBTest.class);
        testsuites.add(WebFragment_DDEARTest.class);
        testsuites.add(Defect96054Test.class);
        testsuites.add(LinkedResourceTest.class);
        testsuites.add(LinkedResourceTest2.class);
        testsuites.add(JEEPublishWarNoDD.class);
        testsuites.add(JEEPublishWarDD.class);
        testsuites.add(JEEPublishWarDD_traceFileName_stdout.class);
        testsuites.add(JEEPublishEarNoDD.class);
        testsuites.add(JEEPublishEarDD.class);
        testsuites.add(JEEPublishEarRemap.class);
        testsuites.add(JEEPublishWarDeployName.class);
        testsuites.add(JEE6DD_WebTest_V85.class);
        testsuites.add(JEE6DD_EARTest_V85.class);
        // Add following tests only if JDK >= 1.7.0
        if (System.getProperty("java.version").compareTo(JDK_VERSION_7) >= 0) {
            testsuites.add(JEEPublishJCA.class);
            testsuites.add(JEEPublishJCA_traceFileName_stdout.class);
            testsuites.add(CDIRequiredFeaturesBeans11.class);
            testsuites.add(CDIRequiredFeaturesBeansVersion11.class);
            testsuites.add(CDIRequiredFeaturesBeans20.class);
            testsuites.add(CDIRequiredFeaturesBeansVersion20.class);
            testsuites.add(JEEBinaryModule.class);
            testsuites.add(WebFragmentResources.class);
        }
        testsuites.add(Defect138051Test.class);
        testsuites.add(Defect150666Test.class);
        testsuites.add(JMSRequiredFeaturesTest.class);
        testsuites.add(JEEPublishAppclient.class);
        testsuites.add(JEEMultiLocationPublish.class);
        testsuites.add(JEEMultiLocationPublish2.class);
        testsuites.add(Issue303Test1.class);
        testsuites.add(Issue303Test2.class);
        testsuites.add(SharedLibTest2.class);
        testsuites.add(SharedLibTest2B.class);
        testsuites.add(SharedLibTest2C.class);
        return testsuites;
    }
}