/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import com.ibm.ws.st.core.internal.FeatureSet;
import com.ibm.ws.st.core.internal.WebSphereRuntime;

public class FeatureUtil {
    public static void verifyFeatures(WebSphereRuntime wr, String[] expectedFeatures, List<String> currentFeatures) {
        FeatureSet fs = wr.getInstalledFeatures();
        FeatureSet currentfs = new FeatureSet(wr, currentFeatures);
        boolean fail = false;
        for (String expected : expectedFeatures) {
            // find the supported feature (note that features are case insensitive)
            String featureToLookFor = fs.resolve(expected);
            String featureToLookFor2 = com.ibm.ws.st.core.internal.FeatureUtil.findLowerVersion(currentFeatures, featureToLookFor);
            if (featureToLookFor2 != null)
                featureToLookFor = featureToLookFor2;

            if (!currentfs.supports(featureToLookFor)) {
                WLPCommonUtil.print("Feature not supported by server: " + expected + "\n");
                fail = true;
            }
        }
        if (fail)
            Assert.fail("Feature(s) were not correctly added to the configuration, expected: " + Arrays.toString(expectedFeatures) + ", actual: " + currentFeatures);
    }

    public static void verifyFeatureNotEnabled(WebSphereRuntime wr, String unexpected, List<String> currentFeatures) {
        FeatureSet fs = wr.getInstalledFeatures();
        String featureToLookFor = fs.resolve(unexpected);
        for (String feature : currentFeatures) {
            if (featureToLookFor.equalsIgnoreCase(feature)) {
                WLPCommonUtil.print("Feature enabled when it should not be: " + feature + "\n");
                Assert.fail("Feature enabled when it should not be, feature: " + feature + ", enabled set: " + currentFeatures);
            }
        }
    }

}