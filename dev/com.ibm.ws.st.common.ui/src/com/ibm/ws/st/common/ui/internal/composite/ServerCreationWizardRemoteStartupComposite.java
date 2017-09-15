/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.ui.internal.composite;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;
import com.ibm.ws.st.common.core.internal.RemoteServerInfo.RemoteServerType;
import com.ibm.ws.st.common.core.internal.Trace;
import com.ibm.ws.st.common.ui.internal.Messages;

public class ServerCreationWizardRemoteStartupComposite extends AbstractRemoteServerStartupComposite {

    protected IWizardHandle wizard;

    public ServerCreationWizardRemoteStartupComposite(Composite parent, IServerWorkingCopy serverWc, IWizardHandle wizard, RemoteServerInfo remoteInfo, boolean isCloudServer) {
        this(parent, serverWc, wizard, remoteInfo, isCloudServer, false);
    }

    public ServerCreationWizardRemoteStartupComposite(Composite parent, IServerWorkingCopy serverWc, IWizardHandle wizard, RemoteServerInfo remoteInfo, boolean isCloudServer,
                                                      boolean isDockerServer) {
        super(parent, serverWc, remoteInfo, isCloudServer, isDockerServer);
        this.wizard = wizard;

        wizard.setTitle(Messages.L_InputRemoteWASServerInfoWizardTitle);
        String description = remoteInfo != null && remoteInfo.getMode().equals(RemoteServerType.Liberty) ? Messages.L_InputRemoteWASServerInfoWizardDescriptionLiberty : Messages.L_InputRemoteWASServerInfoWizardDescriptionTWAS;
        wizard.setDescription(description);
        wizard.setMessage(Messages.L_InputRemoteWASServerInfoWizardDescriptionTWAS, IMessageProvider.NONE);
    }

    @Override
    protected void showValidationError(int key) {
        if (!enabledErrorCheck) {
            return;
        }
        String msg = validate(key);
        if (msg != null) {
            isPageComplete = false; // we must set the flag before the setMessage because the setMessage will check isPageComplete.
            wizard.setMessage(msg, IMessageProvider.ERROR);
        } else {
            isPageComplete = true;
            wizard.setMessage(null, IMessageProvider.NONE);
        }
    }

    @Override
    public void initializeForCloudServerInstance() {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Initializing cloud values");
        super.initializeForCloudServerInstance();
        wizard.setMessage(null, IMessageProvider.NONE);
    }

    @Override
    public void reinitializeNonCloudDefaultValues() {
        if (Trace.ENABLED)
            Trace.trace(Trace.INFO, "Reinitializing default non-cloud values");
        super.reinitializeNonCloudDefaultValues();
        showValidationError(VALIDATION_INDEX_PATHS);
        showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
        //clear error messages on first showing page
        wizard.setMessage(null, IMessageProvider.NONE);
    }

}