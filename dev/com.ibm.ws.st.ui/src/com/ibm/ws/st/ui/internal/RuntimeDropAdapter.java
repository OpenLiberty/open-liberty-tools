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
package com.ibm.ws.st.ui.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereUtil;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.core.internal.repository.License;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite;
import com.ibm.ws.st.ui.internal.download.AbstractWizardFragment;
import com.ibm.ws.st.ui.internal.download.AddonUtil;
import com.ibm.ws.st.ui.internal.download.DownloadHelper;
import com.ibm.ws.st.ui.internal.download.DownloadUI;
import com.ibm.ws.st.ui.internal.download.LocalProduct;
import com.ibm.ws.st.ui.internal.download.ProductHelper;
import com.ibm.ws.st.ui.internal.download.SiteHelper;
import com.ibm.ws.st.ui.internal.wizard.WebSphereRuntimeWizardFragment;

/**
 * Drop adapter that handles runtime artifacts being dropped on the Runtime Explorer. The following is a list of
 * potential drop sources and what the corresponding behaviour is:
 *
 * 1. Runtime archive (e.g. "wlp-developers-8.5.5.jar")
 * - Open existing download/install wizard, tweaked to prompt for runtime name, license, and install location. Creates runtime.
 *
 * 2. Runtime folder (e.g. "c:\myLiberty\wlp")
 * - Prompt to confirm runtime name (defaulted from directory name) and create runtime.
 *
 * 3. Packaged server "all" (e.g. "myRuntime.zip")
 * - Open existing download/install wizard, tweaked to prompt for runtime name, license (optional, likely skipped), and install location. Creates runtime.
 *
 * ---------- Future Possibilities ----------
 *
 * 4. Packaged server "usr" (e.g. "myServer.zip")
 * - Should only be able to drop onto an existing runtime. Prompt to confirm, then unzip into runtime and refresh view to show new servers.
 *
 * 5. ifix (e.g. "8.5.0.0-WS-WASProd_WLPArchive-TFPM69489.jar")
 * - Should only be able to drop onto existing runtime. Prompt to confirm, then apply ifix to runtime and confirm result.
 *
 * 6. Extension offering
 * - Should be able to install separately (for future association with a runtime) or drop onto an existing runtime.
 * - Prompt to confirm, then apply ifix to runtime and confirm result.
 */
@SuppressWarnings("restriction")
public class RuntimeDropAdapter extends CommonDropAdapterAssistant {
    private static final String RUNTIME_MARKER = "wlp/lib/versions/WebSphereApplicationServer.properties";

    private String filename;
    private IRuntime runtime;

    @Override
    public boolean isSupportedType(TransferData transferData) {
        return FileTransfer.getInstance().isSupportedType(transferData);
    }

