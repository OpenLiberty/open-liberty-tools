/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.samples;

import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

/**
 *
 */
@TestCaseDescriptor(description = "JMS sample test case", isStable = true)
@RunWith(AllTests.class)
public class JMSSample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JMSSample.getOrderedTests());
        suite.setName(JMSSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "sendAndReceiveTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "mdbRequestResponseTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "sendMessageTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "receiveAllMessagesTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "nonDurableSubscriberTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "durableSubscriberTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "publishMessagesTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "unsubscribeDurableSubscriberTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "updatedSendAndReceiveTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "updatedDurableSubscriberTest"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "publishMessagesTest1"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "switchNonLooseConfig"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "unsubscribeDurableSubscriberTest1"));
        testSuite.addTest(TestSuite.createTest(JMSSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "JMSSample";

        SERVER_LOCATION = RESOURCE_LOCATION + SAMPLE_TEST_NAME + "/" + SERVER_NAME;

        System.out.println("Initializing Test Setup      :" + getClass().getName());

        //Run init from super class to setup resource folders.
        init();

        //create liberty runtime. Runtime name initialized from super class
        createRuntime();

        //Create server
        createServer();

        //Not sure why we need to create vm ?????
        createVM(JDK_NAME);

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "JMSApp", "JMSAppEAR" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("JMSApp");

    }

    @Test
    public void sendAndReceiveTest() throws Exception {
        testPingWebPage("JMSApp/JMSSampleP2P?ACTION=SendAndReceive", "Liberty Sample Message");

    }

    @Test
    public void mdbRequestResponseTest() throws Exception {
        testPingWebPage("JMSApp/JMSSampleP2P?ACTION=mdbRequestResponse", "MDBRequestResponse Completed");

    }

    @Test
    public void sendMessageTest() throws Exception {
        testPingWebPage("JMSApp/JMSSampleP2P?ACTION=sendMessage", "SendMessage Completed");

    }

    @Test
    public void receiveAllMessagesTest() throws Exception {
        testPingWebPage("JMSApp/JMSSampleP2P?ACTION=receiveAllMessages", "ReceiveAllMessages Completed");

    }

    @Test
    public void nonDurableSubscriberTest() throws Exception {
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=nonDurableSubscriber", "NonDurableSubscriber Completed");

    }

    @Test
    public void durableSubscriberTest() throws Exception {
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=durableSubscriber", "Liberty PubSub Message");

    }

    @Test
    public void publishMessagesTest() throws Exception {
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=publishMessages", "PublishMessage Completed");

    }

    @Test
    public void unsubscribeDurableSubscriberTest() throws Exception {
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=unsubscribeDurableSubscriber", "UnsubscribeDurableSubscriber Completed");

    }

    @Test
    // Update EJB Test
    public void updatedSendAndReceiveTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("JMSSampleP2P.java"), getProject("JMSApp"),
                   "src/wasdev/sample/jms/web/JMSSampleP2P.java", 2000);
        testPingWebPage("JMSApp/JMSSampleP2P?ACTION=SendAndReceive", "Updated Liberty Sample Message");

    }

    @Test
    // Update EJB Test
    public void updatedDurableSubscriberTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("JMSSamplePubSub.java"), getProject("JMSApp"),
                   "src/wasdev/sample/jms/web/JMSSamplePubSub.java", 2000);
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=durableSubscriber", "Updated Liberty PubSub Message");

    }

    @Test
    // Update EJB Test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    public void publishMessagesTest1() throws Exception {
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=publishMessages", "PublishMessage Completed");

    }

    @Test
    public void switchNonLooseConfig() throws Exception {
        switchconfig(false);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), false);

    }

    @Test
    public void unsubscribeDurableSubscriberTest1() throws Exception {
        testPingWebPage("JMSApp/JMSSamplePubSub?ACTION=unsubscribeDurableSubscriber", "UnsubscribeDurableSubscriber Completed");

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("JMSApp", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
