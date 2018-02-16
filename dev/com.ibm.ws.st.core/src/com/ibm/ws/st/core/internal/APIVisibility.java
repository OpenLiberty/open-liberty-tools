/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.EnumSet;
import java.util.Properties;

public enum APIVisibility {

    API,
    IBM_API,
    SPEC,
    STABLE,
    THIRD_PARTY;

    public static EnumSet<APIVisibility> getDefaults() {
        return EnumSet.of(API, IBM_API, SPEC, STABLE);
    }

    public static EnumSet<APIVisibility> parseFromAttribute(String value) {
        EnumSet<APIVisibility> apiVisibility = EnumSet.noneOf(APIVisibility.class);
        if (value != null && value.length() > 0) {
            String[] values = value.split(",");
            for (int i = 0; i < values.length; i++) {
                if (values[i].trim().toLowerCase().equals(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_API)) {
                    apiVisibility.add(API);
                }
                if (values[i].trim().toLowerCase().equals(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_IBM_API)) {
                    apiVisibility.add(IBM_API);
                }
                if (values[i].trim().toLowerCase().equals(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_SPEC)) {
                    apiVisibility.add(SPEC);
                }
                if (values[i].trim().toLowerCase().equals(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_STABLE)) {
                    apiVisibility.add(STABLE);
                }
                if (values[i].trim().toLowerCase().equals(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_THIRD_PARTY)) {
                    apiVisibility.add(THIRD_PARTY);
                }
            }
        }
        return apiVisibility;
    }

    public static String generateAttributeValue(EnumSet<APIVisibility> apiVisibility) {
        StringBuilder stringBuilder = new StringBuilder();
        if (apiVisibility.contains(API)) {
            stringBuilder.append(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_API);
        }
        if (apiVisibility.contains(IBM_API)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_IBM_API);
        }
        if (apiVisibility.contains(SPEC)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_SPEC);
        }
        if (apiVisibility.contains(STABLE)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_STABLE);
        }
        if (apiVisibility.contains(THIRD_PARTY)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(Constants.API_VISIBILITY_ATTRIBUTE_VALUE_THIRD_PARTY);
        }
        return stringBuilder.toString();
    }

    public static EnumSet<APIVisibility> getAPIVisibilityFromProperties(Properties properties) {
        EnumSet<APIVisibility> apiVisibility = EnumSet.noneOf(APIVisibility.class);
        if (Boolean.parseBoolean(properties.getProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_API_KEY))) {
            apiVisibility.add(APIVisibility.API);
        }
        if (Boolean.parseBoolean(properties.getProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_IBM_API_KEY))) {
            apiVisibility.add(APIVisibility.IBM_API);
        }
        if (Boolean.parseBoolean(properties.getProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_SPEC_KEY))) {
            apiVisibility.add(APIVisibility.SPEC);
        }
        if (Boolean.parseBoolean(properties.getProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_STABLE_KEY))) {
            apiVisibility.add(APIVisibility.STABLE);
        }
        if (Boolean.parseBoolean(properties.getProperty(Constants.SHARED_LIBRARY_SETTING_API_VISIBILITY_THIRD_PARTY_KEY))) {
            apiVisibility.add(APIVisibility.THIRD_PARTY);
        }
        if (apiVisibility.size() == 0) {
            apiVisibility = APIVisibility.getDefaults();
        }
        return apiVisibility;
    }

}
