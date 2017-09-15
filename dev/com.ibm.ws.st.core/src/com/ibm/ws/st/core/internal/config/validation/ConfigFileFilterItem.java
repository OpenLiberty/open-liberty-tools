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

import java.util.HashSet;

/**
 *
 */
public class ConfigFileFilterItem implements FilterItem {

    private final FilterItem parent;
    private final String path;
    private final HashSet<MatchFilterItem> children = new HashSet<MatchFilterItem>();

    protected ConfigFileFilterItem(FilterItem parent, String path) {
        this.parent = parent;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    protected MatchFilterItem[] getMatchItems() {
        final MatchFilterItem[] items = new MatchFilterItem[children.size()];
        return children.toArray(items);
    }

    protected void addMatchItem(MatchFilterItem item) {
        children.add(item);
    }

    protected boolean removeMatchItem(MatchFilterItem item) {
        return children.remove(item);
    }

    protected void removeAllChildren() {
        children.clear();
    }

    protected boolean containsMatchItem(MatchFilterItem item) {
        return children.contains(item);
    }

    // FilterItem methods
    @Override
    public FilterItem getParent() {
        return parent;
    }

    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }

    @Override
    public Object[] getChildren() {
        return children.toArray();
    }

    // Object methods
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConfigFileFilterItem)) {
            return false;
        }

        return path.equals(((ConfigFileFilterItem) other).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
