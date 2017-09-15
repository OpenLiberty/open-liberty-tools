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
package com.ibm.ws.st.jee.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class SharedLibRefInfo extends Properties {
    private static final long serialVersionUID = 1L;
    private static final String DELIM = ",";

    public List<String> getLibRefIds() {
        String refs = getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_REF_ID_KEY, "");
        StringTokenizer st = new StringTokenizer(refs, DELIM);
        List<String> ids = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String s = st.nextToken().trim();
            if (!s.isEmpty())
                ids.add(s);
        }
        return ids;
    }

    public void setLibRefIds(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        if (ids != null) {
            boolean first = true;
            for (String s : ids) {
                if (first) {
                    sb.append(s);
                    first = false;
                } else
                    sb.append(DELIM).append(s);
            }
        }
        setProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_REF_ID_KEY, sb.toString());
    }
}
