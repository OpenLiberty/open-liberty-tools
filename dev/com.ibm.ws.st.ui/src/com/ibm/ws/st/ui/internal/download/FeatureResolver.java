/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.ibm.ws.st.core.internal.repository.IProduct;

/**
 * A require feature resolver
 */
class FeatureResolver {

    protected static List<String> getAllRequireFeature(IProduct product, List<IProduct> availableProducts) {
        List<String> requireList = product.getRequireFeature();
        if (requireList.isEmpty())
            return Collections.emptyList();

        Stack<String> requireStack = new Stack<String>();
        requireStack.addAll(requireList);

        List<String> finalRequireList = new ArrayList<String>();

        while (!requireStack.isEmpty()) {
            String feature = requireStack.pop();
            if (finalRequireList.contains(feature))
                continue;

            finalRequireList.add(feature);
            List<String> reqList = getRequireFeature(feature, product, availableProducts);
            if (!reqList.isEmpty())
                requireStack.addAll(reqList);
        }

        return finalRequireList;
    }

    private static List<String> getRequireFeature(String feature, IProduct originalProduct, List<IProduct> availableProducts) {
        List<String> list = new ArrayList<String>();
        for (IProduct p : availableProducts) {
            if (p != originalProduct && p.getType() == IProduct.Type.FEATURE) {
                if (feature.equals(p.getProvideFeature().get(0))) {
                    list.addAll(p.getRequireFeature());
                }
            }
        }
        return list;
    }
}
