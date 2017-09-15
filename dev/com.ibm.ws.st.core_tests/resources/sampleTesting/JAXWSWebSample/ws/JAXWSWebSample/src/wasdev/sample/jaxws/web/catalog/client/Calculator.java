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

package wasdev.sample.jaxws.web.catalog.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "Calculator", targetNamespace = "http://catalog.web.jaxws.sample.wasdev", wsdlLocation = "http://foo.org/calculator.wsdl")
public class Calculator extends Service {

    private final static URL CALCULATOR_WSDL_LOCATION;

    private final static Logger logger = Logger.getLogger(wasdev.sample.jaxws.web.catalog.client.Calculator.class
            .getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = wasdev.sample.jaxws.web.catalog.client.Calculator.class.getResource(".");
            url = new URL(baseUrl, "http://localhost:9080/JaxWsLibertyDemo/Calculator?wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://localhost:9080/JaxWsLibertyDemo/Calculator?wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        CALCULATOR_WSDL_LOCATION = url;
    }

    public Calculator(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Calculator() {
        super(CALCULATOR_WSDL_LOCATION, new QName("http://catalog.web.jaxws.sample.wasdev", "Calculator"));
    }

    /**
     * 
     * @return
     *     returns CalculatorPortType
     */
    @WebEndpoint(name = "CalculatorPort")
    public CalculatorPortType getCalculatorPort() {
        return super.getPort(new QName("http://catalog.web.jaxws.sample.wasdev", "CalculatorPort"),
                CalculatorPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CalculatorPortType
     */
    @WebEndpoint(name = "CalculatorPort")
    public CalculatorPortType getCalculatorPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://catalog.web.jaxws.sample.wasdev", "CalculatorPort"),
                CalculatorPortType.class, features);
    }

}