    /**
     * Filter to only support jars, zips, and folders.
     */
    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferData) {
        runtime = null;
        if (target instanceof IRuntime) {
            runtime = (IRuntime) target;
            IRuntimeType runtimeType = runtime.getRuntimeType();
            if (runtime.getLocation() == null || runtimeType == null || !runtimeType.getId().startsWith(Constants.RUNTIME_ID_PREFIX)) {
                return Status.CANCEL_STATUS;
            }
        }

        if (isSupportedType(transferData)) {
            Object obj = FileTransfer.getInstance().nativeToJava(transferData);
            if (obj != null && obj instanceof String[]) {
                String[] files = (String[]) obj;
                if (files.length != 1)
                    return Status.CANCEL_STATUS;
                filename = files[0];
                if (filename == null)
                    return Status.CANCEL_STATUS;

                File f = new File(filename);
                if (f.isDirectory()) {
                    if (runtime == null && WebSphereRuntime.isValidLocation(new Path(filename)))
                        return Status.OK_STATUS;
                } else if ((filename.toLowerCase().endsWith(".jar") || filename.toLowerCase().endsWith(".zip") || filename.toLowerCase().endsWith(".esa")))
                    return Status.OK_STATUS;

                return Status.CANCEL_STATUS;
            }
        }

        return Status.CANCEL_STATUS;
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent, Object target) {
        final Shell shell = getShell();
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    handleDropImpl();
                } catch (Exception e) {
                    MessageDialog.openError(shell, Messages.title, e.getLocalizedMessage());
                }
            }
        });
        return Status.OK_STATUS;
    }

    public void handleDropImpl() {
        try {
            final File f = new File(filename);
            if (f.isDirectory()) {
                handleRuntimeFolder();
                return;
            }

            // jar or zip
            IStatus status = handleFile();
            if (status != Status.OK_STATUS)
                MessageDialog.openError(getShell(), Messages.title, status.getMessage());
        } catch (Exception e) {
            Trace.logError("Could not handle runtime/add-on drop", e);
        }
    }

    protected void openNewRuntimeWizard() throws CoreException {
        IRuntimeType rt = ServerCore.findRuntimeType(Constants.DEFAULT_RUNTIME_TYPE_ID);
        IRuntimeWorkingCopy wc = rt.createRuntime(null, null);
        TaskModel taskModel = new TaskModel();
        File f = new File(filename);

        if (f.isDirectory())
            wc.setLocation(new Path(filename));
        else
            taskModel.putObject(AbstractWizardFragment.ARCHIVE_SOURCE, filename);

        taskModel.putObject(TaskModel.TASK_RUNTIME, wc);
        WizardFragment fragment = new WizardFragment() {
            @Override
            protected void createChildFragments(List<WizardFragment> list) {
                list.add(new WebSphereRuntimeWizardFragment());
                list.add(WizardTaskUtil.SaveRuntimeFragment);
            }
        };
        TaskWizard wizard = new TaskWizard(Messages.title, fragment, taskModel);
        WizardDialog dialog = new WizardDialog(getShell(), wizard);
        dialog.open();
    }

    protected void handleRuntimeFolder() throws CoreException {
        // check for existing runtimes with the same folder
        IPath path = new Path(filename);
        WebSphereRuntime[] wrts = WebSphereUtil.getWebSphereRuntimes();
        if (wrts.length > 0) {
            for (WebSphereRuntime wrt : wrts) {
                final IRuntime rt = wrt.getRuntime();
                if (rt != null && path.toPortableString().equalsIgnoreCase(rt.getLocation().toPortableString())) {
                    MessageDialog.openError(getShell(), Messages.title, NLS.bind(Messages.errorRuntimeLocationMapped, new String[] { path.toOSString(), rt.getName() }));
                    return;
                }
            }
        }

        openNewRuntimeWizard();
    }

    protected IStatus handleFile() throws CoreException {
        if (runtime == null) {
            if (isLibertyArchive(new File(filename))) {
                openNewRuntimeWizard();
                return Status.OK_STATUS;
            } else if (SiteHelper.downloadAndInstallSupported() && isAddonArchive(filename)) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorRuntimeRequired, null);
            }
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorUnknownDrop, null);
        }

        // dealing with an add-on
        if (SiteHelper.downloadAndInstallSupported() && isAddonArchive(filename)) {
            return handleAddOn();
        }

        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.errorUnknownDrop, null);
    }

    private IStatus handleAddOn() {
        IRuntimeInfo core = DownloadHelper.getRuntimeCore(runtime);
        LocalProduct product = LocalProduct.create(filename);

        IStatus status = ProductHelper.isApplicableTo(product, core);
        if (status != Status.OK_STATUS) {
            return status;
        }

        HashMap<String, Object> map = new HashMap<String, Object>();
        ArrayList<String> archiveList = new ArrayList<String>();
        List<IProduct> selectedList = new ArrayList<IProduct>(4);

        selectedList.add(product);
        archiveList.add(filename);
        map.put(AbstractDownloadComposite.FOLDER, runtime.getLocation().toOSString());
        map.put(AbstractDownloadComposite.RUNTIME_TYPE_ID, runtime.getRuntimeType().getId());
        map.put(AbstractDownloadComposite.ARCHIVES, archiveList);
        map.put(AbstractDownloadComposite.SELECTED_DOWNLOADERS, selectedList);

        try {
            License license = product.getLicense(null);
            if (license != null) {
                Map<IProduct, License> licenseMap = AddonUtil.createLicenseMap();
                map.put(AbstractDownloadComposite.LICENSE, licenseMap);
                licenseMap.put(product, license);
            }
        } catch (IOException e) {
            if (Trace.ENABLED) {
                Trace.trace(Trace.WARNING, "Could not get license", e);
            }
        }

        TaskModel taskModel = new TaskModel();
        taskModel.putObject(TaskModel.TASK_RUNTIME, runtime);
        taskModel.putObject(AbstractDownloadComposite.ADDON_MAP, map);

        DownloadUI.launchAddonsDialog(getShell(), taskModel);

        return Status.OK_STATUS;
    }

    /**
     * Returns true if the given file is a Liberty archive file.
     *
     * @param archiveFile
     */
    protected static boolean isLibertyArchive(File archiveFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(archiveFile);
            return zipFile.getEntry(RUNTIME_MARKER) != null;
        } catch (IOException e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Problem reading archive: " + archiveFile, e);
            return false;
        } finally {
            try {
                if (zipFile != null)
                    zipFile.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected static boolean isAddonArchive(String filePath) {
        LocalProduct product = LocalProduct.create(filePath);
        if (product == null)
            return false;

        IProduct.Type type = product.getType();
        return type != IProduct.Type.UNKNOWN && type != IProduct.Type.INSTALL
               && type != IProduct.Type.CONFIG_SNIPPET && type != IProduct.Type.IFIX;
    }
}
