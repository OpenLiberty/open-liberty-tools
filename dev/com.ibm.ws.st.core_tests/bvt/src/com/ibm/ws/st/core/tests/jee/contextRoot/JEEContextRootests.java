/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.contextRoot;

import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class JEEContextRootests {
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
        // Add following tests only if JDK >= 1.7.0
        if (System.getProperty("java.version").compareTo(JDK_VERSION_7) >= 0) {
            testsuites.add(JEEContextRoot_WAR_With_XML.class);
            testsuites.add(JEEContextRoot_WAR_Without_XML.class);
            testsuites.add(JEEContextRoot_WAR_ProjectSetting.class);
            testsuites.add(JEEContextRoot_EAR_CRinAppXML.class);
            testsuites.add(JEEContextRoot_EAR_NoDD_CRinWebXML.class);
            testsuites.add(JEEContextRoot_EAR_NoDD_defaultCR.class);
            testsuites.add(JEEContextRoot_EAR_NoDD_defaultCRwSpaces.class);
        }
        return testsuites;
    }
}
