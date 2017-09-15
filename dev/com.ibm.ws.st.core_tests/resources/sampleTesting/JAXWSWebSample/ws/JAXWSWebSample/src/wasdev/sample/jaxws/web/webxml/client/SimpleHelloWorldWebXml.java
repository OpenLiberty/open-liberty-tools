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

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "SimpleHelloWorldWebXml", targetNamespace = "http://webxml.web.jaxws.sample.wasdev/")
@XmlSeeAlso({ ObjectFactory.class })
public interface SimpleHelloWorldWebXml {

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "hello", targetNamespace = "http://webxml.web.jaxws.sample.wasdev/", className = "wasdev.sample.jaxws.web.webxml.client.Hello")
    @ResponseWrapper(localName = "helloResponse", targetNamespace = "http://webxml.web.jaxws.sample.wasdev/", className = "wasdev.sample.jaxws.web.webxml.client.HelloResponse")
    public String hello(@WebParam(name = "arg0", targetNamespace = "") String arg0);

}
