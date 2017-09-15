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

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.ibm.ws.st.common.core.internal.RemoteServerInfo;

public class ServerEditorRemoteStartupComposite extends AbstractRemoteServerStartupComposite {

    ServerEditorSection section;

    public ServerEditorRemoteStartupComposite(Composite parent, IServerWorkingCopy serverWc, ServerEditorSection section, RemoteServerInfo info, boolean isCloudServer) {
        this(parent, serverWc, section, info, isCloudServer, false);
    }

    public ServerEditorRemoteStartupComposite(Composite parent, IServerWorkingCopy serverWc, ServerEditorSection section, RemoteServerInfo info, boolean isCloudServer,
                                              boolean isDockerServer) {
        super(parent, serverWc, info, isCloudServer, isDockerServer);
        addCommandListener();
        this.section = section;
    }

    @Override
    protected void showValidationError(int key) {
        // if remote server start is not enabled don't set any error messages
        if (!enabledErrorCheck) {
            return;
        }
        // We do the validation for the specific key.
        // Note: validate(key) will only perform validation for the provided key but will always return
        // an error message if any of the previous validation keys had errors. It is not meant to return
        // a message for only the given key.
        String msg = validate(key);
        if (msg != null) {
            isPageComplete = false; // we must set the flag before the setMessage because the setMessage will check isPageComplete.
            section.setErrorMessage(msg);
        } else {
            isPageComplete = true;
            section.setErrorMessage(null);
        }
    }

    @Override
    public void initializeForCloudServerInstance() {
        super.initializeForCloudServerInstance();
        section.setErrorMessage(null);
    }

    @Override
    public void reinitializeNonCloudDefaultValues() {
        super.reinitializeNonCloudDefaultValues();
        showValidationError(VALIDATION_INDEX_PATHS);
        showValidationError(VALIDATION_INDEX_LOG_ON_METHOD);
        //clear error messages on first showing page
        section.setErrorMessage(null);
    }

    private void addCommandListener() {
        super.addCommandListener(new AbstractRemoteServerStartupComposite.CommandListener() {
            @Override
            public void handleCommand(IUndoableOperation op) {
                section.execute(op);
            }
        });
    }

}