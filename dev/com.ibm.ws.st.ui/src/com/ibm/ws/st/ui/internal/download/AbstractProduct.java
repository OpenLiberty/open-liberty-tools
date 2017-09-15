/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;

/**
 * An install product that stores its information in a map-based structure.
 */
public abstract class AbstractProduct implements IProduct {

    protected static final String PROP_PROVIDE_FEATURE = "provideFeature";
    protected static final String PROP_REQUIRE_FEATURE = "requireFeature";
    private static final Map<String, Type> TYPE_MAP = new HashMap<String, Type>();

    static {
        TYPE_MAP.put("install", Type.INSTALL);
        TYPE_MAP.put("feature", Type.FEATURE);
        TYPE_MAP.put("sample", Type.SAMPLE);
        TYPE_MAP.put("addon", Type.EXTENDED);
        TYPE_MAP.put("opensource", Type.OPEN_SOURCE);
        TYPE_MAP.put("ifix", Type.IFIX);
    }

    private static Type getTypeFor(String name) {
        Type type = TYPE_MAP.get(name);
        return type != null ? type : Type.UNKNOWN;
    }

    protected final Map<String, String> properties;
    private List<String> provideFeature;
    private List<String> requireFeature;

    AbstractProduct(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String getName() {
        return properties.get(PROP_NAME);
    }

    @Override
    public String getDescription() {
        return properties.get(PROP_DESCRIPTION);
    }

    @Override
    public IProduct.Type getType() {
        return getTypeFor(properties.get(PROP_TYPE));
    }

    @Override
    public List<String> getProvideFeature() {
        if (provideFeature == null)
            provideFeature = processFeatureProperty(properties.get(PROP_PROVIDE_FEATURE));

        return provideFeature;
    }

    @Override
    public List<String> getRequireFeature() {
        if (requireFeature == null)
            requireFeature = processFeatureProperty(properties.get(PROP_REQUIRE_FEATURE));

        return requireFeature;
    }

    @Override
    public String getAttribute(String property) {
        return properties.get(property);
    }

    @Override
    public IRuntimeInfo getRuntimeInfo() {
        if (getType() != IProduct.Type.INSTALL)
            return null;

        return new IRuntimeInfo() {
            @Override
            public String getProductId() {
                return properties.get(PROP_PRODUCT_ID);
            }

            @Override
            public String getProductVersion() {
                return properties.get(PROP_PRODUCT_VERSION);
            }

            @Override
            public String getProductEdition() {
                return properties.get(PROP_PRODUCT_EDITION);
            }

            @Override
            public String getProductInstallType() {
                return properties.get(PROP_PRODUCT_INSTALL_TYPE);
            }

            @Override
            public List<String> getInstalledFeatures() {
                return Collections.emptyList();
            }

            @Override
            public IPath getLocation() {
                return null;
            }

            @Override
            public boolean isOnPremiseSupported() {
                return "true".equals(properties.get(PROP_ON_PREMISE));
            }

            @Override
            public String getProductLicenseType() {
                String licenseType = properties.get(PROP_PRODUCT_LICENSE_TYPE);
                return licenseType == null ? "ILAN" : licenseType;
            }
        };
    }

    private List<String> processFeatureProperty(String featureProp) {
        if (featureProp == null || featureProp.isEmpty())
            return Collections.emptyList();

        String[] features = featureProp.split(",");
        List<String> list = new ArrayList<String>(features.length);
        for (String f : features) {
            // A feature might contain a visibility attribute which is
            // preceded by a ';'.
            String feature = f.replaceAll("\\s", "");
            int index = feature.indexOf(';');
            if (index == -1)
                list.add(feature);
            else {
                String featureAtts = feature.substring(index + 1, feature.length());
                if (featureAtts.indexOf("visibility:=") == -1 || featureAtts.contains("visibility:=public"))
                    list.add(feature.substring(0, index));
            }
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public String getHashSHA256() {
        return null;
    }

    @Override
    public boolean isInstallOnlyFeature() {
        return false;
    }

}
