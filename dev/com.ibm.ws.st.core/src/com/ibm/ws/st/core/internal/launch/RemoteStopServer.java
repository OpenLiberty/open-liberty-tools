/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereServer;

public class RemoteStopServer {

    int timeout;
    IUtilityExecutionDelegate execDelegate = null;
    String serverName = null;

    public RemoteStopServer(int timeout, IUtilityExecutionDelegate helper) {
        this.timeout = timeout;
        this.execDelegate = helper;
    }

    protected String getCommand() {
        StringBuilder command = new StringBuilder();

        // get the server script file path
        command.append(execDelegate.getServerScriptFilePath());

        // add stop command specifics
        command.append(" " + CommandConstants.STOP_SERVER + " " + serverName);

        return command.toString();
    }

    int getTimeout() {
        return timeout;
    }

    public void execute(WebSphereServer server, String launchMode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Executing stop remote server command");
        try {
            this.serverName = server.getServerName();
            execDelegate.initialize(server.getServer(), monitor);
            execDelegate.startExecution();
            execDelegate.execute(server, launch.getLaunchMode(), launch, getCommand(), NLS.bind(Messages.taskStopServer, serverName), getTimeout(), monitor, true);
        } catch (CoreException ce) {
            Trace.logError("RemoteStop failed", ce);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ce.getLocalizedMessage()));
        } finally {
            execDelegate.endExecution();
        }
    }

}
