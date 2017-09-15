/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.config.validation;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * A matcher to check whether certain attributes should be ignored during
 * validation
 */
public class ValidationFilterMatcher {

    private final IProject project;
    private final ValidationFilterSettings valSettings;

    public ValidationFilterMatcher(IProject project) {
        this.project = project;
        valSettings = new ValidationFilterSettings(project);
    }

    public boolean isIgnoreAttribute(IResource resource, String elemName, String attrName) {
        final Set<ConfigFileFilterItem> configSet = valSettings.getConfigFileItems();
        final String pathString = ValidationFilterUtil.getRelativePathString(project, resource);

        for (ConfigFileFilterItem cfi : configSet) {
            final String configPath = cfi.getPath();
            if (configPath == Constants.MATCH_ALL || configPath.equals(pathString)) {
                MatchFilterItem[] matchItems = cfi.getMatchItems();
                for (MatchFilterItem matchItem : matchItems) {
                    if (matchAttribute(matchItem, elemName, attrName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean matchAttribute(MatchFilterItem matchItem, String elemName, String attrName) {
        if (Constants.PATTERN_UNREC_ATTR.equals(matchItem.getPattern())) {
            final String matchElemName = matchItem.getElementName();
            if (matchElemName == Constants.MATCH_ALL || matchElemName.equals(elemName)) {
                final String matchAttrName = matchItem.getAttributeName();
                return (matchAttrName == Constants.MATCH_ALL || matchAttrName.equals(attrName));
            }
        }

        return false;
    }
}
