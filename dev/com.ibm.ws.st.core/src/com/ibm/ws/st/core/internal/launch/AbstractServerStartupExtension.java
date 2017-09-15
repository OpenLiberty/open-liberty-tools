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
package com.ibm.ws.st.core.internal.launch;

import java.util.List;

public abstract class AbstractServerStartupExtension {

    /**
     * Abstract method to be implemented by extensions that wish to set JVM arguments for a server that will be applied the next time it is started.
     * 
     * @param serverStartInfo contains a reference to the server object and the launch mode
     * @param additionalJVMOptions does not include options specified by the server.env or jvm.options files. It is the aggregate list of JVM options the tools know about and it
     *            will be up to each extension to handle duplicates in the list.
     */
    public abstract void setJVMOptions(ServerStartInfo serverStartInfo, List<String> additionalJVMOptions);

    /**
     * Provides server startup information to allow extensions to determine whether a server was started
     * with the required settings for profiling.
     * 
     * @param serverStartInfo contains a reference to the server object and the current launch mode
     * @param startupJVMOptions the JVM options that were used to start the server
     * @return true if profiling mode is detected, false if no change is desired or null if profiling mode cannot be determined
     */
    public Boolean isProfiling(ServerStartInfo serverStartInfo, List<String> startupJVMOptions) {
        return null;
    }

}
