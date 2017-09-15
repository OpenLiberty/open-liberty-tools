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

package wasdev.sample.jaxws.web.wsfeatures.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "ImageServiceImpl", targetNamespace = "http://jaxws.service/")
@XmlSeeAlso({ ObjectFactory.class })
public interface ImageServiceImpl {

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns byte[]
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "uploadImage", targetNamespace = "http://jaxws.service/", className = "wasdev.sample.jaxws.web.wsfeatures.client.UploadImage")
    @ResponseWrapper(localName = "uploadImageResponse", targetNamespace = "http://jaxws.service/", className = "wasdev.sample.jaxws.web.wsfeatures.client.UploadImageResponse")
    public byte[] uploadImage(@WebParam(name = "arg0", targetNamespace = "") String arg0,
            @WebParam(name = "arg1", targetNamespace = "") byte[] arg1);

}
