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
package wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

@WebServlet("/EJBInWarWebServicesServlet")
@SuppressWarnings("serial")
public class EJBInWarWebServicesServlet extends HttpServlet {
    @WebServiceRef(value = SayHelloSingletonService.class, wsdlLocation = "WEB-INF/wsdl/SayHelloSingletonService.wsdl")
    SayHelloSingletonService singletonService;

    @WebServiceRef(value = SayHelloStatelessService.class, wsdlLocation = "WEB-INF/wsdl/SayHelloStatelessService.wsdl")
    SayHelloStatelessService statelessService;

    @WebServiceRef(value = SayHelloPOJOService.class, wsdlLocation = "WEB-INF/wsdl/SayHelloPOJOService.wsdl")
    SayHelloPOJOService pojoService;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        SayHelloPort singletonSayHello = getAndConfigPort(singletonService, req);
        SayHelloPort statelessSayHello = getAndConfigPort(statelessService, req);
        SayHelloPort pojoSayHello = getAndConfigPort(pojoService, req);

        String queryMethod = req.getParameter("method");

        try {
            if ("SayHelloFromStatelessBean".equals(queryMethod)) {
                req.setAttribute("demoResponse", statelessSayHello.sayHello("user"));
            } else if ("SayHelloFromSingletonBean".equals(queryMethod)) {
                req.setAttribute("demoResponse", singletonSayHello.sayHello("user"));
            } else if ("InvokeOtherFromStatelessBean".equals(queryMethod)) {
                req.setAttribute("demoResponse", statelessSayHello.invokeOther());
            } else if ("InvokeOtherFromSingletonBean".equals(queryMethod)) {
                req.setAttribute("demoResponse", singletonSayHello.invokeOther());
            } else if ("SayHelloFromPOJO".equals(queryMethod)) {
                req.setAttribute("demoResponse", pojoSayHello.sayHello("user"));
            } else if ("InvokeOtherFromPOJO".equals(queryMethod)) {
                req.setAttribute("demoResponse", pojoSayHello.invokeOther());
            } else {
                req.setAttribute("demoResponse", "Incorrect method called: \"" + queryMethod + "\"");
            }
        } catch (Exception e) {
            req.setAttribute("demoResponse", e.getMessage());
        }
        req.getRequestDispatcher("sayhello_ejbinwarwebservices_sample.jsp").forward(req, resp);
    }

    private SayHelloPort getAndConfigPort(Service service, HttpServletRequest req) {
        SayHelloPort helloPort;
        String path = null;
        if (service instanceof SayHelloSingletonService) {
            helloPort = ((SayHelloSingletonService) service).getSayHelloSingletonPort();
            path = req.getContextPath() + "/SayHelloSingletonService";
        } else if (service instanceof SayHelloStatelessService) {
            helloPort = ((SayHelloStatelessService) service).getSayHelloStalelessPort();
            path = req.getContextPath() + "/SayHelloStatelessService";
        } else {
            helloPort = ((SayHelloPOJOService) service).getSayHelloPOJOPort();
            path = req.getContextPath() + "/SayHelloPOJOService";
        }

        int port = req.getLocalPort();
        String host = req.getLocalAddr();
        BindingProvider provider = (BindingProvider) helloPort;
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                         "http://" + host + ":" + port + path);
        return helloPort;
    }

}
