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

import com.ibm.ws.st.core.tests.util.ServerTestUtil;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// Run tests with the following properties:
// -Dwas.runtime.liberty=<location of liberty runtime>
//
// The following property is set within the test suite but when running
// the tests individually, also set:
// -Dliberty.loosecfg=true
@RunWith(AllTests.class)
public class AllJEETests_LooseCfg {
    public static final String JDK_VERSION_7 = "1.7.0";

    public static TestSuite suite() {
        System.setProperty(ServerTestUtil.LOOSE_CFG_MODE_PROP, "true");
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
        testsuites.add(JEENoDD_DefaultTest.class);
        testsuites.add(JEE6DD_EARTest.class);
        testsuites.add(JEE6DD_WebTest.class);
        testsuites.add(JEEAddRemoveWebTest.class);
        testsuites.add(JEE6DDAddRemoveWebTest.class);
        testsuites.add(JEE_BinaryModule_Test.class);
        testsuites.add(WebFragment_WEBTest.class);
        testsuites.add(WebFragment_EARTest.class);
        testsuites.add(WebFragment_DDWEBTest.class);
        testsuites.add(WebFragment_DDEARTest.class);
        testsuites.add(Defect96054Test.class);
        testsuites.add(LinkedResourceTest.class);
        testsuites.add(LinkedResourceTest2.class);
        testsuites.add(JEEPublishWarNoDD.class);
        testsuites.add(JEEPublishWarDD.class);
        testsuites.add(JEEPublishEarNoDD.class);
        testsuites.add(JEEPublishEarDD.class);
        testsuites.add(JEEPublishEarRemap.class);
        testsuites.add(JEEPublishWarDeployName.class);
        testsuites.add(JEE6DD_WebTest_V85.class);
        testsuites.add(JEE6DD_EARTest_V85.class);
        testsuites.add(JEEMultiLocationPublish.class);
        testsuites.add(JEEMultiLocationPublish2.class);
        return testsuites;
    }
}