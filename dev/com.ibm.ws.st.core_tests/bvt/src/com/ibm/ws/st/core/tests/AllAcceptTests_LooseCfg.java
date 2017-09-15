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
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.jee.JEEPublishEarDD;
import com.ibm.ws.st.core.tests.jee.JEEPublishWarNoDD;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// When running these tests, the following properties must be set:
//    -Dwas.runtime.liberty=<runtime location>
//    -Dliberty.loosecfg=true
@RunWith(AllTests.class)
public class AllAcceptTests_LooseCfg {
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
        testsuites.add(JEEPublishWarNoDD.class);
        testsuites.add(JEEPublishEarDD.class);
        return testsuites;
    }
}