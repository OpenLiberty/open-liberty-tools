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
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * A product matcher
 */
class ProductMatcher {
    private static final Pattern validNumericVersionOrRange = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)\\+?$");

    private static class ProductInfo {
        String id;
        String version;
        String installType;
        List<String> editions = new ArrayList<String>();

        public ProductInfo() {
            // no op
        }
    }

    static List<ProductInfo> processApplicableProducts(String appliesTo) {
        if (appliesTo == null || appliesTo.isEmpty()) {
            return Collections.emptyList();
        }

        ProductInfo info = new ProductInfo();
        List<ProductInfo> list = new ArrayList<ProductInfo>();
        boolean quoted = false;
        int index = 0;
        for (int i = 0; i < appliesTo.length(); i++) {
            char c = appliesTo.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if (!quoted) {
                if (c == ',') {
                    add(info, appliesTo.substring(index, i).trim());
                    index = i + 1;
                    list.add(info);

                    // reset the current before we process the next applicable
                    // product information.
                    info = new ProductInfo();
                } else if (c == ';') {
                    add(info, appliesTo.substring(index, i).trim());
                    index = i + 1;
                }
            }
        }

        add(info, appliesTo.substring(index).trim());
        list.add(info);

        return list;
    }

    private static void add(ProductInfo productInfo, String trimmedString) {
        if (trimmedString.isEmpty()) {
            return;
        }

        if (productInfo.id == null) {
            productInfo.id = trimmedString;
        } else if (trimmedString.startsWith("productVersion")) {
            productInfo.version = getValue(trimmedString);
        } else if (trimmedString.startsWith("productEdition")) {
            String editionStr = getValue(trimmedString);
            for (int startIndex = 0, endIndex = editionStr.indexOf(',');; startIndex = endIndex, endIndex = editionStr.indexOf(',', ++startIndex)) {
                productInfo.editions.add(editionStr.substring(startIndex, endIndex == -1 ? editionStr.length() : endIndex));
                if (endIndex == -1) {
                    break;
                }
            }
        } else if (trimmedString.startsWith("productInstallType")) {
            productInfo.installType = getValue(trimmedString);
        }
    }

    private static String getValue(String substring) {
        int index = substring.indexOf('=');
        String substring1 = substring.substring(index + 1).trim();
        if (substring1.charAt(0) == '"') {
            return substring1.substring(1, substring1.length() - 1);
        }
        return substring1;
    }

    static IStatus match(IProduct product, IRuntimeInfo runtime) {
        List<ProductInfo> appliesToList = processApplicableProducts(product.getAttribute(IProduct.PROP_APPLIES_TO));
        if (appliesToList == null || appliesToList.isEmpty()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorAddonInvalid, null);
        }

        IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorRuntimeNoProductInfo, null);
        List<IRuntimeInfo.IProduct> runtimeProducts = runtime.getProducts();
        if (runtimeProducts != null) {
            for (IRuntimeInfo.IProduct runtimeProduct : runtimeProducts) {
                for (ProductInfo appliesTo : appliesToList) {
                    status = matches(appliesTo, runtimeProduct);
                    if (status == Status.OK_STATUS) {
                        return status;
                    }
                }
            }
        }

        return status;
    }

    private static IStatus matches(ProductInfo appliesTo, IRuntimeInfo.IProduct runtimeProduct) {
        if (appliesTo.id == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorAddonNoProductId, null);
        }

        if (!appliesTo.id.equals(runtimeProduct.getProductId())) {
            String message;
            if (runtimeProduct.getProductId() == null || runtimeProduct.getProductId().isEmpty()) {
                message = Messages.errorRuntimeNoProductId;
            } else {
                message = NLS.bind(Messages.errorAddonProdcutIdMismatch, new String[] { runtimeProduct.getProductId(), appliesTo.id });
            }
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, null);
        }

        String productVersion = runtimeProduct.getProductVersion();
        if (appliesTo.version != null) {
            java.util.regex.Matcher appliesToMatcher = validNumericVersionOrRange.matcher(appliesTo.version);
            boolean appliesToIsRange = appliesTo.version.endsWith("+");
            java.util.regex.Matcher productVersionMatcher = validNumericVersionOrRange.matcher(productVersion);

            if (appliesToMatcher.matches() && appliesToIsRange && productVersionMatcher.matches()) {
                //Do a range check if the applies to is a numeric range n.n.n.n+, and the target product version is a numeric version n.n.n.n
                int[] minAppliesToVersion = allMatcherGroupsToIntArray(appliesToMatcher);
                int[] targetProductVersion = allMatcherGroupsToIntArray(productVersionMatcher);

                if (!versionSatisfiesMinimum(minAppliesToVersion, targetProductVersion)) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorAddonVersionMismatch, new String[] { productVersion, appliesTo.version }), null);
                }
            } else {
                //If appliesTo version doesn't end in +, or target product version is non-numeric, require String.equals()
                if (!appliesTo.version.equals(productVersion)) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorAddonVersionMismatch, new String[] { productVersion, appliesTo.version }), null);
                }
            }
        } else {
            // Older versions of the runtime do not support add-ons
            if (productVersion != null && productVersion.startsWith("8.5.0")) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorAddonNotSupported, productVersion), null);
            }
        }

        if (!appliesTo.editions.isEmpty() && !appliesTo.editions.contains(runtimeProduct.getProductEdition())) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorAddonEditionMismatch, new String[] { runtimeProduct.getProductEdition(),
                                                                                                                              appliesTo.editions.toString() }), null);
        }

        if (appliesTo.installType != null && !appliesTo.installType.equals(runtimeProduct.getProductInstallType())) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorAddonInstallTypeMismatch,
                                                                           new String[] { runtimeProduct.getProductInstallType(), appliesTo.installType }), null);
        }

        return Status.OK_STATUS;
    }

    private static int[] allMatcherGroupsToIntArray(java.util.regex.Matcher matcher) {
        int numGroups = matcher.groupCount();
        int[] digits = new int[numGroups];
        for (int i = 0; i < numGroups; i++) {
            digits[i] = Integer.parseInt(matcher.group(i + 1));
        }
        return digits;
    }

    /**
     * Evaluate whether queryVersion is considered to be greater or equal than minimumVersion.
     *
     * Expects version numbers in array form, where the elements of the array are the digits of the version
     * number, in order.
     *
     * Returns true if the version represented by queryVersion is greater than or equal to the
     * version represented by minimumVersion.
     *
     * Returns false if the version numbers are not of the same length, or the minimum version is not satisfied.
     *
     * @param minimumVersion
     * @param queryVersion
     * @return
     */
    private static boolean versionSatisfiesMinimum(int[] minimumVersion, int[] queryVersion) {
        if (minimumVersion.length == queryVersion.length) {
            for (int i = 0; i < minimumVersion.length; i++) {
                //Start at most significant digit
                if (queryVersion[i] < minimumVersion[i]) {
                    //This is too small, so fail. We're moving in from the most significant end, so no point in continuing
                    return false;
                } else if (queryVersion[i] == minimumVersion[i]) {
                    //This one is the same, so we must continue and check the other digits.
                    continue;
                } else if (queryVersion[i] > minimumVersion[i]) {
                    //This one is bigger. We're moving in from the most significant end, so the other bits don't matter now.
                    return true;
                }
            }
            //We got to the end without breaking any rules, so it is valid.
            return true;
        }
        return false;
    }
}
