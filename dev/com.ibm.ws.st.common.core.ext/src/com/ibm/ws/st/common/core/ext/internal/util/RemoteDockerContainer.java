/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.common.core.ext.internal.util;

import java.io.IOException;
import java.net.ConnectException;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformUtil;

/**
 * A class that represents a remote docker container. This class extends the DockerContainer class
 * and should reuse most of the existing methods. Only some methods that require intermediary steps
 * are required to be defined in this class.
 */
public class RemoteDockerContainer extends BaseDockerContainer {

    /**
     * @param containerName
     * @param dockerMachine
     * @param platformHandler
     */
    public RemoteDockerContainer(String containerName, AbstractDockerMachine dockerMachine, IPlatformHandler platformHandler) {
        super(containerName, dockerMachine, platformHandler);
    }

    public RemoteDockerContainer(String containerName, String machineType, String machineName, IPlatformHandler platformHandler) {
        super(containerName, machineType, machineName, platformHandler);
    }

    /**
     * Copy a file from the host to the remote docker container
     *
     * Remote docker requires intermediary steps (eg. copying the files to the remote machine before
     * executing the docker command to copy the file into the docker machine.
     *
     * @param file Host file
     *
     * @param destinationPath Destination path in docker container
     *
     * @throws ConnectException
     *
     * @throws IOException
     */
    @Override
    public void copyIn(String sourcePath, String destinationPath) throws Exception {
        try {
            platformHandler.startSession();

            // get the temp directory on the remote system
            String tempDir = platformHandler.getTempDir();
            // create the path of the temp file
            String tempPath = tempDir + getFileNameFromPath(sourcePath);
            // upload a copy of the local file to a temp file on the remote system
            platformHandler.uploadFile(sourcePath, tempPath);
            // execute the docker command to copy into the docker machine
            super.copyIn(tempPath, destinationPath);
            // delete the temp file
            platformHandler.startSession();
            platformHandler.deleteFile(tempPath);
        } catch (Exception e) {
            Trace.logError("Failed to copy the file " + sourcePath + " to " + destinationPath, e);
        } finally {
            platformHandler.endSession();
        }
    }

    /**
     * Copy a file from the remote docker container to the host
     *
     * Remote docker requires intermediary steps (eg. executing the docker command
     * to copy the file out of the docker machine and then downloading the file to
     * the local machine)
     *
     * @param sourcePath The path within the docker container
     * @param destinationPath The destination path on the host
     * @param timeout the timeout from running the command
     * @return ProcessResult the result of running the copy out
     * @throws IOException
     */
    @Override
    public ExecutionOutput copyOut(String sourcePath, String destinationPath, long timeout) throws ConnectException, IOException {
        try {
            platformHandler.startSession();
            String filename = getFileNameFromPath(destinationPath);
            String modifiedDestinationPath = platformHandler.getTempDir() + filename;
            if (platformHandler.getOS() == PlatformUtil.OperatingSystem.MAC) {
                // Bug with Docker on MAC - need to touch the file first or Docker complains
                // that the destination is not a directory.
                platformHandler.executeCommand("touch " + modifiedDestinationPath);
            }
            ExecutionOutput output = super.copyOut(sourcePath, modifiedDestinationPath, timeout);
            platformHandler.startSession();
            if (!platformHandler.fileExists(modifiedDestinationPath))
                throw new ConnectException(NLS.bind(Messages.errorFailedDockerCommand,
                                                    new String[] { Integer.toString(output.getReturnCode()), output.getOutput(), output.getError() }));
            platformHandler.downloadFile(modifiedDestinationPath, destinationPath);
            return output;
        } catch (Exception e) {
            throw new IOException(NLS.bind(Messages.errorCopyOut, sourcePath, destinationPath), e);
        } finally {
            platformHandler.endSession();
        }
    }

}
