/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal;

import java.util.List;

import org.eclipse.swt.graphics.Image;

public class CustomServerConfigTreeNode {

    private final String label;
    private final Image icon;
    private final List<Object> children;

    public CustomServerConfigTreeNode(String label, Image icon, List<Object> children) {
        this.label = label;
        this.icon = icon;
        this.children = children;
    }

    public String getLabel() {
        return label;
    }

    public Image getIcon() {
        return icon;
    }

    public List<Object> getChildren() {
        return children;
    }

}
