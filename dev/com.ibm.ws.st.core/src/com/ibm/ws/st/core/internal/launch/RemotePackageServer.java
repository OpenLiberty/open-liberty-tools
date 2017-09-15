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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 * Package server utility for remote server
 */
public class RemotePackageServer {

    int timeout;
    String zipFilePath;
    String include;
    IUtilityExecutionDelegate executionDelegate = null;
    String serverName = null;

    public RemotePackageServer(Map<String, String> commandVariables, int timeout, IUtilityExecutionDelegate helper) {
        this.timeout = timeout;
        this.executionDelegate = helper;
        if (commandVariables != null && !commandVariables.isEmpty()) {
            this.zipFilePath = commandVariables.get(CommandConstants.GENERAL_ARCHIVE);
            this.include = commandVariables.get(CommandConstants.GENERAL_INCLUDE);
        }
    }

    public void execute(WebSphereServer server, String launchMode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Executing remote package server command");
        try {
            this.serverName = server.getServerName();
            executionDelegate.initialize(server.getServer(), monitor);
            executionDelegate.startExecution();
            executionDelegate.execute(server, launch.getLaunchMode(), launch, getCommand(), NLS.bind(Messages.taskPackageServer, serverName), getTimeout(), monitor, true);
            if (!executionDelegate.isDockerExecutionDelegate())
                waitForCommandToFinish();

            if (monitor.isCanceled())
                return;

            downloadPackageServerFile(new SubProgressMonitor(monitor, 2));
            monitor.done();
        } catch (CoreException ce) {
            Trace.logError("PackageServer Utility failed", ce);
            if (ce.getLocalizedMessage() == null)
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction));
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_RemoteServer_ErrorUtilityAction, ce));
        } finally {
            executionDelegate.endExecution();
        }
    }

    int getTimeout() {
        // TODO Auto-generated method stub
        return timeout;
    }

    protected String getCommand() {
        StringBuilder command = new StringBuilder();
        command.append(executionDelegate.getServerScriptFilePath());
        command.append(CommandConstants.PACKAGE_SERVER + " " + serverName);
        if (getDefaultServerPackageName() != null)
            command.append(" " + CommandConstants.GENERAL_ARCHIVE + getDefaultServerPackageName());
        if (include != null)
            command.append(" " + CommandConstants.GENERAL_INCLUDE + include);
        return command.toString();
    }

    private void waitForCommandToFinish() throws CoreException {
        getRemoteUtilityConfigDelegate().checkFileStatus(getDefaultServerPackageName(), true);
    }

    public void downloadPackageServerFile(IProgressMonitor monitor) throws CoreException {
        executionDelegate.downloadFile(getDefaultServerPackageName(), zipFilePath, monitor);
    }

    private IPath getDefaultServerPackageName() {
        if (executionDelegate.getRemoteUserDir() == null)
            return null;
        return executionDelegate.getRemoteUserDir().append(serverName).addFileExtension("zip");
    }

    private LibertyRemoteUtilityExecutionDelegate getRemoteUtilityConfigDelegate() {
        return (LibertyRemoteUtilityExecutionDelegate) executionDelegate;
    }
}