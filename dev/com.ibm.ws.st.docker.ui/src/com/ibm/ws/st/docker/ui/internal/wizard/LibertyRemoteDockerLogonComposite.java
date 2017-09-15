/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.ui.internal.wizard;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.common.ui.internal.composite.ServerCreationWizardRemoteStartupComposite;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.remote.RemoteUtils;
import com.ibm.ws.st.docker.ui.internal.Messages;
import com.ibm.ws.st.ui.internal.wizard.IServerWizardComposite;
import com.ibm.ws.st.ui.internal.wizard.WebSphereServerWizardCommonFragment;

public class LibertyRemoteDockerLogonComposite extends ServerCreationWizardRemoteStartupComposite implements IServerWizardComposite {

    TaskModel taskModel = null;
    IServerWorkingCopy serverWc = null;

    public LibertyRemoteDockerLogonComposite(Composite parent, IServerWorkingCopy serverWc, IWizardHandle wizard, RemoteServerInfo remoteInfo) {
        super(parent, serverWc, wizard, remoteInfo, false, true);
        this.serverWc = serverWc;
        wizard.setDescription(Messages.dockerRemoteLogonWizardDescription);
    }

    @Override
    public void setup(TaskModel taskModel) {
        this.taskModel = taskModel;
        initializeValues();
    }

    @Override
    public void validate() {
        wizard.setMessage(null, IMessageProvider.NONE);
        wizard.update();
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        WebSphereServer wsServer = (WebSphereServer) serverWc.loadAdapter(WebSphereServer.class, null);
        if (wsServer != null) {
            wsServer.setRemoteServerProperties(getRemoteServerInfo());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isComplete() {
        if (super.isPageComplete()) {
            Map<String, String> serviceInfo = (Map<String, String>) taskModel.getObject(WebSphereServerWizardCommonFragment.SERVICE_INFO);
            if (serviceInfo == null) {
                serviceInfo = new HashMap<String, String>();
            }
            RemoteServerInfo remoteInfo = getRemoteServerInfo();
            if (remoteInfo != null) {
                RemoteUtils.copyRemoteInfoToServiceInfo(remoteInfo, serviceInfo);
            }
            taskModel.putObject(WebSphereServerWizardCommonFragment.SERVICE_INFO, serviceInfo);
            return true;
        }
        return false;
    }

    @Override
    public Composite getComposite() {
        return this;
    }

    // Not needed for this class.
    @Override
    public void reInitialize() {
        return;
    }

}