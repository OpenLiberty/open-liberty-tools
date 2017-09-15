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
public class QuickFixIgnoreAllProperties extends AbstractMarkerResolution implements ConfigurationQuickFix {
    private final String elementName;

    public QuickFixIgnoreAllProperties() {
        this(null);
    }

    public QuickFixIgnoreAllProperties(String elemName) {
        elementName = elemName;
    }

    @Override
    public String getLabel() {
        if (elementName == null) {
            return Messages.ignoreAllAttrAllElemLabel;
        }

        return NLS.bind(Messages.ignoreAllAttrElemLabel, new String[] { elementName });
    }

    @Override
    public void run(IMarker marker) {
        final IResource resource = getResource(marker);
        if (resource == null)
            return;

        if (ignoreAllAttributes(resource)) {
            try {
                resource.touch(new NullProgressMonitor());
                return;
            } catch (CoreException e) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.ERROR, "Quick fix for ignore all unrecognized attributes failed. Error trying to refresh configuration file: " + resource.getLocationURI(), e);
            }
        }
        showErrorMessage();
    }

    @Override
    public ResolutionType getResolutionType() {
        if (elementName == null)
            return ResolutionType.IGNORE_ALL_ATTR_ALL_ELEM;

        return ResolutionType.IGNORE_ALL_ATTR_ELEM;
    }

    private boolean ignoreAllAttributes(IResource resource) {
        if (elementName == null)
            return ValidationFilterUtil.ignoreAllAttributes(resource);

        return ValidationFilterUtil.ignoreAllAttributes(resource, elementName);
    }

    @Override
    protected String getErrorMessage() {
        return Messages.ignoreAllPropFailedMessage;
    }
}
