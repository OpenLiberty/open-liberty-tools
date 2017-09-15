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
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;

/**
 * Interface used to request handling of feature conflicts.
 */
public abstract class PublishWithErrorHandler {

    private int returnCode = -99;

    /**
     * @return the returnCode
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * @param returnCode the returnCode to set
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public abstract boolean handlePublishWithError(final ArrayList<String> projectName, final WebSphereServer server);

}