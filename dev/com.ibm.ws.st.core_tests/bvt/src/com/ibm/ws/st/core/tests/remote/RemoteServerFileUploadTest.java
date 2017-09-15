/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.remote;

import java.util.ArrayList;
import java.util.Map;

import javax.management.openmbean.CompositeData;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ibm.ws.st.core.internal.ConfigurationListener;
import com.ibm.ws.st.core.tests.ToolsTestBase;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

@TestCaseDescriptor(description = "Test remote server file upload", isStable = true)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteServerFileUploadTest extends ToolsTestBase {

    // Do not change the runtime name as the application projects are targeted to this runtime
    protected static final String RUNTIME_NAME = "remoteServerTestRuntime";

    @Test
    public void test01_doSetup() throws Exception {
        doRemoteSetup(RUNTIME_NAME);
    }

    @Test
    public void test02_createServer() throws Exception {
        createRemoteServer(runtime);
    }

    @Test
    public void test03_serverAddFeature() throws Exception {
        modifyFeatures("bells-1.0", true);
    }

    @Test
    public void test04_serverRemoveFeature() throws Exception {
        modifyFeatures("bells-1.0", false);
    }

    @Test
    public void test99_doTearDown() {
        cleanUp();
    }

    private void modifyFeatures(String featureName, boolean add) throws Exception {
        jmxConnection.connect();
        ConfigurationListener configListener = new ConfigurationListener();
        ArrayList<Map<String, Object>> notificationsList = new ArrayList<Map<String, Object>>(3);
        configListener.setNotificationList(notificationsList);
        jmxConnection.addConfigListener(configListener);
        try {
            ArrayList<String> changed = new ArrayList<String>(5);
            CompositeData metadata = (CompositeData) jmxConnection.getMetadata("${server.config.dir}", "a");
            print("Remote server's config file location: " + (String) metadata.get("fileName") + "/server.xml");
            changed.add((String) metadata.get("fileName") + "/server.xml");
            if (add) {
                wsServer.getConfiguration().addFeature(featureName);
            } else {
                wsServer.getConfiguration().removeFeature(featureName);
            }
            wsServer.getConfiguration().save(null);
            jmxConnection.uploadFile(wsServer.getConfigurationRoot().toFile(), (String) metadata.get("fileName") + "/server.xml", false);
            jmxConnection.notifyFileChanges(null, changed, null);
            for (int i = 0; i < 20 && notificationsList.size() == 0; i++) {
                print("Waiting for config update notification from remote server: " + i);
                Thread.sleep(1000);
            }
            print("Notifications received: " + notificationsList.size());
            for (Map<String, Object> userData : notificationsList) {
                print("Name: " + userData.get("name"));
                print("Message: " + userData.get("message"));
                print("Status: " + userData.get("status"));
            }
            // we should always receive at least 1 notification (if there were no configuration changes we should
            // receive a NO OP notification)
            assertTrue("Expected to recieve some notifications", notificationsList.size() > 0);
        } finally {
            jmxConnection.removeConfigListener(configListener);
        }
    }

}
