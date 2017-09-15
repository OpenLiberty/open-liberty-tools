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

@WebServiceClient(name = "SayHelloSingletonService", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                  wsdlLocation = "file:/D:/tmp/beta/wsgen_results/SayHelloSingletonService.wsdl")
public class SayHelloSingletonService
                extends Service
{

    private final static URL SAYHELLOSINGLETONSERVICE_WSDL_LOCATION;
    private final static WebServiceException SAYHELLOSINGLETONSERVICE_EXCEPTION;
    private final static QName SAYHELLOSINGLETONSERVICE_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloSingletonService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/D:/tmp/beta/wsgen_results/SayHelloSingletonService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SAYHELLOSINGLETONSERVICE_WSDL_LOCATION = url;
        SAYHELLOSINGLETONSERVICE_EXCEPTION = e;
    }

    public SayHelloSingletonService() {
        super(__getWsdlLocation(), SAYHELLOSINGLETONSERVICE_QNAME);
    }

    public SayHelloSingletonService(WebServiceFeature... features) {
        super(__getWsdlLocation(), SAYHELLOSINGLETONSERVICE_QNAME, features);
    }

    public SayHelloSingletonService(URL wsdlLocation) {
        super(wsdlLocation, SAYHELLOSINGLETONSERVICE_QNAME);
    }

    public SayHelloSingletonService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SAYHELLOSINGLETONSERVICE_QNAME, features);
    }

    public SayHelloSingletonService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SayHelloSingletonService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns SayHelloSingletonBean
     */
    @WebEndpoint(name = "SayHelloSingletonPort")
    public SayHelloPort getSayHelloSingletonPort() {
        return super.getPort(new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloSingletonPort"), SayHelloPort.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns SayHelloSingletonBean
     */
    @WebEndpoint(name = "SayHelloSingletonPort")
    public SayHelloPort getSayHelloSingletonPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloSingletonPort"), SayHelloPort.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SAYHELLOSINGLETONSERVICE_EXCEPTION != null) {
            throw SAYHELLOSINGLETONSERVICE_EXCEPTION;
        }
        return SAYHELLOSINGLETONSERVICE_WSDL_LOCATION;
    }

}
