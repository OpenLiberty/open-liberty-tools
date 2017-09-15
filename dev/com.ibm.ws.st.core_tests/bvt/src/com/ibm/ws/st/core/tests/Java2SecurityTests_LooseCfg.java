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
package com.ibm.ws.st.core.tests;

import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ibm.ws.st.core.tests.jee.JEEPublishEarNoDD;
import com.ibm.ws.st.core.tests.jee.JEEPublishWarDD;
import com.ibm.ws.st.core.tests.jee.WebFragment_DDEARTest;
import com.ibm.ws.st.core.tests.jee.WebFragment_WEBTest;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// Run tests with the following properties
//    -Dwas.runtime.liberty.enableJava2Security=true
//    -Dwas.runtime.liberty=<runtime location>
//    -Dliberty.loosecfg=true
@RunWith(Suite.class)
public class Java2SecurityTests_LooseCfg {
    public static TestSuite suite() {
        System.setProperty(ServerTestUtil.LOOSE_CFG_MODE_PROP, "true");
        TestSuite suite = new TestSuite();
        for (Class<? extends TestCase> testclass : addTestCases()) {
            suite.addTest(new JUnit4TestAdapter(testclass));
        }
        return suite;
    }

    private static ArrayList<Class<? extends TestCase>> addTestCases() {
        ArrayList<Class<? extends TestCase>> testsuites = new ArrayList<Class<? extends TestCase>>();
        testsuites.add(WebFragment_WEBTest.class);
        testsuites.add(WebFragment_DDEARTest.class);
        testsuites.add(JEEPublishWarDD.class);
        testsuites.add(JEEPublishEarNoDD.class);
        return testsuites;
    }
}