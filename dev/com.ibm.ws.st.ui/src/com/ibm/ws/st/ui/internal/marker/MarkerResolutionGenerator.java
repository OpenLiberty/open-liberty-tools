/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import com.ibm.ws.st.core.internal.config.validation.AbstractConfigurationValidator;

/**
 * Resolution generator for configuration file validation problems.
 */
public class MarkerResolutionGenerator implements IMarkerResolutionGenerator2 {
    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        int fixType = marker.getAttribute(
                                          AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR,
                                          AbstractConfigurationValidator.QuickFixType.NONE.ordinal());
        switch (AbstractConfigurationValidator.QuickFixType.values()[fixType]) {
            case PLAIN_TEXT_PASSWORD:
                return new IMarkerResolution[] { new QuickFixPlainTextPassword() };
            case UNRECOGNIZED_PROPERTY:
                return getPropertyResolutions(marker);
            case UNRECOGNIZED_ELEMENT:
                return getElementBestMatchResolutions(marker);
            case UNAVAILABLE_ELEMENT:
                return getElementFeatureResolutions(marker);
            case UNDEFINED_VARIABLE:
                return getUndefinedVariableResolutions(marker);
            case UNRECOGNIZED_FEATURE:
                return getFeatureResolutions(marker);
            case OUT_OF_SYNC_APP:
                return getOutOfSyncAppResolutions(marker);
            case OUT_OF_SYNC_SHARED_LIB_REF_MISMATCH:
                return getOutOfSyncMissingSharedLibRefResolutions(marker);
            case FACTORY_ID_NOT_FOUND:
                return getFactoryIdResolutions(marker);
            case DUPLICATE_FACTORY_ID:
                return new IMarkerResolution[] { new QuickFixDuplicateFactoryRef(
                                marker.getAttribute(AbstractConfigurationValidator.REFERENCE_NAME, ""),
                                marker.getAttribute(AbstractConfigurationValidator.REFERENCE_OFFSET, -1)) };
            case INVALID_WHITESPACE:
                return new IMarkerResolution[] { new QuickFixRemoveWhitespace() };
            case SUPERSEDED_FEATURE:
                return new IMarkerResolution[] { new QuickFixSupersedeFeature(marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "")) };
            case FEATURE_CONFLICT:
                return new IMarkerResolution[] { new QuickFixFeatureConflict(marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "")) };
            case SSL_NO_KEYSTORE:
                return new IMarkerResolution[] { new QuickFixAddSecurityElements() };
            case REMOTE_SERVER_SECURE_PORT_MISMATCH:
                return new IMarkerResolution[] { new QuickFixAddSecurePortElement(), new QuickFixUpdateSecurePort() };
        }
        return null;
    }

    private IMarkerResolution[] getElementBestMatchResolutions(IMarker marker) {
        final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
        final String[] matches = bestMatch.split(",");
        final int len = matches.length;
        final IMarkerResolution[] resolutions = new IMarkerResolution[len];
        for (int i = 0; i < len; i++) {
            resolutions[i] = new QuickFixBestMatchElement(matches[i]);
        }
        return resolutions;
    }

    private IMarkerResolution[] getElementFeatureResolutions(IMarker marker) {
        final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
        final String[] matches = bestMatch.split(",");
        final int len = matches.length;
        final IMarkerResolution[] resolutions = new IMarkerResolution[len];
        for (int i = 0; i < len; i++) {
            resolutions[i] = new QuickFixAddFeature(matches[i]);
        }
        return resolutions;
    }

    private IMarkerResolution[] getPropertyResolutions(IMarker marker) {
        final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
        final String elemName = marker.getAttribute(AbstractConfigurationValidator.ELEMENT_NODE_NAME, "");
        final String attrName = marker.getAttribute(AbstractConfigurationValidator.ATTRIBUTE_NODE_NAME, "");

        if (bestMatch.length() == 0) {
            return new IMarkerResolution[] { new QuickFixIgnoreProperty(elemName, attrName),
                                            new QuickFixIgnoreAllProperties(elemName),
                                            new QuickFixIgnoreAllProperties() };
        }
        final String[] matches = bestMatch.split(",");
        final int len = matches.length;
        final IMarkerResolution[] resolutions = new IMarkerResolution[len + 3];
        int index = 0;
        for (String match : matches) {
            resolutions[index++] = new QuickFixBestMatchProperty(match);
        }
        resolutions[len] = new QuickFixIgnoreProperty(elemName, attrName);
        resolutions[len + 1] = new QuickFixIgnoreAllProperties(elemName);
        resolutions[len + 2] = new QuickFixIgnoreAllProperties();
        return resolutions;
    }

    private IMarkerResolution[] getUndefinedVariableResolutions(IMarker marker) {
        final String name = marker.getAttribute(AbstractConfigurationValidator.REFERENCE_NAME, "");
        final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
        final int referenceOffset = marker.getAttribute(AbstractConfigurationValidator.REFERENCE_OFFSET, 0);

        if (bestMatch.length() == 0 || referenceOffset <= 0) {
            return new IMarkerResolution[] { new QuickFixCreateVariable(name) };
        }

        final String[] matches = bestMatch.split(",");
        final IMarkerResolution[] resolutions = new IMarkerResolution[matches.length + 1];
        int index = 1;

        resolutions[0] = new QuickFixCreateVariable(name);
        for (String match : matches) {
            resolutions[index++] = new QuickFixBestMatchVariable(name, match);
        }

        return resolutions;
    }

    private IMarkerResolution[] getFeatureResolutions(IMarker marker) {
        final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
        final String[] matches = bestMatch.split(",");
        final int len = matches.length;
        final IMarkerResolution[] resolutions = new IMarkerResolution[len];
        int index = 0;
        for (String match : matches) {
            resolutions[index++] = new QuickFixBestMatchFeature(match);
        }
        return resolutions;
    }

    private IMarkerResolution[] getOutOfSyncAppResolutions(IMarker marker) {
        final String appName = marker.getAttribute(AbstractConfigurationValidator.APPLICATION_NAME, "");
        return new IMarkerResolution[] { new QuickFixAddApplicationElement(appName),
                                        new QuickFixRemoveServerApplication(appName) };
    }

    private IMarkerResolution[] getOutOfSyncMissingSharedLibRefResolutions(IMarker marker) {
        final String appName = marker.getAttribute(AbstractConfigurationValidator.APPLICATION_NAME, "");
        return new IMarkerResolution[] { new QuickFixUpdateSharedLibRef(appName) };
    }

    private IMarkerResolution[] getFactoryIdResolutions(IMarker marker) {
        final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
        final String[] matches = bestMatch.split(",");
        final int len = matches.length;
        final IMarkerResolution[] resolutions = new IMarkerResolution[len];
        int index = 0;
        for (String match : matches) {
            resolutions[index++] = new QuickFixBestMatchFactoryRef(match,
                            marker.getAttribute(AbstractConfigurationValidator.REFERENCE_NAME, ""),
                            marker.getAttribute(AbstractConfigurationValidator.REFERENCE_OFFSET, -1));
        }
        return resolutions;
    }

    @Override
    public boolean hasResolutions(IMarker marker) {
        int fixType = marker.getAttribute(
                                          AbstractConfigurationValidator.QUICK_FIX_TYPE_ATTR,
                                          AbstractConfigurationValidator.QuickFixType.NONE.ordinal());

        if (fixType == AbstractConfigurationValidator.QuickFixType.UNDEFINED_VARIABLE.ordinal()) {
            final String name = marker.getAttribute(AbstractConfigurationValidator.REFERENCE_NAME, "");
            return name.length() > 0;
        }

        if (fixType == AbstractConfigurationValidator.QuickFixType.UNRECOGNIZED_ELEMENT.ordinal()) {
            final String bestMatch = marker.getAttribute(AbstractConfigurationValidator.BEST_MATCH, "");
            return bestMatch.length() > 0;
        }

        return fixType != AbstractConfigurationValidator.QuickFixType.NONE.ordinal();
    }
}
