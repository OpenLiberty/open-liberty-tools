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
package com.ibm.ws.st.ui.internal.config;

/**
 * Directory location custom control
 */
public class DirLocationTextCustomObject extends LocationTextCustomObject {

    @Override
    protected LocationType getLocationType() {
        return LocationType.FOLDER;
    }

    @Override
    protected String getClassName() {
        return "com.ibm.ws.st.ui.internal.config.GenericDirBrowser";
    }

}
