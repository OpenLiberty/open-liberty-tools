/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

public class OutOfSyncModuleInfo {

    public enum Type {
        APP_ENTRY_MISSING,
        SHARED_LIB_ENTRY_MISSING,
        SHARED_LIB_REF_MISMATCH
    }

    public enum Property {
        LIB_REF_IDS_ADD,
        LIB_REF_IDS_REMOVE,
        LIB_REF_API_VISIBILITY
    }

    private final Type type;

    public OutOfSyncModuleInfo(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getPropertyValue(Property key) {
        return null;
    }
}
