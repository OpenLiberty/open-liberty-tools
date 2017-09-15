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
package com.ibm.ws.st.docker.core.internal;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.CommandConstants;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.config.DOMUtils;
import com.ibm.ws.st.core.internal.jmx.JMXConnection;
import com.ibm.ws.st.core.internal.launch.IUtilityExecutionDelegate;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerServer;

/**
 *
 */
public class LibertyLocalDockerUtilityExecutionDelegate implements IUtilityExecutionDelegate {

    private IProgressMonitor monitor2;
    private WebSphereServer wsServer;
    private Map<String, String> serviceInfo;
    private LibertyDockerServer serverExt;
    private String configPath;
    private IPath remoteUserDir;
    BaseDockerContainer container;
    private final int amountOfWork = 5;
    protected final long TIMEOUT = (10 * 60 * 1000) + AbstractDockerMachine.DEFAULT_TIMEOUT;

    @Override
    public void initialize(IServer server, IProgressMonitor monitor) {
        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Start to execute.");
        }

        monitor2 = monitor;
        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();
        monitor2.beginTask(NLS.bind(com.ibm.ws.st.docker.core.internal.Messages.L_ExecutingCommands, server.getName()), amountOfWork);
        monitor2.subTask(NLS.bind(com.ibm.ws.st.docker.core.internal.Messages.L_ExecutingCommands, server.getName()));

        wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, monitor2);

        serviceInfo = wsServer.getServiceInfo();

        serverExt = (LibertyDockerServer) wsServer.getAdapter(LibertyDockerServer.class);

        if (serviceInfo == null || serverExt == null) {
            // THROW exception and log it for all the properties here and below
        }

        configPath = serviceInfo.get(Constants.LIBERTY_SERVER_CONFIG_PATH);

        if (configPath != null) {
            remoteUserDir = new Path(configPath).removeTrailingSeparator();
            try {
                container = serverExt.getContainer(wsServer);
            } catch (UnsupportedServiceException e) {
                Trace.logError("Failed to initialize docker container", e);
            }
        }

        monitor2.worked(1);

    }

    public String getDockerRemoteOutputDir() throws Exception {
        JMXConnection jmx = wsServer.createJMXConnection();

        CompositeData metadata = (CompositeData) jmx.getMetadata("${server.output.dir}", "a");
        String remoteOutputDir = (String) metadata.get("fileName");
        remoteOutputDir = remoteOutputDir.replace("\\", "/");
        return remoteOutputDir;
    }

    /**
     * Execute a docker command with a progress monitor. The cancel stops the monitor from being holding up the UI,
     * but does not terminate the docker command, so the job will still be running
     *
     * @param wsServer the server
     * @param execCmd the command to execute in docker
     * @param jobName the name of the job
     * @param timeout the timeout in milliseconds
     * @param monitor the progress monitor
     * @return the ProcessResult, or null if the monitor was cancelled
     * @throws CoreException
     */
    @Override
    public ExecutionOutput execute(WebSphereServer server, String launchMode, ILaunch launch, String execCmd, String jobName, long timeout,
                                   IProgressMonitor monitor, boolean run) throws CoreException {
        IProgressMonitor monitor2 = monitor;

        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        if (server == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Beginning job: " + jobName);
        monitor2.beginTask(NLS.bind(jobName, server.getServerName()), IProgressMonitor.UNKNOWN);

        final ExecutionOutput[] fJobResult = new ExecutionOutput[1];

        LibertyDockerServer serverExt = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        Map<String, String> serviceInfo = server.getServiceInfo();

        if (serverExt == null || serviceInfo == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorDockerInfo));
        }

        String installPath = serviceInfo.get(Constants.LIBERTY_RUNTIME_INSTALL_PATH);

        if (container == null || installPath == null) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorDockerInfo));
        }

        Path pInstallpath = new Path(installPath);
        installPath = pInstallpath.removeTrailingSeparator().toString();

        final String binPath = installPath + "/bin/";

        final String fCommand = binPath + execCmd;
        final long fTimeout = timeout;
        final ConnectException[] connectException = new ConnectException[1];

        Job job = new Job(jobName) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Executing docker command: " + fCommand);
                    fJobResult[0] = container.dockerExec(fCommand, true, fTimeout);
                } catch (ConnectException e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error executing docker command", e);
                    connectException[0] = e;
                }

                return Status.OK_STATUS;
            }

        };

        long startTime = System.currentTimeMillis();

        final Runner running = new Runner();

        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                running.setDone(true);
            }
        });

        job.schedule();

        // Keep waiting until the job is done, the monitor was cancelled, or the time out was reached
        while (!running.isDone && !monitor2.isCanceled() && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        if (fJobResult[0] == null && connectException[0] != null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "ProcessResult is null");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorDockerInfo, connectException[0]));
        }

        if (monitor2.isCanceled()) {
            return null;
        }

        monitor2.done();
        int returnCode = fJobResult[0] == null ? -1 : fJobResult[0].getReturnCode();
        if (returnCode == 0) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Successfully completed command: " + fCommand);
            return fJobResult[0];
        }

        Trace.logError("Execution failed for the command: " + execCmd + ", with output: " + fJobResult[0].getOutput() + ", and error" + fJobResult[0].getError(), null);
        String msg;
        if (fJobResult[0] != null && fJobResult[0].getError() != null && !fJobResult[0].getError().isEmpty()) {
            msg = NLS.bind(Messages.W_RemoteServer_CommandReturnCodeWithError, new String[] { execCmd, Integer.toString(returnCode), fJobResult[0].getError() });
        } else {
            msg = NLS.bind(Messages.W_RemoteServer_CommandReturnCode, new String[] { execCmd, Integer.toString(returnCode) });
        }
        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg));

    }

    public static ExecutionOutput copyOut(WebSphereServer wsServer, String sourcePath, String destinationPath, String jobName, long timeout,
                                          IProgressMonitor monitor) throws CoreException, UnsupportedServiceException {
        IProgressMonitor monitor2 = monitor;

        if (monitor2 == null)
            monitor2 = new NullProgressMonitor();

        if (wsServer == null) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Could not find server");
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorPromptServerNotFound));
        }

        monitor2.beginTask(NLS.bind(jobName, wsServer.getServerName()), IProgressMonitor.UNKNOWN);

        LibertyDockerServer serverExt = (LibertyDockerServer) wsServer.getAdapter(LibertyDockerServer.class);
        Map<String, String> serviceInfo = wsServer.getServiceInfo();

        if (serverExt == null || serviceInfo == null) {
            Trace.logError("The server information could not be retrieved", null);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorDockerInfo));
        }

        final BaseDockerContainer container = serverExt.getContainer(wsServer);

        if (container == null) {
            Trace.logError("A container could not be created for server: " + wsServer.getServerName(), null);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorDockerInfo));
        }

        final long fTimeout = timeout;

        final String fSourcePath = sourcePath;
        final String fDestinationPath = destinationPath;
        final ExecutionOutput[] jobResult = new ExecutionOutput[1];

        Job job = new Job(jobName) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    jobResult[0] = container.copyOut(fSourcePath, fDestinationPath, fTimeout);
                } catch (Exception e) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.WARNING, "Error executing docker command", e);
                }

                return Status.OK_STATUS;
            }

        };

        long startTime = System.currentTimeMillis();

        final Runner running = new Runner();

        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                running.setDone(true);
            }
        });

        job.schedule();

        // Keep waiting until the job is done, the monitor was cancelled, or the time out was reached
        while (!running.isDone && !monitor2.isCanceled() && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        if (monitor2.isCanceled()) {
            return null;
        }

        monitor2.done();

        return jobResult[0];
    }

    /**
     * Given output (e.g. from ProcessResult), get the full zip file location on docker
     *
     * @param installPath
     * @param zipName
     * @return the path or null if something went wrong
     */
    public static String getZipFromOutput(String installPath, String zipName, String output) {
        String result = null;

        int start = output.indexOf(installPath);
        int end = output.indexOf(zipName);
        int len = zipName.length();

        if (start < end && len > 0) {
            result = output.substring(start, end + len);
        }

        return result;
    }

    public static class Runner {
        protected boolean isDone = false;

        public boolean getIsDone() {
            return isDone;
        }

        public void setDone(boolean value) {
            isDone = value;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getServerSecurityUtilityFilePath() {
        return CommandConstants.CREATE_SSL_CERTIFICATE_SECURITY_UTIL + " ";
    }

    /** {@inheritDoc} */
    @Override
    public String getServerScriptFilePath() {
        // TODO Auto-generated method stub
        return CommandConstants.SERVER + " ";
    }

    /** {@inheritDoc} */
    @Override
    public String getServerUtilityFilePath(String command) {
        return command + " ";
    }

    /** {@inheritDoc} */
    @Override
    public boolean fileExists(String remoteFilePath) throws Exception {

        return container.fileExists(remoteFilePath);
    }

    /**
     * {@inheritDoc}
     *
     * @throws ConnectException
     */
    @Override
    public void renameFile(String remoteOldFileName, String newName) throws Exception {
        container.renameFile(remoteOldFileName, newName);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteFile(String remoteFilePath, boolean recursiveDelete, boolean force) throws Exception {
        container.deleteFile(remoteFilePath);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void modifyConfigFile(String includeFileName) throws Exception {
        ConfigurationFile serverConfig = wsServer.getServerInfo().getConfigRoot();
        boolean includeExists = false;
        //remove existing keystore element if any
        if (serverConfig.hasElement(com.ibm.ws.st.core.internal.Constants.KEY_STORE)) {
            serverConfig.removeElement(com.ibm.ws.st.core.internal.Constants.KEY_STORE);
        }

        // Include the generated config file in the server config if it isn't already there
        for (ConfigurationFile config : serverConfig.getAllIncludedFiles()) {
            if (config.getPath().lastSegment().equals(includeFileName)) {
                includeExists = true;
                break;
            }
        }

        // Add the include to the server config if it isn't already added
        if (!includeExists) {
            serverConfig.addInclude(false, includeFileName);
            try {
                serverConfig.save(null);
            } catch (IOException e) {
                Trace.logError("Error occured while executing join collective command for remote server", e);
            }
        }

        // In the remote case, JMX is used to synchronize the server.xml (calls wsServer.createJMXConnection();). But, this causes a large amount of white space to be generated
        // before the include statement. Instead, just copy the server.xml from the local to the Liberty docker
        try {
            container.copyIn((getLocalUserDir().append(com.ibm.ws.st.core.internal.Constants.SERVER_XML)).toOSString(),
                             remoteUserDir.append(com.ibm.ws.st.core.internal.Constants.SERVER_XML).toString());
        } catch (ConnectException e) {
            Trace.logError("Could not sync local server.xml with server.xml on docker", e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void uploadFile(String localFile, String destinationFile) throws Exception {
        container.copyIn(localFile, destinationFile);
    }

    /** {@inheritDoc} */
    @Override
    public void downloadFile(IPath remoteFile, String destFile, IProgressMonitor monitor) throws CoreException {
        try {
            copyOut(wsServer, getOSString(remoteFile.toString()), destFile, "ToDo", TIMEOUT, monitor);
        } catch (UnsupportedServiceException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean matchPasswordField(IPath filePath, String expectedPassword) {
        boolean passwordsMatch = true;
        try {
            ConfigurationFile file = new ConfigurationFile(filePath.toFile().toURI(), wsServer.getUserDirectory(), wsServer.getServerInfo());
            Document doc = file.getDocument();
            if (doc != null) {
                Element elem = doc.getDocumentElement();
                Element keyStoreElem = DOMUtils.getFirstChildElement(elem, com.ibm.ws.st.core.internal.Constants.KEY_STORE);
                if (keyStoreElem != null) {
                    String actualPassword = PasswordUtil.decode(keyStoreElem.getAttribute(com.ibm.ws.st.core.internal.Constants.PASSWORD_TYPE));
                    if (!actualPassword.equals(expectedPassword)) {
                        file.setAttribute(com.ibm.ws.st.core.internal.Constants.KEY_STORE, com.ibm.ws.st.core.internal.Constants.PASSWORD_TYPE, expectedPassword);
                        file.save(null);
                        passwordsMatch = false;
                    }
                }
            }
        } catch (Exception e) {
            Trace.logError("Unable to fetch the keystore password in file: " + filePath, e);
        }
        return passwordsMatch;
    }

    /** {@inheritDoc} */
    @Override
    public IPath getLocalUserDir() {
        return wsServer.getServerInfo().getUserDirectory().getProject().getLocation().append(com.ibm.ws.st.core.internal.Constants.SERVERS_FOLDER).append(wsServer.getServerName());

    }

    @Override
    public IPath getRemoteOutputDir() throws Exception {
        JMXConnection jmx = wsServer.createJMXConnection();

        CompositeData metadata = (CompositeData) jmx.getMetadata("${server.output.dir}", "a");
        String remoteOutputDir = (String) metadata.get("fileName");
        remoteOutputDir = remoteOutputDir.replace("\\", "/");
        return new Path(remoteOutputDir);
    }

    /** {@inheritDoc} */
    @Override
    public IPath getRemoteUserDir() {
        return remoteUserDir;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDockerExecutionDelegate() {
        return true;
    }

    @Override
    public boolean isExecutionSuccessful(ExecutionOutput pr, String expectedSysOut) throws CoreException {
        if (pr == null || pr.getReturnCode() != 0)
            return false;
        if (pr.getReturnCode() == 0 && !pr.getOutput().contains(expectedSysOut))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void startExecution() throws CoreException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void endExecution() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getOSString(String path) {
        String osPath = path;
        osPath = path.replace("\\", "/");
        return osPath;
    }

}
