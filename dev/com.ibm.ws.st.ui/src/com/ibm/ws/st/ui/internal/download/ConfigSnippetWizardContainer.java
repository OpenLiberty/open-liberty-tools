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
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.WebSphereServerInfo;
import com.ibm.ws.st.core.internal.config.ConfigurationFile;
import com.ibm.ws.st.core.internal.repository.AbstractInstaller;
import com.ibm.ws.st.core.internal.repository.IProduct;
import com.ibm.ws.st.ui.internal.Messages;

public class ConfigSnippetWizardContainer extends WizardFragment {
    private List<WizardFragment> cachedList;

    @Override
    protected void createChildFragments(List<WizardFragment> list) {
        if (cachedList == null) {
            cachedList = new ArrayList<WizardFragment>();
            cachedList.add(new ConfigSnippetWizardFragment());
            cachedList.add(new LicenseWizardFragment());
        }
        list.addAll(cachedList);
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) getTaskModel().getObject(AbstractDownloadComposite.ADDON_MAP);
        if (map == null)
            return;

        @SuppressWarnings("unchecked")
        final List<IProduct> selectedList = (List<IProduct>) map.get(AbstractDownloadComposite.SELECTED_DOWNLOADERS);
        if (selectedList.isEmpty())
            return;

        WebSphereServerInfo server = (WebSphereServerInfo) getTaskModel().getObject(TaskModel.TASK_SERVER);
        if (!preFinish(selectedList, server))
            throw new CoreException(Status.CANCEL_STATUS);

        IRuntime runtime = (IRuntime) getTaskModel().getObject(TaskModel.TASK_RUNTIME);
        final WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        int totalTicks = 100;
        int metadataTicks = (wsRuntime != null && AddonUtil.isRefreshMetadataRequired(selectedList)) ? 20 : 0;
        monitor.beginTask(Messages.jobInstallingRuntime, totalTicks);
        @SuppressWarnings("unchecked")
        List<PasswordAuthentication> authList = (List<PasswordAuthentication>) map.get(AbstractDownloadComposite.PRODUCT_AUTHENTICATION);
        final HashMap<String, Object> settings = new HashMap<String, Object>();
        settings.put(AbstractInstaller.RUNTIME_LOCATION, new Path((String) map.get(AbstractDownloadComposite.FOLDER)));
        settings.put(AbstractInstaller.VM_INSTALL, (wsRuntime == null) ? JavaRuntime.getDefaultVMInstall() : wsRuntime.getVMInstall());
        settings.put(AbstractInstaller.WEBSPHERE_SERVER, server);
        if (SiteHelper.isProxyNeeded()) {
            String propsURL = SiteHelper.getRepoPropertiesURL();
            if (propsURL != null) {
                settings.put(AbstractInstaller.REPO_PROPS_LOCATION, propsURL);
            }
        }
        Map<IProduct, IStatus> result = DownloadHelper.install(selectedList, authList, settings, new SubProgressMonitor(monitor, totalTicks - metadataTicks));

        if (wsRuntime != null && result.containsValue(Status.OK_STATUS)) {
            if (metadataTicks > 0)
                AddonUtil.refreshMetadata(wsRuntime, new SubProgressMonitor(monitor, metadataTicks));
            wsRuntime.refresh();
        }
        AddonUtil.refreshServerFolder(server);
        AddonUtil.showResult(null, result);
        monitor.done();
    }

    private boolean preFinish(List<IProduct> selection, WebSphereServerInfo server) {
        ConfigurationFile serverConfig = server.getConfigRoot();
        IPath configDir = serverConfig.getPath().removeLastSegments(1);

        final StringBuilder sb = new StringBuilder();
        for (IProduct p : selection) {
            if (p.getType() != IProduct.Type.CONFIG_SNIPPET)
                continue;

            String location = p.getSource().getLocation();
            if (location == null)
                continue;

            location = location.replace('\\', '/');
            int index = location.lastIndexOf('/');
            String fileName = (index == -1) ? location.trim() : location.substring(index + 1, location.length()).trim();
            File configSnippetFile = configDir.append(fileName).toFile();
            if (configSnippetFile.exists())
                sb.append(configSnippetFile.getAbsolutePath() + "\n");
        }

        if (sb.length() == 0)
            return true;

        // If one or more of the above files exists prompt the user
        final boolean result[] = new boolean[] { false };
        final boolean done[] = new boolean[] { false };
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                result[0] = MessageDialog.openQuestion(null, Messages.wizInstallAddonTitle, NLS.bind(Messages.overwriteExistingFiles, sb.toString()));
                done[0] = true;
            }
        });

        while (!done[0]) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return result[0];
    } // end preFinish
}
