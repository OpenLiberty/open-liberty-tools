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
@TestCaseDescriptor(description = "JAXWS web sample test case", isStable = true)
@RunWith(AllTests.class)
public class JAXWSWebSample extends SampleTestBase {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JAXWSWebSample.getOrderedTests());
        suite.setName(JAXWSWebSample.class.getSimpleName());
        return suite;
    }

    public static TestSuite getOrderedTests() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "doSetup"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "simpleWebServiceTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "simpleWebServiceProviderTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webXMLSampleTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webXMLHandlerTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webServiceContextTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webCatalogTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "wsfeaturesTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "updateSimpleWebServiceProviderTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "updatedWebXMLSampleTest"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "switchLooseConfig"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webXMLHandlerTest1"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webServiceContextTest1"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "webCatalogTest1"));
        testSuite.addTest(TestSuite.createTest(JAXWSWebSample.class, "doTearDown"));
        return testSuite;
    }

    @Test
    public void doSetup() throws Exception {

        //Testname is same as test folder name under resource directory
        SAMPLE_TEST_NAME = "JAXWSWebSample";

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

        importProjects(new Path("sampleTesting" + "/" + SAMPLE_TEST_NAME + "/ws"), new String[] { "JAXWSWebSample", "JAXWSWebSampleEAR" });

        //start server and Add the application
        startServer();
        wait("Wait 5 seconds before adding application.", 5000);
        addApp("JAXWSWebSample");

    }

    @Test
    public void simpleWebServiceTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "hello World");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/SimpleStubClientServlet", "Echo Response [hello World]", param);

    }

    @Test
    public void simpleWebServiceProviderTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "hello world");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/SimpleDynamicClient", "Echo Response [hello world]", param);

    }

    @Test
    public void webXMLSampleTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "1");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/SimpleHelloWorldWebXmlClientServlet", "<td align='center'>Hello, 1</td>", param);

    }

    @Test
    public void webXMLHandlerTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "hello world");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/HandlerClientServlet", "response [hello world] Please check the outputs on the console", param);

    }

    @Test
    public void webServiceContextTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("submit", "Show Current MessageContext Properties");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/WebServiceContextServlet", "HTTP.RESPONSE : com.ibm.ws.webcontainer31.srt.SRTServletResponse31", param);

    }

    @Test
    public void webCatalogTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("parameter1", "1");
        params.put("parameter2", "2");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/CatalogClientServlet", "3", param);

    }

    @Test
    public void wsfeaturesTest() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("uploadId", "1");
        params.put("uploadMTOM", "uploadMTOMEnabled");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/ImageClientServlet", "Accept", param);

    }

    @Test
    // This test whether proper exception is thrown when user is not found
    public void updateSimpleWebServiceProviderTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("SimpleEchoProvider.java"), getProject("JAXWSWebSample"),
                   "src/wasdev/sample/jaxws/web/simple/SimpleEchoProvider.java", 2000);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "hello world");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/SimpleDynamicClient", "Updated Echo Response [hello world]", param);

    }

    @Test
    public void updatedWebXMLSampleTest() throws Exception {
        updateFile(getUpdatedFileFolder().append("SimpleHelloWorldWebXml.java"), getProject("JAXWSWebSample"),
                   "src/wasdev/sample/jaxws/web/webxml/SimpleHelloWorldWebXml.java", 2000);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "1");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/SimpleHelloWorldWebXmlClientServlet", "<td align='center'>Updated Hello, 1</td>", param);

    }

    @Test
    public void switchLooseConfig() throws Exception {
        switchconfig(true);
        safePublishIncremental(server);
        assertEquals(wsServer.isLooseConfigEnabled(), true);

    }

    @Test
    public void webXMLHandlerTest1() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("echoParameter", "hello world");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/HandlerClientServlet", "response [hello world] Please check the outputs on the console", param);

    }

    @Test
    public void webServiceContextTest1() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("submit", "Show Current MessageContext Properties");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/WebServiceContextServlet", "HTTP.RESPONSE : com.ibm.ws.webcontainer31.srt.SRTServletResponse31", param);

    }

    @Test
    public void webCatalogTest1() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("parameter1", "1");
        params.put("parameter2", "2");
        String param = getParamString(params);
        testWebPageWithURLParam("JAXWSWebSample/CatalogClientServlet", "3", param);

    }

    @Test
    public void doTearDown() throws Exception {
        removeApp("JAXWSWebSample", 2500);
        stopServer();
        WLPCommonUtil.cleanUp();
        wait("Ending test: " + getClass().getName() + "\n", 5000);
    }

}
