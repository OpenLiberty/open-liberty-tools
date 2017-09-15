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

@WebServiceClient(name = "SayHelloPOJOService", targetNamespace = "http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/",
                  wsdlLocation = "file:/D:/tmp/beta/wsgen_results/SayHelloPOJOService.wsdl")
public class SayHelloPOJOService
                extends Service
{

    private final static URL SAYHELLOPOJOSERVICE_WSDL_LOCATION;
    private final static WebServiceException SAYHELLOPOJOSERVICE_EXCEPTION;
    private final static QName SAYHELLOPOJOSERVICE_QNAME = new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloPOJOService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/D:/tmp/beta/wsgen_results/SayHelloPOJOService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SAYHELLOPOJOSERVICE_WSDL_LOCATION = url;
        SAYHELLOPOJOSERVICE_EXCEPTION = e;
    }

    public SayHelloPOJOService() {
        super(__getWsdlLocation(), SAYHELLOPOJOSERVICE_QNAME);
    }

    public SayHelloPOJOService(WebServiceFeature... features) {
        super(__getWsdlLocation(), SAYHELLOPOJOSERVICE_QNAME, features);
    }

    public SayHelloPOJOService(URL wsdlLocation) {
        super(wsdlLocation, SAYHELLOPOJOSERVICE_QNAME);
    }

    public SayHelloPOJOService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SAYHELLOPOJOSERVICE_QNAME, features);
    }

    public SayHelloPOJOService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SayHelloPOJOService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns SayHelloPojoBean
     */
    @WebEndpoint(name = "SayHelloPOJOPort")
    public SayHelloPort getSayHelloPOJOPort() {
        return super.getPort(new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloPOJOPort"), SayHelloPort.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns SayHelloPojoBean
     */
    @WebEndpoint(name = "SayHelloPOJOPort")
    public SayHelloPort getSayHelloPOJOPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbinwarwebservices.ejb.jaxws.sample.wasdev/", "SayHelloPOJOPort"), SayHelloPort.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SAYHELLOPOJOSERVICE_EXCEPTION != null) {
            throw SAYHELLOPOJOSERVICE_EXCEPTION;
        }
        return SAYHELLOPOJOSERVICE_WSDL_LOCATION;
    }

}
