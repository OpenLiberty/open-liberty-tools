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

package wasdev.sample.jaxws.web.wsfeatures;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;

@MTOM
@Addressing
@HandlerChain(file = "handler.xml")
@WebService(targetNamespace = "http://jaxws.service/", portName = "ImageServiceImplPort", serviceName = "ImageServiceImplService")
public class ImageServiceImpl {

    @Resource
    private WebServiceContext webServiceContext;

    public byte[] uploadImage(String id, byte[] bytes) {
        /*FileOutputStream out = null;
        try {
            out = new FileOutputStream(id + ".jpg");
            image.writeTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }*/
        return ((String) webServiceContext.getMessageContext().get("request.message")).getBytes();
    }
}
