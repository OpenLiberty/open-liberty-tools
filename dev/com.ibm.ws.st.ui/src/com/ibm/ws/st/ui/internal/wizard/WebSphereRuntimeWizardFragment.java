/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.wizard;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.core.util.SocketUtil;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.FileUtil;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.repository.AbstractInstaller;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.ui.internal.Activator;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.Trace;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite;
import com.ibm.ws.st.ui.internal.download.AbstractWizardFragment;
import com.ibm.ws.st.ui.internal.download.AddonUtil;
import com.ibm.ws.st.ui.internal.download.AddonsWizardFragment;
import com.ibm.ws.st.ui.internal.download.DownloadHelper;
import com.ibm.ws.st.ui.internal.download.DownloaderComposite;
import com.ibm.ws.st.ui.internal.download.DownloaderWizardFragment;
import com.ibm.ws.st.ui.internal.download.IRuntimeHandler;
import com.ibm.ws.st.ui.internal.download.LicenseWizardFragment;
import com.ibm.ws.st.ui.internal.download.LocalProduct;
import com.ibm.ws.st.ui.internal.download.SiteHelper;
import com.ibm.ws.st.ui.internal.download.ValidationResult;
import com.ibm.ws.st.ui.internal.plugin.ServerUtil;

public class WebSphereRuntimeWizardFragment extends WizardFragment {
    protected WebSphereRuntimeComposite comp;
    protected RuntimeHandler runtimeHandler = new RuntimeHandler();
    protected int severity = IMessageProvider.NONE;
    private List<WizardFragment> cachedList;
    private Map<String, Object> map;
    private boolean isFinished = false;

    public WebSphereRuntimeWizardFragment() {
        // do nothing
    }

    @Override
    public boolean hasComposite() {
        return true;
    }

    @Override
    public boolean isComplete() {
        IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
        if (runtime == null)
            return false;

        return (comp != null && !comp.isDisposed() && severity != IMessageProvider.ERROR);
    }

    @Override
    public Composite createComposite(Composite parent, final IWizardHandle wizard) {
        WebSphereRuntimeComposite.Mode mode;
        IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);

        isFinished = false;
        if (map != null)
            map.clear();

        if (runtime.getOriginal() == null) {
            String source = (String) getTaskModel().getObject(AbstractWizardFragment.ARCHIVE_SOURCE);
            if (source != null) {
                mode = WebSphereRuntimeComposite.Mode.ARCHIVE;
                downloadRequested(true);
            } else {
                if (runtime.getLocation() != null)
                    mode = WebSphereRuntimeComposite.Mode.EXISTING_FOLDER;
                else
                    mode = WebSphereRuntimeComposite.Mode.NEW_FOLDER;
            }
        } else {
            mode = WebSphereRuntimeComposite.Mode.EDIT;
        }

        comp = new WebSphereRuntimeComposite(parent, new WebSphereRuntimeComposite.IMessageHandler() {
            @Override
            public void setMessage(String message, int severity) {
                WebSphereRuntimeWizardFragment.this.severity = severity;
                wizard.setMessage(message, severity);
                wizard.update();
            }
        }, mode, new WebSphereRuntimeComposite.IDownloadRequestHandler() {
            @Override
            public void downloadRequested(boolean b) {
                WebSphereRuntimeWizardFragment.this.downloadRequested(b);
            }
        });

