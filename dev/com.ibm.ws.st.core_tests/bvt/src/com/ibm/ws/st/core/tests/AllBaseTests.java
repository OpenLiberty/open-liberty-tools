/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import com.ibm.ws.st.core.tests.config.RecursiveIncludeTestCase;
import com.ibm.ws.st.core.tests.module.AllModuleTestCase;
import com.ibm.ws.st.core.tests.schema.FeatureListTest;
import com.ibm.ws.st.core.tests.schema.SchemaUtilTest;

// When running these tests, the following properties must be set:
// -Dwas.runtime.liberty=<runtime location>
@RunWith(Suite.class)
@Suite.SuiteClasses({ ExistenceTest.class,
                      SchemaUtilTest.class, // WebSphereServerTestCase, OSGiTestCase and SchemaTestCase cause this to fail
                      FeatureListTest.class,
                      RecursiveIncludeTestCase.class,
                      WebSphereRuntimeTestCase.class,
                      MultipleRuntimeTestCase.class,
                      AllModuleTestCase.class,
                      WebSphereServerTestCase.class,
                      UtilitiesTestCase.class,
                      OutOfSyncTest.class,
                      RuntimeVersionCompareTest.class })
public class AllBaseTests {
    // Intentionally empty
}