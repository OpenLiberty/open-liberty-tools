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

import org.eclipse.debug.core.ILaunchManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 * This test simulates getting new names when the docker server is created via the New Server Wizard with
 * loose config enabled.
 */
@TestCaseDescriptor(description = "Test getting new container names simulate from new server wizard", isStable = true)
public class GetNewNamesNewServerWizardOptionsTest extends GetNewNames {

    /**
     *
     */
    public GetNewNamesNewServerWizardOptionsTest() {
        // Empty
    }

    @BeforeClass
    public static void printHeader() {
        System.out.println("=====================================================================================================");
        System.out.println("Tests with new server wizard options set.");
        System.out.println("=====================================================================================================");
    }

    @Before
    public void setInitialOptions() {
        mode = ILaunchManager.RUN_MODE;
        isLooseConfigEnabled = true;
        isUserContainer = true;
        includeMode = false;
    }

    @Test
    public void test001() throws Exception {
        System.out.println("@Test - Current container with simple name. No name conflicts.");

        // Original container that is running when the server was first created
        String origContainerName = "wlp";
        String expectedContainerName = "wlp_dev";
        String expectedImageName = "wlp_dev_";

        doNewNameTest(origContainerName, expectedContainerName, expectedImageName);
    }

    @Test
    public void test002() throws Exception {
        System.out.println("@Test - Current container with simple name. One dev name conflict");
        // Add "existing" name that is in conflict with the initial proposed dev container name.
        existingContainers.add("wlp_dev");
        doNewNameTest("wlp", "wlp-1_dev", "wlp-1_dev_");
    }

    @Test
    public void test003() throws Exception {
        System.out.println("@Test - Current container with simple name. Two dev name conflicts");
        // Add "existing" name that is in conflict with the initial proposed dev container name.
        existingContainers.add("wlp_dev");
        existingContainers.add("wlp-1_dev");
        doNewNameTest("wlp", "wlp-2_dev", "wlp-2_dev_");
    }

    @Test
    public void test004() throws Exception {
        System.out.println("@Test - Current container with name ending with a digit.  No dev name conflicts.");
        // Add "existing" name that is in conflict with the initial proposed dev container name.
        existingContainers.add("foo");
        doNewNameTest("wlp1", "wlp1_dev", "wlp1_dev_");
    }

    @Test
    public void test005() throws Exception {
        System.out.println("@Test - Current container with name ending with a digit.  One dev name conflict.");
        // Add "existing" name that is in conflict with the initial proposed dev container name.
        existingContainers.add("wlp1_dev");
        doNewNameTest("wlp1", "wlp1-1_dev", "wlp1-1_dev_");
    }

    @Test
    public void test006() throws Exception {
        System.out.println("@Test - Current container with name ending with a digit.  Two dev name conflicts.");
        // Add "existing" name that is in conflict with the initial proposed dev container name.
        existingContainers.add("wlp1_dev");
        existingContainers.add("wlp1-1_dev");
        doNewNameTest("wlp1", "wlp1-2_dev", "wlp1-2_dev_");
    }

    @Test
    public void test007() throws Exception {
        System.out.println("@Test - Current container with base name ending with -#.  No dev name conflicts.");
        // Add "existing" names that are in conflict with the first two proposed dev container names
        existingContainers.add("foo");
        // Here the non-user container is used as a 'user-container' to create another docker server
        // The proposed container name is wlp_dev which is appropriate
        doNewNameTest("wlp-1_dev", "wlp_dev", "wlp_dev_");
    }

    @Test
    public void test008() throws Exception {
        System.out.println("@Test - Current container with name ending with -#.  Two dev name conflicts.");
        // Add "existing" names that are in conflict with the first two proposed dev container names
        existingContainers.add("wlp_dev");
        existingContainers.add("wlp-1_dev");
        doNewNameTest("wlp-99", "wlp-2_dev", "wlp-2_dev_");
    }
}
