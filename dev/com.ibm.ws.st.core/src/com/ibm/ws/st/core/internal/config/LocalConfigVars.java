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
package com.ibm.ws.st.core.internal.config;

import java.util.EnumSet;
import java.util.List;

/**
 * Container for local configuration variables (all attributes in the
 * current node are considered local variables).
 * 
 * Parent is the set of global and predefined variables.
 */
public class LocalConfigVars extends ConfigVars {

    private ConfigVars parent;

    public LocalConfigVars(ConfigVars parent) {
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGlobalScope() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String name) {
        if (super.isDefined(name))
            return super.getValue(name);
        return parent.getValue(name);
    }

    /** {@inheritDoc} */
    @Override
    public Type getType(String name) {
        if (super.isDefined(name))
            return super.getType(name);
        return parent.getType(name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefined(String name) {
        if (super.isDefined(name))
            return true;
        return parent.isDefined(name);
    }

    /** {@inheritDoc} */
    @Override
    public DocumentLocation getDocumentLocation(String name) {
        if (super.isDefined(name))
            return super.getDocumentLocation(name);
        return parent.getDocumentLocation(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getGlobalVars(EnumSet<Type> types, boolean includeUnresolvedPredefined, boolean sort) {
        return parent.getGlobalVars(types, includeUnresolvedPredefined, sort);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getVars(EnumSet<Type> types, boolean includeUnresolvedPredefined) {
        List<String> vars = super.getVars(types, includeUnresolvedPredefined);
        List<String> parentVars = parent.getVars(types, includeUnresolvedPredefined);
        for (String var : parentVars) {
            if (!vars.contains(var)) {
                vars.add(var);
            }
        }
        return vars;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPredefinedVar(String name) {
        return parent.isPredefinedVar(name);
    }

    @Override
    public void copyInto(ConfigVars vars) {
        super.copyInto(vars);
        if (vars instanceof LocalConfigVars)
            ((LocalConfigVars) vars).parent = parent;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Local:\n");
        builder.append(super.toString());
        builder.append("Parent:\n");
        builder.append(parent.toString());
        return builder.toString();
    }

}
