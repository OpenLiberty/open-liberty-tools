/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.docker;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.IPlatformHandler.ExecutionOutput;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.tests.remote.RemoteTestUtil;
import com.ibm.ws.st.core.tests.util.ServerTestUtil;
import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerRunUtility.MountProperty;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerServer;
import com.ibm.ws.st.docker.ui.internal.wizard.LibertyDockerUtil;

/**
 *
 */
public class DockerTestUtil extends ServerTestUtil {

    private static String MACHINE_NAME = "default";
    public static final String CONTAINER_NAME = "wlp_test";
    private static String IMAGE_NAME = "websphere-liberty";

    /*
     * Get the docker machine type - must be one of STANDARD, PHANTOM
     * or MINIKUBE
     */
    private static String getDockerMachineType() {
        return System.getProperty("liberty.docker.machinetype", "STANDARD");
    }

    /* Get the docker-machine name, if not specified, assume default */
    private static String getDockerMachineName() {
        return System.getProperty("liberty.docker.machine", MACHINE_NAME);
    }

    public static boolean isContainerSpecified() {
        return System.getProperty("liberty.docker.container") != null;
    }

    /* Get an existing container name, a new container will be created if not specified */
    public static String getDockerImageName() {
        return System.getProperty("liberty.docker.image", IMAGE_NAME);
    }

    /* If a container is specified and has an existing basic registry, get the username */
    protected static String getDockerUsername() {
        return System.getProperty("liberty.docker.username", "remoteUser");
    }

    /* If a container is specified and has an existing basic registry, get the password */
    protected static String getDockerPassword() {
        return System.getProperty("liberty.docker.password", "remotePass");
    }

    /*
     * Specify whether the initial container has no existing usr mount, or a mount that is the same as the local
     * workspace, or different and is external to the existing workspace
     */
    protected static MountProperty getUsrMountProperty() {
        String prop = System.getProperty("liberty.docker.usr.mount.property", "none");
        if (prop.equals("same")) {
            return MountProperty.SAME_USR_MOUNT;
        } else if (prop.equals("other")) {
            return MountProperty.OTHER_USR_MOUNT;
        }
        return MountProperty.NO_USR_MOUNT;
    }

    /*
     * Gets the docker machine. If no machine name is set in the properties then it uses the default.
     * Makes sure that the machine is running.
     */
    public static AbstractDockerMachine getDockerMachine() throws Exception {
        Map<String, String> serviceInfo = RemoteTestUtil.getConnectionInfo();
        IPlatformHandler handler = PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.COMMAND);
        List<AbstractDockerMachine> machines = AbstractDockerMachine.getDockerMachines(handler);
        AbstractDockerMachine defaultMachine = null;
        String machineType = getDockerMachineType();
        String machineName = getDockerMachineName();

        for (AbstractDockerMachine m : machines) {
            if (machineType.equals(m.getMachineType().name())) {
                switch (m.getMachineType()) {
                    case STANDARD:
                        if (machineName != null && machineName.equals(m.getMachineName())) {
                            dockerMachineSetup(machineName, handler);
                            return m;
                        }
                        break;
                    case PHANTOM:
                    case MINIKUBE:
                        return m;

                }
            }
            if (defaultMachine == null && m.getMachineType() == AbstractDockerMachine.MachineType.PHANTOM) {
                defaultMachine = m;
            }
        }
        if (defaultMachine != null) {
            print("Could not find docker machine with type: " + machineType + ", and name (applies to STANDARD only): " + machineName + ".  Defaulting to phantom docker machine");
            return defaultMachine;
        }

