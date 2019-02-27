/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
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

import com.ibm.ws.st.core.tests.config.RecursiveIncludeTestCase;
import com.ibm.ws.st.core.tests.module.AllModuleTestCase;
import com.ibm.ws.st.core.tests.samples.AllSampleTests;
import com.ibm.ws.st.core.tests.schema.FeatureListTest;
import com.ibm.ws.st.core.tests.validation.BestMatchTest;
import com.ibm.ws.st.core.tests.validation.FeatureValidationTest;
import com.ibm.ws.st.core.tests.validation.IncludeValidationTest;
import com.ibm.ws.st.core.tests.validation.MergeRulesValidationTest;
import com.ibm.ws.st.core.tests.validation.MiscellaneousValidationTest;
import com.ibm.ws.st.core.tests.validation.QuickFixTestCase;
import com.ibm.ws.st.core.tests.validation.VariableDefaultValueTest;
import com.ibm.ws.st.core.tests.validation.VariableExpressionValidationTest;
import com.ibm.ws.st.core.tests.validation.VariableValidationTest;

// When running these tests, the following properties must be set:
// -Dwas.runtime.liberty=<runtime location>
@RunWith(Suite.class)
@Suite.SuiteClasses({ ExistenceTest.class,
                      BestMatchTest.class,
                      FeatureListTest.class,
                      RecursiveIncludeTestCase.class,
                      WebSphereServerPublishWIncludeTest.class,
                      WebSphereRuntimeTestCase.class,
                      MultipleRuntimeTestCase.class,
                      WebSphereServerTestCase.class,
                      AllModuleTestCase.class,
                      MiscellaneousValidationTest.class,
                      VariableExpressionValidationTest.class,
                      MergeRulesValidationTest.class,
                      VariableDefaultValueTest.class,
                      IncludeValidationTest.class,
                      VariableValidationTest.class,
                      FeatureValidationTest.class,
                      QuickFixTestCase.class,
                      UtilitiesTestCase.class,
                      OrderedServersTestCase.class,
                      RuntimeVersionCompareTest.class,
                      AllSampleTests.class
// removed temporarily until stabilized
//                     AllThirdPartyFeatureTests.class
})
public class AllSmokeTests {
    // Intentionally empty
}