        wizard.setTitle(Messages.wizRuntimeTitle);
        wizard.setDescription(Messages.wizRuntimeDescription);
        wizard.setImageDescriptor(Activator.getImageDescriptor(Activator.IMG_WIZ_RUNTIME));
        return comp;
    }

    @Override
    public void enter() {
        if (comp != null) {
            IRuntimeWorkingCopy rt = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
            runtimeHandler.setRuntime(rt);
            comp.setRuntimeHandler(runtimeHandler);
        }
    }

    @Override
    public void exit() {
        IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);

        // Set the id to match the name and enable constant id.  Do this only on initial
        // creation of the runtime (since this wizard is used to edit the runtime too).
        if (runtime.getOriginal() == null) {
            WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
            wsRuntime.setConstantId(runtime.getName());

            // Put runtime information into the map if downloading a new runtime
            if (getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP) != null) {
                map.put(AbstractDownloadComposite.RUNTIME_TYPE_ID, runtime.getRuntimeType().getId());
            }
        }

        // set a preference so we can default to the same location next time
        if (runtime.validate(null).getSeverity() != IStatus.ERROR) {
            IRuntimeType type = runtime.getRuntimeType();
            String canonicalPath;
            try {
                canonicalPath = runtime.getLocation().toFile().getCanonicalPath();
            } catch (IOException e) {
                canonicalPath = runtime.getLocation().toString();
            }
            Activator.addToPreferenceList(type.getId() + ".folder", canonicalPath);
        }
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        if (comp == null || comp.isDisposed())
            return;

        IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
        if (runtime.getOriginal() != null)
            return;

        IServerWorkingCopy server = (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
        try {
            int serverCreateTicks = (server == null) ? 0 : 20;
            int metaDataTicks = (map == null || map.isEmpty()) ? 100 - serverCreateTicks : 20;

            monitor.beginTask(Messages.jobInstallingRuntime, 100);

            // We have already performed the install, so nothing to do
            if (isFinished)
                return;

            Map<IProduct, IStatus> result = new LinkedHashMap<IProduct, IStatus>();

            if (map != null && !map.isEmpty())
                result = downlandAndInstall(runtime.getLocation(), new SubProgressMonitor(monitor, 80 - serverCreateTicks));

            if (result.containsValue(Status.OK_STATUS)) {
                WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                createMetaData(wsRuntime, new SubProgressMonitor(monitor, metaDataTicks));
                isFinished = true;
            }

            // If we are creating a runtime as part of creating a server, delay
            // showing the success dialog
            if (getTaskModel().getObject(TaskModel.TASK_SERVER) != null) {
                getTaskModel().putObject(AbstractDownloadComposite.INSTALL_RESULT, new ArrayList<IStatus>(result.values()));
                getTaskModel().putObject(AbstractDownloadComposite.SELECTED_DOWNLOADERS, new ArrayList<IProduct>(result.keySet()));
            } else {
                AddonUtil.showResult(null, result);
            }
        } finally {
            if (server == null)
                monitor.done();
        }

        //resetSites();
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        // We never created the runtime, so there is nothing to cancel
        if (comp == null || comp.isDisposed())
            return;

        if (isFinished) {
            cleanUp();
        }
    }

    private void cleanUp() {
        IRuntimeWorkingCopy runtime = (IRuntimeWorkingCopy) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);

        wsRuntime.deleteProject(null);
        wsRuntime.removeMetadata(null, true, false);

        IPath folder = runtime.getLocation();
        if (map != null && !map.isEmpty()) {
            try {
                FileUtil.deleteDirectory(folder.toOSString(), true);
            } catch (IOException e) {
                Trace.trace(Trace.WARNING, "Failed to clean up install directory", e);
            }
        }

        isFinished = false;
    }

    private Map<IProduct, IStatus> downlandAndInstall(IPath folder, IProgressMonitor monitor) throws CoreException {
        Map<IProduct, IStatus> result = new LinkedHashMap<IProduct, IStatus>();
        try {
            @SuppressWarnings("unchecked")
            final List<IProduct> selectedList = (List<IProduct>) map.get(AbstractDownloadComposite.SELECTED_DOWNLOADERS);

            @SuppressWarnings("unchecked")
            List<PasswordAuthentication> authList = (List<PasswordAuthentication>) map.get(AbstractDownloadComposite.PRODUCT_AUTHENTICATION);
            IProduct coreManager = (IProduct) map.get(AbstractDownloadComposite.SELECTED_CORE_MANAGER);

            // coreManager cannot be null, since we are downloading
            // and installing a runtime
            if (coreManager instanceof LocalProduct) {
                String archivePath = coreManager.getSource().getLocation();
                Activator.addToPreferenceList(DownloaderComposite.PREF_ARCHIVES, archivePath);
            }
            checkInstallPath(folder);

            HashMap<String, Object> settings = new HashMap<String, Object>();
            settings.put(AbstractInstaller.RUNTIME_LOCATION, folder);
            settings.put(AbstractInstaller.VM_INSTALL, JavaRuntime.getDefaultVMInstall());
            if (SiteHelper.isProxyNeeded()) {
                String propsURL = SiteHelper.getRepoPropertiesURL();
                if (propsURL != null) {
                    settings.put(AbstractInstaller.REPO_PROPS_LOCATION, propsURL);
                }
            }
            result = DownloadHelper.install(selectedList, authList, settings, monitor);
            IStatus firstStatus = null;

            // get the status for the runtime installation. clean up if either operation was cancelled or we failed to install the runtime
            for (Map.Entry<IProduct, IStatus> entry : result.entrySet()) {
                firstStatus = entry.getValue();
                break;
            }
            if (firstStatus != null && (firstStatus == Status.CANCEL_STATUS || firstStatus != Status.OK_STATUS)) {
                throw new CoreException(firstStatus);
            }
            return result;
        } catch (final CoreException ce) {
            try {
                FileUtil.deleteDirectory(folder.toOSString(), true);
            } catch (IOException e) {
                Trace.trace(Trace.WARNING, "Failed to clean up install directory.The install operation either failed or it was cancelled", e);
            }
            AddonUtil.showResult(null, result);
            throw ce;
        }
    }

    protected void checkInstallPath(IPath installPath) throws CoreException {
        // check if install path is writable before attempting download
        if (!installPath.toFile().exists()) {
            if (!FileUtil.makeDir(installPath)) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorInstallingRuntimeEnvironment, installPath.toOSString()), null));
            }
        }

        File tempFile = installPath.append("writeTest.bat").toFile();
        // Don't need to check if the file exists already since we require the install
        // directory to be empty. The install directory is validated in the install wizard.
        try {
            tempFile.createNewFile(); // throws an IOException if it cannot write the file
            tempFile.delete();
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, NLS.bind(Messages.errorInstallingRuntimeEnvironment, installPath.toOSString() + "\n\n")
                                                                                      + e.getLocalizedMessage(), e));
        }
    }

    @Override
    protected void createChildFragments(List<WizardFragment> list) {
        if (cachedList == null) {
            cachedList = new ArrayList<WizardFragment>();
            cachedList.add(new DownloaderWizardFragment());
            if (SiteHelper.downloadAndInstallSupported()) {
                cachedList.add(new AddonsWizardFragment());
                cachedList.add(new LicenseWizardFragment());
            }
        }
        list.addAll(cachedList);
    }

    protected void downloadRequested(boolean b) {
        if (b) {
            if (map == null) {
                map = new HashMap<String, Object>();
            }
            getTaskModel().putObject(AbstractDownloadComposite.ADDON_MAP, map);
            getTaskModel().putObject(AbstractDownloadComposite.RUNTIME_HANDLER, runtimeHandler);
        } else {
            if (map != null) {
                map.clear();
            }
            getTaskModel().putObject(AbstractDownloadComposite.ADDON_MAP, null);
            getTaskModel().putObject(AbstractDownloadComposite.RUNTIME_HANDLER, null);
        }
    }

    protected void createMetaData(final WebSphereRuntime wsRuntime, IProgressMonitor monitor) {
        final boolean[] done = new boolean[] { false };

        try {
            final String message = NLS.bind(Messages.taskRefreshingMetadata, wsRuntime.getRuntime().getName());
            monitor.beginTask(message, 300);
            monitor.subTask(message);

            Job runtimeAddedJob = new Job(message) {
                @Override
                protected IStatus run(IProgressMonitor monitor2) {
                    wsRuntime.createMetadata(new JobChangeAdapter() {
                        @Override
                        public void done(IJobChangeEvent event) {
                            done[0] = true;
                            if (!event.getResult().isOK()) {
                                if (Trace.ENABLED) {
                                    Trace.trace(Trace.WARNING, "Metadata create did not finish successfully");
                                }
                            }
                            event.getJob().removeJobChangeListener(this);
                        }
                    });
                    wsRuntime.createProject(null);
                    return Status.OK_STATUS;
                }
            };
            runtimeAddedJob.setPriority(Job.SHORT);
            runtimeAddedJob.schedule();

            for (int i = 0; i < 100 && !done[0]; ++i) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignore
                }
                monitor.worked(1);
            }

            if (!done[0]) {
                if (Trace.ENABLED)
                    Trace.trace(Trace.WARNING, "The create metadata job did not finish in time");
            }
        } finally {
            monitor.done();
        }
    }

    private static class RuntimeHandler implements IRuntimeHandler {

        private IRuntimeWorkingCopy runtimeWC;
        private WebSphereRuntime runtime;
        private IPath oldLocation;

        protected RuntimeHandler() {
            // empty constructor
        }

        protected void setRuntime(IRuntimeWorkingCopy newRuntime) {
            if (newRuntime == null) {
                runtimeWC = null;
                runtime = null;
                oldLocation = null;
            } else {
                runtimeWC = newRuntime;
                runtime = (WebSphereRuntime) newRuntime.loadAdapter(WebSphereRuntime.class, null);
                if (runtimeWC.getLocation() != null && !runtimeWC.getLocation().isEmpty())
                    oldLocation = runtimeWC.getLocation();
                else
                    oldLocation = null;
            }
        }

        @Override
        public void setLocation(IPath location) {
            if (runtimeWC != null)
                runtimeWC.setLocation(location);
        }

        @Override
        public void setRuntimeName(String name) {
            if (runtimeWC != null)
                runtimeWC.setName(name);
        }

        @Override
        public String getRuntimeName() {
            return (runtimeWC == null) ? null : runtimeWC.getName();
        }

        @Override
        public String getRuntimeTypeId() {
            if (runtimeWC == null)
                return null;

            IRuntimeType type = runtimeWC.getRuntimeType();
            return type.getId();
        }

        @Override
        public IPath getLocation() {
            return (runtimeWC == null) ? null : runtimeWC.getLocation();
        }

        @Override
        public WebSphereRuntime getRuntime() {
            return runtime;
        }

        /** {@inheritDoc} */
        @Override
        public ValidationResult validateRuntime(boolean isDownloadOrInstall) {
            if (runtime == null || runtimeWC == null) {
                return new ValidationResult(null, IMessageProvider.ERROR);
            }

            // check to see if the runtime has running servers, and the location
            // changed
            final IPath runtimeLocation = runtimeWC.getLocation();
            if (oldLocation != null && !oldLocation.equals(runtimeLocation) && isLocalServersRunning(runtimeWC)) {
                return new ValidationResult(Messages.errorRuntimeServersRunning, IMessageProvider.ERROR);
            }

            // check to see if another runtime was created at the
            // same location, and if it's the case flag it as an
            // error
            if (runtimeLocation != null) {
                final WebSphereRuntime[] wrts = WebSphereUtil.getWebSphereRuntimes();
                if (wrts.length > 0) {
                    final String runtimeId = runtimeWC.getId();
                    for (WebSphereRuntime wrt : wrts) {
                        final IRuntime rt = wrt.getRuntime();
                        if (rt != null && runtimeLocation.equals(rt.getLocation()) && !runtimeId.equals(rt.getId())) {
                            return new ValidationResult(NLS.bind(Messages.errorRuntimeLocationMapped,
                                                                 new String[] { runtimeLocation.toPortableString(), rt.getId() }), IMessageProvider.ERROR);
                        }
                    }
                }
            }

            IRuntime origRT = runtimeWC.getOriginal();
            if (origRT == null || !runtimeWC.getName().equals(origRT.getName())) {
                if (ResourcesPlugin.getWorkspace().getRoot().exists(new Path(runtimeWC.getName()))) {
                    return new ValidationResult((Messages.errorRTNameSameExistingPrjName), IMessageProvider.ERROR);
                }
            }

            if (isDownloadOrInstall) {
                return new ValidationResult(null, IMessageProvider.NONE);
            }

            IStatus status = runtimeWC.validate(null);
            if (status == null || status.isOK())
                return new ValidationResult(null, IMessageProvider.NONE);
            if (status.getSeverity() == IStatus.WARNING)
                return new ValidationResult(status.getMessage(), IMessageProvider.WARNING);

            return new ValidationResult(status.getMessage(), IMessageProvider.ERROR);
        }

        private static boolean isLocalServersRunning(IRuntimeWorkingCopy runtimeWC) {
            IRuntime originalRT = runtimeWC.getOriginal();
            // creating a new runtime
            if (originalRT == null) {
                return false;
            }

            IServer[] servers = ServerUtil.getServers(originalRT);
            if (servers != null) {
                for (IServer server : servers) {
                    if ((server.getServerState() == IServer.STATE_STARTING || server.getServerState() == IServer.STATE_STARTED) && SocketUtil.isLocalhost(server.getHost())) {
                        return true;
                    }
                }

            }
            return false;
        }
    }
}
