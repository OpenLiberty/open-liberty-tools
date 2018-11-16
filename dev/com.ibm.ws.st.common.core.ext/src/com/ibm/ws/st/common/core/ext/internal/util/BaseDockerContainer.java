/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformUtil.OperatingSystem;

/**
 * Represents a docker container and executes various docker commands
 * using the passed in command protocol.
 */
public class BaseDockerContainer implements IPlatformHandler {

    private static final String HOST_CONFIG_KEY = "HostConfig";
    private static final String PORT_BINDINGS_KEY = "PortBindings";
    private static final String PORT_SUFFIX = "/tcp";
    private static final String HOST_IP_KEY = "HostIp";
    private static final String HOST_PORT_KEY = "HostPort";
    private static final String PUBLISH_ALL_PORTS_KEY = "PublishAllPorts";
    private static final String CONFIG_KEY = "Config";
    private static final String IMAGE_KEY = "Image";
    private static final String CMD_KEY = "Cmd";
    private static final String ENV_KEY = "Env";
    private static final String MOUNTS_KEY = "Mounts";
    private static final String DESTINATION_KEY = "Destination";
    private static final String SOURCE_KEY = "Source";

    private String containerName;
    private final AbstractDockerMachine dockerMachine;
    protected final IPlatformHandler platformHandler;
    private JsonObject inspectObject = null;

    /**
     * Constructor for DockerContainer.
     *
     * @param containerName The name of the container
     * @param machineName The docker machine name. May be null if there
     *            is no docker machine (such as on Linux)
     * @param osName The operating system name
     * @param platformHandler The protocol to use when executing commands
     */
    public BaseDockerContainer(String containerName, String machineType, String machineName, IPlatformHandler platformHandler) {
        this.containerName = containerName;
        this.dockerMachine = AbstractDockerMachine.createDockerMachine(machineType, machineName, platformHandler);
        this.platformHandler = platformHandler;
    }

    /**
     * Constructor for DockerContainer.
     *
     * @param containerName The name of the container
     * @param dockerMachine The docker machine
     * @param platformHandler The protocol to use when executing commands
     */
    BaseDockerContainer(String containerName, AbstractDockerMachine dockerMachine, IPlatformHandler platformHandler) {
        this.containerName = containerName;
        this.dockerMachine = dockerMachine;
        this.platformHandler = platformHandler;
    }

    /**
     * Get the name of this container
     *
     * @return The container name
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Get the docker machine for this container
     *
     * @return The docker machine
     */
    public AbstractDockerMachine getDockerMachine() {
        return dockerMachine;
    }

