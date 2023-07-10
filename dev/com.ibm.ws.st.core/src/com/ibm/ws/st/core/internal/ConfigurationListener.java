/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.internal;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

public class ConfigurationListener implements NotificationListener, java.io.Serializable {

    private static final long serialVersionUID = 4728732986366124893L;

    public static final String OBJECT_NAME = "WebSphere:name=com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean";
    public static final String RUNTIME_UPDATE_NOTIFICATION_TYPE = "com.ibm.websphere.runtime.update.notification";

    // used to store notification data
    private ArrayList<Map<String, Object>> notificationsList = null;

    NotificationFilter filter = new NotificationFilter() {
        private static final long serialVersionUID = 8258472768098017825L;

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            // Filter on runtime update notifications with the "ConfigUpdatesDelivered" name.
            if (notification != null && notification.getType().equals("com.ibm.websphere.runtime.update.notification")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) notification.getUserData();
                return userData != null && "ConfigUpdatesDelivered".equals(userData.get("name"));
            }
            return false;
        }
    };

    /** {@inheritDoc} */
    @Override
    public void handleNotification(Notification notification, Object handback) {

        if (notificationsList == null)
            return;
        // The type will be "com.ibm.websphere.runtime.update.notification"
        // notification.getType()

        // The source is a object name of the MBean (ie: WebSphere:name=com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean)
        // notification.getSource()

        // UserData for "com.ibm.websphere.runtime.update.notification" type will always be a Map<String,Object>
        // Known keys are status, name and message
        // Known names are ApplicationsStopped, ApplicationsStarting and ConfigUpdatesDelivered
        // "status" is a Boolean, and will be true if passed, false if failed
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) notification.getUserData();
        if (userData != null) {
            notificationsList.add(userData);
        }
    }

    public void setNotificationList(ArrayList<Map<String, Object>> list) {
        notificationsList = list;
    }

    public ArrayList<Map<String, Object>> getNotificationList() {
        return notificationsList;
    }

    public NotificationFilter getFilter() {
        return filter;
    }

    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.IOException("Cannot be deserialized");
    }

}
