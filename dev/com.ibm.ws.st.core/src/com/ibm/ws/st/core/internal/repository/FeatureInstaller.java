/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.repository;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.osgi.util.NLS;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.LaunchUtil;
import com.ibm.ws.st.core.internal.Messages;
import com.ibm.ws.st.core.internal.ProcessHelper;
import com.ibm.ws.st.core.internal.ProcessHelper.ProcessResult;
import com.ibm.ws.st.core.internal.Trace;
import com.ibm.ws.st.core.internal.repository.IProduct.ProductType;

public class FeatureInstaller extends AbstractInstaller {

    private static final String ASSET_LOCATION_OVERRIDE_PROP_NAME = "repository.description.url";
    private static final String REPOSITORIES_LOCATION_OVERRIDE_PROP_NAME = "WLP_REPOSITORIES_PROPS";
    private static final String FEATURE_MANAGER_CMD = "featureManager";

    @Override
    public IStatus install(IProduct product, PasswordAuthentication pa, Map<String, Object> settings, IProgressMonitor monitor2) {
        IPath installPath = (IPath) settings.get(RUNTIME_LOCATION);
        if (installPath == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, Messages.errorRuntimeLocationMissing, new IOException(Messages.errorRuntimeLocationMissing));
        }

        if (product.getProductType() == ProductType.MASSIVE_TYPE) {
            Map<String, Object> installKernel = getInstallKernel(installPath.toFile());
            if (installKernel != null)
                return installWithKernel(installKernel, product, pa, settings, monitor2);
        }

