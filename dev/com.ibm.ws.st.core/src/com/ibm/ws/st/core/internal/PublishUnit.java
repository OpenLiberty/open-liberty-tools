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
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.server.core.IModule;

/**
 *
 */
public class PublishUnit {
    private final IModule[] module;
    private PublishUnit parent = null;
    private List<PublishUnit> children = null;
    private int deltaKind;

    public PublishUnit(final IModule[] module, int deltaKind) {
        this.module = module;
        this.deltaKind = deltaKind;
    }

    /**
     * @return the module
     */
    public IModule[] getModule() {
        return module;
    }

    /**
     * @return the deltaKind
     */
    public int getDeltaKind() {
        return deltaKind;
    }

    public void setDeltaKind(int deltaKind) {
        this.deltaKind = deltaKind;
    }

    /**
     * @return the parent
     */
    public PublishUnit getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(PublishUnit parent) {
        this.parent = parent;
    }

    /**
     * @return the children
     */
    public List<PublishUnit> getChildren() {
        return children;
    }

    /**
     * 
     */
    public void addChild(PublishUnit child) {
        if (child == null)
            return;
        if (children == null)
            children = new ArrayList<PublishUnit>();
        children.add(child);
    }

    public String getModuleName() {
        if (module == null)
            return null;
        return module[module.length - 1].getName();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((module[0] == null) ? 0 : module[0].hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PublishUnit other = (PublishUnit) obj;
        if (module == null) {
            if (other.module != null)
                return false;
        } else if (!getModuleName().equals(other.getModuleName()))
            return false;
        return true;
    }

}
