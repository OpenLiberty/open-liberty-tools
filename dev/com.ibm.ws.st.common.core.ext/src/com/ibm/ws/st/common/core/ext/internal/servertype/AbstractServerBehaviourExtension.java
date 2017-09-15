/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.common.core.ext.internal.servertype;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

/**
 * Allows a server type to specify its own server behaviour
 */
public abstract class AbstractServerBehaviourExtension {
    /**
     * Provide the stop implementation for the server
     *
     * @param behaviour the server behaviour delegate
     * @param force force to stop the server
     */
    public abstract void stop(ServerBehaviourDelegate behaviour, boolean force, IProgressMonitor monitor);

    /**
     * Determines if the server is able to stop or not
     *
     * @param behaviour the server behaviour delegate
     * @return a status object with code IStatus.OK if the server can be stopped, otherwise a status object indicating why it can't
     */
    public abstract IStatus canStop(ServerBehaviourDelegate behaviour);

    /**
     * Determines if the server can restart or not
     *
     * @param behaviour The server behaviour delegate
     * @return A status object with code IStatus.OK if the server can be restarted, otherwise a status object indicating why it can't
     */
    public IStatus canRestart(ServerBehaviourDelegate behaviour) {
        return Status.OK_STATUS;
    }

    /**
     * Returns whether the server can be cleaned when started
     *
     * @return Returns true if the server can be cleaned, false otherwise.
     */
    public boolean canCleanOnStart() {
        return true;
    }

    /**
     * Returns whether the server is configured for loose configuration support
     *
     * @param behaviour
     * @return Boolean true if the server supports it; Boolean false if the server does not support it;
     *         null for default behavior (e.g. for no server extensions defined).
     */
    public Boolean isLooseConfigEnabled(ServerBehaviourDelegate behaviour) {
        return null;
    }

    /**
     * Returns the apps path mapped to the runtime (container) path at /opt/ibm/wlp/usr/servers/<servername>/apps
     *
     * @param behaviour
     * @return
     */
    public IPath getAppsOverride(ServerBehaviourDelegate behaviour) {
        return null;
    }

    /**
     * Returns the mapped path for the given path (such as the Docker container path for the local path)
     *
     * @param path
     * @param behaviour
     * @return
     */
    public IPath getMappedPath(IPath path, ServerBehaviourDelegate behaviour) {
        return null;
    }

    /**
     * Perform additional changes when the loose config setting is switched. This happens during
     * the doSave method when the server editor is saved.
     *
     *
     * @param behaviour
     * @param isLooseConfig
     * @return
     */
    public IStatus preSaveLooseConfigChange(ServerBehaviourDelegate behaviour, boolean isLooseConfig) {
        return Status.OK_STATUS;
    }

    /**
     * Ask extensions if they want to update files to the remote server during the handling of the loose config
     * change and prior to starting the app. Extensions will return true to update the files, false, if no update is necessary.
     */
    public boolean doUpdateRemoteAppFiles(ServerBehaviourDelegate behaviour, boolean isLooseConfig) {
        return false;
    }

    /**
     * Allow extensions to remove loose config files from the remote server, during a loose config change, and prior to publishing
     *
     * @param behaviour
     * @param path
     * @param isLooseConfig
     * @param monitor
     */
    public void removeRemoteAppFiles(ServerBehaviourDelegate behaviour, IPath path, boolean isLooseConfig, IProgressMonitor monitor) {
        return;
    }

    public boolean isLocalUserDir(ServerBehaviourDelegate behaviour) {
        return false;
    }

    /**
     * Determine if the publisher should be run given a server type.
     * The server's behaviour is passed in for getting additional information
     * about the server (when server type alone is not enough to determine if
     * the publisher is needed or not)
     *
     * @param behaviour
     * @param publishUnit
     * @param publisher
     * @return true if the publisher should be run and false if not
     */
    public boolean shouldRunPublisherForServerType(ServerBehaviourDelegate behaviour, Object publishUnit, Object publisher) {
        return true;
    }

    /**
     * Performs any operations that are required for the publish to succeed.
     *
     * @param behaviour
     * @return
     */
    public IStatus prePublishModules(ServerBehaviourDelegate behaviour, IProgressMonitor monitor) {
        return Status.OK_STATUS;
    }

    /**
     * Performs operations on behaviour initialization
     *
     * @param behaviour
     * @param monitor
     */
    public void initialize(ServerBehaviourDelegate behaviour, IProgressMonitor monitor) {
        // Do nothing
    }
}
