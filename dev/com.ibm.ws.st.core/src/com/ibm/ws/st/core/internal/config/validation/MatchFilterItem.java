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

import org.w3c.dom.Element;

/**
 *
 */
public final class MatchFilterItem implements FilterItem {

    private final String pattern;
    private final String elemName;
    private final String attrName;
    final ConfigFileFilterItem parent;
    final Element node;

    MatchFilterItem(ConfigFileFilterItem parent, String pattern, String elemName, String attrName, Element node) {
        this.parent = parent;
        this.pattern = pattern;
        this.elemName = elemName;
        this.attrName = attrName;
        this.node = node;
    }

    public String getPattern() {
        return pattern;
    }

    public String getElementName() {
        return elemName;
    }

    public String getAttributeName() {
        return attrName;
    }

    // FilterItem methods
    @Override
    public FilterItem getParent() {
        return parent;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public Object[] getChildren() {
        return null;
    }

    // Object methods
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MatchFilterItem)) {
            return false;
        }

        final MatchFilterItem otherItem = (MatchFilterItem) other;
        return pattern.equals(otherItem.pattern)
               && elemName.equals(otherItem.elemName)
               && attrName.equals(otherItem.attrName);
    }

    @Override
    public int hashCode() {
        return elemName.hashCode();
    }
}
