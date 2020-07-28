/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.featureDetection;

import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class AllFeatureTests {
    public static final String JDK_VERSION_7 = "1.7.0";

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        for (Class<? extends TestCase> testclass : addTestCases()) {
            suite.addTest(new JUnit4TestAdapter(testclass));
        }
        return suite;
    }

    private static ArrayList<Class<? extends TestCase>> addTestCases() {
        ArrayList<Class<? extends TestCase>> testsuites = new ArrayList<Class<? extends TestCase>>();
        // Add following tests only if JDK >= 1.7.0
        if (System.getProperty("java.version").compareTo(JDK_VERSION_7) >= 0) {
            testsuites.add(ClientOnlyFeatureTest.class);
            testsuites.add(AlternativeFeatureTest.class);
            testsuites.add(MixedFeatureTest.class);
            testsuites.add(ImportFeatureTest.class);
            testsuites.add(JakartaEE9FeatureTest.class);
        }
        return testsuites;
    }
}
