/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.util;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.ValidationResults;
import org.eclipse.wst.validation.ValidatorMessage;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.tests.TestsPlugin;

public class TestUtil {
    public static IRuntime createRuntime(String typeId, String location, String name, IPath user_dir) throws Exception {
        IPath runtimePath = new Path(location);

        IServerType st = ServerCore.findServerType(typeId);
        IRuntimeWorkingCopy wc = st.getRuntimeType().createRuntime(null, null);
        wc.setLocation(runtimePath);
        if (name != null)
            wc.setName(name);

        // Force a meta data creation and wait for the operation to finish.
        // This way we can be sure that it was created before we continue
        // with the rest of test.
        WebSphereRuntime wr = (WebSphereRuntime) wc.loadAdapter(WebSphereRuntime.class, null);
        createMetaData(wr);

        if (user_dir != null) {
            wr.addUserDirectory(user_dir);
        }

        return wc.save(true, null);
    }

    public static IRuntime createRuntime(String typeId, String location, String name) throws Exception {
        return createRuntime(typeId, location, name, null);
    }

    public static void createMetaData(final WebSphereRuntime wsRuntime) {
        final boolean[] done = new boolean[] { false };
        wsRuntime.createMetadata(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                done[0] = true;
                event.getJob().removeJobChangeListener(this);
            }
        });

        // Wait up to 1 minute for the generation
        WLPCommonUtil.print("Wait for meta data generation to complete.");
        for (int i = 0; i < 240 && !done[0]; ++i) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // ignore;
            }
        }

        if (done[0])
            WLPCommonUtil.print("Meta data generation is complete");
        else
            WLPCommonUtil.print("Meta data generation is taking more time to complete");
    }

    public static void createVM(String vmName) throws Exception {
        IVMInstallType type = JavaRuntime.getVMInstallType("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");
        IVMInstall[] installs = type.getVMInstalls();
        for (IVMInstall vm : installs) {
            if (vmName.equals(vm.getName()))
                return;
        }

        VMStandin vm = new VMStandin(type, vmName);
        vm.setInstallLocation(JavaRuntime.getDefaultVMInstall().getInstallLocation());

        vm.setName(vmName);
        vm.convertToRealVM();
        // JVM needs to be set as default else existing default workspace JRE will be used.
        JavaRuntime.setDefaultVMInstall(vm, null, true);
        JavaRuntime.saveVMConfiguration();
    }

    public static void setupRuntimeServers(String runtimeLocation, IPath resourcePath) throws IOException {
        IPath runtimePath = (new Path(runtimeLocation)).append("/usr/servers/");
        FileUtil.copyFiles(resourcePath.toOSString(), runtimePath.toOSString());
    }

    public static void createRuntimeServer(String runtimeLocation, String serverName, IPath serverContentPath) throws IOException {
        IPath path = (new Path(runtimeLocation)).append("/usr/servers/").append(serverName);
        FileUtil.copyFiles(serverContentPath.toOSString(), path.toOSString());
    }

    // Run the validator on the given resource.
    public static ValidatorMessage[] validate(IFile resource) throws CoreException {
        ValidationFramework valFram = org.eclipse.wst.validation.ValidationFramework.getDefault();
        ValidationResults result = valFram.validate(resource, new NullProgressMonitor());
        ValidatorMessage[] messages = result.getMessages();
        assertTrue(messages != null);
        return messages;
    }

    public static boolean jobWait(Object family) {
        return WLPCommonUtil.jobWait(family);
    }

    public static boolean jobWaitBuildandResource() {
        return WLPCommonUtil.jobWaitBuildandResource();
    }

    public static boolean jobWaitBuildandResource(String message) {
        if (message == null || message.isEmpty())
            return WLPCommonUtil.jobWaitBuildandResource();

        return WLPCommonUtil.jobWaitBuildandResource(message);
    }

    // Refresh a resource and wait to make sure it is refreshed.
    public static boolean refreshResource(final IResource resource) {
        WorkspaceJob wsJob = new WorkspaceJob("Refreshing resource: " + resource.getName()) {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) {
                try {
                    resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    return Status.OK_STATUS;
                } catch (CoreException e) {
                    return new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, "Failed to refresh the resource: " + resource.getName(), e);
                }
            }
        };
        IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
        ISchedulingRule rule = ruleFactory.refreshRule(resource);
        wsJob.setRule(rule);
        wsJob.setPriority(Job.LONG);
        wsJob.schedule();
        try {
            wsJob.join();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    // Delete a resource and wait to make sure it is deleted.
    public static boolean deleteResource(final IResource resource) {
        WorkspaceJob wsJob = new WorkspaceJob("Deleting resource: " + resource.getName()) {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) {
                try {
                    resource.delete(true, monitor);
                    return Status.OK_STATUS;
                } catch (CoreException e) {
                    return new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, "Failed to refresh the resource: " + resource.getName(), e);
                }
            }
        };
        IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
        ISchedulingRule rule = ruleFactory.refreshRule(resource);
        wsJob.setRule(rule);
        wsJob.setPriority(Job.LONG);
        wsJob.schedule();
        try {
            wsJob.join();
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public static void waitForJobsToComplete(String[] jobNames) {
        if (jobNames == null || jobNames.length == 0) {
            return;
        }

        final IJobManager jobManager = Job.getJobManager();
        boolean completed = false;
        int printCounter = 0;
        while (!completed) {
            boolean stillRunning = false;
            String waitingJobName = "";
            for (Job job : jobManager.find(null)) {
                String name = job.getName();
                for (String jobName : jobNames) {
                    if (name.indexOf(jobName) >= 0) {
                        stillRunning = true;
                        waitingJobName = name;
                        break;
                    }
                }
                if (stillRunning)
                    break;
            }
            if (!stillRunning) {
                completed = true;
            } else {
                if (printCounter == 8) { // print each 2 seconds
                    WLPCommonUtil.print("wait for " + waitingJobName + " to complete.");
                    printCounter = 0;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // do nothing
                }
                printCounter++;
            }
        }
    }

    public static void waitForJobsToComplete(Object family) {
        if (family == null) {
            return;
        }

        final IJobManager jobManager = Job.getJobManager();
        boolean completed = false;
        int printCounter = 0;
        while (!completed) {
            boolean stillRunning = false;
            String waitingJobName = "";
            for (Job job : jobManager.find(null)) {
                if (job.belongsTo(family)) {
                    waitingJobName = job.getName();
                    stillRunning = true;
                    break;
                }
            }
            if (!stillRunning) {
                completed = true;
            } else {
                if (printCounter == 8) { // print each 2 seconds
                    WLPCommonUtil.print("wait for " + waitingJobName + " to complete.");
                    printCounter = 0;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    // do nothing
                }
                printCounter++;
            }
        }
    }

    public static void wait(String msg, int ms) {
        WLPCommonUtil.wait(msg, ms);
    }

    public static void modifyFile(IProject project, String relPath, String orig, String replace) throws Exception {
        IPath path = project.getLocation().append(relPath);
        modifyFile(path.toFile(), orig, replace);
    }

    public static void modifyFile(File file, String orig, String replace) throws Exception {
        // Read into a string
        int len = (int) file.length();
        byte[] bytes = new byte[len];
        FileInputStream inputStream = null;
        String string;
        try {
            inputStream = new FileInputStream(file);
            inputStream.read(bytes, 0, len);
            string = new String(bytes);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        // Do the replace
        string = string.replace(orig, replace);

        // Write out the file
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bytes = string.getBytes();
            outputStream.write(bytes, 0, bytes.length);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

}