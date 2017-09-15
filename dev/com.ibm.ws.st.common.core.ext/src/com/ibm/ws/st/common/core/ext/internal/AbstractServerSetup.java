/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.common.core.ext.internal;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;

import com.ibm.ws.st.common.core.ext.internal.producer.ServerCreationException;

public abstract class AbstractServerSetup {

    /**
     * Initialize the setup.
     *
     * @throws ServerCreationException, UnsupportedServiceException, Exception
     */
    public abstract void initialize(String serviceType, Map<String, String> serviceInfo, IRuntime rt) throws ServerCreationException, UnsupportedServiceException, Exception;

    /**
     * Perform setup steps.
     *
     * @throws ServerCreationException, UnsupportedServiceException, Exception
     */
    public abstract void setup(IProgressMonitor monitor) throws ServerCreationException, UnsupportedServiceException, Exception;

    /**
     * Create a WTP server instance using the provided runtime.
     *
     * @throws ServerCreationException, UnsupportedServiceException, Exception
     */
    public abstract void createServer() throws ServerCreationException, UnsupportedServiceException, Exception;

    /**
     * Updates the serviceInfo map with the key/value pair, if the key cannot be found, create one
     */

    public void updateServiceInfo(String key, String value) {
        // To be overridden
        // Reason for not being abstract: backwards compatibility
        return;
    }

    /**
     * (Liberty)Validate if the server has a basicRegistry, and if so, whether the user has administrator-role and whether the password is correct
     *
     * @param user
     * @param password
     * @param monitor
     */
    public int validateRemoteSecurity(String user, String password, IProgressMonitor monitor) {
        // To be overridden
        // Reason for not being abstract: backwards compatibility
        return -1;
    }

    /**
     * (Liberty)Update the basicRegistry with the user and password specified, and add administrator-role to the user
     *
     * @param user
     * @param password
     */
    public void updateRemoteSecurity(String user, String password, int code, IProgressMonitor monitor) {
        // To be overridden
        // Reason for not being abstract: backwards compatibility
    }

    /**
     * (Liberty)Obtain the server.xml from the remote vm
     *
     * @return a temp server.xml copied from the remote vm, used for validating/updating
     */
    public File getServerXML() {
        // To be overridden
        // Reason for not being abstract: backwards compatibility
        return null;
    }

    /**
     * Obtain the HTTP port numbers from the remote vm
     *
     * @return the integer value of the http/https port number
     */
    public int getHTTPPort(String portType) {
        // To be overridden
        // Reason for not being abstract: backwards compatibility
        return -1;
    }

}
