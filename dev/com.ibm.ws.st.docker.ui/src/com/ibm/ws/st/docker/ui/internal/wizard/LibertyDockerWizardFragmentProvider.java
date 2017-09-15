/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.docker.ui.internal.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.ibm.ws.st.common.core.ext.internal.Activator;
import com.ibm.ws.st.common.core.ext.internal.UnsupportedServiceException;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory;
import com.ibm.ws.st.common.core.ext.internal.setuphandlers.PlatformHandlerFactory.PlatformType;
import com.ibm.ws.st.common.core.ext.internal.util.AbstractDockerMachine;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.common.ui.ext.internal.servertype.WizardFragmentProvider;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.ui.internal.wizard.WebSphereServerWizardCommonFragment;

/**
 * Wizard fragment provider for liberty running on docker.
 */
public class LibertyDockerWizardFragmentProvider extends WizardFragmentProvider {

    /** {@inheritDoc} */
    @Override
    public boolean isActive(TaskModel taskModel) {
        if (WebSphereServerWizardCommonFragment.isLocalhost(taskModel)) {
            try {
                return AbstractDockerMachine.isDockerInstalled(PlatformHandlerFactory.getPlatformHandler(null, PlatformType.COMMAND));
            } catch (UnsupportedServiceException e) {
                Trace.logError("Failed to retrieve the platform handler.", e);
            }
            return false;
        }

        // For the remote case return true by default if the platform handler is available
        // since no way to tell if Docker is installed remotely until the user has filled
        // in the remote log-on information.
        return Activator.getPlatformProvider(PlatformType.SSH_KEYLESS.name()) != null;
    }

    /** {@inheritDoc} */
    @Override
    public Composite getInitialComposite(Composite parent, IWizardHandle handle, TaskModel taskModel) {
        Composite comp = null;
        if (WebSphereServerWizardCommonFragment.isLocalhost(taskModel)) {
            comp = new LibertyDockerComposite(parent, handle, taskModel);
        } else {
            IServerWorkingCopy serverWC = (IServerWorkingCopy) taskModel.getObject(TaskModel.TASK_SERVER);
            WebSphereServer wsServer = (WebSphereServer) serverWC.getAdapter(WebSphereServer.class);
            RemoteServerInfo remoteInfo = getInitializedRemoteServerInfo(wsServer);
            comp = new LibertyRemoteDockerLogonComposite(parent, serverWC, handle, remoteInfo);
        }
        return comp;
    }

    /** {@inheritDoc} */
    @Override
    public List<WizardFragment> getFollowingFragments() {
        List<WizardFragment> fragments = new ArrayList<WizardFragment>();
        fragments.add(new LibertyRemoteDockerWizardFragment());
        return fragments;
    }

    private RemoteServerInfo getInitializedRemoteServerInfo(WebSphereServer wsServer) {
        RemoteServerInfo info = null;
        if (wsServer != null) {
            info = wsServer.getRemoteServerInfo();
        } else {
            info = new RemoteServerInfo(RemoteServerInfo.RemoteServerType.Liberty);
        }
        info.putBooleanValue(RemoteServerInfo.PROPERTY_REMOTE_START_ENABLED, true);
        info.putIntegerValue(RemoteServerInfo.PROPERTY_REMOTE_START_PLATFORM, RemoteServerInfo.REMOTE_SERVER_STARTUP_LINUX);
        return info;
    }
}
