/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.docker;

import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.jee.JEEMultiLocationPublish;
import com.ibm.ws.st.core.tests.jee.JEEMultiLocationPublish2;
import com.ibm.ws.st.core.tests.jee.JEEPublishEarDDExt;
import com.ibm.ws.st.core.tests.jee.JEEPublishWarNoDDExt;
import com.ibm.ws.st.core.tests.samples.EJBSample;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// When running these tests, the following properties must be set:
// -Dwas.runtime.liberty=<runtime location>
// -Dliberty.servertype=LibertyDocker
// -Dliberty.loosecfg=true
//
// IMPORTANT: when running on Windows or OS X the workspace for the tests
// must be in the user home directory or another directory that is shared
// with Docker (this is because loose configuration uses Docker volumes)
@RunWith(AllTests.class)
public class AllDockerTests_LooseCfg {
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
        testsuites.add(EJBSample.class);
        testsuites.add(JEEPublishWarNoDDExt.class);
        testsuites.add(JEEPublishEarDDExt.class);
        testsuites.add(JEEMultiLocationPublish.class);
        testsuites.add(JEEMultiLocationPublish2.class);
        // JEEMultiLocationPublish3 is failing because of an IllegalArgumentException coming from the runtime
//        testsuites.add(JEEMultiLocationPublish3.class);
        return testsuites;
    }
}
