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
package com.ibm.ws.st.core.internal.repository;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.ws.st.core.internal.Messages;

/**
 * An install product. It could represent a core runtime, a feature
 * a product sample, an iFix, etc.
 */
public interface IProduct {

    static final String PROP_NAME = "name";
    static final String PROP_DESCRIPTION = "description";
    static final String PROP_TYPE = "type";
    static final String PROP_PRODUCT_ID = "productId";
    static final String PROP_PRODUCT_VERSION = "productVersion";
    static final String PROP_PRODUCT_INSTALL_TYPE = "productInstallType";
    static final String PROP_PRODUCT_EDITION = "productEdition";
    static final String PROP_PRODUCT_LICENSE_TYPE = "licenseType";
    static final String PROP_APPLIES_TO = "appliesTo";
    static final String PROP_ON_PREMISE = "onPremise";
    static final String PROP_DISPLAY_TYPE = "displayType";

    enum Type {
        INSTALL(Messages.productLabel),
        EXTENDED(Messages.extendedProductLabel),
        FEATURE(Messages.featureLabel),
        SAMPLE(Messages.sampleLabel),
        OPEN_SOURCE(Messages.openSourceLabel),
        CONFIG_SNIPPET(Messages.configSnippetLabel),
        IFIX(Messages.iFixLabel),
        UNKNOWN(null);

        private final String label;

        private Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum ProductType {
        MASSIVE_TYPE,
        LOCAL_TYPE
    }

    /**
     * Return the product's name
     *
     * @return name
     */
    String getName();

    /**
     * Return the product's description
     *
     * @return description
     */
    String getDescription();

    /**
     * Return the product's type
     *
     * @return type
     */
    Type getType();

    /**
     * Return the product type
     *
     * @return type
     */
    ProductType getProductType();

    /**
     * Return the product's download size
     *
     * @return size
     */
    long getSize();

    /**
     * Return the license information
     *
     * @param monitor
     * @return license
     * @throws IOException
     */
    License getLicense(IProgressMonitor monitor) throws IOException;

    /**
     * Return the name of the site the product belongs to
     *
     * @return site name
     */
    String getSiteName();

    /**
     * Return the value of a given property
     */
    String getAttribute(String name);

    /**
     * Return the list of provided features, if any.
     *
     * @return feature list
     */
    List<String> getProvideFeature();

    /**
     * Return the list of required features, if any.
     *
     * @return feature list
     */
    List<String> getRequireFeature();

    /**
     * Return information about the Liberty runtime. Only applies to
     * a product with a type == install.
     *
     * @return information about the liberty runtime
     */
    IRuntimeInfo getRuntimeInfo();

    /**
     * Return the source of the archive to install
     *
     * @return archive file's source
     */
    ISource getSource();

    /**
     * Return the product's main attachment SHA256 hash value.
     *
     * @return hash value
     */
    String getHashSHA256();

    /**
     * Return if this is an install only feature. Install only features
     * are installed if their required features are installed.
     *
     * @return true if this is an install only feature, false otherwise
     */
    boolean isInstallOnlyFeature();
}
