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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;

/**
 * Represents a docker machine.
 */
public class DockerMachine extends AbstractDockerMachine {

    private final String machineName;

    /**
     * Create a new docker machine
     *
     * @param machineName The machine name
     * @param platformHandler The command protocol to use
     */
    public DockerMachine(String machineName, IPlatformHandler platformHandler) {
        super(platformHandler);
        this.machineName = machineName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRealMachine() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public MachineType getMachineType() {
        return MachineType.STANDARD;
    }

    /** {@inheritDoc} */
    @Override
    public String getMachineName() {
        return machineName;
    }

    @Override
    public String getHost() throws Exception {
        ExecutionOutput result = platformHandler.executeCommand("docker-machine ip " + machineName, DEFAULT_TIMEOUT);
        int exitValue = result.getReturnCode();
        String outputStr = result.getOutput();
        String error = result.getError();
        if (exitValue != 0) {
            Trace.logError("Error getting docker env for machine " + machineName + ". Exit value = " + Integer.valueOf(exitValue) + " output: " + outputStr + " error: "
                           + error, null);
            throw new RuntimeException(NLS.bind(Messages.errorFailedGettingDockerEnv, new String[] { machineName, Integer.toString(exitValue), error }));
        }
        String host = outputStr.trim();
        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Host for machine " + machineName + " is: " + host);
        }
        return host;
    }

    @Override
    public Map<String, String> getDockerEnv() throws Exception {
        ExecutionOutput result = platformHandler.executeCommand("docker-machine env --shell cmd " + machineName, DEFAULT_TIMEOUT * 2);
        int exitValue = result.getReturnCode();
        String outputStr = result.getOutput();
        String error = result.getError();
        if (exitValue != 0) {
            Trace.logError("Error getting docker env for machine " + machineName + ". Exit value = " + Integer.valueOf(exitValue) + " output: " + outputStr + " error: "
                           + error, null);
            throw new RuntimeException(NLS.bind(Messages.errorFailedGettingDockerEnv, new String[] { machineName, Integer.toString(exitValue), error.trim() }));
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Environment variables for machine " + machineName + " are: " + outputStr);
        }

        Map<String, String> env = new HashMap<String, String>();
        String[] lines = outputStr.split("[\r,\f,\n]+");
        for (String line : lines) {
            String[] tokens = line.split("[ ,\t]+");
            if (tokens.length == 2 && "SET".equals(tokens[0])) {
                String[] envVar = tokens[1].split("[=]");
                if (envVar.length == 2) {
                    env.put(envVar[0], envVar[1]);
                }
            }
        }

        return env;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return machineName;
    }

}
