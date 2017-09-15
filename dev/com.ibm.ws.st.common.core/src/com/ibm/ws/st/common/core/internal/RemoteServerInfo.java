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
package com.ibm.ws.st.common.core.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * Contains the data model for remote connection settings.
 */
public class RemoteServerInfo extends HashMap<String, Object> {

    private static final long serialVersionUID = 2110714887126584511L;

    //remote server start up platform - non negative int
    public static final int REMOTE_SERVER_STARTUP_WINDOWS = 0;
    public static final int REMOTE_SERVER_STARTUP_LINUX = 1;
    public static final int REMOTE_SERVER_STARTUP_OTHER = 2;
    public static final int REMOTE_SERVER_STARTUP_MAC = 3;

    //remote server start up logon method - non negative int
    public static final int REMOTE_SERVER_STARTUP_LOGON_OS = 0;
    public static final int REMOTE_SERVER_STARTUP_LOGON_SSH = 1;

    //Remote server start up
    public static final String PROPERTY_REMOTE_START_ENABLED = "remoteStart_Enabled";
    public static final String PROPERTY_REMOTE_START_TWAS_PROFILE_PATH = "remoteStart_ProfilePath";
    public static final String PROPERTY_REMOTE_START_LIBERTY_RUNTIME_PATH = "remoteStart_LibertyRuntimePath";
    public static final String PROPERTY_REMOTE_START_LIBERTY_CONFIG_PATH = "remoteStart_LibertyConfigPath";
    public static final String PROPERTY_REMOTE_START_PLATFORM = "remoteStart_Platform";
    public static final String PROPERTY_REMOTE_START_LOGONMETHOD = "remoteStart_LogonMethod";
    public static final String PROPERTY_REMOTE_START_OS_ID = "remoteStart_OSId";
    public static final String PROPERTY_REMOTE_START_OS_PWD = "remoteStart_OSPassword";
    public static final String PROPERTY_REMOTE_START_SSH_ID = "remoteStart_SSHId";
    public static final String PROPERTY_REMOTE_START_SSH_PASSPHRASE = "remoteStart_SSHPassphrase";
    public static final String PROPERTY_REMOTE_START_SSH_KEY_FILE = "remoteStart_SSHKeyFile";
    public static final String SECURE_REMOTE_OS_PASSWORD_KEY = ".remoteOSPassword";
    private transient String tempRemoteOSPassword = "";
    public static final String PROPERTY_REMOTE_START_DEBUG_PORT = "remoteStart_debugPort";

    // Cloud
    public static final String PROPERTY_IS_CLOUD_ENABLED = "cloudIsEnabled";

    protected transient Vector<PropertyChangeListener> propertyListeners;

    public String getTempRemoteOSPassword() {
        return tempRemoteOSPassword;
    }

    public void setTempRemoteOSPassword(String tempRemoteOSPassword) {
        String oldPass = getTempRemoteOSPassword();
        if (oldPass.equals(tempRemoteOSPassword))
            return;
        this.tempRemoteOSPassword = tempRemoteOSPassword;
        firePropertyChangeEvent(PROPERTY_REMOTE_START_OS_PWD, oldPass, tempRemoteOSPassword);
    }

    /**
     * Add a property change listener
     * 
     * @param listener
     *            java.beans.PropertyChangeListener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyListeners == null)
            propertyListeners = new Vector<PropertyChangeListener>();
        propertyListeners.add(listener);
    }

    public enum CloudType {
        CLOUD, NON_CLOUD, UNKNOWN
    }

    public enum RemoteServerType {
        TWAS, Liberty
    }

    private final RemoteServerType type;

    public RemoteServerInfo(RemoteServerType type) {
        this.type = type;
    }

    public RemoteServerType getMode() {
        return type;
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        try {
            Boolean val = (Boolean) get(key);
            if (val != null)
                return val.booleanValue();
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not get value for key: " + key, e);
        }
        return defaultValue;
    }

    public String getStringValue(String key) {
        try {
            String val = (String) get(key);
            if (val != null)
                return val;
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not get value for key: " + key, e);
        }
        return "";
    }

    public int getIntegerValue(String key, int defaultValue) {
        try {
            Integer val = (Integer) get(key);
            if (val != null)
                return val.intValue();
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.trace(Trace.WARNING, "Could not get value for key: " + key, e);
        }
        return defaultValue;
    }

    public void putBooleanValue(String key, boolean value) {
        try {
            Boolean val = Boolean.valueOf(value);
            put(key, val);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Error setting value for key value pair (" + key + ", " + value + ")", e);
        }
    }

    public void putIntegerValue(String key, int value) {
        try {
            Integer val = Integer.valueOf(value);
            put(key, val);
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Error setting value for key value pair (" + key + ", " + value + ")", e);
        }
    }

    public void setRemoteServerOSPwd(String newPassword, String nodeName) {
        String oldValue = getRemoteServerOSPwd(nodeName);
        if (oldValue.equals(newPassword)) {
            return;
        }
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences node = preferences.node(nodeName);
        try {
            node.put(SECURE_REMOTE_OS_PASSWORD_KEY, newPassword, true);
            preferences.flush();
        } catch (Exception e) {
            if (Trace.ENABLED)
                Trace.logError("Failed to set remote OS password", e);
        }
    }

    public String getRemoteServerOSPwd(String nodeName) {
        ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
        ISecurePreferences node = preferences.node(nodeName);
        String osPassword = "";
        try {
            osPassword = node.get(SECURE_REMOTE_OS_PASSWORD_KEY, "");
        } catch (StorageException e) {
            if (Trace.ENABLED)
                Trace.logError("Failed to get remote OS password", e);
        }
        return osPassword;
    }

    public void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue) {
        if (propertyListeners == null)
            return;

        PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        try {
            Vector clone = (Vector) propertyListeners.clone();
            int size = clone.size();
            for (int i = 0; i < size; i++)
                try {
                    PropertyChangeListener listener = (PropertyChangeListener) clone.elementAt(i);
                    listener.propertyChange(event);
                } catch (Exception e) {
                    // Do nothing
                }
        } catch (Exception e) {
            // Do nothing
        }
    }
}
