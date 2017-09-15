/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.jee;

import com.ibm.ws.st.core.tests.ToolsTestBase;

public class JEETestBase extends ToolsTestBase {
    protected static final String SERVER_NAME = "JEETestBase_server";
    protected static final String SERVER_LOCATION = "resources/JEEDDtesting/server/" + SERVER_NAME;

    protected void createServer() throws Exception {
        createServer(runtime, getServerName(), getServerLocation());
        setServerStartTimeout(120);
        setServerStopTimeout(60);
        disableAutoPublish();
    }

    protected static String getServerName() {
        return SERVER_NAME;
    }

    protected static String getServerLocation() {
        return SERVER_LOCATION;
    }

    protected static String getJDKName() {
        return JDK_NAME;
    }

    protected static void createVM() throws Exception {
        createVM(getJDKName());
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return "aaa";
    }

    /** {@inheritDoc} */
    public boolean isStable() {
        return false;
    }
}