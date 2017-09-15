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
package com.ibm.ws.st.common.core.ext.internal.util;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;

public interface IRemoteUtilityExecutionDelegate {

    public void init(int logonMethod, String id, String password, String hostName, boolean isWindowsPlatform, String sshKeyFile);

    public void startExecution() throws CoreException;

    public void endExecution();

    public ExecutionOutput execCommand(ILaunch launch, String command, int timeout, String jvmArgs, Map<String, String> envMap,
                                       IProgressMonitor monitor) throws CoreException;

    public ExecutionOutput runCommand(String command, int timeout, String jvmArgs, Map<String, String> envMap, IProgressMonitor monitor) throws CoreException;

    public boolean fileExists(String remoteFilePath) throws CoreException;

    public String[] listFiles(String remoteDirectory) throws CoreException;

    public long getFileSize(String remoteDirectory, String fileName) throws CoreException;

    public long getTimeStamp(String remoteFileLocation) throws CoreException;

    public void renameFile(String remoteOldFileName, String newName) throws CoreException;

    public void deleteFile(String remoteFilePath, boolean recursiveDelete, boolean force) throws CoreException;

    public void downloadFile(IPath remoteFile, String destFile, IProgressMonitor monitor) throws CoreException;

    public void uploadFile(String localFile, String destinationFile) throws CoreException;
}
