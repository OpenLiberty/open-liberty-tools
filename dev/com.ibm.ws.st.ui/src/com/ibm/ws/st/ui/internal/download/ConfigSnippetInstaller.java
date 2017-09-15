/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.download;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.repository.AbstractInstaller;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;

/**
 * A config snippet installer
 */
public class ConfigSnippetInstaller extends AbstractInstaller {

    @Override
    public IStatus install(IProduct product, PasswordAuthentication pa, Map<String, Object> settings, IProgressMonitor monitor2) {
        WebSphereServerInfo server = (WebSphereServerInfo) settings.get(WEBSPHERE_SERVER);
        if (server == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorServerMissing, new IOException(Messages.errorServerMissing));
        }

        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        File configFile = null;
        File tempConfig = null;
        boolean success = false;
        try {
            monitor.beginTask(Messages.jobInstallingRuntime, 100);

            String configFileName = getConfigFileName(product);
            if (configFileName == null) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorServerConfigMissing, new IOException(Messages.errorServerConfigMissing));
            }

            ConfigurationFile serverConfig = server.getConfigRoot();
            IPath configDir = serverConfig.getPath().removeLastSegments(1);
            configFile = configDir.append(configFileName).toFile();

            if (configFile.exists()) {
                tempConfig = new File(configFile.getAbsolutePath() + ".tmp");
                if (!configFile.renameTo(new File(configFile.getAbsolutePath() + ".tmp"))) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind(Messages.errorBackupFile, configFile.getAbsolutePath()), null);
                }
            }

            monitor.subTask(Messages.taskConnecting);
            monitor.worked(10);

            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;

            DownloadHelper.download(product, configFile, new SubProgressMonitor(monitor, 40));

            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;

            install2(product.getName(), serverConfig, configDir, configFileName, new SubProgressMonitor(monitor, 40));

            success = true;
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        } finally {
            if (success) {
                if (tempConfig != null && tempConfig.exists()) {
                    tempConfig.delete();
                }
            } else {
                if (configFile != null && configFile.exists())
                    configFile.delete();

                if (tempConfig != null && tempConfig.exists())
                    tempConfig.renameTo(configFile);
            }
            monitor.done();
        }

        return Status.OK_STATUS;
    }

    private String getConfigFileName(IProduct p) {
        String location = p.getSource().getLocation();
        if (location == null)
            return null;

        location = location.replace('\\', '/');
        int index = location.lastIndexOf('/');

        return (index == -1) ? location.trim() : location.substring(index + 1, location.length()).trim();
    }

    private void install2(String productName, ConfigurationFile serverConfig, IPath configDir, String configFileName, IProgressMonitor monitor) throws IOException {
        // Include the generated config file in the server config if it isn't already there
        try {
            monitor.beginTask(productName, 100);
            monitor.subTask(productName);
            monitor.worked(25);
            boolean includeExists = false;
            for (ConfigurationFile config : serverConfig.getAllIncludedFiles()) {
                if (config.getPath().toOSString().equals(configDir.append(configFileName).toOSString())) {
                    includeExists = true;
                    break;
                }
            }

            monitor.worked(25);

            // Add the include to the server config if it isn't already added
            if (!includeExists) {
                serverConfig.addInclude(false, configFileName);
                serverConfig.save(new SubProgressMonitor(monitor, 40));
            }
        } finally {
            monitor.done();
        }
    }
}
