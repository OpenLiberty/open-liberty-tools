/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package wasdev.sample.jaxws.ejb.ejbwebservices.webclient;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "UserQuery", targetNamespace = "http://ejbwebservices.ejb.jaxws.sample.wasdev/")
@XmlSeeAlso({
             ObjectFactory.class
})
public interface UserQuery {

    /**
     * 
     * @param arg0
     * @return
     *         returns wasdev.sample.jaxws.ejb.ejbwebservices.webclient.User
     * @throws UserNotFoundException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getUser", targetNamespace = "http://ejbwebservices.ejb.jaxws.sample.wasdev/",
                    className = "wasdev.sample.jaxws.ejb.ejbwebservices.webclient.GetUser")
    @ResponseWrapper(localName = "getUserResponse", targetNamespace = "http://ejbwebservices.ejb.jaxws.sample.wasdev/",
                     className = "wasdev.sample.jaxws.ejb.ejbwebservices.webclient.GetUserResponse")
    @Action(input = "http://ejbwebservices.ejb.jaxws.sample.wasdev/UserQuery/getUserRequest",
            output = "http://ejbwebservices.ejb.jaxws.sample.wasdev/UserQuery/getUserResponse",
            fault = {
                     @FaultAction(className = UserNotFoundException_Exception.class,
                                  value = "http://ejbwebservices.ejb.jaxws.sample.wasdev/UserQuery/getUser/Fault/UserNotFoundException")
            })
    public User getUser(
                        @WebParam(name = "arg0", targetNamespace = "") String arg0)
                    throws UserNotFoundException_Exception;

    /**
     * 
     * @return
     *         returns java.util.List<wasdev.sample.jaxws.ejb.ejbwebservices.webclient.User>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "listUsers", targetNamespace = "http://ejbwebservices.ejb.jaxws.sample.wasdev/",
                    className = "wasdev.sample.jaxws.ejb.ejbwebservices.webclient.ListUsers")
    @ResponseWrapper(localName = "listUsersResponse", targetNamespace = "http://ejbwebservices.ejb.jaxws.sample.wasdev/",
                     className = "wasdev.sample.jaxws.ejb.ejbwebservices.webclient.ListUsersResponse")
    @Action(input = "http://ejbwebservices.ejb.jaxws.sample.wasdev/UserQuery/listUsersRequest",
            output = "http://ejbwebservices.ejb.jaxws.sample.wasdev/UserQuery/listUsersResponse")
    public List<User> listUsers();

}
