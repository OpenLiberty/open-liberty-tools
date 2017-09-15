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

package com.ibm.ws.st.core.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.core.internal.WebSphereServer;

/**
 *
 */
public interface IUtilityExecutionDelegate {

    public void startExecution() throws CoreException;

    public void endExecution();

    /**
     * executes command with specified timeout
     * return ProcessResult with exitvalue and sysout.
     */
    ExecutionOutput execute(WebSphereServer wsServer, String launchMode, ILaunch launch, String execCmd, String jobName, long timeout, IProgressMonitor monitor,
                            boolean run) throws CoreException;

    /**
     * initializes execution delegate before each execution
     */
    void initialize(IServer server, IProgressMonitor monitor) throws CoreException;

    /**
     * return security utility path
     */
    String getServerSecurityUtilityFilePath();

    String getServerScriptFilePath();

    String getServerUtilityFilePath(String command);

    /**
     * checks if the remote file path exists
     */
    boolean fileExists(String remoteFilePath) throws Exception;

    /**
     * renames remote file
     */
    void renameFile(String remoteOldFileName, String newName) throws Exception;

    /**
     * deletes remote File
     */
    void deleteFile(String remoteFilePath, boolean recursiveDelete, boolean force) throws Exception;

    /**
     * modifies server.xml after each execution
     */
    void modifyConfigFile(String includeFileName) throws Exception;

    void uploadFile(String localFile, String destinationFile) throws Exception;

    void downloadFile(IPath remoteFile, String destFile, IProgressMonitor monitor) throws CoreException;

    boolean matchPasswordField(IPath filePath, String expectedPassword);

    IPath getLocalUserDir();

    /**
     * returns remote user directory
     */
    IPath getRemoteUserDir();

    /**
     * returns remote output directory. Usually it is same as remote user directory unless set by environment variable
     * remote output directory is set /opt/ibm/wlp/output in docker
     */
    IPath getRemoteOutputDir() throws Exception;

    /**
     * returns true for Docker Utility Excuetion delegate
     */
    boolean isDockerExecutionDelegate();

    /**
     * check exitValue and sysout to determine if the execution was successful
     */
    boolean isExecutionSuccessful(ExecutionOutput pr, String expectedSysOut) throws CoreException;

    String getOSString(String path);

}
