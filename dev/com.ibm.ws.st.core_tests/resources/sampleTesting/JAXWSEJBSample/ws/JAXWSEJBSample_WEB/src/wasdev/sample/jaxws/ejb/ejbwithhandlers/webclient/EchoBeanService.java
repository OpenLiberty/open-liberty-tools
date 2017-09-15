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

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "EchoBeanService", targetNamespace = "http://ejbwithhandlers.ejb.jaxws.sample.wasdev/",
                  wsdlLocation = "file:/D:/tmp/beta/wsgen_results/EchoBeanService.wsdl")
public class EchoBeanService
                extends Service
{

    private final static URL ECHOBEANSERVICE_WSDL_LOCATION;
    private final static WebServiceException ECHOBEANSERVICE_EXCEPTION;
    private final static QName ECHOBEANSERVICE_QNAME = new QName("http://ejbwithhandlers.ejb.jaxws.sample.wasdev/", "EchoBeanService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/D:/tmp/beta/wsgen_results/EchoBeanService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ECHOBEANSERVICE_WSDL_LOCATION = url;
        ECHOBEANSERVICE_EXCEPTION = e;
    }

    public EchoBeanService() {
        super(__getWsdlLocation(), ECHOBEANSERVICE_QNAME);
    }

    public EchoBeanService(WebServiceFeature... features) {
        super(__getWsdlLocation(), ECHOBEANSERVICE_QNAME, features);
    }

    public EchoBeanService(URL wsdlLocation) {
        super(wsdlLocation, ECHOBEANSERVICE_QNAME);
    }

    public EchoBeanService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, ECHOBEANSERVICE_QNAME, features);
    }

    public EchoBeanService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EchoBeanService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns EchoBean
     */
    @WebEndpoint(name = "EchoBeanPort")
    public EchoBean getEchoBeanPort() {
        return super.getPort(new QName("http://ejbwithhandlers.ejb.jaxws.sample.wasdev/", "EchoBeanPort"), EchoBean.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns EchoBean
     */
    @WebEndpoint(name = "EchoBeanPort")
    public EchoBean getEchoBeanPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbwithhandlers.ejb.jaxws.sample.wasdev/", "EchoBeanPort"), EchoBean.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ECHOBEANSERVICE_EXCEPTION != null) {
            throw ECHOBEANSERVICE_EXCEPTION;
        }
        return ECHOBEANSERVICE_WSDL_LOCATION;
    }

}