        return installWithCommand(product, pa, settings, monitor2);
    }

    private IStatus installWithKernel(final Map<String, Object> installKernel, IProduct product, PasswordAuthentication pa, Map<String, Object> settings,
                                      IProgressMonitor monitor2) {
        String repoPropsURL = (String) settings.get(REPO_PROPS_LOCATION);
        installKernel.put("license.accept", Boolean.TRUE);
        if (repoPropsURL != null) {
            installKernel.put("repositories.properties", new File(repoPropsURL));
        }

        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        try {
            monitor.beginTask("", 100);

            SubProgressMonitor subMonitor1 = new SubProgressMonitor(monitor, 20);
            subMonitor1.beginTask(Messages.taskProductInfo, 100);
            subMonitor1.subTask(Messages.taskProductInfo);

            final List<String> featureList = new ArrayList<String>(2);
            featureList.add(product.getProvideFeature().get(0));

            // In action.install, the installer will try to resolve any
            // dependent features, so it might take some time and there
            // will be no progress. Use a thread for the action, and update
            // the progress monitor while the action is still going.
            final boolean[] done = new boolean[] { false };
            final IStatus[] status = new IStatus[] { Status.OK_STATUS };
            Thread thread = new Thread(Messages.taskProductInfo) {
                @Override
                public void run() {
                    try {
                        if (installKernel.put("action.install", featureList) == null) {
                            status[0] = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, (String) installKernel.get("action.error.message"), null);
                        }
                    } finally {
                        done[0] = true;
                    }
                }
            };

            thread.setDaemon(true);
            thread.start();

            int i = 0;
            while (!done[0] && !subMonitor1.isCanceled()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    //
                }

                if (i + 5 < 100) {
                    i += 5;
                    subMonitor1.subTask(Messages.taskProductInfo);
                    subMonitor1.worked(5);
                }
            }

            subMonitor1.done();
            if (status[0] != Status.OK_STATUS) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, (String) installKernel.get("action.error.message"), null);
            }

            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;

            final SubProgressMonitor subMonitor2 = new SubProgressMonitor(monitor, 80);
            final int[] currentTicks = new int[] { 0 };
            Integer monitorSize = (Integer) installKernel.get("progress.monitor.size");
            final int totalTicks = monitorSize.intValue() * 10;
            subMonitor2.beginTask(Messages.taskInstallFeature, totalTicks);

            installKernel.put("progress.monitor.message", new ArrayList<Object>() {
                private static final long serialVersionUID = 6897631470994089079L;

                @Override
                public boolean add(Object e) {
                    subMonitor2.subTask(e.toString());
                    if (currentTicks[0] + 10 <= totalTicks) {
                        subMonitor2.worked(10);
                        currentTicks[0] += 10;
                    }
                    return true;
                }
            });

            installKernel.put("progress.monitor.canceled", new ArrayList<Boolean>() {
                private static final long serialVersionUID = 1062495457758809852L;

                @Override
                public Boolean get(int location) {
                    return (subMonitor2.isCanceled()) ? Boolean.TRUE : Boolean.FALSE;
                }
            });

            Integer rc = (Integer) installKernel.get("action.result");
            subMonitor2.done();

            if (rc.intValue() != 0) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, (String) installKernel.get("action.error.message"), null);
            }
        } catch (Throwable t) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, t.getLocalizedMessage(), t);
        } finally {
            monitor.done();
        }

        return Status.OK_STATUS;
    }

    private IStatus installWithCommand(IProduct product, PasswordAuthentication pa, Map<String, Object> settings, IProgressMonitor monitor2) {
        IPath installPath = (IPath) settings.get(RUNTIME_LOCATION);
        long time = System.currentTimeMillis();
        int per = (int) (product.getSize() / 10240);
        int ticks = per * 11;
        IProgressMonitor monitor = monitor2;
        if (monitor == null)
            monitor = new NullProgressMonitor();

        if (monitor.isCanceled())
            return Status.CANCEL_STATUS;

        ProcessResult result;
        boolean toAddExtraProps = product.getProductType() == ProductType.MASSIVE_TYPE;

        try {
            monitor.beginTask(product.getName(), ticks);
            monitor.subTask(product.getName());

            String[] command = getCommand(product, pa, installPath, false);
            ProcessBuilder builder = createProcessBuilder(settings, toAddExtraProps, installPath.append("bin").toFile(), command);
            Process p = builder.start();
            monitor.worked(30);
            int waitTicks = (toAddExtraProps) ? (ticks - 60) / 2 : ticks - 30;
            result = ProcessHelper.waitForProcess(p, 1000, 900f, waitTicks, monitor);
            int exitValue = result.getExitValue();
            if (exitValue != 0) {
                if (!toAddExtraProps)
                    throw new IOException(NLS.bind(Messages.errorInstallProcessFailed, Integer.valueOf(exitValue) + ": " + result.getOutput()));

                // We failed to install with the feature name, so try with
                // the URL
                command = getCommand(product, null, installPath, true);
                builder = createProcessBuilder(settings, toAddExtraProps, installPath.append("bin").toFile(), command);
                p = builder.start();
                monitor.worked(30);
                result = ProcessHelper.waitForProcess(p, 1000, 900f, waitTicks, monitor);
                exitValue = result.getExitValue();
                if (exitValue != 0) {
                    throw new IOException(NLS.bind(Messages.errorInstallProcessFailed, Integer.valueOf(exitValue) + ": " + result.getOutput()));
                }
            }
        } catch (Exception e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        } finally {
            monitor.done();
            Trace.tracePerf("Feature install", time);
        }

        return Status.OK_STATUS;
    }

    private String[] getCommand(IProduct p, PasswordAuthentication pa, IPath installPath, boolean isUseLocation) {
        String batch = getBatchCommand(installPath);
        List<String> commandList = new ArrayList<String>();
        commandList.add(batch);
        commandList.add("install");
        commandList.add("--acceptLicense");
        if (batch.contains(FEATURE_MANAGER_CMD))
            commandList.add("--when-file-exists=replace");

        if (pa != null) {
            commandList.add("--user=" + pa.getUserName());
            commandList.add("--password=" + new String(pa.getPassword()));
        }

        if (isUseLocation || (p.getProductType() != ProductType.MASSIVE_TYPE))
            commandList.add(p.getSource().getLocation());
        else
            commandList.add(p.getProvideFeature().get(0));

        String[] command = new String[commandList.size()];
        return commandList.toArray(command);
    }

    protected ProcessBuilder createProcessBuilder(Map<String, Object> settings, boolean isAddExtraProps, File workDir, String... command) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(workDir);

        Map<String, String> env = builder.environment();
        IVMInstall vmInstall = (IVMInstall) settings.get(VM_INSTALL);
        if (vmInstall != null) {
            File javaHome = LaunchUtil.getJavaHome(vmInstall.getInstallLocation());
            env.put("JAVA_HOME", javaHome.getAbsolutePath());

            String jvmArgs = "";
            if (LaunchUtil.isIBMJRE(vmInstall)) {
                jvmArgs = LaunchUtil.VM_QUICKSTART;
            }

            // Pass in the Massive asset location properties file to runtime's
            // installer if it was set.
            if (isAddExtraProps) {
                String assetLoc = System.getProperty(ASSET_LOCATION_OVERRIDE_PROP_NAME);
                if (assetLoc != null) {
                    jvmArgs += " -D" + ASSET_LOCATION_OVERRIDE_PROP_NAME + "=" + assetLoc;
                }

                jvmArgs += " -Duser.agent=OLT/" + Activator.getBundleVersion();

                String repoPropsURL = (String) settings.get(REPO_PROPS_LOCATION);
                if (repoPropsURL != null)
                    jvmArgs += " -D" + REPOSITORIES_LOCATION_OVERRIDE_PROP_NAME + "=" + repoPropsURL;
            }

            if (jvmArgs.length() > 0) {
                env.put("JVM_ARGS", jvmArgs);
            }
        }

        env.put("EXIT_ALL", "1");
        builder.command(command);
        return builder;
    }

    private String getBatchCommand(IPath installPath) {
        String batch = Constants.INSTALL_UTILITY_CMD;
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            batch += ".bat";
        IPath batchPath = installPath.append("bin").append(batch);
        if (batchPath.toFile().exists())
            return batchPath.toOSString();

        batch = FEATURE_MANAGER_CMD;
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            batch += ".bat";

        return installPath.append("bin").append(batch).toOSString();
    }

    protected Map<String, Object> getInstallKernel(File wlpDir) {
        // Setup and new an instance of MapBasedInstallKernel
        Map<String, Object> mapBasedInstallKernel = null;
        final File installJarFile = new File(wlpDir, "bin/tools/ws-installUtility.jar");
        if (installJarFile.exists()) {
            try {
                mapBasedInstallKernel = AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, Object>>() {
                    @SuppressWarnings({ "unchecked" })
                    @Override
                    public Map<String, Object> run() throws Exception {
                        ClassLoader loader = new URLClassLoader(new URL[] { installJarFile.toURI().toURL() }, null);
                        Class<Map<String, Object>> clazz;
                        clazz = (Class<Map<String, Object>>) loader.loadClass("com.ibm.ws.install.MapBasedInstallKernel");
                        return clazz.newInstance();
                    }
                });
                if (mapBasedInstallKernel != null) {
                    mapBasedInstallKernel.put("runtime.install.dir", wlpDir);
                    Integer rc = (Integer) mapBasedInstallKernel.get("install.kernel.init.code");
                    if (rc.intValue() == 0) {
                        return mapBasedInstallKernel;
                    }

                    Trace.logError((String) mapBasedInstallKernel.get("install.kernel.init.error.message"), null);
                }
            } catch (Throwable t) {
                Trace.logError("Failed to create the install kernel", t);
            }
        }
        return null;
    }
}
