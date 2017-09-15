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
package com.ibm.ws.st.core.internal.service.setup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.producer.ServerCreationException;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.internal.Trace;

public class LibertyWASaaSSetup extends LibertySetup {

    /** {@inheritDoc} */
    @Override
    void serverSetup(IPlatformHandler setup, IProgressMonitor monitor) throws ServerCreationException {
        Path serverConfigPath = new Path(serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH));
        super.serverSetup(setup, monitor);

        // change ownership of config dropins for WASaaS pattern
        try {
            setup.executeCommand("chown -R wsadmin:admingroup " + serverConfigPath.append("/" + Constants.DROPINS_DIR).toString(), DEFAULT_TIMEOUT);
        } catch (Exception e) {
            Trace.logError("Failed to change ownership of configuration dropin files", e);
        }
    }

}
