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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.util.SocketUtil;

/**
 * Allows a server type to specify its own server implementation
 */
public abstract class AbstractServerExtension {
    /**
     * Gets the service info attributes
     *
     * @return a map of the service info attributes for the server type
     */
    public String[] getServiceInfoKeys() {
        return new String[] {};
    }

    /**
     * Gives extension a chance to do any cleanup when the server is being deleted.
     */
    public void cleanup(IServer server) {
        ILaunch l = server.getLaunch();
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        if (l != null && lm != null) {
            lm.removeLaunch(l);
        }
    }

    /**
     * Get the server display name to be used in the UI
     *
     * @param server The server for which to get the name
     * @return
     */
    public String getServerDisplayName(IServer server) {
        return server.getName();
    }

    /**
     * Returns the partial/base URL for this server, including the given port number.
     *
     * @param server The server for which to get the base URL
     * @param port The port number from the server.xml or obtained
     *            through the JMX connection for remote
     * @return the partial URL for this server
     */
    public String getBaseURL(IServer server, int port) {
        String hostAddress = server.getHost();
        return getBaseURL(server, hostAddress, port);
    }

    protected String getBaseURL(IServer server, String host, int port) {
        StringBuilder sb = new StringBuilder("http://");

        // IPv6 addresses that use colons as separators need to be enclosed in
        // brackets when included in a URL
        if (host != null && host.contains(":"))
            sb.append("[" + host + "]");
        else
            sb.append(host);

        int port2 = ServerUtil.getMonitoredPort(server, port, "web");
        if (port2 != 80) {
            sb.append(":");
            sb.append(port2);
        }
        return sb.toString();
    }

    public boolean requiresRemoteStartSettings(IServer server) {
        return !SocketUtil.isLocalhost(server.getHost());
    }

    public String getConnectionPort(IServer server, String port) {
        return port;
    }

    public String getConnectionHost(IServer server, String host, String port) {
        return host;
    }

    public IStatus preModifyModules(IServer server, IModule[] add, IModule[] remove, IProgressMonitor monitor) {
        return Status.OK_STATUS;
    }

    /**
     * Determines if server is local setup
     *
     * @param server
     * @return true if local setup, false if not local setup, and null
     *         if the default isLocalSetup method should be called
     */
    public Boolean isLocalSetup(IServer server) {
        return null;
    }
}
