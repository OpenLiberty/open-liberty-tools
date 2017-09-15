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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SimpleHelloWorldWebXmlService", targetNamespace = "http://webxml.web.jaxws.sample.wasdev/", wsdlLocation = "WEB-INF/wsdl/SimpleHelloWorldWebXmlService.wsdl")
public class SimpleHelloWorldWebXmlService extends Service {

    private final static URL SIMPLEHELLOWORLDWEBXMLSERVICE_WSDL_LOCATION;

    private final static Logger logger = Logger
            .getLogger(wasdev.sample.jaxws.web.webxml.client.SimpleHelloWorldWebXmlService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = wasdev.sample.jaxws.web.webxml.client.SimpleHelloWorldWebXmlService.class.getResource(".");
            url = new URL(baseUrl, "http://localhost:9080/JaxWsLibertyDemo/CustomizedHelloWorld?wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://localhost:9080/JaxWsLibertyDemo/CustomizedHelloWorld?wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        SIMPLEHELLOWORLDWEBXMLSERVICE_WSDL_LOCATION = url;
    }

    public SimpleHelloWorldWebXmlService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SimpleHelloWorldWebXmlService() {
        super(SIMPLEHELLOWORLDWEBXMLSERVICE_WSDL_LOCATION, new QName("http://webxml.web.jaxws.sample.wasdev/",
                "SimpleHelloWorldWebXmlService"));
    }

    /**
     * 
     * @return
     *     returns SimpleHelloWorldWebXml
     */
    @WebEndpoint(name = "SimpleHelloWorldWebXmlPort")
    public SimpleHelloWorldWebXml getSimpleHelloWorldWebXmlPort() {
        return super.getPort(new QName("http://webxml.web.jaxws.sample.wasdev/", "SimpleHelloWorldWebXmlPort"),
                SimpleHelloWorldWebXml.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns SimpleHelloWorldWebXml
     */
    @WebEndpoint(name = "SimpleHelloWorldWebXmlPort")
    public SimpleHelloWorldWebXml getSimpleHelloWorldWebXmlPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://webxml.web.jaxws.sample.wasdev/", "SimpleHelloWorldWebXmlPort"),
                SimpleHelloWorldWebXml.class, features);
    }

}
