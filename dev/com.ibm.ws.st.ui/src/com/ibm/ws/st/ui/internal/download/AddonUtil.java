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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.ws.st.common.core.internal.TraceLevel;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.generation.Metadata;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;

public class AddonUtil {

    protected static void refreshMetadata(WebSphereRuntime runtime, IProgressMonitor monitor2) {
        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        final boolean[] done = new boolean[] { false };
        final String runtimeName = runtime.getRuntime().getName();
        final int totalWork = 700;
        int worked = 100;
        String message = NLS.bind(Messages.taskRefreshingMetadata, runtimeName);

        try {
            monitor.beginTask(message, totalWork);
            monitor.subTask(message);
            monitor.worked(worked);
            runtime.generateMetadata(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    done[0] = true;
                    if (!event.getResult().isOK()) {
                        if (Trace.ENABLED) {
                            Trace.trace(TraceLevel.WARNING, "Metadata refresh did not finish successfully for " + runtimeName);
                        }
                    }
                    event.getJob().removeJobChangeListener(this);
                }
            }, true, Metadata.ALL_METADATA);

            while (!monitor.isCanceled() && !done[0]) {
                try {
                    Thread.sleep(300);
                    if (worked < totalWork) {
                        monitor.worked(50);
                        worked += 50;
                    }
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            if (worked < totalWork) {
                monitor.worked(totalWork - worked);
            }
        } finally {
            monitor.done();
        }
    }

    protected static boolean isRefreshMetadataRequired(List<IProduct> selectedList) {
        for (IProduct p : selectedList) {
            List<String> features = p.getProvideFeature();
            if (features != null && !features.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    protected static void refreshServerFolder(final WebSphereServerInfo server) {
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

    public static void showResult(final Shell shell, Map<IProduct, IStatus> statusMap) {
        // Sanity check - statusList should not be empty and at least the first
        //                item should have been installed successfully.
        if (statusMap.isEmpty()) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "The install status is either empty or there was no successful install");
            return;
        }
        final StringBuilder detailedMessage = new StringBuilder();
        StringBuilder productsInstalled = new StringBuilder();
        StringBuilder productsfailed = new StringBuilder();
        StringBuilder productsCancelled = new StringBuilder();
        final StringBuilder messageDescription = new StringBuilder();

        String successMessage = "";
        String failureMessage = "";

        for (IProduct product : statusMap.keySet()) {
            IStatus status = statusMap.get(product);
            if (status == Status.OK_STATUS) { // products that installed successfully
                productsInstalled.append("    - ").append(product.getName()).append("\n");
            } else if (status == Status.CANCEL_STATUS) { //products that were cancelled
                productsCancelled.append("    - ").append(product.getName()).append("\n");
            } else if (status != Status.OK_STATUS) { // products that failed
                productsfailed.append("    - ").append(product.getName()).append("\n");
                if (!status.getMessage().isEmpty()) { // Detailed message for scrollable composite
                    detailedMessage.append("    - ").append(product.getName()).append("\n");
                    detailedMessage.append(status.getMessage()).append("\n").append("\n");
                }
            }

        }

        if (productsInstalled.length() > 0) {
            successMessage = NLS.bind(Messages.wizInstallSuccess, productsInstalled.toString());
            messageDescription.append(successMessage).append("\n");
        }

        if (productsCancelled.length() > 0) {
            productsCancelled.append(Messages.wizInstallCancelled);
            detailedMessage.append(productsCancelled);
            messageDescription.append(productsCancelled).append("\n");
        }

        if (productsfailed.length() > 0) {
            failureMessage = NLS.bind(Messages.wizInstallFailure, productsfailed.toString());
            messageDescription.append(failureMessage).append("\n");

        }

        // If there are no failure message , use Information Icon
        final int dispIcon = failureMessage.length() > 0 ? MessageDialog.WARNING : MessageDialog.INFORMATION;

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (detailedMessage.length() > 0)
                    Trace.logError("Install Failed :" + detailedMessage.toString(), null);
                new ScrollableMessageDialog(shell, detailedMessage.toString(), dispIcon, messageDescription.toString()).open();
            }
        });
    }

    public static Map<IProduct, License> createLicenseMap() {
        return new ConcurrentHashMap<IProduct, License>();
    }
}
