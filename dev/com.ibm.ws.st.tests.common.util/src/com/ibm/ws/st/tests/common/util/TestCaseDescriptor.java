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
package com.ibm.ws.st.tests.common.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide description on Test case for generating test case report.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestCaseDescriptor {
    /**
     * Get a brief description on the scenario covered by the test case
     * 
     * @return a brief description on the scenario covered by the test case
     */
    public String description();

    /**
     * Define if the test case is a stable test case that can run on the automated test framework.
     * 
     * @return true if the test case is a stable test case that can run on the automated test framework; otherwise, return false.
     */
    public boolean isStable();
}
