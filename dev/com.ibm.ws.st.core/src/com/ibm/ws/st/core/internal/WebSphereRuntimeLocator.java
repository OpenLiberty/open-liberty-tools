/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.File;
import java.io.FileFilter;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.model.RuntimeLocatorDelegate;

public class WebSphereRuntimeLocator extends RuntimeLocatorDelegate {
    private static final String[] runtimeTypes = new String[] { Constants.RUNTIME_TYPE_ID, Constants.RUNTIMEV85_TYPE_ID };

    @Override
    public void searchForRuntimes(IPath origPath, IRuntimeSearchListener listener, IProgressMonitor monitor) {
        IPath path = origPath;
        if (path == null)
            path = Path.ROOT;

        File f = path.toFile();
        if (!f.exists())
            return;

        File[] files = f.listFiles();
        if (files != null) {
            int size = files.length;
            int work = 100 / size;
            int workLeft = 100 - (work * size);
            for (int i = 0; i < size; i++) {
                if (monitor.isCanceled())
                    return;
                if (files[i] != null && files[i].isDirectory())
                    searchPath(listener, path.append(files[i].getName()), 4, monitor);
                monitor.worked(work);
            }
            monitor.worked(workLeft);
        } else
            monitor.worked(100);
    }

    protected static void searchPath(IRuntimeSearchListener listener, IPath path, int depth, IProgressMonitor monitor) {
        if (WebSphereRuntime.validateLocation(path).isOK()) {
            IRuntimeWorkingCopy runtime = getRuntimeFromDir(path, monitor);
            if (runtime != null) {
                listener.runtimeFound(runtime);
                return;
            }
        } else {
            if (depth == 0)
                return;

            File[] files = path.toFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            if (files != null) {
                int size = files.length;
                for (int i = 0; i < size; i++) {
                    if (monitor.isCanceled())
                        return;
                    searchPath(listener, path.append(files[i].getName()), depth - 1, monitor);
                }
            }
        }
    }

    public static IRuntimeWorkingCopy getRuntimeFromDir(IPath path, IProgressMonitor monitor) {
        for (int i = 0; i < runtimeTypes.length; i++) {
            try {
                IRuntimeType runtimeType = ServerCore.findRuntimeType(runtimeTypes[i]);

                // ignore existing runtimes
                IRuntime[] runtimes = ServerCore.getRuntimes();
                for (IRuntime runtime : runtimes) {
                    if (runtime.getLocation() != null && runtime.getLocation().equals(path)) {
                        return null;
                    }
                }

                // create new runtime with non-conflicting name
                String name = WebSphereUtil.getUniqueRuntimeName(path.lastSegment(), runtimes);

                String id = path.toOSString().replace(File.separatorChar, '_').replace(':', '-');
                IRuntimeWorkingCopy runtime = runtimeType.createRuntime(id, monitor);
                runtime.setName(name);
                runtime.setLocation(path);
                WebSphereRuntime wc = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                wc.setVMInstall(JavaRuntime.getDefaultVMInstall());
                IStatus status = runtime.validate(monitor);
                if (status == null || status.getSeverity() != IStatus.ERROR) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.INFO, "Runtime found at " + path.toOSString());
                    return runtime;
                }

                if (Trace.ENABLED)
                    Trace.trace(Trace.INFO, "Invalid runtime found at " + path.toOSString() + ": " + status.getMessage());
            } catch (Exception e) {
                Trace.logError("Could not find runtime", e);
            }
        }
        return null;
    }
}