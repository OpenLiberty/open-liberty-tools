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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 * Generate java dumps for support for remote server
 * 
 */
public class RemoteJavaDump {

    int timeout;
    String zipFilePath;
    String include;
    IUtilityExecutionDelegate executionDelegate = null;
    String serverName = null;
    private final String Expected_Sys_Out = "dump complete";

    public RemoteJavaDump(Map<String, String> commandVariables, int timeout, IUtilityExecutionDelegate helper) {
        this.timeout = timeout;
        this.executionDelegate = helper;
        if (commandVariables != null && !commandVariables.isEmpty()) {
            this.include = commandVariables.get(CommandConstants.GENERAL_INCLUDE);
        }
    }

    public void execute(WebSphereServer server, String launchMode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Executing generate java dumps command for remote server");

        try {
            this.serverName = server.getServerName();
            executionDelegate.initialize(server.getServer(), monitor);
            executionDelegate.startExecution();
            ExecutionOutput result = executionDelegate.execute(server, launch.getLaunchMode(), launch, getCommand(), NLS.bind(Messages.taskJavaDump, serverName), getTimeout(),
                                                               monitor, true);
            if (!executionDelegate.isExecutionSuccessful(result, Expected_Sys_Out)) {
                Trace.logError("Remote command failed: " + getCommand() + ", exit value: " + result.getReturnCode() + ", output: " + result.getOutput() + ", error: "
                               + result.getError(), null);
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, result.getError()));
            }

        } catch (CoreException ce) {
            Trace.logError("javaDump utility failed", ce);
            if (ce.getLocalizedMessage() == null)
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction));
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction, ce));
        } finally {
            executionDelegate.endExecution();
        }
    }

    int getTimeout() {
        return timeout;
    }

    protected String getCommand() {
        StringBuilder command = new StringBuilder();
        command.append(executionDelegate.getServerScriptFilePath());
        command.append(CommandConstants.JAVA_DUMP + " " + serverName);
        if (include != null)
            command.append(" " + CommandConstants.GENERAL_INCLUDE + include);
        return command.toString();
    }
}
