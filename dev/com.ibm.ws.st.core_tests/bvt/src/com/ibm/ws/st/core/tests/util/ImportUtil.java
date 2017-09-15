/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import com.ibm.ws.st.core.tests.TestsPlugin;

public class ImportUtil {
    public static void importExistingProjectIntoWorkspace(final String projectName, final IPath importRoot) throws CoreException {
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                File importSource = importRoot.append(projectName).toFile();
                List<?> filesToImport = FileSystemStructureProvider.INSTANCE.getChildren(importSource);

                final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                final IProject project = workspace.getRoot().getProject(projectName);

                OverwriteQuery q = new OverwriteQuery();
                ImportOperation op = new ImportOperation(project.getFullPath(), importSource,
                                FileSystemStructureProvider.INSTANCE, q, filesToImport);
                op.setOverwriteResources(true); // need to overwrite
                op.setCreateContainerStructure(false);
                try {
                    op.run(monitor);
                    monitor.done();
                } catch (InvocationTargetException e) {
                    throw new CoreException(new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, "error when import. ", e));
                } catch (InterruptedException e) {
                    throw new CoreException(new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, "error when import. ", e));
                }
            }
        };

        ResourcesPlugin.getWorkspace().run(runnable, ResourcesPlugin.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE,
                                           new NullProgressMonitor());
    }

    public static void importFile(final IPath destPath, final File importFile) throws CoreException {
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                File sourcePath = importFile.getParentFile();
                OverwriteQuery q = new OverwriteQuery();
                List<File> fileList = new ArrayList<File>(1);
                fileList.add(importFile);
                ImportOperation op = new ImportOperation(destPath, sourcePath,
                                FileSystemStructureProvider.INSTANCE, q, fileList);
                op.setOverwriteResources(true); // need to overwrite
                op.setCreateContainerStructure(false);
                try {
                    op.run(monitor);
                    monitor.done();
                } catch (InvocationTargetException e) {
                    throw new CoreException(new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, "error when import. ", e));
                } catch (InterruptedException e) {
                    throw new CoreException(new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, "error when import. ", e));
                }
            }
        };

        ResourcesPlugin.getWorkspace().run(runnable, ResourcesPlugin.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE,
                                           new NullProgressMonitor());
    }
}