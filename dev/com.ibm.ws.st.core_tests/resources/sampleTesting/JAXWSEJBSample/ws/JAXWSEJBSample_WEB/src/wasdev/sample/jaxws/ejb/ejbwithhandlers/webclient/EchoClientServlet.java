package wasdev.sample.jaxws.ejb.ejbwithhandlers.webclient;

/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

/**
 *
 */
@WebServlet("/EchoClientServlet")
public class EchoClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String SEI_URI = "/AnEJBWebServicesWithHandler/EchoBeanService";

    @WebServiceRef(value = EchoBeanService.class, wsdlLocation = "WEB-INF/wsdl/EchoBeanService.wsdl")
    private EchoBean echoPort;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String echoString = req.getParameter("echoString");
        if (echoString == null || echoString.isEmpty()) {
            req.setAttribute("demoResponse", "No string to echo, and you need to specify one!");
        } else {
            callEcho(echoString, req, resp);
        }
        req.getRequestDispatcher("echo_ejbwithhandler_sample.jsp").forward(req, resp);
    }

    public void callEcho(String echoString, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            setEndpointAddress((BindingProvider) echoPort, req, SEI_URI);
            req.setAttribute("demoResponse", "Got echo string two times: " + echoPort.echo(echoString));
        } catch (Exception e) {
            req.setAttribute("demoResponse", "Oops! An exception is thrown, error message is \"" + e.getMessage() + "\"");
        }
    }

    protected void setEndpointAddress(BindingProvider bindingProvider, HttpServletRequest request, String endpointURI) {
        String endpointAddress = "http://" + request.getServerName() + ":" + request.getServerPort() + endpointURI;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        System.out.println("In servlet " + request.getServletPath().substring(1)
                           + ", javax.xml.ws.service.endpoint.address=http://" + endpointAddress);
    }
}
