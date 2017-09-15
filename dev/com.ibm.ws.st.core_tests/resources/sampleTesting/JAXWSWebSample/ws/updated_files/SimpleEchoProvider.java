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

package wasdev.sample.jaxws.web.simple;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(wsdlLocation = "WEB-INF/wsdl/SimpleEchoProviderService.wsdl", serviceName = "SimpleEchoProviderService", targetNamespace = "http://simpleEchoProvider.web.jaxws.sample.wasdev/")
@ServiceMode(value = Service.Mode.MESSAGE)
public class SimpleEchoProvider implements Provider<SOAPMessage> {

    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        try {
            //
            SOAPElement echoSoapElement = (SOAPElement) request.getSOAPBody().getChildElements().next();
            SOAPElement arg0SoapElement = (SOAPElement) echoSoapElement.getChildElements().next();
            String requestEchoValue = arg0SoapElement.getChildNodes().item(0).getNodeValue();
            //
            MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPElement soapElement = soapMessage.getSOAPBody().addChildElement(
                    new QName("http://web.jaxws.sample.wasdev/", "echoResponse", "tns"));
            soapElement.addChildElement("return").addTextNode("Updated Echo Response [" + requestEchoValue + "]");
            return soapMessage;
        } catch (SOAPException e) {
            e.printStackTrace();
            return null;
        }
    }
}
