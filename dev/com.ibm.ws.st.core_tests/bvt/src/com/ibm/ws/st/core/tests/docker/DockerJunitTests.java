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
package com.ibm.ws.st.core.tests.docker;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Run as a JUnit test, not, JUnit Plug-in test
 *
 * These tests are JUnit 4 tests, and do not extend TestCase
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value = { GetNewNamesNewServerWizardOptionsTest.class,
                              GetNewNamesDisableLooseConfigCurrentDebugMode.class,
                              GetNewNamesDisableLooseConfigCurrentRunMode.class,
                              GetNewNamesEnableLooseConfigCurrentDebugMode.class,
                              GetNewNamesEnableLooseConfigCurrentRunMode.class
})
public class DockerJunitTests {

    /**
     *
     */
    public DockerJunitTests() {
        // Empty
    }

}
