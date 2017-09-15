package wasdev.sample.jaxws.ejb.webservicesxml.webclient;

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
@WebServlet("/CountdownClientServlet")
@SuppressWarnings("serial")
public class CountdownClientServlet extends HttpServlet {

    private static final String SEI_URI = "/JAXWSEJBSample/CountdownImplService";

    @WebServiceRef(value = CountdownImplService.class, wsdlLocation = "WEB-INF/wsdl/CountdownImplService.wsdl")
    private CountdownImpl countdownPort;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String countdownfromme = req.getParameter("countdownfromme");
        if (countdownfromme == null || countdownfromme.isEmpty()) {
            req.setAttribute("demoResponse", "You need to specify a number between 1 and 10!");
        } else {
            callCountdown(countdownfromme, req, resp);
        }
        req.getRequestDispatcher("countdown_webservicesxmlsupport_sample.jsp").forward(req, resp);
    }

    public void callCountdown(String countdownfromme, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            setEndpointAddress((BindingProvider) countdownPort, req, SEI_URI);
            req.setAttribute("demoResponse", "Counting down from " + countdownfromme + ": "
                                             + countdownPort.countdownfromme((new Integer(countdownfromme)).intValue()));
        } catch (Exception e) {
            req.setAttribute("demoResponse", "Oops! An exception is thrown, error message is \"" + e.getMessage() + "\"");
        }
    }

    protected void setEndpointAddress(BindingProvider bindingProvider, HttpServletRequest request, String endpointURI) {
        String endpointAddress = "http://" + request.getServerName() + ":" + request.getServerPort() + endpointURI;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }
}
