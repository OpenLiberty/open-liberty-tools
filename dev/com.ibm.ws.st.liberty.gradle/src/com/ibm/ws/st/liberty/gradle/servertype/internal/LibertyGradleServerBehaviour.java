/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.st.liberty.gradle.servertype.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;
import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.servertype.internal.AbstractLibertyBuildPluginServerBehaviour;
import com.ibm.ws.st.liberty.gradle.internal.LibertyGradle;

/**
 * Liberty Build Plugin Server Behaviour Implementation
 */
@SuppressWarnings("restriction")
public class LibertyGradleServerBehaviour extends AbstractLibertyBuildPluginServerBehaviour {

    /**
     * Performs any operations that are required for the publish to succeed.
     *
     * @param behaviour
     * @return
     */
    @Override
    public IStatus prePublishModules(ServerBehaviourDelegate behaviour, IProgressMonitor monitor) {
        if (behaviour != null) {
            if (behaviour instanceof WebSphereServerBehaviour) {
                WebSphereServerBehaviour wsBehaviour = (WebSphereServerBehaviour) behaviour;
                WebSphereServer wsServer = wsBehaviour.getWebSphereServer();
                String type = wsServer.getServerType();
                if (type != null && type.equals(Constants.SERVER_TYPE_LIBERTY_GRADLE)) {
                    wsServer.ensureLocalConnectorAndAppMBeanConfig(monitor);
                }
            }
        }
        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyGradle.getInstance();
    }

}