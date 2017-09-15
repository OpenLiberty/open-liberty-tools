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

package wasdev.sample.jaxws.web.handler.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "RouteTracker", targetNamespace = "http://web.jaxws.sample.wasdev/")
@XmlSeeAlso({ ObjectFactory.class })
public interface RouteTracker {

    /**
     * 
     * @param message
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "track", targetNamespace = "http://web.jaxws.sample.wasdev/", className = "wasdev.sample.jaxws.web.handler.client.Track")
    @ResponseWrapper(localName = "trackResponse", targetNamespace = "http://web.jaxws.sample.wasdev/", className = "wasdev.sample.jaxws.web.handler.client.TrackResponse")
    public String track(@WebParam(name = "message", targetNamespace = "") String message);

}
