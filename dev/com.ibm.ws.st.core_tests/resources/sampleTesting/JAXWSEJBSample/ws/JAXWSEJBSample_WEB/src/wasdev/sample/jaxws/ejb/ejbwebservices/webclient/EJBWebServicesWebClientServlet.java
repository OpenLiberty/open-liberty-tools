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
package wasdev.sample.jaxws.ejb.ejbwebservices.webclient;

import java.io.IOException;
import java.util.List;

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
@WebServlet("/EJBWebServicesWebClientServlet")
public class EJBWebServicesWebClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String SEI_URI = "AnEJBWebServices/UserQueryService";

    @WebServiceRef(value = UserQueryService.class, wsdlLocation = "WEB-INF/wsdl/UserQueryService.wsdl")
    private UserQuery userQuery;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getParameter("method");
        if (method.equals("webQueryUser")) {
            webQueryUser(req, resp);
        } else if (method.equals("webUserNotFoundException")) {
            webUserNotFoundException(req, resp);
        } else if (method.equals("webListUsers")) {
            webListUsers(req, resp);
        } else {
            req.setAttribute("demoResponse", "Unable to recognize the test method \"" + method + "\"");
            req.getRequestDispatcher("userquery_anejbwebservices_sample.jsp").forward(req, resp);
        }
    }

    public void webUserNotFoundException(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            setEndpointAddress((BindingProvider) userQuery, req, SEI_URI);
            userQuery.getUser("none");
            req.setAttribute("demoResponse", "Oops! An UserNotFoundException should happen.");
        } catch (UserNotFoundException_Exception e) {
            String userName = e.getFaultInfo().getUserName();
            if (userName.equals("none")) {
                req.setAttribute("demoResponse", "The expected UserNotFoundException is thrown, error message is \"" + e.getMessage() + "\"");
            } else {
                req.setAttribute("demoResponse", "Oops! User name 'none' is not found in the exception message.");
            }
        } finally {
            req.getRequestDispatcher("userquery_anejbwebservices_sample.jsp").forward(req, resp);
        }
    }

    public void webQueryUser(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String username = req.getParameter("username") == null ? "Tom" : req.getParameter("username");
        try {
            setEndpointAddress((BindingProvider) userQuery, req, SEI_URI);
            User user = userQuery.getUser(username);
            if (user == null) {
                req.setAttribute("demoResponse", "User " + username + " not found!");
            } else {
                req.getSession().setAttribute("demoResponse", "Found user: " + user.getName() + " who is registered at " + user.getRegisterDate());
                req.setAttribute("demoResponse", "Found user: " + user.getName() + " who is registered at " + user.getRegisterDate());
            }
        } catch (UserNotFoundException_Exception e) {
            req.setAttribute("demoResponse", "Oops! An UserNotFoundException is thrown, error message is \"" + e.getMessage() + "\"");
        } finally {
            req.getRequestDispatcher("userquery_anejbwebservices_sample.jsp").forward(req, resp);
        }
    }

    public void webListUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        setEndpointAddress((BindingProvider) userQuery, req, SEI_URI);
        List<User> users = userQuery.listUsers();
        if (users == null) {
            req.setAttribute("demoResponse", "An error happened, user list is not returned!");
        } else {
            StringBuffer returnString = new StringBuffer();
            returnString.append("Got user list:\n");
            for (User aUser : users) {
                returnString.append("User: " + aUser.getName() + ", registration time: " + aUser.getRegisterDate() + "\n");
            }
            req.setAttribute("demoResponse", returnString.toString());
        }
        req.getRequestDispatcher("userquery_anejbwebservices_sample.jsp").forward(req, resp);
    }

    protected void setEndpointAddress(BindingProvider bindingProvider, HttpServletRequest request, String endpointPath) {
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                "http://" + request.getServerName() + ":" + request.getServerPort() + "/" + endpointPath);
    }
}
