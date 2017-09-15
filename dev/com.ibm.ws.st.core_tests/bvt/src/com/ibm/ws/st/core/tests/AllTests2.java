/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
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

import com.ibm.ws.st.core.tests.jee.AllJEETests;

// When running these tests, the following properties must be set:
// -Dwas.runtime.liberty=<runtime location>
@RunWith(Suite.class)
@Suite.SuiteClasses({ ExistenceTest.class,
                      AllJEETests.class })
public class AllTests2 {
    // intentionally empty
}