    /**
     * Copy a file from the host to the docker container
     *
     * @param file Host file
     * @param destinationPath Destination path in docker container
     * @throws ConnectException
     * @throws IOException
     */
    public void copyIn(String sourcePath, String destinationPath) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("docker cp ");
        builder.append(getPathWithQuotes(sourcePath));
        builder.append(" " + containerName + ":");
        builder.append(destinationPath);
        runCommand(builder.toString(), true);
    }

    /**
     * Copy a file from the docker container to the host
     *
     * @param sourcePath The path within the docker container
     * @param destinationPath The destination path on the host
     * @throws ConnectException
     */
    public void copyOut(String sourcePath, String destinationPath) throws ConnectException, IOException {
        copyOut(sourcePath, destinationPath, AbstractDockerMachine.DEFAULT_TIMEOUT);
    }

    /**
     * Copy a file from the docker container to the host
     *
     * @param sourcePath The path within the docker container
     * @param destinationPath The destination path on the host
     * @param timeout the timeout from running the command
     * @throws ConnectException
     * @return ProcessResult the result of running the copy out
     */
    public ExecutionOutput copyOut(String sourcePath, String destinationPath, long timeout) throws ConnectException, IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("docker cp ");
        builder.append(containerName);
        builder.append(":");
        builder.append(sourcePath);
        builder.append(" ");
        builder.append(getPathWithQuotes(destinationPath));
        return runCommand(builder.toString(), true, timeout);
    }

    /**
     * In the case where the path contains spaces (e.g. Windows OS case),
     * the path needs to be surrounded by quotes to prevent errors
     *
     * @param path
     * @return
     */
    protected String getPathWithQuotes(String path) {
        String result = path;
        if (result.trim().indexOf(" ") > 0) {
            result = "\"" + path + "\"";
        }

        return result;
    }

    /**
     * Execute a command in the container.
     *
     * @param command The command to execute
     * @return The result of the execution as a ProcessResult
     * @throws ConnectException
     */
    public ExecutionOutput dockerExec(String command) throws ConnectException {
        String cmd = getExecCommand() + command;
        return runCommand(cmd, false);
    }

    public ExecutionOutput dockerExec(String command, boolean checkExitValue, long timeout) throws ConnectException {
        String cmd = getExecCommand() + command;
        return runCommand(cmd, checkExitValue, timeout);
    }

    public ExecutionOutput dockerExec(Map<String, String> cmdEnv, String command, boolean checkExitValue, long timeout, IProgressMonitor progressMonitor) throws ConnectException {
        String cmd = getExecCommand() + command;
        return runCommand(cmdEnv, cmd, checkExitValue, timeout, progressMonitor);
    }

    public ExecutionOutput dockerRootExec(String command, boolean checkExitValue, long timeout) throws ConnectException {
        String cmd = getRootExecCommand() + command;
        return runCommand(cmd, checkExitValue, timeout);
    }

    /**
     * Check if a file exits in the container
     *
     * @param path The file path
     * @return True if the path exists and is a regular file, false otherwise
     * @throws ConnectException
     */
    @Override
    public boolean fileExists(String path) throws ConnectException {
        ExecutionOutput result = dockerExec("test -f " + path);
        return result.getReturnCode() == 0;
    }

    /**
     * Check if a directory exits in the container
     *
     * @param path The directory path
     * @return True if the path exists and is a directory, false otherwise
     * @throws ConnectException
     */
    @Override
    public boolean directoryExists(String path) throws ConnectException {
        ExecutionOutput result = dockerExec("test -d " + path);
        return result.getReturnCode() == 0;
    }

    /**
     * Delete the file in the container
     *
     * @param file the name of the file to delete
     * @return The result of the execution as a ProcessResult
     * @throws ConnectException
     */
    @Override
    public void deleteFile(String file) throws ConnectException {
        dockerExec("rm " + " " + file);
    }

    /**
     * Delete the entire folder in the container
     *
     * Not added to IPlatformHandler since deleteFolder may not apply
     *
     * @param folder the name of the folder to delete
     * @return The result of the execution as a ProcessResult
     * @throws ConnectException
     */
    public void deleteFolder(String folder) throws ConnectException {
        dockerExec("rm -rf " + folder);
    }

    /**
     * Rename the file in the container
     *
     * @param oldFileName the name of the existing file
     * @param newFileName the new name of the file
     * @return The result of the execution as a ProcessResult
     * @throws ConnectException
     */
    public ExecutionOutput renameFile(String oldFileName, String newFileName) throws ConnectException {
        return dockerExec("mv " + oldFileName + " " + newFileName);
    }

    /**
     * Check if the container is running.
     *
     * @return <code>true</code> if running, <code>false</code> otherwise
     * @throws ConnectException
     */
    public boolean isRunning() throws ConnectException {
        String cmd = "docker inspect --format {{.State.Running}} " + containerName;
        ExecutionOutput result = runCommand(cmd, true);
        boolean isRunning = "true".equals(result.getOutput().trim());
        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Docker container " + toString() + " is running: " + isRunning);
        }
        return isRunning;
    }

    /**
     * Get the container logs.
     *
     * @return
     */
    public String getLogs() throws ConnectException {
        String cmd = "docker logs " + containerName;
        ExecutionOutput result = runCommand(cmd, true);
        String logs = result.getOutput().trim();
        return logs;
    }

    /**
     * Get the port mapping on the host given the container port. This
     * returns the mapping in the form <host ip>:<port>.
     *
     * @param containerPort The container port
     * @return
     */
    public String getPortMapping(String containerPort) throws ConnectException {
        String cmd = "docker port " + containerName + " " + containerPort;
        ExecutionOutput result = runCommand(cmd, true);
        String mapping = result.getOutput().trim();
        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Docker container " + toString() + " port mapping for " + containerPort + " is: " + mapping);
        }
        return mapping;
    }

    public String getHostMappedPort(String containerPort) throws Exception {
        String mapping = getPortMapping(containerPort);
        return getPortFromMapping(mapping);
    }

    public String getHostMappedIP(String containerPort) throws Exception {
        String mapping = getPortMapping(containerPort);
        String host = getHostFromMapping(mapping);
        if (host == null) {
            host = getDockerMachine().getHost();
        }
        return host;
    }

    public static String getPortFromMapping(String mapping) {
        int index = mapping.lastIndexOf(":");
        if (index >= 0) {
            String port = mapping.substring(index + 1);
            return port;
        }
        return mapping;
    }

    public static String getHostFromMapping(String mapping) {
        int index = mapping.lastIndexOf(":");
        if (index >= 0) {
            String host = mapping.substring(0, index);
            if (!host.isEmpty() && !host.equals("0.0.0.0")) {
                return host;
            }
        }
        return null;
    }

    /**
     * Get the environment variables for the container.
     *
     * @return The environment variables as a list of string: varName=varValue
     * @throws ConnectException
     */
    public List<String> getEnv() throws ConnectException {
        List<String> env = new ArrayList<String>();
        JsonObject config = getConfig();
        if (config != null) {
            JsonArray envArray = (JsonArray) config.get(ENV_KEY);
            if (envArray != null) {
                for (int i = 0; i < envArray.size(); i++) {
                    env.add(envArray.getString(i));
                }
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Docker container " + toString() + " environment variables: " + env);
        }
        return env;
    }

    /**
     * Get the port bindings for the container. This will return static
     * information only. Use getPortMapping for dynamic information.
     * If the user specifies the -P option (publish all bindings) a
     * free port on the host is found for each container port on start
     * of the container and may change each time the container is restarted.
     *
     * @return A map of port bindings: container port -> host port
     * @throws ConnectException
     */
    public Map<String, String> getPortBindings() throws ConnectException {
        JsonObject inspect = getInspectOutput();
        Map<String, String> bindings = new HashMap<String, String>();
        JsonObject hostConfig = (JsonObject) inspect.get(HOST_CONFIG_KEY);
        if (hostConfig != null) {
            JsonValue jsonValue = hostConfig.get(PORT_BINDINGS_KEY);
            // If jsonValue is not an instance of JsonObject, then simply return an empty map because there are no port bindings defined.
            // This handles the case where the JSON for this element is: "PortBindings": {}  or  "PortBindings": null
            if (jsonValue instanceof JsonObject) {
                JsonObject portBindings = (JsonObject) jsonValue;
                for (Object key : portBindings.keySet()) {
                    String containerPort = (String) key;
                    if (containerPort.endsWith(PORT_SUFFIX)) {
                        containerPort = containerPort.substring(0, containerPort.length() - PORT_SUFFIX.length());
                    }
                    JsonArray portArray = (JsonArray) portBindings.get(key);
                    if (portArray != null && portArray.size() > 0) {
                        JsonObject portObject = (JsonObject) portArray.get(0);
                        String hostIP = portObject.getString(HOST_IP_KEY);
                        String hostPort = portObject.getString(HOST_PORT_KEY);
                        if (hostIP.isEmpty()) {
                            bindings.put(containerPort, hostPort);
                        } else {
                            bindings.put(containerPort, hostIP + ":" + hostPort);
                        }
                    }
                }
            }
        }
        return bindings;
    }

    /**
     * Get the publish all port setting for this container
     *
     * @return True if publish all ports is enabled, false otherwise
     * @throws ConnectException
     */
    public boolean getPublishAllPorts() throws ConnectException {
        JsonObject inspect = getInspectOutput();
        JsonObject hostConfig = (JsonObject) inspect.get(HOST_CONFIG_KEY);
        if (hostConfig != null) {
            boolean publishAllPorts = hostConfig.getBoolean(PUBLISH_ALL_PORTS_KEY, false);
            return publishAllPorts;
        }
        return false;
    }

    /**
     * Get the command for the container.
     *
     * @return The command as a list of string.
     * @throws ConnectException
     */
    public List<String> getCommand() throws ConnectException {
        List<String> cmd = new ArrayList<String>();
        JsonObject config = getConfig();
        if (config != null) {
            JsonArray cmdArray = (JsonArray) config.get(CMD_KEY);
            if (cmdArray != null) {
                for (int i = 0; i < cmdArray.size(); i++) {
                    cmd.add(cmdArray.getString(i));
                }
            }
        }
        return cmd;
    }

    /**
     * Get the name of the image this container is based on.
     *
     * @return The image name
     * @throws ConnectException
     */
    public String getImageName() throws ConnectException {
        String imageName = null;
        JsonObject config = getConfig();
        if (config != null) {
            imageName = config.getString(IMAGE_KEY);
        }
        return imageName;
    }

    /**
     * Stop this container
     *
     * @throws ConnectException
     */
    public void stop() throws ConnectException {
        stop(new NullProgressMonitor());
    }

    /**
     * Stop this container
     *
     * @throws ConnectException
     */
    public void stop(IProgressMonitor monitor) throws ConnectException {
        String cmd = "docker stop " + containerName;
        runCommand(cmd, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5, monitor);
    }

    /**
     * Start this container
     *
     * @throws ConnectException
     */
    public void start() throws ConnectException {
        start(new NullProgressMonitor());
    }

    /**
     * Start this container
     *
     * @throws ConnectException
     */
    public void start(IProgressMonitor monitor) throws ConnectException {
        String cmd = "docker start " + containerName;
        runCommand(cmd, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5, monitor);
    }

    /**
     * Commit this container to a new image
     *
     * @param imageName The name to give the new image
     * @throws ConnectException
     */
    public void commit(String imageName) throws ConnectException {
        String cmd = "docker commit " + containerName + " " + imageName;
        runCommand(cmd, true, AbstractDockerMachine.DEFAULT_TIMEOUT * 5);
    }

    /**
     * Flatten a container to a new image. Similar to commit but will
     * reduce the size of the image closer to the original image size.
     * Takes a lot longer than a commit.
     *
     * @param imageName The name to give the new image.
     * @throws ConnectException
     */
    public boolean flatten(String imageName, IProgressMonitor progressMonitor) throws Exception {
        IProgressMonitor monitor = progressMonitor;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        String tmpFile = "";

        try {
            platformHandler.startSession();

            tmpFile = getTmpFileName(containerName, ".tar");
            int retries = 0;
            while (platformHandler.fileExists(tmpFile) && retries++ < 100) {
                tmpFile = getTmpFileName(containerName, ".tar");
            }

            String path = getPathWithQuotes(tmpFile);
            String cmd = "docker export --output " + path + " " + containerName;
            monitor.beginTask(NLS.bind(Messages.flattenExportContainer, containerName), IProgressMonitor.UNKNOWN);
            runCommand(cmd, true, (5 * 60 * 1000) + AbstractDockerMachine.DEFAULT_TIMEOUT, monitor);
            if (monitor.isCanceled()) {
                return false;
            }

            monitor.setTaskName(NLS.bind(Messages.flattenImportImage, imageName));
            cmd = "docker import " + path + " " + imageName;
            runCommand(cmd, true, (5 * 60 * 1000) + AbstractDockerMachine.DEFAULT_TIMEOUT, monitor);
            if (monitor.isCanceled()) {
                try {
                    getDockerMachine().removeImage(imageName);
                } catch (Exception e1) {
                    if (Trace.ENABLED)
                        Trace.logError("Docker import cancelled but unable to remove image: " + imageName + ". The image may still exist.", e1);
                    // Ignore - will fail if the image was not created yet
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            try {
                getDockerMachine().removeImage(imageName);
            } catch (Exception e1) {
                Trace.logError("Docker flatten image encountered problems. Failed to remove the image: " + imageName + ". The image may still exist.", e1);
                // Ignore - will fail if the image was not created yet
            }
        } finally {
            try {
                if (platformHandler.fileExists(tmpFile)) {
                    platformHandler.deleteFile(tmpFile);
                }
            } catch (Exception e) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Failed to delete temporary file for exported container: " + tmpFile);
                }
            }
            try {
                platformHandler.endSession();
            } catch (Exception e) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Rename this container
     *
     * @param name The new container name
     * @throws ConnectException
     */
    public void rename(String name) throws ConnectException {
        String cmd = "docker rename " + containerName + " " + name;
        runCommand(cmd, true);
        containerName = name;
    }

    public List<DockerProcess> getContainerProcesses() throws ConnectException {
        String cmd = "docker top " + containerName;
        ExecutionOutput result = runCommand(cmd, true);
        String output = result.getOutput();
        String[] lines = output.split("[\r\n]+");
        List<DockerProcess> processes = new ArrayList<DockerProcess>();
        // Skip the first line as it contains the table headers
        for (int i = 1; i < lines.length; i++) {
            String[] words = lines[i].split("\\s");
            if (words.length >= 3) {
                List<String> processCmd = new ArrayList<String>(words.length - 2);
                for (int j = 2; j < words.length; j++) {
                    processCmd.add(words[j]);
                }
                DockerProcess process = new DockerProcess(words[0], words[1], processCmd);
                processes.add(process);
            }
        }
        if (Trace.ENABLED) {
            Trace.trace(Trace.DETAILS, "Docker container " + toString() + " process information: " + processes);
        }
        return processes;
    }

    // Get the config JSON object
    private JsonObject getConfig() throws ConnectException {
        JsonObject inspect = getInspectOutput();
        return (JsonObject) inspect.get(CONFIG_KEY);
    }

    // Get the output of docker inspect as a JsonObject.
    // Should only be used for information that is static (can't be changed
    // once the container is created).  Dynamic information such as whether the
    // container is running should not use this.
    private synchronized JsonObject getInspectOutput() throws ConnectException {
        if (inspectObject == null) {
            try {
                ExecutionOutput result = runCommand("docker inspect " + containerName, true);
                InputStream stream = new ByteArrayInputStream(result.getOutput().getBytes());
                JsonReader reader = Json.createReader(stream);
                JsonArray inspectArray = reader.readArray();
                inspectObject = (JsonObject) inspectArray.get(0);
            } catch (Exception e) {
                throw new ConnectException(e.getMessage());
            }
        }
        return inspectObject;
    }

    // Run a command using the command protocol where the timeout for the command is a default value
    protected ExecutionOutput runCommand(String command, boolean checkExitValue) throws ConnectException {
        return runCommand(null, command, checkExitValue, AbstractDockerMachine.DEFAULT_TIMEOUT, null);
    }

    // Run a command using the command protocol where the timeout for the command is provided
    protected ExecutionOutput runCommand(String command, boolean checkExitValue, long timeout) throws ConnectException {
        return runCommand(null, command, checkExitValue, timeout, null);
    }

    // Run a command using the command protocol where the command timeout is passed as a parameter
    protected ExecutionOutput runCommand(String command, boolean checkExitValue, long timeout, IProgressMonitor monitor) throws ConnectException {
        return runCommand(null, command, checkExitValue, timeout, monitor);
    }

    protected ExecutionOutput runCommand(Map<String, String> cmdEnv, String command, boolean checkExitValue, long timeout, IProgressMonitor monitor) throws ConnectException {
        try {
            platformHandler.startSession();
            Map<String, String> envMap = dockerMachine.getDockerEnv();
            if (envMap == null)
                envMap = new HashMap<String, String>(4);
            if (cmdEnv != null) {
                envMap.putAll(cmdEnv);
            }
            ExecutionOutput result = platformHandler.executeCommand(envMap, command, timeout, monitor);
            if (checkExitValue && result.getReturnCode() != 0) {
                if (Trace.ENABLED) {
                    Trace.logError("Docker command failed: " + command + ", exit value: " + result.getReturnCode() + ", output: " + result.getOutput() + ", error: "
                                   + result.getError(), null);
                }
                throw new IOException(NLS.bind(Messages.errorFailedDockerCommand, new String[] { command, Integer.toString(result.getReturnCode()), result.getError() }));
            }
            return result;
        } catch (Exception e) {
            throw new ConnectException(e.getMessage());
        } finally {
            platformHandler.endSession();
        }
    }

    // Get the basic docker exec command for the container
    private String getExecCommand() {
        return "docker exec -i " + containerName + " ";
    }

    private String getRootExecCommand() {
        return "docker exec --user root " + containerName + " ";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "(" + dockerMachine + ") " + containerName;
    }

    public static class DockerProcess {
        String pid;
        String user;
        List<String> command;

        public DockerProcess(String pid, String user, List<String> command) {
            this.pid = pid;
            this.user = user;
            this.command = command;
        }

        public String getPID() {
            return pid;
        }

        public String getUser() {
            return user;
        }

        public List<String> getCommand() {
            return command;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Id: " + pid + ", User: " + user + ", Command: " + command;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void startSession() throws Exception {
        platformHandler.startSession();
    }

    /** {@inheritDoc} */
    @Override
    public void createDirectory(String path) throws ConnectException, IOException {
        try {
            dockerExec("mkdir -p " + path);
        } catch (Exception e) {
            throw new ConnectException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void uploadFile(String sourcePath, String destinationPath) throws Exception {
        copyIn(sourcePath, destinationPath);
        dockerRootExec("sh -c \"chmod 664 " + destinationPath + "\"", true, AbstractDockerMachine.DEFAULT_TIMEOUT);
    }

    /** {@inheritDoc} */
    @Override
    public void downloadFile(String sourcePath, String destinationPath) throws ConnectException, IOException {
        copyOut(sourcePath, destinationPath);
    }

    /** {@inheritDoc} */
    @Override
    public ExecutionOutput executeCommand(String command) throws Exception {
        return dockerExec(command);
    }

    /** {@inheritDoc} */
    @Override
    public ExecutionOutput executeCommand(String command, long timeout) throws Exception {
        return executeCommand(null, command, timeout, null);
    }

    /** {@inheritDoc} */
    @Override
    public ExecutionOutput executeCommand(Map<String, String> cmdEnv, String command, long timeout) throws Exception {
        return executeCommand(cmdEnv, command, timeout, null);
    }

    /** {@inheritDoc} */
    @Override
    public ExecutionOutput executeCommand(Map<String, String> env, String command, long timeout, IProgressMonitor monitor) throws Exception {
        return dockerExec(env, command, true, timeout, monitor);
    }

    /** {@inheritDoc} */
    @Override
    public void endSession() throws ConnectException {
        platformHandler.endSession();
    }

    /** {@inheritDoc} */
    @Override
    public String getEnvValue(String var) throws ConnectException, IOException {
        for (String key : getEnv()) {
            if (key.contains(var))
                return key.split("=")[1];
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTempDir() throws IOException {
        throw new IOException(Messages.errorDockerTempDir);
    }

    /**
     * @return
     * @throws IOException
     * @throws RemoteAccessAuthException
     */
    private String getTmpFileName(String name, String extension) throws Exception {
        int i = (int) (Math.random() * 10000.0);
        String tmpFile = platformHandler.getTempDir() + containerName + i + extension;
        return tmpFile;
    }

    /**
     * @param destinationPath
     * @return
     */
    protected String getFileNameFromPath(String destinationPath) {
        int index = destinationPath.lastIndexOf("/");
        if (index == -1)
            index = destinationPath.lastIndexOf("\\");
        return destinationPath.substring(index + 1);
    }

    /**
     * {@inheritDoc}
     *
     * @throws RemoteAccessAuthException
     */
    @Override
    public OperatingSystem getOS() throws Exception {
        return platformHandler.getOS();
    }

    /**
     * Get all the volume mappings: source1:destination1 source2:destination2 ...
     *
     * @return
     * @throws ConnectException
     */
    public Map<String, String> getAllMountedVolumes() throws Exception {
        Map<String, String> volumes = new HashMap<String, String>();
        JsonObject inspect = getInspectOutput();
        if (inspect.containsKey(MOUNTS_KEY)) {
            Object mounts = inspect.get(MOUNTS_KEY);
            if (mounts instanceof JsonArray) {
                JsonArray mountsArray = (JsonArray) mounts;
                for (Object item : mountsArray) {
                    if (item instanceof JsonObject) {
                        JsonObject jsonObj = (JsonObject) item;
                        String destination = jsonObj.getString(DESTINATION_KEY);
                        String source = jsonObj.getString(SOURCE_KEY);
                        volumes.put(source, destination);
                    }
                }
            }
        }
        return volumes;
    }

    /**
     * Get all of the volume mappings in a source -> destination hash
     *
     * @return
     * @throws ConnectException
     */
    public Map<IPath, IPath> getMountedVolumeHash() throws Exception {
        Map<IPath, IPath> volumeHash = null;
        List<String> volumes = new ArrayList<String>();
        JsonObject inspect = getInspectOutput();
        if (inspect.containsKey(MOUNTS_KEY)) {
            Object mounts = inspect.get(MOUNTS_KEY);
            if (mounts instanceof JsonArray) {
                JsonArray mountsArray = (JsonArray) mounts;
                volumeHash = new HashMap<IPath, IPath>(mountsArray.size());
                for (Object item : mountsArray) {
                    if (item instanceof JsonObject) {
                        JsonObject jsonObj = (JsonObject) item;
                        String destination = jsonObj.getString(DESTINATION_KEY);
                        String source = jsonObj.getString(SOURCE_KEY);
                        IPath sourcePath = new Path(source);
                        // For Windows non-native, the mounted volume is /c/Users/<path>.  It is a 'logical' mapping of the
                        // real local path c:/Users/<path>.   So, we need to convert it back to the usable form.
                        // On Mac and Linux, no conversion is necessary.
                        sourcePath = getContainerToLocalPath(sourcePath);
                        volumeHash.put(sourcePath, new Path(destination));
                    } else {
                        if (Trace.ENABLED) {
                            Trace.trace(Trace.WARNING, "A mount item in the inspect output for the " + getContainerName() + " container was not an instance of JsonObject.");
                        }
                    }
                }
            } else {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "The mounts in the inspect output for the " + getContainerName() + " container were not an instance of JsonArray.");
                }
            }
        } else {
            if (Trace.ENABLED) {
                Trace.trace(Trace.INFO, "The inspect output for the " + getContainerName() + " container did not contain any mounts.");
            }
        }
        return volumeHash;
    }

    /**
     * Read container Mounts until we get one that matches destinationValue. The corresponding Source value is returned.
     *
     * @return
     * @throws ConnectException
     */
    public String getMountSourceForDestination(String destinationValue) throws Exception {
        JsonObject inspect = getInspectOutput();
        if (inspect.containsKey(MOUNTS_KEY)) {
            Object mounts = inspect.get(MOUNTS_KEY);
            if (mounts instanceof JsonArray) {
                JsonArray mountsArray = (JsonArray) mounts;
                for (Object item : mountsArray) {
                    if (item instanceof JsonObject) {
                        JsonObject jsonObj = (JsonObject) item;
                        String destination = jsonObj.getString(DESTINATION_KEY);
                        if (destinationValue.equals(destination)) {
                            String srcValue = jsonObj.getString(SOURCE_KEY);
                            if (srcValue != null && !srcValue.equals("")) {
                                return srcValue;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Read container Mounts until we get one that matches sourceValue. The corresponding Destination value is returned.
     *
     * @return
     * @throws ConnectException
     */
    public IPath getMountDestinationForSource(IPath sourceValue) throws Exception {
        JsonObject inspect = getInspectOutput();
        if (inspect.containsKey(MOUNTS_KEY)) {
            Object mounts = inspect.get(MOUNTS_KEY);
            if (mounts instanceof JsonArray) {
                JsonArray mountsArray = (JsonArray) mounts;
                for (Object item : mountsArray) {
                    if (item instanceof JsonObject) {
                        JsonObject jsonObj = (JsonObject) item;
                        String source = jsonObj.getString(SOURCE_KEY);
                        if (sourceValue.toString().equals(source)) {
                            String destinationValue = jsonObj.getString(DESTINATION_KEY);
                            if (destinationValue != null && !destinationValue.equals("")) {
                                return new Path(destinationValue);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the contents of a container folder as a list of strings
     *
     * @param containerFolder
     * @return
     * @throws ConnectException
     */
    public List<String> getContentsOfFolder(String containerFolder) throws ConnectException {
        List<String> contents = null;
        ExecutionOutput results = dockerExec("ls " + containerFolder, true, AbstractDockerMachine.DEFAULT_TIMEOUT); //$NON-NLS-1$
        String output = results.getOutput();
        if (output != null && output.length() > 0) {
            contents = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(output, "\n"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                contents.add(st.nextToken());
            }
        }
        return contents;
    }

    /**
     * Convert /c/Users or /host_mnt/c/Users to c:/Users for writing back to the local file system.
     *
     * @param container
     * @param tempDirectory
     * @return
     */
    public static IPath getContainerToLocalPath(IPath tempDirectory) {
        // For Windows the mounted volume is /c/Users/<path>.  It is a 'logical' mapping of the
        // real local path c:/Users/<path>.   So, we need to convert it back to the usable form.
        // On Mac and Linux, no conversion is necessary.
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win"); //NON-NLS-1$ //$NON-NLS-2$
        IPath updatedTempDirectory = null;
        // Only change for Windows and non-native Docker
        if (isWindows) {
            String device;
            String winPath;
            if (tempDirectory.segment(0).equals("host_mnt")) { // Docker CE 17.12 Mount point is different
                // Mount:  /host_mnt/c/Users
                device = tempDirectory.toString().substring(10, 11);
                winPath = device + ":" + tempDirectory.toString().substring(11); //$NON-NLS-1$
            } else {
                // Mount:  /c/Users
                device = tempDirectory.toString().substring(1, 2);
                winPath = device + ":" + tempDirectory.toString().substring(2); //$NON-NLS-1$
            }
            updatedTempDirectory = new Path(winPath);
        } else {
            updatedTempDirectory = tempDirectory;
        }
        return FileUtil.getCanonicalPath(updatedTempDirectory);
    }

    /**
     * Get Windows path mount. Must be /c/Users/<path> instead of C:/Users/<path>
     * Note: For Docker CE for Windows 17.20, this path works fine. Alternatively, it can be mounted as C:/anypath.
     *
     * @param tempDirectory
     * @return
     */
    public static IPath getLocalToContainerPath(IPath tempDirectory) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win"); //NON-NLS-1$ //$NON-NLS-2$
        IPath updatedTempDirectory = null;
        // Only change for Windows
        if (isWindows) {
            String device = tempDirectory.getDevice().toLowerCase();
            String winPath = "/" + device.substring(0, 1) + tempDirectory.toString().substring(2); //$NON-NLS-1$
            updatedTempDirectory = new Path(winPath);
        } else {
            updatedTempDirectory = tempDirectory;
        }
        return updatedTempDirectory;
    }

}
