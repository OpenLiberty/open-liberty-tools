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
package com.ibm.ws.st.core.tests;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * A test to verify that JUnit can be run on the CCB machine
 */
@TestCaseDescriptor(description = "Heart beat tests", isStable = true)
public class Heartbeat {

    @Test
    public void heartBeat1() {
        Assert.assertTrue("Assert true", true);
        System.out.println("heatBeat1.");
    }

    @Test
    public void heartBeat2() {
        Assert.assertFalse("Assert false", false);
        System.out.println("heatBeat2.");
    }
}
