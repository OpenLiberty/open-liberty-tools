/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.ui.internal.actions;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.TaskModel;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereRuntime;
import com.ibm.ws.st.core.internal.repository.IRuntimeInfo;
import com.ibm.ws.st.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.download.AbstractDownloadComposite;
import com.ibm.ws.st.ui.internal.download.DownloadHelper;
import com.ibm.ws.st.ui.internal.download.DownloadUI;

public class AddOnRuntimeAction extends SelectionProviderAction {
    private final Shell shell;
    private IRuntime runtime;
    private IRuntimeInfo core;

    protected AddOnRuntimeAction(Shell shell, ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.actionInstallAddOn);
        this.shell = shell;
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() != 1) {
            setEnabled(false);
            return;
        }

        runtime = null;
        core = null;
        Iterator<?> iterator = sel.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof IRuntime) {
                runtime = (IRuntime) obj;
                IRuntimeType runtimeType = runtime.getRuntimeType();
                IPath path = runtime.getLocation();
                if (path == null || runtimeType == null || !runtimeType.getId().startsWith(Constants.RUNTIME_ID_PREFIX)) {
                    setEnabled(false);
                    return;
                }

                // add-on is not supported until 8.5.5
                core = DownloadHelper.getRuntimeCore(runtime);
                if (core.getVersion() == null || core.getVersion().startsWith("8.5.0")) {
                    setEnabled(false);
                    return;
                }

                // Installing additional content is not supported if the install utilities are not there
                WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
                if (wsRuntime != null && !wsRuntime.supportsInstallingAdditionalContent()) {
                    setEnabled(false);
                    return;
                }
            } else {
                setEnabled(false);
                return;
            }
        }
        setEnabled(true);
    }

    @Override
    public void run() {
        if (runtime == null || core == null) {
            return;
        }

        IPath path = runtime.getLocation();
        if (path == null) {
            return;
        }

        WebSphereRuntime wsRuntime = (WebSphereRuntime) runtime.loadAdapter(WebSphereRuntime.class, null);
        if (wsRuntime == null) {
            return;
        }

        HashMap<String, Object> map = new HashMap<String, Object>();
        map = new HashMap<String, Object>();
        map.put(AbstractDownloadComposite.FOLDER, runtime.getLocation().toOSString());
        map.put(AbstractDownloadComposite.RUNTIME_TYPE_ID, runtime.getRuntimeType().getId());
        map.put(AbstractDownloadComposite.RUNTIME_CORE, core);

        TaskModel taskModel = new TaskModel();
        taskModel.putObject(TaskModel.TASK_RUNTIME, runtime);
        taskModel.putObject(AbstractDownloadComposite.ADDON_MAP, map);

        DownloadUI.launchAddonsDialog(shell, taskModel);

    }
}
