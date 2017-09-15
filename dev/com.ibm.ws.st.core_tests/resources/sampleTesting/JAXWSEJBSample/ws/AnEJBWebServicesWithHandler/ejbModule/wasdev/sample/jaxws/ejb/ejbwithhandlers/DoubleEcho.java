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

package wasdev.sample.jaxws.ejb.ejbwithhandlers;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Node;

/**
 *
 */
public class DoubleEcho implements SOAPHandler<SOAPMessageContext> {

    public DoubleEcho() {}

    @PostConstruct
    public void initialize() {
        System.out.println(this.getClass().getName() + ":postConstruct is invoked");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println(this.getClass().getName() + ":PreDestroy is invoked");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.handler.Handler#handleMessage(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleMessage(SOAPMessageContext msgctx) {
        
        try {
            final SOAPMessage message = msgctx.getMessage();
            final SOAPBody body = message.getSOAPBody();
            final String localName = body.getFirstChild().getLocalName();

            if ("echoResponse".equals(localName)) {
                final Node responseNode = body.getFirstChild();
                final Node returnNode = responseNode.getFirstChild();
                final Node stringNode = returnNode.getFirstChild();

                final String value = stringNode.getNodeValue() + " "+ stringNode.getNodeValue();
                stringNode.setNodeValue(value);
            }

            return true;
        } catch (SOAPException e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.handler.Handler#handleFault(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        System.out.println(this.getClass().getName() + ": handle fault message");
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.handler.Handler#close(javax.xml.ws.handler.MessageContext)
     */
    @Override
    public void close(MessageContext context) {
        System.out.println(this.getClass().getName() + " is closed");

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.handler.soap.SOAPHandler#getHeaders()
     */
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

}
