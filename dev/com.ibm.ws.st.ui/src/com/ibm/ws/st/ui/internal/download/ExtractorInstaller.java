/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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

import com.ibm.ws.st.core.internal.repository.AbstractInstaller;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public class ExtractorInstaller extends AbstractInstaller {

    private static String EXT_JAR = ".jar";

    @Override
    public IStatus install(IProduct product, PasswordAuthentication pa, Map<String, Object> settings, IProgressMonitor monitor2) {
        IPath installPath = (IPath) settings.get(RUNTIME_LOCATION);
        if (installPath == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorRuntimeLocationMissing, new IOException(Messages.errorRuntimeLocationMissing));
        }

        File temp = null;
        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        monitor.beginTask(Messages.jobInstallingRuntime, 100);

        try {
            if (product instanceof LocalProduct) {
                File archiveFile = new File(product.getSource().getLocation());
                if (archiveFile.isFile())
                    DownloadHelper.unzip(archiveFile, installPath, archiveFile.length(), new SubProgressMonitor(monitor, 100), product.getName());
                return Status.OK_STATUS;
            }

            temp = File.createTempFile(TEMP_FILE_NAME, EXT_JAR);
            DownloadHelper.download(product, temp, new SubProgressMonitor(monitor, 80));

            String checksum = product.getHashSHA256();

            if (!verifyDownload(temp, checksum))
                throw new Exception(NLS.bind(Messages.errorVerificationFailed, product.getName()));

            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;

            DownloadHelper.unzip(temp, installPath, temp.length(), new SubProgressMonitor(monitor, 20), product.getName());
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        } finally {
            // cleanup temp file
            if (temp != null && temp.exists() && !temp.delete()) {
                if (Trace.ENABLED) {
                    Trace.trace(Trace.WARNING, "Could not delete file: " + temp.getName());
                }
            }
            monitor.done();
        }

        return Status.OK_STATUS;
    }

    private boolean verifyDownload(File temp, String checksum) {
        if (checksum == null)
            return true;

        String fileHash;
        try {
            fileHash = HashUtils.getFileSHA256String(temp);
            return fileHash.equals(checksum);
        } catch (IOException e) {
            Trace.logError("Failed to generate hash for file " + temp, e);
        }

        return false;
    }
}
