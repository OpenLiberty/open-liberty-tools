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
package com.ibm.ws.st.ui.internal;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.server.core.IRuntime;

public class RuntimePropertyTester extends PropertyTester {
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (expectedValue instanceof String)
            return checkProperty(receiver, property, (String) expectedValue);
        if (expectedValue != null)
            return checkProperty(receiver, property, expectedValue.toString());

        return checkProperty(receiver, property, null);
    }

    protected static boolean checkProperty(Object target, String property, String value) {
        if ("runtimeType".equals(property)) {
            if (value == null)
                return false;

            IRuntime runtime = (IRuntime) Platform.getAdapterManager().getAdapter(target, IRuntime.class);
            if (runtime != null) {
                if (runtime.getRuntimeType() == null)
                    return false;

                String id = runtime.getRuntimeType().getId();
                if (value.endsWith("*"))
                    return id.startsWith(value.substring(0, value.length() - 1));
                return id.equals(value);
            }
        }
        return false;
    }
}
