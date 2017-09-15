/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.common.core.ext.internal.producer;

public class ServerCreationException extends Exception {

    private static final long serialVersionUID = 7432992514868568103L;

    public ServerCreationException(String message) {
        super(message);
    }

    public ServerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
