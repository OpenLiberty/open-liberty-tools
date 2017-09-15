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
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

import com.ibm.ws.st.core.internal.config.validation.ConfigFileFilterItem;
import com.ibm.ws.st.core.internal.config.validation.Constants;
import com.ibm.ws.st.core.internal.config.validation.MatchFilterItem;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

public class IgnoreFilterLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof ConfigFileFilterItem) {
            return Activator.getImage(Activator.IMG_RELATIVE_PATH);
        }

        if (element instanceof MatchFilterItem) {
            return Activator.getImage(Activator.IMG_IGNORE_FILTER);
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof ConfigFileFilterItem) {
            return NLS.bind(Messages.configItemPathLabel, new String[] { ((ConfigFileFilterItem) element).getPath() });
        }
        if (element instanceof MatchFilterItem) {
            final MatchFilterItem matchItem = (MatchFilterItem) element;
            final String pattern = matchItem.getPattern();
            final String elementName = matchItem.getElementName();
            final String attributeName = matchItem.getAttributeName();
            if (Constants.PATTERN_UNREC_ATTR.equals(pattern)) {
                if (attributeName == Constants.MATCH_ALL) {
                    if (elementName == Constants.MATCH_ALL) {
                        return Messages.ignoreAllAttrAllElemLabel;
                    }
                    return NLS.bind(Messages.ignoreAllAttrElemLabel, new String[] { elementName });
                }
                return NLS.bind(Messages.ignoreAttrElemLabel, new String[] { attributeName, elementName });
            }

            return NLS.bind(Messages.ignoreLabelGeneric, new String[] { pattern, elementName, attributeName });
        }
        return super.getText(element);
    }
}
