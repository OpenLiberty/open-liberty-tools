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

package com.ibm.ws.st.common.core.ext.internal.setuphandlers;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformUtil.OperatingSystem;

public interface IPlatformHandler {

    public abstract void startSession() throws Exception;

    public abstract boolean directoryExists(String path) throws Exception;

    public abstract boolean fileExists(String filePath) throws Exception;

    public void createDirectory(String path) throws ConnectException, Exception;

    public void uploadFile(String sourcePath, String destinationPath) throws Exception;

    public void downloadFile(String sourcePath, String destinationPath) throws Exception;

    public ExecutionOutput executeCommand(String command) throws Exception;

    public ExecutionOutput executeCommand(String command, long timeout) throws Exception;

    public ExecutionOutput executeCommand(Map<String, String> cmdEnv, String command, long timeout) throws Exception;

    public ExecutionOutput executeCommand(Map<String, String> env, String command, long timeout, IProgressMonitor monitor) throws Exception;

    public void endSession() throws ConnectException;

    public String getEnvValue(String key) throws ConnectException, IOException;

    public String getTempDir() throws Exception;

    public void deleteFile(String path) throws Exception;

    public static class ExecutionOutput {
        private final int returnCode;
        private final String output;
        private final String error;

        public ExecutionOutput(int returnCode, String output, String error) {
            this.returnCode = returnCode;
            this.output = output == null ? "" : output;
            this.error = error == null ? "" : error;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

    }

    public OperatingSystem getOS() throws Exception;
}
