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
package com.ibm.ws.st.core.tests;

import java.util.Map;

import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;

/**
 * Interface for other test base classes, multiple classes can inherit this interface
 */
public interface TestBaseInterface {

    /**
     * Creates a liberty server
     */
    public IServer createServer(IRuntime rt, Map<String, String> serverInfo) throws Exception;

    /**
     * Cleans up the test workspace
     */
    public void cleanUp();

    /**
     * Get the base URL for the server (e.g. http://localhost:9080)
     */
    public String getBaseURL(IServer server) throws Exception;

    /**
     * Does the server support loose configuration
     */
    public boolean supportsLooseCfg(IServer server);

}
