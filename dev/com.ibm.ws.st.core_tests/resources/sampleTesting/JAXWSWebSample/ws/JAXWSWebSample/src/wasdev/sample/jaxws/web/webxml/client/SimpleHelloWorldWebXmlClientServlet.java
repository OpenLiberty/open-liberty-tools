/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.jaxws.web.webxml.client;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import wasdev.sample.jaxws.web.BaseClientServlet;

@WebServlet("/SimpleHelloWorldWebXmlClientServlet")
public class SimpleHelloWorldWebXmlClientServlet extends BaseClientServlet {

    @WebServiceRef(value = SimpleHelloWorldWebXmlService.class)
    private Service service;

    @WebServiceRef
    private SimpleHelloWorldWebXmlService simpleHelloWorldWebXmlService;

    @WebServiceRef(value = SimpleHelloWorldWebXmlService.class)
    private SimpleHelloWorldWebXml simpleHelloWorldWebXml;

    @Resource
    private SimpleHelloWorldWebXmlService simpleHelloWorldWebXmlService2;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String echoParameter = request.getParameter("echoParameter");
        if (echoParameter != null) {
            invokeWebServices(request, response, echoParameter);
        }
        request.getRequestDispatcher("webxml_sample.jsp").forward(request, response);
    }

    private void invokeWebServices(HttpServletRequest request, HttpServletResponse response, String echoParameter)
            throws ServletException, IOException {
        String[][] outputs = new String[4][4];
        /*-----------Use Generic Service ---------------*/
        {
            SimpleHelloWorldWebXml simpleHelloWorldWebXml = service.getPort(SimpleHelloWorldWebXml.class);
            setEndpointAddress((BindingProvider) simpleHelloWorldWebXml, request, "CustomizedHelloWorld");
            String responseMessage = simpleHelloWorldWebXml.hello(echoParameter);
            outputs[0][0] = "@WebServiceRef(value = SimpleHelloWorldWebXmlService.class) <br/>private Service service;";
            outputs[0][1] = "Generic Service";
            outputs[0][2] = echoParameter;
            outputs[0][3] = responseMessage;
        }
        {
            /*-----------Use Sub Service ---------------*/
            SimpleHelloWorldWebXml simpleHelloWorldWebXml = simpleHelloWorldWebXmlService
                    .getSimpleHelloWorldWebXmlPort();
            setEndpointAddress((BindingProvider) simpleHelloWorldWebXml, request, "CustomizedHelloWorld");
            String responseMessage = simpleHelloWorldWebXml.hello(echoParameter);
            outputs[1][0] = " @WebServiceRef <br/> private SimpleHelloWorldWebXmlService simpleHelloWorldWebXmlService;";
            outputs[1][1] = "Specific Service";
            outputs[1][2] = echoParameter;
            outputs[1][3] = responseMessage;
        }
        /*-----------Use Port  ---------------*/
        {
            setEndpointAddress((BindingProvider) simpleHelloWorldWebXml, request, "CustomizedHelloWorld");
            String responseMessage = simpleHelloWorldWebXml.hello(echoParameter);
            outputs[2][0] = "@WebServiceRef(value = SimpleHelloWorldWebXmlService.class) <br/> private SimpleHelloWorldWebXml simpleHelloWorldWebXml;";
            outputs[2][1] = "Specific Port";
            outputs[2][2] = echoParameter;
            outputs[2][3] = responseMessage;
        }
        /*-----------Use Sub Service with Resource Annotation---------------*/
        {
            SimpleHelloWorldWebXml simpleHelloWorldWebXml = simpleHelloWorldWebXmlService2
                    .getPort(SimpleHelloWorldWebXml.class);
            setEndpointAddress((BindingProvider) simpleHelloWorldWebXml, request, "CustomizedHelloWorld");
            String responseMessage = simpleHelloWorldWebXml.hello(echoParameter);
            outputs[3][0] = " @Resource <br/> private SimpleHelloWorldWebXmlService simpleHelloWorldWebXmlService2;";
            outputs[3][1] = "Specific Service with Resource Annotation";
            outputs[3][2] = echoParameter;
            outputs[3][3] = responseMessage;
        }
        request.setAttribute("outputs", outputs);
    }
}
