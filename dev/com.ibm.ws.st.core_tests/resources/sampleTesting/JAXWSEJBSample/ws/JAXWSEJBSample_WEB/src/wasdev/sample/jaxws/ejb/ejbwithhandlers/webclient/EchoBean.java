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

package wasdev.sample.jaxws.ejb.ejbwithhandlers.webclient;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "EchoBean", targetNamespace = "http://ejbwithhandlers.ejb.jaxws.sample.wasdev/")
@XmlSeeAlso({
             ObjectFactory.class
})
public interface EchoBean {

    /**
     * 
     * @param arg0
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://ejbwithhandlers.ejb.jaxws.sample.wasdev/",
                    className = "wasdev.sample.jaxws.ejb.ejbwithhandlers.webclient.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://ejbwithhandlers.ejb.jaxws.sample.wasdev/",
                     className = "wasdev.sample.jaxws.ejb.ejbwithhandlers.webclient.EchoResponse")
    @Action(input = "http://ejbwithhandlers.ejb.jaxws.sample.wasdev/EchoBean/echoRequest", output = "http://ejbwithhandlers.ejb.jaxws.sample.wasdev/EchoBean/echoResponse")
    public String echo(
                       @WebParam(name = "arg0", targetNamespace = "") String arg0);

}
