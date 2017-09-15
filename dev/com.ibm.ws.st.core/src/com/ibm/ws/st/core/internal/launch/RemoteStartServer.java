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
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereServer;

public class RemoteStartServer {

    int timeout;
    IUtilityExecutionDelegate execDelegate = null;
    String serverName = null;
    String launchMode = null;

    public RemoteStartServer(int timeout, IUtilityExecutionDelegate helper) {
        this.timeout = timeout;
        this.execDelegate = helper;
    }

    protected String getCommand() {
        StringBuilder command = new StringBuilder();

        // get the server script file path
        command.append(execDelegate.getServerScriptFilePath());

        // add start command specifics
        if (launchMode.equals(ILaunchManager.DEBUG_MODE))
            command.append(CommandConstants.DEBUG_SERVER + " " + serverName);
        else
            command.append(CommandConstants.START_SERVER + " " + serverName);

        return command.toString();
    }

    int getTimeout() {
        return timeout;
    }

    public void execute(WebSphereServer server, String launchMode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Executing start remote server command");
        try {
            this.serverName = server.getServerName();
            this.launchMode = launchMode;
            execDelegate.initialize(server.getServer(), monitor);
            execDelegate.startExecution();
            execDelegate.execute(server, launch.getLaunchMode(), launch, getCommand(), NLS.bind(Messages.taskStartServer, serverName), getTimeout(), monitor, false);
        } catch (CoreException ce) {
            Trace.logError("RemoteStart failed", ce);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ce.getLocalizedMessage()));
        } finally {
            execDelegate.endExecution();
        }
    }

}
