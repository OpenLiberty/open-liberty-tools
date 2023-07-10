/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
import java.util.Properties;

public class UtilitySharedLibInfo extends Properties {
    private static final long serialVersionUID = 1L;

    public String getLibId() {
        return getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_ID_KEY, "");
    }

    public String getLibDir() {
        return getProperty(JEEServerExtConstants.SHARED_LIBRARY_SETTING_LIB_DIR_KEY, "");
    }

    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.IOException("Cannot be deserialized");
    }
}
