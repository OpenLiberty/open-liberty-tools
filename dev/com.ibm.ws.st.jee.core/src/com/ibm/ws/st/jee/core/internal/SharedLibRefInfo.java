/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.jee.core.internal;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibRef;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.LibraryRefType;

public class SharedLibRefInfo extends Properties {
    private static final long serialVersionUID = 1L;
    private static final String DELIM = ",";

    public List<LibRef> getLibRefs() {
        String ids = getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_REF_ID_KEY, "");
        StringTokenizer stIds = new StringTokenizer(ids, DELIM);
        String types = getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_REF_TYPE_KEY, "");
        StringTokenizer stTypes = new StringTokenizer(types, DELIM);
        List<LibRef> refs = new ArrayList<LibRef>();
        // Old settings files may not have the type as only common was supported before
        while (stIds.hasMoreTokens()) {
            String id = stIds.nextToken().trim();
            if (!id.isEmpty()) {
                LibraryRefType type = LibraryRefType.COMMON;
                if (stTypes.hasMoreTokens()) {
                    String typeStr = stTypes.nextToken().trim();
                    if (!typeStr.isEmpty()) {
                        type = LibraryRefType.getLibraryRefType(typeStr);
                    }
                }
                refs.add(new LibRef(id, type));
            }
        }
        return refs;
    }

    public void setLibRefs(List<LibRef> refs) {
        StringBuilder sbIds = new StringBuilder();
        StringBuilder sbTypes = new StringBuilder();
        if (refs != null) {
            boolean first = true;
            for (LibRef ref : refs) {
                if (first) {
                    sbIds.append(ref.id);
                    sbTypes.append(ref.type.getName());
                    first = false;
                } else {
                    sbIds.append(DELIM).append(ref.id);
                    sbTypes.append(DELIM).append(ref.type.getName());
                }
            }
        }
        setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_REF_ID_KEY, sbIds.toString());
        setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_REF_TYPE_KEY, sbTypes.toString());
    }

    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.IOException("Cannot be deserialized");
    }
}
