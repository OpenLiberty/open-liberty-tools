/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2016, 2017 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.etools.maven.liberty.integration.servertype.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.ws.st.liberty.buildplugin.integration.internal.ILibertyBuildPluginImpl;
import com.ibm.ws.st.liberty.buildplugin.integration.servertype.internal.AbstractLibertyBuildPluginServerBehaviour;
import com.ibm.etools.maven.liberty.integration.internal.LibertyMaven;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.WebSphereServer;
import com.ibm.ws.st.core.internal.WebSphereServerBehaviour;

/**
 * Liberty Build Plugin Server Behaviour Implementation
 */
@SuppressWarnings("restriction")
public class LibertyMavenServerBehaviour extends AbstractLibertyBuildPluginServerBehaviour {

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
                if (type != null && type.equals(Constants.SERVER_TYPE_LIBERTY_MAVEN)) {
                    wsServer.ensureLocalConnectorAndAppMBeanConfig(monitor);
                }
            }
        }
        return Status.OK_STATUS;
    }

    /** {@inheritDoc} */
    @Override
    public ILibertyBuildPluginImpl getBuildPluginImpl() {
        return LibertyMaven.getInstance();
    }

}