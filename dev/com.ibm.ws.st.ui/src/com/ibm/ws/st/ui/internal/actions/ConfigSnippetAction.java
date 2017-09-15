/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.ActionConstants;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite;
import com.ibm.ws.st.ui.internal.download.ConfigSnippetWizardContainer;
import com.ibm.ws.st.ui.internal.download.DownloadHelper;

/**
 * Config snippet add-on action
 */
@SuppressWarnings("restriction")
public class ConfigSnippetAction extends WebSphereUtilityAction {

    private IRuntime runtime;
    private IRuntimeInfo core;

    public ConfigSnippetAction(Shell shell, ISelectionProvider selProvider) {
        super(Messages.actionAddConfigSnippets, shell, selProvider);
    }

    @Override
    public boolean selectionChanged(Iterator<?> iterator) {
        runtime = null;
        core = null;

        if (server == null || server.getWebSphereRuntime() == null)
            return false;

        runtime = server.getWebSphereRuntime().getRuntime();
        if (runtime == null || runtime.getLocation() == null)
            return false;

        if (isDisableUtilityPrompted())
            return false;

        // add-on is not supported until 8.5.5
        core = DownloadHelper.getRuntimeCore(runtime);
        if (core.getProductVersion() == null || core.getProductVersion().startsWith("8.5.0"))
            return false;

        // Installing additional content is not supported if the install utilities are not there
        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (wsRuntime != null && !wsRuntime.supportsInstallingAdditionalContent()) {
            setEnabled(false);
            return false;
        }

        return true;
    }

    @Override
    public boolean isUtilityRemoteSupported() {
        return false;
    }

    @Override
    public void run() {
        if (server == null || core == null) {
            return;
        }

        if (notifyUtilityDisabled(wsServer.getServerType(), ActionConstants.CONFIG_SNIPPET_ACTION_ID))
            return;

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(AbstractDownloadComposite.FOLDER, server.getConfigRoot().getPath().removeLastSegments(1).toOSString());
        map.put(AbstractDownloadComposite.RUNTIME_TYPE_ID, runtime.getRuntimeType().getId());
        map.put(AbstractDownloadComposite.RUNTIME_EXTEND, "true");
        map.put(AbstractDownloadComposite.RUNTIME_CORE, core);

        TaskModel taskModel = new TaskModel();
        taskModel.putObject(TaskModel.TASK_RUNTIME, runtime);
        taskModel.putObject(TaskModel.TASK_SERVER, server);
        taskModel.putObject(AbstractDownloadComposite.ADDON_MAP, map);

        WizardFragment fragment = new WizardFragment() {
            @Override
            protected void createChildFragments(List<WizardFragment> list) {
                list.add(new ConfigSnippetWizardContainer());
            }
        };
        TaskWizard wizard = new TaskWizard(Messages.wizInstallAddonTitle, fragment, taskModel);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setPageSize(450, 600);
        dialog.open();
    }

    @Override
    public String getId() {
        return ActionConstants.CONFIG_SNIPPET_ACTION_ID;
    }
}
