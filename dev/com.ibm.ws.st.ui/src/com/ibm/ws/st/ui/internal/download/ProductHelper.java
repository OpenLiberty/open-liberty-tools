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
package com.ibm.ws.st.ui.internal.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * A helper class
 */
public class ProductHelper {

    static private final String LIBERTY_PRODUCT_ID = "com.ibm.websphere.appserver";

    public static IStatus isApplicableTo(IProduct product, IRuntimeInfo runtime) {
        if (runtime == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorRuntimeRequired, null);
        }

        if (product.getType() == IProduct.Type.IFIX) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorAddonNotSupported, null);
        }

        return ProductMatcher.match(product, runtime);
    }

    static List<String> processProvideFeature(String provideFeature) {
        if (provideFeature == null)
            return Collections.emptyList();

        String[] features = provideFeature.split(",");
        List<String> list = new ArrayList<String>(features.length);
        for (String f : features) {
            list.add(f.replaceAll("\\s", ""));
        }
        return list;
    }

    static boolean isCore(IProduct product) {
        if (product == null) {
            return false;
        }

        return IProduct.Type.INSTALL == product.getType() && LIBERTY_PRODUCT_ID.equals(product.getRuntimeInfo().getPrimaryProductId());
    }
}
