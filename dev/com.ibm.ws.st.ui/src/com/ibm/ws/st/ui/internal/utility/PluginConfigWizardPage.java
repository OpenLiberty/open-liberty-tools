/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.utility;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

/**
 * Wizard page to generate web server plug-in configuration.
 */
public class PluginConfigWizardPage extends UtilityWizardPage {
    public PluginConfigWizardPage(WebSphereServerInfo server) {
        super(server);
        setTitle(Messages.wizPluginConfigTitle);
        setDescription(Messages.wizPluginConfigDescription);
    }

    @Override
    public void createUtilityControl(Composite comp) {
        // use async so the message appears after page has been created
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!server.getConfigRoot().hasFeature(Constants.FEATURE_LOCAL_JMX) ||
                        !server.getWebSphereRuntime().isServerStarted(server, null)) {
                        setMessage(Messages.wizPluginConfigError, IMessageProvider.ERROR);
                        setPageComplete(false);
                    }
                } catch (CoreException ce) {
                    Trace.logError("Error checking state for server: " + server.getServerName(), ce);
                }
            }
        });
    }

    @Override
    protected String getUserMessage() {
        IPath pluginConfigPath = server.getServerOutputPath().append("plugin-cfg.xml");
        try {
            if (!server.getWebSphereRuntime().isServerStarted(server, null))
                return NLS.bind(Messages.wizPluginConfigMessage, pluginConfigPath.toString()).concat(Messages.wizPluginConfigStoppedMessage);
        } catch (CoreException ce) {
            Trace.logError("Error checking state for server: " + server.getServerName(), ce);
        }
        return NLS.bind(Messages.wizPluginConfigMessage, pluginConfigPath.toString());
    }

    public void showPluginConfigFile(final WebSphereServerInfo server) {
        Job job = new Job(Messages.wizRefreshServerFolderJob) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IFolder serverFolder = server.getServerFolder();
                if (serverFolder != null && serverFolder.exists()) {
                    try {
                        serverFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    } catch (CoreException ce) {
                        if (Trace.ENABLED)
                            Trace.trace(Trace.WARNING, "Couldn't refresh server folder: " + serverFolder.getName(), ce);
                    }
                }

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        IPath serverOutputPath = server.getServerOutputPath();
                        File pluginConfigFile = serverOutputPath.append("plugin-cfg.xml").toFile();
                        if (pluginConfigFile != null && pluginConfigFile.exists()) {
                            IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(server.getServerOutputPath().append("plugin-cfg.xml").toFile().toURI());
                            if (files.length > 0) {
                                IFile file = files[0];
                                Activator.showResource(file);
                            }
                        }
                    }
                });
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return Constants.JOB_FAMILY.equals(family);
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    @Override
    public void finish(IProgressMonitor monitor) throws Exception {
        try {
            server.generatePluginConfig();
            showPluginConfigFile(server);
        } catch (Exception e) {
            Trace.logError("Generating web server plug-in configuration failed.", e);
            throw new Exception(Messages.wizPluginConfigFailed, e);
        }
    }
}
