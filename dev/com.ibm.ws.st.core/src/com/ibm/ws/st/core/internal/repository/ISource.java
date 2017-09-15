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
package com.ibm.ws.st.core.internal.repository;

/**
 * A product's archive source information
 */
public interface ISource {

    /**
     * Return the location of archive to install. This may be a file name
     * or a URL.
     * 
     * @return location of archive
     */
    public String getLocation();

    /**
     * Return the size of the product's archive
     * 
     * @return product archive size
     */
    public long getSize();
}
