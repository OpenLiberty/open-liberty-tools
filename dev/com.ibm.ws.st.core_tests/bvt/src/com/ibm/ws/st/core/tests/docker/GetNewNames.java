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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.ILaunchManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility;

/**
 * Base abstract class for various scenarios to get new names for a container
 */
public abstract class GetNewNames {

    protected final String origImageName = "websphere-liberty:javaee7";
    // Existing containers and images to test logic that obtains a unique name
    protected final List<String> existingContainers = new ArrayList<String>();
    protected final List<String> existingImages = new ArrayList<String>();

    /** Set default options for automated tests (set to values used from new server wizard) */
    protected String mode = ILaunchManager.RUN_MODE;
    /** The new loose config setting (not the current) */
    protected boolean isLooseConfigEnabled = true;
    /** User container is true when the server is first created from the new server wizard. Can be either when changed from server editor. */
    protected boolean isUserContainer = true;
    /** The mode is not included in the container name ONLY when the server is first created */
    protected boolean includeMode = false;

    // Original container that is running when the server was first created
    protected String origContainerName = "wlp";

    /**
     *
     */
    public GetNewNames() {
        // Empty
    }

    @Before
    public void setUp() {
//        System.out.println("@Before called before @Test");z
        // Clear out 'used' names before each test
        existingContainers.clear();
        existingImages.clear();
        existingImages.add(origImageName);
        System.out.println("-----------------------------------------------------------------------------------------------------");
    }

    @After
    public void cleanUp() {
        //
    }

    protected void doNewNameTest(String origContainerName, String expectedContainerName, String expectedImageNamePrefix) throws Exception {

        String expectedImageName = expectedImageNamePrefix + origImageName;

        List<String> newNames = LibertyDockerRunUtility.getNewNames(mode, origImageName, origContainerName, isLooseConfigEnabled, isUserContainer, includeMode,
                                                                    existingContainers, existingImages);
        String newImageName = newNames.get(0); // First is the image name
        String newContainerName = newNames.get(1); // Second is the container name

        String containerNameValidateMessage = getContainerNameValidateMessage(expectedContainerName, newContainerName);

        System.out.println("Current container name=" + origContainerName);
        boolean testContainerName = newContainerName.equals(expectedContainerName);
        System.out.println((testContainerName ? "PASS:" : "**FAIL:") + containerNameValidateMessage);
        Assert.assertTrue(containerNameValidateMessage, testContainerName);

        boolean testImageName = newImageName.equals(expectedImageName);
        String imageNameValidateMessage = getImageNameValidateMessage(expectedImageName, newImageName);
        System.out.println((testImageName ? "PASS:" : "**FAIL:") + imageNameValidateMessage);
        Assert.assertTrue(imageNameValidateMessage, testImageName);
    }

    protected String getContainerNameValidateMessage(String expected, String actual) {
        return "Expected container name=" + expected + "\nActual=" + actual;
    }

    protected String getImageNameValidateMessage(String expected, String actual) {
        return "Expected image name=" + expected + "\nActual=" + actual;
    }

}
