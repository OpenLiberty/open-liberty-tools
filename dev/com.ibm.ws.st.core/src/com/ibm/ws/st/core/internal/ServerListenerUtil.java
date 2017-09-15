/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.server.core.IRuntime;

public class ServerListenerUtil {
    // server change listeners
    private transient final List<IWebSphereServerListener> listeners = new ArrayList<IWebSphereServerListener>(2);
    private transient final List<IWebSphereMetadataListener> mlisteners = new ArrayList<IWebSphereMetadataListener>(1);
    private static ServerListenerUtil instance;

    private ServerListenerUtil() {
        // use getInstance() instead
    }

    public static synchronized ServerListenerUtil getInstance() {
        if (instance == null)
            instance = new ServerListenerUtil();
        return instance;
    }

    /**
     * Add a listener to this server.
     * 
     * @param listener
     */
    public void addServerListener(IWebSphereServerListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Remove a listener from this server.
     * 
     * @param listener
     */
    public void removeServerListener(IWebSphereServerListener listener) {
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }

    /**
     * Add a metadata listener to this server.
     * 
     * @param listener
     */
    public void addMetadataListener(IWebSphereMetadataListener mlistener) {
        synchronized (mlisteners) {
            if (!mlisteners.contains(mlistener))
                mlisteners.add(mlistener);
        }
    }

    /**
     * Remove a metadata listener from this server.
     * 
     * @param listener
     */
    public void removeMetadataListener(IWebSphereMetadataListener mlistener) {
        if (mlisteners != null) {
            synchronized (mlisteners) {
                mlisteners.remove(mlistener);
            }
        }
    }
    

    /**
     * Fire an event.
     */
    protected void fireServerChangedEvent(WebSphereServerInfo server) {
        if (listeners == null || listeners.isEmpty())
            return;

        IWebSphereServerListener[] list;
        synchronized (listeners) {
            list = listeners.toArray(new IWebSphereServerListener[listeners.size()]);
        }

        for (IWebSphereServerListener listener : list) {
            try {
                listener.serverChanged(server);
            } catch (Exception e) {
                Trace.logError("Error firing server changed event for server: " + server.getServerName(), e);
            }
        }
    }

    /**
     * Fire an event.
     */
    protected void fireRuntimeChangedEvent(IRuntime runtime) {
        if (listeners == null || listeners.isEmpty())
            return;

        IWebSphereServerListener[] list;
        synchronized (listeners) {
            list = listeners.toArray(new IWebSphereServerListener[listeners.size()]);
        }

        for (IWebSphereServerListener listener : list) {
            try {
                listener.runtimeChanged(runtime);
            } catch (Exception e) {
                Trace.logError("Error firing runtime changed event for runtime: " + runtime.getId(), e);
            }
        }
    }

    /**
     * Fire a metadata changed event
     */

    protected void fireMetadataChangedEvent(IRuntime runtime) {
        if (mlisteners == null || mlisteners.isEmpty())
            return;

        IWebSphereMetadataListener[] list;
        synchronized (mlisteners) {
            list = mlisteners.toArray(new IWebSphereMetadataListener[mlisteners.size()]);
        }

        for (IWebSphereMetadataListener mlistener : list) {
            try {
                mlistener.runtimeMetadataChanged(runtime);
            } catch (Exception e) {
                Trace.logError("Error firing metadata changed event for runtime: " + runtime.getId(), e);
            }
        }

    }
}