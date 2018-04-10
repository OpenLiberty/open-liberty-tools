/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.repository;

import java.util.List;

import org.eclipse.core.runtime.IPath;

/**
 * Information that represents a Liberty runtime environment
 */
public interface IRuntimeInfo {

    public List<IProduct> getProducts();

    public String getVersion();

    public String getPrimaryProductId();

    public List<String> getInstalledFeatures();

    public IPath getLocation();

    public boolean isOnPremiseSupported();

    public interface IProduct {
        public String getProductId();

        public String getProductVersion();

        public String getProductEdition();

        public String getProductInstallType();

        public String getProductLicenseType();
    }
}
