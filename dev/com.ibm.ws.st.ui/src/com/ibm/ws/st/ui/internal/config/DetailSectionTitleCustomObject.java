/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.config;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Element;

import com.ibm.xwt.dde.customization.ICustomLabelObject;

/**
 * Custom object for tree labels
 */
public class DetailSectionTitleCustomObject implements ICustomLabelObject {

    /** {@inheritDoc} */
    @Override
    public String getLabel(Element element, IResource resource) {
        return ConfigUIUtils.getTreeLabel(element, false);
    }

}
