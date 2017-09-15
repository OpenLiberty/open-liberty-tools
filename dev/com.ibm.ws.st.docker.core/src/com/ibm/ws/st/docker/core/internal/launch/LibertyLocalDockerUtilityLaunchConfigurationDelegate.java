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
package com.ibm.ws.st.docker.core.internal.launch;

import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.UtilityExtension;
import com.ibm.ws.st.core.internal.UtilityExtensionFactory;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.LibertyRemoteUtilityExecutionDelegate;
import com.ibm.ws.st.core.internal.launch.RemoteCreateSSLCertificate;
import com.ibm.ws.st.core.internal.launch.RemoteDumpServer;
import com.ibm.ws.st.core.internal.launch.RemoteJavaDump;
import com.ibm.ws.st.core.internal.launch.RemoteUtility;
import com.ibm.ws.st.docker.core.internal.LibertyLocalDockerUtilityExecutionDelegate;
import com.ibm.ws.st.docker.core.internal.Messages;

/**
 * The launch that handles all utilities for docker. Once the launch kicks off, it will pass a utility type
 * into the launch, which it will deal with here
 */
public class LibertyLocalDockerUtilityLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

    public static final String ATTR_SERVER_ID = "serverID";
    public static final String ATTR_COMMAND = "command";

    protected Map<String, String> serviceInfo = null;
    protected LibertyDockerServer serverExt = null;
    protected String containerName = null;
    protected String machineName = null;
    protected String osName = null;
    protected IPlatformHandler platformHandler = null;
    protected String configPath = null;
    protected Path installPath = null;
    protected AbstractDockerMachine machine = null;
    protected BaseDockerContainer container = null;
    protected IPath remoteUserDir = null;
    protected WebSphereServer wsServer;
    protected WebSphereServerBehaviour wsBehaviour;
    protected int amountOfWork = 5;
    protected Map<String, String> commandVariables = null;

    // Timeout is 10 minutes
    protected final long TIMEOUT = (10 * 60 * 1000) + AbstractDockerMachine.DEFAULT_TIMEOUT;

    // Based on com.ibm.ws.st.core.internal.launch.UtilityLaunchConfigurationDelegate.launch(ILaunchConfiguration, String, ILaunch, IProgressMonitor)
    /** {@inheritDoc} */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Launch.");
        }

        IServer server = null;
        String serverID = configuration.getAttribute(ATTR_SERVER_ID, (String) null);

        if (serverID != null) {
            server = ServerCore.findServer(serverID);
        }

        if (server == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Launch configuration could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.E_DockerServerCannotBeFound));
        }

        if (monitor.isCanceled())
            return;

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
            // throw CoreException();
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

        commandVariables = configuration.getAttribute(ATTR_COMMAND, (Map<String, String>) null);

        String utilityType = commandVariables.get(CommandConstants.UTILITY_TYPE);
        try {
            if (utilityType != null) {
                if (CommandConstants.CREATE_SSL_CERTIFICATE.equals(utilityType)) {
                    RemoteCreateSSLCertificate utility = new RemoteCreateSSLCertificate(commandVariables, (int) TIMEOUT, new LibertyLocalDockerUtilityExecutionDelegate());
                    utility.execute(websphereServer, launch.getLaunchMode(), launch, monitor);
                } else if (CommandConstants.DUMP_SERVER.equals(utilityType)) {
                    RemoteDumpServer utility = new RemoteDumpServer(commandVariables, (int) TIMEOUT, new LibertyLocalDockerUtilityExecutionDelegate());
                    utility.execute(websphereServer, launch.getLaunchMode(), launch, monitor);
                } else if (CommandConstants.JAVA_DUMP.equals(utilityType)) {
                    RemoteJavaDump utility = new RemoteJavaDump(commandVariables, (int) TIMEOUT, new LibertyLocalDockerUtilityExecutionDelegate());
                    utility.execute(websphereServer, launch.getLaunchMode(), launch, monitor);
                } else {
                    UtilityExtension utilityExt = UtilityExtensionFactory.getExtensionClass(utilityType);
                    if (utilityExt != null) {
                        RemoteUtility utility = utilityExt.getRemoteUtility(commandVariables, (int) TIMEOUT, new LibertyRemoteUtilityExecutionDelegate());
                        if (utility != null) {
                            utility.execute(websphereServerBehaviour.getWebSphereServer(), mode, launch, monitor);
                        } else {
                            Trace.logError("The utility extension delegate for the " + utilityType + " utility did not provide a remote handler.", null);
                        }
                    } else {
                        Trace.logError("No utility extension delegate was found for the " + utilityType + " utility.", null);
                    }
                }
            }
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage(), e));
        }
    }

    public String getDockerRemoteOutputDir() throws Exception {
        JMXConnection jmx = wsServer.createJMXConnection();

        CompositeData metadata = (CompositeData) jmx.getMetadata("${server.output.dir}", "a");
        String remoteOutputDir = (String) metadata.get("fileName");
        remoteOutputDir = remoteOutputDir.replace("\\", "/");
        return remoteOutputDir;
    }

    class Running {
        protected boolean isDone = false;

        public boolean getIsDone() {
            return isDone;
        }

        public void setDone(boolean value) {
            isDone = value;
        }
    }

}
