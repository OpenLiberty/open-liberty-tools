/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal.launch;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.Activator;
import com.ibm.ws.st.core.internal.Constants;
import com.ibm.ws.st.core.internal.LaunchUtil;
import com.ibm.ws.st.core.internal.WebSphereServer;

public class ExternalProcess extends PlatformObject implements IProcess {
    private final Map<String, String> attributes = new HashMap<String, String>(3);
    private final ConsoleStreamsProxy streamsProxy;
    private final ILaunch launch;
    private final String name;
    private boolean terminated;
    private final IServer server;

    public ExternalProcess(ILaunch launch, IServer server, ConsoleStreamsProxy streamsProxy) {
        this.launch = launch;
        this.server = server;
        this.streamsProxy = streamsProxy;

        setAttribute(IProcess.ATTR_PROCESS_TYPE, "java");

        IPath runtimeLocation = server.getRuntime().getLocation();
        String batch = Constants.BATCH_SCRIPT;
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
            batch += ".bat";
        batch = runtimeLocation.append("bin").append(batch).toOSString();
        name = LaunchUtil.getProcessLabel(batch);

        fireCreationEvent();
    }

    /**
     * Fire a creation event.
     */
    private void fireCreationEvent() {
        fireEvent(new DebugEvent(this, DebugEvent.CREATE));
    }

    /**
     * Fire a terminate event.
     */
    private void fireTerminateEvent() {
        fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
    }

    /**
     * Fire a change event.
     */
    private void fireChangeEvent() {
        fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
    }

    /**
     * Fires the given debug event.
     *
     * @param event debug event to fire
     */
    private void fireEvent(DebugEvent event) {
        DebugPlugin manager = DebugPlugin.getDefault();
        if (manager != null)
            manager.fireDebugEventSet(new DebugEvent[] { event });
    }

    @Override
    public void terminate() throws DebugException {
        if (terminated)
            return;

        int state = server.getServerState();
        WebSphereServer wsServer = (WebSphereServer) server.loadAdapter(WebSphereServer.class, null);
        if (state != IServer.STATE_STOPPED && state != IServer.STATE_STOPPING && wsServer != null && wsServer.isStopOnShutdown()) {
            server.stop(false);
            return;
        }

        terminated = true;
        closeStreams();
        fireTerminateEvent();
    }

    public void closeStreams() {
        streamsProxy.update();
        streamsProxy.close();
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public boolean canTerminate() {
        return !terminated;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter.equals(IProcess.class))
            return this;

        if (adapter.equals(IDebugTarget.class)) {
            ILaunch launch = getLaunch();
            IDebugTarget[] targets = launch.getDebugTargets();
            for (int i = 0; i < targets.length; i++) {
                if (this.equals(targets[i].getProcess())) {
                    return targets[i];
                }
            }
            return null;
        }

        if (adapter.equals(ILaunch.class))
            return getLaunch();

        if (adapter.equals(ILaunchConfiguration.class))
            return getLaunch().getLaunchConfiguration();

        return super.getAdapter(adapter);
    }

    @Override
    public void setAttribute(String key, String value) {
        Object origVal = attributes.get(key);
        if (origVal != null && origVal.equals(value))
            return; // nothing changed

        attributes.put(key, value);
        fireChangeEvent();
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        return streamsProxy;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public int getExitValue() throws DebugException {
        if (!terminated)
            throw new DebugException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Process not terminated"));
        return 0;
    }

    @Override
    public String getAttribute(String key) {
        return attributes.get(key);
    }
}
