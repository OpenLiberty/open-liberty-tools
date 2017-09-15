/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.ibm.ws.st.core.tests.config.ConfigTestCase;
import com.ibm.ws.st.core.tests.config.IncludeTestCase;
import com.ibm.ws.st.core.tests.config.MergedConfigTest;
import com.ibm.ws.st.core.tests.jee.featureDetection.AllFeatureTests;
import com.ibm.ws.st.core.tests.schema.SchemaUtilTest;
import com.ibm.ws.st.core.tests.userDir.ExternalUserDirTest;
import com.ibm.ws.st.core.tests.userDir.SameNameServerTest;
import com.ibm.ws.st.core.tests.validation.ConfigDropinsValidationTest;
import com.ibm.ws.st.core.tests.validation.IncludeRevalidateTestCase;
import com.ibm.ws.st.core.tests.validation.SSLNoKeystoreValidationTest;

// When running these tests, the following properties must be set:
// -Dwas.runtime.liberty=<runtime location>
@RunWith(Suite.class)
@Suite.SuiteClasses({ ExistenceTest.class,
                      SchemaUtilTest.class,
                      WebSphereRuntimeV85TestCase.class,
                      WebSphereServerV85TestCase.class,
                      IncludeRevalidateTestCase.class,
                      ConfigDropinsValidationTest.class,
                      // DropinRevalidateTestCase is failing because of eclipse bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=466749
//                      DropinRevalidateTestCase.class,
                      SSLNoKeystoreValidationTest.class,
                      BluemixLinkTest.class,
                      UpdateConfigTest.class,
                      MergedConfigTest.class,
                      ConsoleTrackerTestCase.class,
                      ExternalUserDirTest.class,
                      SameNameServerTest.class,
                      AllFeatureTests.class,
                      ConfigTestCase.class,
                      IncludeTestCase.class,
                      OutOfSyncTest.class,
})
public class AllTests1 {
    // intentionally empty
}