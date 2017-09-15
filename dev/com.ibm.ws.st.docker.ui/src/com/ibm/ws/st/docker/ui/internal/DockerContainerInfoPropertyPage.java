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

import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.common.core.ext.internal.util.BaseDockerContainer;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.docker.core.internal.launch.LibertyDockerServer;

/**
 * Property page for a liberty server running in a docker container
 */
public class DockerContainerInfoPropertyPage extends PropertyPage {

    private static final int INDENT = 10;

    public DockerContainerInfoPropertyPage() {
        super();
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent) {
        try {
            IAdaptable element = getElement();
            IServer server = (IServer) element.getAdapter(IServer.class);
            BaseDockerContainer container = null;
            Map<String, String> serviceInfo = null;
            if (server != null) {
                WebSphereServer wsServer = (WebSphereServer) server.getAdapter(WebSphereServer.class);
                if (wsServer != null) {
                    serviceInfo = wsServer.getServiceInfo();
                    LibertyDockerServer serverExt = (LibertyDockerServer) wsServer.getAdapter(LibertyDockerServer.class);
                    if (serverExt != null) {
                        container = serverExt.getContainer(wsServer);
                    }
                }
            }

            final Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.numColumns = 2;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            // progress info
            final Label progressLabel = new Label(composite, SWT.NONE);
            progressLabel.setText(Messages.dockerInfoTask);
            progressLabel.setLayoutData(new GridData(GridData.FILL, GridData.END, true, true));

            final ProgressIndicator progress = new ProgressIndicator(composite);
            progress.setLayoutData(new GridData(GridData.FILL, GridData.END, true, false));

            Dialog.applyDialogFont(composite);

            if (container == null || serviceInfo == null) {
                Label label = new Label(composite, SWT.READ_ONLY | SWT.NO_TRIM);
                label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
                label.setText(Messages.dockerInfoUnavailable);

                progressLabel.dispose();
                progress.dispose();
            } else {
                final Display display = getShell().getDisplay();
                final BaseDockerContainer container2 = container;
                final Map<String, String> serviceInfo2 = serviceInfo;
                Thread t = new Thread("Docker container info") {
                    @Override
                    public void run() {
                        IProgressMonitor monitor = new NullProgressMonitor() {
                            @Override
                            public void beginTask(final String name, final int max) {
                                display.syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.beginTask(max);
                                    }
                                });
                            }

                            @Override
                            public void worked(final int work) {
                                display.syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.worked(work);
                                    }
                                });
                            }
                        };

                        monitor.beginTask(Messages.dockerInfoTask, 100);

                        try {
                            // Getting the below information requires docker calls so can be slow
                            String hostIP = null;
                            if (container2.getDockerMachine().isRealMachine()) {
                                hostIP = container2.getDockerMachine().getHost();
                            }
                            monitor.worked(50);
                            String currentImage = container2.getImageName();
                            monitor.worked(50);

                            monitor.done();

                            final String hostIP2 = hostIP;
                            final String currentImage2 = currentImage;

                            display.syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    progressLabel.dispose();
                                    progress.dispose();

                                    // Current working container info - only show if different from the
                                    // original container
                                    boolean isOriginalContainer = container2.getContainerName().equals(serviceInfo2.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_CONTAINER));
                                    if (!isOriginalContainer) {
                                        createHeader(composite, Messages.dockerInfoWorkingContainerLabel);
                                        createItem(composite, Messages.dockerInfoName, container2.getContainerName());
                                        createItem(composite, Messages.dockerInfoImageName, currentImage2);
                                    }

                                    // Original container info
                                    if (isOriginalContainer) {
                                        createHeader(composite, Messages.dockerInfoContainerLabel);
                                    } else {
                                        createHeader(composite, Messages.dockerInfoOrigContainerLabel);
                                    }
                                    createItem(composite, Messages.dockerInfoName, serviceInfo2.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_CONTAINER));
                                    createItem(composite, Messages.dockerInfoImageName, serviceInfo2.get(com.ibm.ws.st.common.core.ext.internal.Constants.DOCKER_IMAGE));

                                    // Docker machine info - only show on platforms where docker does not
                                    // run natively
                                    if (container2.getDockerMachine().isRealMachine()) {
                                        createHeader(composite, Messages.dockerInfoMachineLabel);
                                        createItem(composite, Messages.dockerInfoName, container2.getDockerMachine().getMachineName());
                                        createItem(composite, Messages.dockerInfoMachineIP, hostIP2);
                                    }

                                    composite.layout(true);
                                }
                            });

                        } catch (Exception e) {
                            display.syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    progressLabel.dispose();
                                    progress.dispose();

                                    Label label = new Label(composite, SWT.READ_ONLY | SWT.NO_TRIM);
                                    label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
                                    label.setText(Messages.dockerInfoUnavailable);

                                    composite.layout(true);
                                }
                            });
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }

            return composite;
        } catch (Exception e) {
            Trace.logError("Error creating product info property page", e);
            return null;
        }
    }

    protected void createHeader(Composite composite, String str) {
        StyledText label = new StyledText(composite, SWT.READ_ONLY | SWT.NO_TRIM);
        label.setForeground(composite.getForeground());
        label.setBackground(composite.getBackground());
        label.setText(str);
        StyleRange range = new StyleRange();
        range.start = 0;
        range.length = str.length();
        range.fontStyle = SWT.BOLD;
        label.setStyleRange(range);

        GridData data = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        data.verticalIndent = 5;
        data.horizontalSpan = 2;
        label.setLayoutData(data);
    }

    protected void createItem(Composite composite, String labelStr, String value) {
        Label label = new Label(composite, SWT.NONE);
        label.setText(labelStr);
        GridData data = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
        data.horizontalIndent = INDENT;
        label.setLayoutData(data);

        Text text = new Text(composite, SWT.READ_ONLY | SWT.NO_TRIM);
        text.setText(value);
        data = new GridData(GridData.FILL, GridData.BEGINNING, false, false);
        text.setLayoutData(data);
    }
}
