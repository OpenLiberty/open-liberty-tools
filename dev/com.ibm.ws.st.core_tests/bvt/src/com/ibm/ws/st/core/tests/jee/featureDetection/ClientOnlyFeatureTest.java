/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee.featureDetection;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.internal.config.FeatureList;
import com.ibm.ws.st.core.internal.generation.Feature;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;

/**
 * Tests if client only features are added to the runtime feature list or not.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientOnlyFeatureTest extends ToolsTestBase {

    private static String javaEEClientFeature = "javaeeClient-7.0";

    @Test
    public void test01_doSetup() throws Exception {
        print("Starting test: ClientOnlyFeatureTest");
        init();
        createRuntime();
    }

    @Test
    public void test02_testClientOnlyFeature() {
        assertNotNull("Websphere runtime is null", getWebSphereRuntime());
        Map<String, Feature> allFeatures = FeatureList.getFeatureMap(getWebSphereRuntime());
        assertFalse("Found client only feature javaeeClient-7.0 in the generated feature list", allFeatures.containsKey(javaEEClientFeature));
    }

    public void test99_doTearDown() {
        WLPCommonUtil.cleanUp();
        wait("Ending test: ClientOnlyFeatureTest\n", 1000);
    }
}
