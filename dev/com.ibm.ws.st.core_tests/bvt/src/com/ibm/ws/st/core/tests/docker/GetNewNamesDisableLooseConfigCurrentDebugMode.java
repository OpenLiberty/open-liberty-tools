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

/**
 * This test simulates getting new names when loose config is disabled while in debug mode
 *
 * This test simulates getting new names when the docker server is restarted in debug mode and loose config is already turned off
 */
public class GetNewNamesDisableLooseConfigCurrentDebugMode extends GetNewNames {

    /**
     *
     */
    public GetNewNamesDisableLooseConfigCurrentDebugMode() {
        // Empty
    }

    @BeforeClass
    public static void printHeader() {
        System.out.println("=====================================================================================================");
        System.out.println("Tests getting new names when changing to non-loose config while in debug mode.");
        System.out.println("Tests getting new names when restarting the server in debug mode while loose config is turned off.");
        System.out.println("=====================================================================================================");
    }

    @Before
    public void setInitialOptions() {
        mode = ILaunchManager.DEBUG_MODE;
        isLooseConfigEnabled = false;
        isUserContainer = false;
        includeMode = true;
    }

    @Test
    public void test001() throws Exception {
        System.out.println("@Test - Current container with simple name. No name conflicts.");

        // Original container that is running when the server was first created
        String origContainerName = "wlp";
        String expectedContainerName = "wlp_debug";
        String expectedImageName = "wlp_debug_";

        doNewNameTest(origContainerName, expectedContainerName, expectedImageName);
    }

    @Test
    public void test002() throws Exception {
        System.out.println("@Test - Current container with simple name. One name conflict");
        // Add "existing" name that is in conflict with the initial proposed container name.
        existingContainers.add("wlp_debug");
        doNewNameTest("wlp", "wlp-1_debug", "wlp-1_debug_");
    }

    @Test
    public void test003() throws Exception {
        System.out.println("@Test - Current container with simple name. Two name conflicts");
        // Add "existing" name that is in conflict with the initial proposed container name.
        existingContainers.add("wlp_debug");
        existingContainers.add("wlp-1_debug");
        doNewNameTest("wlp", "wlp-2_debug", "wlp-2_debug_");
    }

    @Test
    public void test004() throws Exception {
        System.out.println("@Test - Current container with name ending with a digit.  No name conflicts.");
        // Add "existing" name that is in conflict with the initial proposed container name.
        existingContainers.add("foo");
        doNewNameTest("wlp1", "wlp1_debug", "wlp1_debug_");
    }

    @Test
    public void test005() throws Exception {
        System.out.println("@Test - Current container with name ending with a digit.  One name conflict.");
        // Add "existing" name that is in conflict with the initial proposed container name.
        existingContainers.add("wlp1_debug");
        doNewNameTest("wlp1", "wlp1-1_debug", "wlp1-1_debug_");
    }

    @Test
    public void test006() throws Exception {
        System.out.println("@Test - Current container with name ending with a digit.  Two name conflicts.");
        // Add "existing" name that is in conflict with the initial proposed container name.
        existingContainers.add("wlp1_debug");
        existingContainers.add("wlp1-1_debug");
        doNewNameTest("wlp1", "wlp1-2_debug", "wlp1-2_debug_");
    }

    @Test
    public void test007() throws Exception {
        System.out.println("@Test - Current container with base name ending with -#.  No name conflicts.");
        // Add "existing" names that are in conflict with the first two proposed container names
        existingContainers.add("foo");
        doNewNameTest("wlp-1", "wlp_debug", "wlp_debug_");
    }

    @Test
    public void test008() throws Exception {
        System.out.println("@Test - Current container with name ending with -#.  Two name conflicts.");
        // Add "existing" names that are in conflict with the first two proposed container names
        existingContainers.add("wlp_debug");
        existingContainers.add("wlp-1_debug");
        doNewNameTest("wlp-99", "wlp-2_debug", "wlp-2_debug_");
    }

    @Test
    public void test009() throws Exception {
        System.out.println("@Test - Current container with base name ending with -#.  No name conflicts.");
        // Add "existing" names that are in conflict with the first two proposed container names

        // In this case, the container with name wlp-1_run was used to create a docker server.  It is a user
        // container. The 'base' name is wlp (tools strip out -1 and _run suffixes)
        isUserContainer = true;
        existingContainers.add("foo");
        doNewNameTest("wlp-1_run", "wlp_debug", "wlp_debug_");
    }

    @Test
    public void test010() throws Exception {
        System.out.println("@Test - Current container with base name ending with -#.  One name conflict.");
        // Add "existing" names that are in conflict with the first two proposed container names
        isUserContainer = true;
        existingContainers.add("wlp_debug");
        doNewNameTest("wlp-1_dev_run", "wlp-1_debug", "wlp-1_debug_");
    }
}
