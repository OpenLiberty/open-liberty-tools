/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.validation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
                      MiscellaneousValidationTest.class,
                      VariableExpressionValidationTest.class,
                      MergeRulesValidationTest.class,
                      IncludeValidationTest.class,
                      VariableValidationTest.class,
                      FeatureValidationTest.class,
                      QuickFixTestCase.class,
                      IncludeRevalidateTestCase.class,
                      BestMatchTest.class,
                      ConfigDropinsValidationTest.class,
                      // DropinRevalidateTestCase is failing because of eclipse bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=466749
//                      DropinRevalidateTestCase.class,
                      SSLNoKeystoreValidationTest.class })
public class AllValidationTests {
    // intentionally empty
}