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

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Element;

import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.xwt.dde.customization.ICustomLabelObject;

/**
 * Class for generating custom tree labels for the configuration editor.
 * Uses specific attributes for some elements, otherwise looks for the id
 * attribute.
 */
public class TreeLabelCustomObject implements ICustomLabelObject {

    private static final HashMap<String, String> treeData = new HashMap<String, String>();

    static {
        treeData.put("include", "location");
        treeData.put("application", "name");
        treeData.put("enterpriseApplication", "name");
        treeData.put("osgiApplication", "name");
        treeData.put("webApplication", "name");
        treeData.put("user", "name");
        treeData.put("group", "name");
        treeData.put("member", "name");
        treeData.put("variable", "name");
        treeData.put("dataSource", "jndiName");
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel(Element element, IResource resource) {
        // Check if a special attribute is defined for this element, otherwise
        // use the id attribute.
        String label = ConfigUIUtils.getTreeLabel(element, false);
        String attr = treeData.get(element.getNodeName());
        if (attr == null)
            attr = "id";
        String value = element.getAttribute(attr);
        if (value != null && value.length() > 0)
            label = NLS.bind(Messages.configTreeLabel, new String[] { label, value });

        // Custom label for property elements with name and value attributes
        if ("property".equals(element.getNodeName())) {
            String nameAttr = element.getAttribute("name");
            String valueAttr = element.getAttribute("value");
            if (nameAttr != null && nameAttr.length() > 0 && valueAttr != null && valueAttr.length() > 0) {
                label = NLS.bind(Messages.propertyTreeLabel, new String[] { label, nameAttr, valueAttr });
            }
        }

        return label;
    }
}
