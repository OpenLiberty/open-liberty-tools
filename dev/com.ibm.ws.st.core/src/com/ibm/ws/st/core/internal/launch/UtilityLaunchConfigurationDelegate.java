/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.LaunchUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UserDirectory;
import com.ibm.ws.st.core.internal.UtilityExtension;
import com.ibm.ws.st.core.internal.UtilityExtensionFactory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;

/**
 * Launch configuration for WLP scripts.
 */
public class UtilityLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
    public static final String ATTR_LABEL = "label";
    public static final String ATTR_RUNTIME = "runtime";
    public static final String ATTR_COMMAND = "command";
    public static final String ATTR_WORK_DIR = "workDir";
    public static final String ATTR_USER_DIR = "userDir";
    public static final String ATTR_JVM_ARGS = "jvmArgs";
    public static final String ATTR_SERVER_ID = "serverID";

    protected static IProcess newProcess(ILaunch launch, Process p, String label) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(IProcess.ATTR_PROCESS_TYPE, "websphere.utility");
        return new RuntimeProcess(launch, p, label, attributes);
    }

    protected static String renderCmdLine(ProcessBuilder pb) {
        List<String> cmdLine = pb.command();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String cmd : cmdLine) {
            if (!first)
                sb.append(" ");
            first = false;
            if (cmd.contains(" "))
                sb.append("\"" + cmd + "\"");
            else
                sb.append(cmd);
        }
        return sb.toString();
    }

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        IServer server = null;
        String serverID = configuration.getAttribute(ATTR_SERVER_ID, (String) null);

        if (serverID != null) {
            server = ServerCore.findServer(serverID);
        }

        if (server == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Launch configuration could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptCannotRunRemoteUtility));
        }

        WebSphereServer websphereServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        WebSphereServerBehaviour websphereServerBehaviour = (WebSphereServerBehaviour) server.loadAdapter(WebSphereServerBehaviour.class, null);

        IRuntime runtime = server.getRuntime();
        if (runtime == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "The runtime is null");
            return;
        }
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (websphereServer == null || websphereServerBehaviour == null || wsRuntime == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Launch configuration could not find WebSphere server");
            return;
        }

        if (monitor.isCanceled())
            return;

        IStatus status2 = websphereServer.validate();
        if (status2 != null && !status2.isOK())
            throw new CoreException(status2);

        //setDefaultSourceLocator(launch, configuration);
        websphereServerBehaviour.setLaunch(launch);

        if (monitor.isCanceled())
            return;

        try {
            if (websphereServer.isLocalSetup()) {
                launchLocal(configuration, mode, launch, monitor);
            } else
                launchRemote(configuration, mode, launch, monitor, websphereServerBehaviour);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
        }
    }

    protected void launchLocal(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException, IOException {
        String runtimeId = configuration.getAttribute(ATTR_RUNTIME, (String) null);
        File workDir = new File(configuration.getAttribute(ATTR_WORK_DIR, (String) null));
        int userDirNum = configuration.getAttribute(ATTR_USER_DIR, 0);
        List<String> commandList = configuration.getAttribute(ATTR_COMMAND, (List<String>) null);
        String label = configuration.getAttribute(ATTR_LABEL, (String) null);
        String jvmArgs = configuration.getAttribute(ATTR_JVM_ARGS, (String) null);

        IRuntime runtime = ServerCore.findRuntime(runtimeId);
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);

        UserDirectory userDir = null;
        if (userDirNum >= 0)
            userDir = wsRuntime.getUserDirectories().get(userDirNum);

        String[] command = new String[commandList.size()];
        int i = 0;
        for (String s : commandList)
            command[i++] = s;
        ProcessBuilder pb = wsRuntime.createProcessBuilder(userDir, workDir, jvmArgs, command);
        Process p = pb.start();

        String cmd = pb.command().get(0) + " " + pb.command().get(1);
        IProcess process = newProcess(launch, p, LaunchUtil.getProcessLabel(cmd));
        process.setAttribute(IProcess.ATTR_CMDLINE, renderCmdLine(pb));
        process.setAttribute(IProcess.ATTR_PROCESS_LABEL, label);
        launch.addProcess(process);
    }

    protected void launchRemote(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor,
                                WebSphereServerBehaviour websphereServerBehaviour) throws CoreException {
        Map<String, String> commandVariables = configuration.getAttribute(ATTR_COMMAND, (Map<String, String>) null);
        int timeout = websphereServerBehaviour.getServer().getStopTimeout() * 10000;
        String utilityType = commandVariables.get(CommandConstants.UTILITY_TYPE);
        try {
            if (utilityType != null)
                if (utilityType.equals(CommandConstants.CREATE_SSL_CERTIFICATE)) {
                    RemoteCreateSSLCertificate remoteLauncher = new RemoteCreateSSLCertificate(commandVariables, timeout, new LibertyRemoteUtilityExecutionDelegate());
                    remoteLauncher.execute(websphereServerBehaviour.getWebSphereServer(), mode, launch, monitor);
                } else if (utilityType.equals(CommandConstants.PACKAGE_SERVER)) {
                    RemotePackageServer remoteLauncher = new RemotePackageServer(commandVariables, timeout, new LibertyRemoteUtilityExecutionDelegate());
                    remoteLauncher.execute(websphereServerBehaviour.getWebSphereServer(), mode, launch, monitor);
                } else if (utilityType.equals(CommandConstants.DUMP_SERVER)) {
                    RemoteDumpServer remoteLauncher = new RemoteDumpServer(commandVariables, timeout, new LibertyRemoteUtilityExecutionDelegate());
                    remoteLauncher.execute(websphereServerBehaviour.getWebSphereServer(), mode, launch, monitor);
                } else if (utilityType.equals(CommandConstants.JAVA_DUMP)) {
                    RemoteJavaDump remoteLauncher = new RemoteJavaDump(commandVariables, timeout, new LibertyRemoteUtilityExecutionDelegate());
                    remoteLauncher.execute(websphereServerBehaviour.getWebSphereServer(), mode, launch, monitor);
                } else {
                    UtilityExtension utilityExt = UtilityExtensionFactory.getExtensionClass(utilityType);
                    RemoteUtility utility = utilityExt.getRemoteUtility(commandVariables, timeout, new LibertyRemoteUtilityExecutionDelegate());
                    utility.execute(websphereServerBehaviour.getWebSphereServer(), mode, launch, monitor);
                }
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
        }
    }
}
