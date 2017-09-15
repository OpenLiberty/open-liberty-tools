/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ibm.ws.st.core.tests.jee.JEEPublishEarDD;
import com.ibm.ws.st.core.tests.jee.JEEPublishWarNoDD;
import com.ibm.ws.st.core.tests.jee.WebFragment_DDWEBTest;
import com.ibm.ws.st.core.tests.jee.WebFragment_EARTest;
import com.ibm.ws.st.core.tests.samples.AllSampleTests;

// Run tests with the following properties
//    -Dwas.runtime.liberty.enableJava2Security=true
//    -Dwas.runtime.liberty=<runtime location>
@RunWith(Suite.class)
@Suite.SuiteClasses({ ExistenceTest.class,
                      WebSphereServerPublishWIncludeTest.class,
                      WebSphereServerTestCase.class,
                      UtilitiesTestCase.class,
                      WebFragment_EARTest.class,
                      WebFragment_DDWEBTest.class,
                      JEEPublishWarNoDD.class,
                      JEEPublishEarDD.class,
                      AllSampleTests.class
})
public class Java2SecurityTests {
    // Intentionally empty
}