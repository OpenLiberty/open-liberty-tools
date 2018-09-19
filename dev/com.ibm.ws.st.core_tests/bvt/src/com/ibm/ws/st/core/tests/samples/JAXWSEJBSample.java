/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.st.core.tests.samples;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.ibm.ws.st.core.tests.util.WLPCommonUtil;
import com.ibm.ws.st.tests.common.util.TestCaseDescriptor;

import junit.framework.TestSuite;

/**
 *
 */
@TestCaseDescriptor(description = "JAXWS EJB sample test case", isStable = true)
@RunWith(AllTests.class)
public class JAXWSEJBSample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JAXWSEJBSample.getOrderedTests());
        suite.setName(JAXWSEJBSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "webListUsersTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "webUserNotFoundExceptionTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "webQueryUserTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "sayHelloFromStatelessBeanTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "sayHelloFromSingletonBeanTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "sayHelloFromPOJOTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "invokeOtherFromStatelessTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "invokeOtherFromSingletonBeanTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "invokeOtherFromPOJOTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "echoClientServletTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "countdownClientServlet"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "updateSayHelloFromStatelessBeanTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "updateEchoClientServletTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "webListUsersTest1"));
        testSuite.addTest(TestSuite.createTest(JAXWSEJBSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "JAXWSEJBSample";

        SERVER_LOCATION = RESOURCE_LOCATION + SAMPLE_TEST_NAME + "/" + SERVER_NAME;

        System.out.println("Initializing Test Setup      :" + getClass().getName());

        //Run init from super class to setup resource folders.
        init();

        //create liberty runtime. Runtime name initialized from super class
        createRuntime();

        //Create server
        createServer();

        //Create a vm with the name expected by the sample test case
        createVM(JDK_NAME);

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "JAXWSEJBSample", "JAXWSEJBSample_WEB", "AnEJBWebServices",
                                                                                                  "AnEJBWebServicesWithHandler" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("JAXWSEJBSample");

    }

    @Test
    // This tests lists all the three users Tom, Jerry ,McQueen. Assertion is done only for McQueen as it is the last user on the list.
    public void webListUsersTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "webListUsers");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBWebServicesWebClientServlet", "User: McQueen, registration time:", param);

    }

    @Test
    // tests whether proper exception is thrown when user is not found
    public void webUserNotFoundExceptionTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "webUserNotFoundException");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBWebServicesWebClientServlet", "The expected UserNotFoundException is thrown, error message is", param);

    }

    @Test
    // Testing for querying particular user
    public void webQueryUserTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "webQueryUser");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBWebServicesWebClientServlet", "Found user: Tom who is registered at", param);

    }

    @Test
    // Tests for other implementations
    public void sayHelloFromStatelessBeanTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "SayHelloFromStatelessBean");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", "Hello, user from SayHelloStatelessBean", param);

    }

    @Test
    public void sayHelloFromSingletonBeanTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "SayHelloFromSingletonBean");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", " Hello, user from SayHelloSingletonBean", param);

    }

    @Test
    public void sayHelloFromPOJOTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "SayHelloFromPOJO");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", "Hello, user from SayHelloPojoBean", param);

    }

    @Test
    public void invokeOtherFromStatelessTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "InvokeOtherFromStatelessBean");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", "Hello, Anonym from SayHelloStatelessBean", param);

    }

    @Test
    public void invokeOtherFromSingletonBeanTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "InvokeOtherFromSingletonBean");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", "Hello, StatelessSessionBeanClient from SayHelloStatelessBean", param);

    }

    @Test
    public void invokeOtherFromPOJOTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "InvokeOtherFromPOJO");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", "Hello, Anonym from SayHelloPojoBean", param);

    }

    @Test
    public void echoClientServletTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoString", "Hello World");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EchoClientServlet", "Got echo string two times: Hello World Hello World", param);

    }

    @Test
    public void countdownClientServlet() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("countdownfromme", "4");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/CountdownClientServlet", "Counting down from 4: four, three, two, one! done", param);

    }

    @Test
    //Bean is updated with new text to verify incremental publish.
    public void updateSayHelloFromStatelessBeanTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("SayHelloStatelessBean.java"), getProject("JAXWSEJBSample_WEB"),
                   "src/wasdev/sample/jaxws/ejb/ejbinwarwebservices/SayHelloStatelessBean.java", 2000);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "SayHelloFromStatelessBean");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBInWarWebServicesServlet", "Updated Hello, user from SayHelloStatelessBean", param);
    }

    @Test
    //config file is updated with Triple Echo
    public void updateEchoClientServletTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("handlers.xml"), getProject("AnEJBWebServicesWithHandler"),
                   "ejbModule/wasdev/sample/jaxws/ejb/ejbwithhandlers/handlers.xml", 2000);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoString", "Hello World");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EchoClientServlet", "Got echo string two times: Hello World Hello World Hello World", param);
    }

    @Test
    // Update EJB Test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    // This tests lists all the three users Tom, Jerry ,McQueen. Assertion is done only for McQueen as it is the last user on the list.
    public void webListUsersTest1() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("method", "webListUsers");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSEJBSample/EJBWebServicesWebClientServlet", "User: McQueen, registration time:", param);

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("JAXWSEJBSample", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
