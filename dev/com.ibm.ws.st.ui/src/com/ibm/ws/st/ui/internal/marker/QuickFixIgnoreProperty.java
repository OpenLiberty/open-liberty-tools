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
package com.ibm.ws.st.ui.internal.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.config.validation.ConfigurationQuickFix;
import com.ibm.ws.st.core.internal.config.validation.ValidationFilterUtil;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Quick fix for unrecognized property name - best match
 */
public class QuickFixIgnoreProperty extends AbstractMarkerResolution implements ConfigurationQuickFix {
    private final String elementName;
    private final String attributeName;

    public QuickFixIgnoreProperty(String elemName, String attrName) {
        elementName = elemName;
        attributeName = attrName;
    }

    @Override
    public String getLabel() {
        return NLS.bind(Messages.ignoreAttrElemLabel, new String[] { attributeName, elementName });
    }

    @Override
    public void run(IMarker marker) {
        final IResource resource = getResource(marker);
        if (resource == null)
            return;

        if (ValidationFilterUtil.ignoreAttribute(resource, elementName, attributeName)) {
            try {
                resource.touch(new NullProgressMonitor());
                return;
            } catch (CoreException e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.ERROR,
                                "Quick fix for ignore unrecognized attribute '" + attributeName + "' failed. Error trying to refresh configuration file: "
                                                + resource.getLocationURI(), e);
                }
            }
        }
        showErrorMessage();
    }

    @Override
    public ResolutionType getResolutionType() {
        return ResolutionType.IGNORE_ATTR_ELEM;
    }

    @Override
    protected String getErrorMessage() {
        return Messages.ignorePropFailedMessage;
    }
}
