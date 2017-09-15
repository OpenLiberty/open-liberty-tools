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

package wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "SayHello", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/")
@XmlSeeAlso({
             ObjectFactory.class
})
public interface SayHelloPort {

    /**
     * 
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "invokeOther", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                    className = "wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient.InvokeOther")
    @ResponseWrapper(localName = "invokeOtherResponse", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                     className = "wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient.InvokeOtherResponse")
    //@Action(input = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/SayHelloPojoBean/invokeOtherRequest", output = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/SayHelloPojoBean/invokeOtherResponse")
    public String invokeOther();

    /**
     * 
     * @param arg0
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                    className = "wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient.SayHello")
    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                     className = "wasdev.sample.jaxws.ejb.ejbinwarwebservices.webclient.SayHelloResponse")
    //@Action(input = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/SayHelloPojoBean/sayHelloRequest", output = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/SayHelloPojoBean/sayHelloResponse")
    public String sayHello(
                           @WebParam(name = "arg0", targetNamespace = "") String arg0);

}
