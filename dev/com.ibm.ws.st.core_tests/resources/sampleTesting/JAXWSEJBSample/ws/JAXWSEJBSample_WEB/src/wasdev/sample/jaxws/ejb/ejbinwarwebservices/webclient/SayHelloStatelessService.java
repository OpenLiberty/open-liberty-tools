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

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SayHelloStatelessService", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                  wsdlLocation = "file:/D:/tmp/beta/wsgen_results/SayHelloStatelessService.wsdl")
public class SayHelloStatelessService
                extends Service
{

    private final static URL SAYHELLOSTATELESSSERVICE_WSDL_LOCATION;
    private final static WebServiceException SAYHELLOSTATELESSSERVICE_EXCEPTION;
    private final static QName SAYHELLOSTATELESSSERVICE_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloStatelessService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/D:/tmp/beta/wsgen_results/SayHelloStatelessService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SAYHELLOSTATELESSSERVICE_WSDL_LOCATION = url;
        SAYHELLOSTATELESSSERVICE_EXCEPTION = e;
    }

    public SayHelloStatelessService() {
        super(__getWsdlLocation(), SAYHELLOSTATELESSSERVICE_QNAME);
    }

    public SayHelloStatelessService(WebServiceFeature... features) {
        super(__getWsdlLocation(), SAYHELLOSTATELESSSERVICE_QNAME, features);
    }

    public SayHelloStatelessService(URL wsdlLocation) {
        super(wsdlLocation, SAYHELLOSTATELESSSERVICE_QNAME);
    }

    public SayHelloStatelessService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SAYHELLOSTATELESSSERVICE_QNAME, features);
    }

    public SayHelloStatelessService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SayHelloStatelessService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns SayHelloStatelessBean
     */
    @WebEndpoint(name = "SayHelloStalelessPort")
    public SayHelloPort getSayHelloStalelessPort() {
        return super.getPort(new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloStalelessPort"), SayHelloPort.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns SayHelloStatelessBean
     */
    @WebEndpoint(name = "SayHelloStalelessPort")
    public SayHelloPort getSayHelloStalelessPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloStalelessPort"), SayHelloPort.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SAYHELLOSTATELESSSERVICE_EXCEPTION != null) {
            throw SAYHELLOSTATELESSSERVICE_EXCEPTION;
        }
        return SAYHELLOSTATELESSSERVICE_WSDL_LOCATION;
    }

}