        throw new Exception("No docker machines were found");
    }

    private static Map<String, String> getServiceInfo(String containerName, AbstractDockerMachine machine) {
        // This is for creating a container so just need the remote information and the container and machine name
        Map<String, String> map = new HashMap<String, String>();
        map.putAll(RemoteTestUtil.getConnectionInfo());
        map.put(Constants.DOCKER_MACHINE, machine.getMachineName());
        map.put(Constants.DOCKER_CONTAINER, containerName);
        return map;
    }

    public static String createDockerContainer() throws Exception {
        BaseDockerContainer container = getDockerContainer();
        return container.getContainerName();
    }

    public static void createDockerContainer(String containerName, boolean removeExisting) throws Exception {
        getDockerContainer(containerName, removeExisting);
    }

    /*
     * Get the container if it exists or create a new one. If no container name specified
     * in the properties then generate a new random name.
     *
     * If you want to find an existing container, use getExistingContainer. The getDockerMachine and
     * getDockerContainerName methods can be used for the parameters.
     *
     */
    public static BaseDockerContainer getDockerContainer() throws Exception {
        return getDockerContainer(getDockerMachine());
    }

    public static BaseDockerContainer getDockerContainer(AbstractDockerMachine machine) throws Exception {
        String containerName = System.getProperty("liberty.docker.container");
        if (containerName == null || containerName.isEmpty()) {
            containerName = generateContainerName(machine);
        }
        return getDockerContainer(containerName, false, null);
    }

    /*
     * Generate a random container name making sure a container with the same
     * name does not already exist.
     *
     * @return A random container name that is not already in use
     */
    public static String generateContainerName(AbstractDockerMachine machine) throws Exception {
        List<String> containerNames = machine.getContainerNames(true);
        String containerName;
        do {
            int id = (int) (Math.random() * Integer.MAX_VALUE);
            containerName = CONTAINER_NAME + id;
        } while (containerNames.contains(containerName));
        return containerName;
    }

    /*
     * Get the container if it exists and removeExisting is set to false or create a new one. If no
     * container name specified in the properties then use the default.
     */
    public static BaseDockerContainer getDockerContainer(String containerName, boolean removeExisting) throws Exception {
        return getDockerContainer(containerName, removeExisting, null);
    }

    public static BaseDockerContainer getDockerContainer(String containerName, boolean removeExisting, Map<IPath, IPath> volumes) throws Exception {
        AbstractDockerMachine machine = getDockerMachine();
        if (removeExisting) {
            removeExistingContainer(machine, containerName);
        }
        dockerContainerSetup(machine, containerName, getDockerImageName(), volumes);
        return getExistingContainer(machine, containerName);
    }

    /*
     * If the container exists, remove it
     */
    public static void removeExistingContainer(AbstractDockerMachine machine, String containerName) {
        BaseDockerContainer existingContainer = getExistingContainer(machine, containerName);
        if (existingContainer != null) {
            try {
                existingContainer.getDockerMachine().removeContainer(existingContainer.getContainerName(), true);
            } catch (Exception e2) {
                Trace.logError("Error stopping and removing the existing container", e2);
            }
        }
    }

    public static void removeExistingContainer(String containerName) throws Exception {
        removeExistingContainer(getDockerMachine(), containerName);
    }

    public static void startContainer(String containerName) throws Exception {
        BaseDockerContainer container = getExistingContainer(getDockerMachine(), containerName);
        if (container != null) {
            container.start();
        }
    }

    public static void stopContainer(String containerName) throws Exception {
        BaseDockerContainer container = getExistingContainer(getDockerMachine(), containerName);
        if (container != null) {
            container.stop();
        }
    }

    /*
     * Stop all containers that are running
     */
    public static void stopAllContainers() throws Exception {
        Map<String, String> serviceInfo = RemoteTestUtil.getConnectionInfo();
        IPlatformHandler handler = PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.COMMAND);
        List<AbstractDockerMachine> machines = AbstractDockerMachine.getDockerMachines(handler);
        for (AbstractDockerMachine machine : machines) {
            try {
                List<BaseDockerContainer> containers = machine.getContainers(false);
                for (BaseDockerContainer container : containers) {
                    if (LibertyDockerUtil.isLibertyContainer(container)) {
                        try {
                            container.stop();
                        } catch (Exception e) {
                            print("Failed to stop container: " + container.getContainerName(), e);
                        }
                    }
                }
            } catch (Exception e) {
                print("Failed to get containers for machine: " + machine.getMachineName(), e);
            }
        }
    }

    public static void createBusyBox(String containerName) throws Exception {
        AbstractDockerMachine machine = getDockerMachine();
        removeExistingContainer(machine, containerName);
        String cmd = "docker run --name " + containerName + " -t busybox";
        try {
            machine.runCommand(cmd, true);
        } catch (Exception e) {
            print("Failed to create a new busybox container with the name: " + containerName, e);
        }
    }

    public static void removeBusyBox(String containerName) throws Exception {
        AbstractDockerMachine machine = getDockerMachine();
        Map<String, String> serviceInfo = getServiceInfo(containerName, machine);
        BaseDockerContainer container = (BaseDockerContainer) PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.DOCKER);
        try {
            container.stop();
            machine.removeContainer(containerName);
        } catch (Exception e) {
            print("Failed to remove busybox container: " + containerName, e);
        }
    }

    /*
     * Get the docker machine and start it
     */
    private static void dockerMachineSetup(String machineName, IPlatformHandler platformHandler) {
        if (machineName != null) {
            try {
                startDockerMachine(machineName, platformHandler);
            } catch (Exception e) {
                Trace.logError("Error starting the docker machine", e);
            }
        }
    }

    /*
     * Start the Docker machine if it is not already running
     */
    private static void startDockerMachine(String machineName, IPlatformHandler platformHandler) {
        if (!machineIsRunning(machineName, platformHandler)) {
            try {
                String cmd = "docker-machine start " + machineName;
                platformHandler.executeCommand(cmd, 60000); //takes about 45 seconds to start the docker machine
            } catch (Exception e) {
                Trace.logError("Error executing the docker-machine start command", e);
            }
        }
    }

    /*
     * Get the status of the docker machine
     */
    private static boolean machineIsRunning(String machineName, IPlatformHandler platformHandler) {
        String outputStr = null;
        try {
            ExecutionOutput result = platformHandler.executeCommand("docker-machine status " + machineName, 5000);
            int exitValue = result.getReturnCode();
            outputStr = result.getOutput().trim();
            if (exitValue != 0) {
                print("Error getting status for machine " + machineName + ". Exit value = " + Integer.valueOf(exitValue) + " output: " + outputStr + " error: "
                      + result.getError());
                return false;
            }
        } catch (Exception e) {
            print("Exception getting status for machine " + machineName, e);
        }
        return "Running".equals(outputStr);
    }

    /*
     * If there is an existing container with the right image, make sure it is started,
     * otherwise create a new container.
     */
    private static void dockerContainerSetup(AbstractDockerMachine machine, String containerName, String imageName, Map<IPath, IPath> volumes) {
        BaseDockerContainer container = getExistingContainer(machine, containerName);
        try {
            // image name can have a tag (eg. beta images have the beta tag and the image name is "websphere-liberty:beta")
            if (container != null && !container.getImageName().matches("^" + imageName + "((:)(.+))?")) {
                removeExistingContainer(machine, containerName);
                container = null;
            }
        } catch (ConnectException e) {
            print("Failed to get the image name for container: " + containerName, e);
        }

        if (container == null) {
            String cmd = "docker run -e LICENSE=accept";
            if (volumes != null) {
                List<String> volumeArgs = LibertyDockerRunUtility.getVolumeArgs(volumes);
                String args = LibertyDockerRunUtility.mergeStrings(volumeArgs);
                cmd = cmd + " " + args;
            }
            cmd = cmd + " -P --name " + containerName + " -td " + imageName;

            try {
                machine.runCommand(cmd, true);
                container = (BaseDockerContainer) PlatformHandlerFactory.getPlatformHandler(getServiceInfo(containerName, machine), PlatformType.DOCKER);

                //Need to wait for the LTPA keys and the SSL certificate to be created
                print("Creating a docker container and waiting for it to be ready.");

                int attempt = 0;
                while (attempt < 20) {
                    String logs = container.getLogs();
                    boolean containerReady = logs.contains("ready to run a smarter planet.");
                    if (containerReady)
                        break;
                    WLPCommonUtil.wait("Wait an additional 3 seconds.", 3000);
                    attempt++;
                }
            } catch (Exception e) {
                print("Failed to create a new " + imageName + " container with the name: " + containerName, e);
            }
            print("Finished setting up new container: " + containerName);
        } else {
            try {
                if (!container.isRunning()) {
                    container.start();
                    print("Finished starting existing container: " + containerName);
                }
            } catch (ConnectException e) {
                print("Could not start container: " + containerName, e);
            }
        }

    }

    /*
     * Get the existing container that matches the container name
     */
    public static BaseDockerContainer getExistingContainer(AbstractDockerMachine machine, String containerName) {
        try {
            List<BaseDockerContainer> containers = machine.getContainers(true);
            for (BaseDockerContainer container : containers) {
                if (containerName.equals(container.getContainerName())) {
                    return container;
                }
            }
        } catch (Exception e) {
            print("Failed to get containers for machine: " + machine.getMachineName(), e);
        }
        print("Could not find a container with the name: " + containerName);
        return null;
    }

    public static String getContainerName(IServer server) {
        if (server != null) {
            WebSphereServer wsServer = server.getAdapter(WebSphereServer.class);
            if (wsServer != null) {
                LibertyDockerServer serverExt = (LibertyDockerServer) wsServer.getAdapter(LibertyDockerServer.class);
                if (serverExt != null) {
                    return serverExt.getContainerName(wsServer);
                }
            }
        }
        return null;
    }
}
