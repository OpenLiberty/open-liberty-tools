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
package com.ibm.ws.st.docker.ui.internal;

import java.net.ConnectException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.docker.core.internal.AbstractModeSwitchHandler;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerServer;

/**
 *
 */
public class ModeSwitchHandler extends AbstractModeSwitchHandler {

    /** {@inheritDoc} */
    @Override
    public void handleExecutionModeSwitch(WebSphereServer server) {
        // If this is a docker server and the current container is the original user container then
        // then show an informational dialog to tell the user about the container and image
        // switch that is necessary on an execution mode switch.
        if (!Activator.SERVER_TYPE.equals(server.getServerType())) {
            return;
        }
        LibertyDockerServer dockerServer = (LibertyDockerServer) server.getAdapter(LibertyDockerServer.class);
        if (dockerServer == null || !dockerServer.isUserContainer(server)) {
            return;
        }

        BaseDockerContainer container;
        try {
            container = dockerServer.getContainer(server);
            if (container == null) {
                Trace.logError("The Docker container was null for server: " + server.getServerName(), null);
                return;
            }
        } catch (UnsupportedServiceException e) {
            Trace.logError("Failed to retrieve Docker container information for server: " + server.getServerName(), e);
            return;
        }

        String containerName = container.getContainerName();
        String imageName;
        try {
            imageName = container.getImageName();
        } catch (ConnectException e) {
            Trace.logError("Could not get the image name for container: " + containerName, e);
            return;
        }

        final String newContainerName = NLS.bind(Messages.dockerServerModeSwitchContainerFormat, containerName);
        final String newImageName = NLS.bind(Messages.dockerServerModeSwitchImageFormat, new String[] { containerName.toLowerCase(), imageName });

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = Display.getDefault().getActiveShell();
                MessageDialog.openInformation(shell, Messages.dockerServerModeSwitchTitle,
                                              NLS.bind(Messages.dockerServerModeSwitchMessage, new String[] { newImageName, newContainerName }));
            }
        });
    }

}
