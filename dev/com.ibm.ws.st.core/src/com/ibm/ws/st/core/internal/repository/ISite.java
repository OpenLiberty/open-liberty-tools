/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.repository;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A product's install site
 */
public interface ISite {

    /**
     * Return the name of the site
     * 
     * @return site name
     */
    public String getName();

    /**
     * Indicate whether authentication is required or not
     * 
     * @return true if authentication is required, otherwise false;
     */
    public boolean isAuthenticationRequired();

    /**
     * Indicate whether authentication was successful or not
     * 
     * @return true if authentication succeeded, otherwise false
     */
    public boolean isAuthenticated();

    /**
     * Perform authentication to the site
     * 
     * @param pa user authentication
     * @param monitor a progress monitor
     * 
     * @return true if successful, otherwise false
     * 
     * @throws IOException
     */
    public boolean authenticate(PasswordAuthentication pa, IProgressMonitor monitor) throws IOException;

    /**
     * Return the list of available runtime environment products
     * 
     * @param monitor a progress monitor
     * @return list of available products
     */
    public List<IProduct> getCoreProducts(IProgressMonitor monitor);

    /**
     * Return the list of applicable assets that can be installed on top of
     * an existing runtime
     * 
     * @param runtimeInfo runtime environment information
     * @param monitor a progress monitor
     * @return list of applicable products
     */
    public List<IProduct> getApplicableProducts(IRuntimeInfo runtimeInfo, IProgressMonitor monitor);

    /**
     * Return the list of available configuration snippet products
     * 
     * @param monitor a progress monitor
     * @return list of available products
     */
    public List<IProduct> getConfigSnippetProducts(IProgressMonitor monitor);

    /**
     * Reset the site.
     */
    public void reset();
}
