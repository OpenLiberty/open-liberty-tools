/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal;

public class UnsupportedServiceException extends Exception {

    private static final long serialVersionUID = 3353561597759569L;

    public UnsupportedServiceException(String message) {
        super(message);
    }

    public UnsupportedServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
