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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.LocalHandler;

/**
 * Abstract docker machine class. Contains convenience methods for interacting directly with a
 * docker machine. Interactions with containers should be done through DockerContainer classes.
 */
public abstract class AbstractDockerMachine {

    public static long DEFAULT_TIMEOUT = 10000;

    static {
        String timeout = System.getProperty("com.ibm.ws.st.DockerCommandTimeoutInSeconds");
        if (timeout != null) {
            try {
                DEFAULT_TIMEOUT = Long.parseLong(timeout);
                DEFAULT_TIMEOUT = DEFAULT_TIMEOUT * 1000;
            } catch (NumberFormatException e) {
                Trace.logError("The Docker command timeout value is not a valid integer: " + timeout, e);
            }
        }
    }

    protected IPlatformHandler platformHandler;

    public enum MachineType {
        STANDARD,
        PHANTOM,
        MINIKUBE;

        public static MachineType getMachineType(String name) {
            for (MachineType type : MachineType.values()) {
                if (type.name().equals(name))
                    return type;
            }
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "The " + name + " machine type is not recognized.");
            }
            return null;
        }
    }

    public static MachineType getMachineType(String typeName, String machineName) {
        MachineType type = MachineType.getMachineType(typeName);
        if (type == null) {
            // If an old workspace is used the type will be null.  Revert to the old way of determining
            // the machine type.  If the machine name is not null then standard, otherwise phantom.
            type = machineName != null ? MachineType.STANDARD : MachineType.PHANTOM;
        }
        return type;
    }

    public AbstractDockerMachine(IPlatformHandler platformHandler) {
        this.platformHandler = platformHandler;
    }

    public static boolean isDockerInstalled(IPlatformHandler platformHandler) {
        if (hasNativeDocker(platformHandler)) {
            return true;
        }

        if (hasDockerMachine(platformHandler)) {
            return true;
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.INFO, "Could not detect that Docker is installed.");
        }
        return false;
    }

    public static boolean hasNativeDocker(IPlatformHandler platformHandler) {
        String cmd = "docker ps";
        try {
            ExecutionOutput result = runCommand(cmd, true, platformHandler);
            if (Trace.ENABLED) {
                cmd = "docker version";
                try {
                    result = runCommand(cmd, true, platformHandler);
                    Trace.trace(Trace.INFO, "Docker is running natively at version: " + result.getOutput());
                } catch (Exception e) {
                    Trace.trace(Trace.INFO, "Execution of '" + cmd + "' failed.", e);
                }
            }
            return true;
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Execution of '" + cmd + "' failed.", e);
        }
        return false;
    }

    public static boolean hasDockerMachine(IPlatformHandler platformHandler) {
        String cmd = "docker-machine version";
        try {
            ExecutionOutput result = runCommand(cmd, true, platformHandler);
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Docker toolbox is running at version: " + result.getOutput());
            }
            return true;
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Execution of '" + cmd + "' failed.", e);
        }
        return false;
    }

    public static boolean hasMiniKubeDockerMachine(IPlatformHandler platformHandler) {
        String cmd = "minikube docker-env --shell cmd";
        try {
            ExecutionOutput result = runCommand(cmd, true, platformHandler);
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "Minikube docker env: " + result.getOutput());
            }
            return true;
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.INFO, "Execution of '" + cmd + "' failed.", e);
        }
        return false;
    }

    /**
     * Create a docker machine.
     *
     * @param machineType The machine type. The name must match one of the MachineType
     *            enum names.
     * @param machineName Machine name. May be null if the platform does
     *            not require a docker machine (such as Linux)
     * @param platformHandler The protocol to use for executing commands
     * @return The appropriate docker machine for the given operating system
     * @throws IOException
     * @throws ConnectException
     */
    public static AbstractDockerMachine createDockerMachine(String machineType, String machineName, IPlatformHandler platformHandler) {
        MachineType type = getMachineType(machineType, machineName);
        return createDockerMachine(type, machineName, platformHandler);
    }

    public static AbstractDockerMachine createDockerMachine(MachineType type, String machineName, IPlatformHandler platformHandler) {
        switch (type) {
            case STANDARD:
                return new DockerMachine(machineName, platformHandler);
            case PHANTOM:
                return new PhantomDockerMachine(platformHandler);
            case MINIKUBE:
                return new MinikubeDockerMachine(platformHandler);
            default:
                return new PhantomDockerMachine(platformHandler);
        }
    }

    /**
     * Check if this is a real docker machine. When docker runs natively there is no actual machine
     * and this will return false.
     *
     * An implementation of this class is still used when there is no real machine so that the code
     * does not need to check if the machine is null everywhere (see PhantomeDockerMachine).
     *
     * @return True if this is a real docker machine, false if docker runs natively and there is no
     *         docker machine.
     */
    public abstract boolean isRealMachine();

    /**
     * Get the machine type of this machine.
     *
     * @return The machine type
     */
    public abstract MachineType getMachineType();

    /**
     * Get the machine name.
     *
     * @return The machine name or null if docker runs natively and there is no machine.
     */
    public abstract String getMachineName();

    /**
     * Get the host name or ip address for the docker machine.
     *
     * @return The host name or ip address
     * @throws Exception
     */
    public abstract String getHost() throws Exception;

    /**
     * Get the command execution environment for the docker machine.
     *
     * @return Map of environment variables and values required to execute
     *         docker commands for this docker machine
     * @throws Exception
     */
    public abstract Map<String, String> getDockerEnv() throws Exception;

    /**
     * Get the command protocol for this docker machine
     *
     * @return The command protocol
     */
    public IPlatformHandler getplatformHandler() {
        return platformHandler;
    }

    /**
     * Get all running or all defined containers for this machine.
     *
     * @param allDefined Whether to return all defined containers or only those
     *            that are running.
     * @return A list of running or defined containers
     * @throws Exception
     */
    public List<BaseDockerContainer> getContainers(boolean allDefined) throws Exception {
        List<String> names = getContainerNames(allDefined);
        List<BaseDockerContainer> containers = new ArrayList<BaseDockerContainer>(names.size());
        for (String name : names) {
            if (platformHandler instanceof LocalHandler)
                containers.add(new BaseDockerContainer(name, this, platformHandler));
            else
                containers.add(new RemoteDockerContainer(name, this, platformHandler));

        }
        return containers;
    }

    /**
     * Get the names of running or all defined containers for this machine.
     *
     * @param allDefined Whether to return the names of all defined containers
     *            or only those that are running.
     * @return A list of running or defined container names.
     * @throws Exception
     */
    public List<String> getContainerNames(boolean allDefined) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("docker ps");
        if (allDefined) {
            builder.append(" -a");
        }
        builder.append(" --format {{.Names}}");
        ExecutionOutput result = runCommand(builder.toString(), true);
        String[] names = result.getOutput().split("[\r\n]+");
        List<String> list = new ArrayList<String>(names.length);
        for (String name : names) {
            String containerName = name.trim();
            if (!containerName.isEmpty()) {
                list.add(containerName);
            }
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Found the following containers: " + list);
        }
        return list;
    }

    /**
     * Get the names of all the images on this machine.
     *
     * @return The names of the images
     * @throws Exception
     */
    public List<String> getImages() throws Exception {
        List<String> images = new ArrayList<String>();
        ExecutionOutput result = runCommand("docker images", true);
        String[] lines = result.getOutput().split("[\r\n]+");
        if (lines.length < 1)
            return images;
        // Skip the first line as it contains the headers for the table.
        // The image name is in the first column.
        for (int i = 1; i < lines.length; i++) {
            String[] entries = lines[i].split("\\s");
            if (entries.length > 0) {
                images.add(entries[0]);
            }
        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Found the following images: " + images);
        }
        return images;
    }

    /**
     * Remove an image
     *
     * @param imageName The image name
     * @throws Exception
     */
    public void removeImage(String imageName) throws Exception {
        String cmd = "docker rmi " + imageName;
        runCommand(cmd, true);
    }

    /**
     * Remove a container
     *
     * @param containerName The container name
     * @throws Exception
     */
    public void removeContainer(String containerName) throws Exception {
        removeContainer(containerName, false);
    }

    /**
     * Remove a container
     *
     * @param containerName The container name
     * @throws Exception
     */
    public void removeContainer(String containerName, boolean force) throws Exception {
        String cmd;
        if (force) {
            cmd = "docker rm -f " + containerName;
        } else {
            cmd = "docker rm " + containerName;
        }
        runCommand(cmd, true);
    }

    /**
     * Get the size of the given image
     *
     * @param imageName The image name
     * @return The size of the image
     * @throws Exception
     */
    public long getImageSize(String imageName) throws Exception {
        String cmd = "docker inspect --format {{.Size}} " + imageName;
        ExecutionOutput result = runCommand(cmd, true);
        String output = result.getOutput().trim();
        long size = Long.parseLong(output);
        return size;
    }

    /**
     * Get a list of docker machines
     *
     * @param platformHandler The command protocol to use
     * @return A list of docker machines
     * @throws Exception
     */
    public static List<AbstractDockerMachine> getDockerMachines(IPlatformHandler platformHandler) throws Exception {
        List<AbstractDockerMachine> machineList = new ArrayList<AbstractDockerMachine>();
        if (hasNativeDocker(platformHandler)) {
            machineList.add(createDockerMachine(MachineType.PHANTOM, null, platformHandler));
        }
        if (hasDockerMachine(platformHandler)) {
            ExecutionOutput result = runCommand("docker-machine ls", true, platformHandler);
            String[] machines = result.getOutput().split("[\r\n]+");
            for (int i = 1; i < machines.length; i++) {
                if (!machines[i].trim().isEmpty()) {
                    String[] machineInfo = machines[i].split("[ \t]+");
                    String name = machineInfo[0];
                    machineList.add(createDockerMachine(MachineType.STANDARD, name, platformHandler));
                }
            }
        }
        // Disable minikube support
//        if (hasMiniKubeDockerMachine(platformHandler)) {
//            machineList.add(createDockerMachine(MachineType.MINIKUBE, null, platformHandler));
//        }

        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Found the following machines: " + machineList);
        }
        return machineList;
    }

    /**
     * Run the provided command on the docker machine. An exception is thrown
     * if the command results in an unexpected exit value.
     *
     * @param command the command to run on the docker machine
     * @param checkExitValue the expected exit value
     * @return
     * @throws Exception
     */
    public ExecutionOutput runCommand(String command, boolean checkExitValue) throws Exception {
        return runCommand(getDockerEnv(), command, checkExitValue, platformHandler, DEFAULT_TIMEOUT, new NullProgressMonitor());
    }

    /**
     * Run the provided command on the docker machine. An exception is thrown
     * if the command results in an unexpected exit value.
     *
     * @param command the command to run on the docker machine
     * @param checkExitValue the expected exit value
     * @param timeout timeout specified in milliseconds
     * @return
     * @throws Exception
     */
    public ExecutionOutput runCommand(String command, boolean checkExitValue, long timeout) throws Exception {
        return runCommand(getDockerEnv(), command, checkExitValue, platformHandler, timeout, new NullProgressMonitor());
    }

    /**
     * Run the provided command on the docker machine. An exception is thrown
     * if the command results in an unexpected exit value.
     *
     * @param command the command to run on the docker machine
     * @param checkExitValue the expected exit value
     * @param timeout timeout specified in milliseconds
     * @return
     * @throws Exception
     */
    public ExecutionOutput runCommand(String command, boolean checkExitValue, long timeout, IProgressMonitor monitor) throws Exception {
        return runCommand(getDockerEnv(), command, checkExitValue, platformHandler, timeout, new NullProgressMonitor());
    }

    /**
     * Run the provided command on the docker machine. An exception is thrown
     * if the command results in an unexpected exit value.
     *
     * @param command the command to run on the docker machine
     * @param checkExitValue the expected exit value
     * @param platformHandler the platform handler used to execute the command
     * @return
     * @throws Exception
     */
    protected static ExecutionOutput runCommand(String command, boolean checkExitValue, IPlatformHandler platformHandler) throws Exception {
        return runCommand(null, command, checkExitValue, platformHandler, DEFAULT_TIMEOUT, new NullProgressMonitor());
    }

    /**
     * Execute the command using the provided platform handler and environment properties.
     *
     * @param env
     * @param command
     * @param checkExitValue
     * @param platformHandler
     * @param timeout
     * @return
     * @throws Exception
     */
    private static ExecutionOutput runCommand(Map<String, String> env, String command, boolean checkExitValue, IPlatformHandler platformHandler, long timeout,
                                              IProgressMonitor monitor) throws Exception {
        ExecutionOutput result = platformHandler.executeCommand(env, command, timeout, monitor);
        if (checkExitValue && result.getReturnCode() != 0) {
            String output = result.getOutput();
            String error = result.getError();
            if (Trace.ENABLED) {
                Trace.logError("Docker command failed: " + command + ", exit value: " + result.getReturnCode() + ", output: " + output + ", error: " + error, null);
            }
            throw new IOException(NLS.bind(Messages.errorFailedDockerCommand, new String[] { command, Integer.toString(result.getReturnCode()), error }));
        }
        return result;
    }

}
