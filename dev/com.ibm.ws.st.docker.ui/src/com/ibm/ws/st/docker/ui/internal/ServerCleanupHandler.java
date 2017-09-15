/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.ui.internal;

import java.util.Properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.PromptUtil;
import com.ibm.ws.st.docker.core.internal.AbstractServerCleanupHandler;

/**
 * Server cleanup handler. Pops up a dialog asking the user if they want to
 * save the temporary container and image created by the tools.
 */
public class ServerCleanupHandler extends AbstractServerCleanupHandler {

    /** {@inheritDoc} */
    @Override
    public void handleServerDelete(BaseDockerContainer container, final Properties properties) {
        final boolean[] response = { true };

        try {
            final String containerName = container.getContainerName();
            final String imageName = container.getImageName();
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    // Suppress the dialog in headless mode, and go ahead and delete the container.
                    if (PromptUtil.isSuppressDialog()) {
                        response[0] = false;
                    } else {
                        Shell shell = Display.getDefault().getActiveShell();
                        response[0] = MessageDialog.openQuestion(shell, Messages.dockerServerDeleteTitle,
                                                                 NLS.bind(Messages.dockerServerDeleteRemoveArtifacts, new String[] { containerName, imageName }));
                    }
                }
            });

            if (!response[0]) {
                try {
                    if (container.isRunning()) {
                        container.stop();
                    }
                    container.getDockerMachine().removeContainer(container.getContainerName());
                } catch (final Exception e) {
                    Trace.logError("Failed to remove Docker container: " + containerName, e);
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            Shell shell = Display.getDefault().getActiveShell();
                            MessageDialog.openError(shell, Messages.dockerServerDeleteErrorTitle,
                                                    NLS.bind(Messages.dockerServerDeleteRemoveContainerFailed, new String[] { containerName, e.getLocalizedMessage() }));
                        }
                    });
                    return;
                }
                try {
                    // #233566: Just like debug mode, for loose config, we commit to a new image now.   So delete the image too.
                    container.getDockerMachine().removeImage(imageName);
                } catch (final Exception e) {
                    Trace.logError("Failed to remove Docker image: " + imageName, e);
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            Shell shell = Display.getDefault().getActiveShell();
                            MessageDialog.openError(shell, Messages.dockerServerDeleteErrorTitle,
                                                    NLS.bind(Messages.dockerServerDeleteRemoveImageFailed, new String[] { imageName, e.getLocalizedMessage() }));
                        }
                    });
                }
            }
        } catch (Exception e) {
            Trace.logError("Failed to get the image name for Docker container: " + container.getContainerName(), e);
        }
    }

}
