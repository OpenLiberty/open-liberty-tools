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

package com.ibm.ws.st.core.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.launch.UtilityLaunchConfigurationDelegate;

/**
 *
 */
public class LibertyServerDumpUtility {

    /**
     * Launches a utility process to generate a server dump.
     * The server must exist.
     *
     * @param server the server
     * @param archiveFile the archive file, or <code>null</code>
     * @param include include options, or <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     * @throws CoreException
     */
    public ILaunch dumpServer(WebSphereRuntime runtime, WebSphereServerInfo server, File archiveFile, String include, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        verifyServerExists(runtime, server);

        if (archiveFile != null && archiveFile.isDirectory())
            throw new IllegalArgumentException(Messages.invalidZipFile);

        String serverName = server.getServerName();
        monitor2.beginTask(NLS.bind(Messages.taskDumpServer, serverName), 200);
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(server);

        if (wsServer == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        ILaunchConfiguration lc = null;
        try {
            List<String> command = new ArrayList<String>();
            if (wsServer.isLocalSetup()) {
                command.add(Constants.BATCH_SCRIPT);
                command.add("dump");
                command.add(server.getServerName());
                if (archiveFile != null)
                    command.add("--archive=" + archiveFile.getAbsolutePath());
                if (include != null && !include.trim().isEmpty())
                    command.add("--include=" + include);

                lc = createUtilityLaunchConfig(runtime, server, null, command.toArray(new String[command.size()]));
            } else { //remote case
                Map<String, String> commandVariables = new HashMap<String, String>();
                commandVariables.put(CommandConstants.UTILITY_TYPE, CommandConstants.DUMP_SERVER);
                if (archiveFile != null)
                    commandVariables.put(CommandConstants.GENERAL_ARCHIVE, archiveFile.getAbsolutePath());
                if (include != null && !include.trim().isEmpty())
                    commandVariables.put(CommandConstants.GENERAL_INCLUDE, include);
                lc = createRemoteUtilityLaunchConfig(runtime, server, commandVariables);
            }
            return lc.launch(ILaunchManager.RUN_MODE, monitor2);
        } catch (Throwable t) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorDumpServer, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
        }
    }

    /**
     * Launches a utility process to generate a server javadump.
     * The server must exist.
     *
     * @param server the server
     * @param include include options, or <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     * @throws CoreException
     */
    public ILaunch javadumpServer(WebSphereRuntime runtime, WebSphereServerInfo server, String include, IProgressMonitor monitor) throws CoreException {
        IProgressMonitor monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        verifyServerExists(runtime, server);

        String serverName = server.getServerName();
        monitor2.beginTask(NLS.bind(Messages.taskDumpServer, serverName), 200);

        WebSphereServer ws = WebSphereUtil.getWebSphereServer(server);

        ILaunchConfiguration lc = null;
        try {
            if (ws != null && ws.isLocalSetup()) {
                List<String> command = new ArrayList<String>();
                command.add(Constants.BATCH_SCRIPT);
                command.add("javadump");
                command.add(server.getServerName());
                if (include != null && !include.trim().isEmpty())
                    command.add("--include=" + include);
                lc = createUtilityLaunchConfig(runtime, server, null, command.toArray(new String[command.size()]));
            } else {// remote case
                Map<String, String> commandVariables = new HashMap<String, String>();
                commandVariables.put(CommandConstants.UTILITY_TYPE, CommandConstants.JAVA_DUMP);
                if (include != null && !include.trim().isEmpty())
                    commandVariables.put(CommandConstants.GENERAL_INCLUDE, include);
                lc = createRemoteUtilityLaunchConfig(runtime, server, commandVariables);
            }
            return lc.launch(ILaunchManager.RUN_MODE, monitor2);
        } catch (Throwable t) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorDumpServer, t.getLocalizedMessage())));
        } finally {
            monitor2.done();
        }
    }

    protected ILaunchConfiguration createUtilityLaunchConfig(WebSphereRuntime runtime, WebSphereServerInfo serverInfo, String jvmArgs, String... command) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);
        String serverID = wsServer.getServer().getId();

        String launchConfigType = UtilityLaunchFactory.getLaunchConfigurationType(wsServer.getServerType());
        ILaunchConfigurationType lct = launchManager.getLaunchConfigurationType(launchConfigType);
        ILaunchConfigurationWorkingCopy wc = lct.newInstance(null, runtime.getRuntime().getName());

        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_RUNTIME, runtime.getRuntime().getId());
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_WORK_DIR, serverInfo.getServerOutputPath().toOSString());
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_USER_DIR, runtime.getUserDirectories().indexOf(serverInfo.getUserDirectory()));
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_LABEL,
                        LaunchUtil.getProcessLabelAttr(serverInfo.getWebSphereRuntime().getRuntime().getName(), serverInfo.getServerName()));
        if (jvmArgs != null) {
            wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_JVM_ARGS, jvmArgs);
        }
        List<String> list = new ArrayList<String>();
        for (String s : command)
            list.add(s);
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_COMMAND, list);
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_SERVER_ID, serverID);
        return wc.doSave();
    }

    protected ILaunchConfiguration createRemoteUtilityLaunchConfig(WebSphereRuntime runtime, WebSphereServerInfo serverInfo,
                                                                   Map<String, String> commandVariables) throws CoreException {

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        WebSphereServer wsServer = WebSphereUtil.getWebSphereServer(serverInfo);
        String serverID = wsServer.getServer().getId();

        String launchConfigType = UtilityLaunchFactory.getLaunchConfigurationType(wsServer.getServerType());
        ILaunchConfigurationType lct = launchManager.getLaunchConfigurationType(launchConfigType);
        ILaunchConfigurationWorkingCopy wc = lct.newInstance(null, runtime.getRuntime().getName());
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_COMMAND, commandVariables);
        wc.setAttribute(UtilityLaunchConfigurationDelegate.ATTR_SERVER_ID, serverID);
        return wc.doSave();
    }

    private void verifyServerExists(WebSphereRuntime runtime, WebSphereServerInfo server) {
        if (server == null || server.getServerName() == null)
            throw new IllegalArgumentException("Server cannot be null");

        IPath path = runtime.getRuntime().getLocation();
        if (path == null)
            throw new IllegalArgumentException("Runtime does not exist");

        if (!server.getServerPath().toFile().exists())
            throw new IllegalArgumentException("Server does not exist");
    }

}
