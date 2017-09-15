/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.setuphandlers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.util.SocketUtil;

import com.ibm.ws.st.common.core.ext.internal.Activator;
import com.ibm.ws.st.common.core.ext.internal.Constants;
import com.ibm.ws.st.common.core.ext.internal.Messages;
import com.ibm.ws.st.common.core.ext.internal.Trace;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.common.core.ext.internal.util.RemoteDockerContainer;

public class PlatformHandlerFactory {

    /**
     * Use COMMAND if you don't have a specific protocol in mind but want to execute command-line
     * commands in a non-docker environment.
     */
    public enum PlatformType {
        COMMAND,
        SSH_KEYLESS,
        DOCKER
    }

    public static IPlatformHandler getPlatformHandler(Map<String, String> serviceInfo, PlatformType type) throws UnsupportedServiceException {
        Map<String, String> map = serviceInfo;
        if (map == null) {
            map = Collections.emptyMap();
        }

        String hostname = map.get(Constants.HOSTNAME);
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Attempting to find a handler for to match the host: " + hostname + " service information: " + getServiceInfoString(map));
        switch (type) {
            case DOCKER:
                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Retrieving Docker handler.");
                String machineType = map.get(Constants.DOCKER_MACHINE_TYPE);
                String machineName = map.get(Constants.DOCKER_MACHINE);
                String containerName = map.get(Constants.DOCKER_CONTAINER);
                IPlatformHandler platformHandler = PlatformHandlerFactory.getPlatformHandler(serviceInfo, PlatformType.COMMAND);
                if (hostname == null || SocketUtil.isLocalhost(hostname)) {
                    if (Trace.ENABLED) {
                        String name = machineName == null ? containerName : containerName + " (" + machineName + ")";
                        Trace.trace(Trace.INFO, "Creating a local docker container for: " + name);
                    }
                    return new BaseDockerContainer(containerName, machineType, machineName, platformHandler);
                }
                if (Trace.ENABLED) {
                    String name = machineName == null ? containerName : containerName + " (" + machineName + ")";
                    Trace.trace(Trace.INFO, "Creating a remote docker container for: " + name);
                }
                return new RemoteDockerContainer(containerName, machineType, machineName, platformHandler);

            case COMMAND:
                /*
                 * The COMMAND type will fall back on SSH if the hostname doesn't resolve to localhost
                 * or is not provided at all
                 */
                if (hostname == null || SocketUtil.isLocalhost(hostname)) {
                    if (Trace.ENABLED) {
                        Trace.trace(Trace.INFO, "Creating a local command handler.");
                    }
                    return new LocalHandler();
                }

            case SSH_KEYLESS:
                if (Trace.ENABLED) {
                    Trace.trace(Trace.INFO, "Creating a remote command handler of type: " + PlatformType.SSH_KEYLESS.name());
                }
                try {
                    IPlatformHandlerProvider provider = Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name());

                    if (provider == null)
                        throw new UnsupportedServiceException(NLS.bind(Messages.errorNoSupportedPlatformFound, type));

                    return provider.init(serviceInfo);
                } catch (Throwable t) {
                    if (Trace.ENABLED)
                        Trace.logError("The SSH Handler could not be created.", t);
                }

                throw new UnsupportedServiceException(NLS.bind(Messages.errorNoSupportedPlatformFound, type));

            default:
                throw new UnsupportedServiceException(NLS.bind(Messages.errorNoSupportedPlatformFound, type));
        }
    }

    /**
     * @param serviceInfo
     * @return String representation of the map
     */
    private static String getServiceInfoString(Map<String, String> serviceInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, String> entry : serviceInfo.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append("(" + key + " : " + value + ") ");
        }
        sb.append("}");
        return sb.toString();
    }

}
