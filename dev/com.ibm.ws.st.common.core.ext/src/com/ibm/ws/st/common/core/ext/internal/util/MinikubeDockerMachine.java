/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;

/**
 * Represents a minikube docker machine.
 */
public class MinikubeDockerMachine extends AbstractDockerMachine {

    private static final String PATTERN_IP_ADDR = "\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}";
    private static final Pattern patternIPAddr = Pattern.compile(PATTERN_IP_ADDR);

    Map<String, String> envMap = null;
    String hostIP = null;

    /**
     * Create a new minikube docker machine
     *
     * @param platformHandler The command protocol to use
     */
    public MinikubeDockerMachine(IPlatformHandler platformHandler) {
        super(platformHandler);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRealMachine() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public MachineType getMachineType() {
        return MachineType.MINIKUBE;
    }

    /** {@inheritDoc} */
    @Override
    public String getMachineName() {
        return "minikube";
    }

    @Override
    public String getHost() throws Exception {
        if (hostIP == null) {
            getEnv();
        }
        return hostIP;
    }

    @Override
    public Map<String, String> getDockerEnv() throws Exception {
        if (envMap == null) {
            getEnv();
        }
        return envMap;
    }

    public void getEnv() throws Exception {
        ExecutionOutput result = platformHandler.executeCommand("minikube docker-env --shell cmd", DEFAULT_TIMEOUT * 2);
        int exitValue = result.getReturnCode();
        String outputStr = result.getOutput();
        String error = result.getError();
        if (exitValue != 0) {
            Trace.logError("Error getting minikube docker env. Exit value = " + Integer.valueOf(exitValue) + " output: " + outputStr + " error: "
                           + error, null);
            throw new RuntimeException(NLS.bind(Messages.errorFailedGettingDockerEnv, new String[] { getMachineName(), Integer.toString(exitValue), error.trim() }));
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Environment variables for machine " + getMachineName() + " are: " + outputStr);
        }

        Map<String, String> env = new HashMap<String, String>();
        String ip = null;
        String[] lines = outputStr.split("[\r,\f,\n]+");
        for (String line : lines) {
            String[] tokens = line.split("[ ,\t]+");
            if (tokens.length == 2 && "SET".equals(tokens[0])) {
                String[] envVar = tokens[1].split("[=]");
                if (envVar.length == 2) {
                    env.put(envVar[0], envVar[1]);
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Adding Minikube Docker environment variable name: " + envVar[0] + ", value: " + envVar[1]);
                    }
                    if (envVar[0].equals("DOCKER_HOST")) {
                        Matcher matcher = patternIPAddr.matcher(envVar[1]);
                        if (matcher.find()) {
                            int start = matcher.start();
                            int end = matcher.end();
                            ip = envVar[1].substring(start, end);
                            if (Trace.ENABLED) {
                                Trace.trace(Trace.INFO, "IP address for Minikube Docker machine: " + ip);
                            }
                        } else if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "Failed to parse out the ip address from: " + envVar[1]);
                        }
                    }
                }
            }
        }

        envMap = env;
        hostIP = ip;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getMachineName();
    }

}
