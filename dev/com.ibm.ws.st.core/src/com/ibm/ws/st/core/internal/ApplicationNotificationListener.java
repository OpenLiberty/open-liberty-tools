/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.util.Map;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;

import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

import com.ibm.ws.st.core.internal.WebSphereServerBehaviour.ApplicationStateTracker;
import com.ibm.ws.st.core.internal.config.ConfigurationFile.Application;

public class ApplicationNotificationListener implements NotificationListener {

    // ApplicationMBean states
    // (STOPPED, STARTING, STARTED, PARTIALY_STARTED, STOPPING, INSTALLED)
    public static final String STATE_STOPPED = "STOPPED";
    public static final String STATE_STARTING = "STARTING";
    public static final String STATE_STARTED = "STARTED";
    public static final String STATE_PARTIALY_STARTED = "PARTIALY_STARTED";
    public static final String STATE_STOPPING = "STOPPING";
    public static final String STATE_INSTALLED = "INSTALLED";

    // enhanced listener states
    public static final String APP_UPDATE = "application.update";

    // custom internal states
    public static final String STATE_UNKNOWN = "UNKNOWN";

    private final String appName;

    private final WebSphereServerBehaviour wsBehaviour;

    public ApplicationNotificationListener(String appName, WebSphereServerBehaviour wsBehaviour) {
        this.appName = appName;
        this.wsBehaviour = wsBehaviour;
    }

    /** {@inheritDoc} */
    @Override
    public void handleNotification(Notification notification, Object arg1) {
        if (notification instanceof AttributeChangeNotification) {
            AttributeChangeNotification acn = (AttributeChangeNotification) notification;
            handleState((String) acn.getOldValue(), (String) acn.getNewValue());
        }

        if (notification != null && notification.getType().startsWith("application.")) {
            //The type will be either "application.start", "application.stop" or "application.update"
            String type = notification.getType();

            //UserData for "application.*" type will always be a Map<String,Object>
            @SuppressWarnings("unchecked")
            Map<String, Object> userData = (Map<String, Object>) notification.getUserData();

            // "status" is a Boolean, and will be true if passed, false if failed
            Boolean status = (Boolean) userData.get("status");

            if (status.booleanValue() == true) {
                if (APP_UPDATE.equals(type)) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.SSM, "\tApplication <" + appName + ">: Update Successful");
                    wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.UPDATED);
                }
            } else {
                if (APP_UPDATE.equals(type)) {
                    if (Trace.ENABLED)
                        Trace.trace(Trace.SSM, "\tApplication <" + appName + ">: Update Failed");
                    wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.FAILED_UPDATE);
                }
            }
        }
    }

    public synchronized void handleState(String oldState, String newState) {

        if (Trace.ENABLED)
            Trace.trace(Trace.SSM, "\tApplication <" + appName + ">: " + oldState + " -> " + newState);

        // X -> STARTED
        if (STATE_STARTED.equals(newState)) {

            // The old state can only be UNKNOWN when it's just initialized, so we are not in a publish operation
            // and do not need to add applications state to the tracker
            if (!STATE_UNKNOWN.equals(oldState)) {
                // X -> STARTED
                wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.STARTED);
                wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.UPDATED);
            }

            setInternalExternalState(IServer.STATE_STARTED);
        }

        // X -> STOPPED
        if (STATE_STOPPED.equals(newState)) {
            // The old state can only be UNKNOWN when it's just initialized, so we are not in a publish operation
            // and do not need to add applications state to the tracker
            if (!STATE_UNKNOWN.equals(oldState)) {
                // X -> STOPPED
                wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.STOPPED);
            }

            IModule[] modules = wsBehaviour.getServer().getModules();
            for (IModule module : modules) {
                if (module.getName().equals(appName)) {
                    if (module.isExternal())
                        wsBehaviour.syncExternalModules();
                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STOPPED);
                    // We cannot break here because it is possible that internal and external module have the same name.
                    // If we have an external app running and we override it with an internal one, before the publishModules(),
                    // both apps are on the server.  We should set both.
                }
            }
        }

        // STARTED -> STOPPING
        if (STATE_STARTED.equals(oldState) && STATE_STOPPING.equals(newState)) {
            IModule[] modules = wsBehaviour.getServer().getModules();
            for (IModule module : modules) {
                if (module.getName().equals(appName)) {
                    if (module.isExternal())
                        wsBehaviour.syncExternalModules();
                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STOPPING);
                    // We cannot break here because it is possible that internal and external module have the same name.
                    // If we have an external app running and we override it with an internal one, before the publishModules(),
                    // both apps are on the server.  We should set both.
                }
            }
        }

        // STOPPED -> STARTING
        if (STATE_STOPPED.equals(oldState) && STATE_STARTING.equals(newState)) {
            IModule[] modules = wsBehaviour.getServer().getModules();
            for (IModule module : modules) {
                if (module.getName().equals(appName)) {
                    if (module.isExternal())
                        wsBehaviour.syncExternalModules();
                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, IServer.STATE_STARTING);
                    // We cannot break here because it is possible that internal and external module have the same name.
                    // If we have an external app running and we override it with an internal one, before the publishModules(),
                    // both apps are on the server.  We should set both.
                }
            }
        }

        // STARTING -> INSTALLED
        if (STATE_STARTING.equals(oldState) && STATE_INSTALLED.equals(newState)) {
            wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.FAILED_START);

            setInternalExternalState(IServer.STATE_UNKNOWN);
        }

        // STARTING -> STOPPED
        if (STATE_STARTING.equals(oldState) && STATE_STOPPED.equals(newState)) {
            // This state transition is an error in most situations, however when an app is published with autoStart set to false
            // then we expect the app to be in stopped state, so we shouldn't throw an error in that case
            boolean isAutoStart = true;
            try {
                for (Application app : wsBehaviour.getWebSphereServerInfo().getConfigRoot().getApplications()) {
                    if (app.getName().equals(appName) && app.getAutoStart() != null && app.getAutoStart().equalsIgnoreCase("false")) {
                        isAutoStart = false;
                        wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.STARTED);
                    }
                }
            } catch (Exception e) {
                Trace.trace(Trace.SSM, "Couldn't check the autoStart value for the app " + appName, e);
            }
            if (isAutoStart)
                wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.FAILED_START);

            setInternalExternalState(IServer.STATE_STOPPED);
        }

        // STARTING -> UNKNOWN
        // This state change happens on publishing a new app to the server.
        // If we can't get the app state after we have already registered the listener then something has gone wrong.
        if (STATE_STARTING.equals(oldState) && STATE_UNKNOWN.equals(newState)) {
            wsBehaviour.appStateTracker.addApplicationState(appName, ApplicationStateTracker.FAILED_START);

            setInternalExternalState(IServer.STATE_UNKNOWN);
        }
    }

    private void setInternalExternalState(int state) {
        IModule[] modules = wsBehaviour.getServer().getModules();
        boolean externalModuleAdded = true;
        for (IModule module : modules) {
            if (module.getName().equals(appName)) {
                wsBehaviour.setModuleStateImpl(new IModule[] { module }, state);
                externalModuleAdded = false;
                break;
            }
        }
        if (externalModuleAdded) {
            wsBehaviour.syncExternalModules();
            modules = wsBehaviour.getServer().getModules();
            for (IModule module : modules) {
                if (module.getName().equals(appName)) {
                    wsBehaviour.setModuleStateImpl(new IModule[] { module }, state);
                    break;
                }
            }
        }
    }

    public String getAppName() {
        return appName;
    }

    public WebSphereServerBehaviour getServerDelegate() {
        return wsBehaviour;
    }
}
